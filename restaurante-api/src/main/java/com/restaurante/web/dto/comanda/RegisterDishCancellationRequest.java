package com.restaurante.web.dto.comanda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud para registrar una cancelación excepcional de platillo en preparación.
 */
@Schema(description = "Solicitud de cancelación excepcional de un platillo en preparación o enviado a cocina")
public record RegisterDishCancellationRequest(
        @Schema(description = "Motivo detallado de la cancelación excepcional", example = "Cliente cambió de opinión tras ordenar", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El motivo de la cancelación es obligatorio")
        @Size(max = 500, message = "El motivo no puede exceder los 500 caracteres")
        @JsonProperty("motivo") @JsonAlias("reason")
        String motivo,

        @Schema(description = "Tipo de excepción (CLIENTE, ERROR_MESERO, NO_DISPONIBLE_COCINA, OTRO)", example = "CLIENTE", defaultValue = "CLIENTE")
        @JsonProperty("tipo") @JsonAlias("type")
        String tipo,

        @Schema(description = "Acción sobre el inventario (REGISTRAR_MERMA, REINTEGRAR, SIN_AJUSTE)", example = "REGISTRAR_MERMA")
        @JsonProperty("accionInventario") @JsonAlias("inventoryAction")
        String accionInventario,

        @Schema(description = "Identificador del supervisor/administrador que autoriza la excepción (opcional)", example = "1")
        @JsonProperty("autorizadoPorId") @JsonAlias("authorizedById")
        Long autorizadoPorId
) {
}
