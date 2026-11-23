package com.inventory.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "operaciones_stock_orden",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_stock_orden_revision",
                columnNames = {"orden_id", "revision"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperacionStockOrden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden_id", nullable = false, updatable = false)
    private Long ordenId;

    @Column(nullable = false, updatable = false)
    private Long revision;

    @Column(name = "estado_origen", nullable = false, updatable = false)
    private String estadoOrigen;

    @Column(name = "estado_destino", nullable = false, updatable = false)
    private String estadoDestino;

    public OperacionStockOrden(
            Long ordenId,
            Long revision,
            String estadoOrigen,
            String estadoDestino
    ) {
        this.ordenId = ordenId;
        this.revision = revision;
        this.estadoOrigen = estadoOrigen;
        this.estadoDestino = estadoDestino;
    }
}