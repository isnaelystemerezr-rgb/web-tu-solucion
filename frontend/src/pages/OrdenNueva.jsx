import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { crearCliente, crearOrden, getClientes } from '../api'
import Cargando from '../components/Cargando'
import ErrorBox from '../components/ErrorBox'

/**
 * Formulario de nueva orden: cliente, equipo, problema y hasta 3 fotos.
 *
 * - Formulario controlado (un estado por campo).
 * - Los clientes se cargan del servidor con useEffect.
 * - useRef sobre el input de archivos para limpiar la selección y poder elegir
 *   el mismo archivo otra vez.
 * - Permite dar de alta un cliente nuevo sin salir de la pantalla.
 */
export default function OrdenNueva() {
  const navegar = useNavigate()

  const [clientes, setClientes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState(null)

  const [clienteId, setClienteId] = useState('')
  const [equipo, setEquipo] = useState('')
  const [problema, setProblema] = useState('')
  const [fotos, setFotos] = useState([])

  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState(null)

  // Alta rápida de cliente
  const [mostrarNuevoCliente, setMostrarNuevoCliente] = useState(false)
  const [nuevoNombre, setNuevoNombre] = useState('')
  const [nuevoTelefono, setNuevoTelefono] = useState('')

  const inputFotos = useRef(null)

  useEffect(() => {
    let activo = true
    ;(async () => {
      setCargando(true)
      try {
        const datos = await getClientes()
        if (activo) {
          setClientes(datos)
          if (datos.length > 0) setClienteId(String(datos[0].id))
        }
      } catch (e) {
        if (activo) setErrorCarga(e.message)
      } finally {
        if (activo) setCargando(false)
      }
    })()
    return () => {
      activo = false
    }
  }, [])

  function elegirFotos(ev) {
    const elegidos = Array.from(ev.target.files || [])
    if (elegidos.length > 3) {
      setError('Máximo 3 fotos por orden.')
      return
    }
    setError(null)
    setFotos(elegidos)
  }

  function quitarFoto(indice) {
    setFotos((prev) => prev.filter((_, i) => i !== indice))
    if (inputFotos.current) inputFotos.current.value = ''
  }

  async function guardarClienteNuevo() {
    setError(null)
    if (!nuevoNombre.trim() || !nuevoTelefono.trim()) {
      setError('Nombre y teléfono son obligatorios para crear el cliente.')
      return
    }
    try {
      const creado = await crearCliente({
        nombre: nuevoNombre.trim(),
        telefono: nuevoTelefono.trim(),
      })
      setClientes((prev) => [...prev, creado])
      setClienteId(String(creado.id))
      setMostrarNuevoCliente(false)
      setNuevoNombre('')
      setNuevoTelefono('')
    } catch (e) {
      setError(e.message)
    }
  }

  async function enviar(ev) {
    ev.preventDefault()
    setError(null)

    if (!clienteId) {
      setError('Seleccioná un cliente.')
      return
    }
    if (!equipo.trim() || !problema.trim()) {
      setError('Equipo y problema son obligatorios.')
      return
    }

    setEnviando(true)
    try {
      const orden = await crearOrden({
        clienteId: Number(clienteId),
        equipo: equipo.trim(),
        problema: problema.trim(),
        fotos,
      })
      navegar(`/ordenes/${orden.id}`)
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  if (cargando) return <Cargando texto="Cargando clientes…" />

  return (
    <div className="pagina angosta">
      <div className="migas">
        <button type="button" className="link" onClick={() => navegar('/ordenes')}>
          ← Órdenes
        </button>
      </div>

      <h1>Nueva orden</h1>
      <p className="mini">La orden se crea en estado «Solicitada».</p>

      {errorCarga && <ErrorBox mensaje={errorCarga} />}

      <form className="panel form-vertical" onSubmit={enviar}>
        <label>
          Cliente
          <select value={clienteId} onChange={(e) => setClienteId(e.target.value)}>
            {clientes.length === 0 && <option value="">No hay clientes cargados</option>}
            {clientes.map((c) => (
              <option key={c.id} value={c.id}>
                {c.nombre} — {c.telefono}
              </option>
            ))}
          </select>
        </label>

        {!mostrarNuevoCliente ? (
          <button
            type="button"
            className="boton chico"
            onClick={() => setMostrarNuevoCliente(true)}
          >
            + Cliente nuevo
          </button>
        ) : (
          <div className="subpanel">
            <strong>Cliente nuevo</strong>
            <label>
              Nombre
              <input value={nuevoNombre} onChange={(e) => setNuevoNombre(e.target.value)} />
            </label>
            <label>
              Teléfono
              <input
                value={nuevoTelefono}
                onChange={(e) => setNuevoTelefono(e.target.value)}
                placeholder="+34 600 000 000"
              />
            </label>
            <div className="acciones">
              <button type="button" className="boton primario chico" onClick={guardarClienteNuevo}>
                Guardar cliente
              </button>
              <button
                type="button"
                className="boton chico"
                onClick={() => setMostrarNuevoCliente(false)}
              >
                Cancelar
              </button>
            </div>
          </div>
        )}

        <label>
          Equipo
          <input
            value={equipo}
            onChange={(e) => setEquipo(e.target.value)}
            placeholder="iPhone 12, Notebook HP, TV Samsung 50…"
            maxLength={120}
          />
        </label>

        <label>
          Problema informado
          <textarea
            rows={4}
            value={problema}
            onChange={(e) => setProblema(e.target.value)}
            placeholder="Qué falla según el cliente"
            maxLength={1000}
          />
        </label>

        <label>
          Fotos (opcional, hasta 3)
          <input
            ref={inputFotos}
            type="file"
            accept="image/png,image/jpeg,image/webp,image/gif"
            multiple
            onChange={elegirFotos}
          />
        </label>

        {fotos.length > 0 && (
          <ul className="lista-fotos">
            {fotos.map((f, i) => (
              <li key={`${f.name}-${i}`}>
                {f.name} <span className="mini">({Math.round(f.size / 1024)} KB)</span>
                <button type="button" className="boton chico peligro" onClick={() => quitarFoto(i)}>
                  Quitar
                </button>
              </li>
            ))}
          </ul>
        )}

        {error && <div className="error-box">{error}</div>}

        <button type="submit" className="boton primario" disabled={enviando}>
          {enviando ? 'Creando…' : 'Crear orden'}
        </button>
      </form>
    </div>
  )
}
