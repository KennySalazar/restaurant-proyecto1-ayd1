package com.restaurante.application.supply;

import com.restaurante.domain.model.InventoryEntry;
import com.restaurante.domain.model.InventoryEntryDetail;
import com.restaurante.domain.model.InventoryWaste;
import com.restaurante.domain.model.InventoryWasteDetail;
import com.restaurante.domain.model.MeasurementUnit;
import com.restaurante.domain.model.Notification;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.model.SupplyCategory;
import com.restaurante.domain.repository.InventoryEntryDetailRepository;
import com.restaurante.domain.repository.InventoryEntryRepository;
import com.restaurante.domain.repository.InventoryWasteDetailRepository;
import com.restaurante.domain.repository.InventoryWasteRepository;
import com.restaurante.domain.repository.MeasurementUnitRepository;
import com.restaurante.domain.repository.NotificationRepository;
import com.restaurante.domain.repository.SupplyCategoryRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.supply.ConfigureStockLimitsRequest;
import com.restaurante.web.dto.supply.CreateSupplyEntryRequest;
import com.restaurante.web.dto.supply.CreateSupplyRequest;
import com.restaurante.web.dto.supply.CreateSupplyWasteRequest;
import com.restaurante.web.dto.supply.MeasurementUnitResponse;
import com.restaurante.web.dto.supply.SingleSupplyAlertStatusResponse;
import com.restaurante.web.dto.supply.SupplyAlertResponse;
import com.restaurante.web.dto.supply.SupplyAlertSummaryResponse;
import com.restaurante.web.dto.supply.SupplyCategoryResponse;
import com.restaurante.web.dto.supply.SupplyEntryRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyEntryResponse;
import com.restaurante.web.dto.supply.SupplyRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyResponse;
import com.restaurante.web.dto.supply.SupplyStockLimitsResponse;
import com.restaurante.web.dto.supply.SupplyUpdateResponse;
import com.restaurante.web.dto.supply.SupplyWasteRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyWasteReportItemResponse;
import com.restaurante.web.dto.supply.SupplyWasteResponse;
import com.restaurante.web.dto.supply.UpdateSupplyRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

/**
 * Service para la gestion del catalogo de insumos y materia prima
 */
@Service
public class SupplyService {

        // Identificador por defecto del restaurante principal en el sistema
        private static final Long DEFAULT_RESTAURANT_ID = 1L;
        private final SupplyRepository supplyRepository;
        private final SupplyCategoryRepository categoryRepository;
        private final MeasurementUnitRepository unitRepository;
        private final NotificationRepository notificationRepository;
        private final InventoryEntryRepository inventoryEntryRepository;
        private final InventoryEntryDetailRepository inventoryEntryDetailRepository;
        private final InventoryWasteRepository inventoryWasteRepository;
        private final InventoryWasteDetailRepository inventoryWasteDetailRepository;

        @PersistenceContext
        private EntityManager entityManager;

        public SupplyService(SupplyRepository supplyRepository,
                        SupplyCategoryRepository categoryRepository,
                        MeasurementUnitRepository unitRepository,
                        NotificationRepository notificationRepository,
                        InventoryEntryRepository inventoryEntryRepository,
                        InventoryEntryDetailRepository inventoryEntryDetailRepository,
                        InventoryWasteRepository inventoryWasteRepository,
                        InventoryWasteDetailRepository inventoryWasteDetailRepository) {
                this.supplyRepository = supplyRepository;
                this.categoryRepository = categoryRepository;
                this.unitRepository = unitRepository;
                this.notificationRepository = notificationRepository;
                this.inventoryEntryRepository = inventoryEntryRepository;
                this.inventoryEntryDetailRepository = inventoryEntryDetailRepository;
                this.inventoryWasteRepository = inventoryWasteRepository;
                this.inventoryWasteDetailRepository = inventoryWasteDetailRepository;
        }

        /**
         * registra un nuevo insumo con su informacion basica, categoria, unidad de
         * medida y costo de compra
         *
         * @param request Datos de registro del insumo
         * @return Confirmacion y datos del insumo guardado
         */
        @Transactional
        public SupplyRegistrationResponse registerSupply(CreateSupplyRequest request) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                // validando que el nombre no este duplicado en el mismo restaurante
                String trimmedName = request.name().trim();
                if (supplyRepository.existsByRestaurantIdAndNameIgnoreCase(restaurantId, trimmedName)) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "duplicate_supply_name",
                                        "Nombre duplicado",
                                        "Ya existe un insumo registrado con el nombre '" + trimmedName + "'");
                }

                // validando existencia y estado de la categoria
                SupplyCategory category = categoryRepository
                                .findByIdAndRestaurantIdAndActiveTrue(request.categoryId(), restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "category_not_found",
                                                "Categoría no válida",
                                                "La categoría de insumo seleccionada no existe o no se encuentra activa"));

                // validando existencia y estado de la unidad de medida
                MeasurementUnit unit = unitRepository
                                .findByIdAndActiveTrue(request.unitId())
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "unit_not_found",
                                                "Unidad de medida no válida",
                                                "La unidad de medida seleccionada no existe o no se encuentra activa"));

                // validando coherencia de stock minimo y maximo
                BigDecimal minStock = request.minimumStock() != null ? request.minimumStock() : BigDecimal.ZERO;
                BigDecimal maxStock = request.maximumStock();
                if (maxStock != null && maxStock.compareTo(minStock) < 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_stock_limits",
                                        "Límites de stock inválidos",
                                        "El stock máximo (" + maxStock + ") no puede ser menor al stock mínimo ("
                                                        + minStock + ")");
                }

                // resolviendo o autogenerando codigo de insumo unico
                String code = resolveSupplyCode(restaurantId, request.code());

                // construyendo la entidad con stock inicial en 0
                Supply supply = new Supply(
                                restaurantId,
                                category,
                                unit,
                                code,
                                trimmedName,
                                request.description() != null ? request.description().trim() : null,
                                request.unitCost(),
                                minStock,
                                maxStock);

                Supply saved = supplyRepository.save(supply);

                return new SupplyRegistrationResponse(
                                "Insumo registrado exitosamente",
                                mapToResponse(saved));
        }

        /**
         * Lista todos los insumos activos del restaurante, con soporte opcional de
         * filtro por categoria y busqueda
         *
         * @param categoryId Identificador opcional de la categoria
         * @param search     Termino opcional de busqueda por nombre o codigo
         * @return Listado de insumos que coinciden con los criterios
         */
        @Transactional(readOnly = true)
        public List<SupplyResponse> listSupplies(Long categoryId, String search) {
                String pattern = (search != null && !search.isBlank())
                                ? "%" + search.trim().toLowerCase() + "%"
                                : null;

                List<Supply> supplies;
                if (categoryId != null && pattern != null) {
                        supplies = supplyRepository.findByRestaurantIdAndActiveTrueAndCategoryIdAndSearchPattern(
                                        DEFAULT_RESTAURANT_ID, categoryId, pattern);
                } else if (categoryId != null) {
                        supplies = supplyRepository.findByRestaurantIdAndActiveTrueAndCategoryIdOrderByNameAsc(
                                        DEFAULT_RESTAURANT_ID, categoryId);
                } else if (pattern != null) {
                        supplies = supplyRepository.findByRestaurantIdAndActiveTrueAndSearchPattern(
                                        DEFAULT_RESTAURANT_ID, pattern);
                } else {
                        supplies = supplyRepository.findByRestaurantIdAndActiveTrueOrderByNameAsc(
                                        DEFAULT_RESTAURANT_ID);
                }

                return supplies.stream().map(this::mapToResponse).toList();
        }

        /**
         * Lista todos los insumos activos del restaurante ordenados por fecha de
         * creacion
         */
        @Transactional(readOnly = true)
        public List<SupplyResponse> listSupplies() {
                return listSupplies(null, null);
        }

        /**
         * Obtiene el detalle de un insumo especifico por su ID.
         *
         * @param id Identificador unico del insumo.
         * @return Detalle del insumo.
         */
        @Transactional(readOnly = true)
        public SupplyResponse getSupplyById(Long id) {
                return supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(id, DEFAULT_RESTAURANT_ID)
                                .map(this::mapToResponse)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador " + id));
        }

        /**
         * Actualiza la informacion de identificacion, clasificacion y costo de un
         * insumo registrado
         *
         * @param id      Identificador unico del insumo a modificar.
         * @param request Datos actualizados del insumo.
         * @return Confirmacion y datos actualizados del insumo.
         */
        @Transactional
        public SupplyUpdateResponse updateSupply(Long id, UpdateSupplyRequest request) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                // verificando existencia del insumo activo
                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(id, restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador " + id));

                // validando que el nuevo nombre no esté duplicado en otro insumo
                String trimmedName = request.name().trim();
                if (supplyRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(restaurantId, trimmedName, id)) {
                        throw new ApiException(
                                        HttpStatus.CONFLICT,
                                        "duplicate_supply_name",
                                        "Nombre duplicado",
                                        "Ya existe otro insumo registrado con el nombre '" + trimmedName + "'");
                }

                // validadnod existencia y estado de la nueva categoría
                SupplyCategory category = categoryRepository
                                .findByIdAndRestaurantIdAndActiveTrue(request.categoryId(), restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "category_not_found",
                                                "Categoría no válida",
                                                "La categoría de insumo seleccionada no existe o no se encuentra activa"));

                // validando existencia y estado de la unidad de medida
                MeasurementUnit unit = unitRepository
                                .findByIdAndActiveTrue(request.unitId())
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "unit_not_found",
                                                "Unidad de medida no válida",
                                                "La unidad de medida seleccionada no existe o no se encuentra activa"));

                // validando coherencia de stock mínimo y máximo
                BigDecimal minStock = request.minimumStock() != null ? request.minimumStock() : BigDecimal.ZERO;
                BigDecimal maxStock = request.maximumStock();
                if (maxStock != null && maxStock.compareTo(minStock) < 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_stock_limits",
                                        "Límites de stock inválidos",
                                        "El stock máximo (" + maxStock + ") no puede ser menor al stock mínimo ("
                                                        + minStock + ")");
                }

                // validandoo si se especifico uno nuevo
                if (request.code() != null && !request.code().isBlank()) {
                        String sanitizedCode = request.code().trim().toUpperCase();
                        if (supplyRepository.existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(restaurantId, sanitizedCode,
                                        id)) {
                                throw new ApiException(
                                                HttpStatus.CONFLICT,
                                                "duplicate_supply_code",
                                                "Código duplicado",
                                                "Ya existe otro insumo registrado con el código '" + sanitizedCode
                                                                + "'");
                        }
                        supply.setCode(sanitizedCode);
                }

                // habilitar permiso en sesión transaccional de PostgreSQL para actualizar
                // insumo
                entityManager.createNativeQuery(
                                "SELECT set_config('restaurante.permitir_actualizacion_stock', 'true', true)")
                                .getSingleResult();

                // registrar en historial de costos si el costo unitario cambió
                BigDecimal oldCost = supply.getCurrentUnitCost();
                BigDecimal newCost = request.unitCost();
                if (oldCost != null && newCost != null && oldCost.compareTo(newCost) != 0) {
                        entityManager.createNativeQuery(
                                        """
                                                        INSERT INTO restaurante.historial_costos_insumo (insumo_id, costo_anterior, costo_nuevo, motivo, vigente_desde)
                                                        VALUES (:insumoId, :costoAnterior, :costoNuevo, :motivo, CURRENT_TIMESTAMP)
                                                        """)
                                        .setParameter("insumoId", supply.getId())
                                        .setParameter("costoAnterior", oldCost)
                                        .setParameter("costoNuevo", newCost)
                                        .setParameter("motivo", "Actualización de costo de insumo por administrador")
                                        .executeUpdate();
                }

                // apliocar cambios a la entidad
                supply.setName(trimmedName);
                supply.setDescription(request.description() != null ? request.description().trim() : null);
                supply.setCategory(category);
                supply.setMeasurementUnit(unit);
                supply.setCurrentUnitCost(newCost);
                supply.setMinimumStock(minStock);
                supply.setMaximumStock(maxStock);

                Supply updated = supplyRepository.save(supply);
                syncSupplyLowStockAlert(updated);

                return new SupplyUpdateResponse(
                                "Insumo actualizado exitosamente",
                                mapToResponse(updated));
        }

        /**
         * Configura los límites de stock (mínimo y máximo) de un insumo registrado
         *
         * @param id      Identificador único del insumo
         * @param request Límites de stock a configurar
         * @return Confirmación y datos del insumo con los límites actualizados
         */
        @Transactional
        public SupplyStockLimitsResponse configureStockLimits(Long id, ConfigureStockLimitsRequest request) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                // verificando existencia del insumo activo
                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(id, restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador " + id));

                BigDecimal minStock = request.minimumStock();
                BigDecimal maxStock = request.maximumStock();

                // validando que el stock minimo sea obligatorio
                if (minStock == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_stock_limits",
                                        "Límites de stock inválidos",
                                        "El stock mínimo es obligatorio");
                }

                // validando que los limites no sean negativos
                if (minStock.compareTo(BigDecimal.ZERO) < 0
                                || (maxStock != null && maxStock.compareTo(BigDecimal.ZERO) < 0)) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_stock_limits",
                                        "Límites de stock inválidos",
                                        "Los límites de stock no pueden ser negativos");
                }

                // validando coherencia de stock minimo y maximo
                if (maxStock != null && maxStock.compareTo(minStock) < 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_stock_limits",
                                        "Límites de stock inválidos",
                                        "El stock máximo (" + maxStock + ") no puede ser menor al stock mínimo ("
                                                        + minStock + ")");
                }

                // habilitar permiso en sesión transaccional de PostgreSQL para actualizar
                // insumo
                entityManager.createNativeQuery(
                                "SELECT set_config('restaurante.permitir_actualizacion_stock', 'true', true)")
                                .getSingleResult();

                supply.setMinimumStock(minStock);
                supply.setMaximumStock(maxStock);

                Supply updated = supplyRepository.save(supply);
                syncSupplyLowStockAlert(updated);

                return new SupplyStockLimitsResponse(
                                "Límites de stock configurados exitosamente",
                                mapToResponse(updated));
        }

        /**
         * Consulta las alertas activas de inventario bajo, sincronizando el estado con
         * notificaciones
         *
         * @param categoryId Filtro opcional por categoría
         * @param level      Filtro opcional por severidad (BAJO, AGOTADO)
         * @return Lista de alertas de insumos con stock bajo
         */
        @Transactional
        public List<SupplyAlertResponse> listLowStockAlerts(Long categoryId, String level) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                List<Supply> lowStockSupplies = supplyRepository.findLowStockSupplies(restaurantId);

                // Sincronizar notificaciones de stock bajo
                syncLowStockNotifications(restaurantId, lowStockSupplies);

                return lowStockSupplies.stream()
                                .filter(s -> categoryId == null || s.getCategory().getId().equals(categoryId))
                                .map(this::mapToAlertResponse)
                                .filter(a -> level == null || level.isBlank()
                                                || a.alertLevel().equalsIgnoreCase(level.trim()))
                                .toList();
        }

        /**
         * Obtiene el resumen consolidado de alertas de inventario bajo
         *
         * @return Resumen con totales por severidad y listado detallado
         */
        @Transactional
        public SupplyAlertSummaryResponse getLowStockAlertSummary() {
                List<SupplyAlertResponse> alerts = listLowStockAlerts(null, null);
                int outOfStock = 0;
                int lowStock = 0;

                for (SupplyAlertResponse a : alerts) {
                        if ("AGOTADO".equalsIgnoreCase(a.alertLevel())) {
                                outOfStock++;
                        } else {
                                lowStock++;
                        }
                }

                return new SupplyAlertSummaryResponse(
                                alerts.size(),
                                outOfStock,
                                lowStock,
                                alerts);
        }

        /**
         * Consulta el estado de alerta de inventario bajo para un insumo específico
         *
         * @param supplyId Identificador único del insumo
         * @return Estado indicando si tiene alerta activa y el detalle correspondiente
         */
        @Transactional(readOnly = true)
        public SingleSupplyAlertStatusResponse getSupplyAlertStatus(Long supplyId) {
                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(supplyId, DEFAULT_RESTAURANT_ID)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador " + supplyId));

                boolean hasAlert = supply.getCurrentStock().compareTo(supply.getMinimumStock()) <= 0;
                SupplyAlertResponse alert = hasAlert ? mapToAlertResponse(supply) : null;

                return new SingleSupplyAlertStatusResponse(hasAlert, alert);
        }

        /**
         * Evalúa y sincroniza la alerta individual de un insumo tras cambios de stock o
         * límites
         */
        private void syncSupplyLowStockAlert(Supply supply) {
                Long restaurantId = supply.getRestaurantId();
                boolean isLowStock = supply.getCurrentStock().compareTo(supply.getMinimumStock()) <= 0;

                if (isLowStock) {
                        boolean exists = notificationRepository
                                        .existsByRestaurantIdAndTypeAndEntityAndEntityIdAndReadFalse(
                                                        restaurantId, "STOCK_BAJO", "INSUMO",
                                                        supply.getId().toString());

                        if (!exists) {
                                boolean isOutOfStock = supply.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0;
                                String message = isOutOfStock
                                                ? "El insumo " + supply.getName() + " se encuentra agotado"
                                                : "El insumo " + supply.getName() + " alcanzo el nivel minimo de stock";

                                Notification notification = new Notification(
                                                restaurantId,
                                                null,
                                                1L, // rol ADMIN
                                                "STOCK_BAJO",
                                                "Insumo con stock bajo",
                                                message,
                                                "INSUMO",
                                                supply.getId().toString(),
                                                isOutOfStock ? "CRITICA" : "ALTA");

                                notificationRepository.save(notification);
                        }
                } else {
                        // Si el insumo está por encima del stock mínimo, retirar la alerta activa
                        notificationRepository.markAsReadByEntity(
                                        restaurantId, "STOCK_BAJO", "INSUMO", supply.getId().toString(), Instant.now());
                }
        }

        /**
         * Sincroniza las alertas con la tabla notificaciones
         */
        private void syncLowStockNotifications(Long restaurantId, List<Supply> lowStockSupplies) {
                for (Supply supply : lowStockSupplies) {
                        boolean exists = notificationRepository
                                        .existsByRestaurantIdAndTypeAndEntityAndEntityIdAndReadFalse(
                                                        restaurantId, "STOCK_BAJO", "INSUMO",
                                                        supply.getId().toString());

                        if (!exists) {
                                boolean isOutOfStock = supply.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0;
                                String message = isOutOfStock
                                                ? "El insumo " + supply.getName() + " se encuentra agotado"
                                                : "El insumo " + supply.getName() + " alcanzo el nivel minimo de stock";

                                Notification notification = new Notification(
                                                restaurantId,
                                                null,
                                                1L,
                                                "STOCK_BAJO",
                                                "Insumo con stock bajo",
                                                message,
                                                "INSUMO",
                                                supply.getId().toString(),
                                                isOutOfStock ? "CRITICA" : "ALTA");

                                notificationRepository.save(notification);
                        }
                }
        }

        /**
         * Mapeador de un Supply a su DTO de alerta de stock bajo
         */
        private SupplyAlertResponse mapToAlertResponse(Supply supply) {
                BigDecimal current = supply.getCurrentStock();
                BigDecimal min = supply.getMinimumStock();
                BigDecimal deficit = min.compareTo(current) > 0 ? min.subtract(current) : BigDecimal.ZERO;
                boolean isOutOfStock = current.compareTo(BigDecimal.ZERO) <= 0;
                String level = isOutOfStock ? "AGOTADO" : "BAJO";
                String priority = isOutOfStock ? "CRITICA" : "ALTA";
                String message = isOutOfStock
                                ? "El insumo '" + supply.getName() + "' se encuentra agotado (stock actual: 0)"
                                : "El insumo '" + supply.getName() + "' ha alcanzado el nivel mínimo de stock (actual: "
                                                + current + ", mínimo: " + min + ")";

                return new SupplyAlertResponse(
                                supply.getId(),
                                supply.getCode(),
                                supply.getName(),
                                supply.getCategory().getId(),
                                supply.getCategory().getName(),
                                supply.getMeasurementUnit().getId(),
                                supply.getMeasurementUnit().getName(),
                                supply.getMeasurementUnit().getAbbreviation(),
                                current,
                                min,
                                supply.getMaximumStock(),
                                deficit,
                                level,
                                priority,
                                message,
                                supply.getUpdatedAt() != null ? supply.getUpdatedAt() : supply.getCreatedAt());
        }

        /**
         * lista las categorias de insumo disponibles
         */
        @Transactional(readOnly = true)
        public List<SupplyCategoryResponse> listCategories() {
                return categoryRepository
                                .findByRestaurantIdAndActiveTrueOrderByNameAsc(DEFAULT_RESTAURANT_ID)
                                .stream()
                                .map(category -> new SupplyCategoryResponse(
                                                category.getId(),
                                                category.getName(),
                                                category.getDescription()))
                                .toList();
        }

        /**
         * lista las unidades de medida activas del sistema
         */
        @Transactional(readOnly = true)
        public List<MeasurementUnitResponse> listMeasurementUnits() {
                return unitRepository
                                .findByActiveTrueOrderByNameAsc()
                                .stream()
                                .map(unit -> new MeasurementUnitResponse(
                                                unit.getId(),
                                                unit.getCode(),
                                                unit.getName(),
                                                unit.getAbbreviation(),
                                                unit.getDimension()))
                                .toList();
        }

        /**
         * resuelve el codigo a asignar: utiliza el provisto o autogenera uno secuencial
         * unico.
         */
        private String resolveSupplyCode(Long restaurantId, String providedCode) {
                if (providedCode != null && !providedCode.isBlank()) {
                        String sanitized = providedCode.trim().toUpperCase();
                        if (supplyRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, sanitized)) {
                                throw new ApiException(
                                                HttpStatus.CONFLICT,
                                                "duplicate_supply_code",
                                                "Código duplicado",
                                                "Ya existe un insumo registrado con el código '" + sanitized + "'");
                        }
                        return sanitized;
                }

                // generar codigo correlativo con formato INS-0001
                long nextIndex = supplyRepository.countByRestaurantId(restaurantId) + 1;
                String generated = String.format("INS-%04d", nextIndex);
                while (supplyRepository.existsByRestaurantIdAndCodeIgnoreCase(restaurantId, generated)) {
                        nextIndex++;
                        generated = String.format("INS-%04d", nextIndex);
                }
                return generated;
        }

        /**
         * Registra una entrada de inventario para un insumo recibido
         *
         * @param supplyId       Identificador del insumo (puede venir de la ruta URL o
         *                       del request)
         * @param request        Datos de la entrada (cantidad, costo, fecha, etc.)
         * @param authentication Información de autenticación del usuario administrador
         * @return Confirmación y detalle de la entrada registrada
         */
        @Transactional
        public SupplyEntryRegistrationResponse registerSupplyEntry(Long supplyId, CreateSupplyEntryRequest request,
                        Authentication authentication) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                Long targetSupplyId = supplyId != null ? supplyId : request.supplyId();
                if (targetSupplyId == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "missing_supply_id",
                                        "Insumo no especificado",
                                        "Debe especificar el identificador del insumo para registrar la entrada");
                }

                if (supplyId != null && request.supplyId() != null && !supplyId.equals(request.supplyId())) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "supply_id_mismatch",
                                        "Insumo no coincide",
                                        "El insumo de la ruta no coincide con el insumo del cuerpo de la solicitud");
                }

                // Validacion de cantidad
                if (request.quantity() == null || request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_quantity",
                                        "Cantidad no válida",
                                        "La cantidad debe ser mayor a cero");
                }

                // Validacion de costo unitario
                if (request.unitCost() == null || request.unitCost().compareTo(BigDecimal.ZERO) < 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_unit_cost",
                                        "Costo no válido",
                                        "El costo de compra no puede ser negativo");
                }

                // Validacion de fecha de recepcion
                if (request.date() == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "missing_date",
                                        "Fecha obligatoria",
                                        "La fecha de recepción es obligatoria");
                }

                if (request.date().isAfter(LocalDate.now())) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "future_date_not_allowed",
                                        "Fecha no válida",
                                        "La fecha de recepción no puede ser futura");
                }

                // Validación de coherencia de fecha de vencimiento si se provee
                if (request.expirationDate() != null && request.expirationDate().isBefore(request.date())) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_expiration_date",
                                        "Fecha de vencimiento no válida",
                                        "La fecha de vencimiento no puede ser anterior a la fecha de recepción");
                }

                // Verificando existencia del insumo activo
                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(targetSupplyId, restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador "
                                                                + targetSupplyId));

                BigDecimal previousStock = supply.getCurrentStock();
                Long receivedById = resolveAdminUserId(authentication);
                String documentNumber = generateDocumentNumber(restaurantId);

                Instant receptionInstant = request.date().atStartOfDay(ZoneOffset.UTC).toInstant();

                // Creando cabecera de entrada de inventario
                InventoryEntry entry = new InventoryEntry(
                                restaurantId,
                                documentNumber,
                                receptionInstant,
                                request.supplierName() != null ? request.supplierName().trim() : null,
                                request.purchaseReference() != null ? request.purchaseReference().trim() : null,
                                request.notes() != null ? request.notes().trim() : null,
                                receivedById);
                InventoryEntry savedEntry = inventoryEntryRepository.save(entry);

                // Creando detalle de la entrada
                InventoryEntryDetail detail = new InventoryEntryDetail(
                                savedEntry,
                                supply,
                                request.quantity(),
                                request.unitCost(),
                                request.batchNumber() != null ? request.batchNumber().trim() : null,
                                request.expirationDate());
                InventoryEntryDetail savedDetail = inventoryEntryDetailRepository.save(detail);

                // Forzar flush para ejecutar los triggers de base de datos que actualizan
                // kardex, stock y costo ponderado
                entityManager.flush();
                entityManager.refresh(supply);
                entityManager.refresh(savedDetail);

                // Sincronizar alertas de inventario bajo por si el stock disponible superó el
                // mínimo
                syncSupplyLowStockAlert(supply);

                BigDecimal totalCost = savedDetail.getTotalCost() != null
                                ? savedDetail.getTotalCost()
                                : request.quantity().multiply(request.unitCost()).setScale(4, RoundingMode.HALF_UP);

                SupplyEntryResponse entryResponse = new SupplyEntryResponse(
                                savedEntry.getId(),
                                savedDetail.getId(),
                                savedEntry.getDocumentNumber(),
                                supply.getId(),
                                supply.getCode(),
                                supply.getName(),
                                supply.getMeasurementUnit().getName(),
                                supply.getMeasurementUnit().getAbbreviation(),
                                savedDetail.getQuantity(),
                                savedDetail.getUnitCost(),
                                totalCost,
                                previousStock,
                                supply.getCurrentStock(),
                                supply.getCurrentUnitCost(),
                                request.date(),
                                savedEntry.getSupplierName(),
                                savedEntry.getPurchaseReference(),
                                savedDetail.getBatchNumber(),
                                savedDetail.getExpirationDate(),
                                savedEntry.getNotes(),
                                savedEntry.getCreatedAt());

                return new SupplyEntryRegistrationResponse(
                                "Entrada de inventario registrada exitosamente",
                                entryResponse);
        }

        /**
         * Consulta el historial de entradas de inventario registradas para un insumo
         *
         * @param supplyId Identificador único del insumo
         * @return Lista de entradas registradas para dicho insumo
         */
        @Transactional(readOnly = true)
        public List<SupplyEntryResponse> listSupplyEntries(Long supplyId) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(supplyId, restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador " + supplyId));

                return inventoryEntryDetailRepository
                                .findBySupplyIdOrderByDateDesc(supplyId)
                                .stream()
                                .map(detail -> mapToEntryResponse(detail, supply))
                                .toList();
        }

        private SupplyEntryResponse mapToEntryResponse(InventoryEntryDetail detail, Supply supply) {
                InventoryEntry entry = detail.getEntry();
                BigDecimal totalCost = detail.getTotalCost() != null
                                ? detail.getTotalCost()
                                : detail.getQuantity().multiply(detail.getUnitCost()).setScale(4, RoundingMode.HALF_UP);
                LocalDate date = entry.getReceptionDate() != null
                                ? entry.getReceptionDate().atZone(ZoneOffset.UTC).toLocalDate()
                                : null;

                return new SupplyEntryResponse(
                                entry.getId(),
                                detail.getId(),
                                entry.getDocumentNumber(),
                                supply.getId(),
                                supply.getCode(),
                                supply.getName(),
                                supply.getMeasurementUnit().getName(),
                                supply.getMeasurementUnit().getAbbreviation(),
                                detail.getQuantity(),
                                detail.getUnitCost(),
                                totalCost,
                                null,
                                supply.getCurrentStock(),
                                supply.getCurrentUnitCost(),
                                date,
                                entry.getSupplierName(),
                                entry.getPurchaseReference(),
                                detail.getBatchNumber(),
                                detail.getExpirationDate(),
                                entry.getNotes(),
                                entry.getCreatedAt());
        }

        private String generateDocumentNumber(Long restaurantId) {
                long count = inventoryEntryRepository.countByRestaurantId(restaurantId) + 1;
                String docNumber = String.format("ENT-%05d", count);
                while (inventoryEntryRepository.existsByRestaurantIdAndDocumentNumber(restaurantId, docNumber)) {
                        count++;
                        docNumber = String.format("ENT-%05d", count);
                }
                return docNumber;
        }

        private Long resolveAdminUserId(Authentication authentication) {
                if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
                        return jwtData.userId();
                }
                return 1L;
        }

        /**
         * mapeador de una entidad Supply en su DTO de respuesta
         */
        private SupplyResponse mapToResponse(Supply supply) {
                return new SupplyResponse(
                                supply.getId(),
                                supply.getRestaurantId(),
                                supply.getCode(),
                                supply.getName(),
                                supply.getDescription(),
                                supply.getCategory().getId(),
                                supply.getCategory().getName(),
                                supply.getMeasurementUnit().getId(),
                                supply.getMeasurementUnit().getName(),
                                supply.getMeasurementUnit().getAbbreviation(),
                                supply.getCurrentUnitCost(),
                                supply.getCurrentStock(),
                                supply.getMinimumStock(),
                                supply.getMaximumStock(),
                                supply.isActive(),
                                supply.getCreatedAt());
        }

        /**
         * Registra la baja de insumos por merma (vencimiento, daño, error de manejo,
         * etc)
         *
         * @param supplyId       Identificador del insumo (puede venir de la ruta URL o
         *                       del request)
         * @param request        Datos de la merma (cantidad, motivo, observaciones,
         *                       etc)
         * @param authentication Información de autenticación del usuario administrador
         * @return Confirmación y detalle de la merma registrada
         */
        @Transactional
        public SupplyWasteRegistrationResponse registerSupplyWaste(Long supplyId, CreateSupplyWasteRequest request,
                        Authentication authentication) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                Long targetSupplyId = supplyId != null ? supplyId : request.supplyId();
                if (targetSupplyId == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "missing_supply_id",
                                        "Insumo no especificado",
                                        "Debe seleccionar o indicar el insumo para registrar la merma");
                }

                if (supplyId != null && request.supplyId() != null && !supplyId.equals(request.supplyId())) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "supply_id_mismatch",
                                        "Insumo no coincide",
                                        "El insumo de la ruta no coincide con el insumo del cuerpo de la solicitud");
                }

                // Validación de cantidad obligatoria
                if (request.quantity() == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "missing_quantity",
                                        "Cantidad obligatoria",
                                        "La cantidad es obligatoria y debe ser mayor que cero");
                }

                // Validación de cantidad mayor que cero
                if (request.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "invalid_quantity",
                                        "Cantidad no válida",
                                        "La cantidad debe ser mayor que cero");
                }

                // Validación de motivo obligatorio
                WasteReason wasteReason = resolveWasteReason(request.reasonType(), request.reason());

                // Verificando existencia del insumo activo
                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(targetSupplyId, restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador "
                                                                + targetSupplyId));

                // Validación de existencias disponibles: la merma no puede exceder las
                // existencias disponibles
                BigDecimal currentStock = supply.getCurrentStock() != null ? supply.getCurrentStock() : BigDecimal.ZERO;
                if (request.quantity().compareTo(currentStock) > 0) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "quantity_exceeds_stock",
                                        "Cantidad excede existencias",
                                        "La cantidad excede las existencias disponibles (disponible: " + currentStock
                                                        + ")");
                }

                BigDecimal previousStock = currentStock;
                Long registeredById = resolveAdminUserId(authentication);
                String documentNumber = generateWasteDocumentNumber(restaurantId);

                Instant registeredInstant = request.date() != null
                                ? request.date().atStartOfDay(ZoneOffset.UTC).toInstant()
                                : Instant.now();

                // Creando cabecera de merma
                InventoryWaste waste = new InventoryWaste(
                                restaurantId,
                                documentNumber,
                                wasteReason.type(),
                                wasteReason.description(),
                                registeredById,
                                registeredInstant);
                InventoryWaste savedWaste = inventoryWasteRepository.save(waste);

                BigDecimal unitCostSnapshot = supply.getCurrentUnitCost() != null ? supply.getCurrentUnitCost()
                                : BigDecimal.ZERO;

                // Creando detalle de merma
                InventoryWasteDetail detail = new InventoryWasteDetail(
                                savedWaste,
                                supply,
                                request.quantity(),
                                unitCostSnapshot,
                                request.batchNumber() != null ? request.batchNumber().trim() : null,
                                request.notes() != null ? request.notes().trim() : null);
                InventoryWasteDetail savedDetail = inventoryWasteDetailRepository.save(detail);

                // Forzar flush para ejecutar los triggers de base de datos que reducen
                // existencias y registran kardex
                entityManager.flush();
                entityManager.refresh(supply);
                entityManager.refresh(savedDetail);

                // Sincronizar alertas de inventario bajo por si el stock disponible cayó al
                // nivel mínimo o por debajo
                syncSupplyLowStockAlert(supply);

                BigDecimal totalCost = savedDetail.getTotalCost() != null
                                ? savedDetail.getTotalCost()
                                : request.quantity().multiply(unitCostSnapshot).setScale(4, RoundingMode.HALF_UP);

                SupplyWasteResponse wasteResponse = new SupplyWasteResponse(
                                savedWaste.getId(),
                                savedDetail.getId(),
                                savedWaste.getDocumentNumber(),
                                supply.getId(),
                                supply.getCode(),
                                supply.getName(),
                                supply.getMeasurementUnit().getName(),
                                supply.getMeasurementUnit().getAbbreviation(),
                                savedDetail.getQuantity(),
                                savedDetail.getUnitCostSnapshot(),
                                totalCost,
                                previousStock,
                                supply.getCurrentStock(),
                                savedWaste.getReasonType(),
                                savedWaste.getGeneralReason(),
                                savedDetail.getBatchNumber(),
                                savedDetail.getNotes(),
                                savedWaste.getRegisteredAt());

                return new SupplyWasteRegistrationResponse(
                                "Merma de inventario registrada exitosamente",
                                wasteResponse);
        }

        /**
         * Consulta el historial de mermas registradas para un insumo específico
         *
         * @param supplyId Identificador único del insumo
         * @return Lista de mermas registradas para dicho insumo
         */
        @Transactional(readOnly = true)
        public List<SupplyWasteResponse> listSupplyWastes(Long supplyId) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                Supply supply = supplyRepository
                                .findByIdAndRestaurantIdAndActiveTrue(supplyId, restaurantId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "supply_not_found",
                                                "Insumo no encontrado",
                                                "No se encontró un insumo activo con el identificador " + supplyId));

                return inventoryWasteDetailRepository
                                .findBySupplyIdOrderByDateDesc(supplyId)
                                .stream()
                                .map(detail -> mapToWasteResponse(detail, supply))
                                .toList();
        }

        /**
         * Consulta la información consolidada del reporte de mermas para el restaurante
         * desde la vista vw_reporte_mermas
         *
         * @param supplyId  Filtro opcional por insumo
         * @param startDate Filtro opcional de fecha inicial
         * @param endDate   Filtro opcional de fecha final
         * @return Listado de filas consolidadas del reporte de mermas
         */
        @Transactional(readOnly = true)
        @SuppressWarnings("unchecked")
        public List<SupplyWasteReportItemResponse> listWasteReport(Long supplyId, LocalDate startDate,
                        LocalDate endDate) {
                Long restaurantId = DEFAULT_RESTAURANT_ID;

                StringBuilder sql = new StringBuilder("""
                                SELECT
                                    fuente,
                                    merma_id,
                                    numero_documento,
                                    tipo_motivo,
                                    motivo_general,
                                    registrada_en,
                                    insumo_id,
                                    insumo,
                                    categoria,
                                    cantidad,
                                    unidad,
                                    costo_unitario_snapshot,
                                    costo_total,
                                    registrada_por_id
                                FROM restaurante.vw_reporte_mermas
                                WHERE restaurante_id = :restaurantId
                                """);

                if (supplyId != null) {
                        sql.append(" AND insumo_id = :supplyId ");
                }
                if (startDate != null) {
                        sql.append(" AND registrada_en >= :startDate ");
                }
                if (endDate != null) {
                        sql.append(" AND registrada_en <= :endDate ");
                }
                sql.append(" ORDER BY registrada_en DESC ");

                var query = entityManager.createNativeQuery(sql.toString());
                query.setParameter("restaurantId", restaurantId);
                if (supplyId != null) {
                        query.setParameter("supplyId", supplyId);
                }
                if (startDate != null) {
                        query.setParameter("startDate", startDate.atStartOfDay(ZoneOffset.UTC).toInstant());
                }
                if (endDate != null) {
                        query.setParameter("endDate", endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant());
                }

                List<Object[]> rows = query.getResultList();
                return rows.stream().map(row -> new SupplyWasteReportItemResponse(
                                (String) row[0],
                                row[1] != null ? ((Number) row[1]).longValue() : null,
                                (String) row[2],
                                (String) row[3],
                                (String) row[4],
                                toInstant(row[5]),
                                ((Number) row[6]).longValue(),
                                (String) row[7],
                                (String) row[8],
                                row[9] != null ? new BigDecimal(row[9].toString()) : BigDecimal.ZERO,
                                (String) row[10],
                                row[11] != null ? new BigDecimal(row[11].toString()) : BigDecimal.ZERO,
                                row[12] != null ? new BigDecimal(row[12].toString()) : BigDecimal.ZERO,
                                row[13] != null ? ((Number) row[13]).longValue() : null)).toList();
        }

        private String generateWasteDocumentNumber(Long restaurantId) {
                long count = inventoryWasteRepository.countByRestaurantId(restaurantId) + 1;
                String docNumber = String.format("MER-%05d", count);
                while (inventoryWasteRepository.existsByRestaurantIdAndDocumentNumber(restaurantId, docNumber)) {
                        count++;
                        docNumber = String.format("MER-%05d", count);
                }
                return docNumber;
        }

        private record WasteReason(String type, String description) {
        }

        private WasteReason resolveWasteReason(String providedType, String providedReason) {
                boolean noReason = (providedReason == null || providedReason.isBlank());
                boolean noType = (providedType == null || providedType.isBlank());

                if (noReason && noType) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "missing_reason",
                                        "Motivo obligatorio",
                                        "El motivo de la merma es obligatorio");
                }

                String reasonText = !noReason ? providedReason.trim() : providedType.trim();

                String type = "OTRO";
                if (!noType) {
                        String normalized = providedType.trim().toUpperCase();
                        if (Set.of("VENCIMIENTO", "DANO", "ERROR_MANEJO", "OTRO").contains(normalized)) {
                                type = normalized;
                        } else if (normalized.contains("VENC")) {
                                type = "VENCIMIENTO";
                        } else if (normalized.contains("DAN") || normalized.contains("DAÑ")) {
                                type = "DANO";
                        } else if (normalized.contains("ERROR") || normalized.contains("MANEJO")) {
                                type = "ERROR_MANEJO";
                        } else {
                                type = "OTRO";
                        }
                } else {
                        String upper = reasonText.toUpperCase();
                        if (upper.contains("VENC")) {
                                type = "VENCIMIENTO";
                        } else if (upper.contains("DAN") || upper.contains("DAÑ")) {
                                type = "DANO";
                        } else if (upper.contains("ERROR") || upper.contains("MANEJO") || upper.contains("ACCIDENTE")
                                        || upper.contains("CAIDA")) {
                                type = "ERROR_MANEJO";
                        } else {
                                type = "OTRO";
                        }
                }

                return new WasteReason(type, reasonText);
        }

        private Instant toInstant(Object obj) {
                if (obj == null) {
                        return null;
                }
                if (obj instanceof Instant inst) {
                        return inst;
                }
                if (obj instanceof java.sql.Timestamp ts) {
                        return ts.toInstant();
                }
                if (obj instanceof java.time.OffsetDateTime odt) {
                        return odt.toInstant();
                }
                if (obj instanceof java.time.ZonedDateTime zdt) {
                        return zdt.toInstant();
                }
                return Instant.parse(obj.toString());
        }

        private SupplyWasteResponse mapToWasteResponse(InventoryWasteDetail detail, Supply supply) {
                InventoryWaste waste = detail.getWaste();
                BigDecimal totalCost = detail.getTotalCost() != null
                                ? detail.getTotalCost()
                                : detail.getQuantity().multiply(detail.getUnitCostSnapshot()).setScale(4,
                                                RoundingMode.HALF_UP);

                return new SupplyWasteResponse(
                                waste.getId(),
                                detail.getId(),
                                waste.getDocumentNumber(),
                                supply.getId(),
                                supply.getCode(),
                                supply.getName(),
                                supply.getMeasurementUnit().getName(),
                                supply.getMeasurementUnit().getAbbreviation(),
                                detail.getQuantity(),
                                detail.getUnitCostSnapshot(),
                                totalCost,
                                null,
                                supply.getCurrentStock(),
                                waste.getReasonType(),
                                waste.getGeneralReason(),
                                detail.getBatchNumber(),
                                detail.getNotes(),
                                waste.getRegisteredAt());
        }
}
