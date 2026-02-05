package com.impetus.order_service.service.payment;

public record GatewayUrls(
        String initialUrl,   // where you send the user to start payment (hosted checkout)
        String successUrl,   // your app’s success callback (redirect target)
        String failureUrl,   // your app’s failure callback (redirect target)
        String webhookUrl    // your public webhook endpoint the gateway calls
) {}
