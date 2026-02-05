package com.impetus.order_service.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.impetus.order_service.enums.OrderStatus;
import com.impetus.order_service.enums.PaymentMode;
import com.impetus.order_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_number", unique = true, nullable = false)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Column(name = "payment_mode", nullable = false)
    private PaymentMode paymentMode;

    @Column(name = "subtotal_amount", nullable = false)
    private Integer subtotalAmount;

    @Column(name = "tax_amount", nullable = false)
    private Integer taxAmount;

    @Column(name = "shipping_fee", nullable = false)
    private Integer shippingFee;

    @Column(name = "discount_amount", nullable = false)
    private Integer discountAmount;

    @Column(name = "total_amount", nullable = false)
    private Integer totalAmount;


    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private Payment paymentReference;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private ShippingAddress shippingAddress;

    private Instant placedAt;
    private Instant cancelledAt;
    private Instant deliveredAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderReservation> reservations = new ArrayList<>();


    public void addItem(OrderItem item){
        this.items.add(item);
        item.setOrder(this);
    }

    public void addReservation(OrderReservation reservation){
        this.reservations.add(reservation);
        reservation.setOrder(this);
    }

}
