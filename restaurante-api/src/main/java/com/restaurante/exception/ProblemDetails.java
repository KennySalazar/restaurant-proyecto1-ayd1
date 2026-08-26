package com.restaurante.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatusCode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

public final class ProblemDetails {

    private ProblemDetails() {
    }

    public static ProblemDetail create(ApiException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatus(), exception.getMessage());
        problem.setTitle(exception.getTitle());
        problem.setType(java.net.URI.create("https://api.ssg.local/problems/" + exception.getCode()));
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        problem.setProperty("code", exception.getCode());
        return problem;
    }

    public static ProblemDetail create(int status, String title, String detail, String code,
                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        problem.setTitle(title);
        if (code != null && !code.isBlank()) {
            problem.setType(java.net.URI.create("https://api.ssg.local/problems/" + code));
        }
        problem.setInstance(java.net.URI.create(request.getRequestURI()));
        if (code != null && !code.isBlank()) {
            problem.setProperty("code", code);
        }
        return problem;
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper,
                             ProblemDetail problem) throws IOException {
        response.setStatus(problem.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
