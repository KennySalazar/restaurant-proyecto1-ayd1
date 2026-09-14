package com.restaurante.application.configuration;

import com.restaurante.domain.model.RestaurantConfiguration;
import com.restaurante.domain.model.RestaurantConfigurationStatus;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.repository.RestaurantConfigurationRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.configuration.TaxConfigurationRequest;
import com.restaurante.web.dto.configuration.TaxConfigurationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import com.restaurante.web.dto.configuration.TipConfigurationRequest;
import com.restaurante.web.dto.configuration.TipConfigurationResponse;

@Service
public class RestaurantConfigurationService {

    private final RestaurantConfigurationRepository configurations;
    private final RestaurantUserProfileRepository profiles;

    public RestaurantConfigurationService(
            RestaurantConfigurationRepository configurations,
            RestaurantUserProfileRepository profiles) {
        this.configurations = configurations;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public TaxConfigurationResponse getCurrentTaxConfiguration(
            Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);

        RestaurantConfiguration current = configurations
                .findByRestaurantIdAndStatus(
                        restaurantId,
                        RestaurantConfigurationStatus.VIGENTE
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "restaurant_configuration_not_found",
                        "Configuración no encontrada",
                        "El restaurante no tiene una configuración vigente"
                ));

        return toTaxResponse(current);
    }

    @Transactional
    public TaxConfigurationResponse updateTaxConfiguration(
            TaxConfigurationRequest request,
            Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);
        Long userId = getUserId(authentication);

        RestaurantConfiguration current = configurations
                .findByRestaurantIdAndStatus(
                        restaurantId,
                        RestaurantConfigurationStatus.VIGENTE
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "restaurant_configuration_not_found",
                        "Configuración no encontrada",
                        "El restaurante no tiene una configuración vigente"
                ));

        OffsetDateTime changeTime = OffsetDateTime.now(
                ZoneId.of("America/Guatemala")
        );

        current.markHistorical(changeTime);
        configurations.saveAndFlush(current);

        RestaurantConfiguration next =
                new RestaurantConfiguration(
                        restaurantId,
                        request.nombre().trim(),
                        request.porcentaje(),
                        current.getTipPercentage(),
                        current.isEditableTip(),
                        current.getPointsPerCurrency(),
                        current.getPointMonetaryValue(),
                        current.getReservationDurationMinutes(),
                        current.getReservationToleranceMinutes(),
                        userId
                );

        RestaurantConfiguration saved =
                configurations.save(next);

        return toTaxResponse(saved);
    }

    @Transactional(readOnly = true)
    public TipConfigurationResponse getCurrentTipConfiguration(
            Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);

        RestaurantConfiguration current = configurations
                .findByRestaurantIdAndStatus(
                        restaurantId,
                        RestaurantConfigurationStatus.VIGENTE
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "restaurant_configuration_not_found",
                        "Configuración no encontrada",
                        "El restaurante no tiene una configuración vigente"
                ));

        return toTipResponse(current);
    }

    @Transactional
    public TipConfigurationResponse updateTipConfiguration(
            TipConfigurationRequest request,
            Authentication authentication) {

        Long restaurantId = getRestaurantId(authentication);
        Long userId = getUserId(authentication);

        RestaurantConfiguration current = configurations
                .findByRestaurantIdAndStatus(
                        restaurantId,
                        RestaurantConfigurationStatus.VIGENTE
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "restaurant_configuration_not_found",
                        "Configuración no encontrada",
                        "El restaurante no tiene una configuración vigente"
                ));

        OffsetDateTime changeTime = OffsetDateTime.now(
                ZoneId.of("America/Guatemala")
        );

        current.markHistorical(changeTime);
        configurations.saveAndFlush(current);

        RestaurantConfiguration next =
                new RestaurantConfiguration(
                        restaurantId,
                        current.getTaxName(),
                        current.getTaxPercentage(),
                        request.porcentaje(),
                        current.isEditableTip(),
                        current.getPointsPerCurrency(),
                        current.getPointMonetaryValue(),
                        current.getReservationDurationMinutes(),
                        current.getReservationToleranceMinutes(),
                        userId
                );

        RestaurantConfiguration saved =
                configurations.save(next);

        return toTipResponse(saved);
    }

    private TipConfigurationResponse toTipResponse(
            RestaurantConfiguration configuration) {

        return new TipConfigurationResponse(
                configuration.getId(),
                configuration.getTipPercentage(),
                configuration.getStatus(),
                configuration.getValidFrom(),
                configuration.getValidUntil()
        );
    }

    private TaxConfigurationResponse toTaxResponse(
            RestaurantConfiguration configuration) {

        return new TaxConfigurationResponse(
                configuration.getId(),
                configuration.getTaxName(),
                configuration.getTaxPercentage(),
                configuration.getStatus(),
                configuration.getValidFrom(),
                configuration.getValidUntil()
        );
    }

    private Long getRestaurantId(Authentication authentication) {
        Long userId = getUserId(authentication);

        RestaurantUserProfile profile = profiles
                .findById(userId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        return profile.getRestaurantId();
    }

    private Long getUserId(Authentication authentication) {
        if (authentication == null
                || !(authentication.getDetails() instanceof JwtData jwtData)) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        return jwtData.userId();
    }
}