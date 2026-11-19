package ar.edu.taller.web.error;

/**
 * Excepción de negocio: representa un 400/409 con mensaje legible para el operador
 * del taller. Se traduce a JSON en GlobalExceptionHandler.
 */
public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}
