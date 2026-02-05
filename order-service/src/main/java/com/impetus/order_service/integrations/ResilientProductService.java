package com.impetus.order_service.integrations;

import com.impetus.order_service.dto.ProductResponseDto;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResilientProductService {
    private final ProductClient productClient;

    private final Map<String, ProductResponseDto> productsCache = new ConcurrentHashMap<>();

    @CircuitBreaker(name = "product-service", fallbackMethod = "productFallback")
    @Retry(name = "product-service")
    @Bulkhead(name = "product-service")
    @RateLimiter(name = "product-service")
    public List<ProductResponseDto> getProducts(List<String> productIds){
        List<ProductResponseDto> productResponseDtoList = productClient.fetchProducts(productIds);
        return productResponseDtoList;
    }

    private List<ProductResponseDto> productFallback(List<String> productIds, Throwable ex){
        log.error("Using fallback of Product Client to fetch products", ex);
        return productIds.stream()
                .map(productsCache::get)
                .filter(Objects::nonNull)
                .toList();
    }
}



//The below code is replaced by this class
//private List<ProductResponseDto> fetchProducts(List<String> productIds){
//    WebClient client = webClient.baseUrl(productBaseUrl).build();
//    List<ProductResponseDto> productResponseDtoList = client.post()
//            .uri("/detailsOfIds")
//            .bodyValue(productIds)
//            .retrieve()
//            .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> {
//                return clientResponse.createException().flatMap(ex->{
//                    return Mono.error(new RuntimeException("One more products not avai;able"));
//                });
//            })
//            .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> {
//                throw new RuntimeException("Unable to fetch products");
//            })
//            .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<ProductResponseDto>>>() {})
//            .map(resp -> {
//                if (resp == null || resp.getData() == null) {
//                    throw new RuntimeException("Products response was empty");
//                }
//                return resp.getData();
//            })
//            .block();
//
//    if(productResponseDtoList == null){
//        throw new RuntimeException("Address not foung");
//    }
//    return productResponseDtoList;
//}