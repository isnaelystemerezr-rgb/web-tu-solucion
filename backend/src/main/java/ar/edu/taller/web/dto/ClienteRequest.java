package ar.edu.taller.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Alta/edición de cliente. */
public record ClienteRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre no puede superar 80 caracteres")
        String nombre,

        @NotBlank(message = "El teléfono es obligatorio")
        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        @Pattern(regexp = "^[0-9+()\\-\\s]{6,30}$", message = "Formato de teléfono inválido")
        String telefono
) {
}
