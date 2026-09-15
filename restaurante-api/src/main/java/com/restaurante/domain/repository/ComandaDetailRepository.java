package com.restaurante.domain.repository;

import com.restaurante.domain.model.ComandaDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para los renglones o ítems de comanda.
 */
@Repository
public interface ComandaDetailRepository extends JpaRepository<ComandaDetail, Long> {

    @Query("SELECT cd FROM ComandaDetail cd WHERE cd.comanda.id = :comandaId")
    List<ComandaDetail> findByComandaId(@Param("comandaId") Long comandaId);
}
