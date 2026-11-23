package com.inventory.services;

import com.inventory.clients.ProductClient;
import com.inventory.exceptions.InventoryException;
import com.inventory.exceptions.RecursoNoEncontradoException;
import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.models.OperacionStockOrden;
import com.inventory.models.dtos.ConfirmacionStockDTO;
import com.inventory.models.dtos.InventoryCantidadDTO;
import com.inventory.models.dtos.InventoryDetalleDTO;
import com.inventory.models.dtos.InventorySaveDTO;
import com.inventory.models.dtos.InventoryUpdateCantidadDisponibleDTO;
import com.inventory.models.dtos.MovimientoDetalleDTO;
import com.inventory.repositories.InventoryRepository;
import com.inventory.repositories.OperacionStockOrdenRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private ProductClient productClient;

    @Autowired
    private OperacionStockOrdenRepository operacionStockOrdenRepository;

    @Transactional(readOnly = true)
    @Override
    public List<InventoryDetalleDTO> findAll() {
        log.info("Listando el stock de todos los productos");
        return inventoryRepository.findAll().stream()
                .map(this::toDTOSinMovimientos)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<InventoryDetalleDTO> findAllByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public InventoryDetalleDTO findByID(Long id) {
        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Inventario con ID " + id + " no encontrado"
                ));
        return toDTO(inventory);
    }

    @Transactional
    @Override
    public InventoryDetalleDTO save(InventorySaveDTO inventorySaveDTO) {
        Long productId = inventorySaveDTO.getProductId();

        try {
            productClient.getProductById(productId);
        } catch (FeignException e) {
            log.error("Error al consultar msvc-product para el producto con ID {}", productId);
            throw new InventoryException(
                    "El producto con ID " + productId
                            + " no existe o el servicio no está disponible. "
                            + "No se puede crear el inventario."
            );
        }

        if (inventoryRepository.existsByProductId(productId)) {
            throw new InventoryException(
                    "El producto con ID " + productId + " ya tiene inventario registrado."
            );
        }

        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setStockDisponible(inventorySaveDTO.getStockDisponible());
        inventory.setStockReservado(inventorySaveDTO.getStockReservado());
        inventory.setStockMinimo(inventorySaveDTO.getStockMinimo());
        inventory.setUbicacion(inventorySaveDTO.getUbicacion());

        List<MovimientoDetalleDTO> movimientos = inventorySaveDTO.getMovimientosDTO();
        if (movimientos == null || movimientos.isEmpty()) {
            registrarMovimiento(inventory, "ENTRADA", inventory.getStockDisponible());
        } else {
            for (MovimientoDetalleDTO movimiento : movimientos) {
                registrarMovimiento(inventory, movimiento.getTipo(), movimiento.getCantidad());
            }
        }

        Inventory guardado = inventoryRepository.save(inventory);
        log.info("Inventario {} creado para el producto {}", guardado.getId(), productId);
        return toDTO(guardado);
    }

    @Transactional
    @Override
    public InventoryDetalleDTO updateCantidadDisponible(
            Long id,
            InventoryUpdateCantidadDisponibleDTO cantidadDisponibleDTO
    ) {
        Inventory inventory = inventoryRepository.findInventarioById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Inventario no encontrado, no se puede actualizar el stock"
                ));

        long nuevoStock = cantidadDisponibleDTO.getStockDisponible();
        if (nuevoStock < 0) {
            throw new InventoryException("El stock disponible no puede quedar en negativo.");
        }

        long diferencia = nuevoStock - inventory.getStockDisponible();
        inventory.setStockDisponible(nuevoStock);

        if (cantidadDisponibleDTO.getStockMinimo() != null) {
            inventory.setStockMinimo(cantidadDisponibleDTO.getStockMinimo());
        }

        String ubicacion = cantidadDisponibleDTO.getUbicacion();
        if (ubicacion != null && !ubicacion.isBlank()) {
            inventory.setUbicacion(ubicacion);
        }

        if (diferencia != 0) {
            String tipo = diferencia > 0 ? "AJUSTE_ENTRADA" : "AJUSTE_SALIDA";
            registrarMovimiento(inventory, tipo, Math.abs(diferencia));
        }

        Inventory guardado = inventoryRepository.save(inventory);
        log.info("Stock disponible actualizado para el inventario {}", id);
        return toDTO(guardado);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        Inventory inventory = inventoryRepository.findInventarioById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Inventario con ID " + id + " no encontrado"
                ));

        if (inventory.getStockReservado() > 0) {
            throw new InventoryException(
                    "No se puede eliminar el inventario dado que hay productos reservados"
            );
        }

        inventoryRepository.delete(inventory);
        log.info("Inventario {} eliminado", id);
    }

    @Transactional
    @Override
    public InventoryDetalleDTO reservarStock(Long productId, InventoryCantidadDTO cantidadDTO) {
        long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = buscarPorProducto(productId);

        if (inventory.getStockDisponible() < cantidad) {
            throw new InventoryException(
                    "Stock insuficiente. Solicitado: " + cantidad
                            + ", disponible: " + inventory.getStockDisponible()
            );
        }

        inventory.setStockDisponible(inventory.getStockDisponible() - cantidad);
        inventory.setStockReservado(inventory.getStockReservado() + cantidad);

        Inventory guardado = inventoryRepository.save(inventory);
        log.info("Reserva de {} unidades para el producto {}", cantidad, productId);
        return findByID(guardado.getId());
    }

    @Transactional
    @Override
    public InventoryDetalleDTO confirmarVenta(Long productId, InventoryCantidadDTO cantidadDTO) {
        long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = buscarPorProducto(productId);

        if (inventory.getStockReservado() < cantidad) {
            throw new InventoryException(
                    "Error: Se estan intentado confirmar mas productos de los reservados"
            );
        }

        aplicarVenta(inventory, cantidad);

        Inventory guardado = inventoryRepository.save(inventory);
        log.info("Venta de {} unidades confirmada para el producto {}", cantidad, productId);
        return findByID(guardado.getId());
    }

    @Transactional
    @Override
    public InventoryDetalleDTO liberarStock(Long productId, InventoryCantidadDTO cantidadDTO) {
        long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = buscarPorProducto(productId);

        if (cantidad > inventory.getStockReservado()) {
            log.warn("La liberación solicitada supera la reserva del producto {}", productId);
            cantidad = inventory.getStockReservado();
        }

        inventory.setStockReservado(inventory.getStockReservado() - cantidad);
        inventory.setStockDisponible(inventory.getStockDisponible() + cantidad);

        Inventory guardado = inventoryRepository.save(inventory);
        log.info("Liberadas {} unidades del producto {}", cantidad, productId);
        return findByID(guardado.getId());
    }

    @Transactional
    @Override
    public void confirmarStockOrden(Long ordenId, ConfirmacionStockDTO solicitud) {
        cambiarEstadoStockOrden(ordenId, "PENDIENTE_PAGO", "PAGADA", solicitud);
    }

    @Transactional
    @Override
    public void cambiarEstadoStockOrden(
            Long ordenId,
            String origen,
            String destino,
            ConfirmacionStockDTO solicitud
    ) {
        boolean desdeReserva = "PENDIENTE_PAGO".equals(origen)
                && ("PAGADA".equals(destino) || "CANCELADA".equals(destino));
        boolean desdeVenta = "PAGADA".equals(origen)
                && ("PENDIENTE_PAGO".equals(destino) || "CANCELADA".equals(destino));

        if (!desdeReserva && !desdeVenta) {
            throw new InventoryException(
                    "Transición de stock inválida: " + origen + " -> " + destino
            );
        }

        Map<Long, Long> cantidades = new TreeMap<>();
        for (ConfirmacionStockDTO.Linea linea : solicitud.items()) {
            cantidades.merge(linea.productId(), linea.cantidad(), Math::addExact);
        }

        // Tomar los bloqueos por ID evita invertir el orden entre dos compras.
        Map<Long, Inventory> inventarios = new TreeMap<>();
        for (Long productId : cantidades.keySet()) {
            inventarios.put(productId, buscarPorProducto(productId));
        }

        var anterior = operacionStockOrdenRepository.findByOrdenIdAndRevision(
                ordenId, solicitud.revision()
        );

        if (anterior.isPresent()) {
            OperacionStockOrden operacion = anterior.get();
            if (origen.equals(operacion.getEstadoOrigen())
                    && destino.equals(operacion.getEstadoDestino())) {
                return;
            }
            throw new InventoryException(
                    "La revisión de esta orden ya corresponde a otra operación."
            );
        }

        if (desdeReserva) {
            for (var entrada : cantidades.entrySet()) {
                Inventory inventario = inventarios.get(entrada.getKey());
                if (inventario.getStockReservado() < entrada.getValue()) {
                    throw new InventoryException(
                            "Stock reservado insuficiente para el producto " + entrada.getKey()
                    );
                }
            }
        }

        operacionStockOrdenRepository.saveAndFlush(new OperacionStockOrden(
                ordenId, solicitud.revision(), origen, destino
        ));

        for (var entrada : cantidades.entrySet()) {
            Inventory inventario = inventarios.get(entrada.getKey());
            long cantidad = entrada.getValue();

            if ("PAGADA".equals(destino)) {
                // El disponible ya bajó cuando se creó la reserva.
                aplicarVenta(inventario, cantidad);
            } else if ("PENDIENTE_PAGO".equals(destino)) {
                // La venta se revierte, pero las unidades siguen asignadas a la orden.
                inventario.setStockReservado(
                        Math.addExact(inventario.getStockReservado(), cantidad)
                );
                registrarMovimiento(inventario, "REVERSA_VENTA", cantidad);
            } else {
                if (desdeReserva) {
                    inventario.setStockReservado(inventario.getStockReservado() - cantidad);
                }
                inventario.setStockDisponible(
                        Math.addExact(inventario.getStockDisponible(), cantidad)
                );
                registrarMovimiento(
                        inventario,
                        desdeReserva ? "LIBERACION" : "DEVOLUCION",
                        cantidad
                );
            }
        }

        log.info("Stock de orden {} actualizado de {} a {}, revisión {}",
                ordenId, origen, destino, solicitud.revision());
    }

    private Inventory buscarPorProducto(Long productId) {
        return inventoryRepository.findInventarioByProductId(productId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Inventario no encontrado para el producto con id " + productId
                ));
    }

    private void aplicarVenta(Inventory inventory, long cantidad) {
        // El disponible ya bajó al reservar la orden.
        inventory.setStockReservado(inventory.getStockReservado() - cantidad);
        registrarMovimiento(inventory, "VENTA", cantidad);
    }

    private void registrarMovimiento(Inventory inventory, String tipo, Long cantidad) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setProductId(inventory.getProductId());
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setFecha(LocalDateTime.now());
        movimiento.setInventario(inventory);

        inventory.getMovimientos().add(movimiento);
    }

    private InventoryDetalleDTO toDTOSinMovimientos(Inventory inventory) {
        InventoryDetalleDTO dto = new InventoryDetalleDTO();
        dto.setId(inventory.getId());
        dto.setProductId(inventory.getProductId());
        dto.setStockDisponible(inventory.getStockDisponible());
        dto.setStockReservado(inventory.getStockReservado());
        dto.setStockMinimo(inventory.getStockMinimo());
        dto.setUbicacion(inventory.getUbicacion());
        return dto;
    }

    private InventoryDetalleDTO toDTO(Inventory inventory) {
        InventoryDetalleDTO dto = toDTOSinMovimientos(inventory);

        dto.setMovimientosDTO(inventory.getMovimientos().stream().map(movimiento -> {
            MovimientoDetalleDTO detalle = new MovimientoDetalleDTO();
            detalle.setId(movimiento.getId());
            detalle.setProductId(movimiento.getProductId());
            detalle.setTipo(movimiento.getTipo());
            detalle.setCantidad(movimiento.getCantidad());
            detalle.setFecha(movimiento.getFecha());
            return detalle;
        }).toList());

        return dto;
    }
}