package com.impetus.order_service.dto;

import com.impetus.order_service.entity.OrderItem;
import com.impetus.order_service.entity.ShippingAddress;
import com.impetus.order_service.enums.OrderStatus;
import com.impetus.order_service.enums.PaymentMode;
import com.impetus.order_service.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class OrderResponse {
    private Long id;
    private Long userId;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMode paymentMode;
    private Integer totalAmount;
    private Instant placedAt;
    private ShippingAddress shippingAddress;
    private List<OrderItemResponse> items;
}
