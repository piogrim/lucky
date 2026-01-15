package com.lucky.inventory_service.inventory.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class InventoryHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;
    private Long productId;
    private Long quantity;

    @Enumerated(EnumType.STRING)
    private HistoryStatus status;

    public InventoryHistory(Long orderId, Long productId, Long quantity, HistoryStatus status) {
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    public void restore() {
        this.status = HistoryStatus.RESTORED;
    }
}
