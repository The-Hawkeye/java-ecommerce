package com.impetus.order_service.service.payment;

public record PaymentInitResponse(
        String redirectUrl,
        String providerOrderId,
        String yourOrderId
) {}

