package com.impetus.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class UpdateInventoryResponse {
    private boolean success;
    private List<FailedItem> failedItems;

    @Data
    @AllArgsConstructor
    public static class FailedItem {
        private String productId;
        private String reason;
        private int availableQuantity;
    }
}
