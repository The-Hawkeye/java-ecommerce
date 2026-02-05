package com.impetus.order_service.dto;

import jakarta.validation.constraints.Min;

public class UpdateCartItemRequest {
    @Min(0)
    public Integer quantity;
}
