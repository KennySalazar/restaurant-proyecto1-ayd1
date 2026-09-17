package com.restaurante.domain.repository;

import com.restaurante.domain.model.ComandaDetailCancellation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ComandaDetailCancellationRepository extends JpaRepository<ComandaDetailCancellation, Long> {

    Optional<ComandaDetailCancellation> findByComandaDetailId(Long comandaDetailId);

    boolean existsByComandaDetailId(Long comandaDetailId);
}
