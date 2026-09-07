package com.restaurante.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.DefaultCorsProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsConfigurationTests {

    @Test
    void allowsOnlyConfiguredOriginsIncludingAuthorizationPreflight() throws Exception {
        var source = new CorsConfiguration().corsConfigurationSource(
                "https://admin.example.invalid, https://pos.example.invalid");
        for (String origin : new String[]{"https://admin.example.invalid", "https://pos.example.invalid",
                "https://attacker.example.invalid"}) {
            var request = new MockHttpServletRequest("OPTIONS", "/auth/me");
            request.addHeader("Origin", origin);
            request.addHeader("Access-Control-Request-Method", "GET");
            request.addHeader("Access-Control-Request-Headers", "authorization,content-type");
            var response = new MockHttpServletResponse();
            boolean allowed = new DefaultCorsProcessor().processRequest(
                    source.getCorsConfiguration(request), request, response);
            assertThat(allowed).isEqualTo(!origin.contains("attacker"));
            assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
            if (allowed) {
                assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(origin);
            } else {
                assertThat(response.getStatus()).isEqualTo(403);
            }
        }
    }

    @Test
    void emptyConfigurationDoesNotAllowCrossOriginRequests() throws Exception {
        var source = new CorsConfiguration().corsConfigurationSource("");
        var request = new MockHttpServletRequest("GET", "/auth/me");
        request.addHeader("Origin", "https://admin.example.invalid");
        assertThat(new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request),
                request, new MockHttpServletResponse())).isFalse();
    }

    @Test
    void rejectsWildcardsAndUrlsThatAreNotOrigins() {
        for (String value : new String[]{"*", "https://*.pages.dev", "https://site.invalid/path",
                "https://site.invalid/", "https://site.invalid?query", "https://user@site.invalid",
                "ftp://site.invalid"}) {
            assertThatThrownBy(() -> new CorsConfiguration().corsConfigurationSource(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
