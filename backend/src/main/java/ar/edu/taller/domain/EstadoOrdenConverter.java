package ar.edu.taller.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Persiste el estado como el valor del "select" del enunciado (minúsculas):
 * solicitada, en_diagnostico, ...
 *
 * Se usa en lugar de @Enumerated(STRING) para que la columna guarde EXACTAMENTE
 * los 9 valores definidos, y coincida con el seed data.sql, con la API y con el
 * frontend (una sola representación en todo el sistema).
 */
@Converter
public class EstadoOrdenConverter implements AttributeConverter<EstadoOrden, String> {

    @Override
    public String convertToDatabaseColumn(EstadoOrden estado) {
        return estado == null ? null : estado.getValor();
    }

    @Override
    public EstadoOrden convertToEntityAttribute(String valor) {
        return valor == null ? null : EstadoOrden.fromValor(valor);
    }
}
