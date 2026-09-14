package com.restaurante.domain.repository;

import com.restaurante.domain.model.ComboDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para los detalles de platillos incluidos en combos.
 */
@Repository
public interface ComboDetailRepository extends JpaRepository<ComboDetail, Long> {

    @Query("SELECT cd FROM ComboDetail cd JOIN FETCH cd.dish d WHERE cd.combo.id = :comboId ORDER BY cd.visualOrder ASC, d.name ASC")
    List<ComboDetail> findByComboIdWithDish(@Param("comboId") Long comboId);

    @Modifying
    @Query("DELETE FROM ComboDetail cd WHERE cd.combo.id = :comboId")
    void deleteByComboId(@Param("comboId") Long comboId);
}
