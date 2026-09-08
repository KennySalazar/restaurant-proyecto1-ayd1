package com.restaurante.application.mail;

import com.restaurante.domain.model.OtpPurpose;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class BrevoEmailServiceTests {

    @Test
    void sendsEveryOtpPurposeThroughHttpsWithTheVerifiedSender() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailService service = new BrevoEmailService(builder, "test-api-key",
                "sender@example.invalid", "Restaurante AyD1");
        for (OtpPurpose purpose : OtpPurpose.values()) {
            server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                    .andExpect(method(HttpMethod.POST))
                    .andExpect(header("api-key", "test-api-key"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.sender.email").value("sender@example.invalid"))
                    .andExpect(jsonPath("$.to[0].email").value("recipient@example.invalid"))
                    .andExpect(jsonPath("$.subject").isNotEmpty())
                    .andExpect(jsonPath("$.textContent").value(
                            org.hamcrest.Matchers.allOf(
                                    org.hamcrest.Matchers.containsString("123456"),
                                    org.hamcrest.Matchers.containsString("10 minutos"))))
                    .andRespond(withStatus(HttpStatus.CREATED)
                            .body("{\"messageId\":\"test-message\"}")
                            .contentType(MediaType.APPLICATION_JSON));
            service.sendOtp("recipient@example.invalid", "123456", purpose, 10);
            server.verify();
            server.reset();
        }
    }

    @Test
    void rejectsProviderFailuresWithoutLeakingResponseBodyOrRetrying() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
                HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR}) {
            RestClient.Builder builder = RestClient.builder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
            BrevoEmailService service = new BrevoEmailService(builder, "test-api-key",
                    "sender@example.invalid", "Restaurante AyD1");
            server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                    .andRespond(withStatus(status).body("sensitive-provider-response"));
            assertThatThrownBy(() -> service.sendOtp("recipient@example.invalid", "123456",
                    OtpPurpose.LOGIN, 10))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Brevo did not accept the email (HTTP " + status.value() + ")")
                    .hasNoCause();
            server.verify();
        }
    }

    @Test
    void sanitizesTransportFailures() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BrevoEmailService service = new BrevoEmailService(builder, "test-api-key",
                "sender@example.invalid", "Restaurante AyD1");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withException(new IOException("sensitive-transport-details")));
        assertThatThrownBy(() -> service.sendOtp("recipient@example.invalid", "123456",
                OtpPurpose.LOGIN, 10))
                .hasMessage("Could not connect to the email provider").hasNoCause();
        server.verify();
    }

    @Test
    void requiresConfigurationBeforeSending() {
        assertThatThrownBy(() -> new BrevoEmailService(RestClient.builder(), "", "", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BREVO_API_KEY");
    }
}
