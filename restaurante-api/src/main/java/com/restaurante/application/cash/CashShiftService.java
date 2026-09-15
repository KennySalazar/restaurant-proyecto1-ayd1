package com.restaurante.application.cash;

import com.restaurante.domain.model.*;
import com.restaurante.domain.repository.*;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.cash.CashShiftResponse;
import com.restaurante.web.dto.cash.CloseCashShiftResponse;
import com.restaurante.web.dto.cash.OpenCashShiftRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import com.restaurante.web.dto.cash.CloseCashShiftRequest;
import com.restaurante.web.dto.cash.CloseCashShiftResponse;

@Service
public class CashShiftService {

    private final CashRegisterRepository cashRegisters;
    private final CashShiftRepository cashShifts;
    private final CashTransactionRepository cashTransactions;
    private final UserAccountRepository users;
    private final RestaurantUserProfileRepository profiles;
    private final EntityManager entityManager;

    public CashShiftService(
            CashRegisterRepository cashRegisters,
            CashShiftRepository cashShifts,
            CashTransactionRepository cashTransactions,
            UserAccountRepository users,
            RestaurantUserProfileRepository profiles,
            EntityManager entityManager) {

        this.cashRegisters = cashRegisters;
        this.cashShifts = cashShifts;
        this.cashTransactions = cashTransactions;
        this.users = users;
        this.profiles = profiles;
        this.entityManager = entityManager;
    }

    @Transactional
    public CashShiftResponse openShift(
            OpenCashShiftRequest request,
            Authentication authentication) {

        UserAccount account = users
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "authenticated_user_not_found",
                        "Usuario no encontrado",
                        "No se encontró la cuenta autenticada"
                ));

        if (!account.isEnabled()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "account_disabled",
                    "Cuenta deshabilitada",
                    "La cuenta se encuentra deshabilitada"
            );
        }

        if (account.getRole().getName() != RoleName.CASHIER) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "cashier_role_required",
                    "Operación no permitida",
                    "Solamente un cajero puede abrir un turno de caja"
            );
        }

        RestaurantUserProfile profile = profiles
                .findById(account.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "employee_profile_not_found",
                        "Empleado no encontrado",
                        "No se encontró el perfil del cajero"
                ));

        Long restaurantId = profile.getRestaurantId();

        CashRegister cashRegister = cashRegisters
                .findByIdAndRestaurantIdAndActiveTrue(
                        request.cajaId(),
                        restaurantId
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "cash_register_not_found",
                        "Caja no encontrada",
                        "La caja seleccionada no existe, está inactiva o no pertenece al restaurante"
                ));

        if (cashShifts.existsByCashierIdAndStatus(
                account.getId(),
                CashShiftStatus.ABIERTA)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "cashier_has_open_shift",
                    "Turno ya abierto",
                    "El cajero ya tiene un turno de caja abierto"
            );
        }

        if (cashShifts.existsByCashRegisterIdAndStatus(
                cashRegister.getId(),
                CashShiftStatus.ABIERTA)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "cash_register_has_open_shift",
                    "Caja ocupada",
                    "La caja seleccionada ya tiene un turno abierto"
            );
        }

        CashShift shift = new CashShift(
                cashRegister.getId(),
                account.getId(),
                request.montoInicialEfectivo(),
                normalizeNotes(request.observaciones())
        );

        shift = cashShifts.saveAndFlush(shift);

        CashTransaction opening = CashTransaction.opening(
                shift.getId(),
                account.getId(),
                shift.getInitialCashAmount()
        );

        cashTransactions.save(opening);

        return new CashShiftResponse(
                shift.getId(),
                cashRegister.getId(),
                cashRegister.getCode(),
                cashRegister.getName(),
                account.getId(),
                shift.getStatus().name(),
                shift.getInitialCashAmount(),
                shift.getOpenedAt(),
                shift.getOpeningNotes()
        );
    }

    private String normalizeNotes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    @Transactional
    public CloseCashShiftResponse closeShift(
            Long shiftId,
            CloseCashShiftRequest request,
            Authentication authentication) {

        UserAccount account = users
                .findByEmail(authentication.getName())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "authenticated_user_not_found",
                        "Usuario no encontrado",
                        "No se encontro la cuenta autenticada"
                ));

        if (!account.isEnabled()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "account_disabled",
                    "Cuenta deshabilitada",
                    "La cuenta se encuentra deshabilitada"
            );
        }

        if (account.getRole().getName() != RoleName.CASHIER) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "cashier_role_required",
                    "Operacion no permitida",
                    "Solamente un cajero puede cerrar un turno de caja"
            );
        }

        CashShift shift = cashShifts
                .findByIdAndCashierIdAndStatus(
                        shiftId,
                        account.getId(),
                        CashShiftStatus.ABIERTA
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "open_cash_shift_not_found",
                        "Turno abierto no encontrado",
                        "El turno no existe, no pertenece al cajero o ya esta cerrado"
                ));

        shift.close(
                request.efectivoReal(),
                normalizeNotes(request.observaciones())
        );

        cashShifts.saveAndFlush(shift);

        CashTransaction closingTransaction =
                CashTransaction.closing(
                        shift.getId(),
                        account.getId()
                );

        cashTransactions.saveAndFlush(closingTransaction);

        entityManager.refresh(shift);

        return new CloseCashShiftResponse(
                shift.getId(),
                shift.getCashRegisterId(),
                shift.getCashierId(),
                shift.getStatus().name(),
                shift.getInitialCashAmount(),
                shift.getExpectedCashAtClose(),
                shift.getActualCashAtClose(),
                shift.getClosingDifference(),
                shift.getOpenedAt(),
                shift.getClosedAt(),
                shift.getClosingNotes()
        );
    }
}