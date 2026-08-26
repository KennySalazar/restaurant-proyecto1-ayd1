# SGR API

API de SGG: autorización, cuentas, OTP y correo electrónico. La API utiliza el contexto `/api/v1` y el perfil activo `dev` se ejecuta en el puerto `8090`.

## Requisitos

- Java 21.
- Docker y Docker Compose para PostgreSQL.
- Variables de entorno configuradas antes de ejecutar Gradle o la aplicación.


## Configuración

Utiliza `.env.example` como referencia y exporta todas sus variables. Spring y Gradle no cargan automáticamente un archivo `.env`.

Variables principales:

- `DATABASE_URL`, `DATABASE_USERNAME` y `DATABASE_PASSWORD`: conexión a PostgreSQL.
- `EMAIL_SENDER_APP` y `EMAIL_SENDER_PASSWORD`: credenciales de Gmail SMTP.
- `SECRET_KEY_JWT`: clave utilizada para firmar JWT; debe tener al menos 32 caracteres.
- `EXPIRATION_TIME_JWT`: duración del JWT en milisegundos.
- `INITIAL_ADMIN_PASSWORD`: contraseña inicial del administrador; debe cumplir la política de contraseñas.
- `OTP_LENGTH`, `OTP_EXPIRATION`, `OTP_MAX_ATTEMPTS` y `OTP_RESEND_COOLDOWN`: política de los desafíos OTP.

El arranque crea de forma idempotente el usuario `sistemasss404@gmail.com` con el rol `ADMIN`. Si el usuario ya existe, no se reemplaza su contraseña en los arranques posteriores. Si `INITIAL_ADMIN_PASSWORD` no está configurada o no cumple la política, el arranque falla explícitamente.

Las contraseñas deben tener entre 8 y 72 caracteres, con al menos una mayúscula, una minúscula y un número. El cambio autenticado debe usar una contraseña diferente de la actual; no se conserva un historial de contraseñas. Los OTP tienen seis dígitos por defecto, expiran después de 10 minutos, permiten cinco intentos fallidos y tienen un tiempo de espera de reenvío de un minuto. Los OTP nunca se almacenan en texto plano.

El envío utiliza Gmail SMTP mediante `EMAIL_SENDER_APP` y `EMAIL_SENDER_PASSWORD`. Un fallo de entrega devuelve `email_delivery_failed` con HTTP 503 y revierte el desafío. Las credenciales, contraseñas, tokens bearer y OTP no se registran en los logs.

## Ejecución

Inicia la base de datos de desarrollo:

```bash
docker compose up -d
```

Ejecuta la aplicación después de exportar las variables de entorno:

```bash
./gradlew bootRun
```

Comandos de verificación:

```bash
./gradlew compileJava
./gradlew test
./gradlew check
./gradlew build
```

Las pruebas de integración requieren PostgreSQL activo. El servicio de correo se reemplaza por un emisor de prueba durante las pruebas automatizadas, por lo que no se necesita acceso a Gmail.

## Arquitectura

El proyecto utiliza una arquitectura por capas. Las clases de aplicación permanecen bajo `com.ssg` para que Spring realice el escaneo de componentes correctamente.

```text
src/main/java/com/ssg/
├── application/
│   ├── auth/       Casos de uso de autenticación, contraseñas y OTP
│   ├── common/     Utilidades compartidas, como la normalización de correo
│   └── mail/       Abstracción y envío de correo mediante Gmail
├── config/         Propiedades, seguridad, OpenAPI y bootstrap del administrador
├── domain/
│   ├── model/      Entidades y enumeraciones del dominio
│   └── repository/ Repositorios JPA
├── exception/      Excepciones de aplicación y construcción de RFC 9457
├── security/       Filtro JWT y respuestas de autenticación/autorización
└── web/
    ├── admin/      Controladores restringidos al rol ADMIN
    ├── auth/       Controladores de autenticación
    ├── dto/        Objetos de entrada y salida de la API
    ├── exception/  Manejador central de excepciones web
    └── validation/ Validaciones de las solicitudes
```

Las migraciones forward-only se encuentran en `src/main/resources/db/migration/`. `V1__create_core_tables.sql` crea las tablas, restricciones e índices; `V2__seed_roles.sql` registra los roles iniciales.

## Contrato de la API

### Endpoints públicos

- `POST /api/v1/auth/register` crea una cuenta pendiente con rol `MEMBER` y devuelve un `challengeId` de registro. Responde HTTP 201.
- `POST /api/v1/auth/register/verify` verifica el OTP de registro.
- `POST /api/v1/auth/register/resend` reemplaza el desafío de registro pendiente.
- `POST /api/v1/auth/login` autentica con correo y contraseña. Las cuentas con 2FA reciben un `challengeId` en lugar de un token.
- `POST /api/v1/auth/login/verify` intercambia un OTP de inicio de sesión válido por un JWT.
- `POST /api/v1/auth/password-recovery` solicita recuperación sin revelar si el correo existe. Responde HTTP 202.
- `POST /api/v1/auth/password-recovery/verify` consume un OTP de recuperación y establece una nueva contraseña.

### Endpoints protegidos con bearer JWT

- `GET /api/v1/auth/me` devuelve la cuenta autenticada.
- `POST /api/v1/auth/password/change` verifica la contraseña actual y cambia la contraseña.
- `POST /api/v1/auth/2fa/enable` y `/api/v1/auth/2fa/enable/verify` habilitan el 2FA opcional mediante un desafío de confirmación.
- `POST /api/v1/auth/2fa/disable` y `/api/v1/auth/2fa/disable/verify` deshabilitan el 2FA después de verificar la contraseña actual y un OTP.
- `GET /api/v1/admin/ping` requiere el único rol asignado `ADMIN`.

Los errores utilizan `application/problem+json` y cumplen RFC 9457 mediante los campos `type`, `title`, `status`, `detail` e `instance`, además de la extensión estable `code`. Los detalles enviados al cliente están en español.

## OpenAPI y Swagger

El documento OpenAPI está disponible en `/api/v1/v3/api-docs` y Swagger UI en `/api/v1/swagger-ui.html`.
