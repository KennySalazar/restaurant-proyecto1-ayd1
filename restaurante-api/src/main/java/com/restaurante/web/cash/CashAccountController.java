package com.restaurante.web.cash;

import com.restaurante.application.account.AccountService;
import com.restaurante.application.account.SubaccountService;
import com.restaurante.web.dto.account.AccountResponse;
import com.restaurante.web.dto.subaccount.SubaccountResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/caja/cuentas")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('CASHIER')")
public class CashAccountController {

    private final AccountService accountService;
    private final SubaccountService subaccountService;

    public CashAccountController(
            AccountService accountService,
            SubaccountService subaccountService) {

        this.accountService = accountService;
        this.subaccountService = subaccountService;
    }

    @GetMapping
    @Operation(
            summary = "Consultar cuentas disponibles para cobro"
    )
    public List<AccountResponse> getAccountsReadyForPayment(
            Authentication authentication) {

        return accountService.getAccountsReadyForPayment(
                authentication
        );
    }

    @GetMapping("/{cuentaId}/subcuentas")
    @Operation(
            summary = "Consultar subcuentas pendientes de una cuenta"
    )
    public List<SubaccountResponse> getPendingSubaccounts(
            @PathVariable Long cuentaId,
            Authentication authentication) {

        return subaccountService
                .getPendingSubaccountsByAccountId(
                        cuentaId,
                        authentication
                );
    }
}