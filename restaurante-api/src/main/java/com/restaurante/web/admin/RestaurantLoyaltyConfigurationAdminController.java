package com.restaurante.web.admin;

import com.restaurante.application.configuration.RestaurantConfigurationService;
import com.restaurante.web.dto.configuration.PointsAccumulationConfigurationRequest;
import com.restaurante.web.dto.configuration.PointsAccumulationConfigurationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.restaurante.web.dto.configuration.PointsRedemptionConfigurationRequest;
import com.restaurante.web.dto.configuration.PointsRedemptionConfigurationResponse;

@RestController
@RequestMapping("/admin/configuracion/fidelizacion")
@Tag(
        name = "Configuración",
        description = "Configuración administrativa del restaurante"
)
@SecurityRequirement(name = "bearerAuth")
public class RestaurantLoyaltyConfigurationAdminController {

    private final RestaurantConfigurationService configurationService;

    public RestaurantLoyaltyConfigurationAdminController(
            RestaurantConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping("/acumulacion-puntos")
    @Operation(summary = "Consultar la configuración de acumulación de puntos")
    public PointsAccumulationConfigurationResponse getCurrentPointsAccumulationConfiguration(
            Authentication authentication) {

        return configurationService
                .getCurrentPointsAccumulationConfiguration(authentication);
    }

    @PutMapping("/acumulacion-puntos")
    @Operation(summary = "Configurar la acumulación de puntos")
    public PointsAccumulationConfigurationResponse updatePointsAccumulationConfiguration(
            @Valid @RequestBody PointsAccumulationConfigurationRequest request,
            Authentication authentication) {

        return configurationService
                .updatePointsAccumulationConfiguration(
                        request,
                        authentication
                );
    }

    @GetMapping("/redencion-puntos")
    @Operation(summary = "Consultar el valor de redención de los puntos")
    public PointsRedemptionConfigurationResponse getCurrentPointsRedemptionConfiguration(
            Authentication authentication) {

        return configurationService
                .getCurrentPointsRedemptionConfiguration(authentication);
    }

    @PutMapping("/redencion-puntos")
    @Operation(summary = "Configurar el valor de redención de los puntos")
    public PointsRedemptionConfigurationResponse updatePointsRedemptionConfiguration(
            @Valid @RequestBody PointsRedemptionConfigurationRequest request,
            Authentication authentication) {

        return configurationService
                .updatePointsRedemptionConfiguration(
                        request,
                        authentication
                );
    }
}