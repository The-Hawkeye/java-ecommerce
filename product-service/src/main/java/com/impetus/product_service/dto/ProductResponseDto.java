package com.impetus.product_service.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductResponseDto {
    private String id;
    private String name;
    private String description;
    private String sku;
    private BigDecimal price;
    private Integer inventoryQuantity;
    private Map<String, Object> attributes;

}
