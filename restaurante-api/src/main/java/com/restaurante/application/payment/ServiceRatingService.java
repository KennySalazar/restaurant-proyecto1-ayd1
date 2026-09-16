package com.restaurante.application.payment;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.rating.ServiceRatingRequest;
import com.restaurante.web.dto.rating.ServiceRatingResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class ServiceRatingService {

    private final EntityManager entityManager;

    public ServiceRatingService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public ServiceRatingResponse registerRating(
            Long invoiceId,
            ServiceRatingRequest request) {

        List<?> invoices = entityManager
                .createNativeQuery("""
                        SELECT
                            f.id,
                            f.numero_documento,
                            f.cuenta_id,
                            f.cliente_id,
                            f.mesero_id,
                            u.nombres,
                            u.apellidos,
                            f.estado,
                            c.estado
                        FROM restaurante.facturas f
                        JOIN restaurante.cuentas c
                          ON c.id = f.cuenta_id
                        JOIN restaurante.usuarios u
                          ON u.id = f.mesero_id
                        WHERE f.id = :invoiceId
                        """)
                .setParameter("invoiceId", invoiceId)
                .getResultList();

        if (invoices.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "invoice_not_found",
                    "Comprobante no encontrado",
                    "La factura indicada no existe"
            );
        }

        Object[] invoice = (Object[]) invoices.get(0);

        String invoiceStatus = (String) invoice[7];
        String accountStatus = (String) invoice[8];

        if (!"EMITIDA".equals(invoiceStatus)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "invoice_not_issued",
                    "Comprobante no emitido",
                    "Solo se puede calificar el servicio de una factura emitida"
            );
        }

        if (!"CERRADA".equals(accountStatus)) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "account_not_closed",
                    "Cuenta no cerrada",
                    "La calificacion solo puede registrarse despues de cerrar la cuenta"
            );
        }

        Number existing = (Number) entityManager
                .createNativeQuery("""
                        SELECT COUNT(*)
                        FROM restaurante.calificaciones_servicio
                        WHERE factura_id = :invoiceId
                        """)
                .setParameter("invoiceId", invoiceId)
                .getSingleResult();

        if (existing.longValue() > 0) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "service_rating_already_exists",
                    "Calificacion ya registrada",
                    "La factura ya posee una calificacion de servicio"
            );
        }

        String comment = request.comentario();

        if (comment != null) {
            comment = comment.trim();

            if (comment.isEmpty()) {
                comment = null;
            }
        }

        Object[] inserted = (Object[]) entityManager
                .createNativeQuery("""
                        INSERT INTO restaurante.calificaciones_servicio (
                            factura_id,
                            cuenta_id,
                            cliente_id,
                            mesero_id,
                            calificacion,
                            comentario
                        )
                        VALUES (
                            :invoiceId,
                            :accountId,
                            :customerId,
                            :waiterId,
                            :rating,
                            :comment
                        )
                        RETURNING
                            id,
                            factura_id,
                            cuenta_id,
                            cliente_id,
                            mesero_id,
                            calificacion,
                            comentario,
                            creada_en
                        """)
                .setParameter(
                        "invoiceId",
                        ((Number) invoice[0]).longValue()
                )
                .setParameter(
                        "accountId",
                        ((Number) invoice[2]).longValue()
                )
                .setParameter(
                        "customerId",
                        invoice[3]
                )
                .setParameter(
                        "waiterId",
                        ((Number) invoice[4]).longValue()
                )
                .setParameter(
                        "rating",
                        request.calificacion()
                )
                .setParameter(
                        "comment",
                        comment
                )
                .getSingleResult();

        return new ServiceRatingResponse(
                ((Number) inserted[0]).longValue(),
                ((Number) inserted[1]).longValue(),
                (String) invoice[1],
                ((Number) inserted[2]).longValue(),
                inserted[3] == null
                        ? null
                        : ((Number) inserted[3]).longValue(),
                ((Number) inserted[4]).longValue(),
                (String) invoice[5],
                (String) invoice[6],
                ((Number) inserted[5]).intValue(),
                (String) inserted[6],
                (Instant) inserted[7]
        );
    }
}