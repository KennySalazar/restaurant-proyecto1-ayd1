package com.restaurante.domain.repository;

import com.restaurante.domain.model.InventoryEntryDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryEntryDetailRepository extends JpaRepository<InventoryEntryDetail, Long> {

    @Query("SELECT d FROM InventoryEntryDetail d JOIN FETCH d.entry e JOIN FETCH d.supply s WHERE s.id = :supplyId ORDER BY e.receptionDate DESC, d.id DESC")
    List<InventoryEntryDetail> findBySupplyIdOrderByDateDesc(@Param("supplyId") Long supplyId);

    List<InventoryEntryDetail> findByEntryId(Long entryId);
}
