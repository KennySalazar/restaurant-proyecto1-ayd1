package com.restaurante.web.dto.configuration;

import com.restaurante.domain.model.RestaurantConfigurationStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PointsRedemptionConfigurationResponse(
        Long id,
        BigDecimal valorMonetarioPunto,
        RestaurantConfigurationStatus estado,
        OffsetDateTime vigenteDesde,
        OffsetDateTime vigenteHasta
) {
}