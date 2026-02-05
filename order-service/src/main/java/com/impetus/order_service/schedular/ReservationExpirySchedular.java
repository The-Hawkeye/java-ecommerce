package com.impetus.order_service.schedular;

import com.impetus.order_service.entity.OrderReservation;
import com.impetus.order_service.enums.OrderStatus;
import com.impetus.order_service.enums.PaymentStatus;
import com.impetus.order_service.enums.ReservationStatus;
import com.impetus.order_service.repository.OrderRepository;
import com.impetus.order_service.repository.OrderReservationRepository;
import com.impetus.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReservationExpirySchedular {
    private static final Logger log = LoggerFactory.getLogger(ReservationExpirySchedular.class);
    private final OrderRepository orderRepository;
    private final OrderReservationRepository orderReservationRepository;

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void expireOldOrders(){
        Instant cutOff = Instant.now().minus(Duration.ofMinutes(2));
        log.info("Cron job executed at {}", LocalDateTime.now());
        List<OrderReservation> old = orderReservationRepository.findByStatus(ReservationStatus.PENDING)
                .stream()
                .filter(r->r.getCreatedAt().isBefore(cutOff))
                .toList();

        if(old.isEmpty()) return;
        log.info("Found {} expired reservations to process", old.size());
        Map<Long, List<OrderReservation>> byOrder = old.stream()
                .collect(Collectors.groupingBy(r -> r.getOrder().getId()));

        for (Map.Entry<Long, List<OrderReservation>> e : byOrder.entrySet()){
            long orderId = e.getKey();
            List<OrderReservation> reservations = e.getValue();
            for(OrderReservation r : reservations){
                r.setStatus(ReservationStatus.EXPIRED);
            }
            orderReservationRepository.saveAll(reservations);
            log.info("OrderId {}", orderId);
            orderRepository.findById(orderId).ifPresent(order -> {
                if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
                    order.setStatus(OrderStatus.CANCELLED);
                    order.setPaymentStatus(PaymentStatus.FAILED);
                    order.setCancelledAt(Instant.now());
                    orderRepository.save(order);
                    log.info("Order {} cancelled due to reservation expiry");
                }
            });
        }
    }
}
