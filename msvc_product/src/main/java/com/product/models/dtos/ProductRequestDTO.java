package com.product.models.dtos;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ProductRequestDTO {

    @NotBlank(message = "El nombre del producto es obligatorio y no puede estar vacío.")
    private String nombre;

    @NotBlank(message = "La marca del producto es obligatoria.")
    private String marca;

    private String modelo;

    @NotNull(message = "El precio es obligatorio.")
    @Positive(message = "El precio debe ser un número mayor que cero.")
    private Long precio;

    @Positive(message = "El precio con descuento debe ser mayor que cero.")
    private Long precioDescuento;

    private Boolean destacado;

    @NotNull(message = "El ID de la categoria es obligatorio.")
    private Long categoriaId;

    @Size(max = 500, message = "La descripción no puede superar los 500 caracteres.")
    private String descripcion;

    private List<String> imagenes = new ArrayList<>();

    private List<String> especificaciones = new ArrayList<>();
}
