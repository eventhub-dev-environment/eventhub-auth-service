package com.eventhub.auth_service.infrastructure.adapter.in.web;

import com.eventhub.auth_service.domain.exception.InvalidCredentialsException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ProblemDetail handleInvalidCredentials(InvalidCredentialsException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problemDetail.setTitle("Error de Autenticación");
        problemDetail.setType(URI.create("https://eventhub.com/errors/unauthorized"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Error de validación en los campos");
        problemDetail.setTitle("Petición Inválida");
        problemDetail.setProperty("invalidFields", errors);
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleInvalidJson(HttpMessageNotReadableException ex) {
        String errorMessage = "El cuerpo de la solicitud es inválido o contiene valores no permitidos.";

        if (ex.getCause() instanceof InvalidFormatException ife && ife.getTargetType().isEnum()) {
            errorMessage = String.format("Valor no permitido en el cuerpo. Los valores válidos para %s son: %s",
                    ife.getTargetType().getSimpleName(),
                    Arrays.toString(ife.getTargetType().getEnumConstants()));
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorMessage);
        problemDetail.setTitle("Lectura de JSON Fallida");
        problemDetail.setType(URI.create("https://eventhub.com/errors/invalid-json"));
        problemDetail.setProperty("timestamp", Instant.now());
        return problemDetail;
    }
}