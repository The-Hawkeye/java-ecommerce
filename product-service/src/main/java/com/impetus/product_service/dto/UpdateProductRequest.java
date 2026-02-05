package com.impetus.product_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
public class UpdateProductRequest {
    public String name;
    public String description;
    public BigDecimal price;
    public Integer inventoryQuantity;
    public Map<String, Object> attributes;
}
