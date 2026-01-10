package com.lucky.order_service.orderItem.domain;

import com.lucky.order_service.order.domain.Order;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@NoArgsConstructor
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long productId;

    private Long quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    private Order order;

    public OrderItem(Long productId, Long quantity, Order order) {
        this.productId = productId;
        this.quantity = quantity;
        this.order = order;
    }
}
