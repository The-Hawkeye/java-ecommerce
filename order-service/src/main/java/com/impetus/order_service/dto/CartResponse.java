package com.impetus.order_service.dto;

import com.impetus.order_service.entity.CartItem;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class CartResponse {
    private Long cartId;
    private Long userId;
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CartItemResponse> items;
}
