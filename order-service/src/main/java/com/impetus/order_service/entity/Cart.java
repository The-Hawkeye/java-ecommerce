package com.impetus.order_service.entity;

import com.impetus.order_service.enums.CartStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private CartStatus cartStatus = CartStatus.ACTIVE;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();


    @OneToMany(
            mappedBy = "cart",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<CartItem> items = new ArrayList<>();

    public void addItem(CartItem item){
        items.add(item);
        item.setCart(this);
    }

    public void removeItem(CartItem item){
        items.remove(item);
        item.setCart(null);
    }
}
