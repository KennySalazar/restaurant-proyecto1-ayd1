package com.restaurante.web.dto.kitchen;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Solicitud para marcar un platillo de comanda como no disponible por discrepancia o falta de insumos.
 */
@Schema(description = "Solicitud para marcar un platillo como no disponible por falta de insumos")
public record MarkDishUnavailableRequest(
        @Schema(
                description = "Motivo de la indisponibilidad o insumo faltante detectado en cocina",
                example = "Falta de carne molida detectada en inventario físico"
        )
        @JsonProperty("reason")
        @JsonAlias({"motivo", "observaciones", "notes", "insumoFaltante"})
        String reason,

        @Schema(
                description = "Indica si se debe marcar el platillo como no disponible en el menú (App1) para evitar nuevos pedidos",
                example = "true",
                defaultValue = "true"
        )
        @JsonProperty("disableInMenu")
        @JsonAlias({"desactivarEnMenu", "afectarMenu", "updateMenuAvailability"})
        Boolean disableInMenu
) {
    public MarkDishUnavailableRequest {
        if (disableInMenu == null) {
            disableInMenu = true;
        }
    }
}
