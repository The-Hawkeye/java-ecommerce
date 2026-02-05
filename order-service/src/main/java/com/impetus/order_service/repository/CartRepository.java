package com.impetus.order_service.repository;

import com.impetus.order_service.entity.Cart;
import com.impetus.order_service.enums.CartStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserIdAndCartStatus(Long userId, CartStatus status);
}
