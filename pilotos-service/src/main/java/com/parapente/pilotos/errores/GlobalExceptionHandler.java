package com.parapente.pilotos.errores;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduce excepciones a JSON uniforme {timestamp, status, error}, igual en los tres servicios.
 * Así los controladores no tienen try/catch.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> detalles.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return respuesta(HttpStatus.BAD_REQUEST, "datos inválidos", detalles);
    }

    /** Cuerpo ilegible, parámetro faltante o con formato incorrecto (p. ej. fecha mal escrita). */
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, Object>> peticionInvalida(Exception e) {
        return respuesta(HttpStatus.BAD_REQUEST, "petición inválida", null);
    }

    /** La base no responde: es una dependencia caída, no un error del cliente. */
    @ExceptionHandler(DataAccessResourceFailureException.class)
    public ResponseEntity<Map<String, Object>> baseCaida(DataAccessResourceFailureException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "base de datos no disponible", null);
    }

    /** Arma el JSON del contrato; "detalles" sólo aparece en errores de validación. */
    static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, Map<String, String> detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", Instant.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", error);
        if (detalles != null) cuerpo.put("detalles", detalles);
        return ResponseEntity.status(status).body(cuerpo);
    }
}
