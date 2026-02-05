package com.impetus.order_service.dto;

import com.impetus.order_service.enums.PaymentMode;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    @NotNull
    private Long shippingAddressId;

    @NotNull
    private PaymentMode paymentMode;
}
