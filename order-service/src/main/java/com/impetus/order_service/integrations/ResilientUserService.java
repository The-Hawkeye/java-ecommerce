package com.impetus.order_service.integrations;

import com.impetus.order_service.dto.AddressResponse;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResilientUserService {
    private final UserClient userClient;

    @Retry(name = "order-service")
    public AddressResponse fetchUserAddress(Long userId, Long addressId){
        return userClient.fetchUserAddress(userId, addressId);
    }
}
