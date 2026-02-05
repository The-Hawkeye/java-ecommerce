package com.impetus.order_service.service.payment;


import com.impetus.order_service.dto.OrderResponse;
import com.impetus.order_service.dto.PaymentInitiationResponse;
import com.impetus.order_service.entity.Order;
import com.impetus.order_service.entity.Payment;
import com.impetus.order_service.enums.PaymentGatewayType;

import java.util.Map;

public interface PaymentGateway {
    PaymentGatewayType type();

    // Create provider order (or simulate) & return redirectUrl for hosted checkout
    PaymentInitiationResponse initiatePayment(Order order, Payment payment);

    // Parse success/failure "return" (informational; not the source of truth)
    OrderResponse handleReturn(Long orderId, Map<String, String> queryParams);

    // Verify webhook payload (source of truth), update DB
    OrderResponse handleWebhook(Long orderId, String rawBody, Map<String, String> headers);

    // Initiate refund for a payment
    boolean initiateRefund(Payment payment, Integer amountPaisa);
}

