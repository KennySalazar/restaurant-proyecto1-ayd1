package com.restaurante.web.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RegisterPaymentRequest(

        @NotNull
        Short metodoPagoId,

        @NotNull
        @DecimalMin(value = "0.01")
        BigDecimal monto,

        BigDecimal montoRecibido,

        String referencia,

        String autorizacion
) {
}