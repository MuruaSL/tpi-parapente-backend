package com.parapente.vuelos.errores;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.parapente.vuelos.reservas.ReservaRechazadaException;

/**
 * Traduce excepciones a JSON uniforme: {timestamp, status, error} (+ detalles en validación).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        Map<String, String> detalles = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> detalles.put(f.getField(), f.getDefaultMessage()));
        Map<String, Object> cuerpo = cuerpo(HttpStatus.BAD_REQUEST, "Error de validación");
        cuerpo.put("detalles", detalles);
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    public ResponseEntity<Map<String, Object>> cuerpoInvalido(Exception e) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Solicitud mal formada"));
    }

    /** Paginación/orden inválidos (p. ej. ?sort=inexistente o ?size=abc): 400 con el JSON del contrato. */
    @ExceptionHandler({ PropertyReferenceException.class, IllegalArgumentException.class })
    public ResponseEntity<Map<String, Object>> parametrosInvalidos(Exception e) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Parámetros de consulta inválidos"));
    }

    /** Último recurso: 500 sin detalles internos en la respuesta; el error real queda en el log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        if (e instanceof ErrorResponse er) { // 404/405/415 propios de Spring MVC: se conserva su status
            HttpStatus st = HttpStatus.valueOf(er.getStatusCode().value());
            return ResponseEntity.status(st).body(cuerpo(st, st.getReasonPhrase()));
        }
        log.error("Error inesperado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(cuerpo(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(HttpStatus.NOT_FOUND, e.getMessage()));
    }

    /** Reserva rechazada: además del error, informa la reserva que quedó guardada como RECHAZADA. */
    @ExceptionHandler(ReservaRechazadaException.class)
    public ResponseEntity<Map<String, Object>> rechazada(ReservaRechazadaException e) {
        Map<String, Object> cuerpo = cuerpo(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        cuerpo.put("reservaId", e.getReserva().id());
        cuerpo.put("estado", e.getReserva().estado());
        cuerpo.put("motivo", e.getReserva().motivo());
        return ResponseEntity.unprocessableEntity().body(cuerpo);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException e) {
        return ResponseEntity.unprocessableEntity().body(cuerpo(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage()));
    }

    @ExceptionHandler(DependenciaNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> dependenciaCaida(DependenciaNoDisponibleException e) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(cuerpo(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage()));
    }

    private Map<String, Object> cuerpo(HttpStatus status, String error) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("timestamp", LocalDateTime.now().toString());
        cuerpo.put("status", status.value());
        cuerpo.put("error", error);
        return cuerpo;
    }
}
