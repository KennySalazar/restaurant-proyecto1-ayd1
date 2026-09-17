package com.restaurante.web.dto.subaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Solicitud para dividir una cuenta asignando ítems específicos a distintas subcuentas.
 */
@Schema(description = "Datos para dividir una cuenta abierta por ítems específicos")
public record SplitByItemsRequest(
        @NotEmpty(message = "Debe especificar al menos dos subcuentas para la división")
        @Size(min = 2, message = "Debe especificar al menos dos subcuentas")
        @Valid
        @Schema(description = "Listado de subcuentas con sus ítems asignados")
        List<SubaccountItemDefinitionRequest> subcuentas
) {
}
