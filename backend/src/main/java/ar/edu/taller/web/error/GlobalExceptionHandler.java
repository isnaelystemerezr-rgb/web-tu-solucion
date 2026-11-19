package ar.edu.taller.web.error;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Manejo global de errores: SIEMPRE responde JSON con
 * { timestamp, status, codigo, mensaje, ruta, detalles[] }.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException ex,
                                                          HttpServletRequest req) {
        List<Map<String, String>> detalles = ex.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    Map<String, String> d = new LinkedHashMap<>();
                    String campo = err instanceof FieldError fe ? fe.getField() : err.getObjectName();
                    d.put("campo", campo);
                    d.put("mensaje", err.getDefaultMessage());
                    return d;
                })
                .toList();
        return construir(HttpStatus.BAD_REQUEST, "VALIDACION",
                "Hay errores de validación en el cuerpo de la petición", req, detalles);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> regla(ReglaNegocioException ex, HttpServletRequest req) {
        HttpStatus status = switch (ex.getCodigo()) {
            case "TRANSICION_INVALIDA", "CANCELACION_INVALIDA", "PRESUPUESTO_INMUTABLE" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
        return construir(status, ex.getCodigo(), ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex,
                                                            HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> recursoInexistente(NoResourceFoundException ex,
                                                                  HttpServletRequest req) {
        return construir(HttpStatus.NOT_FOUND, "NO_ENCONTRADO",
                "No existe el recurso " + req.getRequestURI(), req, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException ex,
                                                              HttpServletRequest req) {
        String detalle = ex.getMostSpecificCause().getMessage();
        return construir(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "JSON inválido o con tipos incorrectos: " + detalle, req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoInvalido(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "PARAMETRO_INVALIDO",
                "El parámetro '" + ex.getName() + "' tiene un valor inválido: " + ex.getValue(), req, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> faltaParametro(MissingServletRequestParameterException ex,
                                                              HttpServletRequest req) {
        return construir(HttpStatus.BAD_REQUEST, "PARAMETRO_FALTANTE",
                "Falta el parámetro obligatorio '" + ex.getParameterName() + "'", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> metodoNoSoportado(HttpRequestMethodNotSupportedException ex,
                                                                 HttpServletRequest req) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO",
                "Método " + ex.getMethod() + " no permitido. Permitidos: " + ex.getSupportedHttpMethods(), req, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> archivoMuyGrande(MaxUploadSizeExceededException ex,
                                                                HttpServletRequest req) {
        return construir(HttpStatus.PAYLOAD_TOO_LARGE, "ARCHIVO_MUY_GRANDE",
                "El archivo supera el tamaño máximo permitido (5 MB por foto)", req, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> argumentoInvalido(IllegalArgumentException ex,
                                                                 HttpServletRequest req) {
        String mensaje = ex.getMessage() == null ? "Parámetro inválido" : ex.getMessage();
        String codigo = mensaje.startsWith("Estado inválido") ? "ESTADO_INVALIDO" : "ARGUMENTO_INVALIDO";
        return construir(HttpStatus.BAD_REQUEST, codigo, mensaje, req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generico(Exception ex, HttpServletRequest req) {
        return construir(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO",
                "Error interno: " + ex.getMessage(), req, List.of());
    }

    private ResponseEntity<Map<String, Object>> construir(HttpStatus status, String codigo, String mensaje,
                                                           HttpServletRequest req,
                                                           List<Map<String, String>> detalles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("status", status.value());
        body.put("codigo", codigo);
        body.put("mensaje", mensaje);
        body.put("ruta", req.getRequestURI());
        body.put("detalles", detalles);
        return ResponseEntity.status(status).body(body);
    }
}
