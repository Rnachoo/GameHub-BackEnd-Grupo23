package com.inventory.models.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class InventoryUpdateCantidadDisponibleDTO {

    @NotNull(message = "El stock disponible es obligatorio")
    @Min(value = 0, message = "El stock disponible no puede ser negativo")
    private Long stockDisponible;

    private Long stockReservado;

    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Long stockMinimo;

    private String ubicacion;
}
