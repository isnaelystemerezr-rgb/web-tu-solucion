package ar.edu.taller.web;

import ar.edu.taller.domain.EstadoOrden;
import ar.edu.taller.domain.Orden;
import ar.edu.taller.service.OrdenMapper;
import ar.edu.taller.service.OrdenService;
import ar.edu.taller.web.dto.OrdenRequest;
import ar.edu.taller.web.dto.OrdenResponse;
import ar.edu.taller.web.dto.OrdenUpdateRequest;
import ar.edu.taller.web.dto.TransicionRequest;
import ar.edu.taller.web.error.ReglaNegocioException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Órdenes: listar, ver, crear, actualizar campos permitidos, cambiar estado, fotos.
 *
 * IMPORTANTE: no existe DELETE /api/ordenes/{id}. Las órdenes no se borran (el
 * histórico de reparaciones es el activo del taller); Spring devuelve 405.
 */
@RestController
@RequestMapping("/api/ordenes")
public class OrdenController {

    private final OrdenService service;
    private final OrdenMapper mapper;

    public OrdenController(OrdenService service, OrdenMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /** Lista órdenes. Filtro opcional por estado: /api/ordenes?estado=en_reparacion */
    @GetMapping
    public List<OrdenResponse> listar(@RequestParam(required = false) String estado) {
        EstadoOrden filtro = EstadoOrden.fromValor(estado);
        return service.listar(filtro).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{id}")
    public OrdenResponse ver(@PathVariable Long id) {
        return mapper.toResponse(service.obtener(id));
    }

    /** Crear orden (JSON) — nace en "solicitada". */
    @PostMapping
    public ResponseEntity<OrdenResponse> crear(@Valid @RequestBody OrdenRequest req) {
        Orden orden = service.crear(req, List.of());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(orden));
    }

    /** Crear orden con fotos en la misma petición (multipart/form-data). */
    @PostMapping(value = "/con-fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<OrdenResponse> crearConFotos(@RequestParam Long clienteId,
                                                       @RequestParam String equipo,
                                                       @RequestParam String problema,
                                                       @RequestPart(required = false) List<MultipartFile> fotos) {
        OrdenRequest req = new OrdenRequest(clienteId, equipo, problema);
        // Bean Validation se aplica a mano porque aquí no hay @RequestBody.
        if (clienteId == null || equipo == null || equipo.isBlank() || problema == null || problema.isBlank()) {
            throw new ReglaNegocioException("VALIDACION", "clienteId, equipo y problema son obligatorios");
        }
        Orden orden = service.crear(req, fotos == null ? List.of() : fotos);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(orden));
    }

    /** Actualización de campos permitidos (nunca estado; nunca presupuesto ya registrado). */
    @PutMapping("/{id}")
    public OrdenResponse actualizar(@PathVariable Long id, @Valid @RequestBody OrdenUpdateRequest req) {
        return mapper.toResponse(service.actualizar(id, req));
    }

    /**
     * Cambio de estado. Único camino para mover una orden.
     * POST /api/ordenes/{id}/transicion  { "a": "presupuestada", ...presupuesto }
     */
    @PostMapping("/{id}/transicion")
    public OrdenResponse transicion(@PathVariable Long id, @Valid @RequestBody TransicionRequest req) {
        Orden orden = service.cambiarEstado(id, req.a(), req);
        return mapper.toResponse(orden);
    }

    /** Subida de hasta 3 fotos por orden (se suman a las existentes hasta el máximo). */
    @PostMapping(value = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OrdenResponse subirFotos(@PathVariable Long id,
                                    @RequestPart("fotos") List<MultipartFile> fotos) {
        return mapper.toResponse(service.agregarFotos(id, fotos));
    }

    /**
     * Total calculado EN EL SERVIDOR: manoObra + Σ(cantidad × precio).
     * Endpoint de solo lectura; no existe columna "total" en la base.
     */
    @GetMapping("/{id}/total")
    public Map<String, Object> total(@PathVariable Long id) {
        Orden orden = service.obtener(id);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ordenId", orden.getId());
        body.put("manoObra", orden.getManoObra() == null ? BigDecimal.ZERO : orden.getManoObra());
        body.put("piezas", orden.getPiezas().stream()
                .map(p -> Map.of("descripcion", p.getDescripcion(),
                        "cantidad", p.getCantidad(),
                        "precio", p.getPrecio(),
                        "subtotal", p.subtotal()))
                .toList());
        body.put("total", orden.calcularTotal());
        body.put("registrado", orden.isPresupuestoRegistrado());
        return body;
    }

    /** Resumen por etapa (opcional en el enunciado, requerido por la pantalla de resumen). */
    @GetMapping("/resumen")
    public List<Map<String, Object>> resumen() {
        return service.resumenPorEstado().entrySet().stream()
                .map(e -> {
                    Map<String, Object> fila = new LinkedHashMap<>();
                    fila.put("estado", e.getKey().getValor());
                    fila.put("cantidad", e.getValue());
                    fila.put("final", e.getKey().esFinal());
                    return fila;
                })
                .toList();
    }
}
