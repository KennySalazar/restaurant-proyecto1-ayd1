package com.restaurante.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI coreOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Restaurante API")
                .version("1.0.0")
                    .description("API para autenticación, autorización y gestión del sistema de restaurante"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT"))
                .addSchemas("ProblemDetail", problemDetailSchema()));
    }

    @Bean
    OpenApiCustomizer problemResponses() {
        return openAPI -> {
            // Springdoc removes unused schemas before running customizers.
            openAPI.getComponents().addSchemas("ProblemDetail", problemDetailSchema());
            openAPI.getPaths().values().forEach(path -> path.readOperations().forEach(operation -> {
                operation.getResponses().addApiResponse("400", problemResponse("Solicitud inválida"));
                operation.getResponses().addApiResponse("401", problemResponse("Autenticación requerida"));
                operation.getResponses().addApiResponse("403", problemResponse("Acceso denegado"));
                operation.getResponses().addApiResponse("500", problemResponse("Error interno"));
            }));
        };
    }

    private ObjectSchema problemDetailSchema() {
        ObjectSchema problemDetail = new ObjectSchema();
        problemDetail.addProperty("type", new StringSchema().format("uri"));
        problemDetail.addProperty("title", new StringSchema());
        problemDetail.addProperty("status", new IntegerSchema());
        problemDetail.addProperty("detail", new StringSchema());
        problemDetail.addProperty("instance", new StringSchema().format("uri"));
        problemDetail.addProperty("code", new StringSchema());
        return problemDetail;
    }

    private ApiResponse problemResponse(String description) {
        return new ApiResponse()
            .description(description)
            .content(new io.swagger.v3.oas.models.media.Content()
                .addMediaType("application/problem+json", new MediaType()
                    .schema(new io.swagger.v3.oas.models.media.Schema<>().$ref("#/components/schemas/ProblemDetail"))));
    }
}
