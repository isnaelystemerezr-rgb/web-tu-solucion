import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router'
import { getOrdenes } from '../api'
import FiltroEstado from '../components/FiltroEstado'
import TablaOrdenes from '../components/TablaOrdenes'
import Cargando from '../components/Cargando'
import ErrorBox from '../components/ErrorBox'

/**
 * Lista de órdenes con filtro por estado.
 *
 * - El filtro vive en la URL (?estado=...): refresh, atrás/adelante y links compartidos
 *   respetan el filtro. No hay estado local duplicado.
 * - Los datos SIEMPRE se piden al servidor (no hay caché local que pueda divergir).
 * - `recarga` es un contador que fuerza volver a pedir la lista (botón Reintentar)
 *   sin duplicar la lógica del fetch.
 */
export default function OrdenesLista() {
  const [busqueda, setBusqueda] = useSearchParams()
  const filtro = busqueda.get('estado') || ''

  const [ordenes, setOrdenes] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    // bandera de cancelación: si el filtro cambia antes de que responda el fetch,
    // la respuesta vieja se descarta (limpieza del efecto).
    let activo = true
    setCargando(true)
    setError(null)

    getOrdenes(filtro)
      .then((datos) => {
        if (activo) setOrdenes(datos)
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
  }, [filtro, recarga])

  function cambiarFiltro(nuevo) {
    const siguiente = new URLSearchParams(busqueda)
    if (nuevo) siguiente.set('estado', nuevo)
    else siguiente.delete('estado')
    setBusqueda(siguiente)
  }

  return (
    <div className="pagina">
      <div className="cabecera-pagina">
        <div>
          <h1>Órdenes</h1>
          <p className="mini">
            {ordenes.length} orden(es) {filtro ? 'en este estado' : 'en total'}
          </p>
        </div>
        <Link className="boton primario" to="/ordenes/nueva">
          + Nueva orden
        </Link>
      </div>

      <FiltroEstado valor={filtro} onCambiar={cambiarFiltro} />

      {error && <ErrorBox mensaje={error} onReintentar={() => setRecarga((n) => n + 1)} />}
      {cargando ? <Cargando /> : <TablaOrdenes ordenes={ordenes} />}
    </div>
  )
}
