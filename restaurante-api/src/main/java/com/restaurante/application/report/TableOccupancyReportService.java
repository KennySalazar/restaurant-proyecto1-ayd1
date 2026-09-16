package com.restaurante.application.report;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.TableOccupancyReportResponse;
import com.restaurante.web.dto.report.TableOccupancySlotResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TableOccupancyReportService {

    private final EntityManager entityManager;

    public TableOccupancyReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public TableOccupancyReportResponse getReport(
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

        Long restaurantId = extractRestaurantId(authentication);

        List<TableOccupancySlotResponse> slots =
                entityManager
                        .createNativeQuery("""
                                WITH historial AS (
                                    SELECT
                                        h.id,
                                        h.mesa_id,
                                        h.estado_nuevo,
                                        h.cambiado_en,
                                        LEAD(h.cambiado_en) OVER (
                                            PARTITION BY h.mesa_id
                                            ORDER BY h.cambiado_en, h.id
                                        ) AS siguiente_cambio
                                    FROM restaurante.historial_estados_mesa h
                                    JOIN restaurante.mesas m
                                      ON m.id = h.mesa_id
                                    WHERE m.restaurante_id = :restaurantId
                                ),
                                ocupaciones AS (
                                    SELECT
                                        mesa_id,
                                        cambiado_en AS inicio,
                                        siguiente_cambio AS fin
                                    FROM historial
                                    WHERE estado_nuevo = 'OCUPADA'
                                      AND siguiente_cambio IS NOT NULL
                                ),
                                configuracion AS (
                                    SELECT zona_horaria
                                    FROM restaurante.restaurantes
                                    WHERE id = :restaurantId
                                ),
                                horas AS (
                                    SELECT generate_series(
                                        CAST(:startDate AS date)::timestamp,
                                        (
                                            CAST(:endDate AS date)
                                            + INTERVAL '1 day'
                                            - INTERVAL '1 hour'
                                        )::timestamp,
                                        INTERVAL '1 hour'
                                    ) AS hora_local
                                )
                                SELECT
                                    h.hora_local::date AS fecha,
                                    EXTRACT(
                                        HOUR FROM h.hora_local
                                    )::integer AS hora,
                                    COUNT(
                                        DISTINCT o.mesa_id
                                    ) AS mesas_ocupadas
                                FROM horas h
                                CROSS JOIN configuracion cfg
                                LEFT JOIN ocupaciones o
                                  ON (
                                        o.inicio
                                        AT TIME ZONE cfg.zona_horaria
                                     ) < h.hora_local + INTERVAL '1 hour'
                                 AND (
                                        o.fin
                                        AT TIME ZONE cfg.zona_horaria
                                     ) > h.hora_local
                                GROUP BY
                                    h.hora_local
                                HAVING COUNT(
                                    DISTINCT o.mesa_id
                                ) > 0
                                ORDER BY
                                    h.hora_local
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

                            return new TableOccupancySlotResponse(
                                    (LocalDate) row[0],
                                    ((Number) row[1]).intValue(),
                                    ((Number) row[2]).longValue()
                            );
                        })
                        .toList();

        return new TableOccupancyReportResponse(
                startDate,
                endDate,
                !slots.isEmpty(),
                slots
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