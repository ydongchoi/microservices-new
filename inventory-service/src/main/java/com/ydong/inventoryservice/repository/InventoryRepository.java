package com.ydong.inventoryservice.repository;

import com.ydong.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
