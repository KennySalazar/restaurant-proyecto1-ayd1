package com.restaurante.web.dto.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;


public record InvoiceResponse(
        Long facturaId,
        String numeroDocumento,
        String serie,
        Long numeroCorrelativo,
        String tipoDocumento,
        String estado,

        Long cuentaId,
        String numeroCuenta,

        String restauranteNombre,
        String restauranteNombreComercial,
        String identificacionFiscal,
        String direccion,
        String telefono,
        String correo,
        String moneda,

        String clienteNombre,
        String clienteApellido,

        BigDecimal subtotal,
        BigDecimal descuentoTotal,
        BigDecimal descuentoPuntos,
        BigDecimal porcentajeImpuesto,
        BigDecimal montoImpuesto,
        BigDecimal porcentajePropina,
        BigDecimal montoPropina,
        BigDecimal total,

        Long puntosRedimidos,
        Long puntosOtorgados,

        Instant emitidaEn,

        List<InvoiceDetailResponse> detalles,
        List<InvoicePaymentResponse> pagos
) {
}