package com.impetus.order_service.repository;

import com.impetus.order_service.entity.Order;
import com.impetus.order_service.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);

    Long countByUserIdAndStatus(Long userId, OrderStatus status);
}
