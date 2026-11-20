package com.promotion.models.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class PromotionSaveDTO {
    @NotBlank(message = "El codigo de la promocion es obligatorio")
    private String codigo;

    @Positive(message = "El valor debe ser mayor que cero")
    private double valor;

    @NotBlank(message = "El tipo de promocion es obligatorio")
    private String tipo;

    private LocalDateTime fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDateTime fechaFin;

    @PositiveOrZero(message = "El monto mínimo no puede ser negativo")
    private double montoMinimo;

    @NotNull(message = "Los usos máximos son obligatorios")
    @Min(value = 1, message = "Los usos máximos deben ser al menos 1")
    private Long usosMaximos;

    @Positive(message = "El tope debe ser mayor que cero")
    private Double tope;

    private String estado;

    private Long categoryId;
}
