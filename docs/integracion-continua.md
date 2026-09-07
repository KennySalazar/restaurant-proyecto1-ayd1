# Integración continua con GitHub Actions

Esta etapa prepara CI; no habilita despliegues. Los archivos todavía deben publicarse
mediante una operación autorizada para que GitHub los ejecute.

## Eventos y ramas

El workflow `CI` corre en todos los pull requests, en pushes a `main` y `develop`,
y manualmente con `workflow_dispatch` cuando esté disponible en GitHub.
No filtra por archivos: un cambio documental también obtiene el resultado final,
evitando comprobaciones obligatorias permanentemente pendientes.

Se mantiene GitFlow: feature hacia develop, y promoción de una release hacia main.
Las referencias locales examinadas de main y develop divergen; no se realizó merge,
fetch ni modificación de ramas como parte de esta preparación.

## Trabajos

1. `Backend y PostgreSQL`: construye la etapa Java del Dockerfile, que ejecuta
   `check bootJar`; extrae los reportes JUnit/HTML; construye la imagen final con la
   caché del mismo runner y ejecuta `scripts/ci/backend_smoke.py`.
2. `Frontend (restaurante-admin)` y `Frontend (restaurante-pos)`: Node 24,
   npm 11.6.2, `npm ci`, pruebas sin watch y compilación de producción.
   Verifica que exista `dist/<proyecto>/browser/index.html` y guarda ese directorio.
3. `CI aprobada`: comprueba que backend y la matriz completa de frontend hayan pasado.
   Un fallo, cancelación o trabajo omitido no se considera éxito.

Los pasos que usan `tee` ejecutan Bash con `pipefail` mediante `shell: bash`,
de modo que guardar logs no oculta un comando fallido.

El script de backend genera credenciales de prueba en memoria y crea PostgreSQL 18
en una red Docker nueva, con datos en tmpfs. Comprueba todas las migraciones versionadas
del repositorio, el arranque en prod, readiness, OpenAPI, login, JWT, autorización y
preflight CORS. Reinicia el backend, vuelve a consultar el puerto efímero asignado por
Docker y verifica que el historial Flyway y la sesión sigan funcionando.
No carga `.env`, no usa Neon, ni llama a Brevo. No ejecutarlo con `python -O`, que
deshabilitaría las aserciones. Al finalizar detiene sus contenedores; no elimina recursos.
En ejecución local quedan contenedores detenidos y una red de prueba; en GitHub el
runner hospedado es temporal. Los volúmenes y contenedores de desarrollo no se tocan.

## Seguridad y evidencias

- Solo permiso `contents: read`, checkout sin credenciales persistentes.
- Evento `pull_request`, sin `pull_request_target` ni secretos de producción.
- Cada job tiene timeout; nuevos cambios cancelan una ejecución anterior del mismo evento/ref.
- Reportes conservados 14 días; builds Angular, 7 días. Guardar evidencias de la
  entrega antes de su vencimiento. Los logs de Actions muestran también los fallos
  de compilación o de tests ocurridos antes de poder extraer reportes del contenedor.
- El resumen de `CI aprobada` identifica el commit y los resultados.
- Artefactos Angular son evidencia de compilación; todavía contienen la configuración
  actual de API y no deben publicarse como frontends de producción sin adaptarla.

## Validación local

Desde la raíz:

```bash
docker build --target build -t restaurante-api:ci-tests restaurante-api
docker build -t restaurante-api:local-prod-check restaurante-api
python3 scripts/ci/backend_smoke.py
```

En cada frontend, con Node 24 y npm 11.6.2:

```bash
npm ci
npm test -- --watch=false
npm run build -- --configuration production
```

La sintaxis del workflow se puede validar con actionlint. Una validación local no
equivale a una ejecución remota de Actions: permisos, cuotas y carga de artefactos
se comprobarán tras el push autorizado.

## Resultado de la validación local — 5 de septiembre de 2026

- `actionlint 1.7.7` validó el workflow sin errores.
- Se extrajeron correctamente los reportes Java: 9 pruebas, 0 fallos y 0 errores.
- El script versionado de PostgreSQL/Docker pasó incluyendo el reinicio y las 8 migraciones.
- Ambos Angular pasaron `npm ci`, su prueba y su build de producción con Node 24.20.0
  y npm 11.6.2 en contenedores aislados. Se confirmó `browser/index.html` en ambos.
- Se comprobaron las 16 combinaciones de éxito/fallo/cancelación/omisión del control
  final: únicamente éxito de backend y frontend permite aprobarlo.
- npm informó 7 vulnerabilidades por frontend (3 moderadas y 4 altas) durante la
  instalación. No se cambiaron dependencias ni lockfiles y no se ejecutó `npm audit fix`.
  Esos avisos quedaron corregidos y validados el 7 de septiembre; consultar la
  [auditoría npm](auditoria-npm.md). Esta CI todavía no impone un umbral de auditoría.
- No hubo ejecución en GitHub, publicación de artefactos remotos, commit ni push.

## Activación posterior

1. Publicar los cambios y abrir el PR autorizado hacia develop.
2. Verificar una ejecución real en Actions y sus artefactos.
3. Según las opciones disponibles en el plan y visibilidad del repositorio, configurar
   protección de develop/main con PR y `CI aprobada` obligatoria.
4. Preparar CD para un push autorizado a main, vinculando todos los despliegues a
   verificaciones satisfactorias del mismo commit. Mantener auto-deploy independiente
   desactivado en los proveedores para no saltarse el pipeline.

Ni main ni develop despliegan con este archivo. El requisito completo de CI/CD queda
pendiente hasta implementar y validar CD; Jenkins no es necesario.

Referencia: [Sintaxis de GitHub Actions](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax).
