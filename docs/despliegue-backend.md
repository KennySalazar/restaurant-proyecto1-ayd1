# Preparación del backend para Render Free

Estado: preparación local. Este documento no implica que el backend esté publicado.
Neon fue creado por el equipo: AWS Ohio (`us-east-2`), rama `production`, base `neondb`.
La conexión real y la entrega de correo siguen pendientes hasta configurar el servicio.

## Construcción local

Desde la raíz del repositorio:

```bash
docker build -t restaurante-api:local-prod-check restaurante-api
```

La primera etapa usa Java 21 JDK y el Gradle Wrapper versionado para ejecutar
`check bootJar`. Una prueba fallida impide construir la imagen final. La segunda
etapa contiene Java 21 JRE y el JAR, y arranca como UID 10001. El contexto de build
solo permite fuentes, pruebas y archivos Gradle; no incluye `.env`, Compose o imágenes locales.
No suministrar secretos como argumentos de build.

Las imágenes base usan tags de Java 21; pueden recibir actualizaciones del proveedor.
El build local registra sus digests. Antes de una entrega reproducible se pueden fijar
esos digests y actualizarlos deliberadamente con nuevas revisiones de seguridad.

La JVM limita su heap al 50% de la memoria del contenedor, dejando espacio para
metaspace, threads y memoria nativa. Esto no limita por sí solo el uso total de memoria.

## Perfil y variables

Sin perfil explícito, `bootRun` mantiene el perfil local `dev` y el puerto 8090.
La imagen activa `prod`, escucha en `0.0.0.0` y utiliza el puerto `PORT` del proveedor
(8080 si no existe). El context path sigue siendo `/api/v1`.

Configurar los valores únicamente en Environment de Render:

| Nombre | Propósito |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Seleccionar `prod`; ya es el valor predeterminado de la imagen. |
| `DATABASE_URL` | URL JDBC directa de Neon con TLS, sin credenciales incrustadas. |
| `DATABASE_USERNAME` | Rol de Neon seleccionado para la aplicación y Flyway. |
| `DATABASE_PASSWORD` | Contraseña del rol. |
| `SECRET_KEY_JWT` | Secreto aleatorio de al menos 32 bytes; conservarlo entre despliegues. |
| `INITIAL_ADMIN_EMAIL` | Cuenta administrativa inicial real. |
| `INITIAL_ADMIN_PASSWORD` | Contraseña inicial que cumpla la política del backend. |
| `BREVO_API_KEY` | Clave API privada de Brevo. |
| `MAIL_FROM` | Dirección verificada en Brevo. |
| `MAIL_SENDER_NAME` | Nombre visible opcional. |
| `CORS_ALLOWED_ORIGINS` | Orígenes HTTPS reales de Admin y POS, separados por comas y sin ruta ni barra final. |
| `EXPIRATION_TIME_JWT` | Duración opcional del JWT en milisegundos. |
| `OTP_EXPIRATION_MINUTES` | Vigencia opcional del OTP. |
| `OTP_MAX_ATTEMPTS` | Intentos opcionales del OTP. |

`prod` selecciona Brevo. No necesita usuario o contraseña SMTP. Mientras no existan
las URL de los frontend, CORS puede quedar vacío: las llamadas entre orígenes se
rechazarán, pero las comprobaciones directas y Swagger seguirán disponibles.
Los futuros orígenes Pages deben ser explícitos; no se permite `*.pages.dev`.

Para la URL JDBC, tomar el hostname de la conexión directa de Neon, el puerto de
PostgreSQL y el nombre `neondb`. Usar el esquema `jdbc:postgresql://`.
Para verificar certificado y hostname con el almacén de confianza de Java, los
parámetros son `sslmode=verify-full` y
`sslfactory=org.postgresql.ssl.DefaultJavaSSLFactory`. No trasladar sin conversión
una URL `postgresql://usuario:contraseña@...`. La conexión TLS real queda pendiente
de comprobación en Render; no desactivar su validación para resolver errores.

El bootstrap actual exige la contraseña inicial en cada arranque y habilita la
cuenta configurada como ADMIN. No retirar esa variable tras el primer despliegue;
no cambiar de correo bootstrap sin revisar el efecto sobre las cuentas.

## Configuración prevista en Render

No crear el servicio hasta autorizar la publicación del código y el primer despliegue:
la creación del servicio dispara un despliegue inicial.

| Campo | Selección |
|---|---|
| Tipo | Web Service |
| Language | Docker |
| Región | Ohio |
| Instance type | Free |
| Branch | `main`, después de promover mediante PR una versión validada. |
| Root Directory | `restaurante-api` |
| Dockerfile Path | `./Dockerfile`, relativo a Root Directory. |
| Docker Build Context | `.`, relativo a Root Directory. |
| Docker Command | Vacío, utilizar el ENTRYPOINT de la imagen. |
| Health Check Path | `/api/v1/actuator/health/readiness` |
| Auto Deploy | Off, hasta que GitHub Actions controle los despliegues. |

No configurar un comando previo de Flyway ni un segundo migrador en el pipeline.
Las ocho migraciones se ejecutan desde Spring Boot; Hibernate solo valida el esquema.
Un reinicio valida el historial y aplica únicamente migraciones pendientes.
No utilizar `clean`, `repair` o `baseline` para ocultar un fallo, ni editar V1–V8.

## Salud y validación

La ruta pública de readiness responde únicamente con el estado. Spring Boot la marca
lista después del arranque, incluidas las migraciones y el bootstrap. Las otras rutas
Actuator requieren autenticación y solo se expone el endpoint health.
Readiness no consulta la base en cada sondeo: evita mantener Neon activo con consultas
permanentes y reiniciar la aplicación por un corte transitorio externo. Por tanto,
`UP` no certifica por sí solo una conexión vigente a Neon; comprobar además login y
`/auth/me`, que sí utilizan la base.

Después del despliegue autorizado, verificar:

1. Readiness responde 200 y OpenAPI es accesible bajo `/api/v1/v3/api-docs`.
2. Flyway registra V1–V8 exitosas en Neon y un reinicio no duplica entradas.
3. Login del administrador y `/auth/me` funcionan; acceso anónimo protegido devuelve 401.
4. Recuperación y 2FA entregan correo real; Brevo aceptarlo no garantiza su entrega al buzón.
5. Preflight desde los orígenes aprobados funciona y un origen ajeno se rechaza.
6. Revisar memoria y arranque en frío en Render: una prueba local no reproduce su CPU o red.

Guardar evidencias sin contraseñas, JWT, claves o códigos OTP.

## Validación local completada — 5 de septiembre de 2026

- Imagen: `restaurante-api:local-prod-check`, identificador
  `sha256:aa0615c0541caa3ba8591bbdaf2fc38178b8d12783e636a7c484b879f9572a61`.
- `check bootJar` terminó con `BUILD SUCCESSFUL` durante la construcción Docker,
  incluyendo pruebas simuladas de Brevo y pruebas de CORS.
- Prueba integral con PostgreSQL 18 aislado, sin utilizar `.env`, la base local ni Neon.
  Credenciales ficticias generadas para la prueba; no se enviaron correos.
- Arranque con perfil `prod` y puerto personalizado: readiness respondió 200 sin detalles.
- V1–V8 se aplicaron correctamente a una base vacía.
- Login bootstrap, JWT, `/auth/me`, autorización administrativa y OpenAPI funcionaron.
  Las rutas protegidas rechazaron solicitudes anónimas.
- Preflight aceptado para Admin/POS de prueba y rechazado para un origen ajeno.
- Tras reiniciar el mismo contenedor, readiness volvió a responder 200, el historial
  de Flyway permaneció idéntico y el JWT anterior siguió funcionando contra la base.
- Usuario de ejecución: UID 10001. Límite probado: 512 MiB, sin swap, 0,5 CPU;
  muestra de memoria: 304 MiB. Primer arranque listo en aproximadamente 53 segundos.
  No es una prueba de carga ni reproduce la CPU disponible en Render Free.
- Los contenedores de prueba quedaron detenidos. No se eliminaron contenedores,
  redes ni volúmenes existentes.
- Compose pasó la validación sintáctica usando un valor ficticio y sin cargar `.env`;
  se comprobó también que falla al omitir `DATABASE_PASSWORD`.
- `git diff --check` no reportó problemas; no se modificaron migraciones.

El primer intento de comprobar el reinicio consultó un puerto efímero antiguo.
Se corrigió el comprobador para consultar el puerto reasignado por Docker y se
repitió la prueba completa, que terminó correctamente. No fue necesario cambiar
el backend para resolver ese problema del comprobador.

Siguen pendientes la conexión TLS real a Neon, el envío real con Brevo y la
validación en Render. No se realizó commit, push, merge ni despliegue remoto.

## Referencias oficiales

- [Docker en Render](https://render.com/docs/docker)
- [Monorepos en Render](https://render.com/docs/monorepo-support)
- [Despliegues y auto-deploy](https://render.com/docs/deploys)
- [Health checks](https://render.com/docs/health-checks)
- [TLS con PostgreSQL JDBC](https://jdbc.postgresql.org/documentation/ssl/)
- [Perfiles Spring Boot](https://docs.spring.io/spring-boot/reference/features/profiles.html)
