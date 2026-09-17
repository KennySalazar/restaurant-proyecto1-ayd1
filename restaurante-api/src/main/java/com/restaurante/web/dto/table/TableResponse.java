package com.restaurante.web.dto.table;

import com.restaurante.domain.model.TableStatus;

public record TableResponse(
        Long id,
        String numero,
        Short capacidad,
        TableZoneResponse zona,
        TableStatus estado,
        boolean activo
) {
}