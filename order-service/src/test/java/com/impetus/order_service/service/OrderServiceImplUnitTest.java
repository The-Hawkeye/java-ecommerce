package com.impetus.order_service.service;

import com.impetus.order_service.dto.*;
import com.impetus.order_service.entity.Cart;
import com.impetus.order_service.entity.CartItem;
import com.impetus.order_service.entity.Order;
import com.impetus.order_service.entity.OrderReservation;
import com.impetus.order_service.enums.CartStatus;
import com.impetus.order_service.integrations.ResilientProductService;
import com.impetus.order_service.integrations.ResilientUserService;
import com.impetus.order_service.repository.CartRepository;
import com.impetus.order_service.repository.OrderItemRepository;
import com.impetus.order_service.repository.OrderRepository;
import com.impetus.order_service.repository.OrderReservationRepository;

import com.impetus.order_service.service.Impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderServiceImpl.
 * Focuses on business logic, mapping, repository interactions, and WebClient orchestration.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplUnitTest {

    @Mock private OrderRepository orderRepository;
    @Mock private CartRepository cartRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private OrderReservationRepository orderReservationRepository;

    @Mock private ModelMapper modelMapper;
    @Mock private WebClient.Builder webClientBuilder;

    @Mock private ResilientProductService resilientProductService;
    @Mock private ResilientUserService resilientUserService;

    // WebClient chain mocks:
    @Mock private WebClient webClient;
    @Mock private WebClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private WebClient.RequestHeadersSpec<?> requestHeadersSpec;
    @Mock private WebClient.ResponseSpec responseSpec;

    private OrderServiceImpl service;

    @Captor private ArgumentCaptor<Order> orderCaptor;
    @Captor private ArgumentCaptor<Map<String, Object>> mapCaptor;

    private final Long userId = 101L;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(
                orderRepository,
                cartRepository,
                orderItemRepository,
                orderReservationRepository,
                modelMapper,
                webClientBuilder,
                // Strings are non-final in your service; we set productBaseUrl via ReflectionTestUtils
                // resilient services:
                resilientProductService,
                resilientUserService
        );

        // Set product base URL for updateInventory tests
        ReflectionTestUtils.setField(service, "productBaseUrl", "http://product-service");
    }

    // --------------------------------------
    // Helpers
    // --------------------------------------
    private Cart newActiveCart(Long id, Long userId) {
        Cart c = new Cart();
        c.setId(id);
        c.setUserId(userId);
        c.setCartStatus(CartStatus.ACTIVE);
        c.setItems(new ArrayList<>());
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }

    private CartItem ci(String productId, int qty) {
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(qty);
        item.setAddedAt(Instant.now());
        return item;
    }

    private ProductResponseDto product(String id, String name, Integer price, Integer inventoryQty) {
        ProductResponseDto dto = new ProductResponseDto();
        dto.setId(id);
        dto.setName(name);
        dto.setPrice(price);
        dto.setInventoryQuantity(inventoryQty);
        return dto;
    }

    private AddressResponse address(String contact, String phone) {
        AddressResponse a = new AddressResponse();
        a.setContactName(contact);
        a.setPhone(phone);
        return a;
    }

    private OrderRequest validOrderReq(Long shippingAddressId) {
        OrderRequest req = new OrderRequest();
        req.setShippingAddressId(shippingAddressId);
        // req.setPaymentMode(PaymentMode.COD); // if required in your DTO; add accordingly
        return req;
    }

    private void mockWebClientChainReturning(UpdateInventoryResponse resp) {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.header(anyString(), anyString())).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.retrieve()).thenReturn(responseSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        // Allow onStatus chaining to return the same responseSpec
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);

        // Return Mono of response
        when(responseSpec.bodyToMono(eq(UpdateInventoryResponse.class))).thenReturn(Mono.just(resp));
    }

    // ======================================
    // createOrderFromCart - success
    // ======================================
    @Test
    void createOrderFromCart_shouldCreateOrder_andComputeTotals_andCheckoutCart() {
        // Cart with two items
        Cart cart = newActiveCart(10L, userId);
        cart.getItems().add(ci("P1", 2)); // price 100 -> 200
        cart.getItems().add(ci("P2", 1)); // price 200 -> 200

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(cart));

        // Address resolution
        when(resilientUserService.fetchUserAddress(eq(userId), eq(999L)))
                .thenReturn(address("John Doe", "9999999999"));

        // Product resolution
        List<ProductResponseDto> products = List.of(
                product("P1", "Prod 1", 100, 10),
                product("P2", "Prod 2", 200, 5)
        );
        when(resilientProductService.getProducts(eq(List.of("P1", "P2"))))
                .thenReturn(products);

        // Order save returns the same order
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // ModelMapper mapping
        when(modelMapper.map(any(Order.class), eq(OrderResponse.class)))
                .thenAnswer(inv -> {
                    Order o = inv.getArgument(0);
                    OrderResponse r = new OrderResponse();
                    r.setOrderNumber(o.getOrderNumber());
                    r.setTotalAmount(o.getTotalAmount());
                    return r;
                });

        OrderRequest req = validOrderReq(999L);

        OrderResponse out = service.createOrderFromCart(userId, req);

        // Verify amounts: subtotal=400, tax=72, shipping=49, total=521
        assertNotNull(out.getOrderNumber());
        assertTrue(out.getOrderNumber().startsWith("ORD-"));
        assertEquals(521, out.getTotalAmount());

        // cart status updated to CHECKED_OUT and saved
        verify(cartRepository).save(argThat(c -> c.getCartStatus() == CartStatus.CHECKED_OUT));

        // order persisted
        verify(orderRepository).save(orderCaptor.capture());
        Order saved = orderCaptor.getValue();
        assertEquals(400, saved.getSubtotalAmount());
        assertEquals(72, saved.getTaxAmount());
        assertEquals(49, saved.getShippingFee());
        assertEquals(521, saved.getTotalAmount());
        assertNotNull(saved.getShippingAddress());
        assertEquals(userId, saved.getShippingAddress().getUserId());
    }

    // ======================================
    // createOrderFromCart - error paths
    // ======================================
    @Test
    void createOrderFromCart_shouldThrow_whenNoActiveCart() {
        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.createOrderFromCart(userId, validOrderReq(1L)));
        assertEquals("No Active Cart found", ex.getMessage());
    }

    @Test
    void createOrderFromCart_shouldThrow_whenCartEmpty() {
        Cart cart = newActiveCart(11L, userId);
        cart.setItems(new ArrayList<>());

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(cart));

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.createOrderFromCart(userId, validOrderReq(1L)));
        assertEquals("No items in cart", ex.getMessage());
    }

    @Test
    void createOrderFromCart_shouldThrow_whenShippingAddressIdMissing() {
        Cart cart = newActiveCart(12L, userId);
        cart.getItems().add(ci("P1", 1));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(cart));

        OrderRequest req = validOrderReq(null);

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.createOrderFromCart(userId, req));
        assertEquals("Shipping Address is required", ex.getMessage());
    }

    @Test
    void createOrderFromCart_shouldThrow_whenProductMissing() {
        Cart cart = newActiveCart(13L, userId);
        cart.getItems().add(ci("P-MISSING", 1));

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(cart));

        when(resilientUserService.fetchUserAddress(eq(userId), eq(1L)))
                .thenReturn(address("A", "B"));

        // return empty products list -> map lookup fails
        when(resilientProductService.getProducts(eq(List.of("P-MISSING"))))
                .thenReturn(List.of());

        OrderRequest req = validOrderReq(1L);

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.createOrderFromCart(userId, req));
        assertEquals("Product not found: P-MISSING", ex.getMessage());
    }

    @Test
    void createOrderFromCart_shouldThrow_whenInsufficientStock() {
        Cart cart = newActiveCart(14L, userId);
        cart.getItems().add(ci("P1", 5)); // request 5; inventory 3 -> fail

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(cart));

        when(resilientUserService.fetchUserAddress(eq(userId), eq(1L)))
                .thenReturn(address("A", "B"));

        when(resilientProductService.getProducts(eq(List.of("P1"))))
                .thenReturn(List.of(product("P1", "Prod 1", 100, 3)));

        OrderRequest req = validOrderReq(1L);

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.createOrderFromCart(userId, req));
        assertTrue(ex.getMessage().startsWith("Insufficient stock for product"));
        assertTrue(ex.getMessage().contains("Requested: 5"));
        assertTrue(ex.getMessage().contains("Available: 3"));
    }

    @Test
    void createOrderFromCart_shouldThrow_whenPriceMissing() {
        Cart cart = newActiveCart(15L, userId);
        cart.getItems().add(ci("P1", 1)); // price null -> fail

        when(cartRepository.findByUserIdAndCartStatus(eq(userId), eq(CartStatus.ACTIVE)))
                .thenReturn(Optional.of(cart));

        when(resilientUserService.fetchUserAddress(eq(userId), eq(1L)))
                .thenReturn(address("A", "B"));

        when(resilientProductService.getProducts(eq(List.of("P1"))))
                .thenReturn(List.of(product("P1", "Prod 1", null, 10)));

        OrderRequest req = validOrderReq(1L);

        NoSuchElementException ex = assertThrows(NoSuchElementException.class,
                () -> service.createOrderFromCart(userId, req));
        // note: message typo in code "proice"
        assertTrue(ex.getMessage().contains("Current proice not available"));
    }

    // ======================================
    // getOrder (with userId)
    // ======================================
    @Test
    void getOrder_withUser_shouldReturnMappedResponse() {
        Order order = new Order();
        order.setId(1000L);
        order.setUserId(userId);

        when(orderRepository.findByIdAndUserId(eq(1000L), eq(userId)))
                .thenReturn(Optional.of(order));

        when(modelMapper.map(eq(order), eq(OrderResponse.class)))
                .thenReturn(new OrderResponse());

        OrderResponse resp = service.getOrder(1000L, userId);
        assertNotNull(resp);
    }

    @Test
    void getOrder_withUser_shouldThrow_whenNotFound() {
        when(orderRepository.findByIdAndUserId(eq(1L), eq(userId)))
                .thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.getOrder(1L, userId));
        assertEquals("Order not found", ex.getMessage());
    }

    // ======================================
    // getOrder (admin)
    // ======================================
    @Test
    void getOrder_admin_shouldReturnMappedResponse() {
        Order order = new Order();
        order.setId(2000L);

        when(orderRepository.findById(eq(2000L)))
                .thenReturn(Optional.of(order));
        when(modelMapper.map(eq(order), eq(OrderResponse.class)))
                .thenReturn(new OrderResponse());

        OrderResponse resp = service.getOrder(2000L);
        assertNotNull(resp);
    }

    @Test
    void getOrder_admin_shouldThrow_whenNotFound() {
        when(orderRepository.findById(eq(2L))).thenReturn(Optional.empty());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.getOrder(2L));
        assertEquals("Order not found", ex.getMessage());
    }

    // ======================================
    // listOrder / listAllOrders
    // ======================================
    @Test
    void listOrder_shouldMapPage() {
        Order o1 = new Order(); o1.setId(1L);
        Order o2 = new Order(); o2.setId(2L);
        Page<Order> page = new PageImpl<>(List.of(o1, o2), PageRequest.of(0, 2), 2);

        when(orderRepository.findByUserId(eq(userId), any()))
                .thenReturn(page);

        when(modelMapper.map(any(Order.class), eq(OrderResponse.class)))
                .thenAnswer(inv -> new OrderResponse());

        Page<OrderResponse> out = service.listOrder(userId, 0, 2);
        assertEquals(2, out.getTotalElements());
        assertEquals(2, out.getContent().size());
    }


    @Test
    void listAllOrders_shouldMapPage() {
        Order o1 = new Order();
        o1.setId(1L);

        Page<Order> page = new PageImpl<>(List.of(o1), PageRequest.of(1, 1), 5);

        // ✅ Disambiguate the overloaded method with a typed matcher:
        when(orderRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        when(modelMapper.map(any(Order.class), eq(OrderResponse.class)))
                .thenAnswer(inv -> new OrderResponse());

        Page<OrderResponse> out = service.listAllOrders(1, 1);

        assertEquals(5, out.getTotalElements());
        assertEquals(1, out.getContent().size());
    }


    // ======================================
    // getPayload
    // ======================================
    @Test
    void getPayload_shouldMapReservations_toUpdateInventoryRequests() {
        Order order = new Order();
        order.setId(7000L);

        when(orderReservationRepository.findByOrderId(eq(order.getId())))
                .thenReturn(List.of(
                        new OrderReservation() {{ setProductId("P1"); setQuantity(2); }},
                        new OrderReservation() {{ setProductId("P2"); setQuantity(5); }}
                ));

        List<UpdateInventoryRequest> out = service.getPayload(order);
        assertEquals(2, out.size());
        assertEquals("P1", out.get(0).getProductId());
        assertEquals(2, out.get(0).getQuantity());
        assertEquals("P2", out.get(1).getProductId());
        assertEquals(5, out.get(1).getQuantity());
    }
}

