package com.restaurante.application.report;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.WaiterPerformanceReportResponse;
import com.restaurante.web.dto.report.WaiterPerformanceResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class WaiterPerformanceReportService {

    private final EntityManager entityManager;

    public WaiterPerformanceReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public WaiterPerformanceReportResponse getReport(
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

        List<WaiterPerformanceResponse> waiters =
                entityManager
                        .createNativeQuery("""
        WITH ventas AS (
            SELECT
                f.mesero_id,
                COUNT(*) AS cantidad_ventas,
                SUM(f.total) AS monto_total_vendido
            FROM restaurante.facturas f
            JOIN restaurante.restaurantes r
              ON r.id = f.restaurante_id
            WHERE f.restaurante_id = :restaurantId
              AND f.estado = 'EMITIDA'
              AND (
                    f.emitida_en
                    AT TIME ZONE r.zona_horaria
                  )::date
                  BETWEEN CAST(:startDate AS date)
                      AND CAST(:endDate AS date)
            GROUP BY f.mesero_id
        ),
        calificaciones AS (
            SELECT
                cs.mesero_id,
                COUNT(*) AS cantidad_calificaciones,
                ROUND(
                    AVG(cs.calificacion),
                    2
                ) AS calificacion_promedio
            FROM restaurante.calificaciones_servicio cs
            JOIN restaurante.facturas f
              ON f.id = cs.factura_id
            JOIN restaurante.restaurantes r
              ON r.id = f.restaurante_id
            WHERE f.restaurante_id = :restaurantId
              AND (
                    cs.creada_en
                    AT TIME ZONE r.zona_horaria
                  )::date
                  BETWEEN CAST(:startDate AS date)
                      AND CAST(:endDate AS date)
            GROUP BY cs.mesero_id
        )
        SELECT
            u.id,
            CONCAT(
                u.nombres,
                ' ',
                u.apellidos
            ) AS mesero,
            v.cantidad_ventas,
            v.monto_total_vendido,
            COALESCE(
                c.cantidad_calificaciones,
                0
            ) AS cantidad_calificaciones,
            c.calificacion_promedio
        FROM ventas v
        JOIN restaurante.usuarios u
          ON u.id = v.mesero_id
        JOIN public.app_users au
          ON au.id = u.id
        JOIN public.roles ro
          ON ro.id = au.role_id
         AND ro.name = 'WAITER'
        LEFT JOIN calificaciones c
          ON c.mesero_id = u.id
        WHERE u.restaurante_id = :restaurantId
        ORDER BY
            v.monto_total_vendido DESC,
            u.id ASC
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

                            return new WaiterPerformanceResponse(
                                    ((Number) row[0]).longValue(),
                                    (String) row[1],
                                    ((Number) row[2]).longValue(),
                                    (BigDecimal) row[3],
                                    ((Number) row[4]).longValue(),
                                    row[5] == null
                                            ? null
                                            : (BigDecimal) row[5]
                            );
                        })
                        .toList();

        return new WaiterPerformanceReportResponse(
                startDate,
                endDate,
                !waiters.isEmpty(),
                waiters
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