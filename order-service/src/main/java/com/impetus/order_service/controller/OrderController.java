package com.impetus.order_service.controller;

import com.impetus.order_service.dto.OrderRequest;
import com.impetus.order_service.dto.OrderResponse;
import com.impetus.order_service.response.ApiResponse;
import com.impetus.order_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    OrderService orderService;

    private Long getCurrentUserId(HttpServletRequest request){
        Long userId = Long.valueOf(request.getHeader("X-User-Id"));
        if(userId == null || userId == 0){
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

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrderFromCart(@Valid @RequestBody OrderRequest orderRequest, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        OrderResponse response = orderService.createOrderFromCart(userId, orderRequest);
        return ResponseEntity.ok(new ApiResponse<>("Order fetched successfully", response));
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long orderId, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        OrderResponse response = orderService.getOrder(orderId, userId);
        return ResponseEntity.ok(new ApiResponse<>("Order fetched duccessfully", response));
    }

    @GetMapping()
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request){
        Long userId = getCurrentUserId(request);
        Page<OrderResponse> orders = orderService.listOrder(userId, page, size);
        return ResponseEntity.ok(new ApiResponse<>("Orders fetched Successfully", orders));
    }

    @GetMapping("/listOrdersOfUser/{userId}")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listOrdersOfUser(@PathVariable Long userId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request){
        Page<OrderResponse> orders = orderService.listOrder(userId, page, size);
        return ResponseEntity.ok(new ApiResponse<>("Orders fetched Successfully", orders));
    }


    @GetMapping("/listAllOrders")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> listAllOrders(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size, HttpServletRequest request){
        Page<OrderResponse> orders = orderService.listAllOrders(page, size);
        return ResponseEntity.ok(new ApiResponse<>("Orders fetched Successfully", orders));
    }
}
