package com.restaurante.web.admin;

import com.restaurante.application.dish.DishService;
import com.restaurante.web.dto.dish.CreateDishRequest;
import com.restaurante.web.dto.dish.DishCategoryResponse;
import com.restaurante.web.dto.dish.DishRegistrationResponse;
import com.restaurante.web.dto.dish.DishResponse;
import com.restaurante.web.dto.dish.DishRetirementResponse;
import com.restaurante.web.dto.dish.DishUpdateResponse;
import com.restaurante.web.dto.dish.UpdateDishRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador administrativo para la gestión y consulta del catálogo de platillos y categorías del menú.
 */
@RestController
@RequestMapping("/admin/dishes")
@Tag(name = "Platillos y Menú (Administración)", description = "Operaciones administrativas para el catálogo de platillos del menú")
@SecurityRequirement(name = "bearerAuth")
public class AdminDishController {

    private final DishService dishService;

    public AdminDishController(DishService dishService) {
        this.dishService = dishService;
    }

    @PostMapping
    @Operation(
            summary = "Registrar un nuevo platillo",
            description = "Registra un nuevo platillo con su información comercial, categoría, precio de venta, imagen y tiempo estimado de preparación."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Platillo registrado exitosamente",
                    content = @Content(schema = @Schema(implementation = DishRegistrationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obligatorios vacíos, precio de venta menor o igual a cero, o tiempo de preparación inválido",
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
                    responseCode = "409",
                    description = "Nombre o código de platillo duplicado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishRegistrationResponse> registerDish(@Valid @RequestBody CreateDishRequest request) {
        DishRegistrationResponse response = dishService.registerDish(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(
            summary = "Consultar el catálogo de platillos",
            description = "Retorna el catálogo de platillos registrados con su imagen, nombre, descripción, categoría, precio de venta, tiempo estimado de preparación y disponibilidad actual (identificando indisponibilidad manual o por falta de insumos). Permite filtrar opcionalmente por categoría o término de búsqueda."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Catálogo de platillos obtenido exitosamente (devuelve lista vacía si todavía no existen platillos registrados)"
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
    public ResponseEntity<List<DishResponse>> listDishes(
            @Parameter(description = "Identificador de la categoría para filtrar (opcional)")
            @RequestParam(required = false) Long categoryId,
            @Parameter(description = "Término de búsqueda por nombre o código (opcional)")
            @RequestParam(required = false) String search
    ) {
        List<DishResponse> dishes = dishService.listDishes(categoryId, search);
        if (dishes.isEmpty()) {
            return ResponseEntity.ok()
                    .header("X-Message", "Todavía no existen platillos registrados")
                    .body(dishes);
        }
        return ResponseEntity.ok(dishes);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar detalle y disponibilidad de un platillo por ID",
            description = "Retorna la información comercial, operativa y disponibilidad actual de un platillo específico, incluyendo porciones disponibles y motivo de indisponibilidad si aplica."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo encontrado exitosamente"
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
                    description = "Platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishResponse> getDishById(
            @Parameter(description = "Identificador único del platillo", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(dishService.getDishById(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar la información de un platillo",
            description = "Actualiza la información comercial y operativa de un platillo registrado en el menú (nombre, descripción, categoría, precio de venta, imagen, tiempo estimado de preparación y disponibilidad manual)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = DishUpdateResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Campos obligatorios vacíos, precio menor o igual a cero, tiempo de preparación menor o igual a cero, o categoría no válida",
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
                    description = "Platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Nombre o código de platillo duplicado en otro platillo",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishUpdateResponse> updateDish(
            @Parameter(description = "Identificador único del platillo a modificar", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateDishRequest request
    ) {
        return ResponseEntity.ok(dishService.updateDish(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Retirar un platillo del menú",
            description = "Retira un platillo del menú marcándolo como inactivo sin eliminar su información histórica (recetas y operaciones anteriores). No permite el retiro si el platillo está incluido en uno o más combos activos o si ya fue retirado previamente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Platillo retirado exitosamente",
                    content = @Content(schema = @Schema(implementation = DishRetirementResponse.class))
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
                    description = "Platillo no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El platillo ya se encuentra retirado o está incluido en uno o más combos activos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<DishRetirementResponse> retireDish(
            @Parameter(description = "Identificador único del platillo a retirar", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(dishService.retireDish(id));
    }

    @GetMapping("/categories")
    @Operation(
            summary = "Consultar categorías disponibles para platillos",
            description = "Retorna las categorías de platillos disponibles (entrada, plato fuerte, bebida, postre) para seleccionar durante el registro o consulta."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de categorías de platillo obtenido exitosamente"
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
    public ResponseEntity<List<DishCategoryResponse>> listCategories() {
        return ResponseEntity.ok(dishService.listDishCategories());
    }
}
