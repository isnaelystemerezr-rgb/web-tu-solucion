import { ETIQUETA_ESTADO, formatearFecha } from '../api'

/**
 * Historial de eventos de la orden (solo lectura: el backend no permite editarlos).
 * El evento de apertura tiene de === a y se muestra como "Orden creada".
 */
export default function HistorialEventos({ eventos }) {
  if (!eventos || eventos.length === 0) {
    return <p className="estado-info">Sin eventos registrados.</p>
  }

  return (
    <ol className="timeline">
      {eventos.map((e) => (
        <li key={e.id} className="timeline-item">
          <span className="timeline-punto" />
          <div>
            {e.de === e.a ? (
              <strong>Orden creada</strong>
            ) : (
              <span>
                {ETIQUETA_ESTADO[e.de] || e.de} <span className="flecha">→</span>{' '}
                <strong>{ETIQUETA_ESTADO[e.a] || e.a}</strong>
              </span>
            )}
            <div className="mini">{formatearFecha(e.creadoEn)}</div>
          </div>
        </li>
      ))}
    </ol>
  )
}
