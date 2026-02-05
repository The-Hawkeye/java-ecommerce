//package com.impetus.order_service.service.payment;
//
//
//import com.impetus.order_service.dto.OrderResponse;
//import com.impetus.order_service.dto.PaymentInitiationResponse;
//import com.impetus.order_service.entity.Order;
//import com.impetus.order_service.entity.Payment;
//import com.impetus.order_service.enums.PaymentGatewayType;
//import com.impetus.order_service.enums.PaymentStatus;
//import io.swagger.v3.core.util.Json;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//
//
//@Component
//public class RazorPayGateway implements PaymentGateway {
//
//    @Override
//    public PaymentGatewayType type() { return PaymentGatewayType.RAZORPAY; }
//
//    @Override
//    public PaymentInitiationResponse initiatePayment(Order order, Payment payment) {
//        // Simulate "creating provider order" by setting a fake id
//        String fakeProviderOrderId = "dummy_order_" + payment.getId();
//        payment.setProviderOrderId(fakeProviderOrderId);
//        payment.setStatus(PaymentStatus.INITIATED);
//
//        // Simulated hosted checkout URL — your frontend will redirect here
//        String redirectUrl = "http://localhost:8084/dummy-checkout?paymentId=" + payment.getId()
//                + "&orderId=" + order.getId()
//                + "&amount=" + order.getTotalAmount();
//
//        return new PaymentInitiationResponse(
//                redirectUrl,
//                payment.getId(),
//                String.valueOf(order.getId()),
//                "DUMMY"
//        );
//    }
//
//    @Override
//    public OrderResponse handleReturn(Long orderId, Map<String, String> queryParams) {
//        // NOT authoritative. For a dummy, we can accept a "status" param.
//        String status = queryParams.getOrDefault("status", "failed");
//        PaymentStatus mapped = "success".equalsIgnoreCase(status) ? PaymentStatus.CAPTURED : PaymentStatus.FAILED;
//        return new OrderResponse();
////        return new OrderResponse(orderId, mapped, null, null, "Return handled (non-authoritative)");
//    }
//
//    @Override
//    public OrderResponse handleWebhook(Long orderId, String rawBody, Map<String, String> headers) {
//        // In a real gateway: verify signature. Here we simulate as always valid.
//        // Suppose rawBody contains {"status":"captured"} for success.
//        boolean captured = rawBody.contains("\"status\":\"captured\"");
//        PaymentStatus mapped = captured ? PaymentStatus.CAPTURED : PaymentStatus.FAILED;
//        return  new OrderResponse();
////        return new OrderResponse(orderId, mapped, null, null, "Webhook handled (simulated)");
//    }
//
//    @Override
//    public boolean initiateRefund(Payment payment, Integer amountPaisa) {
//        // Simulate refund success
////        payment.setStatus(PaymentStatus.REFUNDED);
//        return true;
//    }
//}
