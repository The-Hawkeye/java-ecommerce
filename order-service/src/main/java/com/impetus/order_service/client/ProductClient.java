//package com.impetus.order_service.client;
//
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//import java.time.Instant;
//import java.util.List;
//
//@Component
//public class ProductClient {
//
//    private final WebClient productWebClient;
//
//    public ProductClient(WebClient productWebClient) {
//        this.productWebClient = productWebClient;
//    }
//
//    public Mono<ProductSnapshot> getProductSnapshot(String productId) {
//        return productWebClient.get()
//                .uri("/api/products/{id}/snapshot", productId)
//                .accept(MediaType.APPLICATION_JSON)
//                .retrieve()
//                .bodyToMono(ProductSnapshot.class);
//    }
//
//    public Mono<ReservationResponse> reserveInventory(String orderNumber, List<ReservationItem> items) {
//        return productWebClient.post()
//                .uri("/api/reservations")
//                .contentType(MediaType.APPLICATION_JSON)
//                .bodyValue(new ReservationRequest(orderNumber, items))
//                .retrieve()
//                .bodyToMono(ReservationResponse.class);
//    }
//
//    // DTOs for product service interaction
//    public static class ProductSnapshot {
//        private String productId;
//        private String sku;
//        private String name;
//        private Integer unitPrice;
//        private Integer availableQuantity;
//        // getters/setters...
//    }
//
//    public static class ReservationItem {
//        private String productId;
//        private Integer quantity;
//
//        // ctor/getters/setters...
//        public ReservationItem(String productId, Integer quantity) {
//            this.productId = productId;
//            this.quantity = quantity;
//        }
//    }
//
//    public static class ReservationRequest {
//        private String orderNumber;
//        private List<ReservationItem> items;
//
//        public ReservationRequest(String orderNumber, List<ReservationItem> items) {
//            this.orderNumber = orderNumber;
//            this.items = items;
//        }
//        // getters/setters...
//    }
//
//    public static class ReservationResponse {
//        private Instant expiresAt;
//        private List<ReservationItem> items;
//        // getters/setters...
//    }
//}
