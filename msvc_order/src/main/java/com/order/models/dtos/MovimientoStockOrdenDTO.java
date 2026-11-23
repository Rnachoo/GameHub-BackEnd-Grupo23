package com.order.models.dtos;

import java.util.List;

public record MovimientoStockOrdenDTO(
        Long revision,
        List<Linea> items
) {
    public record Linea(Long productId, Long cantidad) {}
}