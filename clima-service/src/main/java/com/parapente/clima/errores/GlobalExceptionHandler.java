package com.parapente.clima.errores;

import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce excepciones al JSON de errores común: {timestamp, status, error[, detalles]}. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Falta un parámetro obligatorio. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> faltaParametro(MissingServletRequestParameterException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos",
                Map.of(e.getParameterName(), "es obligatorio"));
    }

    /** Parámetro con formato incorrecto (ej.: fecha mal escrita, texto en vez de número). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoIncorrecto(MethodArgumentTypeMismatchException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos",
                Map.of(e.getName(), "formato inválido"));
    }

    /** Falló una anotación de validación (rangos de latitud, viento negativo, etc.). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> validacion(ConstraintViolationException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getConstraintViolations().forEach(v -> {
            String ruta = v.getPropertyPath().toString();
            detalles.put(ruta.substring(ruta.lastIndexOf('.') + 1), v.getMessage());
        });
        return respuesta(HttpStatus.BAD_REQUEST, "Parámetros inválidos", detalles);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException e) {
        return respuesta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null);
    }

    @ExceptionHandler(ClimaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> climaCaido(ClimaNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage(), null);
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String error, Map<String, String> detalles) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", error);
        if (detalles != null) {
            cuerpo.put("detalles", detalles);
        }
        return ResponseEntity.status(status).body(cuerpo);
    }
}
