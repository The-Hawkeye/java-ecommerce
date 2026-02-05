package com.impetus.order_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.impetus.order_service.dto.AddCartItemRequest;
import com.impetus.order_service.dto.CartResponse;
import com.impetus.order_service.dto.UpdateCartItemRequest;
import com.impetus.order_service.service.CartService;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(CartController.class)
@ExtendWith(MockitoExtension.class)
class CartControllerUnitTest {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String ROLES_HEADER = "X-User-Roles";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    private Long userId;
    private String roles;

    @BeforeEach
    void init() {
        userId = 42L;
        roles = "ROLE_USER";
    }

    // --------------------------
    // Happy-path: /cart/add
    // --------------------------
    @Test
    void addItem_shouldReturn200_andInvokeService() throws Exception {
        AddCartItemRequest req = new AddCartItemRequest();
        // Set fields as per your DTO (example)
        // req.setProductId("P-100");
        // req.setQuantity(2);

        CartResponse response = new CartResponse(); // fill if needed
        when(cartService.addItem(eq(userId), any(AddCartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/cart/add")
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, roles)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                // ApiResponse structure: { "message": "...", "data": {...} }
                .andExpect(jsonPath("$.message").value("Cart updated Successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(cartService, times(1)).addItem(eq(userId), any(AddCartItemRequest.class));
    }

    // --------------------------
    // Happy-path: GET /cart
    // --------------------------
    @Test
    void getCart_shouldReturn200_andInvokeService() throws Exception {
        CartResponse response = new CartResponse(); // fill if needed
        when(cartService.getCurrentCart(eq(userId))).thenReturn(response);

        mockMvc.perform(
                        get("/cart")
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, roles)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart fetched successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(cartService, times(1)).getCurrentCart(eq(userId));
    }

    // --------------------------
    // Happy-path: POST /cart/item/{itemId}
    // --------------------------
    @Test
    void updateItem_shouldReturn200_andInvokeService() throws Exception {
        String itemId = "ITEM-123";
        UpdateCartItemRequest req = new UpdateCartItemRequest();
        // req.setQuantity(3);
        // req.setNote("update");

        CartResponse response = new CartResponse();
        when(cartService.updateItem(eq(userId), eq(itemId), any(UpdateCartItemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(
                        post("/cart/item/{itemId}", itemId)
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, roles)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart updated Successfully"))
                .andExpect(jsonPath("$.data").exists());

        verify(cartService, times(1)).updateItem(eq(userId), eq(itemId), any(UpdateCartItemRequest.class));
    }

    // --------------------------
    // Happy-path: DELETE /cart/item/{itemId}
    // --------------------------
    @Test
    void removeItem_shouldReturn200_andInvokeService() throws Exception {
        Long itemId = 999L;

        doNothing().when(cartService).removeItem(eq(userId), eq(itemId));

        mockMvc.perform(
                        delete("/cart/item/{itemId}", itemId)
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, roles)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Item removed successfully"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(cartService, times(1)).removeItem(eq(userId), eq(itemId));
    }

    // --------------------------
    // Happy-path: DELETE /cart
    // --------------------------
    @Test
    void clearCart_shouldReturn200_andInvokeService() throws Exception {
        doNothing().when(cartService).clearCart(eq(userId));

        mockMvc.perform(
                        delete("/cart")
                                .header(USER_ID_HEADER, String.valueOf(userId))
                                .header(ROLES_HEADER, roles)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart cleared"))
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(cartService, times(1)).clearCart(eq(userId));
    }

    // =======================================================
    // Header Precondition tests (direct invocation):
    // Verify exceptions thrown by precondition methods
    // =======================================================

    @Test
    void getCart_shouldThrowBadRequest_whenRolesHeaderMissing() {
        CartController controller = new CartController(cartService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(USER_ID_HEADER, String.valueOf(userId));
        // roles missing

        Assertions.assertThrows(RuntimeException.class, () -> controller.getCart(req));
        verifyNoInteractions(cartService);
    }

    @Test
    void getCart_shouldThrowNumberFormat_whenUserIdHeaderMissing() {
        CartController controller = new CartController(cartService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        // user id missing
        req.addHeader(ROLES_HEADER, roles);

        // Long.valueOf(null) -> NumberFormatException in current code
        Assertions.assertThrows(NumberFormatException.class, () -> controller.getCart(req));
        verifyNoInteractions(cartService);
    }

    @Test
    void getCart_shouldThrowNumberFormat_whenUserIdNotNumeric() {
        CartController controller = new CartController(cartService);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader(USER_ID_HEADER, "abc");
        req.addHeader(ROLES_HEADER, roles);

        Assertions.assertThrows(NumberFormatException.class, () -> controller.getCart(req));
        verifyNoInteractions(cartService);
    }
}
