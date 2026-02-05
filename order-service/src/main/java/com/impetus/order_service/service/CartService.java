package com.impetus.order_service.service;

import com.impetus.order_service.dto.AddCartItemRequest;
import com.impetus.order_service.dto.CartResponse;
import com.impetus.order_service.dto.UpdateCartItemRequest;

public interface CartService {

    CartResponse getCurrentCart(Long userId);

    CartResponse addItem(Long userId, AddCartItemRequest req);

    CartResponse updateItem(Long userId, String itemId, UpdateCartItemRequest req);

    void removeItem(Long userId, Long itemId);

    void clearCart(Long userId);
}
