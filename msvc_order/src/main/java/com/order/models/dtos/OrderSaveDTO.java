package com.order.models.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class OrderSaveDTO {
    @NotNull(message = "El ID del usuario es obligatorio")
    private Long userId;

    private String codigoPromocion;

    @NotEmpty(message = "La orden debe tener al menos un producto")
    private List<@Valid OrderSaveItemDTO> items;
}
