package ar.edu.taller.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orden de reparación: núcleo del sistema.
 *
 * Campos tal como los define el enunciado (nombres exactos):
 *   cliente (relación requerida a clientes), equipo, problema,
 *   estado (select de 9 valores), diagnostico, manoObra, piezas (JSON), fotos (máx. 3).
 */
@Entity
@Table(name = "ordenes")
public class Orden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Relación requerida a clientes. Muchos pedidos pueden ser del mismo cliente. */
    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(name = "equipo", nullable = false, length = 120)
    private String equipo;

    @Column(name = "problema", nullable = false, length = 1000)
    private String problema;

    // Guarda el valor en minúsculas ('en_diagnostico'), igual que la API y el seed.
    @Convert(converter = EstadoOrdenConverter.class)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoOrden estado = EstadoOrden.SOLICITADA;

    @Column(name = "diagnostico", length = 1000)
    private String diagnostico;

    @Column(name = "mano_obra", precision = 12, scale = 2)
    private BigDecimal manoObra;

    /**
     * Piezas del presupuesto, guardadas como JSON en una sola columna.
     * Ver {@link PiezasJsonConverter} para la justificación de esta decisión.
     */
    @Convert(converter = PiezasJsonConverter.class)
    @Column(name = "piezas", columnDefinition = "text")
    private List<Pieza> piezas = new ArrayList<>();

    /** Rutas relativas (/uploads/uuid.jpg) de hasta 3 fotos guardadas en disco. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "orden_fotos", joinColumns = @JoinColumn(name = "orden_id"))
    @OrderColumn(name = "posicion")
    @Column(name = "ruta", nullable = false, length = 300)
    private List<String> fotos = new ArrayList<>();

    /**
     * Marca de que el presupuesto ya fue registrado (transición a "presupuestada").
     * Es el candado de inmutabilidad a nivel de datos, además de la comprobación por estado.
     */
    @Column(name = "presupuesto_registrado", nullable = false)
    private boolean presupuestoRegistrado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn = Instant.now();

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn = Instant.now();

    protected Orden() {
    }

    public Orden(Cliente cliente, String equipo, String problema) {
        this.cliente = cliente;
        this.equipo = equipo;
        this.problema = problema;
        this.estado = EstadoOrden.SOLICITADA;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public String getEquipo() {
        return equipo;
    }

    public void setEquipo(String equipo) {
        this.equipo = equipo;
    }

    public String getProblema() {
        return problema;
    }

    public void setProblema(String problema) {
        this.problema = problema;
    }

    public EstadoOrden getEstado() {
        return estado;
    }

    /**
     * Setter de bajo nivel: NO valida la transición.
     * Usarlo solo desde {@code OrdenService#cambiarEstado} (que sí valida y audita)
     * o desde el seed/migraciones.
     */
    public void setEstado(EstadoOrden estado) {
        this.estado = estado;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public BigDecimal getManoObra() {
        return manoObra;
    }

    public void setManoObra(BigDecimal manoObra) {
        this.manoObra = manoObra;
    }

    public List<Pieza> getPiezas() {
        return piezas;
    }

    public void setPiezas(List<Pieza> piezas) {
        this.piezas = piezas == null ? new ArrayList<>() : new ArrayList<>(piezas);
    }

    public List<String> getFotos() {
        return fotos;
    }

    public void setFotos(List<String> fotos) {
        this.fotos = fotos == null ? new ArrayList<>() : new ArrayList<>(fotos);
    }

    public boolean isPresupuestoRegistrado() {
        return presupuestoRegistrado;
    }

    public void setPresupuestoRegistrado(boolean presupuestoRegistrado) {
        this.presupuestoRegistrado = presupuestoRegistrado;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }

    public void setActualizadoEn(Instant actualizadoEn) {
        this.actualizadoEn = actualizadoEn;
    }

    /** El total NO se persiste: se calcula siempre. Ver {@code OrdenService#total}. */
    public BigDecimal calcularTotal() {
        BigDecimal mano = manoObra == null ? BigDecimal.ZERO : manoObra;
        return piezas.stream()
                .map(Pieza::subtotal)
                .reduce(mano, BigDecimal::add);
    }
}
