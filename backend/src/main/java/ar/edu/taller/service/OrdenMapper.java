package ar.edu.taller.service;

import ar.edu.taller.domain.EstadoOrden;
import ar.edu.taller.domain.Orden;
import ar.edu.taller.domain.Pieza;
import ar.edu.taller.web.dto.ClienteResponse;
import ar.edu.taller.web.dto.EventoResponse;
import ar.edu.taller.web.dto.OrdenResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Mapeo entidad → DTO.
 *
 * El total del presupuesto se calcula aquí (no existe columna "total" en la base):
 *   total = manoObra + Σ(cantidad × precio)
 * y también se expone el subtotal por pieza para que el frontend pueda mostrar el detalle.
 */
@Component
public class OrdenMapper {

    public OrdenResponse toResponse(Orden orden) {
        List<OrdenResponse.PiezaResponse> piezas = new ArrayList<>();
        for (Pieza p : orden.getPiezas()) {
            piezas.add(new OrdenResponse.PiezaResponse(
                    p.getDescripcion(), p.getCantidad(), p.getPrecio(), p.subtotal()));
        }

        BigDecimal total = orden.calcularTotal();
        boolean registrado = orden.isPresupuestoRegistrado();

        // El total solo tiene sentido una vez registrado el presupuesto; antes es 0.
        BigDecimal totalPublicado = registrado ? total : BigDecimal.ZERO;

        List<String> transiciones = orden.getEstado().siguientes().stream()
                .map(EstadoOrden::getValor)
                .sorted()
                .toList();

        return new OrdenResponse(
                orden.getId(),
                ClienteResponse.de(orden.getCliente()),
                orden.getEquipo(),
                orden.getProblema(),
                orden.getEstado().getValor(),
                orden.getDiagnostico(),
                orden.getManoObra(),
                piezas,
                List.copyOf(orden.getFotos()),
                new OrdenResponse.PresupuestoResponse(registrado, totalPublicado),
                transiciones,
                orden.getCreadoEn(),
                orden.getActualizadoEn()
        );
    }

    public EventoResponse toResponse(ar.edu.taller.domain.Evento e) {
        return new EventoResponse(e.getId(), e.getOrden().getId(), e.getDe(), e.getA(), e.getCreadoEn());
    }
}
