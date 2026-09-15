package com.restaurante.domain.repository;

import com.restaurante.domain.model.ComandaDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ComandaDetailRepository extends JpaRepository<ComandaDetail, Long> {

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE d.comanda.id = :comandaId
           """)
    List<ComandaDetail> findByComandaIdWithModifiers(
            @Param("comandaId") Long comandaId
    );
}