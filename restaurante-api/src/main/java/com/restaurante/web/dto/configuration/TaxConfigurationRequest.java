package com.restaurante.web.dto.configuration;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TaxConfigurationRequest(

        @NotBlank
        @Size(max = 80)
        String nombre,

        @NotNull
        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal porcentaje
) {
}