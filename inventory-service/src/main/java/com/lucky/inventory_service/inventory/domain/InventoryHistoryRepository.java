package com.lucky.inventory_service.inventory.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
    List<InventoryHistory> findAllByOrderIdAndStatus(Long orderId, HistoryStatus historyStatus);
}
