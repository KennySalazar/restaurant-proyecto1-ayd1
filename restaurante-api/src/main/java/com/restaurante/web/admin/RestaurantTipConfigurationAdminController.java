package com.restaurante.web.admin;

import com.restaurante.application.configuration.RestaurantConfigurationService;
import com.restaurante.web.dto.configuration.TipConfigurationRequest;
import com.restaurante.web.dto.configuration.TipConfigurationResponse;
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

@RestController
@RequestMapping("/admin/configuracion/propina")
@Tag(
        name = "Configuración",
        description = "Configuración administrativa del restaurante"
)
@SecurityRequirement(name = "bearerAuth")
public class RestaurantTipConfigurationAdminController {

    private final RestaurantConfigurationService configurationService;

    public RestaurantTipConfigurationAdminController(
            RestaurantConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @Operation(summary = "Consultar la configuración vigente de propina")
    public TipConfigurationResponse getCurrentTipConfiguration(
            Authentication authentication) {

        return configurationService
                .getCurrentTipConfiguration(authentication);
    }

    @PutMapping
    @Operation(summary = "Configurar el porcentaje de propina sugerida")
    public TipConfigurationResponse updateTipConfiguration(
            @Valid @RequestBody TipConfigurationRequest request,
            Authentication authentication) {

        return configurationService
                .updateTipConfiguration(request, authentication);
    }
}