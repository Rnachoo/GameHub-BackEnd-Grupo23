package com.inventory.repositories;

import com.inventory.models.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findByProductId(Long productoId);

    boolean existsByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findInventarioByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Inventory> findInventarioById(Long id);
}