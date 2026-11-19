import { useEffect, useRef, useState } from 'react'
import { calcularTotal, formatearMoneda } from '../api'

/**
 * Modal para registrar el presupuesto en la transición a "presupuestada".
 *
 * - Formulario controlado (estado por campo).
 * - useRef: guarda el input del diagnóstico para enfocarlo al abrir (y para
 *   restaurar el foco al cerrar mediante el cleanup del efecto).
 */
export default function ModalPresupuesto({ abierto, onCancelar, onConfirmar, ocupado }) {
  const [diagnostico, setDiagnostico] = useState('')
  const [manoObra, setManoObra] = useState('')
  const [piezas, setPiezas] = useState([])
  const [error, setError] = useState(null)
  const inputDiagnostico = useRef(null)

  // Al abrir: limpia el formulario y enfoca el primer campo. Al cerrar/desmontar
  // devuelve el foco al documento para no dejar el foco atrapado (limpieza).
  useEffect(() => {
    if (!abierto) return undefined
    setDiagnostico('')
    setManoObra('')
    setPiezas([])
    setError(null)
    const id = window.setTimeout(() => inputDiagnostico.current?.focus(), 50)
    return () => window.clearTimeout(id)
  }, [abierto])

  // Cerrar con Escape mientras el modal está abierto.
  useEffect(() => {
    if (!abierto) return undefined
    const alPresionar = (ev) => {
      if (ev.key === 'Escape') onCancelar()
    }
    window.addEventListener('keydown', alPresionar)
    return () => window.removeEventListener('keydown', alPresionar)
  }, [abierto, onCancelar])

  if (!abierto) return null

  const total = calcularTotal(manoObra, piezas)

  function agregarPieza() {
    setPiezas((prev) => [...prev, { descripcion: '', cantidad: 1, precio: 0 }])
  }

  function actualizarPieza(indice, campo, valor) {
    setPiezas((prev) =>
      prev.map((p, i) => (i === indice ? { ...p, [campo]: valor } : p)),
    )
  }

  function quitarPieza(indice) {
    setPiezas((prev) => prev.filter((_, i) => i !== indice))
  }

  function confirmar() {
    if (!diagnostico.trim()) {
      setError('El diagnóstico es obligatorio.')
      return
    }
    if (manoObra === '' || Number(manoObra) < 0) {
      setError('Ingresá la mano de obra (puede ser 0).')
      return
    }
    const limpias = piezas
      .filter((p) => p.descripcion.trim())
      .map((p) => ({
        descripcion: p.descripcion.trim(),
        cantidad: Number(p.cantidad) || 1,
        precio: Number(p.precio) || 0,
      }))
    onConfirmar({
      diagnostico: diagnostico.trim(),
      manoObra: Number(manoObra),
      piezas: limpias,
    })
  }

  return (
    <div className="modal-fondo" role="dialog" aria-modal="true" aria-label="Presupuesto">
      <div className="modal">
        <h2>Registrar presupuesto</h2>
        <p className="mini">
          El presupuesto se guarda <strong>una sola vez</strong> y queda bloqueado.
        </p>

        <label>
          Diagnóstico
          <textarea
            ref={inputDiagnostico}
            rows={3}
            value={diagnostico}
            onChange={(e) => setDiagnostico(e.target.value)}
            placeholder="Qué se detectó y qué se va a hacer"
          />
        </label>

        <label>
          Mano de obra
          <input
            type="number"
            min="0"
            step="0.01"
            value={manoObra}
            onChange={(e) => setManoObra(e.target.value)}
            placeholder="0.00"
          />
        </label>

        <div className="piezas-cabecera">
          <strong>Piezas</strong>
          <button type="button" className="boton chico" onClick={agregarPieza}>
            + Agregar pieza
          </button>
        </div>

        {piezas.length === 0 && <p className="mini">Sin piezas (solo mano de obra).</p>}

        {piezas.map((p, i) => (
          <div className="pieza-fila" key={i}>
            <input
              placeholder="Descripción"
              value={p.descripcion}
              onChange={(e) => actualizarPieza(i, 'descripcion', e.target.value)}
            />
            <input
              type="number"
              min="1"
              value={p.cantidad}
              onChange={(e) => actualizarPieza(i, 'cantidad', e.target.value)}
              aria-label="Cantidad"
            />
            <input
              type="number"
              min="0"
              step="0.01"
              value={p.precio}
              onChange={(e) => actualizarPieza(i, 'precio', e.target.value)}
              aria-label="Precio"
            />
            <button type="button" className="boton chico peligro" onClick={() => quitarPieza(i)}>
              ✕
            </button>
          </div>
        ))}

        <p className="total-preview">
          Total estimado: <strong>{formatearMoneda(total)}</strong>
        </p>

        {error && <div className="error-box">{error}</div>}

        <div className="modal-acciones">
          <button type="button" className="boton" onClick={onCancelar} disabled={ocupado}>
            Cancelar
          </button>
          <button type="button" className="boton primario" onClick={confirmar} disabled={ocupado}>
            {ocupado ? 'Guardando…' : 'Registrar y presupuestar'}
          </button>
        </div>
      </div>
    </div>
  )
}
