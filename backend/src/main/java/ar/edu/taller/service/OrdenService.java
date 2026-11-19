package ar.edu.taller.service;

import ar.edu.taller.domain.Cliente;
import ar.edu.taller.domain.EstadoOrden;
import ar.edu.taller.domain.Evento;
import ar.edu.taller.domain.Orden;
import ar.edu.taller.domain.Pieza;
import ar.edu.taller.repo.ClienteRepository;
import ar.edu.taller.repo.EventoRepository;
import ar.edu.taller.repo.OrdenRepository;
import ar.edu.taller.web.dto.OrdenRequest;
import ar.edu.taller.web.dto.OrdenUpdateRequest;
import ar.edu.taller.web.dto.PiezaRequest;
import ar.edu.taller.web.dto.TransicionRequest;
import ar.edu.taller.web.error.RecursoNoEncontradoException;
import ar.edu.taller.web.error.ReglaNegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de las órdenes.
 *
 * Aquí están las garantías que el backend debe cumplir "aunque se llame la API
 * directamente sin pasar por la UI":
 *
 *  1. Toda transición se valida contra {@link EstadoOrden#puedeTransicionarA}.
 *  2. Cada cambio de estado inserta un {@link Evento} con la hora del servidor,
 *     dentro de la MISMA transacción (@Transactional).
 *  3. El presupuesto se registra una única vez (al pasar a "presupuestada") y
 *     no se puede modificar después: se comprueba por estado y por la bandera
 *     persistida {@code presupuestoRegistrado}.
 *  4. La cancelación solo es válida desde "solicitada" (el grafo no tiene otra arista
 *     hacia cancelada, y además se valida explícitamente por claridad del error).
 *  5. El total nunca se persiste: se calcula en OrdenMapper/calcularTotal.
 *  6. Las órdenes no se borran: no existe método de borrado en este servicio.
 */
@Service
public class OrdenService {

    private final OrdenRepository ordenes;
    private final ClienteRepository clientes;
    private final EventoRepository eventos;
    private final OrdenMapper mapper;
    private final FotoStorageService fotos;
    private final int maxFotos;

    public OrdenService(OrdenRepository ordenes,
                        ClienteRepository clientes,
                        EventoRepository eventos,
                        OrdenMapper mapper,
                        FotoStorageService fotos,
                        @Value("${app.fotos.max-por-orden:3}") int maxFotos) {
        this.ordenes = ordenes;
        this.clientes = clientes;
        this.eventos = eventos;
        this.mapper = mapper;
        this.fotos = fotos;
        this.maxFotos = maxFotos;
    }

    // ------------------------------------------------------------------ lectura

    @Transactional(readOnly = true)
    public List<Orden> listar(EstadoOrden estado) {
        return estado == null
                ? ordenes.findAllByOrderByCreadoEnDesc()
                : ordenes.findByEstadoOrderByCreadoEnDesc(estado);
    }

    @Transactional(readOnly = true)
    public Orden obtener(Long id) {
        return ordenes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la orden " + id));
    }

    /** Resumen por etapa: devuelve TODAS las etapas (incluidas las que tienen 0). */
    @Transactional(readOnly = true)
    public Map<EstadoOrden, Long> resumenPorEstado() {
        Map<EstadoOrden, Long> resumen = new EnumMap<>(EstadoOrden.class);
        for (EstadoOrden e : EstadoOrden.values()) {
            resumen.put(e, 0L);
        }
        for (Object[] fila : ordenes.contarPorEstado()) {
            resumen.put((EstadoOrden) fila[0], ((Number) fila[1]).longValue());
        }
        return resumen;
    }

    @Transactional(readOnly = true)
    public List<Evento> eventosDe(Long ordenId) {
        // Valida existencia para no devolver [] cuando la orden no existe.
        obtener(ordenId);
        return eventos.findByOrdenIdOrderByCreadoEnAscIdAsc(ordenId);
    }

    // ------------------------------------------------------------------ alta

    /**
     * Crea una orden en estado "solicitada" y deja el evento inicial registrado.
     * También admite fotos (multipart) en la misma operación.
     */
    @Transactional
    public Orden crear(OrdenRequest req, List<MultipartFile> fotosSubidas) {
        Cliente cliente = clientes.findById(req.clienteId())
                .orElseThrow(() -> new ReglaNegocioException("CLIENTE_INEXISTENTE",
                        "No existe el cliente " + req.clienteId()));

        Orden orden = new Orden(cliente, req.equipo().trim(), req.problema().trim());
        orden.setEstado(EstadoOrden.SOLICITADA);

        if (fotosSubidas != null && !fotosSubidas.isEmpty()) {
            // Se valida ANTES de escribir en disco para no dejar archivos huérfanos.
            validarCantidadFotos(fotosSubidas.size());
            orden.setFotos(fotos.guardar(fotosSubidas));
        }

        Orden guardada = ordenes.save(orden);

        // Evento de apertura: deja trazabilidad desde el minuto cero.
        eventos.save(Evento.registrar(guardada, EstadoOrden.SOLICITADA, EstadoOrden.SOLICITADA, Instant.now()));
        return guardada;
    }

    // ------------------------------------------------------------------ edición

    /**
     * Actualiza los campos permitidos. El presupuesto solo se puede escribir si
     * todavía NO fue registrado; en cuanto la orden pasó por "presupuestada",
     * cualquier intento de cambiarlo se rechaza (inmutabilidad).
     */
    @Transactional
    public Orden actualizar(Long id, OrdenUpdateRequest req) {
        Orden orden = obtener(id);

        boolean intentaTocarPresupuesto =
                req.diagnostico() != null || req.manoObra() != null || req.piezas() != null;

        if (intentaTocarPresupuesto && (orden.isPresupuestoRegistrado() || orden.getEstado().esTerminalOAvanzado())) {
            throw new ReglaNegocioException("PRESUPUESTO_INMUTABLE",
                    "El presupuesto ya fue registrado y no puede modificarse. "
                            + "Solo puede cargarse una vez, al pasar la orden a 'presupuestada'.");
        }

        if (req.clienteId() != null && !req.clienteId().equals(orden.getCliente().getId())) {
            // Cambiar de cliente solo tiene sentido mientras la orden no avanzó.
            if (orden.getEstado() != EstadoOrden.SOLICITADA && orden.getEstado() != EstadoOrden.EN_DIAGNOSTICO) {
                throw new ReglaNegocioException("ORDEN_AVANZADA",
                        "No se puede cambiar el cliente de una orden en estado '" + orden.getEstado().getValor() + "'");
            }
            Cliente nuevo = clientes.findById(req.clienteId())
                    .orElseThrow(() -> new ReglaNegocioException("CLIENTE_INEXISTENTE",
                            "No existe el cliente " + req.clienteId()));
            orden.setCliente(nuevo);
        }

        if (req.equipo() != null && !req.equipo().isBlank()) {
            orden.setEquipo(req.equipo().trim());
        }
        if (req.problema() != null && !req.problema().isBlank()) {
            orden.setProblema(req.problema().trim());
        }
        if (req.diagnostico() != null) {
            orden.setDiagnostico(req.diagnostico().trim());
        }
        if (req.manoObra() != null) {
            orden.setManoObra(req.manoObra());
        }
        if (req.piezas() != null) {
            orden.setPiezas(aPiezas(req.piezas()));
        }

        orden.setActualizadoEn(Instant.now());
        return ordenes.save(orden);
    }

    /** Agrega fotos a una orden respetando el máximo total (3 por defecto). */
    @Transactional
    public Orden agregarFotos(Long id, List<MultipartFile> nuevas) {
        if (nuevas == null || nuevas.isEmpty()) {
            throw new ReglaNegocioException("SIN_ARCHIVOS", "No se recibió ninguna foto");
        }
        Orden orden = obtener(id);
        int espacio = maxFotos - orden.getFotos().size();
        if (espacio <= 0) {
            throw new ReglaNegocioException("LIMITE_FOTOS",
                    "La orden ya tiene el máximo de " + maxFotos + " fotos");
        }
        if (nuevas.size() > espacio) {
            throw new ReglaNegocioException("LIMITE_FOTOS",
                    "Solo se pueden agregar " + espacio + " foto(s) más (máximo " + maxFotos + " por orden)");
        }
        List<String> rutas = new ArrayList<>(orden.getFotos());
        rutas.addAll(fotos.guardar(nuevas));
        orden.setFotos(rutas);
        orden.setActualizadoEn(Instant.now());
        return ordenes.save(orden);
    }

    // ------------------------------------------------------------------ transiciones

    /**
     * Cambio de estado: el ÚNICO punto de la aplicación que mueve una orden.
     *
     * Pasos (todos en una transacción):
     *  1. Se carga la orden.
     *  2. Se valida la transición contra el grafo del enum (rechaza saltos).
     *  3. Si el destino es "presupuestada", se exige y se graba el presupuesto.
     *  4. Se persiste el nuevo estado.
     *  5. Se inserta el evento {orden, de, a, creadoEn=ahora}. Si el evento fallara,
     *     la transacción entera se revierte: nunca hay cambio de estado sin evento.
     */
    @Transactional
    public Orden cambiarEstado(Long id, String estadoDestino, TransicionRequest cuerpo) {
        Orden orden = obtener(id);
        EstadoOrden destino;
        try {
            destino = EstadoOrden.fromValor(estadoDestino);
        } catch (IllegalArgumentException ex) {
            throw new ReglaNegocioException("ESTADO_INVALIDO", ex.getMessage());
        }
        if (destino == null) {
            throw new ReglaNegocioException("ESTADO_INVALIDO", "El estado destino es obligatorio");
        }
        return aplicarTransicion(orden, destino, cuerpo);
    }

    /**
     * Núcleo reutilizado por /transicion y por POST /eventos. Mantiene una sola
     * implementación de las reglas para que no puedan divergir.
     */
    @Transactional
    public Orden aplicarTransicion(Orden orden, EstadoOrden destino, TransicionRequest cuerpo) {
        EstadoOrden actual = orden.getEstado();

        // (1) Validación estricta de la transición: sin saltos, sin estados finales.
        if (!actual.puedeTransicionarA(destino)) {
            throw new ReglaNegocioException("TRANSICION_INVALIDA",
                    "Transición no permitida: '" + actual.getValor() + "' → '" + destino.getValor() + "'. "
                            + (actual.esFinal()
                                ? "El estado '" + actual.getValor() + "' es final."
                                : "Desde '" + actual.getValor() + "' solo se puede pasar a "
                                  + actual.siguientes().stream().map(EstadoOrden::getValor).sorted().toList() + "."));
        }

        // (2) Reglas específicas por transición.
        if (destino == EstadoOrden.CANCELADA && actual != EstadoOrden.SOLICITADA) {
            // Redundante con el grafo, pero deja un mensaje explícito para la defensa.
            throw new ReglaNegocioException("CANCELACION_INVALIDA",
                    "Solo se puede cancelar una orden en estado 'solicitada'. Estado actual: '" + actual.getValor() + "'");
        }

        if (destino == EstadoOrden.PRESUPUESTADA) {
            registrarPresupuesto(orden, cuerpo);
        }

        // (3) Persistir el cambio + evento, en la misma transacción.
        Instant ahora = Instant.now();
        orden.setEstado(destino);
        orden.setActualizadoEn(ahora);
        Orden guardada = ordenes.save(orden);

        eventos.save(Evento.registrar(guardada, actual, destino, ahora));
        return guardada;
    }

    /**
     * Registro del presupuesto. Se ejecuta SOLO en la transición a "presupuestada"
     * y deja la bandera presupuestoRegistrado en true: a partir de ahí, cualquier
     * actualización de diagnostico/manoObra/piezas es rechazada.
     */
    private void registrarPresupuesto(Orden orden, TransicionRequest cuerpo) {
        if (orden.isPresupuestoRegistrado()) {
            throw new ReglaNegocioException("PRESUPUESTO_INMUTABLE",
                    "Esta orden ya tiene un presupuesto registrado; no puede registrarse otra vez.");
        }
        if (cuerpo == null) {
            throw new ReglaNegocioException("PRESUPUESTO_REQUERIDO",
                    "Para pasar a 'presupuestada' hay que enviar el presupuesto (diagnostico, manoObra, piezas)");
        }
        if (cuerpo.diagnostico() == null || cuerpo.diagnostico().isBlank()) {
            throw new ReglaNegocioException("PRESUPUESTO_REQUERIDO",
                    "El diagnóstico es obligatorio para presupuestar");
        }
        if (cuerpo.manoObra() == null || cuerpo.manoObra().signum() < 0) {
            throw new ReglaNegocioException("PRESUPUESTO_REQUERIDO",
                    "La mano de obra es obligatoria y no puede ser negativa al presupuestar");
        }
        if (cuerpo.piezas() == null) {
            throw new ReglaNegocioException("PRESUPUESTO_REQUERIDO",
                    "La lista de piezas es obligatoria (puede ser vacía) para presupuestar");
        }

        orden.setDiagnostico(cuerpo.diagnostico().trim());
        orden.setManoObra(cuerpo.manoObra());
        orden.setPiezas(aPiezas(cuerpo.piezas()));
        orden.setPresupuestoRegistrado(true);
    }

    // ------------------------------------------------------------------ helpers

    private List<Pieza> aPiezas(List<PiezaRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        return requests.stream()
                .map(p -> new Pieza(p.descripcion().trim(), p.cantidad(), p.precio()))
                .toList();
    }

    private void validarCantidadFotos(int cantidad) {
        if (cantidad > maxFotos) {
            throw new ReglaNegocioException("LIMITE_FOTOS",
                    "Máximo " + maxFotos + " fotos por orden (se recibieron " + cantidad + ")");
        }
    }

    /** Solo para que el controlador pueda mapear la entidad devuelta. */
    public ar.edu.taller.web.dto.OrdenResponse toResponse(Orden orden) {
        return mapper.toResponse(orden);
    }

    public BigDecimal total(Orden orden) {
        return orden.calcularTotal();
    }
}
