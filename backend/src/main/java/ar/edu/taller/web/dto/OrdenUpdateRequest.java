package ar.edu.taller.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Actualización de una orden. Solo se aceptan los campos que el enunciado permite editar.
 *
 * El campo {@code estado} NO existe aquí: se cambia únicamente por
 * POST /ordenes/{id}/transicion. El presupuesto (diagnostico/manoObra/piezas) solo
 * se puede escribir mientras la orden NO tenga presupuesto registrado; el servicio
 * rechaza cualquier intento posterior (ver OrdenService#actualizar).
 */
public record OrdenUpdateRequest(
        @Positive(message = "clienteId inválido")
        Long clienteId,

        @Size(max = 120, message = "El equipo no puede superar 120 caracteres")
        String equipo,

        @Size(max = 1000, message = "El problema no puede superar 1000 caracteres")
        String problema,

        @Size(max = 1000, message = "El diagnóstico no puede superar 1000 caracteres")
        String diagnostico,

        @PositiveOrZero(message = "La mano de obra no puede ser negativa")
        BigDecimal manoObra,

        @Valid
        @Size(max = 50, message = "Demasiadas piezas")
        List<PiezaRequest> piezas
) {
}
