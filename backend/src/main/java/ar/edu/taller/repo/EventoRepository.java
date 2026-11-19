package ar.edu.taller.repo;

import ar.edu.taller.domain.Evento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Los eventos solo se leen y se crean. No hay métodos de borrado ni de actualización
 * expuestos por la API (ver EventoController).
 */
public interface EventoRepository extends JpaRepository<Evento, Long> {

    List<Evento> findByOrdenIdOrderByCreadoEnAscIdAsc(Long ordenId);
}
