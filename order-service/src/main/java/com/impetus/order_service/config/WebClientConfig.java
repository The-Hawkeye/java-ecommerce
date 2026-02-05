package com.impetus.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {

//    @Bean
//    @LoadBalanced
//    public WebClient.Builder webClient(){
//        return WebClient.builder();
//    }


    @Bean
    public WebClient productWebClient(
            @Value("${services.product.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter(propagateGatewayHeaders())
                .build();
    }

    @Bean
    public WebClient userWebClient(@Value("${services.user.base-url}") String baseUrl) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .filter(propagateGatewayHeaders())   // ensure X-user-Id and X-user-Roles forwarded
                .build();
    }



    private ExchangeFilterFunction propagateGatewayHeaders() {
        // Note: In a real app, capture headers from Reactor context via ServerWebExchange
        // or use Spring Cloud Gateway's forward headers. For simplicity, you can inject
        // these via a RequestHeaderInterceptor at controller/service boundary.
        return ExchangeFilterFunction.ofRequestProcessor(Mono::just);
    }


}
