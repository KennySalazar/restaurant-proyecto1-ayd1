# Despliegue continuo: Render y Cloudflare Pages

## Estado al 9 de septiembre de 2026

Implementación en `feature/despliegue-continuo`, con commit y push autorizados para
preparar el PR hacia develop. Merge y activación todavía pendientes de autorización.
Las aplicaciones ya se publicaron manualmente; falta validar una ejecución real de CD.
La cuenta operativa del POS queda pendiente por decisión del usuario.

## Flujo

1. PR, develop y ejecución manual: únicamente CI, sin secretos de producción.
2. Push autorizado a main: backend, ambos Angular y pruebas de automatización.
3. `CI aprobada` exige éxito de todos los trabajos anteriores.
4. Solo si la variable `PRODUCTION_DEPLOY_ENABLED` vale exactamente `true`,
   el trabajo de producción descarga los builds Angular del mismo intento de CI.
5. Verifica configuración de los proveedores, artefactos y que main siga en ese SHA.
6. Solicita a Render desplegar ese commit concreto y espera Live y readiness UP.
7. Publica Admin y POS secuencialmente con Wrangler 4.130.0, usando los builds
   descargados. Verifica el commit y éxito del despliegue canónico de producción.

No hay migrador adicional: Flyway sigue ejecutándose únicamente al arrancar Spring.
Render reconstruye el Dockerfile del mismo commit; no recibe la imagen del runner.
Las imágenes base no están fijadas por digest, por lo que no se garantiza identidad
binaria entre ambas construcciones.

## Configuración privada pendiente

En GitHub → repositorio → Settings → Secrets and variables → Actions → Secrets,
el propietario configura estos **Repository secrets**, sin compartir sus valores:

| Nombre | Propósito |
|---|---|
| `RENDER_API_KEY` | Autorizar consultas y despliegues mediante la API de Render. |
| `RENDER_SERVICE_ID` | Identificar exclusivamente el Web Service existente. |
| `CLOUDFLARE_API_TOKEN` | Permiso Account / Cloudflare Pages / Edit limitado a la cuenta del proyecto. |
| `CLOUDFLARE_ACCOUNT_ID` | Identificar la cuenta que contiene ambos Pages. |

El token de GitHub lo proporciona Actions automáticamente. No trasladar contraseñas
de Neon, JWT, administrador ni clave Brevo a este pipeline: permanecen en Render.

Comprobar antes de activar:

- Render usa la rama main y Auto-Deploy Off.
- Ambos proyectos Pages existentes se llaman `restaurante-admin-ayd1` y
  `restaurante-pos-ayd1`, con rama de producción main. El script no crea proyectos
  ni cambia esa configuración automáticamente; si difiere, falla antes de desplegar.
- CORS en Render contiene los dos orígenes `.pages.dev` ya probados.
- Proteger main/develop con PR y `CI aprobada` obligatoria según los permisos
  disponibles. Este workflow no configura las protecciones del repositorio.

Después de revisar y autorizar la activación, crear en la pestaña **Variables** la
Repository variable `PRODUCTION_DEPLOY_ENABLED=true`. Sin ella CD queda omitida.
Cambiar la variable no inicia una publicación por sí mismo: se necesita un push
autorizado a main o reejecutar todos los trabajos de su ejecución push vigente.
`workflow_dispatch` solo verifica CI.

## Publicación y evidencia

Publicar esta preparación únicamente tras autorización: feature → PR a develop,
CI aprobada → release → PR a main. No hacer push directo a main.
La activación y el primer despliegue remoto también deben autorizarse.

Guardar el enlace de Actions, SHA, resumen y artefacto `production-report-*`.
El informe registra estados e identificadores, sin respuestas completas de proveedores
ni salida de Wrangler. Se conserva 14 días; exportarlo para la entrega.
Si falla la instalación o descarga previa al script, la evidencia estará en el log
del paso y puede no existir el informe JSON.

Tras la primera ejecución real, comprobar login Admin, recarga de rutas, recuperación
y POS con una cuenta operativa cuando esté disponible. Readiness UP por sí solo no
comprueba la conexión actual a Neon ni el envío de correo.

## Fallos y reintentos

Producción se serializa y no cancela automáticamente una publicación en curso.
GitHub puede reemplazar ejecuciones pendientes por otras más recientes. El script
rechaza commits antiguos antes de empezar Render y antes de cada Pages; no bloquea
que main avance durante una petición ya iniciada.

Los tres despliegues no son una transacción: Render o Admin pueden haber quedado
actualizados cuando falla el siguiente paso. No hay rollback automático ni reversión
de Flyway. Revisar los paneles y el informe antes de autorizar un reintento. Cancelar
Actions o agotar su tiempo no cancela una solicitud ya aceptada por el proveedor.
Mantener migraciones compatibles con la versión anterior de la aplicación.

Usar **Re-run all jobs**, no solo los fallidos: los artefactos incluyen el número de
intento y solo se publican builds de ese mismo intento. Un reintento puede volver
a desplegar el mismo SHA; no vuelve a aplicar migraciones ya registradas.

## Validación local

`python3 -B -m unittest discover -s scripts/ci -p 'test_*.py' -v`

Nueve pruebas con proveedores simulados aprobadas: éxito, aislamiento de credenciales,
rechazo de PR/develop, secreto faltante, rama incorrecta, commit antiguo, fallo o SHA
incorrecto de Render, fallo de Pages, timeout y errores HTTP sin contenido sensible.
Actionlint 1.7.7 aprobó el workflow. No se contactaron cuentas privadas ni se desplegó
durante esas pruebas. La validación real de permisos y APIs queda pendiente.

## Referencias oficiales

- [Render: desplegar un commit](https://api-docs.render.com/reference/create-deploy)
- [Render: consultar despliegue](https://api-docs.render.com/reference/retrieve-deploy)
- [Cloudflare: Direct Upload desde CI](https://developers.cloudflare.com/pages/how-to/use-direct-upload-with-continuous-integration/)
- [Cloudflare: consultar proyecto](https://developers.cloudflare.com/api/resources/pages/subresources/projects/methods/get/)
