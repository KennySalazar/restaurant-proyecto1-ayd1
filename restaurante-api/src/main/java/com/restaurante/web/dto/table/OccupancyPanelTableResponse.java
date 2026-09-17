package com.restaurante.web.dto.table;

import java.time.OffsetDateTime;

public record OccupancyPanelTableResponse(
        Long mesaId,
        String numero,
        Short capacidad,
        String zona,
        String estado,
        Long cuentaActualId,
        String numeroCuenta,
        Boolean principal,
        Long reservaActualId,
        String clienteReserva,
        OffsetDateTime reservaHasta,
        Long listaEsperaActualId,
        String clienteListaEspera
) {
}