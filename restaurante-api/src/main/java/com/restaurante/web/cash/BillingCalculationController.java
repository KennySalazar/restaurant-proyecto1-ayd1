package com.restaurante.web.cash;

import com.restaurante.application.billing.BillingCalculationService;
import com.restaurante.web.dto.billing.BillingCalculationResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/caja/cuentas")
@SecurityRequirement(name = "bearerAuth")
public class BillingCalculationController {

    private final BillingCalculationService service;

    public BillingCalculationController(
            BillingCalculationService service) {
        this.service = service;
    }

    @GetMapping("/{cuentaId}/calculo")
    @Operation(
            summary = "Calcular subtotal, impuestos, propina y total de una cuenta"
    )
    public BillingCalculationResponse calculateAccount(
            @PathVariable Long cuentaId,
            Authentication authentication) {

        return service.calculateAccount(
                cuentaId,
                authentication
        );
    }

    @GetMapping("/{cuentaId}/subcuentas/{subcuentaId}/calculo")
    @Operation(
            summary = "Calcular subtotal, impuestos, propina y total de una subcuenta"
    )
    public BillingCalculationResponse calculateSubAccount(
            @PathVariable Long cuentaId,
            @PathVariable Long subcuentaId,
            Authentication authentication) {

        return service.calculateSubAccount(
                cuentaId,
                subcuentaId,
                authentication
        );
    }
}