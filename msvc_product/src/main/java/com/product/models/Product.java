package com.product.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Productos")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String marca;

    private String modelo;

    @Column(nullable = false)
    private Long precio;

    private Long precioDescuento;

    @Column(nullable = false)
    private Boolean destacado = false;

    @Column(name = "categoria_id", nullable = false)
    private Long categoriaId;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private String estado;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_imagenes", joinColumns = @JoinColumn(name = "producto_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "url", length = 600)
    @ToString.Exclude
    private List<String> imagenes = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "producto_especificaciones", joinColumns = @JoinColumn(name = "producto_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "detalle", length = 300)
    @ToString.Exclude
    private List<String> especificaciones = new ArrayList<>();
}
