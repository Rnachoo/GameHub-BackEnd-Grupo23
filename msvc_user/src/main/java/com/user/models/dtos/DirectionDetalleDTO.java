package com.user.models.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class DirectionDetalleDTO {
    @NotBlank(message = "La comuna es obligatoria")
    private String comuna;
    @NotBlank(message = "La ciudad es obligatoria")
    private String ciudad;
    @NotBlank(message = "La calle es obligatoria")
    private String calle;
    @NotBlank(message = "El número es obligatorio")
    private String numero;
}