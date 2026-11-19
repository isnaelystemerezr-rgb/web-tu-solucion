package ar.edu.taller.service;

import ar.edu.taller.domain.EstadoOrden;
import ar.edu.taller.domain.Evento;
import ar.edu.taller.domain.Orden;
import ar.edu.taller.repo.EventoRepository;
import ar.edu.taller.repo.OrdenRepository;
import ar.edu.taller.web.dto.EventoRequest;
import ar.edu.taller.web.dto.TransicionRequest;
import ar.edu.taller.web.error.RecursoNoEncontradoException;
import ar.edu.taller.web.error.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lectura y creación de eventos.
 *
 * No existe ningún método de actualización ni de borrado: la API tampoco los expone
 * (PUT/PATCH/DELETE sobre /eventos devuelven 405, ver EventoController).
 *
 * Crear un evento por POST /eventos es equivalente a hacer la transición de la orden:
 * se delega en OrdenService.aplicarTransicion para que las reglas de estado y la
 * creación del evento sigan siendo una sola implementación.
 */
@Service
public class EventoService {

    private final EventoRepository eventos;
    private final OrdenRepository ordenes;
    private final OrdenService ordenService;

    public EventoService(EventoRepository eventos, OrdenRepository ordenes, OrdenService ordenService) {
        this.eventos = eventos;
        this.ordenes = ordenes;
        this.ordenService = ordenService;
    }

    @Transactional(readOnly = true)
    public List<Evento> listar(Long ordenId) {
        if (ordenId == null) {
            return eventos.findAll();
        }
        if (!ordenes.existsById(ordenId)) {
            throw new RecursoNoEncontradoException("No existe la orden " + ordenId);
        }
        return eventos.findByOrdenIdOrderByCreadoEnAscIdAsc(ordenId);
    }

    @Transactional(readOnly = true)
    public Evento buscar(Long id) {
        return eventos.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el evento " + id));
    }

    @Transactional
    public Evento crear(EventoRequest req) {
        Orden orden = ordenes.findById(req.ordenId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + req.ordenId()));

        EstadoOrden destino;
        try {
            destino = EstadoOrden.fromValor(req.a());
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException("ESTADO_INVALIDO", ex.getMessage());
        }

        // Reutiliza la validación y la creación del evento de OrdenService.
        // Si el destino es "presupuestada", exigirá el presupuesto como en /transicion.
        ordenService.aplicarTransicion(orden, destino, new TransicionRequest(destino.getValor(), null, null, null));

        // Devolvemos el evento recién creado (id más alto de la orden).
        return eventos.findByOrdenIdOrderByCreadoEnAscIdAsc(orden.getId()).stream()
                .max(java.util.Comparator.comparing(Evento::getId))
                .orElseThrow(() -> new IllegalStateException("No se creó el evento de la transición"));
    }
}
