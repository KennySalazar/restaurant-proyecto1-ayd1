package com.restaurante.web.dto.configuration;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PointsRedemptionConfigurationRequest(

        @NotNull
        @DecimalMin(value = "0.0", inclusive = false)
        BigDecimal valorMonetarioPunto
) {
}