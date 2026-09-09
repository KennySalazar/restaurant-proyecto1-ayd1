# Configuración de despliegue y CI/CD

## Servicios y configuración

| Componente | Servicio | Configuración |
|---|---|---|
| Backend | Render Web (Service Free) | Docker, rama `main`, raíz `restaurante-api` |
| Base de datos | Neon | PostgreSQL 18, AWS Ohio `us-east-2`, proyecto `restaurante-ayd1`, rama `production`, base `neondb` |
| Admin | Cloudflare Pages | Direct Upload, proyecto `restaurante-admin-ayd1`, rama de producción `main` |
| POS | Cloudflare Pages | Direct Upload, proyecto `restaurante-pos-ayd1`, rama de producción `main` |
| Correo | Brevo | API HTTPS y remitente verificado por el propietario |
| CI/CD | GitHub Actions | Workflow `CI`, pruebas y publicación del commit aprobado |

Cloudinary está previsto para imágenes de platillos, aún sin integrar.

### Render y Docker

En el servicio existente: Dockerfile Path `./Dockerfile`, Build Context `.`, ambos
relativos a `restaurante-api`. Docker Command y Pre-deploy Command vacíos.
Health Check Path: `/api/v1/actuator/health/readiness`.

El [Dockerfile](../restaurante-api/Dockerfile) construye con Java 21 JDK y Gradle
Wrapper 9.5.1, ejecutando `check bootJar`. La etapa final usa Java 21 JRE, UID 10001
y heap máximo del 50 % de la memoria. Spring Boot es 4.1.0.
El [.dockerignore](../restaurante-api/.dockerignore) limita el contexto y excluye
credenciales. No pasar secretos como argumentos de build.

La imagen selecciona `prod`; la ejecución local mantiene `dev` como predeterminado.
Producción escucha en `0.0.0.0`, usa `PORT` de Render (8080 si no existe), contexto
`/api/v1`, apagado ordenado y cabeceras del proxy. Hikari tiene máximo 5 conexiones
y mínimo inactivo 0. Las imágenes base usan tags, no digests fijados: Render reconstruye
el mismo commit validado por CI, pero no recibe una imagen binariamente idéntica del runner.

### Variables del backend
Esta documentación solo registra nombres y propósitos; las credenciales no se comparten por Git ni por chat.

| Variable | Propósito |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Seleccionar `prod` |
| `DATABASE_URL` | URL JDBC directa de Neon con TLS, sin usuario ni contraseña incrustados |
| `DATABASE_USERNAME` | Rol PostgreSQL usado por la aplicación y Flyway |
| `DATABASE_PASSWORD` | Contraseña de ese rol de Neon |
| `SECRET_KEY_JWT` | Secreto de firma JWT, al menos 32 bytes aleatorios; conservar entre despliegues |
| `INITIAL_ADMIN_EMAIL` | Correo de la cuenta administrativa inicial |
| `INITIAL_ADMIN_PASSWORD` | Contraseña inicial que cumpla la política de la API |
| `BREVO_API_KEY` | Autorización de envío mediante la API HTTPS de Brevo |
| `MAIL_FROM` | Remitente verificado en Brevo |
| `CORS_ALLOWED_ORIGINS` | Los dos orígenes públicos de Pages |
| `MAIL_SENDER_NAME` | Nombre visible opcional del remitente |
| `EXPIRATION_TIME_JWT` | Duración opcional del JWT en milisegundos |
| `OTP_EXPIRATION_MINUTES` | Vigencia opcional del OTP |
| `OTP_MAX_ATTEMPTS` | Límite opcional de intentos del OTP |

El bootstrap requiere las variables del administrador en cada arranque y habilita
la cuenta configurada como ADMIN. No retirarlas ni cambiar el correo sin revisar su efecto.
Producción selecciona Brevo; no necesita credenciales SMTP ni la clave de API de Render.

### Neon, Flyway y persistencia

Construir `DATABASE_URL` con el host **directo** de la opción Connect de Neon, puerto 5432, base `neondb` y esquema `jdbc:postgresql://`. Parámetros TLS utilizados:
`sslmode=verify-full&sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory`. El usuario y la contraseña van separados. No pegar una connection string con credenciales
en el repositorio ni desactivar TLS para resolver un error de autenticación.

Flyway se ejecuta **únicamente al arrancar Spring Boot**. Se aplicaron ocho migraciones
V1–V8; Hibernate usa `ddl-auto=validate`. Los siguientes arranques validan el historial
y aplican solo las pendientes. No editar migraciones aplicadas, ejecutar un segundo
migrador en CI, recrear Neon ni usar `clean`, `repair` o `baseline` para ocultar errores.
Los cambios de esquema nuevos deben ser compatibles con la versión anterior durante
la publicación y añadirse mediante nuevas migraciones.

### Pages y CORS

Ambos `environment.production.ts` usan
`https://restaurant-proyecto1-ayd1.onrender.com/api/v1`.
En desarrollo conservan `/api/v1` con proxy hacia `http://localhost:8090`.
Los archivos que se publican son `dist/restaurante-admin/browser` y
`dist/restaurante-pos/browser`, relativos a cada proyecto Angular.
No existe `404.html` en la raíz publicada; Pages ofrece navegación SPA.

Valor público configurado en `CORS_ALLOWED_ORIGINS`:

```text
https://restaurante-admin-ayd1.pages.dev,https://restaurante-pos-ayd1.pages.dev
```

No añadir rutas, barra final ni comodines. Los frontend solo reciben la URL pública
de la API, nunca secretos de Neon, correo o JWT. La primera publicación se hizo con
ZIP aprobados por CI; las siguientes las realiza Wrangler desde Actions.

## GitHub Actions

Fuentes ejecutables: [workflow](../.github/workflows/ci.yml),
[prueba integral del backend](../scripts/ci/backend_smoke.py),
[script de publicación](../scripts/ci/deploy.py) y
[pruebas de publicación](../scripts/ci/test_deploy.py).

### Credenciales y activación

En Settings → Secrets and variables → Actions, **Repository secrets**:

| Secreto | Propósito |
|---|---|
| `RENDER_API_KEY` | Consultar y solicitar despliegues en Render |
| `RENDER_SERVICE_ID` | Identificar el Web Service existente, con prefijo `srv-` |
| `CLOUDFLARE_API_TOKEN` | Permiso Account / Cloudflare Pages / Edit en la cuenta de los sitios |
| `CLOUDFLARE_ACCOUNT_ID` | Identificar la cuenta que contiene ambos Pages |

Los cuatro fueron configurados por el propietario. Se activó la **Repository variable**
`PRODUCTION_DEPLOY_ENABLED=true`. No es un secreto ni una variable de Render.
`GITHUB_TOKEN` lo aporta Actions. Los secretos de base de datos, correo y aplicación
permanecen en Render; no se duplican en GitHub.

### Eventos y orden de ejecución

| Evento | Resultado |
|---|---|
| Pull request | CI; no despliega |
| Push a `develop` | CI; no despliega |
| Ejecución manual `workflow_dispatch` | CI; no despliega |
| Push o merge a `main` | CI y después CD, si aprueba todo y la variable está en `true` |

No hay filtros por archivos: **un cambio solo documental que llega a main también
puede desplegar**. Las protecciones de ramas con PR y `CI aprobada` obligatoria
siguen pendientes de comprobación; el workflow no impide por sí mismo un push directo.

1. Backend: build Docker, pruebas Java y prueba con PostgreSQL 18 aislado. Comprueba
   migraciones, login/JWT, autorización, CORS y reinicio sin duplicar el historial.
   Genera credenciales efímeras; no carga `.env`, no usa Neon ni envía correos reales.
2. Admin y POS: Node 24, npm 11.6.2, `npm ci`, pruebas y build de producción.
3. Automatización: pruebas Python con proveedores simulados.
4. `CI aprobada`: exige éxito de los tres grupos. Fallo, cancelación u omisión bloquean CD.
5. CD descarga los dos builds del mismo intento e instala Wrangler 4.130.0.
6. Verifica ambos Pages en `main`, Render en `main` con Auto-Deploy Off y que el SHA
   siga siendo el actual de main. Solicita a Render ese commit concreto y espera Live,
   coincidencia de SHA y readiness UP.
7. Publica Admin y después POS; comprueba SHA y éxito del despliegue de producción de cada uno.

Producción se serializa y no se cancela automáticamente por otro push. El script
rechaza commits antiguos antes de Render y antes de cada Pages; main puede avanzar
durante una petición ya iniciada. Los permisos del workflow son `contents: read`;
los secretos de proveedores solo se pasan al paso de publicación.

### Reintentos y fallos

Revisar el job fallido, informe y paneles antes de autorizar un reintento. Usar
**Re-run all jobs** sobre la ejecución push del main vigente: los artefactos incluyen
el número de intento y deben reconstruirse juntos. Repetir solo CD no recupera los
builds con el nombre del nuevo intento. Mantener desmarcado el debug de diagnóstico.

Render y los dos Pages no forman una transacción: un fallo tardío puede dejar solo
parte de los servicios actualizados. No hay rollback automático ni reversión de Flyway.
Cancelar Actions o agotar el timeout no cancela una publicación ya aceptada por el proveedor.
Desactivar la variable evita futuras publicaciones elegibles; no revierte las ya iniciadas.

## Evidencias y validación realizada

- Preparación publicada mediante PR #3–#8. Primer CD exitoso:
  [Actions 34349886383, intento 2](https://github.com/KennySalazar/restaurant-proyecto1-ayd1/actions/runs/34349886383/attempts/2).
- Commit publicado: `614f899cf7064da725744bd24598020fcc873607`.
- Todos los jobs aprobaron, incluido `Desplegar producción`.
- Se confirmó la existencia de `production-report-34349886383-2` en Actions.
- Comprobación pública posterior: readiness HTTP 200/UP y `/auth/me` sin sesión HTTP 401.
- El usuario confirmó login Admin después de CD; verifica el recorrido frontend → API → Neon.
- Flyway aplicó V1–V8 en Neon y el usuario recibió un correo real de recuperación de Brevo
  durante la validación inicial. No se probó el cambio completo de contraseña ni todo 2FA.
- Validación local previa: 9 pruebas Java, una prueba por Angular, 9 pruebas Python,
  actionlint 1.7.7 y prueba integral Docker/PostgreSQL aprobadas.

Los informes se conservan 14 días y los builds Angular 7 días según el workflow.
Descargar y guardar evidencia para la evaluación antes de que expire; la presencia del
artefacto en Actions no significa que ya exista una copia permanente en este repositorio.
No guardar tokens, contraseñas ni códigos OTP en capturas o reportes.

### Dependencias revisadas durante la preparación

Se actualizaron y validaron los dos lockfiles, sin `--force`, `--legacy-peer-deps`
ni `npm audit fix`. Angular/core y compiler-cli 21.2.22, build/CLI 21.2.21,
Tailwind 4.3.2, PostCSS 8.5.23; transitivas undici 7.29.0, fast-uri 3.1.7,
nanoid 3.3.18 y qs 6.16.0. El ajuste posterior incluyó Vitest y asociados 4.1.11,
Hono 4.13.7 y js-yaml 4.3.2. Los lockfiles son la referencia exacta.

La auditoría completa y sin dependencias de desarrollo dio cero vulnerabilidades
el 9 de septiembre de 2026. Es un resultado fechado, no una garantía permanente;
CI todavía no impone un umbral de `npm audit`.

## Pendientes de cierre

- Probar POS con cuenta operativa, pospuesto por el equipo.
- Comprobar protecciones de main/develop y conservar evidencias fuera de su plazo de retención.
- Revisar la autoconfiguración de usuario en memoria de Spring Security observada en logs.
- Completar pruebas de recuperación, 2FA y recarga de rutas; Cloudinary se integrará con platillos.

Readiness solo comprueba el estado de arranque, no consulta Neon en cada sondeo.
Para validar datos y correo se necesitan pruebas funcionales adicionales. La publicación
de los servicios no certifica la implementación completa de las funciones del enunciado.