package com.order.repositories;

import com.order.models.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserId(Long userId);

    List<Order> findByEstado(String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Order> findOrdenById(Long id);
}