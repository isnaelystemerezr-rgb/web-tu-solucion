package ar.edu.taller.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de POST /ordenes/{id}/transicion.
 *
 * Además del estado destino, permite enviar el presupuesto cuando la transición
 * lleva a "presupuestada": el presupuesto se registra EN ESA MISMA transacción y
 * queda inmutable para siempre.
 */
public record TransicionRequest(
        @NotNull(message = "El estado destino es obligatorio")
        String a,

        // ---- Solo se aceptan (y solo se aplican) al pasar a "presupuestada" ----
        @Size(max = 1000, message = "El diagnóstico no puede superar 1000 caracteres")
        String diagnostico,

        @PositiveOrZero(message = "La mano de obra no puede ser negativa")
        BigDecimal manoObra,

        @Valid
        @Size(max = 50, message = "Demasiadas piezas")
        List<PiezaRequest> piezas
) {
    public boolean llevaPresupuesto() {
        return diagnostico != null || manoObra != null || piezas != null;
    }
}
