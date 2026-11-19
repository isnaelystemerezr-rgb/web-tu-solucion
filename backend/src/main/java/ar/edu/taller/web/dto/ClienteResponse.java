package ar.edu.taller.web.dto;

import ar.edu.taller.domain.Cliente;

import java.time.Instant;

public record ClienteResponse(Long id, String nombre, String telefono, Instant creadoEn) {
    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getTelefono(), c.getCreadoEn());
    }
}
