package com.restaurante.application.inventory;

import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.InventoryMovement;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.InventoryMovementRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.kardex.KardexRecordResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

/**
 * Servicio de aplicación para la consulta y trazabilidad de movimientos en el kardex de inventario.
 */
@Service
public class KardexService {

    private static final Long DEFAULT_RESTAURANT_ID = 1L;

    private final InventoryMovementRepository inventoryMovementRepository;
    private final SupplyRepository supplyRepository;

    public KardexService(InventoryMovementRepository inventoryMovementRepository,
                         SupplyRepository supplyRepository) {
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.supplyRepository = supplyRepository;
    }

    /**
     * Consulta el historial general de movimientos del kardex con filtros opcionales.
     *
     * @param supplyId  Identificador opcional del insumo para filtrar sus movimientos
     * @param type      Tipo de movimiento opcional (ENTRADA_COMPRA, SALIDA_VENTA, SALIDA_MERMA, AJUSTE, etc.)
     * @param startDate Fecha inicial del rango de consulta
     * @param endDate   Fecha final del rango de consulta
     * @return Listado de movimientos ordenados cronológicamente descendente
     */
    @Transactional(readOnly = true)
    public List<KardexRecordResponse> listKardexMovements(Long supplyId, String type, LocalDate startDate, LocalDate endDate) {
        Long restaurantId = DEFAULT_RESTAURANT_ID;

        List<InventoryMovement> movements;

        if (supplyId != null) {
            if (!supplyRepository.existsByIdAndRestaurantId(supplyId, restaurantId)) {
                throw new ApiException(
                        HttpStatus.NOT_FOUND,
                        "supply_not_found",
                        "Insumo no encontrado",
                        "No se encontró un insumo con el identificador " + supplyId
                );
            }
            movements = inventoryMovementRepository.findBySupplyIdWithDetails(restaurantId, supplyId);
        } else {
            movements = inventoryMovementRepository.findAllWithDetailsByRestaurantId(restaurantId);
        }

        Stream<InventoryMovement> stream = movements.stream();

        if (type != null && !type.isBlank()) {
            String trimmedType = type.trim();
            if ("AJUSTE".equalsIgnoreCase(trimmedType)) {
                stream = stream.filter(m -> m.getType() != null && m.getType().toUpperCase().startsWith("AJUSTE"));
            } else {
                stream = stream.filter(m -> m.getType() != null && m.getType().equalsIgnoreCase(trimmedType));
            }
        }

        if (startDate != null) {
            Instant startInstant = startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
            stream = stream.filter(m -> m.getCreatedAt() != null && !m.getCreatedAt().isBefore(startInstant));
        }

        if (endDate != null) {
            Instant endInstant = endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
            stream = stream.filter(m -> m.getCreatedAt() != null && !m.getCreatedAt().isAfter(endInstant));
        }

        return stream.map(this::mapToRecordResponse).toList();
    }

    /**
     * Consulta los movimientos de kardex correspondientes a un insumo específico.
     *
     * @param supplyId Identificador único del insumo
     * @return Listado de movimientos de entrada, salida y ajuste del insumo
     */
    @Transactional(readOnly = true)
    public List<KardexRecordResponse> getSupplyKardex(Long supplyId) {
        return listKardexMovements(supplyId, null, null, null);
    }

    private KardexRecordResponse mapToRecordResponse(InventoryMovement movement) {
        Supply supply = movement.getSupply();
        String supplyCode = supply != null ? supply.getCode() : null;
        String supplyName = supply != null ? supply.getName() : null;
        String unit = (supply != null && supply.getMeasurementUnit() != null)
                ? supply.getMeasurementUnit().getAbbreviation()
                : "u";

        String type = movement.getType();
        String typeDescription = resolveTypeDescription(type);
        String movementNature = resolveMovementNature(type);
        boolean isAdjustment = type != null && type.toUpperCase().startsWith("AJUSTE");
        String stockEffect = resolveStockEffect(type);

        BigDecimal unitCost = movement.getUnitCostSnapshot();
        BigDecimal totalCost = movement.getTotalCostSnapshot();
        if (totalCost == null && movement.getQuantity() != null && unitCost != null) {
            totalCost = movement.getQuantity().multiply(unitCost).setScale(4, RoundingMode.HALF_UP);
        }

        Long responsibleUserId = movement.getResponsibleUserId();
        String responsibleUserName = null;
        String responsibleUserCode = null;

        if (movement.getResponsibleUser() != null) {
            RestaurantUserProfile profile = movement.getResponsibleUser();
            responsibleUserName = (profile.getFirstName() + " " + profile.getLastName()).trim();
            responsibleUserCode = profile.getEmployeeCode();
        } else if (responsibleUserId != null) {
            responsibleUserName = "Usuario #" + responsibleUserId;
        }

        // Datos de comanda y mesero de origen
        Long comandaId = null;
        Long comandaDetailId = null;
        Short comandaRound = null;
        String orderItemName = null;
        Long comandaSenderId = null;
        String comandaSenderName = null;

        if (movement.getComandaDetail() != null) {
            ComandaDetail cd = movement.getComandaDetail();
            comandaDetailId = cd.getId();
            orderItemName = cd.getNameSnapshot();

            Comanda comanda = cd.getComanda();
            if (comanda != null) {
                comandaId = comanda.getId();
                comandaRound = comanda.getRoundNumber();
                comandaSenderId = comanda.getWaiterId();

                if (comanda.getWaiterUser() != null) {
                    RestaurantUserProfile waiter = comanda.getWaiterUser();
                    comandaSenderName = (waiter.getFirstName() + " " + waiter.getLastName()).trim();
                } else if (comandaSenderId != null) {
                    comandaSenderName = "Usuario #" + comandaSenderId;
                }
            }
        }

        // Datos de entrada de compras
        Long entryDetailId = movement.getEntryDetailId();
        String entryDocumentNumber = null;
        if (movement.getEntryDetail() != null && movement.getEntryDetail().getEntry() != null) {
            entryDocumentNumber = movement.getEntryDetail().getEntry().getDocumentNumber();
        }

        // Datos de merma
        Long wasteDetailId = movement.getWasteDetailId();
        String wasteDocumentNumber = null;
        if (movement.getWasteDetail() != null && movement.getWasteDetail().getWaste() != null) {
            wasteDocumentNumber = movement.getWasteDetail().getWaste().getDocumentNumber();
        }

        return new KardexRecordResponse(
                movement.getId(),
                supply != null ? supply.getId() : null,
                supplyCode,
                supplyName,
                unit,
                type,
                typeDescription,
                movementNature,
                isAdjustment,
                stockEffect,
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getResultingStock(),
                unitCost,
                totalCost,
                movement.getReason(),
                responsibleUserId,
                responsibleUserName,
                responsibleUserCode,
                movement.getCreatedAt(),
                comandaId,
                comandaDetailId,
                comandaRound,
                orderItemName,
                comandaSenderId,
                comandaSenderName,
                entryDetailId,
                entryDocumentNumber,
                wasteDetailId,
                wasteDocumentNumber
        );
    }

    private String resolveTypeDescription(String type) {
        if (type == null) {
            return "Movimiento de inventario";
        }
        return switch (type.toUpperCase()) {
            case "ENTRADA_COMPRA" -> "Entrada por compra";
            case "SALIDA_VENTA" -> "Salida por venta";
            case "SALIDA_MERMA" -> "Salida por merma";
            case "AJUSTE_ENTRADA" -> "Ajuste manual de inventario (Entrada)";
            case "AJUSTE_SALIDA" -> "Ajuste manual de inventario (Salida)";
            case "REINTEGRO_CANCELACION" -> "Reintegro por cancelación de comanda";
            default -> type;
        };
    }

    private String resolveMovementNature(String type) {
        if (type == null) {
            return "MOVIMIENTO";
        }
        String upper = type.toUpperCase();
        if (upper.startsWith("AJUSTE")) {
            return "AJUSTE";
        }
        if (upper.startsWith("ENTRADA") || upper.startsWith("REINTEGRO")) {
            return "ENTRADA";
        }
        if (upper.startsWith("SALIDA")) {
            return "SALIDA";
        }
        return "MOVIMIENTO";
    }

    private String resolveStockEffect(String type) {
        if (type == null) {
            return "NEUTRO";
        }
        return switch (type.toUpperCase()) {
            case "ENTRADA_COMPRA", "AJUSTE_ENTRADA", "REINTEGRO_CANCELACION" -> "AUMENTO";
            case "SALIDA_VENTA", "SALIDA_MERMA", "AJUSTE_SALIDA" -> "DISMINUCION";
            default -> "NEUTRO";
        };
    }
}
