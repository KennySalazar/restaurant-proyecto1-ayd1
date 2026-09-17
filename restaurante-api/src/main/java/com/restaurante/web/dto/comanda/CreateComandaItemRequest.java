package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Solicitud de un ítem individual (platillo o combo) para incluir en una comanda.
 */
@Schema(description = "Ítem de platillo o combo a incluir en una comanda")
public record CreateComandaItemRequest(
        @Schema(description = "Identificador del platillo (opcional si se especifica comboId)", example = "1")
        Long dishId,

        @Schema(description = "Identificador del combo (opcional si se especifica dishId)", example = "2")
        Long comboId,

        @Schema(description = "Cantidad solicitada", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero")
        short quantity,

        @Schema(description = "Notas especiales de preparación", example = "Sin hielo")
        String specialNotes,

        @Schema(description = "Lista de identificadores de modificadores seleccionados")
        List<Long> modifierIds
) {
}
