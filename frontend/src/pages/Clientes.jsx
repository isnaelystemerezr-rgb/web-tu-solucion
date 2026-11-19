import { useEffect, useState } from 'react'
import { crearCliente, formatearFecha, getClientes } from '../api'
import Cargando from '../components/Cargando'
import ErrorBox from '../components/ErrorBox'

/** Clientes del taller: listado y alta. No son usuarios: no tienen login. */
export default function Clientes() {
  const [clientes, setClientes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  const [nombre, setNombre] = useState('')
  const [telefono, setTelefono] = useState('')
  const [enviando, setEnviando] = useState(false)

  useEffect(() => {
    let activo = true
    setCargando(true)
    setError(null)

    getClientes()
      .then((datos) => {
        if (activo) setClientes(datos)
      })
      .catch((e) => {
        if (activo) setError(e.message)
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    return () => {
      activo = false
    }
  }, [recarga])

  async function alta(ev) {
    ev.preventDefault()
    setError(null)
    setEnviando(true)
    try {
      const creado = await crearCliente({ nombre, telefono })
      // Se agrega el cliente devuelto por el servidor (sin inventar datos locales).
      setClientes((prev) => [...prev, creado].sort((a, b) => a.nombre.localeCompare(b.nombre)))
      setNombre('')
      setTelefono('')
    } catch (e) {
      setError(e.message)
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="pagina angosta">
      <h1>Clientes</h1>

      <form className="panel form-inline" onSubmit={alta}>
        <label>
          Nombre
          <input
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            placeholder="Nombre y apellido"
            maxLength={80}
          />
        </label>
        <label>
          Teléfono
          <input
            value={telefono}
            onChange={(e) => setTelefono(e.target.value)}
            placeholder="+34 600 000 000"
            maxLength={30}
          />
        </label>
        <button type="submit" className="boton primario" disabled={enviando}>
          {enviando ? 'Guardando…' : 'Agregar'}
        </button>
      </form>

      {error && <ErrorBox mensaje={error} onReintentar={() => setRecarga((n) => n + 1)} />}
      {cargando ? (
        <Cargando />
      ) : (
        <div className="tabla-wrap">
          <table className="tabla">
            <thead>
              <tr>
                <th>#</th>
                <th>Nombre</th>
                <th>Teléfono</th>
                <th>Alta</th>
              </tr>
            </thead>
            <tbody>
              {clientes.map((c) => (
                <tr key={c.id}>
                  <td className="mono">{c.id}</td>
                  <td>{c.nombre}</td>
                  <td>{c.telefono}</td>
                  <td className="mini">{formatearFecha(c.creadoEn)}</td>
                </tr>
              ))}
              {clientes.length === 0 && (
                <tr>
                  <td colSpan={4} className="mini">
                    No hay clientes.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
