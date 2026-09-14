package com.restaurante.application.recipe;

import com.restaurante.domain.model.Dish;
import com.restaurante.domain.model.MeasurementUnit;
import com.restaurante.domain.model.RecipeDetail;
import com.restaurante.domain.model.RecipeVersion;
import com.restaurante.domain.model.Supply;
import com.restaurante.domain.repository.DishRepository;
import com.restaurante.domain.repository.MeasurementUnitRepository;
import com.restaurante.domain.repository.RecipeDetailRepository;
import com.restaurante.domain.repository.RecipeVersionRepository;
import com.restaurante.domain.repository.SupplyRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.recipe.DefineRecipeRequest;
import com.restaurante.web.dto.recipe.RecipeIngredientRequest;
import com.restaurante.web.dto.recipe.RecipeIngredientResponse;
import com.restaurante.web.dto.recipe.RecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.RecipeResponse;
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
import java.util.Set;

/**
 * Servicio de aplicación para la gestión de recetas de platillos.
 */
@Service
public class RecipeService {

        private final DishRepository dishRepository;
        private final RecipeVersionRepository recipeVersionRepository;
        private final RecipeDetailRepository recipeDetailRepository;
        private final SupplyRepository supplyRepository;
        private final MeasurementUnitRepository measurementUnitRepository;

        public RecipeService(DishRepository dishRepository,
                        RecipeVersionRepository recipeVersionRepository,
                        RecipeDetailRepository recipeDetailRepository,
                        SupplyRepository supplyRepository,
                        MeasurementUnitRepository measurementUnitRepository) {
                this.dishRepository = dishRepository;
                this.recipeVersionRepository = recipeVersionRepository;
                this.recipeDetailRepository = recipeDetailRepository;
                this.supplyRepository = supplyRepository;
                this.measurementUnitRepository = measurementUnitRepository;
        }

        /**
         * Define la receta de un platillo con sus insumos y cantidades exactas.
         *
         * @param dishId         Identificador del platillo (puede venir en URL o en
         *                       request)
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
                                        "Debe seleccionar o indicar el platillo para definir su receta");
                }

                if (dishId != null && request.dishId() != null && !dishId.equals(request.dishId())) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "dish_id_mismatch",
                                        "Platillo no coincide",
                                        "El identificador del platillo en la ruta no coincide con el del cuerpo de la solicitud");
                }

                Dish dish = dishRepository.findById(targetDishId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "dish_not_found",
                                                "Platillo no encontrado",
                                                "No se encontró un platillo con el identificador " + targetDishId));

                if (recipeVersionRepository.existsActiveByDishId(targetDishId)) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "dish_already_has_active_recipe",
                                        "Receta vigente existente",
                                        "El platillo ya cuenta con una receta vigente; la receta existente debe actualizarse");
                }

                if (request.ingredients() == null || request.ingredients().isEmpty()) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "empty_recipe_ingredients",
                                        "Receta sin insumos",
                                        "Debe agregarse al menos un insumo a la receta");
                }

                Set<Long> seenSupplyIds = new HashSet<>();
                for (RecipeIngredientRequest ingredient : request.ingredients()) {
                        if (ingredient.supplyId() == null) {
                                throw new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "missing_supply_id",
                                                "Insumo no especificado",
                                                "El identificador del insumo es obligatorio");
                        }

                        if (ingredient.quantity() == null || ingredient.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                                throw new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "invalid_ingredient_quantity",
                                                "Cantidad no válida",
                                                "La cantidad del insumo debe ser mayor que cero");
                        }

                        if (!seenSupplyIds.add(ingredient.supplyId())) {
                                throw new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "duplicate_recipe_ingredient",
                                                "Insumo duplicado",
                                                "El insumo ya forma parte de la receta");
                        }
                }

                Long adminUserId = resolveAdminUserId(authentication);
                int nextVersion = recipeVersionRepository.findMaxVersionNumberByDishId(targetDishId) + 1;

                RecipeVersion version = new RecipeVersion(
                                dish,
                                nextVersion,
                                "BORRADOR",
                                request.changeReason() != null ? request.changeReason().trim() : null,
                                adminUserId);
                RecipeVersion savedVersion = recipeVersionRepository.saveAndFlush(version);

                for (RecipeIngredientRequest ingredientReq : request.ingredients()) {
                        Supply supply = supplyRepository.findById(ingredientReq.supplyId())
                                        .orElseThrow(() -> new ApiException(
                                                        HttpStatus.NOT_FOUND,
                                                        "supply_not_found",
                                                        "Insumo no encontrado",
                                                        "No se encontró un insumo con el identificador "
                                                                        + ingredientReq.supplyId()));

                        if (!supply.getRestaurantId().equals(dish.getRestaurantId())) {
                                throw new ApiException(
                                                HttpStatus.BAD_REQUEST,
                                                "supply_restaurant_mismatch",
                                                "Insumo no pertenece al restaurante",
                                                "El insumo no pertenece al restaurante del platillo");
                        }

                        MeasurementUnit recipeUnit;
                        if (ingredientReq.measurementUnitId() != null) {
                                recipeUnit = measurementUnitRepository.findById(ingredientReq.measurementUnitId())
                                                .orElseThrow(() -> new ApiException(
                                                                HttpStatus.NOT_FOUND,
                                                                "unit_not_found",
                                                                "Unidad de medida no encontrada",
                                                                "No se encontró la unidad de medida con el identificador "
                                                                                + ingredientReq.measurementUnitId()));

                                if (!recipeUnit.getDimension()
                                                .equalsIgnoreCase(supply.getMeasurementUnit().getDimension())) {
                                        throw new ApiException(
                                                        HttpStatus.BAD_REQUEST,
                                                        "incompatible_unit_dimension",
                                                        "Unidad de medida incompatible",
                                                        "La dimensión de la unidad de medida no coincide con la del insumo");
                                }
                        } else {
                                recipeUnit = supply.getMeasurementUnit();
                        }

                        RecipeDetail detail = new RecipeDetail(
                                        savedVersion,
                                        supply,
                                        recipeUnit,
                                        ingredientReq.quantity(),
                                        ingredientReq.notes() != null ? ingredientReq.notes().trim() : null);
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
                                response);
        }

        /**
         * Consulta la receta vigente de un platillo específico.
         *
         * @param dishId Identificador único del platillo
         * @return Información consolidada de la receta vigente con sus insumos y
         *         cantidades
         */
        @Transactional(readOnly = true)
        public RecipeResponse getActiveRecipe(Long dishId) {
                if (dishId == null) {
                        throw new ApiException(
                                        HttpStatus.BAD_REQUEST,
                                        "missing_dish_id",
                                        "Platillo no especificado",
                                        "Debe indicar el identificador del platillo para consultar su receta");
                }

                Dish dish = dishRepository.findById(dishId)
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "dish_not_found",
                                                "Platillo no encontrado",
                                                "No se encontró un platillo con el identificador " + dishId));

                RecipeVersion activeVersion = recipeVersionRepository.findActiveByDishId(dish.getId())
                                .orElseThrow(() -> new ApiException(
                                                HttpStatus.NOT_FOUND,
                                                "recipe_not_found",
                                                "Receta no definida",
                                                "El platillo aún no tiene una receta definida"));

                return buildRecipeResponse(activeVersion);
        }

        /**
         * Construye el DTO consolidado de la receta, calculando costo total, margen bruto
         * y porcentaje de margen con base en las fórmulas oficiales del sistema.
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
                                        detail.getNotes()));
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
                                version.getCreatedAt());
        }

        private Long resolveAdminUserId(Authentication authentication) {
                if (authentication != null && authentication.getDetails() instanceof JwtData jwtData) {
                        return jwtData.userId();
                }
                return 1L;
        }
}
