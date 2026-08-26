# Restaurante API

Backend REST del **Sistema de Gestión de Restaurante**. Centraliza autenticación, autorización, JWT, recuperación de contraseña, OTP, 2FA y servirá como API compartida por `restaurante-admin` y `restaurante-pos`.

La API utiliza el contexto `/api/v1`, el perfil de desarrollo `dev` y localmente se ejecuta en el puerto `8090`.

## Requisitos

- Java 21
- Docker
- Docker Compose
- PostgreSQL 18 mediante el `docker-compose.yaml` del proyecto
- Variables de entorno configuradas antes de ejecutar la aplicación

Los comandos de esta sección deben ejecutarse desde `restaurante-api/`.

## Configuración

Usa `.env.example` como referencia:

```bash
cp .env.example .env
```

Spring Boot y Gradle no cargan automáticamente el archivo `.env`. Antes de ejecutar la aplicación:

```bash
set -a
source .env
set +a
```

Variables principales:

```env
DATABASE_URL=jdbc:postgresql://localhost:5437/restaurante_db
DATABASE_USERNAME=restaurante_user
DATABASE_PASSWORD=replace-with-local-password

SECRET_KEY_JWT=replace-with-at-least-32-random-characters
EXPIRATION_TIME_JWT=86400000

INITIAL_ADMIN_EMAIL=admin@restaurante.com
INITIAL_ADMIN_PASSWORD=replace-with-a-strong-password

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=correo-del-restaurante@gmail.com
MAIL_PASSWORD=replace-with-app-password
MAIL_FROM=correo-del-restaurante@gmail.com

OTP_EXPIRATION_MINUTES=10
OTP_MAX_ATTEMPTS=5
```

Nunca se deben versionar `.env`, contraseñas, OTP, JWT, App Passwords ni credenciales cloud.

## Política de contraseñas

Las contraseñas deben tener:

- entre 8 y 72 caracteres;
- al menos una mayúscula;
- al menos una minúscula;
- al menos un número.

Los OTP son códigos de 6 dígitos, expiran después del tiempo configurado y tienen un máximo de intentos. El código no se almacena en texto plano: se persiste su hash.

## Base de datos

Levantar PostgreSQL:

```bash
docker compose up -d
```

Verificar:

```bash
docker compose ps
```

Configuración local actual:

```text
Host: localhost
Puerto: 5437
Base de datos: restaurante_db
Usuario: restaurante_user
```

## Flyway

Las migraciones están en:

```text
src/main/resources/db/migration/
```

Migraciones actuales:

```text
V1__create_core_tables.sql
V2__seed_roles.sql
V3__add_two_factor_and_otp.sql
```

Las migraciones son forward-only. Una migración ya aplicada no debe editarse para introducir un cambio posterior; debe crearse una nueva `V4__...`, `V5__...`, etc.

## Ejecución

Compilar:

```bash
./gradlew clean compileJava
```

Pruebas:

```bash
./gradlew test
```

Validación:

```bash
./gradlew check
```

Build:

```bash
./gradlew build
```

Ejecutar:

```bash
set -a
source .env
set +a
./gradlew bootRun
```

API:

```text
http://localhost:8090/api/v1
```

## Arquitectura

El proyecto utiliza una arquitectura por capas bajo `com.restaurante`.

```text
src/main/java/com/restaurante/
├── application/
│   ├── auth/       Casos de uso de autenticación, contraseñas y OTP
│   ├── common/     Utilidades compartidas
│   └── mail/       Abstracción y envío SMTP
├── config/         Seguridad, propiedades, bootstrap y configuración
├── domain/
│   ├── model/      Entidades y enumeraciones
│   └── repository/ Repositorios JPA
├── exception/      Excepciones de aplicación
├── security/       JWT, filtro y respuestas de seguridad
└── web/
    ├── admin/      Endpoints restringidos a ADMIN
    ├── auth/       Endpoints de autenticación
    ├── dto/        Contratos de entrada y salida
    ├── exception/  Manejo global de errores HTTP
    └── validation/ Validaciones web
```

La estructura crecerá con los módulos del restaurante manteniendo esta separación.

## Roles

Roles actuales:

```text
ADMIN
WAITER
KITCHEN
CASHIER
```

- `ADMIN`: plataforma administrativa.
- `WAITER`: operación de mesas y comandas.
- `KITCHEN`: preparación de órdenes.
- `CASHIER`: facturación, cobros y caja.

No existe registro público de empleados. Las cuentas operativas serán administradas internamente.

## Autenticación

### Login sin 2FA

```text
correo + contraseña
        ↓
POST /auth/login
        ↓
JWT
```

### Login con 2FA

```text
correo + contraseña
        ↓
POST /auth/login
        ↓
challengeId + OTP por correo
        ↓
POST /auth/login/verify
        ↓
JWT
```

### Recuperación

```text
correo
  ↓
POST /auth/password-recovery
  ↓
OTP
  ↓
POST /auth/password-recovery/verify
  ↓
contraseña actualizada
```

### Activar/desactivar 2FA

Los endpoints requieren JWT, contraseña actual y confirmación OTP.

## Contrato actual de autenticación

Todos los endpoints usan el prefijo `/api/v1`.

### Públicos

```http
POST /api/v1/auth/login
POST /api/v1/auth/login/verify
POST /api/v1/auth/password-recovery
POST /api/v1/auth/password-recovery/verify
```

### Protegidos con Bearer JWT

```http
GET  /api/v1/auth/me
POST /api/v1/auth/password/change
POST /api/v1/auth/change-password
POST /api/v1/auth/2fa/enable
POST /api/v1/auth/2fa/enable/verify
POST /api/v1/auth/2fa/disable
POST /api/v1/auth/2fa/disable/verify
```

### Administrativo

```http
GET /api/v1/admin/ping
```

Requiere `ROLE_ADMIN`.

## Errores HTTP

La API utiliza Problem Details con campos como:

```json
{
  "type": "...",
  "title": "...",
  "status": 400,
  "detail": "...",
  "instance": "...",
  "code": "..."
}
```

`code` se utiliza como identificador estable para que los frontends puedan traducir o manejar errores.

## OpenAPI y Swagger

OpenAPI:

```text
http://localhost:8090/api/v1/v3/api-docs
```

Swagger UI:

```text
http://localhost:8090/api/v1/swagger-ui/index.html
```

También está configurada la ruta:

```text
/api/v1/swagger-ui.html
```

## Antes de un Pull Request

```bash
./gradlew test
./gradlew build
```
