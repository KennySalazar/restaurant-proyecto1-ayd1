package com.restaurante.application.billing;

import com.restaurante.application.cash.CashShiftService;
import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.RestaurantConfiguration;
import com.restaurante.domain.model.RestaurantConfigurationStatus;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.RestaurantConfigurationRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.billing.BillingCalculationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BillingCalculationService {

    private static final BigDecimal ONE_HUNDRED =
            new BigDecimal("100");

    private final AccountRepository accounts;
    private final RestaurantConfigurationRepository configurations;
    private final RestaurantUserProfileRepository profiles;
    private final CashShiftService cashShiftService;

    public BillingCalculationService(
            AccountRepository accounts,
            RestaurantConfigurationRepository configurations,
            RestaurantUserProfileRepository profiles,
            CashShiftService cashShiftService) {

        this.accounts = accounts;
        this.configurations = configurations;
        this.profiles = profiles;
        this.cashShiftService = cashShiftService;
    }

    @Transactional(readOnly = true)
    public BillingCalculationResponse calculateAccount(
            Long accountId,
            Authentication authentication) {

        cashShiftService.requireOpenShift(authentication);

        Long restaurantId = getRestaurantId(authentication);

        Account account = getBillableAccount(
                accountId,
                restaurantId
        );

        BigDecimal subtotal =
                accounts.calculateDeliveredSubtotal(accountId);

        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }

        return calculate(
                account,
                null,
                subtotal,
                restaurantId
        );
    }

    @Transactional(readOnly = true)
    public BillingCalculationResponse calculateSubAccount(
            Long accountId,
            Long subAccountId,
            Authentication authentication) {

        cashShiftService.requireOpenShift(authentication);

        Long restaurantId = getRestaurantId(authentication);

        Account account = getBillableAccount(
                accountId,
                restaurantId
        );

        BigDecimal subtotal = accounts
                .findPendingSubAccountSubtotal(
                        accountId,
                        subAccountId
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "subaccount_not_found",
                        "Subcuenta no encontrada",
                        "La subcuenta no existe, no pertenece a la cuenta o ya no esta pendiente"
                ));

        return calculate(
                account,
                subAccountId,
                subtotal,
                restaurantId
        );
    }

    private Account getBillableAccount(
            Long accountId,
            Long restaurantId) {

        Account account = accounts
                .findByIdAndRestaurantId(
                        accountId,
                        restaurantId
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta seleccionada no existe en el restaurante"
                ));

        if (!"LISTA_COBRO".equals(account.getStatus())
                && !"PARCIALMENTE_PAGADA".equals(account.getStatus())) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_not_ready_for_payment",
                    "Cuenta no disponible para cobro",
                    "La cuenta debe estar marcada como lista para cobro"
            );
        }

        return account;
    }

    private BillingCalculationResponse calculate(
            Account account,
            Long subAccountId,
            BigDecimal subtotal,
            Long restaurantId) {

        RestaurantConfiguration configuration =
                configurations
                        .findByRestaurantIdAndStatus(
                                restaurantId,
                                RestaurantConfigurationStatus.VIGENTE
                        )
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "restaurant_configuration_not_found",
                                "Configuracion no encontrada",
                                "El restaurante no tiene una configuracion vigente"
                        ));

        BigDecimal normalizedSubtotal =
                money(subtotal);

        BigDecimal taxPercentage =
                configuration.getTaxPercentage();

        BigDecimal tipPercentage =
                configuration.getTipPercentage();

        BigDecimal taxAmount = percentage(
                normalizedSubtotal,
                taxPercentage
        );

        BigDecimal tipAmount = percentage(
                normalizedSubtotal,
                tipPercentage
        );

        BigDecimal total = money(
                normalizedSubtotal
                        .add(taxAmount)
                        .add(tipAmount)
        );

        return new BillingCalculationResponse(
                account.getId(),
                subAccountId,
                account.getAccountNumber(),
                normalizedSubtotal,
                taxPercentage,
                taxAmount,
                tipPercentage,
                tipAmount,
                total
        );
    }

    private BigDecimal percentage(
            BigDecimal base,
            BigDecimal percentage) {

        return base
                .multiply(percentage)
                .divide(
                        ONE_HUNDRED,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private Long getRestaurantId(
            Authentication authentication) {

        Long userId = getUserId(authentication);

        RestaurantUserProfile profile = profiles
                .findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        return profile.getRestaurantId();
    }

    private Long getUserId(
            Authentication authentication) {

        if (authentication == null
                || !(authentication.getDetails()
                instanceof JwtData jwtData)) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        return jwtData.userId();
    }
}