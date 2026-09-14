package com.restaurante.web.dto.employee;

import com.restaurante.domain.model.RoleName;
import com.restaurante.web.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateEmployeeRequest(

        @NotBlank
        @Size(max = 30)
        String codigoEmpleado,

        @NotBlank
        @Size(max = 100)
        String nombres,

        @NotBlank
        @Size(max = 100)
        String apellidos,

        @NotBlank
        @Email
        @Size(max = 320)
        String email,

        @NotBlank
        @ValidPassword
        String password,

        LocalDate fechaContratacion,

        @NotNull
        RoleName rol
) {
}