package com.impetus.order_service.repository;

import com.impetus.order_service.entity.OrderReservation;
import com.impetus.order_service.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderReservationRepository extends JpaRepository<OrderReservation, Long> {
    List<OrderReservation> findByOrderId(Long orderId);
    List<OrderReservation> findByStatus(ReservationStatus status);
}
