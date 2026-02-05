package com.impetus.order_service.integrations;

import com.impetus.order_service.dto.AddressResponse;
import com.impetus.order_service.exception.BadRequestException;
import com.impetus.order_service.exception.InternalServerError;
import com.impetus.order_service.exception.NotFoundException;
import com.impetus.order_service.exception.ServiceUnavailableException;
import com.impetus.order_service.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class UserClient {
//
//    private final WebClient.Builder webClientBuilder;
//    @Value("${services.user.base-url}")
//    private String userServiceBaseUrl;
//
//    public AddressResponse fetchUserAddress(Long userId, Long addressId){
//        log.info("Fetching address for User : {} and addressId : {}", userId, addressId);
//        WebClient client = webClientBuilder.baseUrl(userServiceBaseUrl).build();
//        return client.get()
//                .uri("/addresses/{addressId}", addressId)
//                .header("X-User-Id", String.valueOf(userId))
//                .header("X-User-Roles", "USER")
//                .retrieve()
//                .onStatus(
//
//                        HttpStatusCode::is4xxClientError,
//                        clientResponse -> clientResponse.bodyToMono(String.class)
//                                .defaultIfEmpty("") // in case server returns empty body
//                                .flatMap(body -> {
//                                    HttpStatusCode statusCode = clientResponse.statusCode();
//                                    if (statusCode == HttpStatus.NOT_FOUND) {
//                                        // 404 → NotFoundException
//                                        return Mono.error(new RuntimeException(
//                                                "Address not found for userId=" + userId + ", addressId=" + addressId
//                                                        + (body.isBlank() ? "" : " | body: " + body)));
//                                    }
//                                    // other 4xx → BadRequestException
//                                    return Mono.error(new RuntimeException(
//                                            "Invalid address selected | status=" + statusCode
//                                                    + (body.isBlank() ? "" : " | body: " + body)));
//                                })
//
//                )
//                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
//                    log.error("UserService returned 5xx for userId: {}, addressId: {}", userId, addressId);
//                    throw new RuntimeException("Unable to fetch Address");
//                })
//                .bodyToMono(new ParameterizedTypeReference<ApiResponse<AddressResponse>>() {})
//                .map(resp -> {
//                    if (resp == null || resp.getData() == null) {
//                        throw new RuntimeException("Products response was empty");
//                    }
//                    return resp.getData();
//                })
//                .block();
//    }
//}



@Component
@RequiredArgsConstructor
@Slf4j
public class UserClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;

    public AddressResponse fetchUserAddress(Long userId, Long addressId) {
        log.info("Fetching address for User : {} and addressId : {}", userId, addressId);

        WebClient client = webClientBuilder.baseUrl(userServiceBaseUrl).build();

        try {
            return client.get()
                    .uri("/addresses/{addressId}", addressId)
                    .header("X-User-Id", String.valueOf(userId))
                    .header("X-User-Roles", "USER")
                    .retrieve()
                    // Handle 4xx
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        HttpStatusCode statusCode = clientResponse.statusCode();
                                        if (statusCode == HttpStatus.NOT_FOUND) {
                                            // Not Found → NotFoundException (or your custom)
                                            return Mono.error(new NotFoundException(
                                                    "Address not found for userId=" + userId + ", addressId=" + addressId +
                                                            (body.isBlank() ? "" : " | body: " + body)
                                            ));
                                        }
                                        // Other 4xx → BadRequestException (or your custom)
                                        return Mono.error(new BadRequestException(
                                                "Invalid address selected | status=" + statusCode +
                                                        (body.isBlank() ? "" : " | body: " + body)
                                        ));
                                    })
                    )
                    // Handle 5xx
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        log.error("UserService returned 5xx for userId: {}, addressId: {}, body: {}",
                                                userId, addressId, body);
                                        return Mono.error(new ServiceUnavailableException(
                                                "User service is currently unavailable. Please try again later."
                                        ));
                                    })
                    )
                    // Convert body to ApiResponse<AddressResponse>
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<AddressResponse>>() {})
                    // Optional: timeout
                    // .timeout(Duration.ofSeconds(3))
                    // Map empty data to a controlled error
                    .map(resp -> {
                        if (resp == null || resp.getData() == null) {
                            throw new InternalServerError("Address response was empty");
                        }
                        return resp.getData();
                    })
                    // Handle network-level errors (connection refused, timeouts, etc.)
                    .onErrorMap(WebClientRequestException.class, ex -> {
                        // Connection refused / DNS / Timeout etc.
                        log.error("UserService network error for userId={}, addressId={}: {}",
                                userId, addressId, ex.getMessage(), ex);

                        // You can refine messages by inspecting ex.getCause()
                        if (ex.getCause() instanceof java.net.ConnectException) {
                            return new ServiceUnavailableException(
                                    "User service is currently unavailable. Please try again later."
                            );
                        }
                        if (ex.getCause() instanceof java.net.SocketTimeoutException) {
                            return new ServiceUnavailableException(
                                    "User service timed out. Please try again later."
                            );
                        }
                        // Generic network failure
                        return new ServiceUnavailableException(
                                "Unable to reach user service. Please try again later."
                        );
                    })
                    // Optionally retry a couple of times with backoff
                    // .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                    //         .filter(ex -> ex instanceof ServiceUnavailableException))
                    .block();

        } catch (NotFoundException e) {
            // 404 mapped to graceful domain error
            throw e;
        } catch (BadRequestException e) {
            // 4xx mapped to graceful domain error
            throw e;
        } catch (ServiceUnavailableException e) {
            // Network/5xx mapped to graceful domain error
            throw e;
        } catch (InternalServerError e) {
            // Controlled internal errors
            throw e;
        } catch (Exception e) {
            // Fallback: wrap any unexpected exception in a gentle message
            log.error("Unexpected error while fetching address for userId={}, addressId={}", userId, addressId, e);
            throw new InternalServerError("Something went wrong while fetching the address. Please try again later.");
        }
    }
}



//private AddressResponse fetchUserAddress(Long userId, Long addressId){
//    log.info("Fetching address : "+addressId+ " for user : "+userId);
//
//    WebClient client = webClient.baseUrl(userServiceBaseUrl).build();
//
//    AddressResponse addressResponse = client.get()
//            .uri("/addresses/{addressId}", addressId)
//            .header("X-User-Id", String.valueOf(userId))
//            .header("X-User-Roles", "USER")
//            .retrieve()
//            .onStatus(
//
//                    HttpStatusCode::is4xxClientError,
//                    clientResponse -> clientResponse.bodyToMono(String.class)
//                            .defaultIfEmpty("") // in case server returns empty body
//                            .flatMap(body -> {
//                                HttpStatusCode statusCode = clientResponse.statusCode();
//                                if (statusCode == HttpStatus.NOT_FOUND) {
//                                    // 404 → NotFoundException
//                                    return Mono.error(new RuntimeException(
//                                            "Address not found for userId=" + userId + ", addressId=" + addressId
//                                                    + (body.isBlank() ? "" : " | body: " + body)));
//                                }
//                                // other 4xx → BadRequestException
//                                return Mono.error(new RuntimeException(
//                                        "Invalid address selected | status=" + statusCode
//                                                + (body.isBlank() ? "" : " | body: " + body)));
//                            })
//
//            )
//            .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
//                log.error("UserService returned 5xx for userId: {}, addressId: {}", userId, addressId);
//                throw new RuntimeException("Unable to fetch Address");
//            })
//            .bodyToMono(new ParameterizedTypeReference<ApiResponse<AddressResponse>>() {})
//            .map(resp -> {
//                if (resp == null || resp.getData() == null) {
//                    throw new RuntimeException("Products response was empty");
//                }
//                return resp.getData();
//            })
//            .block();
//
//    if(addressResponse == null){
//        throw new RuntimeException("Address not foung");
//    }
//
//    return addressResponse;
//}