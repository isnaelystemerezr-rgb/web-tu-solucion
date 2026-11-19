-- ============================================================================
-- Seed de datos de prueba
--   · 2 clientes
--   · 4 órdenes en etapas distintas (solicitada, en_diagnostico, en_reparacion,
--     entregada) con sus eventos coherentes con el flujo.
--
-- Se usa data.sql (en lugar de Flyway) porque Hibernate crea el esquema desde las
-- entidades y con "defer-datasource-initialization: true" el seed corre después,
-- lo que mantiene una única fuente de verdad del modelo (las entidades JPA).
-- OJO: se ejecuta en cada arranque de H2 (memoria) → base limpia cada vez.
-- ============================================================================

INSERT INTO clientes (id, nombre, telefono, creado_en) VALUES
  (1, 'María González', '+34 611 223 344', CURRENT_TIMESTAMP),
  (2, 'Javier Ruiz',    '+34 699 887 766', CURRENT_TIMESTAMP);

-- Resetear la secuencia de identidad de clientes (H2 2.x)
ALTER TABLE clientes ALTER COLUMN id RESTART WITH 100;

-- ---------------------------------------------------------------------------
-- Orden 1: "solicitada" (recién recibida, sin diagnóstico)
-- ---------------------------------------------------------------------------
INSERT INTO ordenes (id, cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
VALUES (1, 1, 'iPhone 11', 'No carga y se calienta al conectar el cargador',
        'solicitada', NULL, NULL, '[]', FALSE,
        DATEADD('DAY', -1, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP));

INSERT INTO eventos (orden_id, de, a, creado_en) VALUES
  (1, 'solicitada', 'solicitada', DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- ---------------------------------------------------------------------------
-- Orden 2: "en_diagnostico"
-- ---------------------------------------------------------------------------
INSERT INTO ordenes (id, cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
VALUES (2, 2, 'Notebook Lenovo IdeaPad 3', 'Pantalla negra, enciende pero no da imagen',
        'en_diagnostico', NULL, NULL, '[]', FALSE,
        DATEADD('DAY', -3, CURRENT_TIMESTAMP), DATEADD('DAY', -2, CURRENT_TIMESTAMP));

INSERT INTO eventos (orden_id, de, a, creado_en) VALUES
  (2, 'solicitada', 'solicitada',    DATEADD('DAY', -3, CURRENT_TIMESTAMP)),
  (2, 'solicitada', 'en_diagnostico', DATEADD('DAY', -2, CURRENT_TIMESTAMP));

-- ---------------------------------------------------------------------------
-- Orden 3: "en_reparacion" — pasó por presupuesto y aprobación.
-- Presupuesto: mano de obra 60 + (1 x 85) + (2 x 7.50) = 160.00
-- ---------------------------------------------------------------------------
INSERT INTO ordenes (id, cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
VALUES (3, 1, 'Televisor LG 43"', 'Se apaga solo a los pocos minutos de encender',
        'en_reparacion',
        'Fuente de alimentación con condensadores hinchados; se reemplazan y se revisa la etapa de standby.',
        60.00,
        '[{"descripcion":"Kit de condensadores","cantidad":1,"precio":85.00},{"descripcion":"Fusible térmico","cantidad":2,"precio":7.50}]',
        TRUE,
        DATEADD('DAY', -8, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP));

INSERT INTO eventos (orden_id, de, a, creado_en) VALUES
  (3, 'solicitada',    'solicitada',     DATEADD('DAY', -8, CURRENT_TIMESTAMP)),
  (3, 'solicitada',    'en_diagnostico', DATEADD('DAY', -7, CURRENT_TIMESTAMP)),
  (3, 'en_diagnostico','presupuestada',  DATEADD('DAY', -6, CURRENT_TIMESTAMP)),
  (3, 'presupuestada', 'aprobada',       DATEADD('DAY', -4, CURRENT_TIMESTAMP)),
  (3, 'aprobada',      'en_reparacion',  DATEADD('DAY', -1, CURRENT_TIMESTAMP));

-- ---------------------------------------------------------------------------
-- Orden 4: "entregada" (estado final)
-- Presupuesto: mano de obra 40 + (1 x 55) = 95.00
-- ---------------------------------------------------------------------------
INSERT INTO ordenes (id, cliente_id, equipo, problema, estado, diagnostico, mano_obra,
                     piezas, presupuesto_registrado, creado_en, actualizado_en)
VALUES (4, 2, 'Samsung Galaxy A52', 'Puerto de carga flojo, hay que mover el cable',
        'entregada',
        'Conector de carga desgastado; se reemplaza el módulo de carga y se limpia el zócalo.',
        40.00,
        '[{"descripcion":"Módulo de carga USB-C","cantidad":1,"precio":55.00}]',
        TRUE,
        DATEADD('DAY', -20, CURRENT_TIMESTAMP), DATEADD('DAY', -10, CURRENT_TIMESTAMP));

INSERT INTO eventos (orden_id, de, a, creado_en) VALUES
  (4, 'solicitada',    'solicitada',     DATEADD('DAY', -20, CURRENT_TIMESTAMP)),
  (4, 'solicitada',    'en_diagnostico', DATEADD('DAY', -19, CURRENT_TIMESTAMP)),
  (4, 'en_diagnostico','presupuestada',  DATEADD('DAY', -18, CURRENT_TIMESTAMP)),
  (4, 'presupuestada', 'aprobada',       DATEADD('DAY', -16, CURRENT_TIMESTAMP)),
  (4, 'aprobada',      'en_reparacion',  DATEADD('DAY', -14, CURRENT_TIMESTAMP)),
  (4, 'en_reparacion', 'lista',          DATEADD('DAY', -12, CURRENT_TIMESTAMP)),
  (4, 'lista',         'entregada',      DATEADD('DAY', -10, CURRENT_TIMESTAMP));

ALTER TABLE ordenes  ALTER COLUMN id RESTART WITH 100;
ALTER TABLE eventos  ALTER COLUMN id RESTART WITH 1000;
