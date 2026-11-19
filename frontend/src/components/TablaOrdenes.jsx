import { Link } from 'react-router'
import EstadoBadge from './EstadoBadge'
import { formatearFecha, formatearMoneda } from '../api'

/**
 * Tabla de órdenes. Los datos llegan por props (no hace fetch).
 * Cada fila tiene key = id (requisito del enunciado).
 */
export default function TablaOrdenes({ ordenes }) {
  if (ordenes.length === 0) {
    return <p className="estado-info">No hay órdenes para este filtro.</p>
  }

  return (
    <div className="tabla-wrap">
      <table className="tabla">
        <thead>
          <tr>
            <th>#</th>
            <th>Equipo</th>
            <th>Cliente</th>
            <th>Estado</th>
            <th className="num">Total</th>
            <th>Creada</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {ordenes.map((o) => (
            <tr key={o.id}>
              <td className="mono">{o.id}</td>
              <td>
                <Link to={`/ordenes/${o.id}`}>{o.equipo}</Link>
              </td>
              <td>
                {o.cliente?.nombre}
                <div className="mini">{o.cliente?.telefono}</div>
              </td>
              <td>
                <EstadoBadge estado={o.estado} />
              </td>
              <td className="num">
                {o.presupuesto?.registrado
                  ? formatearMoneda(o.presupuesto.total)
                  : <span className="mini">sin presupuesto</span>}
              </td>
              <td className="mini">{formatearFecha(o.creadoEn)}</td>
              <td>
                <Link className="boton chico" to={`/ordenes/${o.id}`}>
                  Ver
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}
