package com.impetus.product_service.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Document()
public class Product {
    @Id
    private String id;

    @NotBlank
    @Indexed
    private String name;

    @TextIndexed(weight = 2)
    private String description;

    @NotBlank
    private String sku;

    @NotNull
    private BigDecimal price;

    @NotNull
    private Integer inventoryQuantity;

    private Map<String, Object> attributes = new HashMap<>();

    private Instant createdAt = Instant.now();
    private Instant updatedAt = Instant.now();
}
