package com.restaurante.application.report;

import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.report.DishSalesReportItemResponse;
import com.restaurante.web.dto.report.DishSalesReportResponse;
import jakarta.persistence.EntityManager;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DishSalesReportService {

    private final EntityManager entityManager;

    public DishSalesReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public DishSalesReportResponse getReport(
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

        List<DishSalesReportItemResponse> dishes =
                entityManager
                        .createNativeQuery("""
                                SELECT
                                    p.id,
                                    p.codigo,
                                    p.nombre,
                                    cp.id,
                                    cp.nombre,
                                    COALESCE(SUM(
                                        CASE
                                            WHEN (
                                                f.emitida_en
                                                AT TIME ZONE r.zona_horaria
                                            )::date
                                            BETWEEN CAST(:startDate AS date)
                                                AND CAST(:endDate AS date)
                                            THEN fd.cantidad
                                            ELSE 0
                                        END
                                    ), 0) AS cantidad_vendida
                                FROM restaurante.platillos p
                                JOIN restaurante.categorias_platillo cp
                                  ON cp.id = p.categoria_platillo_id
                                JOIN restaurante.restaurantes r
                                  ON r.id = p.restaurante_id
                                LEFT JOIN restaurante.factura_detalles fd
                                  ON fd.platillo_id = p.id
                                LEFT JOIN restaurante.facturas f
                                  ON f.id = fd.factura_id
                                 AND f.estado = 'EMITIDA'
                                WHERE p.restaurante_id = :restaurantId
                                  AND p.activo = TRUE
                                GROUP BY
                                    p.id,
                                    p.codigo,
                                    p.nombre,
                                    cp.id,
                                    cp.nombre
                                """)
                        .setParameter("restaurantId", restaurantId)
                        .setParameter("startDate", startDate.toString())
                        .setParameter("endDate", endDate.toString())
                        .getResultList()
                        .stream()
                        .map(value -> {
                            Object[] row = (Object[]) value;

                            return new DishSalesReportItemResponse(
                                    ((Number) row[0]).longValue(),
                                    (String) row[1],
                                    (String) row[2],
                                    ((Number) row[3]).longValue(),
                                    (String) row[4],
                                    (BigDecimal) row[5]
                            );
                        })
                        .toList();

        List<DishSalesReportItemResponse> mostSold =
                dishes.stream()
                        .sorted((a, b) -> {
                            int comparison =
                                    b.cantidadVendida()
                                            .compareTo(a.cantidadVendida());

                            if (comparison != 0) {
                                return comparison;
                            }

                            return a.nombre()
                                    .compareToIgnoreCase(b.nombre());
                        })
                        .toList();

        List<DishSalesReportItemResponse> leastSold =
                dishes.stream()
                        .sorted((a, b) -> {
                            int comparison =
                                    a.cantidadVendida()
                                            .compareTo(b.cantidadVendida());

                            if (comparison != 0) {
                                return comparison;
                            }

                            return a.nombre()
                                    .compareToIgnoreCase(b.nombre());
                        })
                        .toList();

        boolean hadSales = dishes.stream()
                .anyMatch(dish ->
                        dish.cantidadVendida()
                                .compareTo(BigDecimal.ZERO) > 0
                );

        return new DishSalesReportResponse(
                startDate,
                endDate,
                hadSales,
                mostSold,
                leastSold
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