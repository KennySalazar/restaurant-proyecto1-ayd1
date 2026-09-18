package com.restaurante.web.cash;

import com.restaurante.application.payment.PaymentService;
import com.restaurante.web.dto.payment.ChargeRequest;
import com.restaurante.web.dto.payment.ChargeResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/caja/cuentas")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService) {

        this.paymentService = paymentService;
    }

    @PostMapping("/{cuentaId}/cobro")
    public ResponseEntity<ChargeResponse> chargeAccount(
            @PathVariable Long cuentaId,
            @Valid @RequestBody ChargeRequest request,
            Authentication authentication) {

        ChargeResponse response =
                paymentService.chargeAccount(
                        cuentaId,
                        request,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{cuentaId}/subcuentas/{subcuentaId}/cobro")
    public ResponseEntity<ChargeResponse> chargeSubaccount(
            @PathVariable Long cuentaId,
            @PathVariable Long subcuentaId,
            @Valid @RequestBody ChargeRequest request,
            Authentication authentication) {

        ChargeResponse response =
                paymentService.chargeSubaccount(
                        cuentaId,
                        subcuentaId,
                        request,
                        authentication
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}