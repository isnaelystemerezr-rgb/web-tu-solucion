# Segundo Parcial — Taller de reparaciones

Sistema de gestión para un taller de reparación de equipos electrónicos (teléfonos,
laptops, televisores). **El personal del taller es el único usuario**: no hay login,
usuarios ni roles. Los clientes son solo datos registrados.

La aplicación registra **clientes, órdenes, diagnósticos, presupuestos, reparaciones,
entregas e historial de cambios de estado**, y garantiza el flujo de estados en el
backend aunque se llame la API directamente sin pasar por la interfaz.

| Capa | Stack |
|---|---|
| Backend | Java 17 · Spring Boot 3.3.5 · Spring Data JPA · Bean Validation · H2 (o PostgreSQL por perfil) |
| Frontend | React 18 · Vite 6 · React Router 7 (`react-router`) · JavaScript · `fetch` |

---

## 1. Estructura del proyecto

```
web-tu-solucion/
├── backend/                                  # API REST Spring Boot (Maven)
│   ├── pom.xml
│   ├── db/postgres-seed.sql                  # seed equivalente para PostgreSQL
│   └── src/
│       ├── main/java/ar/edu/taller/
│       │   ├── TallerApplication.java
│       │   ├── config/WebConfig.java         # /uploads estático + CORS
│       │   ├── domain/                       # Entidades JPA y reglas de estado
│       │   │   ├── Cliente.java
│       │   │   ├── Orden.java
│       │   │   ├── Evento.java
│       │   │   ├── Pieza.java                # línea de presupuesto (no es entidad)
│       │   │   ├── PiezasJsonConverter.java  # List<Pieza> ↔ JSON
│       │   │   ├── EstadoOrden.java          # enum + grafo de transiciones
│       │   │   └── EstadoOrdenConverter.java # persiste 'en_diagnostico' (minúsculas)
│       │   ├── repo/                         # Spring Data JPA
│       │   ├── service/                      # Lógica de negocio
│       │   │   ├── OrdenService.java         # transiciones, eventos, presupuesto
│       │   │   ├── ClienteService.java
│       │   │   ├── EventoService.java
│       │   │   ├── FotoStorageService.java
│       │   │   └── OrdenMapper.java          # total calculado
│       │   └── web/                          # Controladores REST + DTOs + errores
│       ├── main/resources/
│       │   ├── application.yml               # H2 (desarrollo)
│       │   ├── application-postgres.yml      # perfil opcional PostgreSQL
│       │   └── data.sql                      # seed: 2 clientes + 4 órdenes
│       └── test/java/ar/edu/taller/OrdenesApiTest.java
├── frontend/                                 # React + Vite
│   ├── index.html · vite.config.js · package.json
│   └── src/
│       ├── main.jsx                          # router (rutas anidadas, 404)
│       ├── App.jsx                           # layout con <Outlet/>
│       ├── api.js                            # fetch + helpers (total, formato)
│       ├── styles.css
│       ├── components/                       # piezas pequeñas reutilizables
│       │   ├── BotonesTransicion.jsx
│       │   ├── ModalPresupuesto.jsx
│       │   ├── PanelPresupuesto.jsx
│       │   ├── HistorialEventos.jsx
│       │   ├── TablaOrdenes.jsx
│       │   ├── FiltroEstado.jsx
│       │   ├── GaleriaFotos.jsx
│       │   ├── EstadoBadge.jsx · Cargando.jsx · ErrorBox.jsx
│       └── pages/
│           ├── OrdenesLista.jsx   # /ordenes  (filtro por estado)
│           ├── OrdenNueva.jsx     # /ordenes/nueva (cliente + hasta 3 fotos)
│           ├── OrdenDetalle.jsx   # /ordenes/:id (parámetro de ruta)
│           ├── Resumen.jsx        # /resumen
│           ├── Clientes.jsx       # /clientes
│           ├── NoEncontrada.jsx   # 404
│           └── ErrorDeRuta.jsx
├── start.sh                                  # arranca backend + frontend
└── README.md
```

---

## 2. Levantar desde cero en una máquina limpia

### Requisitos

- **JDK 17 o superior** (`java -version`)
- **Maven 3.9+** (`mvn -version`) — o usar el wrapper si lo agregás
- **Node.js 18+** y npm (`node -v`, `npm -v`)

### Opción rápida (dos terminales — recomendado)

```bash
# Terminal 1 — backend
cd backend
mvn spring-boot:run
# API en http://localhost:8080 · consola H2 en http://localhost:8080/h2-console

# Terminal 2 — frontend
cd frontend
npm install
npm run dev
# App en http://localhost:5173
```

Abrí **http://localhost:5173**.

### Opción con un solo comando

```bash
chmod +x start.sh
./start.sh
```

### Datos de prueba

Se cargan **automáticamente al arrancar** (H2 en memoria + `backend/src/main/resources/data.sql`):

- 2 clientes: *María González*, *Javier Ruiz*
- 4 órdenes en etapas distintas: `solicitada`, `en_diagnostico`, `en_reparacion`, `entregada`
- Sus eventos de historial coherentes con el flujo

H2 es en memoria: cada reinicio del backend deja la base limpia y vuelve a ejecutar el seed.
La consola está en `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:taller`, usuario `sa`, sin contraseña).

### PostgreSQL (opcional, perfil `postgres`)

```bash
createdb taller
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=postgres   # crea las tablas
psql -d taller -f db/postgres-seed.sql                    # datos de prueba
```

Variables: `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` (por defecto
`localhost:5432/taller`, `postgres/postgres`).

### Tests del backend

```bash
cd backend && mvn test
```

Cubren, vía HTTP (`MockMvc`), las garantías duras: flujo completo, rechazo de saltos,
estados finales, cancelación solo desde `solicitada`, presupuesto inmutable, total
calculado, eventos inmutables (405), órdenes no borrables (405) y validaciones.

### Build de producción del frontend

```bash
cd frontend && npm run build     # genera frontend/dist
```

Si el backend no está en el mismo origen, definí `VITE_API_URL` en `frontend/.env`
(ver `.env.example`). En desarrollo **no hace falta**: `vite.config.js` proxea `/api` y
`/uploads` a `http://127.0.0.1:8080`, así no hay CORS ni URLs de localhost en el cliente.

---

## 3. Decisiones técnicas (y justificación)

### Maven en lugar de Gradle

Se eligió **Maven** porque: (a) el proyecto tiene un solo módulo y no necesita la
flexibilidad de build scripts de Gradle; (b) el `pom.xml` declara dependencias de forma
declarativa y es el estándar que se usa en la cursada; (c) el ciclo
`mvn spring-boot:run` / `mvn test` / `mvn package` es suficiente y reproducible; y
(d) el enunciado ya pide Spring Boot, y el `spring-boot-starter-parent` gestiona las
versiones de todas las dependencias sin configuración extra.

### Piezas como JSON en una columna

`piezas` se define en el enunciado como **JSON** (`[{descripcion, cantidad, precio}]`).
Se guarda serializado en una columna `text` con un `AttributeConverter`
(`PiezasJsonConverter`), no en una tabla hija, porque:

- las piezas siempre se leen y escriben junto con la orden (nunca se consultan por pieza);
- la lista es pequeña (unas pocas líneas por presupuesto) y no crece con el tiempo;
- se evita una tabla + join + cascade para datos que son un *value object* del presupuesto.

`Pieza` sí tiene Bean Validation (`@NotBlank`, `@Positive`, `@PositiveOrZero`) y se
valida al entrar en el DTO (`@Valid`), así que el JSON no es "texto libre".

### Estado del select en minúsculas

`EstadoOrden` es un enum cuyo valor persistido y expuesto es **exactamente** el del
enunciado (`solicitada`, `en_diagnostico`, …). Se usa `EstadoOrdenConverter` en vez de
`@Enumerated(STRING)` para que base de datos, API y frontend hablen el mismo idioma
(si no, la columna guardaría `EN_DIAGNOSTICO` y el seed/`?estado=` dejarían de coincidir).

### Seed con `data.sql` en vez de Flyway

Hibernate genera el esquema desde las entidades (`ddl-auto: create-drop`) y, con
`spring.jpa.defer-datasource-initialization: true`, `data.sql` corre después. Así hay
**una sola fuente de verdad del modelo** (las entidades) y el seed no puede quedar
desactualizado respecto de ellas. Flyway obligaría a mantener a mano un DDL paralelo,
lo que en un proyecto de parcial es una fuente de errores. Para PostgreSQL el seed es
`backend/db/postgres-seed.sql`.

### Total no persistido

No existe columna `total`. Se calcula en el servidor (`Orden.calcularTotal()`, expuesto
en `GET /api/ordenes/{id}/total` y en `presupuesto.total` del detalle) y el frontend lo
recalcula con `calcularTotal()` para **verificar que coinciden** (se muestra ✔ o ⚠ en la
pantalla de detalle).

---

## 4. Modelo de datos

**`clientes`**: `nombre` (texto), `telefono` (texto).

**`ordenes`**: `cliente` (relación requerida a `clientes`), `equipo` (texto),
`problema` (texto), `estado` (select con los 9 valores), `diagnostico` (texto),
`manoObra` (número), `piezas` (JSON), `fotos` (hasta 3 imágenes).
Extras internos: `presupuesto_registrado` (candado de inmutabilidad), `creado_en`,
`actualizado_en`.

**`eventos`**: `orden` (relación a `ordenes`), `de` (texto), `a` (texto),
`creado_en` (fecha automática del servidor). Solo se crean.

Relaciones: `Orden → Cliente` `@ManyToOne` (obligatoria), `Evento → Orden` `@ManyToOne`.
Las fotos se guardan en la tabla `orden_fotos` (`@ElementCollection`), con la ruta
relativa `/uploads/<archivo>`.

### Flujo de estados (exacto)

```
solicitada ──▶ en_diagnostico ──▶ presupuestada ──┬──▶ aprobada ──▶ en_reparacion ──▶ lista ──▶ entregada
                                                  └──▶ rechazada        (final)
     │
     └──▶ cancelada  (final)

rechazada, cancelada y entregada son FINALES: no admiten más cambios.
```

Transiciones permitidas, exactamente estas y ninguna otra:

| De | A |
|---|---|
| `solicitada` | `en_diagnostico`, `cancelada` |
| `en_diagnostico` | `presupuestada` |
| `presupuestada` | `aprobada`, `rechazada` |
| `aprobada` | `en_reparacion` |
| `en_reparacion` | `lista` |
| `lista` | `entregada` |

---

## 5. Cómo se cumple cada garantía (dónde mirar el código)

| Garantía | Implementación |
|---|---|
| **1. Transiciones validadas en servicio** | `EstadoOrden.TRANSICIONES` (grafo único) + `OrdenService.aplicarTransicion()` lanza `TRANSICION_INVALIDA` (HTTP 409) con el estado actual y los destinos posibles. |
| **2. Evento automático, misma transacción** | `@Transactional` en `aplicarTransicion`; `eventos.save(Evento.registrar(orden, de, a, Instant.now()))` en la misma unidad de trabajo. Si el evento falla, se revierte el cambio. `Evento` no tiene setters ni endpoints de mutación (`PUT/PATCH/DELETE → 405`). |
| **3. Persistencia relacional, frontend = servidor** | H2 en memoria (PostgreSQL por perfil). Ningún componente guarda copias: después de cada cambio se usa la entidad devuelta o se vuelve a pedir. |
| **4. Cancelación solo desde `solicitada`** | Grafo (`solicitada → cancelada`) + comprobación explícita en `aplicarTransicion` con código `CANCELACION_INVALIDA`. |
| **5. Presupuesto inmutable** | Se registra en `registrarPresupuesto()` al pasar a `presupuestada` y activa `presupuestoRegistrado=true`. Cualquier `PUT /api/ordenes/{id}` con `diagnostico/manoObra/piezas` posterior devuelve `PRESUPUESTO_INMUTABLE` (409). No se acepta `estado` en el DTO de actualización. |
| **6. Total no persistido y coincidente** | `Orden.calcularTotal()` (servidor) y `calcularTotal()` (cliente) usan la misma fórmula `manoObra + Σ(cantidad × precio)`. Endpoint de solo lectura `GET /api/ordenes/{id}/total`. |
| **7. Órdenes no borrables** | No existe `DELETE /api/ordenes/{id}` (405). `OrdenService` no tiene método de borrado. |
| **8. Bean Validation + reglas en servicio** | DTOs `record` anotados (`@NotNull`, `@NotBlank`, `@Size`, `@Positive`, `@PositiveOrZero`, `@Valid` en listas anidadas). Reglas de negocio en `OrdenService`/`ClienteService`, nunca solo en la UI. |

Errores: `GlobalExceptionHandler` (`@RestControllerAdvice`) devuelve siempre JSON
`{ timestamp, status, codigo, mensaje, ruta, detalles[] }` — incluidos los `detalles`
por campo de Bean Validation.

---

## 6. API REST

Base: `http://localhost:8080`

### Clientes

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/clientes` | Listar (ordenados por nombre) |
| GET | `/api/clientes/{id}` | Ver |
| POST | `/api/clientes` | Crear `{nombre, telefono}` |
| PUT | `/api/clientes/{id}` | Actualizar |
| DELETE | `/api/clientes/{id}` | Borrar (409 si tiene órdenes) |

### Órdenes

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/ordenes?estado=en_reparacion` | Listar / filtrar por estado |
| GET | `/api/ordenes/{id}` | Detalle (incluye `transicionesPermitidas`, `presupuesto.total`) |
| POST | `/api/ordenes` | Crear `{clienteId, equipo, problema}` → nace en `solicitada` |
| POST | `/api/ordenes/con-fotos` | Crear con `multipart/form-data` (hasta 3 fotos) |
| PUT | `/api/ordenes/{id}` | Actualizar `clienteId/equipo/problema` y, si aún no hay presupuesto, `diagnostico/manoObra/piezas` |
| POST | `/api/ordenes/{id}/transicion` | **Cambiar estado** (único camino) |
| POST | `/api/ordenes/{id}/fotos` | Agregar fotos (`multipart`, máximo total 3) |
| GET | `/api/ordenes/{id}/total` | Total calculado (solo lectura) |
| GET | `/api/ordenes/resumen` | Cantidad de órdenes por etapa |
| — | `DELETE` | **No existe** → 405 |

Cuerpo de una transición (el presupuesto solo se envía al pasar a `presupuestada`):

```json
POST /api/ordenes/3/transicion
{ "a": "presupuestada",
  "diagnostico": "Condensadores hinchados",
  "manoObra": 60,
  "piezas": [{ "descripcion": "Kit de condensadores", "cantidad": 1, "precio": 85 }] }
```

Ejemplos con `curl` (probando las garantías sin la UI):

```bash
# Salto inválido → 409 TRANSICION_INVALIDA
curl -i -X POST http://localhost:8080/api/ordenes/1/transicion \
  -H 'Content-Type: application/json' -d '{"a":"aprobada"}'

# Cancelar ya no es posible fuera de 'solicitada' → 409
curl -i -X POST http://localhost:8080/api/ordenes/2/transicion \
  -H 'Content-Type: application/json' -d '{"a":"cancelada"}'

# Editar eventos / borrar órdenes → 405
curl -i -X DELETE http://localhost:8080/api/eventos/1
curl -i -X DELETE http://localhost:8080/api/ordenes/1
```

### Eventos (solo lectura y creación)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/eventos?ordenId=3` | Listar eventos (historial) |
| GET | `/api/eventos/{id}` | Ver |
| POST | `/api/eventos` | Crear `{ordenId, a}` — valida y ejecuta la transición igual que `/transicion` |
| PUT/PATCH/DELETE | `/api/eventos/**` | **405** |

### Fotos

Se guardan en `backend/uploads/` y se sirven como recursos estáticos:
`GET /uploads/<archivo>`. Formatos: JPG, PNG, WEBP, GIF · máx. 5 MB por archivo · máx. 3 por orden.

---

## 7. Frontend

- **Rutas** (`createBrowserRouter`, anidadas con `<Outlet/>` en `App.jsx`):
  `/` y `/ordenes` (lista + filtro), `/ordenes/nueva`, `/ordenes/:id` (parámetro de ruta),
  `/resumen`, `/clientes`, `*` → página **404**. `errorElement` para errores de ruta.
- **`useEffect` con dependencias y limpieza**: cada carga usa una bandera `activo` en la
  limpieza para descartar respuestas en vuelo (cambio de filtro o de `:id` rápido).
- **`useRef`**: `<input type="file">` en `OrdenDetalle` (limpiar el valor para reelegir el
  mismo archivo) y foco automático del diagnóstico en `ModalPresupuesto`.
- **`useMemo`**: en `Resumen` (agrupar/ordenar por etapa, total y máximo para las barras) y
  en `OrdenDetalle` (fotos restantes, derivado de `orden.fotos`). Ambos son valores
  derivados que se recalculan solo cuando cambia su dependencia.
- **Sin librerías de estado ni de componentes**: solo React, React Router y `fetch`.
- **Listas con `key`**, componentes pequeños que reciben props, formularios controlados.
- **Botones de transición**: se dibujan a partir de `orden.transicionesPermitidas` que
  envía el backend (no se duplica el grafo en el cliente).

Pantallas: lista de órdenes con filtro por estado · detalle con historial de eventos y
total calculado · nueva orden con selección de cliente (y alta rápida de cliente) y
subida de hasta 3 fotos · botones de transición según el estado · resumen por etapa ·
clientes · 404.

---

## 8. Historial de commits (avance por etapas)

El repositorio se construyó en etapas; `git log --oneline` muestra, en orden:

1. Configuración del proyecto y estructura de carpetas
2. Dominio: entidades JPA, enum de estados y grafo de transiciones
3. Repositorios y DTOs con Bean Validation
4. Servicios: transiciones, eventos y presupuesto inmutable
5. Controladores REST y manejo global de errores
6. Fotos, seed de datos y configuración de perfiles
7. Tests de integración de las garantías
8. Frontend: rutas, layout y cliente HTTP
9. Frontend: páginas y componentes
10. README y script de arranque

---

## 9. Notas sobre el enunciado adjunto

Se priorizaron las garantías de la consigna escrita. Diferencias y aclaraciones:

1. **Backend en Java/Spring Boot en lugar de PocketBase** (variante pedida). Eso elimina
   las reglas de PocketBase (API rules, hooks) y las reemplaza por validación en la capa
   de servicio + `@ControllerAdvice`.
2. **Eventos**: se permite `POST /api/eventos` (lo pide el enunciado) pero **no** es un
   insert libre: deduce `de` del estado actual de la orden y aplica las mismas reglas de
   transición, para que no se puedan fabricar eventos que contradigan el estado real.
3. **Orden creada**: se registra un evento de apertura (`de = a = "solicitada"`) para que
   el historial empiece en el minuto cero. Es el único caso en que `de === a`.
4. **Presupuesto**: solo puede cargarse **al** transicionar a `presupuestada`. Si se
   intenta con `PUT` después, el backend responde `409 PRESUPUESTO_INMUTABLE`. Antes de
   esa transición, `diagnostico/manoObra/piezas` sí pueden guardarse como borrador.
5. **`fotos`**: además del alta con `multipart`, un `PUT` no modifica fotos; se agregan con
   `POST /api/ordenes/{id}/fotos` respetando el máximo de 3.
6. **Autenticación**: no existe (el taller es el único usuario), tal como indica el
   contexto de negocio.
