package com.impetus.order_service.service;

import com.impetus.order_service.dto.AddCartItemRequest;
import com.impetus.order_service.dto.UpdateCartItemRequest;
import com.impetus.order_service.dto.CartResponse;
import com.impetus.order_service.dto.CartItemResponse;
import com.impetus.order_service.entity.Cart;
import com.impetus.order_service.entity.CartItem;
import com.impetus.order_service.enums.CartStatus;
import com.impetus.order_service.repository.CartRepository;
import com.impetus.order_service.repository.CartItemRepository;

import com.impetus.order_service.service.Impl.CartServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceImplUnitTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    private CartServiceImpl service;

    @Captor
    private ArgumentCaptor<Cart> cartCaptor;

    private final Long userId = 42L;

    @BeforeEach
    void setUp() {
        service = new CartServiceImpl(cartRepository, cartItemRepository);
    }

    // ----------------------------
    // Helpers
    // ----------------------------
    private Cart newCart(Long id, Long userId, CartStatus status) {
        Cart c = new Cart();
        c.setId(id);
        c.setUserId(userId);
        c.setCartStatus(status);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        // Ensure items is initialized to avoid NPE
        if (c.getItems() == null) {
            c.setItems(new ArrayList<>());
        }
        return c;
    }

    private CartItem newItem(Long id, String productId, int qty) {
        CartItem ci = new CartItem();
        ci.setId(id);
        ci.setProductId(productId);
        ci.setQuantity(qty);
        ci.setAddedAt(Instant.now());
        return ci;
    }

    // ==========================================================
    // getCurrentCart
    // ==========================================================

    @Test
    void getCurrentCart_shouldReturnExistingActiveCart() {
        Cart existing = newCart(1L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(11L, "P-100", 2));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));

        CartResponse res = service.getCurrentCart(userId);

        assertEquals(1L, res.getCartId());
        assertEquals(userId, res.getUserId());
        assertEquals(CartStatus.ACTIVE.name(), res.getStatus());
        assertEquals(1, res.getItems().size());
        assertEquals("P-100", res.getItems().get(0).getProductId());
        assertEquals(2, res.getItems().get(0).getQuantity());

        verify(cartRepository, never()).save(any(Cart.class)); // no creation path
    }

    @Test
    void getCurrentCart_shouldCreateWhenMissing() {
        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        Cart created = newCart(10L, userId, CartStatus.ACTIVE);
        created.setItems(List.of()); // service sets empty immutable list for new cart
        when(cartRepository.save(any(Cart.class))).thenReturn(created);

        CartResponse res = service.getCurrentCart(userId);

        assertEquals(10L, res.getCartId());
        assertEquals(userId, res.getUserId());
        assertEquals(CartStatus.ACTIVE.name(), res.getStatus());
        assertNotNull(res.getCreatedAt());
        assertNotNull(res.getUpdatedAt());
        assertTrue(res.getItems().isEmpty());

        verify(cartRepository, times(1)).save(any(Cart.class));
    }

    // ==========================================================
    // addItem
    // ==========================================================

    @Test
    void addItem_shouldCreateCartAndAddNewItem_whenCartMissing() {
        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        Cart created = newCart(100L, userId, CartStatus.ACTIVE);
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddCartItemRequest req = new AddCartItemRequest();
        // If DTO uses setters, swap to setProductId / setQuantity
        req.productId = "P-200";
        req.quantity = 3;

        CartResponse res = service.addItem(userId, req);

        assertEquals(userId, res.getUserId());
        assertEquals(CartStatus.ACTIVE.name(), res.getStatus());
        assertEquals(1, res.getItems().size());
        assertEquals("P-200", res.getItems().get(0).getProductId());
        assertEquals(3, res.getItems().get(0).getQuantity());

        verify(cartRepository, atLeastOnce()).save(cartCaptor.capture());
        Cart saved = cartCaptor.getValue();
        assertEquals(1, saved.getItems().size());
        assertEquals("P-200", saved.getItems().get(0).getProductId());
        assertEquals(3, saved.getItems().get(0).getQuantity());
    }

    @Test
    void addItem_shouldIncreaseQuantity_whenItemAlreadyExists() {
        Cart existing = newCart(200L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(21L, "P-300", 2));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddCartItemRequest req = new AddCartItemRequest();
        req.productId = "P-300";
        req.quantity = 5;

        CartResponse res = service.addItem(userId, req);

        assertEquals(1, res.getItems().size());
        assertEquals("P-300", res.getItems().get(0).getProductId());
        assertEquals(7, res.getItems().get(0).getQuantity()); // 2 + 5

        verify(cartRepository).save(cartCaptor.capture());
        Cart saved = cartCaptor.getValue();
        assertEquals(7, saved.getItems().get(0).getQuantity());
        assertNotNull(saved.getUpdatedAt());
    }

    // ==========================================================
    // updateItem
    // ==========================================================
    // NOTE: updateItem matches item by PRODUCT ID (String), not by itemId.
    // Your controller passes {itemId} but service compares ci.getProductId().equals(itemId).

    @Test
    void updateItem_shouldUpdateQuantity_whenPositive() {
        Cart existing = newCart(300L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(31L, "P-400", 2));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.quantity = 10; // set qty to 10

        CartResponse res = service.updateItem(userId, "P-400", req);

        assertEquals(1, res.getItems().size());
        assertEquals(10, res.getItems().get(0).getQuantity());

        verify(cartRepository).save(cartCaptor.capture());
        Cart saved = cartCaptor.getValue();
        assertEquals(10, saved.getItems().get(0).getQuantity());
    }

    @Test
    void updateItem_shouldRemoveItem_whenQuantityZeroOrLess() {
        Cart existing = newCart(301L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(32L, "P-500", 5));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.quantity = 0; // <= 0 triggers removal

        CartResponse res = service.updateItem(userId, "P-500", req);

        assertTrue(res.getItems().isEmpty());

        verify(cartRepository).save(cartCaptor.capture());
        Cart saved = cartCaptor.getValue();
        assertTrue(saved.getItems().isEmpty());
    }

    @Test
    void updateItem_shouldDoNothing_whenQuantityIsNull() {
        Cart existing = newCart(302L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(33L, "P-600", 7));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.quantity = null;

        CartResponse res = service.updateItem(userId, "P-600", req);

        assertEquals(1, res.getItems().size());
        assertEquals(7, res.getItems().get(0).getQuantity()); // unchanged

        verify(cartRepository).save(any(Cart.class)); // still saved (updatedAt modified)
    }

    @Test
    void updateItem_shouldThrow_whenActiveCartMissing() {
        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.quantity = 1;

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.updateItem(userId, "P-777", req));

        assertEquals("No active Cat found", ex.getMessage()); // message from service
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateItem_shouldThrow_whenItemNotFound() {
        Cart existing = newCart(303L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(34L, "P-800", 3));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));

        UpdateCartItemRequest req = new UpdateCartItemRequest();
        req.quantity = 9;

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.updateItem(userId, "P-NOT-FOUND", req));
        assertEquals("Cart item not found", ex.getMessage());
    }

    // ==========================================================
    // removeItem
    // ==========================================================

    @Test
    void removeItem_shouldRemoveByItemId() {
        Cart existing = newCart(400L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(41L, "P-900", 4));
        existing.getItems().add(newItem(42L, "P-901", 1));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        service.removeItem(userId, 41L);

        verify(cartRepository).save(cartCaptor.capture());
        Cart saved = cartCaptor.getValue();
        assertEquals(1, saved.getItems().size());
        assertEquals(42L, saved.getItems().get(0).getId());
    }

    @Test
    void removeItem_shouldThrow_whenCartMissing() {
        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.removeItem(userId, 99L));

        assertEquals("Cart not found", ex.getMessage());
    }

    @Test
    void removeItem_shouldThrow_whenItemMissing() {
        Cart existing = newCart(401L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(43L, "P-902", 2));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.removeItem(userId, 999L));

        assertEquals("Item Not Found", ex.getMessage());
        verify(cartRepository, never()).save(any(Cart.class));
    }

    // ==========================================================
    // clearCart
    // ==========================================================

    @Test
    void clearCart_shouldClearItems_whenCartExists() {
        Cart existing = newCart(500L, userId, CartStatus.ACTIVE);
        existing.getItems().add(newItem(51L, "P-1000", 5));
        existing.getItems().add(newItem(52L, "P-1001", 1));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        service.clearCart(userId);

        verify(cartRepository).save(cartCaptor.capture());
        Cart saved = cartCaptor.getValue();
        assertTrue(saved.getItems().isEmpty());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void clearCart_shouldDoNothing_whenCartMissing() {
        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        service.clearCart(userId);

        verify(cartRepository, never()).save(any(Cart.class));
    }
}

