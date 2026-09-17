package com.restaurante.web.dto.cash;

import java.math.BigDecimal;
import java.time.Instant;

public record CurrentCashShiftResponse(
        Long id,
        Long cajaId,
        String codigoCaja,
        String nombreCaja,
        String estado,
        BigDecimal montoInicialEfectivo,
        BigDecimal efectivoEsperado,
        Instant abiertaEn
) {
}