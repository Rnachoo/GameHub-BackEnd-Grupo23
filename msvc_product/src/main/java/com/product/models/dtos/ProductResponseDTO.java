package com.product.models.dtos;

import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ProductResponseDTO {
    private Long id;
    private String nombre;
    private String marca;
    private String modelo;
    private Long precio;
    private Long precioDescuento;
    private Boolean destacado;
    private Long categoriaId;
    private String descripcion;
    private String estado;
    private List<String> imagenes;
    private List<String> especificaciones;
}
