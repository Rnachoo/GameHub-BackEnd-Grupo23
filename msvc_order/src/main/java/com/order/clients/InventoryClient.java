package com.order.clients;

import com.order.models.dtos.InventoryCantidadDTO;
import com.order.models.dtos.InventoryDTO;
import com.order.models.dtos.MovimientoStockOrdenDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "msvc-inventory",
        url = "http://localhost:8005/api/v1/inventories"
)
public interface InventoryClient {

    @PutMapping("/producto/{productId}/reservar")
    InventoryDTO reservarStock(
            @PathVariable("productId") Long productId,
            @RequestBody InventoryCantidadDTO cantidadDTO
    );

    @PutMapping("/producto/{productId}/liberar")
    InventoryDTO liberarStock(
            @PathVariable("productId") Long productId,
            @RequestBody InventoryCantidadDTO cantidadDTO
    );

    @PutMapping("/ordenes/{ordenId}/confirmacion")
    void confirmarStockOrden(
            @PathVariable("ordenId") Long ordenId,
            @RequestBody MovimientoStockOrdenDTO solicitud
    );

    @PutMapping("/ordenes/{ordenId}/transiciones/{origen}/{destino}")
    void cambiarEstadoStockOrden(
            @PathVariable("ordenId") Long ordenId,
            @PathVariable("origen") String origen,
            @PathVariable("destino") String destino,
            @RequestBody MovimientoStockOrdenDTO solicitud
    );
}