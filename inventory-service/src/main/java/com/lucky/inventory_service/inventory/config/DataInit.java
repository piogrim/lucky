package com.lucky.inventory_service.inventory.config;

import com.lucky.inventory_service.inventory.domain.Inventory;
import com.lucky.inventory_service.inventory.domain.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
class DataInit implements CommandLineRunner {

    private final InventoryRepository inventoryRepository;

    @Autowired
    public DataInit(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    public void run(String... args){
        for(Long i = 0L; i < 100L; i++) {
            if (inventoryRepository.findByProductId(i).isEmpty()) {
                inventoryRepository.save(new Inventory(i, 100L));
            }
        }
    }
}
