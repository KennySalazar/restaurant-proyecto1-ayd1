# Frontend en Cloudflare Pages

## Estado

Publicados manualmente tras los PR #5 y #6: `https://restaurante-admin-ayd1.pages.dev`
y `https://restaurante-pos-ayd1.pages.dev`. El usuario confirmó login y dashboard
Admin; POS carga y rechaza al ADMIN por rol. Cuenta operativa y su prueba pendientes.
CORS de ambos orígenes ya configurado. CD está preparada localmente, pendiente de
publicación y activación: [guía de CD](despliegue-continuo.md).
Las instrucciones siguientes conservan el procedimiento de publicación inicial.
Ambos `environment.production.ts` apuntan a:

`https://restaurant-proyecto1-ayd1.onrender.com/api/v1`

Desarrollo conserva `/api/v1` y los proxies locales. La URL del backend es pública;
no se incluyen credenciales de Neon, Brevo ni secretos JWT en Angular.

## Publicación prevista

Crear dos proyectos Pages independientes. Se recomienda Direct Upload para subir
los artefactos compilados y aprobados por GitHub Actions. Así no existe otro flujo
de publicación automático que despliegue antes de que termine la CI.
Direct Upload no permite cambiar después a integración Git en el mismo proyecto.
La creación y publicación de los proyectos requiere autorización del usuario.

| Aplicación | Directorio que se publicará |
|---|---|
| Admin | `restaurante-admin/dist/restaurante-admin/browser` |
| POS | `restaurante-pos/dist/restaurante-pos/browser` |

Compilar con Node 24 y npm 11.6.2: `npm ci` y
`npm run build -- --configuration production` en cada frontend.
La CI existente ya compila y conserva esos directorios como artefactos.
Los pasos de CD están preparados después del control de éxito de todos los jobs,
solo para producción en main, pendientes de activación. Los PR no despliegan.

Para CD, el usuario configurará en GitHub Secrets `CLOUDFLARE_API_TOKEN` (permiso
de edición de Pages limitado a su cuenta) y `CLOUDFLARE_ACCOUNT_ID` (identificador
de la cuenta). Los proyectos son `restaurante-admin-ayd1` y `restaurante-pos-ayd1`.
No se requieren secretos del backend en Cloudflare Pages.

## Navegación y conexión

Pages admite rutas de SPA automáticamente si no existe un `404.html` en la raíz.
No se agrega uno. Probar la entrada directa y recarga de `/auth/login`.

Cuando existan las dos URL reales, configurar en Render `CORS_ALLOWED_ORIGINS`
con ambos orígenes HTTPS separados por coma, sin rutas, comodines ni barra final.
Guardar esa variable con redeploy requiere autorización. Hasta entonces el navegador
no podrá completar las llamadas entre Pages y la API, aunque el frontend cargue.

Completar las pruebas de recuperación y recarga de rutas desde los sitios publicados.
Login Admin ya fue confirmado por el usuario; acceso POS con rol operativo pendiente.

## Validación

Validación local completada el 9 de septiembre: instalación limpia, una prueba
por frontend y builds de producción aprobados en Node 24/npm 11.6.2. Ambos builds
contienen la URL real de Render y browser/index.html, sin 404.html en la raíz.
La primera instalación informó cuatro paquetes afectados por avisos npm
(3 moderados, 1 alto): vitest/@vitest/mocker, hono y js-yaml. Este último también
aparecía en la auditoría sin dependencias de desarrollo; eso por sí solo no demuestra
que el código vulnerable esté incluido o sea alcanzable en el bundle del navegador.

Se corrigieron ambos proyectos con una actualización acotada:

- Vitest y sus paquetes asociados: 4.1.10 → 4.1.11.
- Hono: 4.13.0 → 4.13.7.
- js-yaml: 4.3.1 → 4.3.2.

Se conservaron las demás versiones del lockfile. No se usó `--force`,
`--legacy-peer-deps` ni `npm audit fix`. La resolución temporal reportó cero avisos.

Validación final con Node 24.20.0/npm 11.6.2 en Docker: `npm ci`, `npm audit` y
`npm audit --omit=dev` aprobados sin vulnerabilidades en ambos proyectos; una
prueba por frontend y ambos builds de producción aprobados. Se confirmó la URL de
Render dentro de los bundles y el directorio browser con index.html, sin 404.html.
El contenedor terminó con código 0. Esto no sustituye la prueba en navegador desde
Pages, que requiere la publicación y los orígenes CORS reales.

## Referencias oficiales

- [Angular](https://developers.cloudflare.com/pages/framework-guides/deploy-an-angular-site/)
- [Direct Upload con CI](https://developers.cloudflare.com/pages/how-to/use-direct-upload-with-continuous-integration/)
- [Rutas SPA](https://developers.cloudflare.com/pages/configuration/serving-pages/)
- [Límites Free](https://developers.cloudflare.com/pages/platform/limits/)
- [Aviso de Vitest](https://github.com/vitest-dev/vitest/security/advisories/GHSA-82fw-gwwq-j7x9)
- [Aviso de js-yaml](https://github.com/nodeca/js-yaml/security/advisories/GHSA-2883-xcg3-v3hh)
- [Aviso de Hono](https://github.com/honojs/hono/security/advisories/GHSA-gqvv-2mrq-wpjv)
