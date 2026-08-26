package com.restaurante.security;

import com.restaurante.domain.model.RoleName;

public record JwtData(String email, long userId, RoleName role, int tokenVersion) {
}
