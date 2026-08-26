# Sistema de Gestión de Restaurante

Proyecto 1 del curso **Análisis y Diseño de Sistemas 1**, Segundo Semestre 2026.

El sistema está diseñado como una solución integral para la administración y operación de un restaurante. Está compuesto por **dos aplicaciones Angular independientes**, conectadas a un **mismo backend Spring Boot** y a una **base de datos PostgreSQL**.

Actualmente el repositorio contiene el **esqueleto técnico funcional**, incluyendo autenticación, autorización por roles, JWT, recuperación de contraseña y autenticación de dos factores (2FA). Los módulos de negocio se implementarán de forma incremental utilizando GitFlow y ramas `feature/*`.

---

## 1. Arquitectura general

```text
restaurante-proyecto1-ayd1/
├── restaurante-api/        # Backend REST - Spring Boot
├── restaurante-admin/      # Aplicación administrativa - Angular
├── restaurante-pos/        # Aplicación operativa / POS - Angular
├── docs/
│   ├── historias-usuario/
│   ├── diagramas/
│   ├── manual-tecnico/
│   └── manual-usuario/
├── Jenkinsfile             # Pipeline CI/CD
├── .gitignore
└── README.md
```

### Flujo de comunicación

```text
restaurante-admin :4200 ─┐
                         ├──> restaurante-api :8090/api/v1 ───> PostgreSQL :5437
restaurante-pos   :4201 ─┘
```

Las dos aplicaciones consumen la misma API y comparten la misma base de datos. Esto permite que la configuración administrativa y la operación diaria del restaurante trabajen sobre una única fuente de información.

---

## 2. Aplicaciones

### Aplicación Administrativa

Uso exclusivo del rol:

```text
ADMIN
```

Permitirá administrar:

- insumos;
- recetas;
- platillos y menú;
- inventario y kardex;
- mesas;
- empleados;
- configuración;
- impuestos;
- propina sugerida;
- fidelización;
- reservas;
- lista de espera;
- ocupación;
- reportes.

Puerto local:

```text
http://localhost:4200
```

### Aplicación Operativa / POS

Roles permitidos:

```text
WAITER
KITCHEN
CASHIER
```

Permitirá cubrir la operación diaria:

- mesas;
- apertura de cuentas;
- comandas;
- cocina;
- facturación;
- cobros;
- caja;
- fidelización;
- calificación del servicio.

Puerto local:

```text
http://localhost:4201
```

---

## 3. Tecnologías

### Backend

- Java 21
- Spring Boot 4.1.0
- Gradle
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Spring Validation
- Spring Mail
- JWT con JJWT 0.12.6
- Flyway
- PostgreSQL 18
- Swagger / OpenAPI
- MapStruct 1.6.3
- Lombok

### Frontend

Ambas aplicaciones utilizan:

- Angular 21
- TypeScript 5.9
- PrimeNG 21
- PrimeIcons
- Tailwind CSS 4
- Transloco
- RxJS
- npm

### Infraestructura y herramientas

- Docker / Docker Compose
- Git
- GitHub
- GitFlow
- JIRA
- BDD con Gherkin
- Jenkins para CI/CD
- Proveedor cloud para despliegue final

---

# 4. Backend: `restaurante-api`

La API utiliza el contexto:

```text
/api/v1
```

El perfil activo de desarrollo es:

```text
dev
```

y localmente se ejecuta en:

```text
http://localhost:8090/api/v1
```

Los comandos del backend deben ejecutarse desde:

```text
restaurante-api/
```

---

## 4.1 Estructura del backend

El backend utiliza una arquitectura por capas bajo el paquete raíz:

```text
com.restaurante
```

Estructura principal:

```text
restaurante-api/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── restaurante/
│   │   │           ├── RestauranteApiApplication.java
│   │   │           │
│   │   │           ├── application/
│   │   │           │   ├── auth/
│   │   │           │   │   ├── AuthService.java
│   │   │           │   │   ├── OtpService.java
│   │   │           │   │   ├── OtpDeliveryService.java
│   │   │           │   │   ├── OtpIssue.java
│   │   │           │   │   └── PasswordPolicy.java
│   │   │           │   │
│   │   │           │   ├── common/
│   │   │           │   │   └── EmailNormalizer.java
│   │   │           │   │
│   │   │           │   └── mail/
│   │   │           │       ├── EmailService.java
│   │   │           │       └── SmtpEmailService.java
│   │   │           │
│   │   │           ├── config/
│   │   │           │   ├── CoreConfiguration.java
│   │   │           │   ├── SecurityConfiguration.java
│   │   │           │   ├── JwtProperties.java
│   │   │           │   ├── OtpProperties.java
│   │   │           │   ├── BootstrapProperties.java
│   │   │           │   └── ...
│   │   │           │
│   │   │           ├── domain/
│   │   │           │   ├── model/
│   │   │           │   │   ├── UserAccount.java
│   │   │           │   │   ├── Role.java
│   │   │           │   │   ├── RoleName.java
│   │   │           │   │   ├── OtpChallenge.java
│   │   │           │   │   └── OtpPurpose.java
│   │   │           │   │
│   │   │           │   └── repository/
│   │   │           │       ├── UserAccountRepository.java
│   │   │           │       ├── RoleRepository.java
│   │   │           │       └── OtpChallengeRepository.java
│   │   │           │
│   │   │           ├── exception/
│   │   │           │   └── ApiException.java
│   │   │           │
│   │   │           ├── security/
│   │   │           │   ├── JwtService.java
│   │   │           │   ├── JwtAuthenticationFilter.java
│   │   │           │   ├── ProblemAuthenticationEntryPoint.java
│   │   │           │   └── ProblemAccessDeniedHandler.java
│   │   │           │
│   │   │           └── web/
│   │   │               ├── admin/
│   │   │               │   └── ...
│   │   │               ├── auth/
│   │   │               │   └── AuthController.java
│   │   │               ├── dto/
│   │   │               │   ├── LoginRequest.java
│   │   │               │   ├── LoginResponse.java
│   │   │               │   ├── VerifyChallengeRequest.java
│   │   │               │   ├── ChallengeResponse.java
│   │   │               │   ├── RecoveryRequest.java
│   │   │               │   ├── RecoveryVerificationRequest.java
│   │   │               │   ├── ChangePasswordRequest.java
│   │   │               │   ├── PasswordChangeResponse.java
│   │   │               │   ├── TwoFactorRequest.java
│   │   │               │   ├── UserResponse.java
│   │   │               │   └── MessageResponse.java
│   │   │               ├── exception/
│   │   │               │   └── ...
│   │   │               └── validation/
│   │   │                   └── ...
│   │   │
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── db/
│   │           └── migration/
│   │               ├── V1__create_core_tables.sql
│   │               ├── V2__seed_roles.sql
│   │               └── V3__add_two_factor_and_otp.sql
│   │
│   └── test/
│       └── java/
│           └── com/
│               └── restaurante/
│                   └── ...
│
├── .env.example
├── .gitignore
├── build.gradle
├── settings.gradle
├── docker-compose.yaml
├── gradlew
└── gradlew.bat
```


---

## 4.2 Responsabilidad de cada capa

### `application/`

Contiene los casos de uso y la lógica de aplicación.

Ejemplos actuales:

```text
application/auth/
```

Responsable de:

- login;
- verificación 2FA;
- generación y validación de OTP;
- recuperación de contraseña;
- cambio de contraseña;
- activar y desactivar 2FA.

```text
application/common/
```

Contiene utilidades reutilizables, por ejemplo la normalización de correos.

```text
application/mail/
```

Contiene la abstracción del envío de correo y su implementación SMTP.

### `config/`

Contiene configuración transversal:

- Spring Security;
- propiedades JWT;
- propiedades OTP;
- bootstrap;
- beans comunes;
- OpenAPI;
- configuración específica de la aplicación.

### `domain/model/`

Contiene entidades y enumeraciones del dominio.

Actualmente incluye la base de autenticación:

- `UserAccount`;
- `Role`;
- `RoleName`;
- `OtpChallenge`;
- `OtpPurpose`.

A medida que avance el proyecto se agregarán aquí las entidades del restaurante.

### `domain/repository/`

Contiene las interfaces JPA para persistencia.

### `security/`

Responsable de:

- creación y validación JWT;
- filtro de autenticación;
- tratamiento de accesos no autenticados;
- tratamiento de accesos denegados.

### `web/`

Expone el contrato HTTP de la API.

```text
web/auth/
```

Controladores de autenticación.

```text
web/admin/
```

Controladores exclusivos del rol `ADMIN`.

```text
web/dto/
```

Objetos de entrada y salida de la API.

```text
web/exception/
```

Manejo centralizado de errores HTTP.

```text
web/validation/
```

Validaciones específicas para solicitudes.

---

## 4.3 Base de datos y Flyway

El desarrollo utiliza PostgreSQL 18 mediante Docker Compose.

Configuración local actual:

```text
Host: localhost
Puerto: 5437
Base de datos: restaurante_db
Usuario: restaurante_user
```

Las migraciones se encuentran en:

```text
src/main/resources/db/migration/
```

Migraciones actuales:

```text
V1__create_core_tables.sql
V2__seed_roles.sql
V3__add_two_factor_and_otp.sql
```

Responsabilidad:

```text
V1
└── tablas base de autenticación

V2
└── roles iniciales

V3
├── two_factor_enabled
└── otp_challenges
```

Las migraciones son **forward-only**.

Una migración ya aplicada no debe editarse para introducir un cambio nuevo. Se debe crear una nueva migración:

```text
V4__descripcion_del_cambio.sql
V5__descripcion_del_cambio.sql
...
```

---

## 4.4 Variables de entorno

Copiar:

```bash
cd restaurante-api
cp .env.example .env
```

Spring y Gradle no cargan automáticamente `.env`, por lo que debe exportarse antes de ejecutar la aplicación:

```bash
set -a
source .env
set +a
```

Variables actuales:

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

### Variables de base de datos

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

### Variables JWT

- `SECRET_KEY_JWT`
- `EXPIRATION_TIME_JWT`

La clave JWT debe ser suficientemente larga y nunca debe versionarse.

### Bootstrap del administrador

- `INITIAL_ADMIN_EMAIL`
- `INITIAL_ADMIN_PASSWORD`

La cuenta bootstrap permite disponer de un administrador inicial del sistema.

### Correo SMTP

- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM`

El desarrollo actual utiliza Gmail SMTP.

Para Gmail debe utilizarse una **Contraseña de aplicación** y no la contraseña normal de la cuenta.

### OTP

- `OTP_EXPIRATION_MINUTES`
- `OTP_MAX_ATTEMPTS`

Actualmente:

```text
Expiración: 10 minutos
Intentos máximos: 5
Longitud: 6 dígitos
```

Los códigos OTP no se almacenan en texto plano. Se almacena un hash del código.

---

## 4.5 Política de contraseñas

Las contraseñas deben contener:

```text
8 a 72 caracteres
al menos una mayúscula
al menos una minúscula
al menos un número
```

Ejemplo de validación conceptual:

```text
Password123
```

El backend:

- almacena hashes;
- nunca devuelve la contraseña;
- incrementa `tokenVersion` cuando una contraseña cambia;
- invalida sesiones JWT anteriores cuando corresponde.

---

## 4.6 Flujo de autenticación

### Login sin 2FA

```text
correo + contraseña
        ↓
POST /auth/login
        ↓
JWT
        ↓
GET /auth/me
        ↓
Aplicación
```

### Login con 2FA

```text
correo + contraseña
        ↓
POST /auth/login
        ↓
requiresTwoFactor = true
        ↓
OTP por correo
        ↓
POST /auth/login/verify
        ↓
JWT
        ↓
Aplicación
```

### Recuperación de contraseña

```text
correo
  ↓
POST /auth/password-recovery
  ↓
OTP por correo
  ↓
código + nueva contraseña
  ↓
POST /auth/password-recovery/verify
  ↓
contraseña actualizada
```


### Activación de 2FA

```text
JWT
 ↓
contraseña actual
 ↓
POST /auth/2fa/enable
 ↓
OTP
 ↓
POST /auth/2fa/enable/verify
 ↓
two_factor_enabled = true
```

### Desactivación de 2FA

```text
JWT
 ↓
contraseña actual
 ↓
POST /auth/2fa/disable
 ↓
OTP
 ↓
POST /auth/2fa/disable/verify
 ↓
two_factor_enabled = false
```

---

## 4.7 Contrato actual de la API

Todos los endpoints siguientes utilizan como prefijo:

```text
/api/v1
```

### Endpoints públicos

#### Login

```http
POST /api/v1/auth/login
```

Valida correo y contraseña.

Si 2FA está deshabilitado, devuelve JWT.

Si 2FA está habilitado, devuelve un `challengeId` y solicita el segundo factor.

#### Verificación del login 2FA

```http
POST /api/v1/auth/login/verify
```

Valida el OTP y entrega el JWT.

#### Solicitar recuperación de contraseña

```http
POST /api/v1/auth/password-recovery
```

Genera un desafío de recuperación sin revelar si la cuenta existe.

#### Confirmar recuperación

```http
POST /api/v1/auth/password-recovery/verify
```

Valida OTP y establece la nueva contraseña.

### Endpoints protegidos mediante Bearer JWT

#### Usuario autenticado

```http
GET /api/v1/auth/me
```

Devuelve información de la cuenta autenticada, incluyendo:

- id;
- correo;
- rol;
- estado;
- estado de 2FA.

#### Cambiar contraseña

```http
POST /api/v1/auth/password/change
```

Alias actualmente disponible:

```http
POST /api/v1/auth/change-password
```

#### Solicitar activación 2FA

```http
POST /api/v1/auth/2fa/enable
```

#### Confirmar activación 2FA

```http
POST /api/v1/auth/2fa/enable/verify
```

#### Solicitar desactivación 2FA

```http
POST /api/v1/auth/2fa/disable
```

#### Confirmar desactivación 2FA

```http
POST /api/v1/auth/2fa/disable/verify
```

### Endpoint administrativo

```http
GET /api/v1/admin/ping
```

Requiere:

```text
ROLE_ADMIN
```

> No existe registro público de usuarios. Las cuentas del personal serán administradas internamente por el sistema.

---

## 4.8 Roles actuales

```text
ADMIN
WAITER
KITCHEN
CASHIER
```

Responsabilidad general:

| Rol | Aplicación | Responsabilidad |
|---|---|---|
| `ADMIN` | Admin | Administración del restaurante |
| `WAITER` | POS | Mesas, cuentas y comandas |
| `KITCHEN` | POS | Preparación de órdenes |
| `CASHIER` | POS | Cobros, facturación y caja |

La plataforma Administrativa solo acepta:

```text
ADMIN
```

La plataforma POS acepta:

```text
WAITER
KITCHEN
CASHIER
```

---

## 4.9 Errores HTTP

Los errores del backend utilizan Problem Details.

Formato conceptual:

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

El campo:

```text
code
```

permite que frontend y backend compartan identificadores de errores estables.

Los mensajes enviados al usuario se presentan en español.

---

## 4.10 OpenAPI y Swagger

OpenAPI:

```text
http://localhost:8090/api/v1/v3/api-docs
```

Swagger UI:

```text
http://localhost:8090/api/v1/swagger-ui/index.html
```

También puede estar disponible mediante la ruta configurada:

```text
/api/v1/swagger-ui.html
```

Swagger debe mantenerse actualizado conforme se agreguen módulos y endpoints.

---

## 4.11 Ejecución del backend

Levantar PostgreSQL:

```bash
cd restaurante-api
docker compose up -d
```

Verificar:

```bash
docker compose ps
```

Cargar variables:

```bash
set -a
source .env
set +a
```

Compilar:

```bash
./gradlew clean compileJava
```

Pruebas:

```bash
./gradlew test
```

Validación completa:

```bash
./gradlew check
```

Build:

```bash
./gradlew build
```

Ejecutar:

```bash
./gradlew bootRun
```

API:

```text
http://localhost:8090/api/v1
```

---

# 5. Aplicación administrativa: `restaurante-admin`

La plataforma administrativa se ejecuta en:

```text
http://localhost:4200
```

Rol permitido:

```text
ADMIN
```

Actualmente contiene:

- login;
- 2FA;
- recuperación de contraseña;
- cambio de contraseña;
- activar/desactivar 2FA;
- dashboard base;
- sesión JWT;
- guards;
- interceptores;
- manejo de errores;
- diseño administrativo.

Instalación:

```bash
cd restaurante-admin
npm ci
```

Ejecución:

```bash
npm start
```

Build:

```bash
npm run build
```

Salida:

```text
dist/restaurante-admin
```

---

# 6. Aplicación operativa: `restaurante-pos`

La plataforma POS se ejecuta en:

```text
http://localhost:4201
```

Roles permitidos:

```text
WAITER
KITCHEN
CASHIER
```

Actualmente contiene:

- login;
- 2FA;
- recuperación de contraseña;
- cambio de contraseña;
- activar/desactivar 2FA;
- dashboard operativo base;
- sesión JWT independiente del Admin;
- guards;
- interceptores;
- manejo de errores;
- diseño operativo.

Instalación:

```bash
cd restaurante-pos
npm ci
```

Ejecución:

```bash
npm start
```

Build:

```bash
npm run build
```

Salida:

```text
dist/restaurante-pos
```

---

# 7. Estado actual del esqueleto

## Backend

```text
✅ Spring Boot base
✅ PostgreSQL
✅ Flyway
✅ roles
✅ JWT
✅ Spring Security
✅ correo SMTP
✅ OTP
✅ recuperación de contraseña
✅ 2FA
✅ Swagger
✅ bootstrap ADMIN
```

## Admin

```text
✅ Login
✅ Login 2FA
✅ Recuperación
✅ Cambio de contraseña
✅ Activar/desactivar 2FA
✅ Dashboard base
✅ Protección ADMIN
```

## POS

```text
✅ Login
✅ Login 2FA
✅ Recuperación
✅ Cambio de contraseña
✅ Activar/desactivar 2FA
✅ Dashboard base
✅ Protección WAITER/KITCHEN/CASHIER
```

---

# 8. Módulos planificados

## Plataforma Administrativa

- Insumos
- Recetas
- Historial de recetas
- Platillos
- Categorías
- Modificadores
- Combos y promociones
- Inventario
- Kardex
- Mermas y ajustes
- Disponibilidad
- Mesas
- Empleados
- Impuestos
- Propina sugerida
- Fidelización
- Reservas
- Lista de espera
- Ocupación
- Reportes
- Exportación PDF / Excel

## Plataforma POS

- Mesas
- Apertura de cuentas
- Comandas
- Detalle de órdenes
- Modificadores
- Cocina
- Estados de preparación
- Facturación
- Cobros
- Apertura y cierre de caja
- Fidelización
- Calificación de servicio

---

# 9. BDD, Gherkin y JIRA

Cada funcionalidad debe estar respaldada por una historia de usuario en JIRA.

Los criterios de aceptación deben documentarse utilizando Gherkin.


---

# 10. GitFlow

El repositorio utiliza GitFlow.

## Ramas permanentes

```text
main
develop
```

### `main`

Contiene:

- versiones estables;
- entregas;
- código listo para producción.

No se desarrolla directamente sobre `main`.

### `develop`

Es la rama de integración del equipo.

Todas las funcionalidades nacen desde `develop`.

## Ramas temporales

```text
feature/*
release/*
hotfix/*
```

## Release

```text
develop
   ↓
release/1.0.0
   ↓
main
```

## Hotfix

```text
main
 ↓
hotfix/*
 ↓
main
 ↓
develop
```

---

# 11. Convención de commits

Se recomienda Conventional Commits.

```text
feat
fix
docs
test
refactor
chore
style
ci
```

Ejemplos:

```text
feat(auth): agregar autenticacion de dos factores
feat(mesas): implementar gestion de mesas
fix(comandas): corregir calculo del total
docs: actualizar instrucciones
ci: agregar pipeline de build
```

---

# 12. CI/CD

El proyecto debe utilizar un pipeline formal.

Archivo previsto:

```text
Jenkinsfile
```

Flujo general:

```text
Checkout
   ↓
Backend compile
   ↓
Backend tests
   ↓
Backend build
   ↓
Admin npm ci
   ↓
Admin build
   ↓
POS npm ci
   ↓
POS build
   ↓
Empaquetado / imágenes
   ↓
Despliegue
```

Las credenciales del despliegue deben almacenarse como secretos del proveedor o de Jenkins.


---

# 13. Despliegue

Para la entrega final deben encontrarse desplegados:

```text
Backend
Admin
POS
PostgreSQL
```

El backend dispone de perfiles:

```text
dev
prod
```

El pipeline deberá controlar el proceso de despliegue.

---

# 14. Documentación

```text
docs/
├── historias-usuario/
├── diagramas/
├── manual-tecnico/
└── manual-usuario/
```

Entregables documentales previstos:

- historias de usuario;
- criterios BDD;
- diagramas;
- Swagger/OpenAPI;
- manual técnico;
- manual de usuario.

---

# 15. Requisitos locales

Se recomienda:

```text
Java 21
Node.js 24+
npm 11+
Docker
Docker Compose
Git
```

Comprobar:

```bash
java -version
node -v
npm -v
docker --version
docker compose version
git --version
```

---

# 16. Clonar y preparar el proyecto

```bash
git clone <URL_DEL_REPOSITORIO>
cd restaurante-proyecto1-ayd1
git checkout develop
```

Backend:

```bash
cd restaurante-api
cp .env.example .env
docker compose up -d
set -a
source .env
set +a
./gradlew bootRun
```

Admin:

```bash
cd restaurante-admin
npm ci
npm start
```

POS:

```bash
cd restaurante-pos
npm ci
npm start
```

---

# 17. Reglas del repositorio

No subir:

```text
.env
node_modules/
dist/
build/
.gradle/
.angular/
.idea/
credenciales
contraseñas
JWT secrets
App Passwords
tokens
OTP
```

Sí subir:

```text
.env.example
migraciones Flyway
package-lock.json
gradle wrapper
README
documentación
Jenkinsfile
```

---

# 18. Flujo de trabajo para integrantes

Antes de iniciar:

```bash
git checkout develop
git pull origin develop
git checkout -b feature/AYD1-XX-descripcion
```

Antes de hacer Pull Request:

Backend:

```bash
./gradlew test
./gradlew build
```

Frontend:

```bash
npm run build
```

Después:

```bash
git add .
git commit -m "feat(modulo): descripcion"
git push -u origin feature/AYD1-XX-descripcion
```

Crear Pull Request hacia:

```text
develop
```

---

---

# 20. Equipo

Proyecto desarrollado por un equipo de 4 integrantes.

```text
1. Herberth Julian Reyes Pacajoj - 202230236
2. Kenny Marco Augusto López Salazar  - 202031554
3. Mario Raul Yancor Ocana - 201930761
4. Rony Mauricio Rojas Aguilar - 202031191
```

---

# 21. Estado del repositorio

Actualmente el repositorio se encuentra en la etapa de:

```text
ESQUELETO TÉCNICO FUNCIONAL
```

Ya se encuentran preparados:

- backend;
- base de datos inicial;
- autenticación;
- recuperación de contraseña;
- 2FA;
- roles;
- frontend administrativo;
- frontend POS;
- estructura para documentación;
- estrategia GitFlow.

A partir de este punto los módulos funcionales del restaurante deben desarrollarse mediante historias de usuario, ramas `feature/*`, Pull Requests, pruebas y CI/CD.
# restaurant-proyecto1-ayd1
