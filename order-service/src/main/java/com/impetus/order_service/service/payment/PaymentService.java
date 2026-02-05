package com.impetus.order_service.service.payment;

import com.impetus.order_service.dto.OrderResponse;
import com.impetus.order_service.dto.PaymentInitiationResponse;

public interface PaymentService {
    PaymentInitiationResponse initiatePayment(Long orderId);
    OrderResponse handlePaymentSuccess(Long orderId, Long paymentReference);
    void handlePaymentFailure(Long orderId);
    Void initiateRefund(Long orderId, Integer amountInPaisa);
}
