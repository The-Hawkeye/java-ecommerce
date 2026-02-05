package com.impetus.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.order_service.dto.OrderRequest;
import com.impetus.order_service.dto.OrderResponse;
import com.impetus.order_service.enums.PaymentMode;
import com.impetus.order_service.service.OrderService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;


@WebMvcTest(OrderController.class)
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class OrderControllerUnitTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;

    // --------------------------
    // POST /order
    // --------------------------

    @Test
    void createOrderFromCart_shouldReturn200_andInvokeService() throws Exception {
        Long userId = 101L;

        OrderRequest req = new OrderRequest();
        req.setShippingAddressId(123L);               // <-- required
        req.setPaymentMode(PaymentMode.UPI);          // <-- required (use any valid enum)

        OrderResponse response = new OrderResponse();
        when(orderService.createOrderFromCart(eq(userId), any(OrderRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/order")
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, "ROLE_USER")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order fetched successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(orderService, times(1)).createOrderFromCart(eq(userId), any(OrderRequest.class));
    }


    // --------------------------
    // GET /order/{orderId}
    // --------------------------
    @Test
    void getOrder_shouldReturn200_andInvokeService() throws Exception {
        Long userId = 42L;
        Long orderId = 555L;

        OrderResponse response = new OrderResponse();
        when(orderService.getOrder(eq(orderId), eq(userId))).thenReturn(response);

        mockMvc.perform(
                        get("/order/{orderId}", orderId)
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, "ROLE_USER")
                )
                .andExpect(status().isOk())
                // Note: message has a typo in controller: "duccessfully"
                .andExpect(jsonPath("$.message").value("Order fetched duccessfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(orderService, times(1)).getOrder(eq(orderId), eq(userId));
    }

    // --------------------------
    // GET /order?page=&size=
    // --------------------------
    @Test
    void listOrders_shouldReturn200_andInvokeService() throws Exception {
        Long userId = 777L;
        int page = 1;
        int size = 2;

        Page<OrderResponse> pageResp = new PageImpl<>(
                List.of(new OrderResponse(), new OrderResponse()),
                PageRequest.of(page, size),
                5
        );

        when(orderService.listOrder(eq(userId), eq(page), eq(size))).thenReturn(pageResp);

        mockMvc.perform(
                        get("/order")
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, "ROLE_USER")
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Orders fetched Successfully"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(5));

        verify(orderService, times(1)).listOrder(eq(userId), eq(page), eq(size));
    }

    // --------------------------
    // GET /order/listOrdersOfUser/{userId}?page=&size=
    // --------------------------
    @Test
    void listOrdersOfUser_shouldReturn200_andInvokeService() throws Exception {
        Long userIdPath = 999L;
        int page = 0;
        int size = 3;

        Page<OrderResponse> pageResp = new PageImpl<>(
                List.of(new OrderResponse(), new OrderResponse(), new OrderResponse()),
                PageRequest.of(page, size),
                3
        );

        when(orderService.listOrder(eq(userIdPath), eq(page), eq(size))).thenReturn(pageResp);

        mockMvc.perform(
                        get("/order/listOrdersOfUser/{userId}", userIdPath)
                                .header(USER_ID_HEADER, "123") // current controller does not validate consistency
                                .header(ROLES_HEADER, "ROLE_ADMIN")
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Orders fetched Successfully"))
                .andExpect(jsonPath("$.data.content.length()").value(3));

        verify(orderService, times(1)).listOrder(eq(userIdPath), eq(page), eq(size));
    }

    // --------------------------
    // GET /order/listAllOrders?page=&size=
    // --------------------------
    @Test
    void listAllOrders_shouldReturn200_andInvokeService() throws Exception {
        int page = 2;
        int size = 10;

        Page<OrderResponse> pageResp = new PageImpl<>(
                List.of(new OrderResponse()),
                PageRequest.of(page, size),
                21
        );

        when(orderService.listAllOrders(eq(page), eq(size))).thenReturn(pageResp);

        mockMvc.perform(
                        get("/order/listAllOrders")
                                .header(USER_ID_HEADER, "321")
                                .header(ROLES_HEADER, "ROLE_ADMIN")
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Orders fetched Successfully"))
                .andExpect(jsonPath("$.data.totalElements").value(21));

        verify(orderService, times(1)).listAllOrders(eq(page), eq(size));
    }

    // =======================================================
    // Header Precondition tests (direct invocation)
    // =======================================================

    @Test
    void getOrder_shouldThrowBadRequest_whenRolesHeaderMissing() {
        // Directly instantiate controller for unit-level test (bypassing MockMvc)
        OrderController controller = new OrderController();
        // Inject service via field (since in your code it's @Autowired)
        controller.orderService = orderService;

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(USER_ID_HEADER, "42");
        // Missing roles header

        // getCurrentUserRoles should throw BadRequestException
        Assertions.assertThrows(RuntimeException.class, () -> controller.getOrder(1L, req));
        verifyNoInteractions(orderService);
    }

    @Test
    void createOrder_shouldThrowNumberFormat_whenUserIdHeaderMissing() {
        OrderController controller = new OrderController();
        controller.orderService = orderService;

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(ROLES_HEADER, "ROLE_USER");
        // Missing X-User-Id → Long.valueOf(null) -> NumberFormatException

        Assertions.assertThrows(NumberFormatException.class, () -> controller.createOrderFromCart(new OrderRequest(), req));
        verifyNoInteractions(orderService);
    }

    @Test
    void listOrders_shouldThrowNumberFormat_whenUserIdNotNumeric() {
        OrderController controller = new OrderController();
        controller.orderService = orderService;

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(USER_ID_HEADER, "abc");
        req.addHeader(ROLES_HEADER, "ROLE_USER");

        Assertions.assertThrows(NumberFormatException.class, () -> controller.listOrders(0, 20, req));
        verifyNoInteractions(orderService);
    }
}
