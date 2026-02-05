package com.impetus.order_service.controller;

import com.impetus.order_service.dto.AddCartItemRequest;
import com.impetus.order_service.dto.CartResponse;
import com.impetus.order_service.dto.UpdateCartItemRequest;
import com.impetus.order_service.response.ApiResponse;
import com.impetus.order_service.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);
    private final CartService cartService;

    private Long getCurrentUserId(HttpServletRequest request){
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        if(userId == null){
            log.error("Gateway failed to assign X-User-Id header");
            throw new BadRequestException("Header missing");
        }
        log.info("UserId: "+userId);
        log.info("Roles from Header : "+ getCurrentUserRoles(request));
        return userId;
    }

    private String getCurrentUserRoles(HttpServletRequest request){
        String roles =  request.getHeader("X-User-Roles");
        if(roles == null){
            log.error("Gateway failed to assign X-User-Roles header");
            throw new BadRequestException("Header missing");
        }
        return roles;
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(@RequestBody AddCartItemRequest addItemreq, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        CartResponse res = cartService.addItem(userId, addItemreq);
        return ResponseEntity.ok(new ApiResponse<>("Cart updated Successfully", res));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        CartResponse res = cartService.getCurrentCart(userId);
        return ResponseEntity.ok(new ApiResponse<>("Cart fetched successfully", res));
    }

    @PostMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<CartResponse>> updateItem(@PathVariable String itemId, @Valid @RequestBody UpdateCartItemRequest req, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        CartResponse res = cartService.updateItem(userId, itemId, req);
        return ResponseEntity.ok(new ApiResponse<>("Cart updated Successfully", res));
    }

    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long itemId, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(new ApiResponse<>("Item removed successfully", null));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        cartService.clearCart(userId);
        return ResponseEntity.ok(new ApiResponse<>("Cart cleared", null));
    }
}
