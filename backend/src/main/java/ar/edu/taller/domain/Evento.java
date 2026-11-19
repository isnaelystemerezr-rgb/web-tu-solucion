package ar.edu.taller.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Registro de auditoría de cambios de estado de una orden.
 *
 * Reglas duras (se aplican en la capa de servicio y en el mapeo HTTP):
 *  - Solo se crean (nunca se editan ni se borran: no hay setters ni endpoints de mutación).
 *  - La fecha la pone SIEMPRE el servidor (no se acepta del cliente).
 *  - Los crea exclusivamente OrdenService, dentro de la misma transacción que
 *    el cambio de estado de la orden.
 */
@Entity
@Table(name = "eventos")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private Orden orden;

    /** Estado anterior ("de"). */
    @Column(name = "de", nullable = false, length = 20)
    private String de;

    /** Estado nuevo ("a"). */
    @Column(name = "a", nullable = false, length = 20)
    private String a;

    /** Fecha de creación automática, tomada del reloj del servidor. */
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    protected Evento() {
    }

    private Evento(Orden orden, String de, String a, Instant creadoEn) {
        this.orden = orden;
        this.de = de;
        this.a = a;
        this.creadoEn = creadoEn;
    }

    /**
     * Única fábrica de eventos. Obliga a pasar el instante del servidor y deja el
     * registro sin setters: una vez creado, es inmutable.
     */
    public static Evento registrar(Orden orden, EstadoOrden de, EstadoOrden a, Instant ahora) {
        return new Evento(orden, de.getValor(), a.getValor(), ahora);
    }

    public Long getId() {
        return id;
    }

    public Orden getOrden() {
        return orden;
    }

    public String getDe() {
        return de;
    }

    public String getA() {
        return a;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
