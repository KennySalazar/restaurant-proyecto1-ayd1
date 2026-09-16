package com.restaurante.application.report;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.LoyaltyFrequentCustomerResponse;
import com.restaurante.web.dto.report.LoyaltyReportResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LoyaltyReportService {

    private final EntityManager entityManager;

    public LoyaltyReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public LoyaltyReportResponse getReport(
            LocalDate startDate,
            LocalDate endDate,
            Authentication authentication) {

        if (startDate.isAfter(endDate)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_report_period",
                    "Periodo invalido",
                    "La fecha inicial no puede ser posterior a la fecha final"
            );
        }

        Long restaurantId =
                extractRestaurantId(authentication);

        Object[] points = (Object[]) entityManager
                .createNativeQuery("""
                        SELECT
                            COALESCE(SUM(
                                CASE
                                    WHEN mp.tipo = 'OTORGAMIENTO'
                                    THEN mp.cantidad_puntos
                                    ELSE 0
                                END
                            ), 0),
                            ABS(COALESCE(SUM(
                                CASE
                                    WHEN mp.tipo = 'REDENCION'
                                    THEN mp.cantidad_puntos
                                    ELSE 0
                                END
                            ), 0))
                        FROM restaurante.movimientos_puntos mp
                        JOIN restaurante.clientes c
                          ON c.id = mp.cliente_id
                        JOIN restaurante.restaurantes r
                          ON r.id = c.restaurante_id
                        LEFT JOIN restaurante.facturas f
                          ON f.id = mp.factura_id
                        WHERE c.restaurante_id = :restaurantId
                          AND (
                                mp.factura_id IS NULL
                                OR f.estado = 'EMITIDA'
                          )
                          AND (
                                mp.creado_en
                                AT TIME ZONE r.zona_horaria
                              )::date
                              BETWEEN CAST(:startDate AS date)
                                  AND CAST(:endDate AS date)
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("startDate", startDate.toString())
                .setParameter("endDate", endDate.toString())
                .getSingleResult();

        long pointsGranted =
                ((Number) points[0]).longValue();

        long pointsRedeemed =
                ((Number) points[1]).longValue();

        List<LoyaltyFrequentCustomerResponse> customers =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    c.id,
                                    c.nombres,
                                    c.apellidos,
                                    COUNT(cu.id) AS cantidad_visitas
                                FROM restaurante.clientes c
                                JOIN restaurante.cuentas cu
                                  ON cu.cliente_id = c.id
                                JOIN restaurante.restaurantes r
                                  ON r.id = c.restaurante_id
                                WHERE c.restaurante_id = :restaurantId
                                  AND cu.estado = 'CERRADA'
                                  AND (
                                        cu.cerrada_en
                                        AT TIME ZONE r.zona_horaria
                                      )::date
                                      BETWEEN CAST(:startDate AS date)
                                          AND CAST(:endDate AS date)
                                GROUP BY
                                    c.id,
                                    c.nombres,
                                    c.apellidos
                                ORDER BY
                                    COUNT(cu.id) DESC,
                                    c.id ASC
                                """)
                        .setParameter(
                                "restaurantId",
                                restaurantId
                        )
                        .setParameter(
                                "startDate",
                                startDate.toString()
                        )
                        .setParameter(
                                "endDate",
                                endDate.toString()
                        )
                        .getResultList()
                        .stream()
                        .map(value -> {
                            Object[] row = (Object[]) value;

                            return new LoyaltyFrequentCustomerResponse(
                                    ((Number) row[0]).longValue(),
                                    (String) row[1],
                                    (String) row[2],
                                    ((Number) row[3]).longValue()
                            );
                        })
                        .toList();

        return new LoyaltyReportResponse(
                startDate,
                endDate,
                pointsGranted,
                pointsRedeemed,
                customers
        );
    }

    private Long extractRestaurantId(
            Authentication authentication) {

        Object result = entityManager
                .createNativeQuery("""
                        SELECT u.restaurante_id
                        FROM restaurante.usuarios u
                        JOIN public.app_users au
                          ON au.id = u.id
                        WHERE au.email = :email
                        """)
                .setParameter(
                        "email",
                        authentication.getName()
                )
                .getSingleResult();

        return ((Number) result).longValue();
    }
}