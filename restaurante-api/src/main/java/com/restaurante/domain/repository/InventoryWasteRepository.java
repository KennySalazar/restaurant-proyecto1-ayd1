package com.restaurante.domain.repository;

import com.restaurante.domain.model.InventoryWaste;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryWasteRepository extends JpaRepository<InventoryWaste, Long> {

    boolean existsByRestaurantIdAndDocumentNumber(Long restaurantId, String documentNumber);

    long countByRestaurantId(Long restaurantId);

    List<InventoryWaste> findByRestaurantIdOrderByRegisteredAtDesc(Long restaurantId);
}
