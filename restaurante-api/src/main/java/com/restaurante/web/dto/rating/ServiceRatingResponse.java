package com.restaurante.web.dto.rating;

import java.time.Instant;

public record ServiceRatingResponse(
        Long calificacionId,
        Long facturaId,
        String numeroDocumento,
        Long cuentaId,
        Long clienteId,
        Long meseroId,
        String meseroNombres,
        String meseroApellidos,
        Integer calificacion,
        String comentario,
        Instant creadaEn
) {
}