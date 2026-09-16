package com.restaurante.application.payment;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.invoice.InvoiceDetailResponse;
import com.restaurante.web.dto.invoice.InvoicePaymentResponse;
import com.restaurante.web.dto.invoice.InvoiceResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import com.restaurante.web.dto.invoice.InvoiceHistoryResponse;
import jakarta.persistence.Query;
import java.time.LocalDate;

@Service
public class InvoiceService {

    private final EntityManager entityManager;

    public InvoiceService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(Long invoiceId) {

        List<?> rows = entityManager
                .createNativeQuery("""
                        SELECT
                            f.id,
                            f.numero_documento,
                            f.serie,
                            f.numero_correlativo,
                            f.tipo_documento,
                            f.estado,
                            f.cuenta_id,
                            c.numero_cuenta,
                            r.nombre,
                            r.nombre_comercial,
                            r.identificacion_fiscal,
                            r.direccion,
                            r.telefono,
                            r.correo,
                            r.moneda,
                            cl.nombres,
                            cl.apellidos,
                            f.subtotal,
                            f.descuento_total,
                            f.descuento_puntos,
                            f.porcentaje_impuesto,
                            f.monto_impuesto,
                            f.porcentaje_propina,
                            f.monto_propina,
                            f.total,
                            f.puntos_redimidos,
                            f.puntos_otorgados,
                            f.emitida_en
                        FROM restaurante.facturas f
                        JOIN restaurante.cuentas c
                          ON c.id = f.cuenta_id
                        JOIN restaurante.restaurantes r
                          ON r.id = f.restaurante_id
                        LEFT JOIN restaurante.clientes cl
                          ON cl.id = f.cliente_id
                        WHERE f.id = :invoiceId
                          AND f.estado = 'EMITIDA'
                        """)
                .setParameter("invoiceId", invoiceId)
                .getResultList();

        if (rows.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "invoice_not_found",
                    "Comprobante no encontrado",
                    "La factura indicada no existe o no se encuentra emitida"
            );
        }

        Object[] row = (Object[]) rows.get(0);

        List<InvoiceDetailResponse> details =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    fd.id,
                                    fd.nombre_snapshot,
                                    fd.cantidad,
                                    fd.precio_unitario_snapshot,
                                    fd.total_modificadores_snapshot,
                                    fd.subtotal_linea
                                FROM restaurante.factura_detalles fd
                                WHERE fd.factura_id = :invoiceId
                                ORDER BY fd.id
                                """)
                        .setParameter("invoiceId", invoiceId)
                        .getResultList()
                        .stream()
                        .map(value -> {
                            Object[] d = (Object[]) value;

                            return new InvoiceDetailResponse(
                                    ((Number) d[0]).longValue(),
                                    (String) d[1],
                                    (BigDecimal) d[2],
                                    (BigDecimal) d[3],
                                    (BigDecimal) d[4],
                                    (BigDecimal) d[5]
                            );
                        })
                        .toList();

        List<InvoicePaymentResponse> payments =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    p.id,
                                    mp.codigo,
                                    mp.nombre,
                                    p.monto,
                                    p.monto_recibido,
                                    p.cambio_entregado,
                                    p.referencia,
                                    p.autorizacion
                                FROM restaurante.pagos p
                                JOIN restaurante.metodos_pago mp
                                  ON mp.id = p.metodo_pago_id
                                WHERE p.factura_id = :invoiceId
                                ORDER BY p.id
                                """)
                        .setParameter("invoiceId", invoiceId)
                        .getResultList()
                        .stream()
                        .map(value -> {
                            Object[] p = (Object[]) value;

                            return new InvoicePaymentResponse(
                                    ((Number) p[0]).longValue(),
                                    (String) p[1],
                                    (String) p[2],
                                    (BigDecimal) p[3],
                                    (BigDecimal) p[4],
                                    (BigDecimal) p[5],
                                    (String) p[6],
                                    (String) p[7]
                            );
                        })
                        .toList();

        return new InvoiceResponse(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue(),
                (String) row[4],
                (String) row[5],
                ((Number) row[6]).longValue(),
                (String) row[7],
                (String) row[8],
                (String) row[9],
                (String) row[10],
                (String) row[11],
                (String) row[12],
                (String) row[13],
                (String) row[14],
                (String) row[15],
                (String) row[16],
                (BigDecimal) row[17],
                (BigDecimal) row[18],
                (BigDecimal) row[19],
                (BigDecimal) row[20],
                (BigDecimal) row[21],
                (BigDecimal) row[22],
                (BigDecimal) row[23],
                (BigDecimal) row[24],
                ((Number) row[25]).longValue(),
                ((Number) row[26]).longValue(),
                ((java.time.Instant) row[27]),
                details,
                payments
        );
    }

    @Transactional(readOnly = true)
    public List<InvoiceHistoryResponse> getInvoiceHistory(
            LocalDate date,
            Long tableId,
            Long waiterId) {

        StringBuilder sql = new StringBuilder("""
            SELECT
                f.id,
                f.numero_documento,
                f.emitida_en,
                f.cuenta_id,
                c.numero_cuenta,
                m.id,
                m.numero,
                u.id,
                u.nombres,
                u.apellidos,
                cl.nombres,
                cl.apellidos,
                f.subtotal,
                f.descuento_total,
                f.monto_impuesto,
                f.monto_propina,
                f.total,
                f.estado
            FROM restaurante.facturas f
            JOIN restaurante.cuentas c
              ON c.id = f.cuenta_id
            LEFT JOIN restaurante.mesas m
              ON m.id = c.mesa_id
            JOIN restaurante.usuarios u
              ON u.id = f.mesero_id
            JOIN restaurante.restaurantes r
              ON r.id = f.restaurante_id
            LEFT JOIN restaurante.clientes cl
              ON cl.id = f.cliente_id
            WHERE f.estado = 'EMITIDA'
            """);

        if (date != null) {
            sql.append("""
            AND (f.emitida_en AT TIME ZONE r.zona_horaria)::date
                = CAST(:date AS date)
            """);
        }

        if (tableId != null) {
            sql.append("""
                AND c.mesa_id = :tableId
                """);
        }

        if (waiterId != null) {
            sql.append("""
                AND f.mesero_id = :waiterId
                """);
        }

        sql.append("""
            ORDER BY f.emitida_en DESC, f.id DESC
            """);

        Query query = entityManager
                .createNativeQuery(sql.toString());

        if (date != null) {
            query.setParameter("date", date.toString());
        }

        if (tableId != null) {
            query.setParameter("tableId", tableId);
        }

        if (waiterId != null) {
            query.setParameter("waiterId", waiterId);
        }

        List<?> rows = query.getResultList();

        return rows.stream()
                .map(value -> {
                    Object[] row = (Object[]) value;

                    return new InvoiceHistoryResponse(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            (Instant) row[2],
                            ((Number) row[3]).longValue(),
                            (String) row[4],
                            row[5] == null
                                    ? null
                                    : ((Number) row[5]).longValue(),
                            (String) row[6],
                            ((Number) row[7]).longValue(),
                            (String) row[8],
                            (String) row[9],
                            (String) row[10],
                            (String) row[11],
                            (BigDecimal) row[12],
                            (BigDecimal) row[13],
                            (BigDecimal) row[14],
                            (BigDecimal) row[15],
                            (BigDecimal) row[16],
                            (String) row[17]
                    );
                })
                .toList();
    }
}