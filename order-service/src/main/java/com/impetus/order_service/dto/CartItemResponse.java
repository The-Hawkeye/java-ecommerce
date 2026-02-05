package com.impetus.order_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class CartItemResponse {
    private Long itemId;
    private String productId;
    private Integer quantity;
    private Instant addedAt;
}
