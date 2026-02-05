package com.impetus.order_service.integrations;


import com.impetus.order_service.dto.ProductResponseDto;
import com.impetus.order_service.exception.BadRequestException;
import com.impetus.order_service.exception.InternalServerError;
import com.impetus.order_service.exception.NotFoundException;
import com.impetus.order_service.exception.ServiceUnavailableException;
import com.impetus.order_service.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.util.List;

//@Component
//@RequiredArgsConstructor
//@Slf4j
//public class ProductClient {
//
//    private final WebClient.Builder webClientBuilder;
//
//    public List<ProductResponseDto> fetchProducts(List<String> productIds){
//        log.info("Fetching details of products with Ids {}", productIds.toString());
//        return webClientBuilder.build()
//                .post()
//                .uri("http://localhost:8082/product/detailsOfIds")
//                .bodyValue(productIds)
//                .retrieve()
//                .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ProductResponseDto>>>() {})
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
public class ProductClient {

    private final WebClient.Builder webClientBuilder;

    public List<ProductResponseDto> fetchProducts(List<String> productIds) {
        log.info("Fetching details of products with Ids {}", productIds);

        WebClient client = webClientBuilder.build();

        try {
            return client.post()
                    .uri("http://localhost:8082/product/detailsOfIds")
                    .bodyValue(productIds)
                    .retrieve()
                    // Handle 4xx responses
                    .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        HttpStatusCode statusCode = clientResponse.statusCode();
                                        if (statusCode == HttpStatus.NOT_FOUND) {
                                            return Mono.error(new NotFoundException(
                                                    "Some products were not found for the provided IDs."
                                                            + (body.isBlank() ? "" : " | details: " + body)
                                            ));
                                        }
                                        return Mono.error(new BadRequestException(
                                                "Invalid product IDs provided."
                                                        + " | status=" + statusCode
                                                        + (body.isBlank() ? "" : " | details: " + body)
                                        ));
                                    })
                    )
                    // Handle 5xx responses
                    .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .flatMap(body -> {
                                        log.error("ProductService returned 5xx. body={}", body);
                                        return Mono.error(new ServiceUnavailableException(
                                                "Product service is currently unavailable. Please try again later."
                                        ));
                                    })
                    )
                    // Deserialize to ApiResponse<List<ProductResponseDto>>
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ProductResponseDto>>>() {})
                    // Optional: timeout if service hangs
                    // .timeout(Duration.ofSeconds(3))
                    // Validate response shape
                    .map(resp -> {
                        if (resp == null || resp.getData() == null) {
                            throw new InternalServerError("Products response was empty");
                        }
                        return resp.getData();
                    })
                    // Handle network-level exceptions (connection refused, DNS, timeouts)
                    .onErrorMap(WebClientRequestException.class, ex -> {
                        log.error("Network error calling ProductService: {}", ex.getMessage(), ex);

                        Throwable cause = ex.getCause();
                        if (cause instanceof java.net.ConnectException) {
                            return new ServiceUnavailableException(
                                    "Product service is currently unavailable. Please try again later."
                            );
                        }
                        if (cause instanceof java.net.SocketTimeoutException) {
                            return new ServiceUnavailableException(
                                    "Product service timed out. Please try again later."
                            );
                        }
                        return new ServiceUnavailableException(
                                "Unable to reach product service. Please try again later."
                        );
                    })
                    // Optional: retry transient failures (e.g., ServiceUnavailableException)
                    // .retryWhen(Retry.backoff(2, Duration.ofMillis(200))
                    //         .filter(ex -> ex instanceof ServiceUnavailableException))
                    .block();

        } catch (NotFoundException | BadRequestException | ServiceUnavailableException | InternalServerError e) {
            // Re-throw controlled exceptions to be handled by @ControllerAdvice
            throw e;
        } catch (Exception e) {
            // Fallback: wrap unexpected errors
            log.error("Unexpected error fetching products for ids={}", productIds, e);
            throw new InternalServerError("Something went wrong while fetching product details. Please try again later.");
        }
    }
}