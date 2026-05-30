# MS-Inventario — Syncro Platform
Microservicio de gestión de inventario en tiempo real para la plataforma Syncro.  
Puerto: **8082** | Base de datos: **MySQL (Aiven)** | Mensajería: **RabbitMQ (CloudAMQP)**

---

## 1. Descripción del Microservicio

MS-Inventario es responsable de gestionar el stock de productos de las PYMEs que usan Syncro.  
Se comunica de forma asíncrona con MS-Pedidos mediante RabbitMQ: al consumir el evento `pedido.creado`, descuenta automáticamente las unidades comprometidas, eliminando el riesgo de overselling.

### Funcionalidades principales
- Consulta de productos y stock por empresa y categoría
- Descuento automático de stock al confirmar pedidos (evento RabbitMQ)
- Ajuste manual de inventario (entradas, mermas, correcciones)
- Reserva y liberación de stock
- CRUD completo de productos
- Trazabilidad de movimientos de inventario

---

## 2. Tecnologías utilizadas

| Tecnología | Versión | Rol |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 3.3.0 | Framework backend |
| Spring Data JPA | - | Persistencia de datos (patrón Repository) |
| Spring Security | - | Autenticación y autorización |
| Spring AMQP | - | Consumidor RabbitMQ |
| MySQL (Aiven) | 8.x | Base de datos propia (patrón Database per Service) |
| RabbitMQ (CloudAMQP) | - | Message broker para eventos asíncronos |
| Springdoc OpenAPI | 2.8.8 | Documentación Swagger |
| JaCoCo | 0.8.11 | Cobertura de pruebas unitarias |
| JUnit 5 + Mockito | - | Pruebas unitarias |
| Maven | 3.9.x | Gestión de dependencias |

---

## 3. Requisitos previos

- Java 21 instalado
- Maven 3.9+ (o usar el wrapper `./mvnw`)
- Acceso a MySQL en Aiven (credenciales en `.env`)
- Acceso a RabbitMQ en CloudAMQP (credenciales en `.env`)

---

## 4. Instalación y configuración

### 4.1 Clonar el repositorio
```bash
git clone https://github.com/oriana02/Inventario-Syncro.git
cd Inventario-Syncro
```

### 4.2 Crear el archivo de credenciales locales
Crear el archivo `src/main/resources/application-dev.properties` con las siguientes variables (NO subir a GitHub):

```properties
spring.datasource.url=jdbc:mysql://<HOST_AIVEN>:<PUERTO>/syncro_inventario?ssl-mode=REQUIRED
spring.datasource.username=<USUARIO>
spring.datasource.password=<PASSWORD>

spring.rabbitmq.host=<HOST_CLOUDAMQP>
spring.rabbitmq.port=5672
spring.rabbitmq.username=<USUARIO_RABBIT>
spring.rabbitmq.password=<PASSWORD_RABBIT>
spring.rabbitmq.virtual-host=<VHOST>
```

### 4.3 Verificar que el perfil dev está activo
En `src/main/resources/application.properties`:
```properties
spring.profiles.active=dev
```

---

## 5. Ejecución

```bash
# Con Maven Wrapper (recomendado)
./mvnw spring-boot:run

# Con Maven instalado
mvn spring-boot:run
```

El servicio levanta en: `http://localhost:8082`

---

## 6. API REST — Endpoints disponibles

Documentación interactiva completa en Swagger:
```
http://localhost:8082/swagger-ui.html
```

| Método | Endpoint | Descripción | Auth |
|---|---|---|---|
| GET | `/inventario` | Health check | No |
| GET | `/inventario/productos` | Listar productos por empresa | JWT |
| GET | `/inventario/productos/{id}` | Obtener producto por ID | JWT |
| POST | `/inventario/productos` | Crear nuevo producto | JWT |
| PATCH | `/inventario/productos/{id}` | Actualizar producto | JWT |
| DELETE | `/inventario/productos/{id}` | Eliminar producto (borrado lógico) | JWT |
| PATCH | `/inventario/descontar` | Descontar stock al confirmar pedido | JWT |
| POST | `/inventario/ajuste` | Ajuste manual de inventario | JWT |
| POST | `/inventario/reserva` | Reservar stock para un pedido | JWT |
| POST | `/inventario/reserva/{id}/liberar` | Liberar reserva activa | JWT |
| POST | `/inventario/reserva/{id}/confirmar` | Confirmar reserva (descuento definitivo) | JWT |

### Ejemplo de request — Crear producto
```json
POST /inventario/productos
{
  "nombre": "Laptop Dell Inspiron 15",
  "sku": "ELEC-001",
  "descripcion": "Laptop 15 pulgadas Intel Core i5",
  "stock": 50,
  "stockMinimo": 5,
  "empresaId": 1,
  "categoriaId": 2
}
```

### Ejemplo de response
```json
{
  "id": 1,
  "nombre": "Laptop Dell Inspiron 15",
  "sku": "ELEC-001",
  "stock": 50,
  "stockMinimo": 5,
  "activo": true
}
```

### Ejemplo de request — Ajuste de inventario
```json
POST /inventario/ajuste
{
  "productoId": 1,
  "cantidad": 10,
  "tipoAjuste": "ENTRADA",
  "motivo": "Ingreso de mercadería proveedor X"
}
```

---

## 7. Persistencia de datos — JPA (patrón Repository)

La persistencia se implementa con **Spring Data JPA** sobre **MySQL**. Cada entidad del dominio tiene su propio repositorio que extiende `JpaRepository<Entidad, Long>`.

### Entidades principales

| Entidad | Tabla | Descripción |
|---|---|---|
| `Producto` | `producto` | Catálogo de productos con stock |
| `Categoria` | `categoria` | Categorías de productos |
| `AjusteInventario` | `ajuste_inventario` | Historial de ajustes manuales |
| `MovimientoInventario` | `movimiento_inventario` | Trazabilidad de movimientos |
| `ReservaStock` | `reserva_stock` | Reservas temporales de stock |

### Patrón Database per Service
MS-Inventario tiene su propia base de datos `syncro_inventario` en Aiven, **independiente** de MS-Pedidos y MS-Envíos. No existen foreign keys cruzadas entre bases de datos; la referencia a otros servicios se realiza mediante IDs lógicos.

### Ejemplo de query derivada (Spring Data JPA)
```java
// Spring genera el SQL automáticamente desde el nombre del método
List<Producto> findByEmpresaIdAndCategoriaId(Long empresaId, Long categoriaId);
// SELECT * FROM producto WHERE empresa_id = ? AND categoria_id = ?
```

---

## 8. Comunicación asíncrona — RabbitMQ

MS-Inventario actúa como **consumidor** del evento `pedido.creado` publicado por MS-Pedidos.

| Elemento | Valor |
|---|---|
| Exchange | `pedidos.exchange` (tipo Fanout) |
| Cola escuchada | `inventario.sincronizar` |
| Acción al consumir | Descuento definitivo de stock por SKU |

```java
// PedidoCreadoConsumer.java
@RabbitListener(queues = "inventario.sincronizar")
public void consumirPedidoCreado(PedidoCreadoEvent evento) {
    inventarioService.descontarStockPorEvento(evento);
}
```

---

## 9. Pruebas unitarias

### Ejecutar pruebas
```bash
./mvnw test
```

### Generar reporte de cobertura JaCoCo
```bash
./mvnw test
```
El reporte se genera automáticamente en:
```
target/site/jacoco/index.html
```

Abrirlo en el navegador:
```bash
# Windows
start target/site/jacoco/index.html
```

### Resultados de cobertura

| Paquete | Cobertura instrucciones |
|---|---|
| controller | 100% |
| consumer | 100% |
| config | 89% |
| exception | 74% |
| service | 70% |
| **Total** | **75%** ✅ |

> Supera el mínimo requerido del **60%**.

### Clases de prueba

| Clase | Qué prueba |
|---|---|
| `InventarioServiceTest` | Lógica de negocio: descuento, ajuste, reserva, CRUD |
| `InventarioControllerTest` | Endpoints REST: códigos HTTP, request/response |
| `PedidoCreadoConsumerTest` | Consumo correcto del evento RabbitMQ |
| `InventarioApplicationTests` | Carga correcta del contexto Spring |

---

## 10. Estructura del proyecto

```
src/
├── main/
│   ├── java/com/syncro/inventario/
│   │   ├── config/          # SecurityConfig, RabbitMQConfig, SwaggerConfig
│   │   ├── consumer/        # PedidoCreadoConsumer (RabbitMQ)
│   │   ├── controller/      # InventarioController
│   │   ├── dto/             # Request y Response DTOs
│   │   ├── event/           # PedidoCreadoEvent
│   │   ├── exception/       # GlobalExceptionHandler, excepciones custom
│   │   ├── model/           # Entidades JPA
│   │   ├── repository/      # Interfaces Spring Data JPA
│   │   └── service/         # InventarioService (lógica de negocio)
│   └── resources/
│       └── application.properties
└── test/
    ├── java/com/syncro/inventario/
    │   ├── controller/      # InventarioControllerTest
    │   ├── consumer/        # PedidoCreadoConsumerTest
    │   ├── service/         # InventarioServiceTest
    │   └── InventarioApplicationTests
    └── resources/
        └── application-test.properties
```

---

## 11. Repositorios del proyecto Syncro

| Repositorio | URL | Descripción |
|---|---|---|
| MS-Inventario | https://github.com/oriana02/Inventario-Syncro | Este microservicio |
| MS-Pedidos | *(URL del repo de pedidos)* | Gestión del ciclo de vida de pedidos |
| MS-Envíos | *(URL del repo de envíos)* | Gestión de despachos y trazabilidad |
| Frontend | *(URL del repo frontend)* | Interfaz Vite + React |
