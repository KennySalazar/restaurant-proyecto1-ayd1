package com.restaurante.web.dto.employee;

import com.restaurante.domain.model.RoleName;

public record OperationalRoleResponse(
        RoleName codigo,
        String nombre
) {
}