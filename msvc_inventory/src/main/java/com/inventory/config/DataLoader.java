package com.inventory.config;

import com.inventory.models.Inventory;
import com.inventory.models.MovimientoInventario;
import com.inventory.repositories.InventoryRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
public class DataLoader implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    public DataLoader(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args) {
        if (inventoryRepository.count() > 0) {
            return;
        }
        crear(1L, 12L);
        crear(2L, 4L);
        crear(3L, 8L);
        crear(4L, 10L);
        crear(5L, 3L);
        crear(6L, 7L);
        crear(7L, 0L);
        log.info("Seed de inventario cargado: {} registros", inventoryRepository.count());
    }

    private void crear(Long productId, Long stock) {
        Inventory inventory = new Inventory();
        inventory.setProductId(productId);
        inventory.setStockDisponible(stock);
        inventory.setStockReservado(0L);
        inventory.setStockMinimo(2L);
        inventory.setUbicacion("Bodega Santiago");

        MovimientoInventario entrada = new MovimientoInventario();
        entrada.setProductId(productId);
        entrada.setTipo("ENTRADA");
        entrada.setCantidad(stock);
        entrada.setFecha(LocalDateTime.now());
        entrada.setInventario(inventory);
        inventory.getMovimientos().add(entrada);

        inventoryRepository.save(inventory);
    }
}
