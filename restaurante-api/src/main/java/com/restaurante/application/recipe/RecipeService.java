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
import com.restaurante.web.dto.recipe.ModifierIngredientRequest;
import com.restaurante.web.dto.recipe.ModifierIngredientResponse;
import com.restaurante.web.dto.recipe.ModifierProductionCostResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeResponse;
import com.restaurante.web.dto.recipe.ProductionCostIngredientResponse;
import com.restaurante.web.dto.recipe.RecipeIngredientRequest;
import com.restaurante.web.dto.recipe.RecipeIngredientResponse;
import com.restaurante.web.dto.recipe.RecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.RecipeResponse;
import com.restaurante.web.dto.recipe.RecipeUpdateResponse;
import com.restaurante.web.dto.recipe.UpdateRecipeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
