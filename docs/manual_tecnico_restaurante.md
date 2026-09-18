# MANUAL TÉCNICO
## Sistema de Gestión de Restaurante

**Proyecto:** Restaurante Proyecto 1 – AyD1  
**Módulo documentado:** Backend, caja, pagos, fidelización y reportes administrativos  
**Arquitectura:** API REST + Frontends web + PostgreSQL  
**Backend:** Spring Boot 4.1.0 / Java 21  
**Base de datos:** PostgreSQL 18  
**Frontend administrativo:** Angular  
**Frontend POS:** Angular  
**Control de versiones:** Git + GitHub  
**Contenedores:** Docker / Docker Compose  
**Documentación de API:** Swagger / OpenAPI  
**Migraciones:** Flyway  
**Seguridad:** Spring Security + JWT  
**CI/CD:** GitHub Actions

---

# 1. Introducción

El presente manual técnico documenta la arquitectura, tecnologías, configuración, componentes y funcionalidades desarrolladas para el sistema de restaurante.

Durante el desarrollo se implementaron diferentes historias de usuario relacionadas principalmente con:

| Área | Funcionalidad |
|---|---|
| Caja | Apertura, recuperación y cierre de turno |
| Caja | Registro de movimientos |
| Pagos | Validación de turno abierto |
| Facturación | Cálculos, pagos y emisión de comprobantes |
| Fidelización | Redención y acreditación de puntos |
| Servicio | Calificación al mesero |
| Reportes | Ventas |
| Reportes | Platillos más y menos vendidos |
| Reportes | Rentabilidad |
| Reportes | Ocupación de mesas |
| Reportes | Desempeño de meseros |
| Reportes | Fidelización |
| Exportación | PDF y Excel |
| CI/CD | Validación de backend, PostgreSQL y frontends |

Todo el código desarrollado fue finalmente integrado en `main`.

---

# 2. Objetivo del manual

El objetivo de este documento es proporcionar información técnica suficiente para:

- comprender la estructura del sistema;
- preparar el ambiente de desarrollo;
- ejecutar el backend;
- comprender las principales tablas y servicios;
- conocer los endpoints implementados;
- realizar pruebas;
- mantener el sistema;
- comprender el flujo de integración y despliegue.

---

# 3. Arquitectura general

El sistema utiliza una arquitectura cliente-servidor.

```text
┌───────────────────────┐
│ Restaurante Admin     │
│ Angular               │
└───────────┬───────────┘
            │
            │ HTTP / REST / JSON
            │
┌───────────▼───────────┐
│                       │
│ Restaurante API       │
│ Spring Boot 4.1.0     │
│ Java 21               │
│                       │
├───────────────────────┤
│ Spring Security / JWT │
│ Services              │
│ Repositories          │
│ JPA / Hibernate       │
│ Flyway                │
└───────────┬───────────┘
            │
            │ SQL
            │
┌───────────▼───────────┐
│ PostgreSQL 18         │
│ restaurante_db        │
│ schema restaurante    │
└───────────────────────┘

            ▲
            │ HTTP / REST / JSON
            │
┌───────────┴───────────┐
│ Restaurante POS       │
│ Angular               │
└───────────────────────┘
```

> **Nota:** Para consultar los diagramas técnicos de modelado del sistema (Arquitectura, Base de Datos, Casos de Uso, Clases y Componentes en Google Drive) junto con el **Diagrama de Despliegue Cloud**, consultar la [Sección 41. Diagramas del sistema y arquitectura de despliegue](#41-diagramas-del-sistema-y-arquitectura-de-despliegue).

---

# 4. Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Java 21 | Lenguaje principal del backend |
| Spring Boot 4.1.0 | Framework backend |
| Spring Web MVC | API REST |
| Spring Data JPA | Acceso a datos |
| Hibernate | ORM |
| Spring Security | Seguridad |
| JWT | Autenticación |
| PostgreSQL 18 | Base de datos |
| Flyway | Migraciones |
| Gradle | Compilación y dependencias |
| Swagger/OpenAPI | Documentación y pruebas REST |
| OpenPDF | Generación de PDF |
| Apache POI | Generación de Excel `.xlsx` |
| Angular | Aplicaciones frontend |
| Docker | Contenedores |
| Docker Compose | Orquestación local |
| Git | Control de versiones |
| GitHub | Repositorio remoto |
| GitHub Actions | Integración y despliegue continuo |

---

# 5. Estructura principal del backend

```text
restaurante-api/
├── src/
│   ├── main/
│   │   ├── java/com/restaurante/
│   │   │   ├── application/
│   │   │   │   ├── account/
│   │   │   │   ├── billing/
│   │   │   │   ├── cash/
│   │   │   │   ├── inventory/
│   │   │   │   ├── kitchen/
│   │   │   │   ├── payment/
│   │   │   │   ├── report/
│   │   │   │   └── ...
│   │   │   ├── domain/
│   │   │   │   ├── model/
│   │   │   │   ├── repository/
│   │   │   │   └── projection/
│   │   │   ├── security/
│   │   │   └── web/
│   │   │       ├── admin/
│   │   │       ├── cash/
│   │   │       ├── dto/
│   │   │       └── operation/
│   │   └── resources/
│   │       └── db/migration/
│   └── test/
├── build.gradle
├── Dockerfile
└── docker-compose.yaml
```

---

# 6. Arquitectura por capas

## 6.1 Controllers

Los controladores reciben solicitudes HTTP, procesan parámetros y delegan la lógica de negocio a los servicios.

```text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
Repository / EntityManager
     ↓
PostgreSQL
```

Algunos controladores relevantes son:

```text
CashShiftController
CashRegisterController
PaymentController
InvoiceController
ServiceRatingController
SalesReportAdminController
DishSalesReportAdminController
ProfitabilityReportAdminController
TableOccupancyReportAdminController
WaiterPerformanceReportAdminController
LoyaltyReportAdminController
ReportExportAdminController
```

---

# 7. Base de datos

La aplicación utiliza:

```text
Motor: PostgreSQL 18
Base: restaurante_db
Schema principal: restaurante
```

Para acceder:

```bash
docker compose exec postgres psql -U restaurante_user -d restaurante_db
```

Luego:

```sql
SET search_path TO restaurante, public;
```

---

# 8. Migraciones

Las migraciones se administran mediante Flyway.

Ubicación:

```text
restaurante-api/src/main/resources/db/migration/
```

Durante la integración continua se comprobó que las migraciones pudieran ejecutarse sobre una instancia limpia de PostgreSQL 18.

El smoke test confirmó:

```text
PASS: all 9 migrations applied to fresh PostgreSQL 18
```

Además, el proyecto incorporó la migración:

```text
V9__add_tax_name_to_restaurant_configuration.sql
```

---

# 9. HU-B-019 – Apertura de turno de caja

## Objetivo

Permitir que un cajero abra su turno indicando la caja y el monto inicial disponible.

Endpoint:

```http
POST /api/v1/caja/turnos/abrir
```

El turno almacena:

```text
caja
cajero
estado
monto inicial
fecha de apertura
observaciones
```

Estado inicial:

```text
ABIERTA
```

Entidad principal:

```java
CashShift
```

Tabla:

```text
turnos_caja
```

---

# 10. Consulta de cajas disponibles

Durante la integración del frontend se detectó que el POS no debía hardcodear `cajaId`.

Se agregó:

```http
GET /api/v1/caja/cajas
```

Ejemplo:

```json
[
  {
    "cajaId": 1,
    "codigo": "CAJA-01",
    "nombre": "Caja principal",
    "ubicacion": "Area de cobro",
    "activa": true,
    "disponible": false
  }
]
```

Esto permite que el POS determine dinámicamente las cajas registradas.

---

# 11. HU-B-020 – Movimientos de caja

Se implementó el registro de transacciones relacionadas con un turno.

Servicio:

```text
CashTransactionService
```

Entidad:

```text
CashTransaction
```

Tipos manejados:

```text
APERTURA
VENTA
PROPINA
REDENCION_PUNTOS
CIERRE
```

Se evita duplicar transacciones asociadas a un mismo pago.

También se implementó registro independiente para:

```text
venta
propina
redención de puntos
```

---

# 12. HU-B-021 – Cierre y cuadre de caja

Endpoint:

```http
PUT /api/v1/caja/turnos/{shiftId}/cerrar
```

El backend calcula:

```text
efectivo esperado
efectivo real
diferencia
```

Fórmula principal:

```text
efectivo esperado =
monto inicial
+ pagos cuyo método afecta efectivo
```

Ejemplo real utilizado:

```text
Monto inicial:        Q500.00
Ventas efectivo:      Q727.90
--------------------------------
Efectivo esperado:   Q1227.90
```

No se agregan pagos correspondientes a métodos que:

```text
afecta_efectivo = false
```

---

# 13. Recuperar turno actual

Durante la integración con el POS se agregó una mejora importante para permitir que el cajero recargue la aplicación sin perder el turno.

Endpoint:

```http
GET /api/v1/caja/turnos/actual
```

Ejemplo:

```json
{
  "id": 3,
  "cajaId": 1,
  "codigoCaja": "CAJA-01",
  "nombreCaja": "Caja principal",
  "estado": "ABIERTA",
  "montoInicialEfectivo": 500,
  "efectivoEsperado": 1227.9,
  "abiertaEn": "2026-09-15T06:52:23.517683Z"
}
```

Para obtener el efectivo esperado en tiempo real se utiliza:

```text
vw_cuadre_turnos_caja
```

y específicamente:

```text
efectivo_esperado_calculado
```

Esto evita utilizar:

```text
efectivo_esperado_cierre
```

porque dicho valor se establece únicamente cuando se cierra el turno.

---

# 14. HU-B-022 – Bloqueo de cobro sin turno abierto

Se implementó una validación para evitar que un cajero realice operaciones de cobro si no posee turno abierto.

Método principal:

```java
requireOpenShift(authentication)
```

Si no existe turno:

```text
open_cash_shift_required
```

Flujo:

```text
LOGIN CASHIER
     ↓
¿Existe turno abierto?
     ↓
  NO ──────> abrir turno
     ↓
    SÍ
     ↓
permitir cobro
```

---

# 15. HU-B-023 – Cálculo de cuenta

Se implementó el cálculo de:

```text
subtotal
descuentos
impuesto
propina
total
```

Endpoints:

```http
GET /api/v1/caja/cuentas/{cuentaId}/calculo
```

y para división:

```http
GET /api/v1/caja/cuentas/{cuentaId}/subcuentas/{subcuentaId}/calculo
```

La configuración se obtiene de la configuración vigente del restaurante.

---

# 16. Facturación

El sistema almacena snapshots de la operación para garantizar trazabilidad.

Tablas principales:

```text
facturas
factura_detalles
pagos
metodos_pago
```

Un `factura_detalle` conserva:

```text
nombre_snapshot
cantidad
precio_unitario_snapshot
total_modificadores_snapshot
costo_unitario_snapshot
subtotal_linea
```

Esto permite que los reportes históricos utilicen los valores realmente aplicados en el momento de la venta.

---

# 17. HU-B-025 – Redención de puntos

Se implementó la posibilidad de aplicar puntos acumulados como descuento.

Validaciones principales:

```text
cliente registrado
saldo suficiente
cantidad de puntos válida
configuración de fidelización vigente
factura asociada
```

Se genera un movimiento:

```text
tipo = REDENCION
```

y se registra el descuento en la factura.

---

# 18. HU-B-026 – Acreditación de puntos

Después de una compra válida se acreditan automáticamente los puntos correspondientes.

Ejemplo probado:

```text
Factura: A-4
Total: Q210.00
Puntos otorgados: 210
```

Movimiento:

```text
OTORGAMIENTO
```

Ejemplo almacenado:

```text
saldo_anterior  = 0
cantidad_puntos = 210
saldo_resultante = 210
```

También incrementa:

```text
total_visitas
ultima_visita_en
```

del cliente correspondiente.

---

# 19. HU-B-027 – Emitir comprobante

Endpoints:

```http
GET /api/v1/caja/facturas/{facturaId}
```

y:

```http
GET /api/v1/caja/facturas/{facturaId}/pdf
```

El JSON contiene:

```text
número de documento
serie
correlativo
cuenta
restaurante
cliente
detalles
subtotal
descuentos
impuesto
propina
total
puntos
pagos
fecha de emisión
```

Ejemplo:

```text
A-4
```

El correlativo es generado automáticamente.

---

# 20. Exportación del comprobante a PDF

Se utilizó:

```gradle
implementation 'com.github.librepdf:openpdf:2.0.3'
```

Servicio:

```text
InvoicePdfService
```

El PDF contiene:

```text
Restaurante
Número del comprobante
Cuenta
Platillos
Cantidad
Precio
Subtotal
Descuentos
Impuesto
Propina
Total
Métodos de pago
Puntos obtenidos
```

El backend devuelve:

```http
Content-Type: application/pdf
```

y:

```http
Content-Disposition: attachment; filename="A-4.pdf"
```

---

# 21. HU-B-028 – Historial de facturas

Permite consultar comprobantes emitidos utilizando filtros.

Filtro por:

```text
fecha
mesa
mesero
```

La aplicación utiliza la zona horaria:

```text
America/Guatemala
```

para interpretar correctamente los períodos.

---

# 22. HU-B-029 – Calificación del servicio

Después del cierre de una cuenta el cliente puede evaluar el servicio del mesero.

Endpoint:

```http
POST /api/v1/caja/facturas/{facturaId}/calificacion
```

Ejemplo:

```json
{
  "calificacion": 5,
  "comentario": "Excelente atencion del mesero"
}
```

La calificación se almacena en:

```text
calificaciones_servicio
```

con:

```text
factura_id
cuenta_id
cliente_id
mesero_id
calificacion
comentario
creada_en
```

Rango permitido:

```text
1 <= calificacion <= 5
```

Una función y trigger de PostgreSQL validan que factura, cuenta, cliente y mesero correspondan entre sí.

---

# 23. HU-045 – Reporte de ventas

Endpoint administrativo:

```http
GET /api/v1/admin/reportes/ventas
```

Parámetros:

```text
fechaInicio
fechaFin
```

El reporte muestra:

```text
cantidad de ventas
monto total
ventas por categoría
ventas por mesero
```

Vista base:

```text
vw_ventas_detalladas
```

Esta vista utiliza snapshots de facturación.

---

# 24. HU-046 – Platillos más y menos vendidos

Permite obtener los platillos ordenados por cantidad vendida.

El reporte contempla también platillos con:

```text
cantidadVendida = 0
```

Esto es importante para el escenario de platillos sin ventas.

Se utilizan únicamente ventas del período solicitado.

---

# 25. HU-047 – Rentabilidad por platillo

Se implementaron dos modalidades.

## Rentabilidad actual

Utiliza:

```text
precio de venta vigente
receta vigente
costo actual de los insumos
```

Vista:

```text
vw_costos_platillo
```

Fórmula:

```text
ganancia = precio_venta - costo_produccion
```

Margen:

```text
margen =
(ganancia / precio_venta) * 100
```

Ejemplo:

```text
Precio venta: Q100.00
Costo:        Q2.325
Ganancia:     Q97.675
Margen:       97.675 %
```

## Rentabilidad histórica

Utiliza los snapshots almacenados en:

```text
factura_detalles
```

Por lo tanto, no sustituye precios históricos por valores actuales.

---

# 26. HU-049 – Ocupación de mesas

Reporte:

```http
GET /api/v1/admin/reportes/ocupacion-mesas
```

Utiliza:

```text
historial_estados_mesa
```

Se consideran únicamente transiciones reales hacia:

```text
OCUPADA
```

Las reservas que nunca se transformaron en ocupación no se contabilizan.

El resultado se agrupa por:

```text
fecha
hora
cantidad de mesas ocupadas
```

---

# 27. HU-050 – Desempeño de meseros

Endpoint:

```http
GET /api/v1/admin/reportes/desempeno-meseros
```

El reporte incluye:

```text
mesero
cantidad de ventas
monto vendido
cantidad de calificaciones
calificación promedio
```

Ejemplo validado:

```text
Pedro Mesero
Ventas: 4
Monto: Q837.90
Calificaciones: 1
Promedio: 5
```

Para meseros sin evaluación:

```text
calificacionPromedio = null
```

en lugar de asumir `0`.

---

# 28. HU-051 – Reporte de fidelización

Endpoint:

```http
GET /api/v1/admin/reportes/fidelizacion
```

Devuelve independientemente:

```text
puntos otorgados
puntos redimidos
clientes frecuentes
```

Ejemplo probado:

```text
Puntos otorgados: 417
Puntos redimidos: 100
```

Los clientes se ordenan según su cantidad de visitas.

---

# 29. HU-052 – Exportación de reportes

Se permitió exportar los reportes administrativos a:

```text
PDF
Excel (.xlsx)
```

Tipos soportados:

```text
VENTAS
PLATILLOS_VENDIDOS
RENTABILIDAD_ACTUAL
RENTABILIDAD_HISTORICA
OCUPACION_MESAS
DESEMPENO_MESEROS
FIDELIZACION
```

Servicio principal:

```text
ReportExportService
```

Servicios auxiliares:

```text
ReportPdfExportService
ReportExcelExportService
```

---

# 30. Exportación PDF

Biblioteca:

```gradle
implementation 'com.github.librepdf:openpdf:2.0.3'
```

Contenido:

```text
título del reporte
período
criterios
encabezados
resultados
```

---

# 31. Exportación Excel

Biblioteca:

```gradle
implementation 'org.apache.poi:poi-ooxml:5.4.1'
```

El archivo se genera en formato:

```text
.xlsx
```

con:

```text
título
filtros
encabezados
filas
columnas autoajustadas
```

MIME:

```text
application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
```

---

# 32. Seguridad

La API utiliza:

```text
Spring Security
JWT Bearer Token
```

Roles principales:

```text
ADMIN
CASHIER
WAITER
KITCHEN
```

Ejemplos:

| Operación | Rol |
|---|---|
| Reportes | ADMIN |
| Apertura/cierre caja | CASHIER |
| Facturación | CASHIER |
| Cobros | CASHIER |
| Calificación | flujo de caja |
| Atención mesa | WAITER |
| Cocina | KITCHEN |

Swagger utiliza:

```http
Authorization: Bearer <JWT>
```

---

# 33. Manejo de errores

Se utiliza un manejador central:

```text
ApiExceptionHandler
```

Formato general:

```json
{
  "detail": "Descripcion",
  "instance": "/api/v1/...",
  "status": 400,
  "title": "Titulo",
  "type": "https://api.ssg.local/problems/...",
  "code": "codigo_error"
}
```

Algunos códigos utilizados:

```text
open_cash_shift_required
invalid_report_period
invoice_not_found
report_export_failed
```

---

# 34. Pruebas unitarias

Se trabajó con:

```text
JUnit 5
Mockito
Spring Boot Test
```

Entre los tests agregados:

```text
CashShiftPaymentGuardTest
CashTransactionServiceTest
```

---

# 35. Comandos de validación

Compilar:

```bash
./gradlew compileJava
```

Ejecutar pruebas y verificaciones:

```bash
./gradlew check
```

Limpieza completa:

```bash
./gradlew clean check
```

Resultado esperado:

```text
BUILD SUCCESSFUL
```

---

# 36. Swagger

Ruta habitual:

```text
http://localhost:8090/api/v1/swagger-ui/index.html
```

Procedimiento:

```text
Login
↓
Obtener JWT
↓
Authorize
↓
Bearer Token
↓
Ejecutar endpoints
```

---

# 37. Docker

PostgreSQL se ejecuta dentro de Docker.

```bash
docker compose up -d
```

Consultar estado:

```bash
docker compose ps
```

Acceder a PostgreSQL:

```bash
docker compose exec postgres psql -U restaurante_user -d restaurante_db
```

---

# 38. CI/CD con GitHub Actions

Durante la integración final se validaron automáticamente:

```text
Backend + PostgreSQL
Frontend restaurante-admin
Frontend restaurante-pos
Pruebas de automatización
Estado global del CI
```

El backend se prueba sobre:

```text
PostgreSQL 18
```

y mediante:

```text
scripts/ci/backend_smoke.py
```

---

# 39. Smoke test del backend

El smoke test comprueba:

```text
arranque de producción
puerto personalizado
readiness
migraciones
seguridad
OpenAPI
autenticación
reinicio del contenedor
```

Durante la integración se detectó que:

```text
/v3/api-docs
```

podía tardar más debido al tamaño actual de la API.

Se agregó timeout parametrizable y un timeout mayor específicamente para OpenAPI.

También se agregó logging:

```python
print(f"HTTP {method} {path}", flush=True)
```

para identificar rápidamente la solicitud problemática.

---

# 40. Resultado final del CI

Antes de integrar en `main` se obtuvo:

```text
Backend y PostgreSQL        SUCCESS
CI aprobada                 SUCCESS
Frontend restaurante-admin SUCCESS
Frontend restaurante-pos   SUCCESS
Pruebas de automatización  SUCCESS
```

El despliegue de producción aparecía como:

```text
Skipped
```

durante el Pull Request, comportamiento esperado porque el despliegue se ejecuta posteriormente bajo las condiciones configuradas para `main`.

---

# 41. Diagramas del sistema y arquitectura de despliegue

## 41.1 Enlaces a diagramas de modelado del sistema (Google Drive)

Todos los diagramas técnicos del sistema se encuentran en la carpeta pública compartida de Google Drive:

-  **Carpeta de Google Drive:**  
  [https://drive.google.com/drive/folders/1Kptm78w_FmziWxM4fmkc86VHl3yTzNl_?usp=sharing](https://drive.google.com/drive/folders/1Kptm78w_FmziWxM4fmkc86VHl3yTzNl_?usp=sharing)

A continuación se detallan los enlaces individuales directos a cada documento de modelado:

| Diagrama | Descripción del Modelado | Formato | Enlace de Acceso |
|---|---|---|---|
| **Diagrama de Arquitectura** | Modela la arquitectura lógica general, distribución por capas y desacoplamiento entre el backend RESTful Spring Boot y los frontends web. | PDF | [Ver Diagrama de Arquitectura](https://drive.google.com/file/d/1Ii9fuZ4n3YlCdZv3Ts-HUYTsi-n5YNQ4/view?usp=sharing) |
| **Diagrama de Base de Datos** | Modelo entidad-relación (ERD) relacional con la totalidad de tablas de los esquemas `restaurante` y `public`, llaves primarias, llaves foráneas y vistas. | PDF | [Ver Diagrama de Base de Datos](https://drive.google.com/file/d/10AMsjRMTTpbMOZMnytFShwjdoonpMZOR/view?usp=sharing) |
| **Diagrama de Casos de Uso** | Casos de uso e interacciones de los actores del sistema (`ADMIN`, `WAITER`, `KITCHEN`, `CASHIER`, `CLIENT`) sobre los módulos de menú, mesas, comandas, facturación y caja. | PDF | [Ver Diagrama de Casos de Uso](https://drive.google.com/file/d/1NyifM7Hu8ANyIE8CQcqIsJ1jLSdj-xFK/view?usp=sharing) |
| **Diagrama de Clases** | Modela las entidades de dominio JPA del backend (`Supply`, `Dish`, `RecipeVersion`, `Account`, `Comanda`, `CashShift`, `UserAccount`, etc.) y sus relaciones estructurales. | PDF | [Ver Diagrama de Clases](https://drive.google.com/file/d/1aE1uh1nD1i1jdAn1Xj6cuiLvK5Bs-HAJ/view?usp=sharing) |
| **Diagrama de Componentes** | Representa los paquetes de software, servicios de aplicación, controladores web, DTOs y componentes visuales de Angular interconectados vía REST. | PDF | [Ver Diagrama de Componentes](https://drive.google.com/file/d/1vewt_0XVdGg_YVI-Bei0bhGchu0D1b1l/view?usp=sharing) |

---

## 41.2 Diagrama de despliegue en producción (UML / Mermaid)

El sistema cuenta con una arquitectura de despliegue distribuida en la nube, desacoplada en tres capas principales: capa de presentación (Edge/CDN con Cloudflare Pages), capa de lógica de negocio y servicios (PaaS con Render) y capa de persistencia administrada (DBaaS con Neon Serverless PostgreSQL), integrando además servicios externos transaccionales (SaaS con Brevo) y un pipeline automatizado de CI/CD (GitHub Actions).

```mermaid
flowchart TB
    subgraph CLIENTES ["Dispositivos de Usuario (Clientes Web)"]
        ADMIN_DEV["Dispositivo Administrador<br/>(PC / Laptop - Navegador Web)"]
        POS_DEV["Terminales Operativas POS<br/>(Tablets Mesero / PC Caja / Pantalla Cocina)"]
    end

    subgraph CDN ["Red Edge / CDN: Cloudflare Pages"]
        CF_ADMIN["restaurante-admin-ayd1.pages.dev<br/>• SPA Angular 21 (HTML5, JS, CSS)<br/>• PrimeNG + Tailwind CSS<br/>• Routing SPA & i18n Transloco"]
        CF_POS["restaurante-pos-ayd1.pages.dev<br/>• SPA Angular 21 (HTML5, JS, CSS)<br/>• PrimeNG + Tailwind CSS<br/>• Routing SPA & i18n Transloco"]
    end

    subgraph PAAS ["PaaS Cloud: Render (Web Service)"]
        subgraph DOCKER ["Contenedor Docker (Linux x86_64 / Temurin JRE 21)"]
            API["restaurante-api<br/>restaurant-proyecto1-ayd1.onrender.com<br/>• Java 21 LTS / Spring Boot 4.1.0<br/>• Servidor Apache Tomcat Embebido (:PORT / :8080)<br/>• Context Path: /api/v1<br/>• Spring Security + JWT Stateless Filter<br/>• Pool HikariCP (Máx: 5 conexiones)<br/>• Swagger UI (/swagger-ui.html)<br/>• Health Readiness (/actuator/health/readiness)"]
        end
    end

    subgraph DAAS ["DBaaS Cloud: Neon Serverless PostgreSQL"]
        subgraph NEON_DB ["PostgreSQL 18 (AWS Ohio us-east-2)"]
            SCHEMA_RES["Esquema: restaurante<br/>• Cuentas, comandas, mesas, pagos<br/>• Cajas, turnos, movimientos<br/>• Platillos, recetas, insumos, mermas<br/>• Vistas optimizadas de reportes"]
            SCHEMA_PUB["Esquema: public<br/>• app_users, roles, perfiles"]
            FLYWAY["Migraciones Flyway V1–V9<br/>• Validación automática en arranque"]
        end
    end

    subgraph SAAS ["SaaS Externo: Brevo"]
        BREVO["API Transaccional Brevo<br/>api.brevo.com<br/>• Códigos OTP de 2FA<br/>• Recuperación de contraseña"]
    end

    subgraph CICD ["Plataforma de CI/CD: GitHub Actions"]
        RUNNER["Runner: ubuntu-24.04<br/>• Job Automation (Python smoke tests)<br/>• Job Backend (Docker build & smoke)<br/>• Job Frontend (Matrix: admin & pos)<br/>• Job Production (Deploy a Render & Pages)"]
    end

    %% Conexiones Clientes a Cloudflare Pages
    ADMIN_DEV -->|HTTPS / TLS 1.3<br/>Descarga de assets estáticos| CF_ADMIN
    POS_DEV -->|HTTPS / TLS 1.3<br/>Descarga de assets estáticos| CF_POS

    %% Conexiones Frontends a Backend API
    CF_ADMIN -->|HTTPS / TLS 1.3 - REST JSON<br/>Bearer JWT / CORS autorizada| API
    CF_POS -->|HTTPS / TLS 1.3 - REST JSON<br/>Bearer JWT / CORS autorizada| API

    %% Conexiones Backend a Base de Datos y Servicios
    API -->|JDBC sobre TLS (sslmode=verify-full)<br/>Puerto 5432 - TCP Cifrado| NEON_DB
    API -->|HTTPS REST API (POST /v3/smtp/email)<br/>Puerto 443 - TLS 1.3| BREVO

    %% Flujo de Despliegue CI/CD
    RUNNER -.->|Direct Upload via Wrangler CLI<br/>dist/restaurante-admin/browser| CF_ADMIN
    RUNNER -.->|Direct Upload via Wrangler CLI<br/>dist/restaurante-pos/browser| CF_POS
    RUNNER -.->|Deploy Trigger via Render API<br/>Docker build en main aprobada| API
```

---

## 41.3 Diagrama de despliegue en formato esquemático (Texto / ASCII)

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   DISPOSITIVOS DE USUARIO (CLIENTES)                                   │
│                                                                                                        │
│   ┌──────────────────────────────────────────────┐    ┌────────────────────────────────────────────┐   │
│   │ Dispositivo Administrador                    │    │ Terminales Operativas (POS / Cocina / Caja)│   │
│   │ Navegador Web (Chrome, Firefox, Edge)        │    │ Tablets Meseros / PC Caja / Pantalla Cocina│   │
│   └──────────────────────┬───────────────────────┘    └─────────────────────┬──────────────────────┘   │
└──────────────────────────┼──────────────────────────────────────────────────┼──────────────────────────┘
                           │ HTTPS (TLS 1.3)                                  │ HTTPS (TLS 1.3)
                           ▼                                                  ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                              EDGE NETWORK / CDN: CLOUDFLARE PAGES                                      │
│                                                                                                        │
│   ┌──────────────────────────────────────────────┐    ┌────────────────────────────────────────────┐   │
│   │ restaurante-admin-ayd1.pages.dev             │    │ restaurante-pos-ayd1.pages.dev             │   │
│   │ • SPA Angular 21 (HTML5, JS bundles, CSS)    │    │ • SPA Angular 21 (HTML5, JS bundles, CSS)  │   │
│   │ • Panel Administrativo                       │    │ • Terminal Punto de Venta Operativo        │   │
│   │ • Despliegue: Direct Upload (Wrangler CLI)   │    │ • Despliegue: Direct Upload (Wrangler CLI) │   │
│   └──────────────────────┬───────────────────────┘    └─────────────────────┬──────────────────────┘   │
└──────────────────────────┼──────────────────────────────────────────────────┼──────────────────────────┘
                           │                                                  │
                           │ HTTPS REST / JSON (CORS + Authorization: Bearer) │
                           └──────────────────────────┬───────────────────────┘
                                                      │
                                                      ▼
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    PAAS CLOUD: RENDER (WEB SERVICE)                                    │
│                                                                                                        │
│   ┌────────────────────────────────────────────────────────────────────────────────────────────────┐   │
│   │ Host: restaurant-proyecto1-ayd1.onrender.com                                                   │   │
│   │ Contenedor Docker (Eclipse Temurin JRE 21 LTS / Linux x86_64)                                  │   │
│   │                                                                                                │   │
│   │ ┌────────────────────────────────────────────────────────────────────────────────────────────┐ │   │
│   │ │ Backend RESTful API: restaurante-api                                                        │ │   │
│   │ │ • Framework: Spring Boot 4.1.0                                                             │ │   │
│   │ │ • Servidor Embebido: Apache Tomcat (Puerto interno 8080 mapeado a PORT)                    │ │   │
│   │ │ • Context Path: /api/v1                                                                    │ │   │
│   │ │ • Filtro de Seguridad: Spring Security + JWT Stateless Authentication                      │ │   │
│   │ │ • Pool de Conexiones: HikariCP (Máximo 5 conexiones activas)                               │ │   │
│   │ │ • Documentación y Monitoreo: Swagger UI (/swagger-ui.html) y Actuator (/health/readiness)  │ │   │
│   │ └──────────────────────────────────────┬───────────────────────────────┬─────────────────────┘ │   │
│   └────────────────────────────────────────┼───────────────────────────────┼───────────────────────┘   │
└────────────────────────────────────────────┼───────────────────────────────┼───────────────────────────┘
                                             │                               │
                 JDBC sobre TLS              │                               │ HTTPS (TLS 1.3)
                 Puerto 5432                 │                               │ Puerto 443
                 sslmode=verify-full         │                               │ POST /v3/smtp/email
                                             ▼                               ▼
┌──────────────────────────────────────────────────────────┐    ┌────────────────────────────────────────┐
│               DBAAS: NEON SERVERLESS                     │    │              SAAS EXTERNO              │
│                                                          │    │                                        │
│   Host: ep-*.us-east-2.aws.neon.tech (AWS Ohio)          │    │   Brevo Transactional Email Platform   │
│   Motor: PostgreSQL 18                                   │    │   Host: api.brevo.com                  │
│   Base de Datos: neondb                                  │    │   • Envío de OTP para 2FA              │
│   • Esquema: restaurante (cuentas, comandas, facturas...)│    │   • Enlaces de recuperación            │
│   • Esquema: public (app_users, roles)                   │    │   • Remitente verificado               │
│   • Migraciones versionadas Flyway (V1 a V9)             │    │                                        │
└──────────────────────────────────────────────────────────┘    └────────────────────────────────────────┘

                               ▲
                               │ Publicación automatizada en merge a 'main'
┌──────────────────────────────┴─────────────────────────────────────────────────────────────────────────┐
│                                 INTEGRACIÓN Y DESPLIEGUE CONTINUO (CI/CD)                              │
│                                                                                                        │
│   Plataforma: GitHub Actions (.github/workflows/ci.yml)                                                │
│   Runner: ubuntu-24.04                                                                                 │
│   • Jobs: automation (Python), backend (Docker tests + smoke), frontend (matrix: admin & pos)          │
│   • CD Job (production):                                                                               │
│     - Despliegue de Frontends: Direct Upload a Cloudflare Pages mediante Wrangler CLI                  │
│     - Despliegue de Backend: Trigger de despliegue y rebuild de imagen Docker en Render                │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 41.4 Diagrama de despliegue en entorno de desarrollo local

En el entorno de desarrollo local, los servicios se ejecutan de manera aislada en la estación de trabajo del desarrollador para pruebas y depuración rápidas sin impactar los servicios cloud:

```text
┌────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                    ENTORNO DE DESARROLLO LOCAL (HOST)                                  │
│                                                                                                        │
│   ┌───────────────────────────────┐    ┌──────────────────────────────┐    ┌───────────────────────┐   │
│   │ Frontend Admin (Angular 21)   │    │ Frontend POS (Angular 21)    │    │ Backend Spring Boot   │   │
│   │ http://localhost:4200         │    │ http://localhost:4201        │    │ http://localhost:8090 │   │
│   │ Proxy /api -> :8090           │    │ Proxy /api -> :8090          │    │ Perfil: dev           │   │
│   └───────────────┬───────────────┘    └──────────────┬───────────────┘    │ Contexto: /api/v1     │   │
│                   │                                   │                    └───────────┬───────────┘   │
│                   └─────────────────┬─────────────────┘                                │               │
│                                     │ HTTP REST Local                                  │ JDBC Local    │
│                                     └─────────────────────────────────────────────────►│ Puerto 5437   │
│                                                                                        ▼               │
│                                                        ┌───────────────────────────────────────────┐   │
│                                                        │ Contenedor Docker: postgres:18            │   │
│                                                        │ Host: localhost:5437 -> Interno: 5432     │   │
│                                                        │ Base: restaurante_db (Flyway V1–V9)       │   │
│                                                        │ Volumen persistente: pg_restaurante_data  │   │
│                                                        └───────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 41.5 Matriz de nodos, componentes y conectividad

| Nodo / Capa | Proveedor / Host | Tecnología / Componente | Dirección / Endpoint | Puerto y Protocolo | Responsabilidad |
|---|---|---|---|---|---|
| **Clientes** | Dispositivos finales | Navegador Web (Chrome, Edge, Firefox) | N/A | Salida TCP 443 (HTTPS) | Renderizado visual e interacción de usuarios administradores y operativos. |
| **Frontend Admin** | Cloudflare Pages | Angular 21, PrimeNG, TailwindCSS | `https://restaurante-admin-ayd1.pages.dev` | 443 (HTTPS / TLS 1.3) | SPA de gestión administrativa: menú, inventario, reportes, usuarios y configuración. |
| **Frontend POS** | Cloudflare Pages | Angular 21, PrimeNG, TailwindCSS | `https://restaurante-pos-ayd1.pages.dev` | 443 (HTTPS / TLS 1.3) | SPA operativa: control de mesas, comandas en tiempo real, facturación y caja. |
| **Backend API** | Render (Web Service Free) | Docker, Java 21, Spring Boot 4.1.0, Tomcat | `https://restaurant-proyecto1-ayd1.onrender.com/api/v1` | 443 (HTTPS / TLS 1.3) | Reglas de negocio, seguridad JWT, endpoints REST, Swagger UI y endpoints Actuator. |
| **Base de Datos** | Neon Serverless | PostgreSQL 18 (AWS Ohio `us-east-2`) | `ep-*.us-east-2.aws.neon.tech` | 5432 (JDBC / TLS `verify-full`) | Almacenamiento relacional transaccional, esquemas `restaurante` y `public`. |
| **Correo / OTP** | Brevo Platform | HTTPS REST API | `https://api.brevo.com/v3/smtp/email` | 443 (HTTPS / TLS 1.3) | Servicio transaccional para envío de OTP de doble factor y recuperación de contraseña. |
| **Pipeline CI/CD**| GitHub Actions | Runner `ubuntu-24.04`, Docker, Wrangler CLI | Repositorio GitHub | Salida HTTPS | Automatización de pruebas, compilación de artefactos y publicación coordinada. |

---

## 41.6 Políticas de seguridad y aislamiento de red en el despliegue

1. **Aislamiento de la Base de Datos:**
   - La base de datos Neon PostgreSQL **únicamente acepta conexiones del backend** mediante JDBC sobre TLS (`sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory`).
   - Los frontends no poseen conexión ni credenciales directas a la base de datos.
2. **Control de Origen Cruzado (CORS):**
   - El backend restringe las llamadas HTTP permitiendo exclusivamente los dos dominios autorizados de Cloudflare Pages:
     ```text
     https://restaurante-admin-ayd1.pages.dev,https://restaurante-pos-ayd1.pages.dev
     ```
3. **Autenticación y Autorización Stateless:**
   - Cada solicitud hacia endpoints protegidos viaja con el encabezado `Authorization: Bearer <JWT>`, verificado criptográficamente por la clave secreta `SECRET_KEY_JWT` sin almacenar estado de sesión en el servidor.
4. **Protección de Secretos y Credenciales:**
   - Ninguna credencial de producción (Neon, Brevo, JWT o API keys de Render/Cloudflare) se versiona en el repositorio Git.
   - En producción, los secretos son inyectados mediante GitHub Repository Secrets y variables de entorno seguras de Render.

---

# 42. Estrategia Git utilizada

Flujo normal:

```text
feature/*
     ↓
develop
     ↓
release/integrar-develop-main
     ↓
main
```

Para cada HU:

```bash
git switch develop
git pull origin develop

git switch -c feature/nombre-hu

# desarrollo

git add .
git commit -m "feat: descripcion"
git push -u origin feature/nombre-hu
```

Después:

```text
Pull Request
feature/* -> develop
```

---

# 43. Integración final hacia main

Debido a diferencias históricas entre `develop` y `main`, se creó:

```text
release/integrar-develop-main
```

desde `develop`.

Posteriormente se integró `main` en esa rama:

```bash
git merge origin/main
```

Se conservaron las implementaciones funcionales de `develop` y los componentes válidos de CI/CD de `main`.

Después de solucionar los conflictos:

```bash
./gradlew clean check
```

produjo:

```text
BUILD SUCCESSFUL
```

Finalmente se creó el PR:

```text
release/integrar-develop-main
              ↓
             main
```

y todos los checks fueron exitosos.

---

# 44. Estado final de main

La actualización final de `main` reincorporó el sistema completo, incluyendo documentación, frontends, servicios, controladores, DTOs, reportes, caja y facturación.

La integración incorporó cientos de archivos y decenas de miles de líneas respecto al estado anterior de `main`.

---

# 45. Componentes más importantes implementados

| Componente | Responsabilidad |
|---|---|
| `CashShiftService` | Apertura, recuperación y cierre de turno |
| `CashTransactionService` | Movimientos de caja |
| `BillingCalculationService` | Cálculo de cuentas |
| `PaymentService` | Registro de pagos |
| `LoyaltyService` | Fidelización |
| `InvoiceService` | Consulta de comprobantes |
| `InvoicePdfService` | PDF de factura |
| `ServiceRatingService` | Calificación del servicio |
| `SalesReportService` | Reporte de ventas |
| `DishSalesReportService` | Más/menos vendidos |
| `ProfitabilityReportService` | Rentabilidad |
| `TableOccupancyReportService` | Ocupación |
| `WaiterPerformanceReportService` | Desempeño |
| `LoyaltyReportService` | Fidelización |
| `ReportExportService` | Coordinación de exportaciones |
| `ReportPdfExportService` | Exportar reportes PDF |
| `ReportExcelExportService` | Exportar reportes Excel |

---

# 46. Vistas relevantes de PostgreSQL

| Vista | Uso |
|---|---|
| `vw_cuadre_turnos_caja` | Cuadre y efectivo esperado |
| `vw_ventas_detalladas` | Reportes de ventas |
| `vw_costos_platillo` | Rentabilidad actual |
| `vw_desempeno_meseros` | Información general de desempeño |
| `vw_reporte_fidelizacion` | Fidelización |
| `vw_ocupacion_mesas_actual` | Estado actual de mesas |
| `vw_comandas_cocina` | Cocina |
| `vw_stock_bajo` | Alertas de inventario |
| `vw_reporte_mermas` | Reporte de mermas |

---

# 47. Tablas principales involucradas

| Tabla | Propósito |
|---|---|
| `cajas` | Cajas registradas |
| `turnos_caja` | Turnos |
| `transacciones_caja` | Movimientos |
| `cuentas` | Cuenta de mesa |
| `subcuentas` | División de cuenta |
| `comandas` | Órdenes |
| `comanda_detalles` | Platillos |
| `facturas` | Comprobantes |
| `factura_detalles` | Detalle histórico |
| `pagos` | Pagos |
| `metodos_pago` | Formas de pago |
| `clientes` | Clientes |
| `movimientos_puntos` | Fidelización |
| `calificaciones_servicio` | Evaluaciones |
| `platillos` | Menú |
| `receta_versiones` | Versiones de receta |
| `receta_detalles` | Ingredientes |
| `insumos` | Inventario |
| `historial_estados_mesa` | Historial de ocupación |

---

# 48. Consideraciones de mantenimiento

Cuando se agregue una nueva funcionalidad de negocio, debe mantenerse la separación Controller → Service → Repository y utilizar DTOs para exponer información.

Los cálculos históricos deben continuar utilizando snapshots y no valores actuales.

Las modificaciones de estructura de base de datos deben realizarse mediante nuevas migraciones Flyway y nunca modificando una migración ya ejecutada.

Antes de integrar una rama:

```bash
./gradlew clean check
```

Y antes de integrar en `main` deben pasar todos los checks del CI.

---

# 49. Conclusión técnica

El sistema terminó con una arquitectura capaz de gestionar el ciclo completo de operación de un restaurante:

```text
Mesa
 ↓
Cuenta
 ↓
Comanda
 ↓
Preparación
 ↓
Entrega
 ↓
Solicitud de cobro
 ↓
Cálculo
 ↓
Pago
 ↓
Factura
 ↓
Fidelización
 ↓
Calificación
 ↓
Reportes
 ↓
PDF / Excel
```

También quedó cubierta la operación de caja:

```text
Seleccionar caja
      ↓
Abrir turno
      ↓
Registrar operaciones
      ↓
Recuperar turno después de recargar
      ↓
Calcular efectivo esperado
      ↓
Registrar efectivo real
      ↓
Calcular diferencia
      ↓
Cerrar turno
```

Y la capa administrativa:

```text
Ventas
Platillos vendidos
Rentabilidad
Ocupación
Desempeño de meseros
Fidelización
      ↓
PDF / Excel
```

Con la integración final, `main` contiene la implementación funcional desarrollada en `develop`, junto con la configuración de CI/CD y despliegue.
