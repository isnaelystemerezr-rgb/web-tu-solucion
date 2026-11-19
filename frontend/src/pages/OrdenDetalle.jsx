import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router'
import {
  actualizarOrden,
  formatearFecha,
  getEventos,
  getOrden,
  subirFotos,
} from '../api'
import EstadoBadge from '../components/EstadoBadge'
import HistorialEventos from '../components/HistorialEventos'
import PanelPresupuesto from '../components/PanelPresupuesto'
import GaleriaFotos from '../components/GaleriaFotos'
import BotonesTransicion from '../components/BotonesTransicion'
import Cargando from '../components/Cargando'
import ErrorBox from '../components/ErrorBox'

const MAX_FOTOS = 3

/**
 * Detalle de una orden: ruta /ordenes/:id (parámetro de ruta).
 *
 * Todo lo que se ve viene del servidor; después de cada cambio se usa la orden
 * que devuelve el backend (o se vuelve a pedir) para no mostrar estado local
 * desactualizado.
 */
export default function OrdenDetalle() {
  const { id } = useParams()

  const [orden, setOrden] = useState(null)
  const [eventos, setEventos] = useState([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState(null)
  const [recarga, setRecarga] = useState(0)

  // Edición de los campos permitidos (equipo / problema)
  const [editando, setEditando] = useState(false)
  const [equipo, setEquipo] = useState('')
  const [problema, setProblema] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorEdicion, setErrorEdicion] = useState(null)

  // useRef: referencia al <input type="file"> para limpiar su valor y permitir
  // volver a elegir el mismo archivo (se usa en alElegirFotos y quitarFoto).
  const inputFotos = useRef(null)
  const [subiendo, setSubiendo] = useState(false)

  // Un único efecto de carga: se dispara al cambiar de :id o al pedir "Reintentar".
  useEffect(() => {
    let activo = true
    setCargando(true)
    setError(null)

    Promise.all([getOrden(id), getEventos(id)])
      .then(([o, ev]) => {
        if (!activo) return
        setOrden(o)
        setEventos(ev)
        setEquipo(o.equipo)
        setProblema(o.problema)
      })
      .catch((e) => {
        if (activo) setError(e.message)
      })
      .finally(() => {
        if (activo) setCargando(false)
      })

    // Limpieza: al desmontar o cambiar de orden se ignoran respuestas en vuelo.
    return () => {
      activo = false
    }
  }, [id, recarga])

  /** Tras una transición: usar la orden devuelta y refrescar solo el historial. */
  async function alCambiarEstado(ordenActualizada) {
    setOrden(ordenActualizada)
    setEquipo(ordenActualizada.equipo)
    setProblema(ordenActualizada.problema)
    try {
      setEventos(await getEventos(ordenActualizada.id))
    } catch (e) {
      setError(e.message)
    }
  }

  async function guardarDatos() {
    setGuardando(true)
    setErrorEdicion(null)
    try {
      const actualizada = await actualizarOrden(id, { equipo, problema })
      setOrden(actualizada)
      setEditando(false)
    } catch (e) {
      setErrorEdicion(e.message)
    } finally {
      setGuardando(false)
    }
  }

  async function alElegirFotos(ev) {
    const archivos = Array.from(ev.target.files || [])
    if (archivos.length === 0) return
    setSubiendo(true)
    setError(null)
    try {
      setOrden(await subirFotos(id, archivos))
    } catch (e) {
      setError(e.message)
    } finally {
      setSubiendo(false)
      if (inputFotos.current) inputFotos.current.value = ''
    }
  }

  /**
   * useMemo: valor derivado de orden.fotos. Evita recalcular en cada tecla del
   * formulario de edición y deja explícito que no es estado propio.
   */
  const fotosRestantes = useMemo(
    () => MAX_FOTOS - (orden?.fotos?.length || 0),
    [orden?.fotos],
  )

  if (cargando) return <Cargando texto="Cargando orden…" />
  if (error && !orden) {
    return <ErrorBox mensaje={error} onReintentar={() => setRecarga((n) => n + 1)} />
  }
  if (!orden) return null

  return (
    <div className="pagina">
      <div className="migas">
        <Link to="/ordenes">← Órdenes</Link>
      </div>

      {error && <ErrorBox mensaje={error} onReintentar={() => setRecarga((n) => n + 1)} />}

      <div className="cabecera-pagina">
        <div>
          <h1>
            Orden #{orden.id} · {orden.equipo}
          </h1>
          <p className="mini">
            {orden.cliente?.nombre} · {orden.cliente?.telefono} · creada{' '}
            {formatearFecha(orden.creadoEn)}
          </p>
        </div>
        <EstadoBadge estado={orden.estado} />
      </div>

      <div className="grid-dos">
        <div>
          <section className="panel">
            <div className="panel-cabecera">
              <h2>Datos del equipo</h2>
              {!editando && (
                <button type="button" className="boton chico" onClick={() => setEditando(true)}>
                  Editar datos
                </button>
              )}
            </div>

            {editando ? (
              <div className="form-vertical">
                <label>
                  Equipo
                  <input
                    value={equipo}
                    onChange={(e) => setEquipo(e.target.value)}
                    maxLength={120}
                  />
                </label>
                <label>
                  Problema informado
                  <textarea
                    rows={3}
                    value={problema}
                    onChange={(e) => setProblema(e.target.value)}
                    maxLength={1000}
                  />
                </label>
                {errorEdicion && <div className="error-box">{errorEdicion}</div>}
                <div className="acciones">
                  <button
                    type="button"
                    className="boton primario"
                    onClick={guardarDatos}
                    disabled={guardando}
                  >
                    {guardando ? 'Guardando…' : 'Guardar'}
                  </button>
                  <button
                    type="button"
                    className="boton"
                    onClick={() => {
                      setEditando(false)
                      setEquipo(orden.equipo)
                      setProblema(orden.problema)
                    }}
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            ) : (
              <dl className="datos">
                <dt>Problema</dt>
                <dd>{orden.problema}</dd>
                <dt>Estado</dt>
                <dd>
                  <EstadoBadge estado={orden.estado} />
                </dd>
                <dt>Actualizada</dt>
                <dd>{formatearFecha(orden.actualizadoEn)}</dd>
              </dl>
            )}
          </section>

          <PanelPresupuesto orden={orden} />

          <section className="panel">
            <div className="panel-cabecera">
              <h2>
                Fotos ({orden.fotos.length}/{MAX_FOTOS})
              </h2>
            </div>
            <GaleriaFotos fotos={orden.fotos} />

            {fotosRestantes > 0 ? (
              <div className="subida">
                <label className="boton chico">
                  {subiendo ? 'Subiendo…' : '+ Agregar fotos'}
                  <input
                    ref={inputFotos}
                    type="file"
                    accept="image/png,image/jpeg,image/webp,image/gif"
                    multiple
                    hidden
                    onChange={alElegirFotos}
                    disabled={subiendo}
                  />
                </label>
                <span className="mini">
                  Podés agregar {fotosRestantes} foto(s) más (JPG, PNG, WEBP o GIF · máx. 5 MB).
                </span>
              </div>
            ) : (
              <p className="mini">Se alcanzó el máximo de {MAX_FOTOS} fotos.</p>
            )}
          </section>
        </div>

        <div>
          <BotonesTransicion orden={orden} onCambio={alCambiarEstado} />

          <section className="panel">
            <h2>Historial de eventos</h2>
            <HistorialEventos eventos={eventos} />
          </section>
        </div>
      </div>
    </div>
  )
}
