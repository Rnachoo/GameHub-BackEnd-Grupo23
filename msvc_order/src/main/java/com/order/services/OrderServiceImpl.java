package com.order.services;

import com.order.clients.InventoryClient;
import com.order.clients.ProductClient;
import com.order.clients.PromotionClient;
import com.order.clients.UserClient;
import com.order.exceptions.AccesoDenegadoException;
import com.order.exceptions.OrderException;
import com.order.exceptions.RecursoNoEncontradoException;
import com.order.models.Order;
import com.order.models.OrderItem;
import com.order.models.dtos.*;
import com.order.repositories.OrderRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final Set<String> ESTADOS =
            Set.of("PENDIENTE_PAGO", "PAGADA", "CANCELADA");

    private static final Pattern MENSAJE_ERROR =
            Pattern.compile("\"(?:error|mensaje)\"\\s*:\\s*\"([^\"]+)\"");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private InventoryClient inventoryClient;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private PromotionClient promotionClient;

    @Autowired
    private UserClient userClient;

    @Transactional(readOnly = true)
    @Override
    public List<OrderDetalleDTO> findByClient(Long userId) {
        log.info("Buscando ordenes para el cliente con ID: {}", userId);
        return orderRepository.findByUserId(userId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<OrderDetalleDTO> findByEstado(String estado) {
        log.info("Buscando ordenes con estado: {}", estado);
        return orderRepository.findByEstado(estado.toUpperCase(Locale.ROOT)).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public OrderDetalleDTO findById(Long id) {
        return toDTO(buscar(id));
    }

    @Override
    public void validarDueno(Long userId, String email) {
        UserDTO usuario;
        try {
            usuario = userClient.getUserById(userId);
        } catch (FeignException e) {
            throw new RecursoNoEncontradoException("El user con id " + userId + " no existe");
        }

        if (usuario.getEmail() == null || !usuario.getEmail().equalsIgnoreCase(email)) {
            throw new AccesoDenegadoException(
                    "No tienes permiso para acceder a las ordenes de otro usuario"
            );
        }
    }

    @Transactional
    @Override
    public OrderDetalleDTO save(OrderSaveDTO orderSaveDTO) {
        log.info("Creando orden para el user {}", orderSaveDTO.getUserId());

        try {
            userClient.getUserById(orderSaveDTO.getUserId());
        } catch (FeignException e) {
            throw new RecursoNoEncontradoException(
                    "El user con id " + orderSaveDTO.getUserId() + " no existe"
            );
        }

        Order newOrder = new Order();
        newOrder.setUserId(orderSaveDTO.getUserId());
        newOrder.setFecha(LocalDateTime.now());
        newOrder.setEstado("PENDIENTE_PAGO");

        long subtotal = 0;
        List<OrderItem> items = new ArrayList<>();

        for (OrderSaveItemDTO saveItem : orderSaveDTO.getItems()) {
            ProductDTO producto = buscarProducto(saveItem.getProductId());
            long precio = producto.getPrecioDescuento() != null
                    ? producto.getPrecioDescuento()
                    : producto.getPrecio();

            OrderItem item = new OrderItem();
            item.setProductId(saveItem.getProductId());
            item.setCantidad(saveItem.getCantidad());
            item.setPrecioUnitario(precio);
            item.setOrder(newOrder);
            items.add(item);

            subtotal += precio * saveItem.getCantidad();
        }

        List<OrderItem> reservados = new ArrayList<>();
        for (OrderItem item : items) {
            try {
                inventoryClient.reservarStock(item.getProductId(), cantidad(item.getCantidad()));
                reservados.add(item);
            } catch (FeignException e) {
                liberar(reservados);
                throw new OrderException(
                        "No hay stock suficiente para el producto " + item.getProductId()
                );
            }
        }

        long descuento = 0;
        String codigo = orderSaveDTO.getCodigoPromocion();

        if (codigo != null && !codigo.isBlank()) {
            try {
                PromotionSaveDTO promotionSaveDTO = new PromotionSaveDTO(
                        codigo.trim().toUpperCase(Locale.ROOT),
                        orderSaveDTO.getItems()
                );
                PromotionDTO promocion = promotionClient.aplicarPromocion(
                        promotionSaveDTO.getCodigo(),
                        promotionSaveDTO,
                        (double) subtotal
                );
                descuento = calcularDescuento(promocion, subtotal);
            } catch (FeignException e) {
                liberar(reservados);
                throw new OrderException(mensajeCupon(e));
            }
        }

        newOrder.setItems(items);
        newOrder.setSubtotal(subtotal);
        newOrder.setDescuento(descuento);
        newOrder.setTotal(subtotal - descuento);

        Order guardada = orderRepository.save(newOrder);
        log.info("Orden {} guardada con total {}", guardada.getId(), guardada.getTotal());
        return toDTO(guardada);
    }

    @Transactional
    @Override
    public OrderDetalleDTO updateEstado(Long id, OrderUpdateEstadoDTO orderUpdateEstadoDTO) {
        Order orden = buscarParaActualizar(id);
        String nuevoEstado = orderUpdateEstadoDTO.getEstado().toUpperCase(Locale.ROOT);

        if (!ESTADOS.contains(nuevoEstado)) {
            throw new OrderException("Estado no valido. Use PENDIENTE_PAGO, PAGADA o CANCELADA");
        }

        // Una repetición posterior al guardado no debe ejecutar otra vez la transición.
        if (nuevoEstado.equals(orden.getEstado())) {
            return toDTO(orden);
        }

        if ("CANCELADA".equals(orden.getEstado())) {
            throw new OrderException("Una orden cancelada no puede cambiar de estado.");
        }

        if ("CANCELADA".equals(nuevoEstado)) {
            liberar(orden.getItems());
        }

        orden.setEstado(nuevoEstado);
        Order guardada = orderRepository.save(orden);
        log.info("Orden {} actualizada a {}", id, nuevoEstado);
        return toDTO(guardada);
    }

    @Transactional
    @Override
    public void cancelarOrden(Long id) {
        Order orden = buscarParaActualizar(id);

        if ("CANCELADA".equals(orden.getEstado())) {
            throw new OrderException("La orden ya se encuentra cancelada.");
        }

        if (!"PENDIENTE_PAGO".equals(orden.getEstado())) {
            throw new OrderException("Solo se puede cancelar una orden pendiente de pago.");
        }

        orden.setEstado("CANCELADA");
        liberar(orden.getItems());
        orderRepository.save(orden);
        log.info("Orden {} cancelada", id);
    }

    private Order buscar(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La orden con id " + id + " no existe"
                ));
    }

    private Order buscarParaActualizar(Long id) {
        return orderRepository.findOrdenById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "La orden con id " + id + " no existe"
                ));
    }

    private long calcularDescuento(PromotionDTO promocion, long subtotal) {
        double descuento = "PORCENTAJE".equalsIgnoreCase(promocion.getTipo())
                ? subtotal * promocion.getValor() / 100
                : promocion.getValor();

        if (promocion.getTope() != null && descuento > promocion.getTope()) {
            descuento = promocion.getTope();
        }

        return Math.min(Math.round(descuento), subtotal);
    }

    private String mensajeCupon(FeignException e) {
        Matcher matcher = MENSAJE_ERROR.matcher(e.contentUTF8());
        return matcher.find()
                ? matcher.group(1)
                : "El cupon ingresado no es valido o esta vencido";
    }

    private ProductDTO buscarProducto(Long productId) {
        ProductDTO producto;
        try {
            producto = productClient.getProductById(productId);
        } catch (FeignException e) {
            throw new RecursoNoEncontradoException(
                    "El producto con id " + productId + " no existe o está inactivo"
            );
        }

        if (producto == null || !"Activo".equals(producto.getEstado())) {
            throw new OrderException(
                    "El producto con id " + productId + " no existe o está inactivo"
            );
        }

        return producto;
    }

    private void liberar(List<OrderItem> items) {
        for (OrderItem item : items) {
            try {
                inventoryClient.liberarStock(item.getProductId(), cantidad(item.getCantidad()));
            } catch (FeignException e) {
                log.error("No se pudo liberar stock del producto {}", item.getProductId());
            }
        }
    }

    private InventoryCantidadDTO cantidad(Long valor) {
        InventoryCantidadDTO dto = new InventoryCantidadDTO();
        dto.setCantidad(valor);
        return dto;
    }

    private OrderDetalleDTO toDTO(Order orden) {
        OrderDetalleDTO dto = new OrderDetalleDTO();
        dto.setId(orden.getId());
        dto.setUserId(orden.getUserId());
        dto.setFecha(orden.getFecha());
        dto.setEstado(orden.getEstado());
        dto.setSubtotal(orden.getSubtotal());
        dto.setDescuento(orden.getDescuento());
        dto.setTotal(orden.getTotal());
        dto.setItems(orden.getItems().stream().map(item -> {
            OrderItemDTO itemDto = new OrderItemDTO();
            itemDto.setId(item.getId());
            itemDto.setProductId(item.getProductId());
            itemDto.setCantidad(item.getCantidad());
            itemDto.setPrecioUnitario(item.getPrecioUnitario());
            return itemDto;
        }).toList());
        return dto;
    }
}