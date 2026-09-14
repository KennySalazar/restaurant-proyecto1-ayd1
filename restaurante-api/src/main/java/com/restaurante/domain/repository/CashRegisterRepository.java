package com.restaurante.domain.repository;

import com.restaurante.domain.model.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CashRegisterRepository
        extends JpaRepository<CashRegister, Long> {

    Optional<CashRegister> findByIdAndRestaurantIdAndActiveTrue(
            Long id,
            Long restaurantId
    );
}