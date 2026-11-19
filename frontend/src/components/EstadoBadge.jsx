import { ETIQUETA_ESTADO } from '../api'

/** Pastilla con el nombre legible del estado. Componente pequeño y sin estado. */
export default function EstadoBadge({ estado }) {
  return (
    <span className={`badge estado-${estado}`} title={estado}>
      {ETIQUETA_ESTADO[estado] || estado}
    </span>
  )
}
