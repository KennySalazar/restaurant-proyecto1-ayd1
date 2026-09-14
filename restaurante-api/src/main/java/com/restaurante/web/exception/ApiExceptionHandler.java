package com.restaurante.web.exception;

import com.restaurante.exception.ApiException;
import com.restaurante.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        return response(ProblemDetails.create(exception, request), exception.getStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException exception,
                                                    HttpServletRequest request) {
        String fields = exception.getBindingResult().getFieldErrors().stream()
            .map(error -> "El campo '" + error.getField() + "' no es válido")
            .distinct()
            .sorted()
            .collect(Collectors.joining(". "));
        String detail = fields.isBlank() ? "La solicitud contiene datos inválidos" : fields;
        ProblemDetail problem = ProblemDetails.create(HttpStatus.BAD_REQUEST.value(), "Solicitud inválida",
            detail, "validation_error", request);
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception,
                                                              HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.create(HttpStatus.BAD_REQUEST.value(), "Solicitud inválida",
            "La solicitud contiene datos inválidos", "validation_error", request);
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableMessage(HttpMessageNotReadableException exception,
                                                            HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.create(HttpStatus.BAD_REQUEST.value(), "Solicitud inválida",
            "El cuerpo de la solicitud no tiene un formato válido", "invalid_request", request);
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ProblemDetail> handleMethodValidation(HandlerMethodValidationException exception,
                                                          HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.create(HttpStatus.BAD_REQUEST.value(), "Solicitud inválida",
            "La solicitud contiene datos inválidos", "validation_error", request);
        return response(problem, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleMethodNotSupported(HttpRequestMethodNotSupportedException exception,
                                                             HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.create(HttpStatus.METHOD_NOT_ALLOWED.value(), "Método no permitido",
            "El método HTTP no está permitido para este recurso", "method_not_allowed", request);
        return response(problem, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleNotFound(NoResourceFoundException exception, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.create(HttpStatus.NOT_FOUND.value(), "Recurso no encontrado",
            "El recurso solicitado no existe", "not_found", request);
        return response(problem, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ProblemDetail> handleDataIntegrity(DataIntegrityViolationException exception,
                                                       HttpServletRequest request) {
        ProblemDetail problem = ProblemDetails.create(HttpStatus.CONFLICT.value(), "Conflicto de datos",
            "La operación no puede completarse por un conflicto de datos", "data_integrity_error", request);
        return response(problem, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        logger.error("Unexpected API failure", exception);
        ProblemDetail problem = ProblemDetails.create(HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Error interno", "Ocurrió un error interno", "internal_error", request);
        return response(problem, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<ProblemDetail> response(ProblemDetail problem, HttpStatus status) {
        return ResponseEntity.status(status)
            .contentType(MediaType.APPLICATION_PROBLEM_JSON)
            .body(problem);
    }
}
