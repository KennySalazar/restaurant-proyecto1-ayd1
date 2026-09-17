package com.restaurante.web.dto.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TipConfigurationRequest(

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal porcentaje
) {
}