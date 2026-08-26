package com.restaurante.application.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTests {

    @Test
    void acceptsTheDocumentedPasswordPolicy() {
        assertThat(PasswordPolicy.isValid("Password123")).isTrue();
    }

    @Test
    void rejectsShortAndMissingCharacterClasses() {
        assertThat(PasswordPolicy.isValid("Pass1")).isFalse();
        assertThat(PasswordPolicy.isValid("password123")).isFalse();
        assertThat(PasswordPolicy.isValid("PASSWORD123")).isFalse();
        assertThat(PasswordPolicy.isValid("PasswordOnly")).isFalse();
    }
}
