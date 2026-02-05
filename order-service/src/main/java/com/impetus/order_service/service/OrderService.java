package com.impetus.order_service.service;

import com.impetus.order_service.dto.OrderRequest;
import com.impetus.order_service.dto.OrderResponse;
import com.impetus.order_service.dto.UpdateInventoryRequest;
import com.impetus.order_service.dto.UpdateInventoryResponse;
import com.impetus.order_service.entity.Order;
import org.springframework.data.domain.Page;

import java.util.List;

public interface OrderService {
    OrderResponse createOrderFromCart(Long userId, OrderRequest req);
    OrderResponse getOrder(Long orderId, Long userId);
    Page<OrderResponse> listOrder(Long userId, int page, int size);
    OrderResponse getOrder(Long orderId);
    UpdateInventoryResponse updateInventory(Order order);

    //For Admins, hence no userId Required
    Page<OrderResponse> listAllOrders(int page, int size);
//    Page<OrderResponse> listOrderOfUser(Long userId, int page, int size);
}
