package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de confirmación de registro de entrada de inventario")
public record SupplyEntryRegistrationResponse(
                @Schema(description = "Mensaje de confirmación", example = "Entrada de inventario registrada exitosamente") String message,

                @Schema(description = "Detalle de la entrada de inventario registrada") SupplyEntryResponse entry) {
}
