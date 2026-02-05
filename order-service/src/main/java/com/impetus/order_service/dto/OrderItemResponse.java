package com.impetus.order_service.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemResponse {
    private Long id;
    private String productId;
    private String productSku;
    private String productName;
    private Integer unitPrice;
    private Integer quantity;
    private Integer totalPrice;
}
