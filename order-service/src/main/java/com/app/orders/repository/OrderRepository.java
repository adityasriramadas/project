package com.app.orders.repository;

import com.app.orders.repository.entity.Order;
import com.app.orders.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/**
 * @author Aditya Sriramadas
 * @since 2026-07-04
 */
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    Optional<Order> findByIdempotencyKey(String idempotencyKey);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
}


