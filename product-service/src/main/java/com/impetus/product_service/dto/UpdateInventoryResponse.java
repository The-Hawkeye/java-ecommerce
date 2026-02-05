package com.impetus.product_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class UpdateInventoryResponse {
    private boolean success;
    private List<FailedItem> failedItems;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FailedItem {
        private String productId;
        private String reason;
        private int availableQuantity;
    }
}
