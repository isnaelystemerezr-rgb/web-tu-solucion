package ar.edu.taller.service;

import ar.edu.taller.domain.Cliente;
import ar.edu.taller.repo.ClienteRepository;
import ar.edu.taller.repo.OrdenRepository;
import ar.edu.taller.web.dto.ClienteRequest;
import ar.edu.taller.web.error.RecursoNoEncontradoException;
import ar.edu.taller.web.error.ReglaNegocioException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** CRUD de clientes. No hay borrado si el cliente tiene órdenes (integridad del histórico). */
@Service
public class ClienteService {

    private final ClienteRepository clientes;
    private final OrdenRepository ordenes;

    public ClienteService(ClienteRepository clientes, OrdenRepository ordenes) {
        this.clientes = clientes;
        this.ordenes = ordenes;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return clientes.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return clientes.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente " + id));
    }

    @Transactional
    public Cliente crear(ClienteRequest req) {
        Cliente cliente = new Cliente(req.nombre().trim(), req.telefono().trim());
        return clientes.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteRequest req) {
        Cliente cliente = obtener(id);
        cliente.setNombre(req.nombre().trim());
        cliente.setTelefono(req.telefono().trim());
        return clientes.save(cliente);
    }

    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = obtener(id);
        if (ordenes.existsByClienteId(id)) {
            throw new ReglaNegocioException("CLIENTE_CON_ORDENES",
                    "No se puede borrar el cliente '" + cliente.getNombre() + "' porque tiene órdenes registradas");
        }
        clientes.delete(cliente);
    }
}
