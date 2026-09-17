package com.restaurante.application.recipe;

import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.MeasurementUnit;
import com.restaurante.domain.model.Modifier;
import com.restaurante.domain.model.ModifierRecipeDetail;
import com.restaurante.domain.model.ModifierRecipeVersion;
import com.restaurante.domain.model.RecipeDetail;
import com.restaurante.domain.model.RecipeVersion;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.domain.repository.MeasurementUnitRepository;
import com.restaurante.domain.repository.ModifierRecipeDetailRepository;
import com.restaurante.domain.repository.ModifierRecipeVersionRepository;
import com.restaurante.domain.repository.ModifierRepository;
import com.restaurante.domain.repository.RecipeDetailRepository;
import com.restaurante.domain.repository.RecipeVersionRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.recipe.DefineModifierRecipeRequest;
import com.restaurante.web.dto.recipe.DefineRecipeRequest;
import com.restaurante.web.dto.recipe.DishCostSummaryResponse;
import com.restaurante.web.dto.recipe.DishProductionCostResponse;
import com.restaurante.web.dto.recipe.ModifierIngredientChangeResponse;
import com.restaurante.web.dto.recipe.ModifierIngredientRequest;
import com.restaurante.web.dto.recipe.ModifierIngredientResponse;
import com.restaurante.web.dto.recipe.ModifierProductionCostResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeHistoryResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeVersionChangeDetailResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeVersionHistoryItemResponse;
import com.restaurante.web.dto.recipe.ProductionCostIngredientResponse;
import com.restaurante.web.dto.recipe.RecipeHistoryResponse;
import com.restaurante.web.dto.recipe.RecipeIngredientChangeResponse;
import com.restaurante.web.dto.recipe.RecipeIngredientRequest;
import com.restaurante.web.dto.recipe.RecipeIngredientResponse;
import com.restaurante.web.dto.recipe.RecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.RecipeResponse;
import com.restaurante.web.dto.recipe.RecipeUpdateResponse;
import com.restaurante.web.dto.recipe.RecipeVersionChangeDetailResponse;
import com.restaurante.web.dto.recipe.RecipeVersionHistoryItemResponse;
import com.restaurante.web.dto.recipe.UpdateRecipeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Servicio de aplicación para la gestión de recetas de platillos y modificadores.
 */
@Service
public class RecipeService {

    private final DishRepository dishRepository;
    private final RecipeVersionRepository recipeVersionRepository;
    private final RecipeDetailRepository recipeDetailRepository;
    private final SupplyRepository supplyRepository;
    private final MeasurementUnitRepository measurementUnitRepository;
    private final ModifierRepository modifierRepository;
    private final ModifierRecipeVersionRepository modifierRecipeVersionRepository;
    private final ModifierRecipeDetailRepository modifierRecipeDetailRepository;

    public RecipeService(DishRepository dishRepository,
                         RecipeVersionRepository recipeVersionRepository,
                         RecipeDetailRepository recipeDetailRepository,
                         SupplyRepository supplyRepository,
                         MeasurementUnitRepository measurementUnitRepository,
                         ModifierRepository modifierRepository,
                         ModifierRecipeVersionRepository modifierRecipeVersionRepository,
                         ModifierRecipeDetailRepository modifierRecipeDetailRepository) {
        this.dishRepository = dishRepository;
        this.recipeVersionRepository = recipeVersionRepository;
        this.recipeDetailRepository = recipeDetailRepository;
        this.supplyRepository = supplyRepository;
        this.measurementUnitRepository = measurementUnitRepository;
        this.modifierRepository = modifierRepository;
        this.modifierRecipeVersionRepository = modifierRecipeVersionRepository;
        this.modifierRecipeDetailRepository = modifierRecipeDetailRepository;
    }

    /**
     * Define la receta de un platillo con sus insumos y cantidades exactas.
     *
     * @param dishId         Identificador del platillo (puede venir en URL o en request)
     * @param request        Datos de la receta e insumos
     * @param authentication Credenciales del usuario autenticado
     * @return Confirmación y detalle de la receta creada
     */
    @Transactional
    public RecipeRegistrationResponse defineRecipe(Long dishId,
                                                  DefineRecipeRequest request,
                                                  Authentication authentication) {
        Long targetDishId = dishId != null ? dishId : request.dishId();
        if (targetDishId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe seleccionar o indicar el platillo para definir su receta"
            );
        }

        if (dishId != null && request.dishId() != null && !dishId.equals(request.dishId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_id_mismatch",
                    "Platillo no coincide",
                    "El identificador del platillo en la ruta no coincide con el del cuerpo de la solicitud"
            );
        }

        Dish dish = dishRepository.findById(targetDishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + targetDishId
                ));

        if (recipeVersionRepository.existsActiveByDishId(targetDishId)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "dish_already_has_active_recipe",
                    "Receta vigente existente",
                    "El platillo ya cuenta con una receta vigente; la receta existente debe actualizarse"
            );
        }

        if (request.ingredients() == null || request.ingredients().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "empty_recipe_ingredients",
                    "Receta sin insumos",
                    "Debe agregarse al menos un insumo a la receta"
            );
        }

        Set<Long> seenSupplyIds = new HashSet<>();
        for (RecipeIngredientRequest ingredient : request.ingredients()) {
            if (ingredient.supplyId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_supply_id",
                        "Insumo no especificado",
                        "El identificador del insumo es obligatorio"
                );
            }

            if (ingredient.quantity() == null || ingredient.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_ingredient_quantity",
                        "Cantidad no válida",
                        "La cantidad del insumo debe ser mayor que cero"
                );
            }

            if (!seenSupplyIds.add(ingredient.supplyId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "duplicate_recipe_ingredient",
                        "Insumo duplicado",
                        "El insumo ya forma parte de la receta"
                );
            }
        }

        Long adminUserId = resolveAdminUserId(authentication);
        int nextVersion = recipeVersionRepository.findMaxVersionNumberByDishId(targetDishId) + 1;

        RecipeVersion version = new RecipeVersion(
                dish,
                nextVersion,
                "BORRADOR",
                request.changeReason() != null ? request.changeReason().trim() : null,
                adminUserId
        );
        RecipeVersion savedVersion = recipeVersionRepository.saveAndFlush(version);

        for (RecipeIngredientRequest ingredientReq : request.ingredients()) {
            Supply supply = supplyRepository.findById(ingredientReq.supplyId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "supply_not_found",
                            "Insumo no encontrado",
                            "No se encontró un insumo con el identificador " + ingredientReq.supplyId()
                    ));

            if (!supply.getRestaurantId().equals(dish.getRestaurantId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "supply_restaurant_mismatch",
                        "Insumo no pertenece al restaurante",
                        "El insumo no pertenece al restaurante del platillo"
                );
            }

            MeasurementUnit recipeUnit;
            if (ingredientReq.measurementUnitId() != null) {
                recipeUnit = measurementUnitRepository.findById(ingredientReq.measurementUnitId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "unit_not_found",
                                "Unidad de medida no encontrada",
                                "No se encontró la unidad de medida con el identificador " + ingredientReq.measurementUnitId()
                        ));

                if (!recipeUnit.getDimension().equalsIgnoreCase(supply.getMeasurementUnit().getDimension())) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "incompatible_unit_dimension",
                            "Unidad de medida incompatible",
                            "La dimensión de la unidad de medida no coincide con la del insumo"
                    );
                }
            } else {
                recipeUnit = supply.getMeasurementUnit();
            }

            RecipeDetail detail = new RecipeDetail(
                    savedVersion,
                    supply,
                    recipeUnit,
                    ingredientReq.quantity(),
                    ingredientReq.notes() != null ? ingredientReq.notes().trim() : null
            );
            savedVersion.addDetail(detail);
        }

        recipeDetailRepository.saveAll(savedVersion.getDetails());
        recipeDetailRepository.flush();

        savedVersion.setStatus("VIGENTE");
        savedVersion.setEffectiveFrom(Instant.now());
        RecipeVersion publishedVersion = recipeVersionRepository.saveAndFlush(savedVersion);

        RecipeResponse response = buildRecipeResponse(publishedVersion);

        return new RecipeRegistrationResponse(
                "Receta del platillo definida exitosamente",
                response
        );
    }

    /**
     * Consulta la receta vigente de un platillo específico.
     *
     * @param dishId Identificador único del platillo
     * @return Información consolidada de la receta vigente con sus insumos y cantidades
     */
    @Transactional(readOnly = true)
    public RecipeResponse getActiveRecipe(Long dishId) {
        if (dishId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe indicar el identificador del platillo para consultar su receta"
            );
        }

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + dishId
                ));

        RecipeVersion activeVersion = recipeVersionRepository.findActiveByDishId(dish.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "recipe_not_found",
                        "Receta no definida",
                        "El platillo aún no tiene una receta definida"
                ));

        return buildRecipeResponse(activeVersion);
    }

    /**
     * Actualiza la receta vigente de un platillo, creando una nueva versión y conservando el historial.
     *
     * @param dishId         Identificador único del platillo
     * @param request        Nueva composición de la receta e insumos
     * @param authentication Credenciales del usuario autenticado
     * @return Confirmación y detalle de la nueva versión de receta vigente
     */
    @Transactional
    public RecipeUpdateResponse updateRecipe(Long dishId,
                                            UpdateRecipeRequest request,
                                            Authentication authentication) {
        if (dishId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe indicar el identificador del platillo para actualizar su receta"
            );
        }

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + dishId
                ));

        RecipeVersion currentActiveVersion = recipeVersionRepository.findActiveByDishId(dish.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "recipe_not_found",
                        "Receta no definida",
                        "El platillo aún no tiene una receta definida para actualizar"
                ));

        if (request.ingredients() == null || request.ingredients().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "empty_recipe_ingredients",
                    "Receta sin insumos",
                    "La receta debe contener al menos un insumo"
            );
        }

        Set<Long> seenSupplyIds = new HashSet<>();
        for (RecipeIngredientRequest ingredient : request.ingredients()) {
            if (ingredient.supplyId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_supply_id",
                        "Insumo no especificado",
                        "El identificador del insumo es obligatorio"
                );
            }

            if (ingredient.quantity() == null || ingredient.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_ingredient_quantity",
                        "Cantidad no válida",
                        "La cantidad del insumo debe ser mayor que cero"
                );
            }

            if (!seenSupplyIds.add(ingredient.supplyId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "duplicate_recipe_ingredient",
                        "Insumo duplicado",
                        "El insumo ya forma parte de la receta"
                );
            }
        }

        Long adminUserId = resolveAdminUserId(authentication);
        Instant now = Instant.now();

        Instant archiveTime = now.isAfter(currentActiveVersion.getEffectiveFrom())
                ? now
                : currentActiveVersion.getEffectiveFrom().plusMillis(1);
        Instant newEffectiveFrom = archiveTime.plusMillis(1);

        currentActiveVersion.setStatus("HISTORICA");
        currentActiveVersion.setEffectiveTo(archiveTime);
        recipeVersionRepository.saveAndFlush(currentActiveVersion);

        int nextVersion = recipeVersionRepository.findMaxVersionNumberByDishId(dish.getId()) + 1;
        RecipeVersion newVersion = new RecipeVersion(
                dish,
                nextVersion,
                "BORRADOR",
                request.changeReason() != null ? request.changeReason().trim() : "Actualización de receta",
                adminUserId
        );
        RecipeVersion savedNewVersion = recipeVersionRepository.saveAndFlush(newVersion);

        for (RecipeIngredientRequest ingredientReq : request.ingredients()) {
            Supply supply = supplyRepository.findById(ingredientReq.supplyId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "supply_not_found",
                            "Insumo no encontrado",
                            "No se encontró un insumo con el identificador " + ingredientReq.supplyId()
                    ));

            if (!supply.getRestaurantId().equals(dish.getRestaurantId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "supply_restaurant_mismatch",
                        "Insumo no pertenece al restaurante",
                        "El insumo no pertenece al restaurante del platillo"
                );
            }

            MeasurementUnit recipeUnit;
            if (ingredientReq.measurementUnitId() != null) {
                recipeUnit = measurementUnitRepository.findById(ingredientReq.measurementUnitId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "unit_not_found",
                                "Unidad de medida no encontrada",
                                "No se encontró la unidad de medida con el identificador " + ingredientReq.measurementUnitId()
                        ));

                if (!recipeUnit.getDimension().equalsIgnoreCase(supply.getMeasurementUnit().getDimension())) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "incompatible_unit_dimension",
                            "Unidad de medida incompatible",
                            "La dimensión de la unidad de medida no coincide con la del insumo"
                    );
                }
            } else {
                recipeUnit = supply.getMeasurementUnit();
            }

            RecipeDetail detail = new RecipeDetail(
                    savedNewVersion,
                    supply,
                    recipeUnit,
                    ingredientReq.quantity(),
                    ingredientReq.notes() != null ? ingredientReq.notes().trim() : null
            );
            savedNewVersion.addDetail(detail);
        }

        recipeDetailRepository.saveAll(savedNewVersion.getDetails());
        recipeDetailRepository.flush();

        savedNewVersion.setStatus("VIGENTE");
        savedNewVersion.setEffectiveFrom(newEffectiveFrom);
        RecipeVersion publishedNewVersion = recipeVersionRepository.saveAndFlush(savedNewVersion);

        RecipeResponse response = buildRecipeResponse(publishedNewVersion);

        return new RecipeUpdateResponse(
                "Receta del platillo actualizada exitosamente",
                response
        );
    }

    /**
     * Define o actualiza la receta de un modificador asociando insumos y cantidades.
     * Si el modificador ya cuenta con una receta vigente, conserva la versión anterior en el historial
     * y publica la nueva composición.
     *
     * @param modifierId     Identificador único del modificador
     * @param request        Datos de los insumos y cantidades
     * @param authentication Credenciales del usuario autenticado
     * @return Confirmación y detalle de la receta de modificador registrada
     */
    @Transactional
    public ModifierRecipeRegistrationResponse defineModifierRecipe(Long modifierId,
                                                                   DefineModifierRecipeRequest request,
                                                                   Authentication authentication) {
        if (modifierId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador para registrar su receta"
            );
        }

        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + modifierId
                ));

        if (request.ingredients() == null || request.ingredients().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "empty_modifier_recipe",
                    "Receta sin insumos",
                    "Debe agregarse al menos un insumo"
            );
        }

        Set<Long> seenSupplyIds = new HashSet<>();
        for (ModifierIngredientRequest ingredient : request.ingredients()) {
            if (ingredient.supplyId() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "missing_supply_id",
                        "Insumo no especificado",
                        "El identificador del insumo es obligatorio"
                );
            }

            if (ingredient.quantity() == null || ingredient.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_ingredient_quantity",
                        "Cantidad no válida",
                        "La cantidad del insumo debe ser mayor que cero"
                );
            }

            if (!seenSupplyIds.add(ingredient.supplyId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "duplicate_ingredient",
                        "Insumo duplicado",
                        "El insumo ya se encuentra asociado"
                );
            }
        }

        Optional<ModifierRecipeVersion> activeOpt = modifierRecipeVersionRepository.findActiveByModifierId(modifier.getId());
        Instant now = Instant.now();
        Instant newEffectiveFrom;

        if (activeOpt.isPresent()) {
            ModifierRecipeVersion oldVersion = activeOpt.get();
            Instant archiveTime = now.isAfter(oldVersion.getEffectiveFrom())
                    ? now
                    : oldVersion.getEffectiveFrom().plusMillis(1);
            newEffectiveFrom = archiveTime.plusMillis(1);

            oldVersion.setStatus("HISTORICA");
            oldVersion.setEffectiveTo(archiveTime);
            modifierRecipeVersionRepository.saveAndFlush(oldVersion);
        } else {
            newEffectiveFrom = now;
        }

        Long adminUserId = resolveAdminUserId(authentication);
        int nextVersion = modifierRecipeVersionRepository.findMaxVersionNumberByModifierId(modifier.getId()) + 1;

        ModifierRecipeVersion newVersion = new ModifierRecipeVersion(
                modifier,
                nextVersion,
                "BORRADOR",
                request.changeReason() != null ? request.changeReason().trim() : "Definición de receta de modificador",
                adminUserId
        );
        ModifierRecipeVersion savedVersion = modifierRecipeVersionRepository.saveAndFlush(newVersion);

        for (ModifierIngredientRequest ingredientReq : request.ingredients()) {
            Supply supply = supplyRepository.findById(ingredientReq.supplyId())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "supply_not_found",
                            "Insumo no encontrado",
                            "No se encontró un insumo con el identificador " + ingredientReq.supplyId()
                    ));

            if (!supply.getRestaurantId().equals(modifier.getRestaurantId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "supply_restaurant_mismatch",
                        "Insumo no pertenece al restaurante",
                        "El insumo no pertenece al restaurante del modificador"
                );
            }

            MeasurementUnit recipeUnit;
            if (ingredientReq.measurementUnitId() != null) {
                recipeUnit = measurementUnitRepository.findById(ingredientReq.measurementUnitId())
                        .orElseThrow(() -> new ApiException(
                                HttpStatus.NOT_FOUND,
                                "unit_not_found",
                                "Unidad de medida no encontrada",
                                "No se encontró la unidad de medida con el identificador " + ingredientReq.measurementUnitId()
                        ));

                if (!recipeUnit.getDimension().equalsIgnoreCase(supply.getMeasurementUnit().getDimension())) {
                    throw new ApiException(
                            HttpStatus.BAD_REQUEST,
                            "incompatible_unit_dimension",
                            "Unidad de medida incompatible",
                            "La dimensión de la unidad de medida no coincide con la del insumo"
                    );
                }
            } else {
                recipeUnit = supply.getMeasurementUnit();
            }

            String adjType = ingredientReq.adjustmentType() != null && !ingredientReq.adjustmentType().isBlank()
                    ? ingredientReq.adjustmentType().trim().toUpperCase()
                    : "AGREGAR";
            if (!"AGREGAR".equals(adjType) && !"RETIRAR".equals(adjType)) {
                adjType = "AGREGAR";
            }

            ModifierRecipeDetail detail = new ModifierRecipeDetail(
                    savedVersion,
                    supply,
                    recipeUnit,
                    adjType,
                    ingredientReq.quantity(),
                    ingredientReq.notes() != null ? ingredientReq.notes().trim() : null
            );
            savedVersion.addDetail(detail);
        }

        modifierRecipeDetailRepository.saveAll(savedVersion.getDetails());
        modifierRecipeDetailRepository.flush();

        savedVersion.setStatus("VIGENTE");
        savedVersion.setEffectiveFrom(newEffectiveFrom);
        ModifierRecipeVersion publishedVersion = modifierRecipeVersionRepository.saveAndFlush(savedVersion);

        ModifierRecipeResponse response = buildModifierRecipeResponse(publishedVersion);

        return new ModifierRecipeRegistrationResponse(
                "Receta del modificador guardada exitosamente",
                response
        );
    }

    /**
     * Consulta la receta vigente de un modificador.
     *
     * @param modifierId Identificador único del modificador
     * @return Detalle de la receta vigente con sus insumos y cantidades
     */
    @Transactional(readOnly = true)
    public ModifierRecipeResponse getActiveModifierRecipe(Long modifierId) {
        if (modifierId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador para consultar su receta"
            );
        }

        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + modifierId
                ));

        ModifierRecipeVersion activeVersion = modifierRecipeVersionRepository.findActiveByModifierId(modifier.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "recipe_not_found",
                        "Receta no definida",
                        "El modificador aún no tiene una receta definida"
                ));

        return buildModifierRecipeResponse(activeVersion);
    }

    /**
     * Calcula automáticamente el costo de producción actual de un platillo con conversión de unidades y desglose de insumos.
     *
     * @param dishId Identificador único del platillo
     * @return Costo de producción detallado del platillo y márgenes comerciales
     */
    @Transactional(readOnly = true)
    public DishProductionCostResponse calculateDishProductionCost(Long dishId) {
        if (dishId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe indicar el identificador del platillo para calcular su costo"
            );
        }

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + dishId
                ));

        RecipeVersion activeVersion = recipeVersionRepository.findActiveByDishId(dish.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "recipe_not_found",
                        "Receta no definida",
                        "El platillo aún no tiene una receta definida"
                ));

        BigDecimal totalProductionCost = BigDecimal.ZERO;
        List<ProductionCostIngredientResponse> ingredientBreakdown = new ArrayList<>();

        for (RecipeDetail detail : activeVersion.getDetails()) {
            Supply supply = detail.getSupply();
            MeasurementUnit recipeUnit = detail.getMeasurementUnit();
            MeasurementUnit stockUnit = supply.getMeasurementUnit();

            BigDecimal quantity = detail.getQuantity();
            BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
            BigDecimal usBaseFactor = stockUnit.getBaseFactor();
            BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                    ? supply.getCurrentUnitCost()
                    : BigDecimal.ZERO;

            BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                    .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
            BigDecimal subtotalCost = proportionalQty.multiply(currentUnitCost)
                    .setScale(4, RoundingMode.HALF_UP);

            totalProductionCost = totalProductionCost.add(subtotalCost);

            ingredientBreakdown.add(new ProductionCostIngredientResponse(
                    supply.getId(),
                    supply.getCode(),
                    supply.getName(),
                    quantity,
                    recipeUnit.getId(),
                    recipeUnit.getName(),
                    recipeUnit.getAbbreviation(),
                    stockUnit.getName(),
                    currentUnitCost,
                    proportionalQty,
                    subtotalCost,
                    detail.getNotes()
            ));
        }

        totalProductionCost = totalProductionCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal salePrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
        BigDecimal grossMargin = salePrice.subtract(totalProductionCost).setScale(4, RoundingMode.HALF_UP);

        BigDecimal marginPercentage = null;
        if (salePrice.compareTo(BigDecimal.ZERO) > 0) {
            marginPercentage = grossMargin.divide(salePrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        String categoryName = dish.getCategory() != null ? dish.getCategory().getName() : null;

        return new DishProductionCostResponse(
                dish.getId(),
                dish.getCode(),
                dish.getName(),
                categoryName,
                salePrice,
                activeVersion.getId(),
                activeVersion.getVersionNumber(),
                totalProductionCost,
                grossMargin,
                marginPercentage,
                ingredientBreakdown,
                Instant.now()
        );
    }

    /**
     * Calcula automáticamente el costo de producción adicional de un modificador con conversión de unidades.
     *
     * @param modifierId Identificador único del modificador
     * @return Costo de producción calculado del modificador
     */
    @Transactional(readOnly = true)
    public ModifierProductionCostResponse calculateModifierProductionCost(Long modifierId) {
        if (modifierId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador para calcular su costo"
            );
        }

        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + modifierId
                ));

        ModifierRecipeVersion activeVersion = modifierRecipeVersionRepository.findActiveByModifierId(modifier.getId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "recipe_not_found",
                        "Receta no definida",
                        "El modificador aún no tiene una receta definida"
                ));

        BigDecimal totalProductionCost = BigDecimal.ZERO;
        List<ProductionCostIngredientResponse> ingredientBreakdown = new ArrayList<>();

        for (ModifierRecipeDetail detail : activeVersion.getDetails()) {
            Supply supply = detail.getSupply();
            MeasurementUnit recipeUnit = detail.getMeasurementUnit();
            MeasurementUnit stockUnit = supply.getMeasurementUnit();

            BigDecimal quantity = detail.getQuantity();
            BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
            BigDecimal usBaseFactor = stockUnit.getBaseFactor();
            BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                    ? supply.getCurrentUnitCost()
                    : BigDecimal.ZERO;

            BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                    .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
            BigDecimal subtotalCost = proportionalQty.multiply(currentUnitCost)
                    .setScale(4, RoundingMode.HALF_UP);

            totalProductionCost = totalProductionCost.add(subtotalCost);

            ingredientBreakdown.add(new ProductionCostIngredientResponse(
                    supply.getId(),
                    supply.getCode(),
                    supply.getName(),
                    quantity,
                    recipeUnit.getId(),
                    recipeUnit.getName(),
                    recipeUnit.getAbbreviation(),
                    stockUnit.getName(),
                    currentUnitCost,
                    proportionalQty,
                    subtotalCost,
                    detail.getNotes()
            ));
        }

        totalProductionCost = totalProductionCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal additionalPrice = modifier.getAdditionalPrice() != null ? modifier.getAdditionalPrice() : BigDecimal.ZERO;
        BigDecimal grossMargin = additionalPrice.subtract(totalProductionCost).setScale(4, RoundingMode.HALF_UP);

        BigDecimal marginPercentage = null;
        if (additionalPrice.compareTo(BigDecimal.ZERO) > 0) {
            marginPercentage = grossMargin.divide(additionalPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        return new ModifierProductionCostResponse(
                modifier.getId(),
                modifier.getCode(),
                modifier.getName(),
                additionalPrice,
                activeVersion.getId(),
                activeVersion.getVersionNumber(),
                totalProductionCost,
                grossMargin,
                marginPercentage,
                ingredientBreakdown,
                Instant.now()
        );
    }

    /**
     * Lista los costos de producción y márgenes de los platillos que cuentan con receta vigente.
     *
     * @return Listado de resumen de costos de producción
     */
    @Transactional(readOnly = true)
    public List<DishCostSummaryResponse> listDishProductionCosts() {
        Long restaurantId = 1L;
        List<Dish> dishes = dishRepository.findByRestaurantIdAndActiveTrueOrderByNameAsc(restaurantId);
        List<DishCostSummaryResponse> result = new ArrayList<>();

        for (Dish dish : dishes) {
            Optional<RecipeVersion> activeOpt = recipeVersionRepository.findActiveByDishId(dish.getId());
            if (activeOpt.isEmpty()) {
                continue;
            }
            RecipeVersion activeVersion = activeOpt.get();
            BigDecimal totalProductionCost = BigDecimal.ZERO;

            for (RecipeDetail detail : activeVersion.getDetails()) {
                Supply supply = detail.getSupply();
                MeasurementUnit recipeUnit = detail.getMeasurementUnit();
                MeasurementUnit stockUnit = supply.getMeasurementUnit();

                BigDecimal quantity = detail.getQuantity();
                BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
                BigDecimal usBaseFactor = stockUnit.getBaseFactor();
                BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                        ? supply.getCurrentUnitCost()
                        : BigDecimal.ZERO;

                BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                        .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
                BigDecimal subtotalCost = proportionalQty.multiply(currentUnitCost)
                        .setScale(4, RoundingMode.HALF_UP);

                totalProductionCost = totalProductionCost.add(subtotalCost);
            }

            totalProductionCost = totalProductionCost.setScale(4, RoundingMode.HALF_UP);
            BigDecimal salePrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
            BigDecimal grossMargin = salePrice.subtract(totalProductionCost).setScale(4, RoundingMode.HALF_UP);

            BigDecimal marginPercentage = null;
            if (salePrice.compareTo(BigDecimal.ZERO) > 0) {
                marginPercentage = grossMargin.divide(salePrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(4, RoundingMode.HALF_UP);
            }

            String categoryName = dish.getCategory() != null ? dish.getCategory().getName() : null;

            result.add(new DishCostSummaryResponse(
                    dish.getId(),
                    dish.getCode(),
                    dish.getName(),
                    categoryName,
                    salePrice,
                    activeVersion.getId(),
                    activeVersion.getVersionNumber(),
                    totalProductionCost,
                    grossMargin,
                    marginPercentage
            ));
        }

        return result;
    }

    /**
     * Consulta el historial completo de versiones de la receta de un platillo en orden cronológico.
     *
     * @param dishId Identificador único del platillo
     * @return Historial consolidado de versiones y cambios registrados
     */
    @Transactional(readOnly = true)
    public RecipeHistoryResponse getDishRecipeHistory(Long dishId) {
        if (dishId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe indicar el identificador del platillo para consultar su historial"
            );
        }

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + dishId
                ));

        List<RecipeVersion> versions = recipeVersionRepository.findByDishIdOrderByVersionNumberAsc(dishId);
        if (versions.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "recipe_not_found",
                    "Receta no definida",
                    "El platillo aún no tiene una receta definida"
            );
        }

        List<RecipeVersionHistoryItemResponse> historyItems = new ArrayList<>();
        Integer activeVersionNumber = null;
        RecipeVersion previousVersion = null;

        for (RecipeVersion version : versions) {
            if ("VIGENTE".equalsIgnoreCase(version.getStatus())) {
                activeVersionNumber = version.getVersionNumber();
            }

            RecipeResponse recipeResponse = buildRecipeResponse(version);
            List<RecipeIngredientChangeResponse> changes = compareDishRecipeVersions(previousVersion, version);

            historyItems.add(new RecipeVersionHistoryItemResponse(
                    version.getId(),
                    version.getVersionNumber(),
                    version.getStatus(),
                    version.getChangeReason(),
                    version.getEffectiveFrom(),
                    version.getEffectiveTo(),
                    recipeResponse.totalCost(),
                    recipeResponse.dishSalePrice(),
                    recipeResponse.grossMargin(),
                    recipeResponse.marginPercentage(),
                    recipeResponse.ingredients().size(),
                    recipeResponse.ingredients(),
                    changes,
                    version.getCreatedById(),
                    version.getCreatedAt()
            ));

            previousVersion = version;
        }

        if (activeVersionNumber == null && !versions.isEmpty()) {
            activeVersionNumber = versions.get(versions.size() - 1).getVersionNumber();
        }

        boolean hasSubsequentChanges = versions.size() > 1;
        String message = hasSubsequentChanges
                ? "Historial de modificaciones de la receta obtenido exitosamente"
                : "No existen cambios posteriores; la receta se mantiene con su versión inicial vigente.";

        return new RecipeHistoryResponse(
                dish.getId(),
                dish.getCode(),
                dish.getName(),
                activeVersionNumber,
                versions.size(),
                hasSubsequentChanges,
                message,
                historyItems
        );
    }

    /**
     * Consulta el detalle de una versión específica de receta de platillo, comparándola con su versión anterior.
     *
     * @param dishId        Identificador único del platillo
     * @param versionNumber Número de versión consultada
     * @return Detalle de la versión con composiciones previa y nueva, y cambios identificados
     */
    @Transactional(readOnly = true)
    public RecipeVersionChangeDetailResponse getDishRecipeVersionDetail(Long dishId, Integer versionNumber) {
        if (dishId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_dish_id",
                    "Platillo no especificado",
                    "Debe indicar el identificador del platillo"
            );
        }

        if (versionNumber == null || versionNumber < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_version_number",
                    "Número de versión inválido",
                    "El número de versión debe ser un entero positivo mayor que cero"
            );
        }

        Dish dish = dishRepository.findById(dishId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "dish_not_found",
                        "Platillo no encontrado",
                        "No se encontró un platillo con el identificador " + dishId
                ));

        RecipeVersion version = recipeVersionRepository.findByDishIdAndVersionNumber(dishId, versionNumber)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "version_not_found",
                        "Versión no encontrada",
                        "No se encontró la versión " + versionNumber + " para la receta del platillo"
                ));

        RecipeResponse currentRecipeResponse = buildRecipeResponse(version);
        Integer previousVersionNumber = null;
        BigDecimal previousTotalCost = null;
        BigDecimal totalCostDifference = null;
        List<RecipeIngredientResponse> previousComposition = Collections.emptyList();
        List<RecipeIngredientChangeResponse> changes = Collections.emptyList();

        if (versionNumber > 1) {
            Optional<RecipeVersion> prevOpt = recipeVersionRepository.findByDishIdAndVersionNumber(dishId, versionNumber - 1);
            if (prevOpt.isPresent()) {
                RecipeVersion prevVersion = prevOpt.get();
                RecipeResponse prevRecipeResponse = buildRecipeResponse(prevVersion);
                previousVersionNumber = prevVersion.getVersionNumber();
                previousTotalCost = prevRecipeResponse.totalCost();
                totalCostDifference = currentRecipeResponse.totalCost().subtract(previousTotalCost).setScale(4, RoundingMode.HALF_UP);
                previousComposition = prevRecipeResponse.ingredients();
                changes = compareDishRecipeVersions(prevVersion, version);
            }
        }

        return new RecipeVersionChangeDetailResponse(
                dish.getId(),
                dish.getCode(),
                dish.getName(),
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getChangeReason(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                currentRecipeResponse.totalCost(),
                previousVersionNumber,
                previousTotalCost,
                totalCostDifference,
                previousComposition,
                currentRecipeResponse.ingredients(),
                changes,
                version.getCreatedById(),
                version.getCreatedAt()
        );
    }

    /**
     * Consulta el historial completo de versiones de la receta de un modificador en orden cronológico.
     *
     * @param modifierId Identificador único del modificador
     * @return Historial consolidado de versiones y cambios registrados
     */
    @Transactional(readOnly = true)
    public ModifierRecipeHistoryResponse getModifierRecipeHistory(Long modifierId) {
        if (modifierId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador para consultar su historial"
            );
        }

        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + modifierId
                ));

        List<ModifierRecipeVersion> versions = modifierRecipeVersionRepository.findByModifierIdOrderByVersionNumberAsc(modifierId);
        if (versions.isEmpty()) {
            throw new ApiException(
                    HttpStatus.NOT_FOUND,
                    "recipe_not_found",
                    "Receta no definida",
                    "El modificador aún no tiene una receta definida"
            );
        }

        List<ModifierRecipeVersionHistoryItemResponse> historyItems = new ArrayList<>();
        Integer activeVersionNumber = null;
        ModifierRecipeVersion previousVersion = null;

        for (ModifierRecipeVersion version : versions) {
            if ("VIGENTE".equalsIgnoreCase(version.getStatus())) {
                activeVersionNumber = version.getVersionNumber();
            }

            ModifierRecipeResponse recipeResponse = buildModifierRecipeResponse(version);
            List<ModifierIngredientChangeResponse> changes = compareModifierRecipeVersions(previousVersion, version);

            historyItems.add(new ModifierRecipeVersionHistoryItemResponse(
                    version.getId(),
                    version.getVersionNumber(),
                    version.getStatus(),
                    version.getChangeReason(),
                    version.getEffectiveFrom(),
                    version.getEffectiveTo(),
                    recipeResponse.totalCost(),
                    recipeResponse.ingredients().size(),
                    recipeResponse.ingredients(),
                    changes,
                    version.getCreatedById(),
                    version.getCreatedAt()
            ));

            previousVersion = version;
        }

        if (activeVersionNumber == null && !versions.isEmpty()) {
            activeVersionNumber = versions.get(versions.size() - 1).getVersionNumber();
        }

        boolean hasSubsequentChanges = versions.size() > 1;
        String message = hasSubsequentChanges
                ? "Historial de modificaciones de la receta del modificador obtenido exitosamente"
                : "No existen cambios posteriores; la receta del modificador se mantiene con su versión inicial vigente.";

        return new ModifierRecipeHistoryResponse(
                modifier.getId(),
                modifier.getCode(),
                modifier.getName(),
                modifier.getAdditionalPrice(),
                activeVersionNumber,
                versions.size(),
                hasSubsequentChanges,
                message,
                historyItems
        );
    }

    /**
     * Consulta el detalle de una versión específica de receta de modificador, comparándola con su versión anterior.
     *
     * @param modifierId    Identificador único del modificador
     * @param versionNumber Número de versión consultada
     * @return Detalle de la versión con composiciones previa y nueva, y cambios identificados
     */
    @Transactional(readOnly = true)
    public ModifierRecipeVersionChangeDetailResponse getModifierRecipeVersionDetail(Long modifierId, Integer versionNumber) {
        if (modifierId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "missing_modifier_id",
                    "Modificador no especificado",
                    "Debe indicar el identificador del modificador"
            );
        }

        if (versionNumber == null || versionNumber < 1) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_version_number",
                    "Número de versión inválido",
                    "El número de versión debe ser un entero positivo mayor que cero"
            );
        }

        Modifier modifier = modifierRepository.findById(modifierId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "modifier_not_found",
                        "Modificador no encontrado",
                        "No se encontró un modificador con el identificador " + modifierId
                ));

        ModifierRecipeVersion version = modifierRecipeVersionRepository.findByModifierIdAndVersionNumber(modifierId, versionNumber)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "version_not_found",
                        "Versión no encontrada",
                        "No se encontró la versión " + versionNumber + " para la receta del modificador"
                ));

        ModifierRecipeResponse currentRecipeResponse = buildModifierRecipeResponse(version);
        Integer previousVersionNumber = null;
        BigDecimal previousTotalCost = null;
        BigDecimal totalCostDifference = null;
        List<ModifierIngredientResponse> previousComposition = Collections.emptyList();
        List<ModifierIngredientChangeResponse> changes = Collections.emptyList();

        if (versionNumber > 1) {
            Optional<ModifierRecipeVersion> prevOpt = modifierRecipeVersionRepository.findByModifierIdAndVersionNumber(modifierId, versionNumber - 1);
            if (prevOpt.isPresent()) {
                ModifierRecipeVersion prevVersion = prevOpt.get();
                ModifierRecipeResponse prevRecipeResponse = buildModifierRecipeResponse(prevVersion);
                previousVersionNumber = prevVersion.getVersionNumber();
                previousTotalCost = prevRecipeResponse.totalCost();
                totalCostDifference = currentRecipeResponse.totalCost().subtract(previousTotalCost).setScale(4, RoundingMode.HALF_UP);
                previousComposition = prevRecipeResponse.ingredients();
                changes = compareModifierRecipeVersions(prevVersion, version);
            }
        }

        return new ModifierRecipeVersionChangeDetailResponse(
                modifier.getId(),
                modifier.getCode(),
                modifier.getName(),
                modifier.getAdditionalPrice(),
                version.getId(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getChangeReason(),
                version.getEffectiveFrom(),
                version.getEffectiveTo(),
                currentRecipeResponse.totalCost(),
                previousVersionNumber,
                previousTotalCost,
                totalCostDifference,
                previousComposition,
                currentRecipeResponse.ingredients(),
                changes,
                version.getCreatedById(),
                version.getCreatedAt()
        );
    }

    private List<RecipeIngredientChangeResponse> compareDishRecipeVersions(RecipeVersion oldVersion, RecipeVersion newVersion) {
        if (oldVersion == null) {
            return Collections.emptyList();
        }

        Map<Long, RecipeDetail> oldDetailsBySupply = oldVersion.getDetails().stream()
                .collect(Collectors.toMap(d -> d.getSupply().getId(), d -> d, (d1, d2) -> d1));
        Map<Long, RecipeDetail> newDetailsBySupply = newVersion.getDetails().stream()
                .collect(Collectors.toMap(d -> d.getSupply().getId(), d -> d, (d1, d2) -> d1));

        Set<Long> allSupplyIds = new LinkedHashSet<>();
        oldVersion.getDetails().forEach(d -> allSupplyIds.add(d.getSupply().getId()));
        newVersion.getDetails().forEach(d -> allSupplyIds.add(d.getSupply().getId()));

        List<RecipeIngredientChangeResponse> changes = new ArrayList<>();

        for (Long supplyId : allSupplyIds) {
            RecipeDetail oldDetail = oldDetailsBySupply.get(supplyId);
            RecipeDetail newDetail = newDetailsBySupply.get(supplyId);

            if (oldDetail == null && newDetail != null) {
                BigDecimal subtotal = calculateDetailCost(newDetail);
                changes.add(new RecipeIngredientChangeResponse(
                        newDetail.getSupply().getId(),
                        newDetail.getSupply().getCode(),
                        newDetail.getSupply().getName(),
                        "AGREGADO",
                        null,
                        null,
                        newDetail.getQuantity(),
                        newDetail.getMeasurementUnit().getName(),
                        newDetail.getQuantity(),
                        subtotal,
                        newDetail.getNotes()
                ));
            } else if (oldDetail != null && newDetail == null) {
                BigDecimal subtotal = calculateDetailCost(oldDetail);
                changes.add(new RecipeIngredientChangeResponse(
                        oldDetail.getSupply().getId(),
                        oldDetail.getSupply().getCode(),
                        oldDetail.getSupply().getName(),
                        "RETIRADO",
                        oldDetail.getQuantity(),
                        oldDetail.getMeasurementUnit().getName(),
                        null,
                        null,
                        oldDetail.getQuantity().negate(),
                        subtotal.negate(),
                        oldDetail.getNotes()
                ));
            } else if (oldDetail != null && newDetail != null) {
                BigDecimal oldSubtotal = calculateDetailCost(oldDetail);
                BigDecimal newSubtotal = calculateDetailCost(newDetail);
                boolean quantityChanged = oldDetail.getQuantity().compareTo(newDetail.getQuantity()) != 0;
                boolean unitChanged = !oldDetail.getMeasurementUnit().getId().equals(newDetail.getMeasurementUnit().getId());

                String changeType = (quantityChanged || unitChanged) ? "MODIFICADO" : "SIN_CAMBIOS";
                BigDecimal qtyDiff = newDetail.getQuantity().subtract(oldDetail.getQuantity());
                BigDecimal costDiff = newSubtotal.subtract(oldSubtotal);

                changes.add(new RecipeIngredientChangeResponse(
                        newDetail.getSupply().getId(),
                        newDetail.getSupply().getCode(),
                        newDetail.getSupply().getName(),
                        changeType,
                        oldDetail.getQuantity(),
                        oldDetail.getMeasurementUnit().getName(),
                        newDetail.getQuantity(),
                        newDetail.getMeasurementUnit().getName(),
                        qtyDiff,
                        costDiff,
                        newDetail.getNotes() != null ? newDetail.getNotes() : oldDetail.getNotes()
                ));
            }
        }

        return changes;
    }

    private List<ModifierIngredientChangeResponse> compareModifierRecipeVersions(ModifierRecipeVersion oldVersion, ModifierRecipeVersion newVersion) {
        if (oldVersion == null) {
            return Collections.emptyList();
        }

        Map<Long, ModifierRecipeDetail> oldDetailsBySupply = oldVersion.getDetails().stream()
                .collect(Collectors.toMap(d -> d.getSupply().getId(), d -> d, (d1, d2) -> d1));
        Map<Long, ModifierRecipeDetail> newDetailsBySupply = newVersion.getDetails().stream()
                .collect(Collectors.toMap(d -> d.getSupply().getId(), d -> d, (d1, d2) -> d1));

        Set<Long> allSupplyIds = new LinkedHashSet<>();
        oldVersion.getDetails().forEach(d -> allSupplyIds.add(d.getSupply().getId()));
        newVersion.getDetails().forEach(d -> allSupplyIds.add(d.getSupply().getId()));

        List<ModifierIngredientChangeResponse> changes = new ArrayList<>();

        for (Long supplyId : allSupplyIds) {
            ModifierRecipeDetail oldDetail = oldDetailsBySupply.get(supplyId);
            ModifierRecipeDetail newDetail = newDetailsBySupply.get(supplyId);

            if (oldDetail == null && newDetail != null) {
                BigDecimal subtotal = calculateModifierDetailCost(newDetail);
                changes.add(new ModifierIngredientChangeResponse(
                        newDetail.getSupply().getId(),
                        newDetail.getSupply().getCode(),
                        newDetail.getSupply().getName(),
                        "AGREGADO",
                        null,
                        null,
                        null,
                        newDetail.getAdjustmentType(),
                        newDetail.getQuantity(),
                        newDetail.getMeasurementUnit().getName(),
                        newDetail.getQuantity(),
                        subtotal,
                        newDetail.getNotes()
                ));
            } else if (oldDetail != null && newDetail == null) {
                BigDecimal subtotal = calculateModifierDetailCost(oldDetail);
                changes.add(new ModifierIngredientChangeResponse(
                        oldDetail.getSupply().getId(),
                        oldDetail.getSupply().getCode(),
                        oldDetail.getSupply().getName(),
                        "RETIRADO",
                        oldDetail.getAdjustmentType(),
                        oldDetail.getQuantity(),
                        oldDetail.getMeasurementUnit().getName(),
                        null,
                        null,
                        null,
                        oldDetail.getQuantity().negate(),
                        subtotal.negate(),
                        oldDetail.getNotes()
                ));
            } else if (oldDetail != null && newDetail != null) {
                BigDecimal oldSubtotal = calculateModifierDetailCost(oldDetail);
                BigDecimal newSubtotal = calculateModifierDetailCost(newDetail);
                boolean qtyChanged = oldDetail.getQuantity().compareTo(newDetail.getQuantity()) != 0;
                boolean unitChanged = !oldDetail.getMeasurementUnit().getId().equals(newDetail.getMeasurementUnit().getId());
                boolean adjChanged = !oldDetail.getAdjustmentType().equalsIgnoreCase(newDetail.getAdjustmentType());

                String changeType = (qtyChanged || unitChanged || adjChanged) ? "MODIFICADO" : "SIN_CAMBIOS";
                BigDecimal qtyDiff = newDetail.getQuantity().subtract(oldDetail.getQuantity());
                BigDecimal costDiff = newSubtotal.subtract(oldSubtotal);

                changes.add(new ModifierIngredientChangeResponse(
                        newDetail.getSupply().getId(),
                        newDetail.getSupply().getCode(),
                        newDetail.getSupply().getName(),
                        changeType,
                        oldDetail.getAdjustmentType(),
                        oldDetail.getQuantity(),
                        oldDetail.getMeasurementUnit().getName(),
                        newDetail.getAdjustmentType(),
                        newDetail.getQuantity(),
                        newDetail.getMeasurementUnit().getName(),
                        qtyDiff,
                        costDiff,
                        newDetail.getNotes() != null ? newDetail.getNotes() : oldDetail.getNotes()
                ));
            }
        }

        return changes;
    }

    private BigDecimal calculateDetailCost(RecipeDetail detail) {
        Supply supply = detail.getSupply();
        MeasurementUnit recipeUnit = detail.getMeasurementUnit();
        MeasurementUnit stockUnit = supply.getMeasurementUnit();

        BigDecimal quantity = detail.getQuantity();
        BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
        BigDecimal usBaseFactor = stockUnit.getBaseFactor();
        BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                ? supply.getCurrentUnitCost()
                : BigDecimal.ZERO;

        BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
        return proportionalQty.multiply(currentUnitCost)
                .setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateModifierDetailCost(ModifierRecipeDetail detail) {
        Supply supply = detail.getSupply();
        MeasurementUnit recipeUnit = detail.getMeasurementUnit();
        MeasurementUnit stockUnit = supply.getMeasurementUnit();

        BigDecimal quantity = detail.getQuantity();
        BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
        BigDecimal usBaseFactor = stockUnit.getBaseFactor();
        BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                ? supply.getCurrentUnitCost()
                : BigDecimal.ZERO;

        BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
        return proportionalQty.multiply(currentUnitCost)
                .setScale(4, RoundingMode.HALF_UP);
    }

    /**
     * Construye el DTO consolidado de la receta de un platillo.
     */
    private RecipeResponse buildRecipeResponse(RecipeVersion version) {
        Dish dish = version.getDish();
        BigDecimal totalCost = BigDecimal.ZERO;
        List<RecipeIngredientResponse> ingredientResponses = new ArrayList<>();

        for (RecipeDetail detail : version.getDetails()) {
            Supply supply = detail.getSupply();
            MeasurementUnit recipeUnit = detail.getMeasurementUnit();
            MeasurementUnit stockUnit = supply.getMeasurementUnit();

            BigDecimal quantity = detail.getQuantity();
            BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
            BigDecimal usBaseFactor = stockUnit.getBaseFactor();
            BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                    ? supply.getCurrentUnitCost()
                    : BigDecimal.ZERO;

            BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                    .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
            BigDecimal subtotalCost = proportionalQty.multiply(currentUnitCost)
                    .setScale(4, RoundingMode.HALF_UP);

            totalCost = totalCost.add(subtotalCost);

            ingredientResponses.add(new RecipeIngredientResponse(
                    detail.getId(),
                    supply.getId(),
                    supply.getCode(),
                    supply.getName(),
                    quantity,
                    recipeUnit.getId(),
                    recipeUnit.getName(),
                    recipeUnit.getAbbreviation(),
                    currentUnitCost,
                    subtotalCost,
                    detail.getNotes()
            ));
        }

        totalCost = totalCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal salePrice = dish.getSalePrice() != null ? dish.getSalePrice() : BigDecimal.ZERO;
        BigDecimal grossMargin = salePrice.subtract(totalCost).setScale(4, RoundingMode.HALF_UP);

        BigDecimal marginPercentage = null;
        if (salePrice.compareTo(BigDecimal.ZERO) > 0) {
            marginPercentage = grossMargin.divide(salePrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(4, RoundingMode.HALF_UP);
        }

        return new RecipeResponse(
                version.getId(),
                dish.getId(),
                dish.getCode(),
                dish.getName(),
                version.getVersionNumber(),
                version.getStatus(),
                version.getChangeReason(),
                version.getEffectiveFrom(),
                totalCost,
                salePrice,
                grossMargin,
                marginPercentage,
                ingredientResponses,
                version.getCreatedById(),
                version.getCreatedAt()
        );
    }

    /**
     * Construye el DTO consolidado de la receta de un modificador.
     */
    private ModifierRecipeResponse buildModifierRecipeResponse(ModifierRecipeVersion version) {
        Modifier modifier = version.getModifier();
        BigDecimal totalCost = BigDecimal.ZERO;
        List<ModifierIngredientResponse> ingredientResponses = new ArrayList<>();

        for (ModifierRecipeDetail detail : version.getDetails()) {
            Supply supply = detail.getSupply();
            MeasurementUnit recipeUnit = detail.getMeasurementUnit();
            MeasurementUnit stockUnit = supply.getMeasurementUnit();

            BigDecimal quantity = detail.getQuantity();
            BigDecimal urBaseFactor = recipeUnit.getBaseFactor();
            BigDecimal usBaseFactor = stockUnit.getBaseFactor();
            BigDecimal currentUnitCost = supply.getCurrentUnitCost() != null
                    ? supply.getCurrentUnitCost()
                    : BigDecimal.ZERO;

            BigDecimal proportionalQty = quantity.multiply(urBaseFactor)
                    .divide(usBaseFactor, 6, RoundingMode.HALF_UP);
            BigDecimal subtotalCost = proportionalQty.multiply(currentUnitCost)
                    .setScale(4, RoundingMode.HALF_UP);

            totalCost = totalCost.add(subtotalCost);

            ingredientResponses.add(new ModifierIngredientResponse(
                    detail.getId(),
                    supply.getId(),
                    supply.getCode(),
                    supply.getName(),
                    quantity,
                    recipeUnit.getId(),
                    recipeUnit.getName(),
                    recipeUnit.getAbbreviation(),
                    detail.getAdjustmentType(),
                    currentUnitCost,
                    subtotalCost,
                    detail.getNotes()
            ));
        }

        totalCost = totalCost.setScale(4, RoundingMode.HALF_UP);
        BigDecimal additionalPrice = modifier.getAdditionalPrice() != null ? modifier.getAdditionalPrice() : BigDecimal.ZERO;

        return new ModifierRecipeResponse(
                version.getId(),
                modifier.getId(),
                modifier.getCode(),
                modifier.getName(),
                additionalPrice,
                version.getVersionNumber(),
                version.getStatus(),
                version.getChangeReason(),
                version.getEffectiveFrom(),
                totalCost,
                ingredientResponses,
                version.getCreatedById(),
                version.getCreatedAt()
        );
    }

    private Long resolveAdminUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
            return jwtData.userId();
        }
        return 1L;
    }
}
