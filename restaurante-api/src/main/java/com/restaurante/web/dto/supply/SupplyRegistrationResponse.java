package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de confirmación al registrar un nuevo insumo")
public record SupplyRegistrationResponse(
                @Schema(description = "Mensaje de confirmación", example = "Insumo registrado exitosamente") String message,

                @Schema(description = "Datos del insumo registrado") SupplyResponse supply) {
}
