package ar.edu.taller.web.dto;

import java.time.Instant;

public record EventoResponse(Long id, Long ordenId, String de, String a, Instant creadoEn) {
}
