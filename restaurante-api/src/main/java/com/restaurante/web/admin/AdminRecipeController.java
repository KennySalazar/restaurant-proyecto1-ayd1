package com.restaurante.web.admin;

import com.restaurante.application.recipe.RecipeService;
import com.restaurante.web.dto.recipe.DefineModifierRecipeRequest;
import com.restaurante.web.dto.recipe.DefineRecipeRequest;
import com.restaurante.web.dto.recipe.DishCostSummaryResponse;
import com.restaurante.web.dto.recipe.DishProductionCostResponse;
import com.restaurante.web.dto.recipe.ModifierProductionCostResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeHistoryResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeResponse;
import com.restaurante.web.dto.recipe.ModifierRecipeVersionChangeDetailResponse;
import com.restaurante.web.dto.recipe.RecipeHistoryResponse;
import com.restaurante.web.dto.recipe.RecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.RecipeResponse;
import com.restaurante.web.dto.recipe.RecipeUpdateResponse;
import com.restaurante.web.dto.recipe.RecipeVersionChangeDetailResponse;
import com.restaurante.web.dto.recipe.UpdateRecipeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador administrativo para la gestión y consulta de recetas de platillos y modificadores.
 */
@RestController
@Tag(name = "Recetas (Administración)", description = "Operaciones administrativas para definición, actualización y consulta de recetas de platillos y modificadores")
@SecurityRequirement(name = "bearerAuth")
public class AdminRecipeController {

    private final RecipeService recipeService;

    public AdminRecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @PostMapping("/admin/dishes/{dishId}/recipe")
    @Operation(
            summary = "Definir receta de un platillo por ID de platillo",
            description = "Define la receta de un platillo especificando sus insumos y cantidades exactas. Calcula automáticamente el costo total, margen bruto y porcentaje de margen."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Receta del platillo definida exitosamente",
                    content = @Content(schema = @Schema(implementation = RecipeRegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obligatorios vacíos, receta sin insumos, cantidad inválida, insumo duplicado o platillo con receta vigente existente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o insumo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RecipeRegistrationResponse> defineDishRecipe(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long dishId,
            @Valid @RequestBody DefineRecipeRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        RecipeRegistrationResponse response = recipeService.defineRecipe(dishId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/admin/recipes")
    @Operation(
            summary = "Definir receta de un platillo",
            description = "Define la receta de un platillo indicando el identificador del platillo en el cuerpo de la solicitud."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Receta del platillo definida exitosamente",
                    content = @Content(schema = @Schema(implementation = RecipeRegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obligatorios vacíos, receta sin insumos, cantidad inválida o insumo duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o insumo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RecipeRegistrationResponse> defineRecipe(
            @Valid @RequestBody DefineRecipeRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        RecipeRegistrationResponse response = recipeService.defineRecipe(request.dishId(), request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admin/dishes/{dishId}/recipe")
    @Operation(
            summary = "Consultar receta vigente de un platillo",
            description = "Retorna los insumos que componen la receta vigente del platillo, la cantidad requerida de cada insumo y la unidad de medida correspondiente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Receta vigente consultada exitosamente",
                    content = @Content(schema = @Schema(implementation = RecipeResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de platillo inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo no encontrado o el platillo aún no tiene una receta definida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RecipeResponse> getActiveRecipe(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long dishId) {
        return ResponseEntity.ok(recipeService.getActiveRecipe(dishId));
    }

    @PutMapping("/admin/dishes/{dishId}/recipe")
    @Operation(
            summary = "Actualizar receta de un platillo",
            description = "Actualiza la receta vigente de un platillo reflejando cambios en insumos o cantidades, registrando la fecha de modificación y conservando la versión anterior en el historial."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Receta del platillo actualizada exitosamente",
                    content = @Content(schema = @Schema(implementation = RecipeUpdateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obligatorios vacíos, receta sin insumos, cantidad menor o igual a cero, o insumo duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo no encontrado o sin receta previa para actualizar",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RecipeUpdateResponse> updateRecipe(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long dishId,
            @Valid @RequestBody UpdateRecipeRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        RecipeUpdateResponse response = recipeService.updateRecipe(dishId, request, authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/admin/modifiers/{modifierId}/recipe")
    @Operation(
            summary = "Definir receta de un modificador",
            description = "Asocia insumos y cantidades a los modificadores de un platillo. Si el modificador ya cuenta con una receta definida, guarda la nueva composición y conserva la anterior en el historial."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Receta del modificador guardada exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierRecipeRegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obligatorios vacíos, receta sin insumos, cantidad menor o igual a cero, o insumo duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador o insumo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierRecipeRegistrationResponse> defineModifierRecipe(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long modifierId,
            @Valid @RequestBody DefineModifierRecipeRequest request,
            @Parameter(hidden = true) Authentication authentication) {
        ModifierRecipeRegistrationResponse response = recipeService.defineModifierRecipe(modifierId, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admin/dishes/{dishId}/cost")
    @Operation(
            summary = "Calcular costo de producción de un platillo",
            description = "Calcula automáticamente el costo de producción actual del platillo a partir de los insumos de su receta vigente, realizando conversión de unidades y calculando el margen de ganancia."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Costo de producción calculado exitosamente",
                    content = @Content(schema = @Schema(implementation = DishProductionCostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de platillo inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo no encontrado o sin receta definida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishProductionCostResponse> getDishProductionCost(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long dishId) {
        return ResponseEntity.ok(recipeService.calculateDishProductionCost(dishId));
    }

    @GetMapping("/admin/dishes/costs")
    @Operation(
            summary = "Listar costos de producción de platillos",
            description = "Retorna el resumen de costos de producción calculados y márgenes comerciales de todos los platillos activos con receta vigente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de costos de platillos obtenido exitosamente"
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<DishCostSummaryResponse>> listDishProductionCosts() {
        return ResponseEntity.ok(recipeService.listDishProductionCosts());
    }

    @GetMapping("/admin/modifiers/{modifierId}/cost")
    @Operation(
            summary = "Calcular costo de producción de un modificador",
            description = "Calcula automáticamente el costo de producción adicional del modificador a partir de su receta vigente con conversión de unidades."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Costo de producción del modificador calculado exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierProductionCostResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de modificador inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador no encontrado o sin receta definida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierProductionCostResponse> getModifierProductionCost(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long modifierId) {
        return ResponseEntity.ok(recipeService.calculateModifierProductionCost(modifierId));
    }

    @GetMapping("/admin/dishes/{dishId}/recipe/history")
    @Operation(
            summary = "Consultar historial de cambios de la receta de un platillo",
            description = "Retorna el historial cronológico de versiones y cambios registrados en la receta de un platillo, con fecha de cada modificación, insumos y costos calculados."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Historial de la receta consultado exitosamente",
                    content = @Content(schema = @Schema(implementation = RecipeHistoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de platillo inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo no encontrado o sin receta definida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RecipeHistoryResponse> getDishRecipeHistory(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long dishId) {
        return ResponseEntity.ok(recipeService.getDishRecipeHistory(dishId));
    }

    @GetMapping("/admin/dishes/{dishId}/recipe/versions/{versionNumber}")
    @Operation(
            summary = "Consultar detalle de un cambio o versión de receta de platillo",
            description = "Retorna la composición anterior, la nueva composición y el desglose de insumos agregados, retirados o modificados para una versión específica de la receta."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la versión y cambios obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = RecipeVersionChangeDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de platillo o número de versión inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Platillo o versión no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<RecipeVersionChangeDetailResponse> getDishRecipeVersionDetail(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long dishId,
            @Parameter(description = "Número de versión a consultar", required = true)
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(recipeService.getDishRecipeVersionDetail(dishId, versionNumber));
    }

    @GetMapping("/admin/modifiers/{modifierId}/recipe/history")
    @Operation(
            summary = "Consultar historial de cambios de la receta de un modificador",
            description = "Retorna las versiones registradas y detalla cronológicamente los cambios realizados en los insumos y cantidades de la receta del modificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Historial de la receta del modificador consultado exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierRecipeHistoryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de modificador inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador no encontrado o sin receta definida",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierRecipeHistoryResponse> getModifierRecipeHistory(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long modifierId) {
        return ResponseEntity.ok(recipeService.getModifierRecipeHistory(modifierId));
    }

    @GetMapping("/admin/modifiers/{modifierId}/recipe/versions/{versionNumber}")
    @Operation(
            summary = "Consultar detalle de un cambio o versión de receta de modificador",
            description = "Retorna la composición anterior, la nueva composición y el desglose de insumos agregados, retirados o modificados para una versión específica de la receta del modificador."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Detalle de la versión y cambios obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = ModifierRecipeVersionChangeDetailResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Identificador de modificador o número de versión inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol ADMIN)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Modificador o versión no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ModifierRecipeVersionChangeDetailResponse> getModifierRecipeVersionDetail(
            @Parameter(description = "Identificador único del modificador", required = true)
            @PathVariable Long modifierId,
            @Parameter(description = "Número de versión a consultar", required = true)
            @PathVariable Integer versionNumber) {
        return ResponseEntity.ok(recipeService.getModifierRecipeVersionDetail(modifierId, versionNumber));
    }
}
