package com.restaurante.web.dto.account;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.restaurante.web.dto.comanda.CreateComandaItemRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Solicitud para registrar una nueva ronda de platillos en una cuenta abierta.
 */
@Schema(description = "Solicitud para registrar una ronda adicional de platillos en una cuenta abierta")
public record CreateAccountRoundRequest(
        @Schema(description = "Notas generales de la nueva ronda", example = "Segunda ronda de bebidas y postres")
        @JsonProperty("generalNotes")
        @JsonAlias({"notas", "notes", "observaciones", "notasGenerales"})
        String generalNotes,

        @Schema(description = "Indica si la nueva ronda debe enviarse a cocina y descontar inventario de inmediato", example = "true", defaultValue = "false")
        @JsonProperty("sendImmediately")
        @JsonAlias({"enviarInmediatamente", "enviar_inmediatamente", "enviar"})
        Boolean sendImmediately,

        @Schema(description = "Listado de platillos y/o combos que componen la nueva ronda", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "La ronda debe contener al menos un platillo o combo")
        @Valid
        @JsonProperty("items")
        @JsonAlias({"platillos", "detalles", "productos"})
        List<CreateComandaItemRequest> items
) {
}
