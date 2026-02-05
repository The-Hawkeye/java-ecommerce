package com.impetus.product_service.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductSearchRequest {
    public String query;
    public String name;
    public BigDecimal minPrice;
    public BigDecimal maxPrice;
    public Integer page = 0;
    public Integer size = 20;
}
