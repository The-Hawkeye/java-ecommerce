package com.impetus.product_service.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductRequestDto {
    private String name;
    private String description;
    private BigDecimal price;
}
