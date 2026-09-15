package com.restaurante.domain.repository;

import com.restaurante.domain.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository
        extends JpaRepository<PaymentMethod, Short> {

    Optional<PaymentMethod> findByIdAndActiveTrue(Short id);
}