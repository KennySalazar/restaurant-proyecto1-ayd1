package com.restaurante.web.admin;

import com.restaurante.application.configuration.RestaurantConfigurationService;
import com.restaurante.web.dto.configuration.TaxConfigurationRequest;
import com.restaurante.web.dto.configuration.TaxConfigurationResponse;
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
@RequestMapping("/admin/configuracion/impuesto")
@Tag(
        name = "Configuración",
        description = "Configuración administrativa del restaurante"
)
@SecurityRequirement(name = "bearerAuth")
public class RestaurantConfigurationAdminController {

    private final RestaurantConfigurationService configurationService;

    public RestaurantConfigurationAdminController(
            RestaurantConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @Operation(summary = "Consultar la configuración vigente del impuesto")
    public TaxConfigurationResponse getCurrentTaxConfiguration(
            Authentication authentication) {

        return configurationService
                .getCurrentTaxConfiguration(authentication);
    }

    @PutMapping
    @Operation(summary = "Configurar el impuesto del restaurante")
    public TaxConfigurationResponse updateTaxConfiguration(
            @Valid @RequestBody TaxConfigurationRequest request,
            Authentication authentication) {

        return configurationService
                .updateTaxConfiguration(request, authentication);
    }
}