package com.impetus.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaymentInitiationResponse {
    private String paymentId;
    private Long expiresInSecond;
}
