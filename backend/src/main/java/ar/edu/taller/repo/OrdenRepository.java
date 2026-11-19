package ar.edu.taller.repo;

import ar.edu.taller.domain.EstadoOrden;
import ar.edu.taller.domain.Orden;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    List<Orden> findAllByOrderByCreadoEnDesc();

    List<Orden> findByEstadoOrderByCreadoEnDesc(EstadoOrden estado);

    long countByEstado(EstadoOrden estado);

    boolean existsByClienteId(Long clienteId);

    /** Resumen por etapa: [estado, cantidad] para alimentar /ordenes/resumen. */
    @Query("select o.estado, count(o) from Orden o group by o.estado")
    List<Object[]> contarPorEstado();
}
