package com.restaurante.web.dto.configuration;

import com.restaurante.domain.model.RestaurantConfigurationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TipConfigurationResponse(
        Long id,
        BigDecimal porcentaje,
        RestaurantConfigurationStatus estado,
        OffsetDateTime vigenteDesde,
        OffsetDateTime vigenteHasta
) {
}