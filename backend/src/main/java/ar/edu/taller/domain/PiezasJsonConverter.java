package ar.edu.taller.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte List<Pieza> ↔ JSON para guardarlo en la columna TEXT {@code piezas}.
 *
 * La columna guarda exactamente el JSON que el frontend envía/consume, así que
 * "lo que se ve" en la API es lo que está persistido (no hay normalización oculta).
 */
@Converter
public class PiezasJsonConverter implements AttributeConverter<List<Pieza>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<Pieza>> TIPO = new TypeReference<>() {
    };

    @Override
    public String convertToDatabaseColumn(List<Pieza> piezas) {
        try {
            return MAPPER.writeValueAsString(piezas == null ? List.of() : piezas);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar las piezas del presupuesto", e);
        }
    }

    @Override
    public List<Pieza> convertToEntityAttribute(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(MAPPER.readValue(json, TIPO));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron leer las piezas del presupuesto", e);
        }
    }
}
