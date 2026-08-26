# SGR Client

Cliente web de SGR para autenticación, recuperación de cuentas, seguridad de sesión y acceso protegido por roles. Está construido con Angular 21, PrimeNG, Tailwind CSS y Transloco. La aplicación consume la API bajo el contexto `/api/v1`.

## Requisitos

- Node.js 24 y npm.
- El backend de `restaurante-api` ejecutándose en `http://localhost:8090` para desarrollo local.
- PostgreSQL y las variables de entorno del backend configuradas cuando se valide la aplicación contra la API real.
- Navegador moderno con soporte para `localStorage`.

Los comandos del cliente deben ejecutarse desde `restaurante-pos/` y `restaurante-admin/`. Las dependencias exactas se encuentran en `package-lock.json` y deben instalarse con `npm ci`.

## Configuración

La URL utilizada por la aplicación se define en los archivos de entorno:

- `src/environments/environment.ts`: utiliza `/api/v1` en desarrollo.
- `src/environments/environment.production.ts`: utiliza `/api/v1` en producción.
- `angular.json`: reemplaza el entorno de desarrollo por el de producción en `ng build`.

Durante `ng serve`, `proxy.conf.json` redirige las solicitudes `/api` hacia `http://localhost:8090`. Esto evita problemas de CORS sin exponer credenciales en el frontend. En producción, `/api/v1` debe estar disponible detrás del mismo origen web o de un reverse proxy.

La traducción se configura con Transloco y únicamente está habilitado el idioma español (`es`). El catálogo se encuentra en `public/i18n/es.json` y se carga desde `/i18n/es.json`.

El JWT se persiste junto con su tipo, fecha de expiración y usuario actual en `localStorage` bajo la clave `sgg.auth.session`. El interceptor agrega automáticamente el encabezado `Authorization: Bearer <token>` a las solicitudes protegidas. No se deben registrar ni incluir en el código contraseñas, OTP, tokens bearer o credenciales del correo.

La sesión se valida al iniciar la aplicación mediante `GET /api/v1/auth/me`. Una sesión inválida se elimina y redirige al inicio de sesión. Una sesión activa que intenta abrir cualquier ruta `/auth/**` se redirige al inicio común `/app/dashboard`.

## Transloco

Transloco centraliza los textos visibles de la aplicación para evitar cadenas hardcodeadas en los componentes y permitir agregar más idiomas en el futuro. Actualmente solo está habilitado el idioma español (`es`).

La configuración se encuentra en `src/app/app.config.ts`:

- `defaultLang` y `fallbackLang` están configurados como `es`.
- `TranslocoHttpLoader` carga el catálogo desde `/i18n/{lang}.json`.
- `public/i18n/es.json` contiene las traducciones agrupadas por funcionalidad.

Para utilizar una traducción en un componente standalone, importa `TranslocoPipe` y agrégalo a `imports`:

```typescript
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  imports: [TranslocoPipe],
})
export class ExampleComponent {}
```

Después usa la clave del catálogo en la plantilla:

```html
<h1>{{ 'auth.login.title' | transloco }}</h1>
<input [placeholder]="'auth.fields.emailPlaceholder' | transloco" />
```

Al agregar un texto nuevo, registra su clave en `public/i18n/es.json` y utiliza el pipe `transloco` en la plantilla. Los títulos, etiquetas, acciones, mensajes y textos de accesibilidad de la interfaz deben utilizar el catálogo en lugar de valores escritos directamente.

## Ejecución

Instala las dependencias:

```bash
npm ci
```

Inicia el servidor de desarrollo:

```bash
npm start
```

La aplicación estará disponible en `http://localhost:4200/`. El servidor utiliza el proxy configurado para comunicarse con la API en `http://localhost:8090/api/v1`.

Comandos de verificación:

```bash
npm run build
npx prettier --check "src/**/*.{ts,html,scss}" "public/i18n/es.json" "proxy.conf.json"
```

El build de producción genera los artefactos en `dist/sgg-client/`. Esta funcionalidad no agrega una suite de pruebas unitarias.

## Arquitectura

El cliente utiliza componentes standalone, rutas lazy-loaded y una separación entre infraestructura, layouts, páginas y componentes compartidos.

```text
src/
├── app/
│   ├── core/
│   │   ├── guards/        Guards de autenticación, guest y roles
│   │   ├── i18n/          Loader HTTP de Transloco
│   │   ├── interceptors/  Bearer JWT y manejo global de errores API
│   │   ├── models/        Contratos tipados de API, sesión y roles
│   │   └── services/      Sesión, autenticación y RFC 9457
│   ├── layouts/
│   │   ├── app-shell/     Layout protegido con navegación por rol
│   │   └── public-layout/ Layout para los flujos de autenticación
│   ├── pages/
│   │   ├── auth/          Login, OTP, recuperación y restablecimiento
│   │   ├── protected/     Dashboard, seguridad y administración
│   │   └── unauthorized/  Respuesta para rutas sin permisos
│   ├── shared/
│   │   └── components/    Marca, encabezados, errores y feedback reutilizable
│   ├── app.config.ts      Providers de Angular, HTTP, Transloco y PrimeNG
│   └── app.routes.ts      Rutas lazy-loaded y protección de navegación
├── environments/          Configuración de API por ambiente
├── index.html              Metadatos y título de CUNOC GYM
├── styles.scss             Estilos globales y componentes de formulario
└── main.ts                 Bootstrap de la aplicación
```

Los interceptors se registran en `app.config.ts`. `authInterceptor` agrega el bearer token y `apiErrorInterceptor` transforma los errores `application/problem+json`, muestra feedback global en español y maneja sesiones inválidas sin cerrar una sesión válida por errores de credenciales.

## Rutas y autorización

### Rutas públicas

- `/auth/login`: inicio de sesión con correo y contraseña.
- `/auth/login/verify`: verificación del OTP de inicio de sesión cuando 2FA está activo.
- `/auth/recovery`: solicitud de recuperación de contraseña.
- `/auth/recovery/reset`: restablecimiento con OTP y contraseña nueva.

Una sesión activa no puede permanecer en estas rutas y se redirige a `/app/dashboard`.

### Rutas protegidas

- `/app/dashboard`: inicio común para `ADMIN`, `TRAINER`, `RECEPTIONIST` y `MEMBER`.
- `/app/security`: cambio de contraseña y activación/desactivación de 2FA.
- `/app/admin`: comprobación de autorización mediante `/api/v1/admin/ping`; requiere el rol `ADMIN`.
- `/unauthorized`: respuesta para usuarios autenticados sin el rol requerido.

El `authGuard` protege el shell y sus hijos, el `guestGuard` evita el acceso de sesiones activas a rutas públicas y el `roleGuard` aplica los roles definidos en los metadatos de cada ruta. La API mantiene la autorización como segunda barrera.
