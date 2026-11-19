package ar.edu.taller.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Creación de una orden.
 *
 * A propósito NO incluye {@code estado}: toda orden nace en "solicitada" y solo se
 * mueve con POST /ordenes/{id}/transicion. Tampoco incluye fotos: se suben aparte
 * por multipart. Permite, si se envían, las fotos por URL relativa para el seed
 * o pruebas de API (se valida el máximo de 3 en el servicio).
 */
public record OrdenRequest(
        @NotNull(message = "El cliente es obligatorio")
        @Positive(message = "clienteId inválido")
        Long clienteId,

        @NotBlank(message = "El equipo es obligatorio")
        @Size(max = 120, message = "El equipo no puede superar 120 caracteres")
        String equipo,

        @NotBlank(message = "El problema es obligatorio")
        @Size(max = 1000, message = "El problema no puede superar 1000 caracteres")
        String problema
) {
}
