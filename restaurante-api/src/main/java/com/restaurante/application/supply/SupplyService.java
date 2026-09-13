package com.restaurante.application.supply;

import com.restaurante.domain.model.MeasurementUnit;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.model.SupplyCategory;
import com.restaurante.domain.repository.MeasurementUnitRepository;
import com.restaurante.domain.repository.SupplyCategoryRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.web.dto.supply.ConfigureStockLimitsRequest;
import com.restaurante.web.dto.supply.CreateSupplyRequest;
import com.restaurante.web.dto.supply.MeasurementUnitResponse;
import com.restaurante.web.dto.supply.SupplyCategoryResponse;
import com.restaurante.web.dto.supply.SupplyRegistrationResponse;
import com.restaurante.web.dto.supply.SupplyResponse;
import com.restaurante.web.dto.supply.SupplyStockLimitsResponse;
import com.restaurante.web.dto.supply.SupplyUpdateResponse;
import com.restaurante.web.dto.supply.UpdateSupplyRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

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

        @PersistenceContext
        private EntityManager entityManager;

        public SupplyService(SupplyRepository supplyRepository,
                        SupplyCategoryRepository categoryRepository,
                        MeasurementUnitRepository unitRepository) {
                this.supplyRepository = supplyRepository;
                this.categoryRepository = categoryRepository;
                this.unitRepository = unitRepository;
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
                String sanitizedSearch = (search != null && !search.isBlank()) ? search.trim() : null;
                return supplyRepository
                                .searchCatalog(DEFAULT_RESTAURANT_ID, categoryId, sanitizedSearch)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
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
                if (minStock.compareTo(BigDecimal.ZERO) < 0 || (maxStock != null && maxStock.compareTo(BigDecimal.ZERO) < 0)) {
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

                // habilitar permiso en sesión transaccional de PostgreSQL para actualizar insumo
                entityManager.createNativeQuery(
                                "SELECT set_config('restaurante.permitir_actualizacion_stock', 'true', true)")
                                .getSingleResult();

                supply.setMinimumStock(minStock);
                supply.setMaximumStock(maxStock);

                Supply updated = supplyRepository.save(supply);

                return new SupplyStockLimitsResponse(
                                "Límites de stock configurados exitosamente",
                                mapToResponse(updated));
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
}
