package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Definición de una sub-cuenta y sus ítems específicos asignados.
 */
@Schema(description = "Definición de una subcuenta por ítems específicos")
public record SubaccountItemDefinitionRequest(
        @Schema(description = "Nombre descriptivo de la subcuenta", example = "Persona 1 - Juan")
        String nombre,

        @NotEmpty(message = "Debe incluir al menos un ítem para la subcuenta")
        @Valid
        @Schema(description = "Listado de platillos asignados a esta subcuenta")
        List<AssignItemRequest> items
) {
}
