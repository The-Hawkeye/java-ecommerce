package com.impetus.order_service.service.payment;

import com.impetus.order_service.enums.PaymentStatus;

public record PaymentCallbackResult(PaymentStatus status, String providerPaymentId, String message) {}
