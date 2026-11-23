package com.inventory.repositories;

import com.inventory.models.OperacionStockOrden;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OperacionStockOrdenRepository
        extends JpaRepository<OperacionStockOrden, Long> {

    Optional<OperacionStockOrden> findByOrdenIdAndRevision(
            Long ordenId,
            Long revision
    );
}