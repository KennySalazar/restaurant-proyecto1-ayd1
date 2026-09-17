package com.restaurante.application.report;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.CurrentDishProfitabilityResponse;
import com.restaurante.web.dto.report.HistoricalDishProfitabilityResponse;
import com.restaurante.web.dto.report.HistoricalProfitabilityReportResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
public class ProfitabilityReportService {

    private final EntityManager entityManager;

    public ProfitabilityReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public List<CurrentDishProfitabilityResponse> getCurrentProfitability(
            Authentication authentication) {

        Long restaurantId = extractRestaurantId(authentication);

        List<?> rows = entityManager
                .createNativeQuery("""
                        SELECT
                            v.platillo_id,
                            v.codigo,
                            v.platillo,
                            v.categoria,
                            v.precio_venta,
                            v.costo_receta_actual,
                            v.margen_bruto,
                            v.porcentaje_margen,
                            v.receta_version_id,
                            CASE
                                WHEN v.receta_version_id IS NULL THEN FALSE
                                WHEN NOT EXISTS (
                                    SELECT 1
                                    FROM restaurante.receta_detalles rd
                                    WHERE rd.receta_version_id = v.receta_version_id
                                ) THEN FALSE
                                WHEN EXISTS (
                                    SELECT 1
                                    FROM restaurante.receta_detalles rd
                                    JOIN restaurante.insumos i
                                      ON i.id = rd.insumo_id
                                    WHERE rd.receta_version_id = v.receta_version_id
                                      AND i.costo_unitario_actual IS NULL
                                ) THEN FALSE
                                ELSE TRUE
                            END AS calculable
                        FROM restaurante.vw_costos_platillo v
                        WHERE v.restaurante_id = :restaurantId
                        ORDER BY v.platillo
                        """)
                .setParameter("restaurantId", restaurantId)
                .getResultList();

        return rows.stream()
                .map(value -> {
                    Object[] row = (Object[]) value;

                    boolean calculable = (Boolean) row[9];

                    String reason = null;

                    if (!calculable) {
                        if (row[8] == null) {
                            reason = "El platillo no posee una receta vigente";
                        } else {
                            reason = "No es posible calcular el costo de produccion";
                        }
                    }

                    return new CurrentDishProfitabilityResponse(
                            ((Number) row[0]).longValue(),
                            (String) row[1],
                            (String) row[2],
                            (String) row[3],
                            (BigDecimal) row[4],
                            calculable ? (BigDecimal) row[5] : null,
                            calculable ? (BigDecimal) row[6] : null,
                            calculable ? (BigDecimal) row[7] : null,
                            calculable,
                            reason
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public HistoricalProfitabilityReportResponse getHistoricalProfitability(
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

        List<HistoricalDishProfitabilityResponse> dishes =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    fd.platillo_id,
                                    fd.nombre_snapshot,
                                    SUM(fd.cantidad) AS cantidad_vendida,
                                    SUM(
                                        fd.cantidad
                                        * fd.precio_unitario_snapshot
                                    ) AS ingreso_historico,
                                    SUM(
                                        fd.cantidad
                                        * fd.costo_unitario_snapshot
                                    ) AS costo_historico
                                FROM restaurante.factura_detalles fd
                                JOIN restaurante.facturas f
                                  ON f.id = fd.factura_id
                                JOIN restaurante.restaurantes r
                                  ON r.id = f.restaurante_id
                                WHERE f.restaurante_id = :restaurantId
                                  AND f.estado = 'EMITIDA'
                                  AND fd.platillo_id IS NOT NULL
                                  AND (
                                        f.emitida_en
                                        AT TIME ZONE r.zona_horaria
                                      )::date
                                      BETWEEN CAST(:startDate AS date)
                                          AND CAST(:endDate AS date)
                                GROUP BY
                                    fd.platillo_id,
                                    fd.nombre_snapshot
                                ORDER BY fd.nombre_snapshot
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

                            BigDecimal revenue =
                                    (BigDecimal) row[3];

                            BigDecimal cost =
                                    (BigDecimal) row[4];

                            BigDecimal profit =
                                    revenue.subtract(cost);

                            BigDecimal margin = null;

                            if (revenue.compareTo(BigDecimal.ZERO) != 0) {
                                margin = profit
                                        .divide(
                                                revenue,
                                                8,
                                                RoundingMode.HALF_UP
                                        )
                                        .multiply(
                                                new BigDecimal("100")
                                        )
                                        .setScale(
                                                4,
                                                RoundingMode.HALF_UP
                                        );
                            }

                            return new HistoricalDishProfitabilityResponse(
                                    ((Number) row[0]).longValue(),
                                    (String) row[1],
                                    (BigDecimal) row[2],
                                    revenue,
                                    cost,
                                    profit,
                                    margin
                            );
                        })
                        .toList();

        return new HistoricalProfitabilityReportResponse(
                startDate,
                endDate,
                !dishes.isEmpty(),
                dishes
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