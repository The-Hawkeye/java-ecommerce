package com.impetus.order_service.enums;

public enum OrderStatus {
    CREATED,
    PENDING_PAYMENT,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED
}
