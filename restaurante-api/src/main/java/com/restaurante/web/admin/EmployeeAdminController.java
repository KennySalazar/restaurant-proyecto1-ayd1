package com.restaurante.web.admin;

import com.restaurante.application.employee.EmployeeService;
import com.restaurante.web.dto.employee.CreateEmployeeRequest;
import com.restaurante.web.dto.employee.EmployeeResponse;
import com.restaurante.web.dto.employee.OperationalRoleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.restaurante.web.dto.employee.UpdateEmployeeRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


@RestController
@RequestMapping("/admin/empleados")
@Tag(
        name = "Empleados",
        description = "Gestión administrativa de empleados"
)
@SecurityRequirement(name = "bearerAuth")
public class EmployeeAdminController {

    private final EmployeeService employeeService;

    public EmployeeAdminController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PostMapping
    @Operation(summary = "Registrar un empleado")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            Authentication authentication) {

        EmployeeResponse response =
                employeeService.createEmployee(request, authentication);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/roles")
    @Operation(summary = "Consultar roles operativos disponibles")
    public List<OperationalRoleResponse> getOperationalRoles() {
        return employeeService.getOperationalRoles();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un empleado y su rol")
    public EmployeeResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request,
            Authentication authentication) {

        return employeeService.updateEmployee(
                id,
                request,
                authentication
        );
    }

    @GetMapping
    @Operation(summary = "Consultar los empleados registrados")
    public List<EmployeeResponse> getEmployees(
            Authentication authentication) {

        return employeeService.getEmployees(authentication);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle de un empleado")
    public EmployeeResponse getEmployee(
            @PathVariable Long id,
            Authentication authentication) {

        return employeeService.getEmployee(
                id,
                authentication
        );
    }
}