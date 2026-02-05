package com.impetus.product_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateProductRequest {
    @NotBlank
    public String name;
    public String description;
    @NotBlank
    public String sku;
    @NotNull
    public BigDecimal price;
    public Integer inventoryQuantity = 0;
    public Map<String, Object> attributes;
}
