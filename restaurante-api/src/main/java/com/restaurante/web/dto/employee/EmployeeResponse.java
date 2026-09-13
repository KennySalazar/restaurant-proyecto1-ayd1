package com.restaurante.web.dto.employee;

import com.restaurante.domain.model.RoleName;

import java.time.LocalDate;

public record EmployeeResponse(
        Long id,
        String codigoEmpleado,
        String nombres,
        String apellidos,
        String email,
        LocalDate fechaContratacion,
        RoleName rol,
        boolean habilitado
) {
}