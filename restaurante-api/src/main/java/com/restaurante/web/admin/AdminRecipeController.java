package com.restaurante.web.admin;

import com.restaurante.application.recipe.RecipeService;
import com.restaurante.web.dto.recipe.DefineRecipeRequest;
import com.restaurante.web.dto.recipe.RecipeRegistrationResponse;
import com.restaurante.web.dto.recipe.RecipeResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador administrativo para la gestión y consulta de recetas de
 * platillos.
 */
@RestController
@Tag(name = "Recetas (Administración)", description = "Operaciones administrativas para definición y consulta de recetas de platillos")
@SecurityRequirement(name = "bearerAuth")
public class AdminRecipeController {

        private final RecipeService recipeService;

        public AdminRecipeController(RecipeService recipeService) {
                this.recipeService = recipeService;
        }

        @PostMapping("/admin/dishes/{dishId}/recipe")
        @Operation(summary = "Definir receta de un platillo por ID de platillo", description = "Define la receta de un platillo especificando sus insumos y cantidades exactas. Calcula automáticamente el costo total, margen bruto y porcentaje de margen.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Receta del platillo definida exitosamente", content = @Content(schema = @Schema(implementation = RecipeRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos, receta sin insumos, cantidad inválida, insumo duplicado o platillo con receta vigente existente", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Platillo o insumo no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<RecipeRegistrationResponse> defineDishRecipe(
                        @Parameter(description = "Identificador único del platillo", required = true) @PathVariable Long dishId,
                        @Valid @RequestBody DefineRecipeRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                RecipeRegistrationResponse response = recipeService.defineRecipe(dishId, request, authentication);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @PostMapping("/admin/recipes")
        @Operation(summary = "Definir receta de un platillo", description = "Define la receta de un platillo indicando el identificador del platillo en el cuerpo de la solicitud.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Receta del platillo definida exitosamente", content = @Content(schema = @Schema(implementation = RecipeRegistrationResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Campos obligatorios vacíos, receta sin insumos, cantidad inválida o insumo duplicado", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Platillo o insumo no encontrado", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<RecipeRegistrationResponse> defineRecipe(
                        @Valid @RequestBody DefineRecipeRequest request,
                        @Parameter(hidden = true) Authentication authentication) {
                RecipeRegistrationResponse response = recipeService.defineRecipe(request.dishId(), request,
                                authentication);
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
        }

        @GetMapping("/admin/dishes/{dishId}/recipe")
        @Operation(summary = "Consultar receta vigente de un platillo", description = "Retorna los insumos que componen la receta vigente del platillo, la cantidad requerida de cada insumo y la unidad de medida correspondiente.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Receta vigente consultada exitosamente", content = @Content(schema = @Schema(implementation = RecipeResponse.class))),
                        @ApiResponse(responseCode = "400", description = "Identificador de platillo inválido", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "401", description = "No autenticado o token no provisto", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "403", description = "Acceso denegado (requiere rol ADMIN)", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
                        @ApiResponse(responseCode = "404", description = "Platillo no encontrado o el platillo aún no tiene una receta definida", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
        })
        public ResponseEntity<RecipeResponse> getActiveRecipe(
                        @Parameter(description = "Identificador único del platillo", required = true) @PathVariable Long dishId) {
                return ResponseEntity.ok(recipeService.getActiveRecipe(dishId));
        }
}
