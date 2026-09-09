# Punto de continuación del despliegue

## Última continuación: CD local — 9 de septiembre de 2026

Esta sección prevalece sobre TODAS las notas anteriores conservadas abajo.

- PR #5 y #6 fusionados con autorización; main en `11e000b` y CI 34314981825 aprobada.
- Admin y POS publicados por el usuario mediante Direct Upload en Cloudflare Pages:
  `https://restaurante-admin-ayd1.pages.dev` y `https://restaurante-pos-ayd1.pages.dev`.
- CORS real configurado; usuario confirmó login/dashboard Admin. POS rechaza ADMIN
  por rol y el usuario pidió posponer la cuenta operativa. No implementar ese CRUD ahora.
- Backend Render, Neon con ocho migraciones y correo Brevo ya probados.
- Rama local `feature/despliegue-continuo` desde develop. Workflow extendido,
  `scripts/ci/deploy.py`, pruebas simuladas y `docs/despliegue-continuo.md` preparados.
- Nueve pruebas de automatización y actionlint aprobados. No despliegues de prueba.
- CD requiere cuatro secretos de proveedor y `PRODUCTION_DEPLOY_ENABLED=true`;
  todavía no se ha configurado ni probado en remoto. Render Auto-Deploy sigue Off.
- El usuario autorizó commit y push de los ocho archivos de esta preparación en
  `feature/despliegue-continuo` y preparar el PR hacia develop. Merge, activación de
  CD y despliegue siguen pendientes de autorización. Comprobar el PR y su CI al retomar.
- Sigue pendiente la limpieza de autoconfiguración de usuario de Spring Security,
  prueba de POS operativo, flujo completo de 2FA y documentación final de entrega.

Para retomar: git status, leer esta sección y la guía de CD. No leer `.env` ni secretos.

## Actualización al 9 de septiembre de 2026

Esta sección prevalece sobre las notas históricas siguientes:

- PR #3 fusionado en develop (`aace07c`), PR #4 fusionado en main (`9ff594f`),
  con autorización del usuario. Las CI de ambos PR y de main aprobaron.
- Backend desplegado por el usuario en Render Free:
  `https://restaurant-proyecto1-ayd1.onrender.com`.
- El primer arranque falló por credenciales de Neon. El usuario las corrigió;
  Flyway aplicó las ocho migraciones y Render indicó Live.
- Verificado por HTTPS: readiness 200/UP, OpenAPI 200 y `/auth/me` anónimo 401.
- El usuario verificó login 200, token JWT y `/auth/me` con ADMIN habilitado.
  También confirmó recepción real del correo de recuperación enviado por Brevo.
  No se completó el cambio de contraseña ni se probó el flujo completo de 2FA.
- Pendiente eliminar la autoconfiguración de usuario en memoria de Spring Security
  que imprime una contraseña generada. No copiar logs con ese valor a documentos.
- Preparación local de Pages en `feature/frontends-cloud` desde develop:
  URL de producción real en ambos Angular y guía `docs/despliegue-frontends.md`.
  El usuario autorizó commit, push y preparación del PR hacia develop para estos
  ocho archivos. Merge y publicación en Pages siguen pendientes de autorización.
  Las pruebas y builds de ambos frontend pasaron y se verificó la URL en los bundles.
  Los cuatro paquetes con avisos npm detectados al preparar Pages se corrigieron:
  Vitest y asociados 4.1.11, Hono 4.13.7 y js-yaml 4.3.2. La corrección conserva
  el resto de versiones. Ver validación en la guía de frontend y comprobar el estado
  efectivo de la rama y del PR en GitHub al retomar.
- `docs/Render Dashboard.pdf` es un archivo del usuario, no versionado. Conservarlo
  y no incorporarlo por accidente a un commit ni extraer posibles secretos.
- Faltan publicar Pages, configurar CORS con sus URL y completar CD para backend
  y ambos frontend. Auto-Deploy de Render se configuró en Off según el usuario.

## Notas históricas de preparación

Guardado inicialmente el 5 de septiembre de 2026 y actualizado el 7 de septiembre.
El usuario pidió conservar un punto seguro para retomar el trabajo.

## Leer primero al retomar

1. Ejecutar `git status` y respetar todos los cambios existentes.
2. Leer este archivo, `docs/despliegue-backend.md`, `docs/integracion-continua.md`,
   el enunciado completo (`docs/Proyecto 1 AyD1.txt` y PDF) y los cuatro README.
3. Los archivos nuevos y modificados son trabajo guardado de esta preparación.
   No descartarlos, reemplazarlos ni asumir que falta implementarlos por no tener commit.

## Autorizaciones y límites

- Se autorizó preparar y probar localmente backend, Docker y CI.
- El 7 de septiembre se autorizó crear `feature/preparacion-despliegue-ci`, hacer
  commit y push de la preparación y preparar el PR hacia `develop`.
- Merge y despliegues remotos siguen sin autorizarse; solicitar aprobación explícita.
- No abrir `.env`, ni leer o exponer contraseñas, tokens o claves API.
- El usuario configura sus valores privados en los paneles; pedir solo nombres y propósitos.
- No eliminar archivos ni usar comandos destructivos. No editar migraciones ya aplicadas.
- Verificar requisitos actuales mediante documentación oficial de los proveedores.
- Trabajar una etapa a la vez y explicar en español sencillo qué está pasando.

## Arquitectura acordada

- Admin y POS Angular: Cloudflare Pages, aún sin crear/desplegar.
- Backend Java 21 / Spring Boot 4.1.0, Gradle Wrapper 9.5.1, Docker: Render Free.
- Render debe quedar en Ohio para estar próximo a Neon.
- PostgreSQL 18 en Neon: proyecto `restaurante-ayd1`, AWS `us-east-2` (Ohio),
  rama Neon `production`, base `neondb`. Creado por el usuario, sin migrar todavía.
  La conexión directa con TLS queda pendiente de validación real.
- Brevo HTTPS para recuperación y OTP/2FA: el usuario creó la cuenta, verificó su
  remitente Gmail y generó una clave API privada. No se conoce ni se necesita conocer
  esa clave. No tienen dominio propio. Entrega real y habilitación transaccional
  siguen sin comprobarse; verificar el remitente no garantiza que el envío funcione.
- Cloudinary después, al desarrollar CRUD de platillos; no integrar ahora.
- GitHub Actions para CI/CD. Jenkins no es obligatorio según la sección Despliegue
  del enunciado. Q0 es el objetivo, sujeto a cuotas y validación de los proveedores.

## Estado de Git

La preparación partió de `develop`. La rama de publicación autorizada es
`feature/preparacion-despliegue-ci`; comprobar Git y GitHub al retomar para conocer
el estado efectivo del commit, push, PR y CI. Al auditar, `main` tenía
un commit de merge exclusivo y `develop` once exclusivos. No se hizo fetch ni se
verificaron permisos o protecciones remotas; no asumir que esas referencias están
actualizadas. No cambiar ramas ni mezclar historiales sin revisar primero.

Existe contenido local de herramientas en `.github/modernize/` ajeno al workflow.
No incluirlo por accidente al preparar un futuro commit: seleccionar archivos explícitos.

## Implementado y guardado

1. Brevo HTTPS seleccionable mediante `EmailService`, con contenido OTP compartido
   con SMTP, tiempos de espera y errores sin cuerpo sensible. SMTP sigue siendo
   predeterminado local; `prod` selecciona Brevo. Hay cuatro pruebas simuladas.
2. Perfil de producción: `PORT`, escucha en todas las interfaces, pool pequeño de
   conexiones, logs sin SQL, apagado ordenado y variables privadas.
3. CORS con orígenes explícitos, vacío por defecto y tres pruebas. Faltan las URL
   finales de ambos Pages. No se admiten comodines.
4. Actuator: readiness público en `/api/v1/actuator/health/readiness`, sin detalles.
   No consulta la BD constantemente; comprobar login también para validar conexión.
5. Dockerfile de dos etapas Java 21: ejecuta `check bootJar`, luego JRE con UID 10001
   y heap máximo del 50% de memoria. `.dockerignore` permite solo fuentes y Gradle.
6. Compose ahora toma `DATABASE_PASSWORD` en lugar de la contraseña literal anterior.
   No se alteró la base local. El historial Git anterior puede conservar esa credencial;
   no mostrar diffs con valores antiguos ni reutilizarla en producción.
7. `.gitignore` protege variantes de `.env` y evidencias locales; `.env.example`
   enumera también las variables de correo/CI sin valores reales.
8. `.github/workflows/ci.yml`: CI para todos los PR y pushes a develop/main,
   ejecución manual, backend Docker/PostgreSQL y matriz Angular Node 24/npm 11.6.2.
   Genera evidencias y el control final `CI aprobada`. SIN pasos de despliegue.
9. `scripts/ci/backend_smoke.py`: prueba reproducible con PostgreSQL 18 aislado,
   credenciales generadas, migraciones, login/JWT, CORS y reinicio. No usa nube ni `.env`.
10. README raíz corregido respecto de Jenkins y estado CI; documentación detallada
    en las dos guías enlazadas arriba.

## Pruebas terminadas (no repetir sin motivo)

- `check bootJar` dentro de Docker: 9 pruebas Java aprobadas y reportes extraídos.
- actionlint 1.7.7: workflow válido.
- Admin y POS: instalación limpia `npm ci`, una prueba por frontend y build de
  producción aprobados en Node 24.20.0 / npm 11.6.2. Confirmado `browser/index.html`.
- Backend en 512 MiB / 0,5 CPU: PostgreSQL 18 vacío, ocho migraciones V1–V8 exitosas,
  login/bootstrap/JWT, rutas protegidas, Swagger y preflight CORS correctos.
- Reinicio: mismo historial Flyway, sin duplicación, JWT anterior todavía válido.
- La prueba inicial del reinicio consultó un puerto efímero antiguo; se corrigió el
  script para releer el puerto tras `docker restart` y la prueba completa pasó.
- El primer contenedor Node de validación tenía tmpfs sin permiso de ejecución;
  se corrigió ese montaje y ambos frontend pasaron. No fue un error del frontend.
- Control `CI aprobada`: las 16 combinaciones de resultados comprobadas; solo pasa
  cuando backend y frontend están en success.
- Compose valida sin abrir `.env` usando un valor ficticio; sin DATABASE_PASSWORD falla.
- `git diff --check` correcto. V1–V8 sin cambios.

Las pruebas de integración quedaron detenidas. La última consulta de `docker ps`
no mostró contenedores en ejecución. No se borraron contenedores, redes ni volúmenes.
Los artefactos y recursos de prueba locales pueden permanecer y no deben publicarse.

## Avisos npm corregidos — 7 de septiembre

El usuario autorizó corregir localmente las dependencias. Los siete paquetes
señalados pertenecían al árbol de desarrollo; la auditoría sin dev daba cero.
Se actualizaron `package.json` y `package-lock.json` de ambos frontend, conservando
los cambios previos. Angular/core/compiler 21.2.22, CLI/build 21.2.21, Tailwind 4.3.2,
PostCSS 8.5.23 y dependencias transitivas corregidas. No se utilizó force,
legacy-peer-deps, overrides ni audit fix.

Ambos frontend pasaron instalación limpia, auditoría completa con **cero
vulnerabilidades**, una prueba cada uno y compilación de producción en Docker,
Node 24.20.0/npm 11.6.2. Confirmado browser/index.html en ambos. El contenedor terminó
con código 0. Los node_modules locales no se actualizaron; antes de desarrollar
con estas versiones, ejecutar npm ci en cada frontend.

Detalles, alcance y fuentes: [auditoría npm](auditoria-npm.md).
Se autorizó publicar esta preparación mediante rama, commit, push y PR hacia
`develop`. El próximo paso es completar esa publicación y comprobar CI en GitHub.
Merge y despliegue siguen pendientes de autorización.

## Después, en orden

1. Presentar los cambios locales concretos y solicitar autorización para su publicación
   mediante rama/commit/push/PR conforme a GitFlow. No hacer push directo a main.
2. Comprobar CI realmente en GitHub; hoy solo se validó localmente. Verificar permisos,
   plan/visibilidad, artefactos y protecciones con `CI aprobada` obligatoria.
3. Promover una versión validada mediante PR a main con autorización correspondiente.
4. Configurar Render Free (Ohio, Docker, root `restaurante-api`) con las variables
   documentadas. Crear el servicio dispara el primer despliegue: pedir autorización
   antes de ese paso. Mantener auto-deploy independiente en Off.
5. Comprobar backend → Neon (TLS y migraciones) y Brevo real con una cuenta del equipo.
   Flyway solo al arrancar el backend; no duplicarlo con un migrador del pipeline.
6. Configurar las URL de API en ambos Angular, publicar Pages y ajustar CORS real.
7. Implementar CD: push autorizado a main → todas las verificaciones del mismo commit
   → despliegue. Si fallan, no desplegar. Los PR nunca despliegan.
8. Prueba completa, evidencias y documentación técnica/usuario.

**CI actual no despliega al hacer merge a main. CD todavía no está implementada.**
Neon está provisionado; el backend y los frontend siguen sin publicar.

## Apagar y retomar

Los archivos están guardados en disco aunque todavía no estén en GitHub. No hay
que dejar la terminal, la sesión del asistente o los contenedores de prueba abiertos.
Apagar la PC no elimina esos archivos ni los proyectos creados en los proveedores.
Guardar la clave de Brevo en un lugar privado y seguro, no en este documento.
Al abrir otra terminal para desarrollo local hay que volver a cargar las variables
y arrancar PostgreSQL/backend si se necesitan; ver README de la API.

Mensaje para la próxima sesión:

> Continúa el despliegue leyendo primero docs/CONTINUAR-DESPLIEGUE.md.
> Conserva todos los cambios locales. No hagas commit, push, merge ni despliegue sin
> mi autorización explícita. Los avisos npm ya están corregidos; revisa los cambios
> preparados y el siguiente paso para publicarlos mediante GitFlow.
