-- ============================================================================
-- Seed equivalente para PostgreSQL (perfil "postgres").
--
-- El perfil postgres NO ejecuta data.sql automáticamente (data.sql usa sintaxis
-- H2). Para cargar estos datos de prueba:
--
--   createdb taller
--   psql -d taller -f backend/db/postgres-seed.sql
--
-- Requiere que las tablas ya existan: levantar una vez la app con el perfil
-- postgres (ddl-auto: update) y luego ejecutar este script.
-- ============================================================================

INSERT INTO clientes (nombre, telefono, creado_en) VALUES
  ('María González', '+34 611 223 344', now()),
  ('Javier Ruiz',    '+34 699 887 766', now())
ON CONFLICT DO NOTHING;

-- Orden 1: solicitada
INSERT INTO ordenes (cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
SELECT c.id, 'iPhone 11', 'No carga y se calienta al conectar el cargador',
       'solicitada', NULL, NULL, '[]', false, now() - interval '1 day', now() - interval '1 day'
FROM clientes c WHERE c.nombre = 'María González' LIMIT 1;

-- Orden 2: en_diagnostico
INSERT INTO ordenes (cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
SELECT c.id, 'Notebook Lenovo IdeaPad 3', 'Pantalla negra, enciende pero no da imagen',
       'en_diagnostico', NULL, NULL, '[]', false, now() - interval '3 days', now() - interval '2 days'
FROM clientes c WHERE c.nombre = 'Javier Ruiz' LIMIT 1;

-- Orden 3: en_reparacion (presupuesto 60 + 85 + 2*7.50 = 160.00)
INSERT INTO ordenes (cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
SELECT c.id, 'Televisor LG 43"', 'Se apaga solo a los pocos minutos de encender',
       'en_reparacion',
       'Fuente de alimentación con condensadores hinchados; se reemplazan y se revisa la etapa de standby.',
       60.00,
       '[{"descripcion":"Kit de condensadores","cantidad":1,"precio":85.00},{"descripcion":"Fusible térmico","cantidad":2,"precio":7.50}]',
       true, now() - interval '8 days', now() - interval '1 day'
FROM clientes c WHERE c.nombre = 'María González' LIMIT 1;

-- Orden 4: entregada (presupuesto 40 + 55 = 95.00)
INSERT INTO ordenes (cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
SELECT c.id, 'Samsung Galaxy A52', 'Puerto de carga flojo, hay que mover el cable',
       'entregada',
       'Conector de carga desgastado; se reemplaza el módulo de carga y se limpia el zócalo.',
       40.00,
       '[{"descripcion":"Módulo de carga USB-C","cantidad":1,"precio":55.00}]',
       true, now() - interval '20 days', now() - interval '10 days'
FROM clientes c WHERE c.nombre = 'Javier Ruiz' LIMIT 1;

-- Eventos coherentes con el flujo (se insertan por orden, en orden cronológico)
INSERT INTO eventos (orden_id, de, a, creado_en)
SELECT o.id, e.de, e.a, o.creado_en + e.offset_h
FROM ordenes o
CROSS JOIN (VALUES
    ('Notebook Lenovo IdeaPad 3', 'solicitada', 'solicitada', interval '0 hour'),
    ('Notebook Lenovo IdeaPad 3', 'solicitada', 'en_diagnostico', interval '24 hours'),
    ('Televisor LG 43"', 'solicitada', 'solicitada', interval '0 hour'),
    ('Televisor LG 43"', 'solicitada', 'en_diagnostico', interval '24 hours'),
    ('Televisor LG 43"', 'en_diagnostico', 'presupuestada', interval '48 hours'),
    ('Televisor LG 43"', 'presupuestada', 'aprobada', interval '96 hours'),
    ('Televisor LG 43"', 'aprobada', 'en_reparacion', interval '168 hours'),
    ('Samsung Galaxy A52', 'solicitada', 'solicitada', interval '0 hour'),
    ('Samsung Galaxy A52', 'solicitada', 'en_diagnostico', interval '24 hours'),
    ('Samsung Galaxy A52', 'en_diagnostico', 'presupuestada', interval '48 hours'),
    ('Samsung Galaxy A52', 'presupuestada', 'aprobada', interval '96 hours'),
    ('Samsung Galaxy A52', 'aprobada', 'en_reparacion', interval '144 hours'),
    ('Samsung Galaxy A52', 'en_reparacion', 'lista', interval '192 hours'),
    ('Samsung Galaxy A52', 'lista', 'entregada', interval '240 hours')
) AS e(equipo, de, a, offset_h)
WHERE o.equipo = e.equipo;

-- La orden 1 (solicitada) solo tiene su evento de apertura
INSERT INTO eventos (orden_id, de, a, creado_en)
SELECT o.id, 'solicitada', 'solicitada', o.creado_en
FROM ordenes o WHERE o.equipo = 'iPhone 11';
