Restaurante POS

Aplicación web operativa / POS del Sistema de Gestión de Restaurante.

Está construida con Angular 21, PrimeNG, PrimeIcons, Tailwind CSS 4 y Transloco. Consume la misma API que la aplicación administrativa mediante /api/v1.

Roles permitidos

WAITER
KITCHEN
CASHIER

El rol ADMIN utiliza restaurante-admin.

Requisitos

Node.js 24+

npm 11+

restaurante-api ejecutándose en http://localhost:8090

Navegador moderno con soporte para localStorage y sessionStorage

Los comandos deben ejecutarse desde:

restaurante-pos/

Instalar dependencias:

npm ci

Configuración

En desarrollo:

src/environments/environment.ts

utiliza:

/api/v1

proxy.conf.json redirige /api hacia:

http://localhost:8090

La aplicación se ejecuta localmente en:

http://localhost:4201

Sesión

La sesión del POS es independiente de la sesión administrativa.

Clave utilizada en localStorage:

restaurante.pos.auth.session

Los challenges temporales de login y recuperación utilizan sessionStorage con claves propias del POS.

El interceptor agrega el Bearer JWT a solicitudes protegidas.

Transloco

Idioma actual:

es

Catálogo:

public/i18n/es.json

Los textos de la interfaz deben centralizarse en Transloco siempre que sea posible.

Arquitectura

La aplicación utiliza componentes standalone y rutas lazy-loaded.

src/
├── app/
│   ├── core/
│   │   ├── guards/        Auth, guest y roles
│   │   ├── i18n/          Loader de Transloco
│   │   ├── interceptors/  JWT y errores API
│   │   ├── models/        Contratos, sesión y roles
│   │   └── services/      Auth, sesión, flujo OTP y errores
│   ├── layouts/
│   │   ├── app-shell/     Layout operativo protegido
│   │   └── public-layout/ Layout de autenticación
│   ├── pages/
│   │   ├── auth/
│   │   │   ├── login/
│   │   │   ├── login-verify/
│   │   │   ├── recovery/
│   │   │   └── recovery-reset/
│   │   ├── protected/
│   │   │   ├── dashboard/
│   │   │   └── security/
│   │   └── unauthorized/
│   ├── shared/
│   │   └── components/
│   ├── app.config.ts
│   └── app.routes.ts
├── environments/
├── public/
│   └── i18n/
│       └── es.json
├── index.html
├── styles.scss
└── main.ts

La estructura crecerá conforme se implementen Mesas, Comandas, Cocina y Caja.

Rutas públicas

/auth/login
/auth/login-verify
/auth/recovery
/auth/recovery-reset

Rutas protegidas actuales

/app/dashboard
/app/account
/unauthorized

El shell protegido acepta exclusivamente:

WAITER
KITCHEN
CASHIER

Funcionalidad de autenticación implementada

✅ Login
✅ Login con 2FA
✅ OTP por correo
✅ Recuperación de contraseña
✅ Restablecimiento de contraseña
✅ Cambio de contraseña
✅ Activar 2FA
✅ Desactivar 2FA
✅ Sesión JWT independiente
✅ Guards por rol
✅ Manejo global de errores

Módulos operativos planificados

WAITER

mesas;

apertura de cuentas;

comandas;

detalle de órdenes;

modificadores.

KITCHEN

recepción de comandas;

preparación;

cambio de estados;

órdenes listas.

CASHIER

facturación;

cobros;

apertura de caja;

cierre de caja.

Funciones compartidas

fidelización;

calificación del servicio.

Ejecución

npm ci
npm start

URL:

http://localhost:4201

Build

npm run build

Salida:

dist/restaurante-pos

Verificación de formato

npx prettier --check "src/**/*.{ts,html,scss}" "public/i18n/es.json" "proxy.conf.json"

Antes de un Pull Request

npm run build

No versionar:

node_modules/
dist/
.angular/
