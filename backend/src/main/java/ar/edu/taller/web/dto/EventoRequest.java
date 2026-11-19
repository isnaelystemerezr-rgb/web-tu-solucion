package ar.edu.taller.web.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Cuerpo de POST /eventos (solo pruebas/importaciones).
 *
 * El "de" NO se recibe: el servidor lo deduce del estado actual de la orden y valida
 * la transición igual que en /transicion, para que no se puedan inyectar eventos
 * falsos que contradigan el estado real.
 */
public record EventoRequest(
        @NotNull(message = "La orden es obligatoria")
        @Positive(message = "ordenId inválido")
        Long ordenId,

        @NotNull(message = "El estado destino es obligatorio")
        String a
) {
}
