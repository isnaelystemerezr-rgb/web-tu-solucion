package ar.edu.taller.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una pieza/línea del presupuesto: {descripcion, cantidad, precio}.
 *
 * No es una entidad JPA: se guarda serializada como JSON dentro de la columna
 * {@code piezas} de la tabla {@code ordenes} (ver {@link PiezasJsonConverter}).
 * Se eligió JSON en lugar de una tabla hija porque el enunciado define el campo
 * como JSON, se guarda y lee siempre junto a la orden y nunca se consulta por pieza.
 */
public class Pieza {

    @NotBlank
    private String descripcion;

    @Positive
    private Integer cantidad;

    @PositiveOrZero
    private BigDecimal precio;

    public Pieza() {
    }

    public Pieza(String descripcion, Integer cantidad, BigDecimal precio) {
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    /** cantidad × precio, protegido contra nulos. */
    public BigDecimal subtotal() {
        if (cantidad == null || precio == null) {
            return BigDecimal.ZERO;
        }
        return precio.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Pieza pieza)) {
            return false;
        }
        return Objects.equals(descripcion, pieza.descripcion)
                && Objects.equals(cantidad, pieza.cantidad)
                && Objects.equals(precio, pieza.precio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descripcion, cantidad, precio);
    }
}
