package com.impetus.order_service.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.impetus.order_service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;


//    private Long orderId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    private Order order;

    private Integer amountPaisa;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    // Other details like vendor , gst , charges, settlement details etc;
}
