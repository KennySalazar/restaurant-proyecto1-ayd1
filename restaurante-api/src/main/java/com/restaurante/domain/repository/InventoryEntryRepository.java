package com.restaurante.domain.repository;

import com.restaurante.domain.model.InventoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryEntryRepository extends JpaRepository<InventoryEntry, Long> {

    long countByRestaurantId(Long restaurantId);

    boolean existsByRestaurantIdAndDocumentNumber(Long restaurantId, String documentNumber);

    Optional<InventoryEntry> findByIdAndRestaurantId(Long id, Long restaurantId);
}
