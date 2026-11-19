import { useState } from 'react'
import { ETIQUETA_ESTADO, transicionar } from '../api'
import ModalPresupuesto from './ModalPresupuesto'

/**
 * Botones de cambio de estado.
 *
 * Las opciones vienen del SERVIDOR en orden.transicionesPermitidas: la UI no
 * inventa transiciones, solo dibuja lo que el backend autoriza. El servidor
 * revalida siempre, así que la UI es una comodidad, no la garantía.
 *
 * "presupuestada" es especial: abre el modal para cargar el presupuesto, que se
 * registra una única vez en esa misma transición.
 */
export default function BotonesTransicion({ orden, onCambio }) {
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState(null)
  const [modalAbierto, setModalAbierto] = useState(false)

  const permitidas = orden.transicionesPermitidas || []

  async function mover(destino, extras = {}) {
    setError(null)
    setOcupado(true)
    try {
      const actualizada = await transicionar(orden.id, destino, extras)
      setModalAbierto(false)
      onCambio(actualizada)
    } catch (e) {
      setError(e.message)
    } finally {
      setOcupado(false)
    }
  }

  if (permitidas.length === 0) {
    return (
      <section className="panel">
        <h2>Acciones</h2>
        <p className="estado-info">
          El estado <strong>{ETIQUETA_ESTADO[orden.estado] || orden.estado}</strong> es final:
          la orden no admite más cambios.
        </p>
      </section>
    )
  }

  return (
    <section className="panel">
      <h2>Acciones</h2>
      <div className="acciones">
        {permitidas.map((destino) => (
          <button
            key={destino}
            type="button"
            className={`boton ${
              destino === 'cancelada' || destino === 'rechazada' ? 'peligro' : 'primario'
            }`}
            disabled={ocupado}
            onClick={() => (destino === 'presupuestada' ? setModalAbierto(true) : mover(destino))}
          >
            {`Pasar a ${ETIQUETA_ESTADO[destino] || destino}`}
          </button>
        ))}
      </div>
      {ocupado && <p className="mini">Guardando cambio de estado…</p>}
      {error && <div className="error-box">{error}</div>}

      <ModalPresupuesto
        abierto={modalAbierto}
        ocupado={ocupado}
        onCancelar={() => setModalAbierto(false)}
        onConfirmar={(presupuesto) => mover('presupuestada', presupuesto)}
      />
    </section>
  )
}
