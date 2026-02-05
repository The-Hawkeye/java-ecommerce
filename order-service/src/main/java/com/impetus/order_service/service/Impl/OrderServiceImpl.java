package com.impetus.order_service.service.Impl;

//import com.impetus.order_service.client.UserClient;

import com.impetus.order_service.dto.*;
import com.impetus.order_service.entity.*;
import com.impetus.order_service.enums.CartStatus;
import com.impetus.order_service.enums.OrderStatus;
import com.impetus.order_service.enums.PaymentStatus;
import com.impetus.order_service.enums.ReservationStatus;
import com.impetus.order_service.integrations.ResilientProductService;
import com.impetus.order_service.integrations.ResilientUserService;
import com.impetus.order_service.repository.CartRepository;
import com.impetus.order_service.repository.OrderItemRepository;
import com.impetus.order_service.repository.OrderRepository;
import com.impetus.order_service.repository.OrderReservationRepository;
import com.impetus.order_service.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderReservationRepository orderReservationRepository;
//    private final UserClient userClient;
    private final ModelMapper modelMapper;
    private final WebClient.Builder webClient;
    @Value("${services.user.base-url}")
    private String userServiceBaseUrl;
    @Value("${services.product.base-url}")
    private String productBaseUrl;

    private final ResilientProductService resilientProductService;
    private final ResilientUserService resilientUserService;


    @Override
    public OrderResponse createOrderFromCart(Long userId, OrderRequest req) {
        Cart cart = cartRepository.findByUserIdAndCartStatus(userId, CartStatus.ACTIVE).orElseThrow(()-> new RuntimeException("No Active Cart found"));
        if(cart.getItems().isEmpty()){
            throw new NoSuchElementException("No items in cart");
        }

        // 1) Resolve shipping address from User Service

        Long shippingAddressId = req.getShippingAddressId();
        if(shippingAddressId == null){
            throw new NoSuchElementException("Shipping Address is required");
        }

        AddressResponse address = resilientUserService.fetchUserAddress(userId, shippingAddressId);

        // 2) Generate order number
        String orderNumber = generateOrderNumber(userId);

        // 3) Build order skeleton
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMode(req.getPaymentMode());
        order.setPlacedAt(Instant.now());

        // 4) Map shipping address
        ShippingAddress sa = new ShippingAddress();
        sa.setUserId(userId);
        sa.setContactName(address.getContactName());
        sa.setPhone(address.getPhone());
        sa.setAddressLabel(address.getAddressLabel());
        sa.setAddressLine1(address.getAddressLine1());
        sa.setAddressLine2(address.getAddressLine2());
        sa.setLocality(address.getLocality());
        sa.setCity(address.getCity());
        sa.setState(address.getState());
        sa.setPincode(address.getPincode());
        sa.setOrder(order);
        order.setShippingAddress(sa);

        // 5) Create OrderItems (pricing snapshot logic — ideally from Product Service)
        List<OrderItem> orderItems = new ArrayList<>();
        Integer subtotal = 0;

        List<String> productIds = cart.getItems().stream()
                .map(CartItem::getProductId)
                .distinct()
                .toList();



// Using resilient client instead of direclty fetching
//        List<ProductResponseDto> productList = fetchProducts(productIds);

        List<ProductResponseDto> productList = resilientProductService.getProducts(productIds);
        log.info(productList.stream().map(ProductResponseDto::getId).collect(Collectors.joining()));
        Map<String, ProductResponseDto> mapOfProductsById = productList.stream()
                .collect(Collectors.toMap(ProductResponseDto::getId, p->p));


        for(CartItem cartItem : cart.getItems()){
            String productId = cartItem.getProductId();
            ProductResponseDto product = mapOfProductsById.get(productId);
            if(product == null){
                throw new NoSuchElementException("Product not found: "+ productId);
            }

            int requestedQuantity = cartItem.getQuantity();
            if(product.getInventoryQuantity() == null || product.getInventoryQuantity() < requestedQuantity){
                throw new NoSuchElementException("Insufficient stock for product " + product.getName()+ " (Requested: "+ requestedQuantity+ " Available: "+ product.getInventoryQuantity());
            }

            Integer unitPrice = product.getPrice();
            if(unitPrice == null){
                throw new NoSuchElementException("Current proice not available for product "+ product.getName());
            }

            Integer productTotal = unitPrice * requestedQuantity;
            subtotal += productTotal;


            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(productId);
            orderItem.setProductSku(product.getSku());
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(requestedQuantity);
            orderItem.setTotalPrice(productTotal);

            order.addItem(orderItem);

            OrderReservation reservation = new OrderReservation();
            reservation.setProductId(productId);
            reservation.setQuantity(requestedQuantity);
            reservation.setStatus(ReservationStatus.PENDING);
            reservation.setCreatedAt(Instant.now());

            order.addReservation(reservation);
        }

        // 6) Totals (configurable)
        int taxAmount = Math.round(subtotal * 0.18f);
        int shippingFee = 49;
        int discountAmount = 0;
        int totalAmount = subtotal + taxAmount + shippingFee - discountAmount;

        order.setSubtotalAmount(subtotal);
        order.setTaxAmount(taxAmount);
        order.setShippingFee(shippingFee);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(totalAmount);

        // Save initial order
        Order savedOrder = orderRepository.save(order);


        // 9) Mark cart checked out
        cart.setCartStatus(CartStatus.CHECKED_OUT);
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        return toResponse(order);

    }

    @Override
    public OrderResponse getOrder(Long orderId, Long userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(()-> new RuntimeException("Order not found"));

        return toResponse(order);
    }

    public OrderResponse getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(()-> new RuntimeException("Order not found"));

        return toResponse(order);
    }

    @Override
    public Page<OrderResponse> listOrder(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
        return orderPage.map(this::toResponse);
    }


    private String generateOrderNumber(Long userId) {
        return "ORD-" + Instant.now().toEpochMilli() + "-" + (userId % 1000) + "-" + new Random().nextInt(9999);
    }


    public UpdateInventoryResponse updateInventory(List<UpdateInventoryRequest> payload, Long userId) {
        WebClient client = webClient.baseUrl(productBaseUrl).build();
        UpdateInventoryResponse response = client.post()
                .uri("/updateInventory")
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Roles", "USER")
                .bodyValue(Collections.singletonMap("items", payload))
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(), res -> res.bodyToMono(String.class).flatMap(body -> Mono.error(new RuntimeException("ProductService error : " + body))))
                .bodyToMono(UpdateInventoryResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Product Service Returned Empty response");
        }
        return response;
    }

    public UpdateInventoryResponse updateInventory(Order order) {

        List<UpdateInventoryRequest> payload = this.getPayload(order);

        WebClient client = webClient.baseUrl(productBaseUrl).build();
        UpdateInventoryResponse response = client.post()
                .uri("/updateInventory")
                .header("X-User-Id", String.valueOf(order.getUserId()))
                .header("X-User-Roles", "USER")
                .bodyValue(Collections.singletonMap("item", payload))
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.is4xxClientError() || httpStatusCode.is5xxServerError(), res -> res.bodyToMono(String.class).flatMap(body -> Mono.error(new RuntimeException("ProductService error : " + body))))
                .bodyToMono(UpdateInventoryResponse.class)
                .block();

        if (response == null) {
            throw new RuntimeException("Product Service Returned Empty response");
        }
        return response;
    }

    @Override
    public Page<OrderResponse> listAllOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> allOrders = orderRepository.findAll(pageable);
        return allOrders.map(this::toResponse);
    }

//    @Override
//    public Page<OrderResponse> listOrderOfUser(Long userId, int page, int size) {
//        Pageable pageable = PageRequest.of(page, size);
//        Page<Order> orderPage = orderRepository.findByUserId(userId, pageable);
//        return orderPage.map(this::toResponse);
//    }

    public List<UpdateInventoryRequest> getPayload(Order order){
        List<OrderReservation> ress = orderReservationRepository.findByOrderId(order.getId());
        return ress.stream()
                .map(r -> new UpdateInventoryRequest(r.getProductId(), r.getQuantity()))
                .toList();

    }

    private OrderResponse toResponse(Order order){
        return modelMapper.map(order, OrderResponse.class);
    }
}
