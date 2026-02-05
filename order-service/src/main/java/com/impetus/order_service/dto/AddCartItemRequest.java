package com.impetus.order_service.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class AddCartItemRequest {
    @NotBlank
    public String productId;

    @Min(1)
    public Integer quantity;
}
