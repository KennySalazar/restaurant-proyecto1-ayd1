package com.restaurante.web.dto.cash;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CloseCashShiftRequest(

        @NotNull
        @DecimalMin(value = "0.00")
        BigDecimal efectivoReal,

        String observaciones
) {
}