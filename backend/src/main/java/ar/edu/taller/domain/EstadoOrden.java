package ar.edu.taller.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Estados de una orden y su grafo de transiciones permitidas.
 *
 * La tabla de transiciones vive EN EL DOMINIO (no en el servicio ni en la UI) para
 * que sea imposible "olvidarse" una validación: cualquier camino que cambie el estado
 * de una orden pasa por {@code OrdenService}, que consulta {@link #siguientes()}.
 *
 * Flujo principal:
 *   solicitada → en_diagnostico → presupuestada → aprobada → en_reparacion → lista → entregada
 * Salidas:
 *   presupuestada → rechazada
 *   solicitada   → cancelada
 *
 * rechazada, cancelada y entregada son finales (conjunto de salidas vacío).
 */
public enum EstadoOrden {

    SOLICITADA("solicitada"),
    EN_DIAGNOSTICO("en_diagnostico"),
    PRESUPUESTADA("presupuestada"),
    APROBADA("aprobada"),
    EN_REPARACION("en_reparacion"),
    LISTA("lista"),
    ENTREGADA("entregada"),
    RECHAZADA("rechazada"),
    CANCELADA("cancelada");

    private static final Map<EstadoOrden, Set<EstadoOrden>> TRANSICIONES = Map.of(
            SOLICITADA,     Set.of(EN_DIAGNOSTICO, CANCELADA),
            EN_DIAGNOSTICO, Set.of(PRESUPUESTADA),
            PRESUPUESTADA,  Set.of(APROBADA, RECHAZADA),
            APROBADA,       Set.of(EN_REPARACION),
            EN_REPARACION,  Set.of(LISTA),
            LISTA,          Set.of(ENTREGADA),
            // Estados finales: ninguna transición de salida.
            ENTREGADA,      Set.of(),
            RECHAZADA,      Set.of(),
            CANCELADA,      Set.of()
    );

    private final String valor;

    EstadoOrden(String valor) {
        this.valor = valor;
    }

    /** Valor expuesto en la API y en la base (select con los 9 valores exactos). */
    @JsonValue
    public String getValor() {
        return valor;
    }

    public Set<EstadoOrden> siguientes() {
        return Collections.unmodifiableSet(TRANSICIONES.get(this));
    }

    public boolean esFinal() {
        return siguientes().isEmpty();
    }

    /** ¿Es legal pasar de este estado a {@code destino}? Única fuente de verdad. */
    public boolean puedeTransicionarA(EstadoOrden destino) {
        return destino != null && siguientes().contains(destino);
    }

    /** Estados finales en los que el presupuesto (y los datos de la orden) ya no se tocan. */
    public boolean esTerminalOAvanzado() {
        return switch (this) {
            case PRESUPUESTADA, APROBADA, EN_REPARACION, LISTA, ENTREGADA, RECHAZADA, CANCELADA -> true;
            case SOLICITADA, EN_DIAGNOSTICO -> false;
        };
    }

    /** Acepta "en_diagnostico", "EN_DIAGNOSTICO" o "en-diagnostico" y devuelve el enum. */
    @JsonCreator
    public static EstadoOrden fromValor(String valor) {
        if (valor == null) {
            return null;
        }
        String normalizado = valor.trim().toLowerCase().replace('-', '_').replace(' ', '_');
        Optional<EstadoOrden> encontrado = Arrays.stream(values())
                .filter(e -> e.valor.equals(normalizado) || e.name().equalsIgnoreCase(normalizado))
                .findFirst();
        if (encontrado.isEmpty()) {
            throw new IllegalArgumentException(
                    "Estado inválido: '" + valor + "'. Valores permitidos: " + valoresPermitidos());
        }
        return encontrado.get();
    }

    public static String valoresPermitidos() {
        return Arrays.stream(values()).map(EstadoOrden::getValor).toList().toString();
    }
}
