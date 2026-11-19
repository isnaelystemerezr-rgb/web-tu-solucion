import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router'
import { ESTADOS, ETIQUETA_ESTADO, getResumen } from '../api'
import Cargando from '../components/Cargando'
import ErrorBox from '../components/ErrorBox'

/**
 * Resumen de órdenes agrupadas por etapa (GET /api/ordenes/resumen).
 *
 * useMemo: `ordenadas`, `total` y `maximo` son valores DERIVADOS de `filas`.
 * Se justifica porque se recorre y ordena el arreglo y se calcula el máximo para
 * escalar las barras: sin memo se repetiría en cada render sin que cambien los datos.
 */
export default function Resumen() {
  const [filas, setFilas] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  useEffect(() => {
    let activo = true
    setCargando(true)
    setError(null)

    getResumen()
      .then((datos) => {
        if (activo) setFilas(datos)
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

  const { ordenadas, total, maximo } = useMemo(() => {
    const porEstado = new Map(filas.map((f) => [f.estado, f]))
    // Se muestra siempre el flujo completo, incluso las etapas con 0 órdenes.
    const lista = ESTADOS.map((e) => ({
      estado: e.valor,
      etiqueta: ETIQUETA_ESTADO[e.valor],
      cantidad: porEstado.get(e.valor)?.cantidad ?? 0,
      final: porEstado.get(e.valor)?.final ?? false,
    }))
    return {
      ordenadas: lista,
      total: lista.reduce((acc, f) => acc + f.cantidad, 0),
      maximo: Math.max(1, ...lista.map((f) => f.cantidad)),
    }
  }, [filas])

  return (
    <div className="pagina">
      <div className="cabecera-pagina">
        <div>
          <h1>Resumen por etapa</h1>
          <p className="mini">{total} orden(es) registradas</p>
        </div>
      </div>

      {error && <ErrorBox mensaje={error} onReintentar={() => setRecarga((n) => n + 1)} />}
      {cargando ? (
        <Cargando />
      ) : (
        <div className="tablero">
          {ordenadas.map((f) => (
            <Link
              key={f.estado}
              to={`/ordenes?estado=${f.estado}`}
              className={`tarjeta ${f.final ? 'final' : ''}`}
            >
              <span className="tarjeta-titulo">{f.etiqueta}</span>
              <span className="tarjeta-numero">{f.cantidad}</span>
              <span className="barra">
                <span style={{ width: `${(f.cantidad / maximo) * 100}%` }} />
              </span>
              {f.final && <span className="mini">estado final</span>}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
