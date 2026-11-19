import { calcularTotal, formatearMoneda } from '../api'

/**
 * Detalle del presupuesto con el total calculado.
 *
 * El total del servidor llega en orden.presupuesto.total (lo calcula el backend).
 * Aquí se recalcula en el cliente con calcularTotal() para mostrar ambos y avisar
 * si no coinciden: es la comprobación pedida en el enunciado.
 */
export default function PanelPresupuesto({ orden }) {
  const piezas = orden.piezas || []
  const totalCliente = calcularTotal(orden.manoObra, piezas)
  const totalServidor = Number(orden.presupuesto?.total ?? 0)
  const coinciden = Math.abs(totalCliente - totalServidor) < 0.01

  if (!orden.presupuesto?.registrado) {
    return (
      <section className="panel">
        <h2>Presupuesto</h2>
        <p className="estado-info">
          Todavía no hay presupuesto: se registra (una única vez) al pasar la orden a{' '}
          <strong>Presupuestada</strong>.
        </p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>Presupuesto</h2>
      <p>{orden.diagnostico}</p>

      <table className="tabla compacta">
        <thead>
          <tr>
            <th>Pieza</th>
            <th className="num">Cant.</th>
            <th className="num">Precio</th>
            <th className="num">Subtotal</th>
          </tr>
        </thead>
        <tbody>
          {piezas.length === 0 && (
            <tr>
              <td colSpan={4} className="mini">
                Sin piezas (solo mano de obra)
              </td>
            </tr>
          )}
          {piezas.map((p, i) => (
            <tr key={`${p.descripcion}-${i}`}>
              <td>{p.descripcion}</td>
              <td className="num">{p.cantidad}</td>
              <td className="num">{formatearMoneda(p.precio)}</td>
              <td className="num">
                {formatearMoneda((p.cantidad || 0) * (p.precio || 0))}
              </td>
            </tr>
          ))}
        </tbody>
        <tfoot>
          <tr>
            <td colSpan={3}>Mano de obra</td>
            <td className="num">{formatearMoneda(orden.manoObra)}</td>
          </tr>
          <tr className="total">
            <td colSpan={3}>Total</td>
            <td className="num">{formatearMoneda(totalServidor)}</td>
          </tr>
        </tfoot>
      </table>

      <p className={`mini ${coinciden ? 'ok' : 'alerta'}`}>
        {coinciden
          ? `✔ Total calculado en el cliente (${formatearMoneda(totalCliente)}) coincide con el del servidor.`
          : `⚠ Discrepancia: cliente ${formatearMoneda(totalCliente)} vs servidor ${formatearMoneda(totalServidor)}.`}
      </p>
    </section>
  )
}
