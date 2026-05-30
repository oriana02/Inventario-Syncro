-- ============================================================
--  SYNCRO - Base de Datos: MS Gestión-Inventario (Puerto 8082)
--  Motor: MySQL 8.0+
--  Patrón: Database per Service
--  Migrado desde Oracle DB → MySQL (Aiven)
-- ============================================================

CREATE DATABASE IF NOT EXISTS syncro_inventario CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE syncro_inventario;

-- ------------------------------------------------------------
-- TABLA: CATEGORIA
-- Clasificación de productos para filtros (RF-2.4)
-- ------------------------------------------------------------
CREATE TABLE categoria (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100)    NOT NULL UNIQUE,
    descripcion VARCHAR(300),
    activo      TINYINT(1)      NOT NULL DEFAULT 1,
    CONSTRAINT chk_cat_activo CHECK (activo IN (0, 1))
);

-- ------------------------------------------------------------
-- TABLA: PRODUCTO
-- Catálogo con SKU, stock actual y datos del artículo (RF-2.4)
-- empresa_id referencia lógica a MS-Pedidos (sin FK cruzada)
-- ------------------------------------------------------------
CREATE TABLE producto (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    empresa_id          BIGINT          NOT NULL,
    categoria_id        BIGINT,
    sku                 VARCHAR(80)     NOT NULL,
    nombre              VARCHAR(200)    NOT NULL,
    descripcion         VARCHAR(500),
    unidad_medida       VARCHAR(30)     NOT NULL DEFAULT 'UNIDAD',

    -- Stock
    stock_actual        INT             NOT NULL DEFAULT 0,
    stock_reservado     INT             NOT NULL DEFAULT 0,
    stock_minimo        INT             NOT NULL DEFAULT 0,

    -- Precio referencial
    precio_unitario     DECIMAL(14,2)   NOT NULL DEFAULT 0,

    activo              TINYINT(1)      NOT NULL DEFAULT 1,
    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uq_producto_sku_empresa UNIQUE (sku, empresa_id),
    CONSTRAINT fk_producto_categoria   FOREIGN KEY (categoria_id) REFERENCES categoria(id),
    CONSTRAINT chk_stock_actual        CHECK (stock_actual >= 0),
    CONSTRAINT chk_stock_reservado     CHECK (stock_reservado >= 0),
    CONSTRAINT chk_stock_minimo        CHECK (stock_minimo >= 0),
    CONSTRAINT chk_precio_unitario     CHECK (precio_unitario >= 0),
    CONSTRAINT chk_producto_activo     CHECK (activo IN (0, 1))
);

-- ------------------------------------------------------------
-- TABLA: RESERVA_STOCK
-- Stock comprometido durante el proceso de pago (RF-2.5)
-- ------------------------------------------------------------
CREATE TABLE reserva_stock (
    id                  BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    producto_id         BIGINT          NOT NULL,
    pedido_id           BIGINT          NOT NULL,
    cantidad            INT             NOT NULL,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'ACTIVA',
    fecha_creacion      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion    DATETIME        NOT NULL,
    fecha_resolucion    DATETIME,
    CONSTRAINT fk_reserva_producto  FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT chk_reserva_cantidad CHECK (cantidad > 0),
    CONSTRAINT chk_reserva_estado   CHECK (estado IN ('ACTIVA', 'CONFIRMADA', 'LIBERADA', 'EXPIRADA'))
);

-- ------------------------------------------------------------
-- TABLA: MOVIMIENTO_INVENTARIO
-- Registro inmutable de cada cambio de stock (auditoría completa)
-- ------------------------------------------------------------
CREATE TABLE movimiento_inventario (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    producto_id     BIGINT          NOT NULL,
    tipo            VARCHAR(30)     NOT NULL,
    cantidad        INT             NOT NULL,
    stock_anterior  INT             NOT NULL,
    stock_posterior INT             NOT NULL,
    pedido_id       BIGINT,
    usuario_id      BIGINT,
    origen          VARCHAR(20)     NOT NULL DEFAULT 'SISTEMA',
    motivo          VARCHAR(300),
    fecha           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mov_producto   FOREIGN KEY (producto_id) REFERENCES producto(id),
    CONSTRAINT chk_mov_tipo      CHECK (tipo IN (
        'VENTA', 'AJUSTE_ENTRADA', 'AJUSTE_SALIDA',
        'DEVOLUCION', 'MERMA', 'RESERVA', 'LIBERACION_RESERVA'
    )),
    CONSTRAINT chk_mov_origen    CHECK (origen IN ('SISTEMA', 'USUARIO', 'EVENTO_RABBITMQ'))
);

-- ------------------------------------------------------------
-- TABLA: AJUSTE_INVENTARIO
-- Ajustes manuales por ingresos, mermas o correcciones (RF-2.2)
-- ------------------------------------------------------------
CREATE TABLE ajuste_inventario (
    id              BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    producto_id     BIGINT          NOT NULL,
    empresa_id      BIGINT          NOT NULL,
    tipo_ajuste     VARCHAR(30)     NOT NULL,
    cantidad        INT             NOT NULL,
    motivo          VARCHAR(300)    NOT NULL,
    usuario_id      BIGINT          NOT NULL,
    movimiento_id   BIGINT,
    fecha           DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ajuste_producto    FOREIGN KEY (producto_id)   REFERENCES producto(id),
    CONSTRAINT fk_ajuste_movimiento  FOREIGN KEY (movimiento_id) REFERENCES movimiento_inventario(id),
    CONSTRAINT chk_ajuste_tipo       CHECK (tipo_ajuste IN (
        'INGRESO_MERCADERIA', 'MERMA', 'CORRECCION', 'DEVOLUCION_PROVEEDOR'
    )),
    CONSTRAINT chk_ajuste_cantidad   CHECK (cantidad != 0)
);

-- ============================================================
-- ÍNDICES
-- ============================================================
CREATE INDEX idx_producto_empresa    ON producto(empresa_id);
CREATE INDEX idx_producto_sku        ON producto(sku);
CREATE INDEX idx_producto_categoria  ON producto(categoria_id);
CREATE INDEX idx_producto_activo     ON producto(activo);
CREATE INDEX idx_mov_producto_id     ON movimiento_inventario(producto_id);
CREATE INDEX idx_mov_fecha           ON movimiento_inventario(fecha);
CREATE INDEX idx_mov_pedido_id       ON movimiento_inventario(pedido_id);
CREATE INDEX idx_reserva_producto    ON reserva_stock(producto_id);
CREATE INDEX idx_reserva_pedido      ON reserva_stock(pedido_id);
CREATE INDEX idx_reserva_estado      ON reserva_stock(estado);
CREATE INDEX idx_ajuste_producto     ON ajuste_inventario(producto_id);
CREATE INDEX idx_ajuste_empresa      ON ajuste_inventario(empresa_id);

-- Trigger eliminado: movimientos manejados desde InventarioService para evitar duplicacion

-- ============================================================
-- DATOS SEMILLA
-- ============================================================
INSERT INTO categoria (nombre, descripcion) VALUES ('Electrónica',   'Dispositivos y accesorios electrónicos');
INSERT INTO categoria (nombre, descripcion) VALUES ('Indumentaria',  'Ropa y accesorios de vestir');
INSERT INTO categoria (nombre, descripcion) VALUES ('Alimentos',     'Productos alimenticios y bebidas');
INSERT INTO categoria (nombre, descripcion) VALUES ('Sin Categoría', 'Categoría por defecto');

INSERT INTO producto (empresa_id, categoria_id, sku, nombre, descripcion, stock_actual, stock_minimo, precio_unitario)
VALUES (1, 1, 'ELEC-001', 'Audífonos Bluetooth',  'Audífonos inalámbricos con cancelación de ruido', 50, 10, 29990);

INSERT INTO producto (empresa_id, categoria_id, sku, nombre, descripcion, stock_actual, stock_minimo, precio_unitario)
VALUES (1, 1, 'ELEC-002', 'Cable USB-C 1m',       'Cable de carga y datos USB tipo C', 200, 30, 4990);

INSERT INTO producto (empresa_id, categoria_id, sku, nombre, descripcion, stock_actual, stock_minimo, precio_unitario)
VALUES (1, 2, 'IND-001',  'Polera Básica Talla M', 'Polera de algodón 100% unisex', 80, 15, 9990);

COMMIT;
