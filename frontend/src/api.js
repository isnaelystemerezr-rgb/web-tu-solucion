/**
 * Cliente HTTP del backend. usamos fetch puro (sin SDKs ni librerías de estado).
 *
 * Todas las rutas son relativas (/api/...): en desarrollo las proxea Vite hacia
 * Spring Boot y en producción se sirven desde el mismo origen. Si se despliega el
 * backend en otro host, se configura VITE_API_URL en un .env.
 */
const BASE = import.meta.env.VITE_API_URL ?? ''

/** Estados del flujo, en el orden exacto del enunciado. */
export const ESTADOS = [
  { valor: 'solicitada', etiqueta: 'Solicitada' },
  { valor: 'en_diagnostico', etiqueta: 'En diagnóstico' },
  { valor: 'presupuestada', etiqueta: 'Presupuestada' },
  { valor: 'aprobada', etiqueta: 'Aprobada' },
  { valor: 'en_reparacion', etiqueta: 'En reparación' },
  { valor: 'lista', etiqueta: 'Lista' },
  { valor: 'entregada', etiqueta: 'Entregada' },
  { valor: 'rechazada', etiqueta: 'Rechazada' },
  { valor: 'cancelada', etiqueta: 'Cancelada' },
]

/** Mapa valor → etiqueta legible. */
export const ETIQUETA_ESTADO = Object.fromEntries(
  ESTADOS.map((e) => [e.valor, e.etiqueta]),
)

/*
 * No se duplica el grafo de transiciones en el cliente: el backend expone
 * orden.transicionesPermitidas y la UI dibuja exactamente esos botones.
 * (Si se duplicara, podría desincronizarse de la regla real.)
 */

/**
 * Envuelve fetch: agrega el header JSON cuando corresponde, parsea la respuesta
 * y convierte los errores del backend ({mensaje, detalles[]}) en Error de JS.
 */
async function pedir(ruta, opciones = {}) {
  const esFormData = opciones.body instanceof FormData
  const headers = { ...(opciones.headers || {}) }
  if (opciones.body && !esFormData) headers['Content-Type'] = 'application/json'

  const respuesta = await fetch(`${BASE}${ruta}`, { ...opciones, headers })

  if (respuesta.status === 204) return null
  const texto = await respuesta.text()
  const datos = texto ? JSON.parse(texto) : null

  if (!respuesta.ok) {
    const detalle = datos?.detalles?.length
      ? ' · ' + datos.detalles.map((d) => `${d.campo}: ${d.mensaje}`).join(' · ')
      : ''
    const error = new Error((datos?.mensaje || `Error ${respuesta.status}`) + detalle)
    error.status = respuesta.status
    error.codigo = datos?.codigo
    throw error
  }
  return datos
}

// --- Clientes ---------------------------------------------------------------

export const getClientes = () => pedir('/api/clientes')
export const crearCliente = (body) =>
  pedir('/api/clientes', { method: 'POST', body: JSON.stringify(body) })

// --- Órdenes ----------------------------------------------------------------

/** filtroEstado: valor de estado o '' para todas. */
export const getOrdenes = (filtroEstado) =>
  pedir(`/api/ordenes${filtroEstado ? `?estado=${encodeURIComponent(filtroEstado)}` : ''}`)

export const getOrden = (id) => pedir(`/api/ordenes/${id}`)

export const getResumen = () => pedir('/api/ordenes/resumen')

export const getTotal = (id) => pedir(`/api/ordenes/${id}/total`)

/** Crea la orden. Si hay fotos usa multipart (/con-fotos), si no, JSON. */
export function crearOrden({ clienteId, equipo, problema, fotos }) {
  if (fotos && fotos.length > 0) {
    const form = new FormData()
    form.append('clienteId', clienteId)
    form.append('equipo', equipo)
    form.append('problema', problema)
    fotos.forEach((f) => form.append('fotos', f))
    return pedir('/api/ordenes/con-fotos', { method: 'POST', body: form })
  }
  return pedir('/api/ordenes', {
    method: 'POST',
    body: JSON.stringify({ clienteId, equipo, problema }),
  })
}

export const actualizarOrden = (id, body) =>
  pedir(`/api/ordenes/${id}`, { method: 'PUT', body: JSON.stringify(body) })

/**
 * Cambio de estado. `extras` solo lleva datos cuando el destino es "presupuestada".
 */
export const transicionar = (id, a, extras = {}) =>
  pedir(`/api/ordenes/${id}/transicion`, {
    method: 'POST',
    body: JSON.stringify({ a, ...extras }),
  })

export function subirFotos(id, archivos) {
  const form = new FormData()
  archivos.forEach((f) => form.append('fotos', f))
  return pedir(`/api/ordenes/${id}/fotos`, { method: 'POST', body: form })
}

// --- Eventos ----------------------------------------------------------------

export const getEventos = (ordenId) =>
  pedir(`/api/eventos?ordenId=${encodeURIComponent(ordenId)}`)

/**
 * Calcula el total en el cliente: manoObra + Σ(cantidad × precio).
 * Se usa para verificar (y mostrar) que coincide con el total del servidor.
 */
export function calcularTotal(manoObra, piezas) {
  const mano = Number(manoObra) || 0
  const suma = (piezas || []).reduce(
    (acc, p) => acc + (Number(p.cantidad) || 0) * (Number(p.precio) || 0),
    0,
  )
  return Math.round((mano + suma) * 100) / 100
}

export const formatearMoneda = (valor) =>
  new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(
    Number(valor) || 0,
  )

export const formatearFecha = (iso) =>
  iso
    ? new Date(iso).toLocaleString('es-AR', {
        dateStyle: 'short',
        timeStyle: 'short',
      })
    : '—'
