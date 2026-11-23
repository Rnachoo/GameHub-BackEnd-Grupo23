package com.inventory.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ConfirmacionStockDTO(
        @NotNull @Positive Long revision,
        @NotEmpty List<@NotNull @Valid Linea> items
) {
    public record Linea(
            @NotNull @Positive Long productId,
            @NotNull @Positive Long cantidad
    ) {}
}