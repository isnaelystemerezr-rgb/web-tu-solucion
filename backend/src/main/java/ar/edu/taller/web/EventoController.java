package ar.edu.taller.web;

import ar.edu.taller.domain.Evento;
import ar.edu.taller.service.EventoService;
import ar.edu.taller.service.OrdenMapper;
import ar.edu.taller.web.dto.EventoRequest;
import ar.edu.taller.web.dto.EventoResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Eventos: SOLO lectura y creación.
 *
 * PUT / PATCH / DELETE sobre /api/eventos/** no están mapeados; Spring responde
 * 405 Method Not Allowed (mapeado a JSON por GlobalExceptionHandler). Esto es
 * deliberado: la auditoría es inmutable.
 */
@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService service;
    private final OrdenMapper mapper;

    public EventoController(EventoService service, OrdenMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** GET /api/eventos?ordenId=1  (sin filtro devuelve todos, ordenados por fecha). */
    @GetMapping
    public List<EventoResponse> listar(@RequestParam(required = false) Long ordenId) {
        return service.listar(ordenId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public EventoResponse ver(@PathVariable Long id) {
        return mapper.toResponse(service.buscar(id));
    }

    /**
     * POST /api/eventos — crea el evento Y valida/efectúa la transición de la orden.
     * La fecha la pone el servidor; el "de" se deduce del estado actual.
     */
    @PostMapping
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest req) {
        Evento evento = service.crear(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(evento));
    }

    /** Respuesta explícita y clara si alguien intenta editar un evento. */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> noEditable(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("codigo", "EVENTO_INMUTABLE",
                        "mensaje", "Los eventos son de solo lectura: no se pueden editar"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Map<String, Object>> noParcheable(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("codigo", "EVENTO_INMUTABLE",
                        "mensaje", "Los eventos son de solo lectura: no se pueden editar"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> noBorrable(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Map.of("codigo", "EVENTO_INMUTABLE",
                        "mensaje", "Los eventos son de solo lectura: no se pueden borrar"));
    }
}
