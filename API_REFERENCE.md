# Referencia de la API — Taller de reparaciones

Base URL: `http://localhost:8080` · Sin autenticación (el taller es el único usuario).

Los errores siempre devuelven el mismo JSON:

```json
{
  "timestamp": "2026-11-19T12:00:00Z",
  "status": 409,
  "codigo": "TRANSICION_INVALIDA",
  "mensaje": "Transición no permitida: 'solicitada' → 'aprobada'. Desde 'solicitada' solo se puede pasar a [cancelada, en_diagnostico].",
  "ruta": "/api/ordenes/1/transicion",
  "detalles": []
}
```

En errores de Bean Validation, `codigo` es `VALIDACION` y `detalles` trae `[{campo, mensaje}]`.

| Código | HTTP | Cuándo |
|---|---|---|
| `VALIDACION` | 400 | DTO inválido (`@NotNull`, `@Size`, `@Positive`…) |
| `CUERPO_INVALIDO` | 400 | JSON malformado o tipo incorrecto |
| `PARAMETRO_INVALIDO` / `PARAMETRO_FALTANTE` | 400 | Query/param inválido o ausente |
| `ESTADO_INVALIDO` | 400 | Valor de estado que no existe |
| `CLIENTE_INEXISTENTE` | 400 | `clienteId` no existe |
| `PRESUPUESTO_REQUERIDO` | 400 | Pasar a `presupuestada` sin diagnóstico/manoObra/piezas |
| `LIMITE_FOTOS` | 400 | Más de 3 fotos por orden |
| `ARCHIVO_INVALIDO` | 400 | Formato de imagen no permitido |
| `TRANSICION_INVALIDA` | 409 | Salto de estado o estado final |
| `CANCELACION_INVALIDA` | 409 | Cancelar fuera de `solicitada` |
| `PRESUPUESTO_INMUTABLE` | 409 | Modificar el presupuesto ya registrado |
| `CLIENTE_CON_ORDENES` | 409 | Borrar un cliente con órdenes |
| `NO_ENCONTRADO` | 404 | Recurso inexistente |
| `METODO_NO_PERMITIDO` | 405 | Verbo no soportado (p. ej. DELETE de una orden) |
| `ARCHIVO_MUY_GRANDE` | 413 | Foto de más de 5 MB |

---

## Clientes

### `GET /api/clientes`
```json
[{ "id": 1, "nombre": "María González", "telefono": "+34 611 223 344", "creadoEn": "2026-11-19T10:00:00Z" }]
```

### `POST /api/clientes` → 201
```json
{ "nombre": "María González", "telefono": "+34 611 223 344" }
```
> `nombre`: obligatorio, ≤ 80 · `telefono`: obligatorio, 6–30, formato `^[0-9+()\-\s]{6,30}$`

### `GET /api/clientes/{id}` · `PUT /api/clientes/{id}` · `DELETE /api/clientes/{id}` → 204
`DELETE` devuelve **409 `CLIENTE_CON_ORDENES`** si el cliente tiene órdenes (no se rompe el histórico).

---

## Órdenes

### `GET /api/ordenes?estado=en_reparacion`
Sin `estado` devuelve todas. Orden descendente por `creadoEn`.

```json
[
  {
    "id": 3,
    "cliente": { "id": 1, "nombre": "María González", "telefono": "+34 611 223 344", "creadoEn": "…" },
    "equipo": "Televisor LG 43\"",
    "problema": "Se apaga solo a los pocos minutos de encender",
    "estado": "en_reparacion",
    "diagnostico": "Fuente de alimentación con condensadores hinchados…",
    "manoObra": 60.00,
    "piezas": [
      { "descripcion": "Kit de condensadores", "cantidad": 1, "precio": 85.00, "subtotal": 85.00 },
      { "descripcion": "Fusible térmico", "cantidad": 2, "precio": 7.50, "subtotal": 15.00 }
    ],
    "fotos": ["/uploads/2f3c…jpg"],
    "presupuesto": { "registrado": true, "total": 160.00 },
    "transicionesPermitidas": ["lista"],
    "creadoEn": "2026-11-11T10:00:00Z",
    "actualizadoEn": "2026-11-18T10:00:00Z"
  }
]
```

> `presupuesto.total` es **calculado** (`manoObra + Σ cantidad × precio`), no persistido.
> `transicionesPermitidas` es la lista de destinos legales del estado actual: la UI dibuja
> exactamente esos botones.

### `GET /api/ordenes/{id}` → igual al objeto anterior · 404 `NO_ENCONTRADO`

### `POST /api/ordenes` → 201
```json
{ "clienteId": 1, "equipo": "iPhone 11", "problema": "No carga" }
```
La orden nace siempre en `solicitada` (no se acepta `estado` en el cuerpo) y se crea el
evento de apertura.

### `POST /api/ordenes/con-fotos` → 201 (`multipart/form-data`)
Campos: `clienteId`, `equipo`, `problema`, y `fotos` (repetible, máx. 3).

### `PUT /api/ordenes/{id}`
Campos permitidos: `clienteId` (solo en `solicitada`/`en_diagnostico`), `equipo`,
`problema`, y el presupuesto **solo antes** de registrarlo:
`diagnostico`, `manoObra`, `piezas`.
El `estado` **no** se puede cambiar por aquí. Si el presupuesto ya fue registrado:
**409 `PRESUPUESTO_INMUTABLE`**.

### `POST /api/ordenes/{id}/transicion` → 200 (orden actualizada)
```json
{ "a": "presupuestada", "diagnostico": "…", "manoObra": 60, "piezas": [{ "descripcion": "…", "cantidad": 1, "precio": 85 }] }
```
- `a` es obligatorio y debe ser uno de los 9 valores.
- Solo `solicitada → en_diagnostico`, `en_diagnostico → presupuestada`,
  `presupuestada → aprobada|rechazada`, `aprobada → en_reparacion`,
  `en_reparacion → lista`, `lista → entregada`, `solicitada → cancelada`.
- Cualquier otra cosa → **409 `TRANSICION_INVALIDA`**; `entregada`/`rechazada`/`cancelada` son finales.
- Al pasar a `presupuestada`, el presupuesto es obligatorio y se guarda una única vez.
- Cada transición crea un evento en la misma transacción.

### `POST /api/ordenes/{id}/fotos` → 200 (`multipart/form-data`)
Campo `fotos` (repetible). Respeta el máximo de 3 acumuladas.

### `GET /api/ordenes/{id}/total` (solo lectura)
```json
{
  "ordenId": 3,
  "manoObra": 60.00,
  "piezas": [{ "descripcion": "Kit de condensadores", "cantidad": 1, "precio": 85.00, "subtotal": 85.00 }],
  "total": 160.00,
  "registrado": true
}
```

### `GET /api/ordenes/resumen`
```json
[
  { "estado": "solicitada", "cantidad": 1, "final": false },
  { "estado": "en_diagnostico", "cantidad": 1, "final": false },
  { "estado": "presupuestada", "cantidad": 0, "final": false },
  { "...": "las 9 etapas, incluidas las que tienen 0" }
]
```

### `DELETE /api/ordenes/{id}` → **405** (las órdenes no se borran)

---

## Eventos

### `GET /api/eventos?ordenId=3` · `GET /api/eventos` · `GET /api/eventos/{id}`
```json
[{ "id": 12, "ordenId": 3, "de": "en_diagnostico", "a": "presupuestada", "creadoEn": "2026-11-17T10:00:00Z" }]
```

### `POST /api/eventos` → 201
```json
{ "ordenId": 3, "a": "aprobada" }
```
Crea el evento **y** ejecuta la transición, aplicando las mismas reglas que
`/transicion`. El `de` lo deduce el servidor del estado actual y la fecha la pone el
servidor: no se pueden fabricar eventos incoherentes.

### `PUT` · `PATCH` · `DELETE /api/eventos/{id}` → **405** `EVENTO_INMUTABLE`
Los eventos son append-only: no se editan ni se borran.

---

## Fotos

`GET /uploads/<archivo>` sirve la imagen guardada en `backend/uploads/`.
Formatos: JPG, PNG, WEBP, GIF · máx. 5 MB por archivo · máx. 3 por orden.
