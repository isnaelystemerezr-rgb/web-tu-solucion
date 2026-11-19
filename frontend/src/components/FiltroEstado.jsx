import { ESTADOS } from '../api'

/**
 * Filtro por estado: <select> controlado o botones.
 * Se sincroniza con el querystring (?estado=...) para que el filtro sobreviva
 * a un refresh y se pueda compartir el link.
 */
export default function FiltroEstado({ valor, onCambiar }) {
  return (
    <div className="filtro">
      <span className="filtro-label">Filtrar:</span>
      <button
        type="button"
        className={`chip ${valor === '' ? 'activo' : ''}`}
        onClick={() => onCambiar('')}
      >
        Todas
      </button>
      {ESTADOS.map((e) => (
        <button
          key={e.valor}
          type="button"
          className={`chip ${valor === e.valor ? 'activo' : ''}`}
          onClick={() => onCambiar(e.valor)}
        >
          {e.etiqueta}
        </button>
      ))}
    </div>
  )
}
