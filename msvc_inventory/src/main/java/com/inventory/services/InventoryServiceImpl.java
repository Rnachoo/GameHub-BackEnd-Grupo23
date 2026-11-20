package com.inventory.services;

import com.inventory.clients.ProductClient;
import com.inventory.exceptions.InventoryException;
import com.inventory.exceptions.RecursoNoEncontradoException;
import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.models.dtos.*;
import com.inventory.repositories.InventoryRepository;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    @Autowired
    private InventoryRepository inventoryRepository;
    @Autowired
    private ProductClient productClient;

    @Transactional(readOnly = true)
    @Override
    public List<InventoryDetalleDTO> findAll() {
        log.info("Listando el stock de todos los productos");
        return this.inventoryRepository.findAll().stream().map(this::toDTOSinMovimientos).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<InventoryDetalleDTO> findAllByProduct(Long productId) {
        log.info("Listando Inventario registrado por producto");
        return this.inventoryRepository.findByProductId(productId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public InventoryDetalleDTO findByID(Long id) {
        return toDTO(buscar(id));
    }

    @Transactional
    @Override
    public InventoryDetalleDTO save(InventorySaveDTO inventorySaveDTO) {
        try {
            productClient.getProductById(inventorySaveDTO.getProductId());
        } catch (FeignException e) {
            log.error("Error al consultar msvc-product para el producto con ID " + inventorySaveDTO.getProductId());
            throw new InventoryException("El producto con ID " + inventorySaveDTO.getProductId() + " no existe o el servicio no está disponible. No se puede crear el inventario.");
        }
        if (this.inventoryRepository.existsByProductId(inventorySaveDTO.getProductId())) {
            throw new InventoryException("El producto con ID " + inventorySaveDTO.getProductId() + " ya tiene inventario registrado.");
        }

        log.info("Creando nuevo inventario para producto con id " + inventorySaveDTO.getProductId());
        Inventory inventory = new Inventory();
        inventory.setProductId(inventorySaveDTO.getProductId());
        inventory.setStockDisponible(inventorySaveDTO.getStockDisponible());
        inventory.setStockReservado(inventorySaveDTO.getStockReservado());
        inventory.setStockMinimo(inventorySaveDTO.getStockMinimo());
        inventory.setUbicacion(inventorySaveDTO.getUbicacion());

        List<MovimientoDetalleDTO> movimientosDTO = inventorySaveDTO.getMovimientosDTO();
        if (movimientosDTO == null || movimientosDTO.isEmpty()) {
            registrarMovimiento(inventory, "ENTRADA", inventory.getStockDisponible());
        } else {
            movimientosDTO.forEach(mov -> registrarMovimiento(inventory, mov.getTipo(), mov.getCantidad()));
        }

        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Inventario con id " + inventorySave.getId() + " ha sido guardado con exito");
        return toDTO(inventorySave);
    }

    @Transactional
    @Override
    public InventoryDetalleDTO updateCantidadDisponible(Long id, InventoryUpdateCantidadDisponibleDTO cantidadDisponibleDTO) {
        Inventory inventory = this.inventoryRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Inventario no encontrado, no se puede actualizar el stock"));

        if (cantidadDisponibleDTO.getStockDisponible() < 0) {
            log.error("Intento de actualizar stock a un valor negativo en el inventario ID " + id);
            throw new InventoryException("El stock disponible no puede quedar en negativo.");
        }

        long diferencia = cantidadDisponibleDTO.getStockDisponible() - inventory.getStockDisponible();
        inventory.setStockDisponible(cantidadDisponibleDTO.getStockDisponible());
        if (cantidadDisponibleDTO.getStockMinimo() != null) {
            inventory.setStockMinimo(cantidadDisponibleDTO.getStockMinimo());
        }
        if (cantidadDisponibleDTO.getUbicacion() != null && !cantidadDisponibleDTO.getUbicacion().isBlank()) {
            inventory.setUbicacion(cantidadDisponibleDTO.getUbicacion());
        }
        if (diferencia != 0) {
            registrarMovimiento(inventory, diferencia > 0 ? "AJUSTE_ENTRADA" : "AJUSTE_SALIDA", Math.abs(diferencia));
        }

        inventory = this.inventoryRepository.save(inventory);
        log.info("Cantidad del stock actualizado con exito");
        return toDTO(inventory);
    }

    @Transactional
    @Override
    public void deleteById(Long id) {
        Inventory inventory = buscar(id);
        if (inventory.getStockReservado() > 0) {
            log.error("Intento de eliminación rechazado, el inventario con id " + id + " tiene stock reservado");
            throw new InventoryException("No se puede eliminar el inventario dado que hay productos reservados");
        }
        inventoryRepository.delete(inventory);
        log.info("Inventario eliminado con exito!");
    }

    @Transactional
    @Override
    public InventoryDetalleDTO reservarStock(Long productId, InventoryCantidadDTO cantidadDTO) {
        Long cantidad = cantidadDTO.getCantidad();
        Inventory inventory = buscarPorProducto(productId);

        if (inventory.getStockDisponible() < cantidad) {
            throw new InventoryException("Stock insuficiente. Solicitado: " + cantidad + ", disponible: " + inventory.getStockDisponible());
        }

        inventory.setStockDisponible(inventory.getStockDisponible() - cantidad);
        inventory.setStockReservado(inventory.getStockReservado() + cantidad);

        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Reserva exitosa para el producto ID " + productId);
        return this.findByID(inventorySave.getId());
    }

    @Transactional
    @Override
    public InventoryDetalleDTO confirmarVenta(Long productId, InventoryCantidadDTO cantidadDTO) {
        Long cantidad = cantidadDTO.getCantidad();
        log.info("Confirmando venta de unidades: " + cantidad + " de producto con id " + productId);
        Inventory inventory = buscarPorProducto(productId);

        if (inventory.getStockReservado() < cantidad) {
            throw new InventoryException("Error: Se estan intentado confirmar mas productos de los reservados");
        }

        inventory.setStockReservado(inventory.getStockReservado() - cantidad);
        registrarMovimiento(inventory, "VENTA", cantidad);
        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Venta confirmada para el producto con ID " + productId);
        return this.findByID(inventorySave.getId());
    }

    @Transactional
    @Override
    public InventoryDetalleDTO liberarStock(Long productId, InventoryCantidadDTO cantidadDTO) {
        Long cantidad = cantidadDTO.getCantidad();
        log.info("Liberando reserva de " + cantidad + " del producto con id " + productId);
        Inventory inventory = buscarPorProducto(productId);

        if (inventory.getStockReservado() < cantidad) {
            log.warn("Se intenta liberar mas stock del que hay reservado. Ajustando a la cantidad maxima en reserva.");
            cantidad = inventory.getStockReservado();
        }

        inventory.setStockReservado(inventory.getStockReservado() - cantidad);
        inventory.setStockDisponible(inventory.getStockDisponible() + cantidad);

        Inventory inventorySave = this.inventoryRepository.save(inventory);
        log.info("Stock liberado con exito para el producto ID " + productId);
        return this.findByID(inventorySave.getId());
    }

    private Inventory buscar(Long id) {
        return this.inventoryRepository.findById(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Inventario con ID " + id + " no encontrado"));
    }

    private Inventory buscarPorProducto(Long productId) {
        return this.inventoryRepository.findByProductId(productId).stream().findFirst().orElseThrow(
                () -> new RecursoNoEncontradoException("Inventario no encontrado para el producto con id " + productId));
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
        dto.setStockMinimo(inventory.getStockMinimo());
        dto.setStockReservado(inventory.getStockReservado());
        dto.setUbicacion(inventory.getUbicacion());
        return dto;
    }

    private InventoryDetalleDTO toDTO(Inventory inventory) {
        InventoryDetalleDTO dto = toDTOSinMovimientos(inventory);
        dto.setMovimientosDTO(inventory.getMovimientos().stream().map(mov -> {
            MovimientoDetalleDTO movDTO = new MovimientoDetalleDTO();
            movDTO.setId(mov.getId());
            movDTO.setProductId(mov.getProductId());
            movDTO.setTipo(mov.getTipo());
            movDTO.setCantidad(mov.getCantidad());
            movDTO.setFecha(mov.getFecha());
            return movDTO;
        }).toList());
        return dto;
    }
}
