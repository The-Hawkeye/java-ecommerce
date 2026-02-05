package com.impetus.order_service.controller;

import com.impetus.order_service.dto.PaymentInitiationResponse;
import com.impetus.order_service.service.payment.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/{orderId}")
    public ResponseEntity<?> initiatePayment(@PathVariable Long orderId, @RequestHeader("X-User-Id") Long userIdHeader){
        PaymentInitiationResponse res = paymentService.initiatePayment(orderId);
        return ResponseEntity.ok(res);
    }

}


