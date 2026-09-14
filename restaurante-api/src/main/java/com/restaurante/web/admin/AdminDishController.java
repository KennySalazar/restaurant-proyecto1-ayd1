package com.restaurante.web.admin;

import com.restaurante.application.dish.DishService;
import com.restaurante.web.dto.dish.CreateDishRequest;
import com.restaurante.web.dto.dish.DishCategoryResponse;
import com.restaurante.web.dto.dish.DishRegistrationResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador administrativo para la gestión de platillos y categorías del menú.
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
