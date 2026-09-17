package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Respuesta de confirmación tras registrar exitosamente una merma de
 * inventario.
 */
@Schema(description = "Respuesta de confirmación del registro de merma")
public record SupplyWasteRegistrationResponse(
                @Schema(description = "Mensaje de confirmación", example = "Merma de inventario registrada exitosamente") String message,

                @Schema(description = "Detalle de la merma registrada") SupplyWasteResponse waste) {
}
