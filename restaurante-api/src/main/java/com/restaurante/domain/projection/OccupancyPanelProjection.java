package com.restaurante.domain.projection;

import java.time.Instant;

public interface OccupancyPanelProjection {

    Long getMesaId();

    Long getRestauranteId();

    String getNumero();

    Short getCapacidad();

    String getZona();

    String getEstadoVisual();

    Long getCuentaActualId();

    String getNumeroCuenta();

    Boolean getEsPrincipal();

    Long getReservaActualId();

    String getClienteReserva();

    Instant getReservaHasta();

    Long getListaEsperaActualId();

    String getClienteListaEspera();
}