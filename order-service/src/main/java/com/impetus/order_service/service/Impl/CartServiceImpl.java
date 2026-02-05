package com.impetus.order_service.service.Impl;

import com.impetus.order_service.dto.AddCartItemRequest;
import com.impetus.order_service.dto.CartItemResponse;
import com.impetus.order_service.dto.CartResponse;
import com.impetus.order_service.dto.UpdateCartItemRequest;
import com.impetus.order_service.entity.Cart;
import com.impetus.order_service.entity.CartItem;
import com.impetus.order_service.enums.CartStatus;
import com.impetus.order_service.repository.CartItemRepository;
import com.impetus.order_service.repository.CartRepository;
import com.impetus.order_service.service.CartService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private Cart getOrCreateActiveCart(Long userId){
        return cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE)
                .orElseGet(()->{
                    Cart c = new Cart();
                    c.setUserId(userId);
                    c.setCartStatus(CartStatus.ACTIVE);
                    c.setCreatedAt(Instant.now());
                    c.setUpdatedAt(Instant.now());
                    return cartRepository.save(c);
                });
    }

    private CartResponse mapToResponse(Cart cart){
        CartResponse res = new CartResponse();
        res.setCartId(cart.getId());
        res.setUserId(cart.getUserId());
        res.setStatus(cart.getCartStatus().name());
        res.setCreatedAt(cart.getCreatedAt());
        res.setUpdatedAt(cart.getUpdatedAt());

        List<CartItemResponse> items = cart.getItems().stream()
                .map(item -> {
                    CartItemResponse ir = new CartItemResponse();
                    ir.setItemId(item.getId());
                    ir.setProductId(item.getProductId());
                    ir.setQuantity(item.getQuantity());
                    ir.setAddedAt(item.getAddedAt());
                    return ir;
                }).toList();

        res.setItems(items);
        return res;
    }

    @Override
    public CartResponse getCurrentCart(Long userId) {
        Cart cart = cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE).orElseGet(()->{
            Cart empty = new Cart();
            empty.setUserId(userId);
            empty.setCartStatus(CartStatus.ACTIVE);
            empty.setCreatedAt(Instant.now());
            empty.setUpdatedAt(Instant.now());
            empty.setItems(List.of());
            return cartRepository.save(empty);
        });

        return mapToResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest req) {
        Cart cart = getOrCreateActiveCart(userId);

        //If item already exists increase its qty;
        CartItem existing = cart.getItems().stream()
                .filter(it-> it.getProductId().equals(req.productId))
                .findFirst()
                .orElse(null);

        if(existing != null){
            existing.setQuantity(existing.getQuantity()+ req.quantity);
        }else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductId(req.productId);
            item.setQuantity(req.quantity);
            item.setAddedAt(Instant.now());
            cart.addItem(item);
        }

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    @Override
    public CartResponse updateItem(Long userId, String itemId, UpdateCartItemRequest req) {
        Cart cart = cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE).orElseThrow(()->{
            return new NoSuchElementException("No active Cat found");
        });

        CartItem item = cart.getItems().stream()
                .filter(ci->ci.getProductId().equals(itemId))
                .findFirst()
                .orElseThrow(()-> new NoSuchElementException("Cart item not found"));

        if(req.quantity != null){
            if(req.quantity <= 0){
                cart.removeItem(item);
            }else {
                item.setQuantity(req.quantity);
            }
        }

        cart.setUpdatedAt(Instant.now());
        Cart saved = cartRepository.save(cart);
        return mapToResponse(saved);
    }

    @Override
    public void removeItem(Long userId, Long itemId) {
        Cart cart = cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE)
                .orElseThrow(()-> new NoSuchElementException("Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(ci-> ci.getId().equals(itemId))
                .findFirst()
                .orElseThrow(()-> new NoSuchElementException("Item Not Found"));

        cart.removeItem(item);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE)
                .orElse(null);

        if(cart == null) return;

        cart.getItems().clear();
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }
}
