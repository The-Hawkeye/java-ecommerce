package com.impetus.order_service.service.payment;

import com.impetus.order_service.enums.PaymentStatus;

public record WebhookVerifyResult(boolean valid, PaymentStatus status, String providerPaymentId, String message) {}
