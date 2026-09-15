package com.restaurante.application.cash;

import com.restaurante.domain.model.CashShift;
import com.restaurante.domain.model.CashShiftStatus;
import com.restaurante.domain.model.RoleName;
import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.CashRegisterRepository;
import com.restaurante.domain.repository.CashShiftRepository;
import com.restaurante.domain.repository.CashTransactionRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.UserAccountRepository;
import com.restaurante.exception.ApiException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import com.restaurante.domain.model.Role;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class CashShiftPaymentGuardTest {

    private CashShiftRepository cashShifts;
    private UserAccountRepository users;
    private CashShiftService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        CashRegisterRepository cashRegisters =
                mock(CashRegisterRepository.class);

        cashShifts =
                mock(CashShiftRepository.class);

        CashTransactionRepository cashTransactions =
                mock(CashTransactionRepository.class);

        users =
                mock(UserAccountRepository.class);

        RestaurantUserProfileRepository profiles =
                mock(RestaurantUserProfileRepository.class);

        EntityManager entityManager =
                mock(EntityManager.class);

        authentication =
                mock(Authentication.class);

        service = new CashShiftService(
                cashRegisters,
                cashShifts,
                cashTransactions,
                users,
                profiles,
                entityManager
        );
    }

    @Test
    void bloqueaCobroCuandoCajeroNoTieneTurnoAbierto() {
        UserAccount account = mock(UserAccount.class);
        Role role = mock(Role.class);

        when(authentication.getName())
                .thenReturn("cajero@test.com");

        when(users.findByEmail("cajero@test.com"))
                .thenReturn(Optional.of(account));

        when(account.isEnabled())
                .thenReturn(true);

        when(account.getId())
                .thenReturn(4L);

        when(account.getRole())
                .thenReturn(role);

        when(role.getName())
                .thenReturn(RoleName.CASHIER);

        when(cashShifts.findByCashierIdAndStatus(
                4L,
                CashShiftStatus.ABIERTA
        )).thenReturn(Optional.empty());

        ApiException exception = assertThrows(
                ApiException.class,
                () -> service.requireOpenShift(authentication)
        );

        assertEquals(
                "open_cash_shift_required",
                exception.getCode()
        );
    }

    @Test
    void permiteCobroCuandoCajeroTieneTurnoAbierto() {
        UserAccount account = mock(UserAccount.class);
        Role role = mock(Role.class);
        CashShift shift = mock(CashShift.class);

        when(authentication.getName())
                .thenReturn("cajero@test.com");

        when(users.findByEmail("cajero@test.com"))
                .thenReturn(Optional.of(account));

        when(account.isEnabled())
                .thenReturn(true);

        when(account.getId())
                .thenReturn(4L);

        when(account.getRole())
                .thenReturn(role);

        when(role.getName())
                .thenReturn(RoleName.CASHIER);

        when(cashShifts.findByCashierIdAndStatus(
                4L,
                CashShiftStatus.ABIERTA
        )).thenReturn(Optional.of(shift));

        CashShift result =
                service.requireOpenShift(authentication);

        assertSame(shift, result);
    }
}