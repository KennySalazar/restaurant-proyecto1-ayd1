Restaurante Admin

Aplicación web administrativa del Sistema de Gestión de Restaurante.

Está construida con Angular 21, PrimeNG, PrimeIcons, Tailwind CSS 4 y Transloco. Consume la API compartida bajo /api/v1.

Rol permitido

ADMIN

Los roles operativos WAITER, KITCHEN y CASHIER utilizan restaurante-pos.

Requisitos

Node.js 24+

npm 11+

restaurante-api ejecutándose en http://localhost:8090

Navegador moderno con soporte para localStorage y sessionStorage

Los comandos deben ejecutarse desde:

restaurante-admin/

Instalar dependencias exactas:

npm ci

Configuración

En desarrollo:

src/environments/environment.ts

utiliza:

/api/v1

proxy.conf.json redirige /api hacia:

http://localhost:8090

La aplicación se ejecuta localmente en:

http://localhost:4200

Sesión

El JWT se almacena en localStorage utilizando la clave:

restaurante.admin.auth.session

Los challenges temporales de autenticación y recuperación utilizan sessionStorage.

El interceptor agrega automáticamente:

Authorization: Bearer <token>

a solicitudes protegidas.

No se deben guardar ni registrar contraseñas, OTP o tokens en el código.

Transloco

Idioma actual:

es

Catálogo:

public/i18n/es.json

Los textos visibles deben agregarse al catálogo siempre que sea posible.

Ejemplo:

<h1>{{ 'auth.login.title' | transloco }}</h1>

Arquitectura

La aplicación utiliza componentes standalone y rutas lazy-loaded.

src/
├── app/
│   ├── core/
│   │   ├── guards/        Autenticación, guest y roles
│   │   ├── i18n/          Loader de Transloco
│   │   ├── interceptors/  JWT y errores API
│   │   ├── models/        Contratos, sesión y roles
│   │   └── services/      Auth, sesión, flujo OTP y errores
│   ├── layouts/
│   │   ├── app-shell/     Layout administrativo protegido
│   │   └── public-layout/ Layout de autenticación
│   ├── pages/
│   │   ├── auth/
│   │   │   ├── login/
│   │   │   ├── login-verify/
│   │   │   ├── recovery/
│   │   │   └── recovery-reset/
│   │   ├── protected/
│   │   │   ├── dashboard/
│   │   │   ├── security/
│   │   │   └── admin/
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

Rutas públicas

/auth/login
/auth/login-verify
/auth/recovery
/auth/recovery-reset

Una sesión autenticada no debe permanecer en las rutas públicas.

Rutas protegidas actuales

/app/dashboard
/app/security
/app/admin
/unauthorized

El shell administrativo está restringido a:

ADMIN

/app/admin utiliza /api/v1/admin/ping como comprobación de autorización administrativa.

Funcionalidad de autenticación implementada

✅ Login
✅ Login con 2FA
✅ OTP por correo
✅ Recuperación de contraseña
✅ Restablecimiento de contraseña
✅ Cambio de contraseña
✅ Activar 2FA
✅ Desactivar 2FA
✅ Manejo de JWT
✅ Guards por rol
✅ Manejo global de errores API

Módulos administrativos pendientes

Entre los módulos del proyecto se desarrollarán:

insumos;

recetas;

platillos;

categorías;

modificadores;

inventario;

kardex;

mesas;

empleados;

configuración;

reservas;

lista de espera;

ocupación;

fidelización;

reportes.

Ejecución

npm ci
npm start

URL:

http://localhost:4200

Build

npm run build

Salida:

dist/restaurante-admin

Verificación de formato

npx prettier --check "src/**/*.{ts,html,scss}" "public/i18n/es.json" "proxy.conf.json"

Antes de un Pull Request

npm run build

No versionar:

node_modules/
dist/
.angular/
