package com.restaurante.application.mail;

import com.restaurante.domain.model.OtpPurpose;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "app.mail.provider", havingValue = "brevo")
public class BrevoEmailService implements EmailService {

    private final RestClient client;
    private final String from;
    private final String senderName;

    @Autowired
    public BrevoEmailService(
            @Value("${app.mail.brevo.api-key:}") String apiKey,
            @Value("${app.mail.from:}") String from,
            @Value("${app.mail.sender-name:Restaurante AyD1}") String senderName
    ) {
        this(clientBuilder(), apiKey, from, senderName);
    }

    BrevoEmailService(RestClient.Builder builder, String apiKey, String from, String senderName) {
        if (apiKey == null || apiKey.isBlank() || from == null || from.isBlank()
                || senderName == null || senderName.isBlank()) {
            throw new IllegalStateException("Brevo requires BREVO_API_KEY, MAIL_FROM and MAIL_SENDER_NAME");
        }
        this.from = from;
        this.senderName = senderName;
        this.client = builder.baseUrl("https://api.brevo.com/v3")
                .defaultHeader("api-key", apiKey)
                .build();
    }

    //metodo para construir el cliente HTTP para enviar correos electrónicos a través de la API de Brevo
    private static RestClient.Builder clientBuilder() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(factory);
    }

    //metodo para enviar un correo electrónico OTP
    @Override
    public void sendOtp(String recipient, String code, OtpPurpose purpose, int expirationMinutes) {
        try {
            client.post().uri("/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "sender", Map.of("email", from, "name", senderName),
                            "to", List.of(Map.of("email", recipient)),
                            "subject", OtpEmailContent.subjectFor(purpose),
                            "textContent", OtpEmailContent.bodyFor(code, purpose, expirationMinutes)
                    ))
                    .exchange((request, response) -> {
                        if (response.getStatusCode().value() != 201) {
                            throw new IllegalStateException("Brevo did not accept the email (HTTP "
                                    + response.getStatusCode().value() + ")");
                        }
                        return null;
                    });
        } catch (RestClientException exception) {
            // catch para excepciones como problemas de red, tiempo de espera o errores de cliente HTTP
            throw new IllegalStateException("Could not connect to the email provider");
        }
    }
}
