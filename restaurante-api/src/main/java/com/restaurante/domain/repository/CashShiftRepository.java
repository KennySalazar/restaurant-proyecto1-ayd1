package com.restaurante.domain.repository;

import com.restaurante.domain.model.CashShift;
import com.restaurante.domain.model.CashShiftStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CashShiftRepository
        extends JpaRepository<CashShift, Long> {

    boolean existsByCashierIdAndStatus(
            Long cashierId,
            CashShiftStatus status
    );

    boolean existsByCashRegisterIdAndStatus(
            Long cashRegisterId,
            CashShiftStatus status
    );

    Optional<CashShift> findByIdAndCashierIdAndStatus(
            Long id,
            Long cashierId,
            CashShiftStatus status
    );

    Optional<CashShift> findByCashierIdAndStatus(
            Long cashierId,
            CashShiftStatus status
    );
}