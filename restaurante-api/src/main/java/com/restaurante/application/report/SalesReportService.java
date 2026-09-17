package com.restaurante.application.report;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.SalesByCategoryResponse;
import com.restaurante.web.dto.report.SalesByWaiterResponse;
import com.restaurante.web.dto.report.SalesReportResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class SalesReportService {

    private final EntityManager entityManager;

    public SalesReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public SalesReportResponse getReport(
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

        Object[] summary = (Object[]) entityManager
                .createNativeQuery("""
                        SELECT
                            COUNT(f.id),
                            COALESCE(SUM(f.total), 0)
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
                        """)
                .setParameter("restaurantId", restaurantId)
                .setParameter("startDate", startDate.toString())
                .setParameter("endDate", endDate.toString())
                .getSingleResult();

        long salesCount =
                ((Number) summary[0]).longValue();

        BigDecimal totalSold =
                (BigDecimal) summary[1];

        List<SalesByCategoryResponse> byCategory =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    cp.id,
                                    COALESCE(
                                        vd.categoria,
                                        cp.nombre,
                                        CASE
                                            WHEN vd.combo_id IS NOT NULL
                                            THEN 'Combo'
                                            ELSE 'Sin categoria'
                                        END
                                    ) AS categoria,
                                    SUM(vd.cantidad) AS cantidad_vendida,
                                    COALESCE(
                                        SUM(vd.subtotal_linea),
                                        0
                                    ) AS monto_vendido
                                FROM restaurante.vw_ventas_detalladas vd
                                JOIN restaurante.restaurantes r
                                  ON r.id = vd.restaurante_id
                                LEFT JOIN restaurante.platillos p
                                  ON p.id = vd.platillo_id
                                LEFT JOIN restaurante.categorias_platillo cp
                                  ON cp.id = p.categoria_platillo_id
                                WHERE vd.restaurante_id = :restaurantId
                                  AND (
                                        vd.emitida_en
                                        AT TIME ZONE r.zona_horaria
                                      )::date
                                      BETWEEN CAST(:startDate AS date)
                                          AND CAST(:endDate AS date)
                                GROUP BY
                                    cp.id,
                                    COALESCE(
                                        vd.categoria,
                                        cp.nombre,
                                        CASE
                                            WHEN vd.combo_id IS NOT NULL
                                            THEN 'Combo'
                                            ELSE 'Sin categoria'
                                        END
                                    )
                                ORDER BY
                                    monto_vendido DESC,
                                    categoria ASC
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

                            return new SalesByCategoryResponse(
                                    row[0] == null
                                            ? null
                                            : ((Number) row[0]).longValue(),
                                    (String) row[1],
                                    (BigDecimal) row[2],
                                    (BigDecimal) row[3]
                            );
                        })
                        .toList();

        List<SalesByWaiterResponse> byWaiter =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    f.mesero_id,
                                    CONCAT(
                                        u.nombres,
                                        ' ',
                                        u.apellidos
                                    ) AS mesero,
                                    COUNT(f.id) AS cantidad_ventas,
                                    COALESCE(
                                        SUM(f.total),
                                        0
                                    ) AS monto_vendido
                                FROM restaurante.facturas f
                                JOIN restaurante.usuarios u
                                  ON u.id = f.mesero_id
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
                                GROUP BY
                                    f.mesero_id,
                                    u.nombres,
                                    u.apellidos
                                ORDER BY
                                    monto_vendido DESC,
                                    f.mesero_id ASC
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

                            return new SalesByWaiterResponse(
                                    ((Number) row[0]).longValue(),
                                    (String) row[1],
                                    ((Number) row[2]).longValue(),
                                    (BigDecimal) row[3]
                            );
                        })
                        .toList();

        return new SalesReportResponse(
                startDate,
                endDate,
                salesCount,
                totalSold,
                byCategory,
                byWaiter
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