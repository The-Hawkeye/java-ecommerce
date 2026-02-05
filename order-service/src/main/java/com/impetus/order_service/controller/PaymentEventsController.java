package com.impetus.order_service.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/razorpay/payment-events")
public class PaymentEventsController {

    @GetMapping
    public ResponseEntity<Void> check(){
        return ResponseEntity.ok(null);
    }

    @PostMapping
    public ResponseEntity<Void> updateEvents(HttpServletRequest request){
//        Boolean captured = request.getBody().get("payload").get("payment").get("entity").get("status");

        boolean captured = true;
        if(captured){
//            mark the reservations as fullfillled
//            Extract payment Id and Order Id from notes which needs to passed while creating payment order in paymentGateway
//            From that order fetch all reservations and mark them
        }
        return ResponseEntity.ok(null);

    }
}
