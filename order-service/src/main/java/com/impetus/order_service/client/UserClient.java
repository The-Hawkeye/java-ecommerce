//package com.impetus.order_service.client;
//
//import lombok.Getter;
//import lombok.Setter;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//@Component
//public class UserClient {
//
//    private final WebClient userWebClient;
//
//    public UserClient(WebClient userWebClient) {
//        this.userWebClient = userWebClient;
//    }
//
//    public Mono<UserAddressDto> getAddressById(Long addressId) {
//        return userWebClient.get()
//                .uri("/api/users/addresses/{id}", addressId)
//                .accept(MediaType.APPLICATION_JSON)
//                .retrieve()
//                .bodyToMono(UserAddressDto.class);
//    }
//
//    /** Shape returned by User Service */
//    @Getter
//    @Setter
//    public static class UserAddressDto {
//        private Long id;
//        private Long userId;
//        private String contactName;
//        private String phone;
//        private String addressLabel;
//        private String addressLine1;
//        private String addressLine2;
//        private String locality;
//        private String city;
//        private String state;
//        private String pincode;
//    }
//}
//
