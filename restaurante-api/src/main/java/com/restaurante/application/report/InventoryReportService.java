package com.restaurante.application.report;

import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.web.dto.report.*;
import jakarta.persistence.EntityManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryReportService {

    private final SupplyRepository supplyRepository;
    private final EntityManager entityManager;

    public InventoryReportService(
            SupplyRepository supplyRepository,
            EntityManager entityManager) {
        this.supplyRepository = supplyRepository;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Authentication authentication) {

        Long restaurantId = extractRestaurantId(authentication);
        return getInventoryReport(fechaInicio, fechaFin, restaurantId);
    }

    @Transactional(readOnly = true)
    public InventoryReportResponse getInventoryReport(
            LocalDate fechaInicio,
            LocalDate fechaFin,
            Long restaurantId) {

        List<Supply> supplies = supplyRepository
                .findByRestaurantIdAndActiveTrueOrderByNameAsc(restaurantId);

        long totalInsumos = supplies.size();
        long totalInsumosSinCosto = 0;
        BigDecimal valorTotalSum = BigDecimal.ZERO;

        boolean allStocksZero = !supplies.isEmpty() && supplies.stream()
                .allMatch(s -> s.getCurrentStock() == null
                        || s.getCurrentStock().compareTo(BigDecimal.ZERO) == 0);

        List<InventorySupplyValuationResponse> valoracionInventario = new ArrayList<>();

        for (Supply s : supplies) {
            BigDecimal stock = s.getCurrentStock() != null ? s.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal minStock = s.getMinimumStock() != null ? s.getMinimumStock() : BigDecimal.ZERO;
            BigDecimal unitCost = s.getCurrentUnitCost();
            String catName = s.getCategory() != null ? s.getCategory().getName() : "";
            String unit = s.getMeasurementUnit() != null ? s.getMeasurementUnit().getAbbreviation() : "";

            boolean hasStock = stock.compareTo(BigDecimal.ZERO) > 0;
            boolean hasValidCost = unitCost != null && unitCost.compareTo(BigDecimal.ZERO) > 0;

            boolean calculable;
            BigDecimal valorTotal;
            BigDecimal costoParaMostrar;
            String advertencia;

            if (hasStock && !hasValidCost) {
                calculable = false;
                valorTotal = null;
                costoParaMostrar = null;
                advertencia = "El insumo tiene existencias pero no tiene costo unitario registrado para el cálculo.";
                totalInsumosSinCosto++;
            } else if (!hasStock) {
                calculable = true;
                valorTotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                costoParaMostrar = hasValidCost ? unitCost.setScale(4, RoundingMode.HALF_UP) : null;
                advertencia = null;
            } else {
                calculable = true;
                costoParaMostrar = unitCost.setScale(4, RoundingMode.HALF_UP);
                valorTotal = stock.multiply(unitCost).setScale(2, RoundingMode.HALF_UP);
                advertencia = null;
                valorTotalSum = valorTotalSum.add(valorTotal);
            }

            valoracionInventario.add(new InventorySupplyValuationResponse(
                    s.getId(),
                    s.getCode(),
                    s.getName(),
                    catName,
                    stock.setScale(4, RoundingMode.HALF_UP),
                    minStock.setScale(4, RoundingMode.HALF_UP),
                    unit,
                    costoParaMostrar,
                    valorTotal,
                    calculable,
                    advertencia
            ));
        }

        BigDecimal valorTotalInventario;
        boolean valorTotalCompleto;
        String advertenciaValorIncompleto;

        if (supplies.isEmpty() || allStocksZero) {
            valorTotalInventario = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            valorTotalCompleto = true;
            advertenciaValorIncompleto = null;
        } else if (totalInsumosSinCosto > 0) {
            valorTotalInventario = valorTotalSum.setScale(2, RoundingMode.HALF_UP);
            valorTotalCompleto = false;
            advertenciaValorIncompleto = String.format(
                    "El valor total del inventario es incompleto porque existen %d insumo(s) con existencias sin costo registrado para el cálculo.",
                    totalInsumosSinCosto
            );
        } else {
            valorTotalInventario = valorTotalSum.setScale(2, RoundingMode.HALF_UP);
            valorTotalCompleto = true;
            advertenciaValorIncompleto = null;
        }

        List<Supply> lowStockSupplies = supplyRepository.findLowStockSupplies(restaurantId);
        List<InventoryLowStockItemResponse> insumosBajoStock = new ArrayList<>();

        for (Supply s : lowStockSupplies) {
            BigDecimal stock = s.getCurrentStock() != null ? s.getCurrentStock() : BigDecimal.ZERO;
            BigDecimal minStock = s.getMinimumStock() != null ? s.getMinimumStock() : BigDecimal.ZERO;
            BigDecimal deficit = minStock.subtract(stock);
            if (deficit.compareTo(BigDecimal.ZERO) < 0) {
                deficit = BigDecimal.ZERO;
            }
            String estado = stock.compareTo(BigDecimal.ZERO) <= 0 ? "AGOTADO" : "BAJO";
            String catName = s.getCategory() != null ? s.getCategory().getName() : "";
            String unit = s.getMeasurementUnit() != null ? s.getMeasurementUnit().getAbbreviation() : "";

            insumosBajoStock.add(new InventoryLowStockItemResponse(
                    s.getId(),
                    s.getCode(),
                    s.getName(),
                    catName,
                    stock.setScale(4, RoundingMode.HALF_UP),
                    minStock.setScale(4, RoundingMode.HALF_UP),
                    unit,
                    deficit.setScale(4, RoundingMode.HALF_UP),
                    estado
            ));
        }

        long totalInsumosBajoStock = insumosBajoStock.size();

        InventoryWasteSectionResponse seccionMermas;

        if (fechaInicio != null && fechaFin != null && fechaInicio.isAfter(fechaFin)) {
            seccionMermas = new InventoryWasteSectionResponse(
                    fechaInicio,
                    fechaFin,
                    false,
                    false,
                    "El período seleccionado no es válido: la fecha inicial no puede ser posterior a la fecha final",
                    0L,
                    BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                    BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                    List.of()
            );
        } else {
            LocalDate startDate = fechaInicio;
            LocalDate endDate = fechaFin;

            if (startDate == null && endDate == null) {
                startDate = LocalDate.now().withDayOfMonth(1);
                endDate = LocalDate.now();
            } else if (startDate == null) {
                startDate = endDate.withDayOfMonth(1);
            } else if (endDate == null) {
                endDate = LocalDate.now();
            }

            if (startDate.isAfter(endDate)) {
                seccionMermas = new InventoryWasteSectionResponse(
                        startDate,
                        endDate,
                        false,
                        false,
                        "El período seleccionado no es válido: la fecha inicial no puede ser posterior a la fecha final",
                        0L,
                        BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP),
                        BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP),
                        List.of()
                );
            } else {
                List<?> rows = entityManager.createNativeQuery("""
                        SELECT
                            vm.fuente,
                            vm.merma_id,
                            vm.cancelacion_comanda_detalle_id,
                            vm.numero_documento,
                            vm.registrada_en,
                            vm.insumo_id,
                            vm.insumo,
                            vm.categoria,
                            vm.cantidad,
                            vm.unidad,
                            vm.tipo_motivo,
                            vm.motivo_general,
                            vm.costo_unitario_snapshot,
                            vm.costo_total
                        FROM restaurante.vw_reporte_mermas vm
                        JOIN restaurante.restaurantes r
                          ON r.id = vm.restaurante_id
                        WHERE vm.restaurante_id = :restaurantId
                          AND (
                                vm.registrada_en
                                AT TIME ZONE r.zona_horaria
                              )::date
                              BETWEEN CAST(:startDate AS date)
                                  AND CAST(:endDate AS date)
                        ORDER BY vm.registrada_en DESC
                        """)
                        .setParameter("restaurantId", restaurantId)
                        .setParameter("startDate", startDate.toString())
                        .setParameter("endDate", endDate.toString())
                        .getResultList();

                List<InventoryWasteItemResponse> mermas = new ArrayList<>();
                BigDecimal cantidadTotal = BigDecimal.ZERO;
                BigDecimal costoTotal = BigDecimal.ZERO;

                for (Object item : rows) {
                    Object[] row = (Object[]) item;
                    String fuente = (String) row[0];
                    Long mermaId = row[1] != null ? ((Number) row[1]).longValue() : null;
                    Long cancelacionComandaDetalleId = row[2] != null ? ((Number) row[2]).longValue() : null;
                    String numeroDocumento = (String) row[3];

                    Instant fechaRegistro = null;
                    if (row[4] instanceof Instant inst) {
                        fechaRegistro = inst;
                    } else if (row[4] instanceof java.time.OffsetDateTime odt) {
                        fechaRegistro = odt.toInstant();
                    } else if (row[4] instanceof java.sql.Timestamp ts) {
                        fechaRegistro = ts.toInstant();
                    } else if (row[4] instanceof java.util.Date dt) {
                        fechaRegistro = dt.toInstant();
                    }

                    Long insumoId = row[5] != null ? ((Number) row[5]).longValue() : null;
                    String insumo = (String) row[6];
                    String categoria = (String) row[7];
                    BigDecimal cantidad = row[8] != null ? new BigDecimal(row[8].toString()).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    String unidad = (String) row[9];
                    String tipoMotivo = (String) row[10];
                    String motivoGeneral = (String) row[11];
                    BigDecimal costoUnitarioSnapshot = row[12] != null ? new BigDecimal(row[12].toString()).setScale(4, RoundingMode.HALF_UP) : BigDecimal.ZERO;
                    BigDecimal mermaCostoTotal = row[13] != null ? new BigDecimal(row[13].toString()).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

                    cantidadTotal = cantidadTotal.add(cantidad);
                    costoTotal = costoTotal.add(mermaCostoTotal);

                    mermas.add(new InventoryWasteItemResponse(
                            fuente,
                            mermaId,
                            cancelacionComandaDetalleId,
                            numeroDocumento,
                            fechaRegistro,
                            insumoId,
                            insumo,
                            categoria,
                            cantidad,
                            unidad,
                            tipoMotivo,
                            motivoGeneral,
                            costoUnitarioSnapshot,
                            mermaCostoTotal
                    ));
                }

                seccionMermas = new InventoryWasteSectionResponse(
                        startDate,
                        endDate,
                        true,
                        true,
                        null,
                        (long) mermas.size(),
                        cantidadTotal.setScale(4, RoundingMode.HALF_UP),
                        costoTotal.setScale(2, RoundingMode.HALF_UP),
                        mermas
                );
            }
        }

        return new InventoryReportResponse(
                LocalDate.now(),
                totalInsumos,
                totalInsumosBajoStock,
                totalInsumosSinCosto,
                valorTotalInventario,
                valorTotalCompleto,
                advertenciaValorIncompleto,
                valoracionInventario,
                insumosBajoStock,
                seccionMermas
        );
    }

    private Long extractRestaurantId(Authentication authentication) {
        Object result = entityManager
                .createNativeQuery("""
                        SELECT u.restaurante_id
                        FROM restaurante.usuarios u
                        JOIN public.app_users au
                          ON au.id = u.id
                        WHERE au.email = :email
                        """)
                .setParameter("email", authentication.getName())
                .getSingleResult();

        return ((Number) result).longValue();
    }
}
