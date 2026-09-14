package com.restaurante.domain.repository;

import com.restaurante.domain.model.InventoryWasteDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryWasteDetailRepository extends JpaRepository<InventoryWasteDetail, Long> {

    @Query("SELECT d FROM InventoryWasteDetail d JOIN FETCH d.waste w JOIN FETCH d.supply s WHERE s.id = :supplyId ORDER BY w.registeredAt DESC")
    List<InventoryWasteDetail> findBySupplyIdOrderByDateDesc(@Param("supplyId") Long supplyId);

    @Query("SELECT d FROM InventoryWasteDetail d JOIN FETCH d.waste w JOIN FETCH d.supply s WHERE w.restaurantId = :restaurantId ORDER BY w.registeredAt DESC")
    List<InventoryWasteDetail> findByRestaurantIdOrderByDateDesc(@Param("restaurantId") Long restaurantId);
}
