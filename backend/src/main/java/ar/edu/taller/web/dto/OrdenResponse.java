package ar.edu.taller.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Vista completa de una orden. El total se calcula (manoObra + Σ cantidad×precio)
 * y NUNCA se lee de la base: si se corrompiera el cálculo, la API y la UI coinciden
 * porque ambas derivan del mismo campo "presupuesto.total".
 */
public record OrdenResponse(
        Long id,
        ClienteResponse cliente,
        String equipo,
        String problema,
        String estado,
        String diagnostico,
        BigDecimal manoObra,
        List<PiezaResponse> piezas,
        List<String> fotos,
        PresupuestoResponse presupuesto,
        List<String> transicionesPermitidas,
        Instant creadoEn,
        Instant actualizadoEn
) {
    public record PiezaResponse(String descripcion, Integer cantidad, BigDecimal precio, BigDecimal subtotal) {
    }

    /** Total derivado + detalle, para que el frontend no replique la fórmula sin querer. */
    public record PresupuestoResponse(boolean registrado, BigDecimal total) {
    }
}
