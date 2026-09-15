package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Solicitud para registrar una comanda u orden en el sistema.
 */
@Schema(description = "Solicitud para registrar una nueva comanda")
public record CreateComandaRequest(
        @Schema(description = "Identificador de la mesa asociada (opcional si se indica accountId)", example = "1")
        Long tableId,

        @Schema(description = "Identificador de la cuenta asociada (opcional si se indica tableId)", example = "1")
        Long accountId,

        @Schema(description = "Notas generales de la orden", example = "Mesa cerca de la ventana")
        String generalNotes,

        @Schema(description = "Indica si la comanda debe enviarse a cocina y descontar inventario inmediatamente", example = "false")
        Boolean sendImmediately,

        @Schema(description = "Listado de platillos y/o combos que componen la orden", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La comanda debe contener al menos un platillo")
        @Valid
        List<CreateComandaItemRequest> items
) {
}
