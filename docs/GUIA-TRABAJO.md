# Guía de desarrollo en Linux — Restaurante AyD1

  Esta guía explica cómo preparar el proyecto, ejecutarlo localmente y trabajar en equipo hasta publicar cambios.

  Cada integrante utiliza su propia base de datos local. Neon se utiliza para producción.

  ## 1. Versiones necesarias

   Herramienta       Versión
  ━━━━━━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Java JDK          21
  ────────────────  ───────────────────────────────────────────────────────
   Node.js           24.x
  ────────────────  ───────────────────────────────────────────────────────
   npm               11.6.2
  ────────────────  ───────────────────────────────────────────────────────
   Docker Engine     Versión compatible con Docker Compose v2
  ────────────────  ───────────────────────────────────────────────────────
   Docker Compose    v2, comando docker compose
  ────────────────  ───────────────────────────────────────────────────────
   Git               2.x
  ────────────────  ───────────────────────────────────────────────────────
   PostgreSQL        18, lo descarga Docker Compose
  ────────────────  ───────────────────────────────────────────────────────
   Gradle            9.5.1, incluido mediante el Wrapper
  ────────────────  ───────────────────────────────────────────────────────
   Angular           21.2.x, instalado desde las dependencias del proyecto

  No es necesario instalar PostgreSQL, Gradle ni Angular CLI globalmente. Los lockfiles determinan las versiones exactas de los
  frontend.

  Puedes comprobar tu entorno con:

  java -version
  javac -version
  node --version
  npm --version
  docker version
  docker compose version
  git --version

  Docker debe estar funcionando antes de levantar la base de datos.

  ## 2. Clonar el repositorio

  En una terminal, entra a la carpeta donde guardarás tus proyectos:

  git clone https://github.com/KennySalazar/restaurant-proyecto1-ayd1.git
  cd restaurant-proyecto1-ayd1

  Cambia a la rama de integración del equipo:

  git switch develop
  git pull --ff-only origin develop

  Revisa tu estado:

  git status

  Para abrirlo en VS Code, si utilizas ese editor:

  code .

  La estructura principal es:

  restaurant-proyecto1-ayd1/
  ├── restaurante-api/       # Backend Spring Boot
  ├── restaurante-admin/     # Frontend administrativo
  ├── restaurante-pos/       # Frontend operativo
  ├── docs/                  # Documentación
  └── .github/workflows/     # CI/CD

  ## 3. Crear tu rama de trabajo

  Antes de modificar código, crea una rama desde develop actualizado:

  git switch -c feature/descripcion-de-tu-tarea

  Por ejemplo:

  git switch -c feature/crud-proveedores

  Comprueba la rama actual:

  git branch --show-current

  Una rama debe concentrarse en una tarea o historia de usuario. Evita incluir cambios ajenos al objetivo del PR.

  ## 4. Colocar el .env

  El responsable del proyecto compartirá el archivo de configuración local.

  Guárdalo exactamente aquí:

  restaurant-proyecto1-ayd1/restaurante-api/.env

  Debe llamarse .env, sin extensión .txt. No lo coloques dentro de los frontend.

  Comprueba que incluya estas configuraciones locales:

  SPRING_PROFILES_ACTIVE=dev
  DATABASE_URL=jdbc:postgresql://localhost:5437/restaurante_db
  CORS_ALLOWED_ORIGINS=http://localhost:4200,http://localhost:4201

  Las demás variables serán las incluidas en el archivo compartido.

  ## 5. Instalar dependencias de ambos frontend

  Desde la raíz del repositorio:

  cd restaurante-admin
  npm install

  Después:

  cd ../restaurante-pos
  npm install

  Regresa a la raíz:

  cd ..

  npm ci instala las versiones del lockfile. Repítelo en el frontend correspondiente cuando recibas cambios en sus dependencias.

  El backend descargará sus dependencias mediante Gradle al ejecutarse por primera vez.

  ## 6. Levantar PostgreSQL y el backend

  Utiliza una terminal exclusiva para el backend.

  Desde la raíz:

  cd restaurante-api

  Carga las variables del archivo compartido:

  set -a
  source .env
  set +a

  Esto exporta las variables en esta terminal y para los programas que ejecutes desde ella. Debes repetirlo cuando abras otra terminal
  para el backend o cambies el .env.

  Levanta PostgreSQL:

  docker compose up -d

  Comprueba su estado:

  docker compose ps

  Ahora inicia Spring Boot:

  ./gradlew bootRun

  Si el Wrapper muestra Permission denied:

  chmod +x gradlew
  ./gradlew bootRun

  La primera ejecución puede tardar por la descarga de Gradle y sus dependencias.

  Al arrancar:

  - Flyway aplica las migraciones pendientes en tu PostgreSQL local.
  - El backend utiliza el perfil dev.
  - La API escucha en el puerto 8090.
  - El contexto de las rutas es /api/v1.

  Deja esta terminal abierta.

  Docker Compose levanta PostgreSQL; bootRun levanta el backend. Son dos procesos diferentes.

  ## 7. Levantar Admin

  Abre una segunda terminal, ubicada en la raíz del repositorio:

  cd restaurante-admin
  npm start

  Abre:

  http://localhost:4200

  Deja esta terminal abierta.

  ## 8. Levantar POS

  Abre una tercera terminal, ubicada en la raíz:

  cd restaurante-pos
  npm start

  Abre:

  http://localhost:4201

  El script del proyecto ya configura el puerto 4201.

  No necesitas cargar el .env del backend en ninguna terminal de los frontend.

  ## 9. Comprobar el funcionamiento

   Componente    Dirección local
  ━━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
   Admin         http://localhost:4200
  ────────────  ────────────────────────────────────────────────────────
   POS           http://localhost:4201
  ────────────  ────────────────────────────────────────────────────────
   Swagger       http://localhost:8090/api/v1/swagger-ui.html
  ────────────  ────────────────────────────────────────────────────────
   Readiness     http://localhost:8090/api/v1/actuator/health/readiness

  Para Admin, utiliza las credenciales iniciales del .env compartido cuando trabajes con una base nueva.

  ## 10. Propósito de las ramas

   Rama         Propósito                             ¿Despliega producción?
  ━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  ━━━━━━━━━━━━━━━━━━━━━━━━━━━
   feature/*    Desarrollar una tarea                 No
  ───────────  ────────────────────────────────────  ───────────────────────────
   develop      Integrar el trabajo del equipo        No
  ───────────  ────────────────────────────────────  ───────────────────────────
   release/*    Preparar una versión para publicar    No
  ───────────  ────────────────────────────────────  ───────────────────────────
   main         Mantener la versión publicada         Sí, después de aprobar CI

  Flujo del equipo:

  develop actualizado
          ↓
  feature/mi-tarea
          ↓
  PR hacia develop
          ↓
  Pruebas + revisión + merge
          ↓
  release/version
          ↓
  PR hacia main
          ↓
  Pruebas + revisión + merge
          ↓
  CI y despliegue automático

  Fusionar en develop no actualiza automáticamente main. La publicación se decide y se prepara por separado.

  ## 11. Trabajar con migraciones de base de datos

  Si tu tarea cambia el esquema, agrega una migración nueva dentro de:

  restaurante-api/src/main/resources/db/migration/

  Consulta primero las migraciones existentes:

  ls restaurante-api/src/main/resources/db/migration/

  El nombre sigue este formato:

  V<numero>__descripcion.sql

  Hay dos guiones bajos entre la versión y la descripción.

  Reglas para el equipo:
  1. Coordinar el siguiente número antes de crear la migración.
  2. Nunca modificar una migración que ya fue aplicada o integrada.
  3. Probar los cambios en PostgreSQL local.
  4. Subir la migración junto con el código que depende de ella.
  5. Corregir migraciones compartidas mediante una nueva migración.
  6. Revisar el impacto sobre los datos existentes antes de publicar.

  Al arrancar el backend, Flyway aplica lo pendiente. No hace falta ejecutar manualmente el mismo SQL en Neon.

  Cambiar de rama no revierte tu base local. Si una feature agregó tablas, esas tablas pueden seguir existiendo al cambiar a otra rama.
  Coordina cualquier limpieza; no borres el volumen para resolver un error sin revisar primero.

  ## 12. Probar antes de subir cambios

  Desde la raíz, ejecuta las pruebas del backend:

  cd restaurante-api
  ./gradlew check
  cd ..

  Para Admin:

  cd restaurante-admin
  npm test -- --watch=false
  npm run build -- --configuration production
  cd ..

  Para POS:

  cd restaurante-pos
  npm test -- --watch=false
  npm run build -- --configuration production
  cd ..

  También prueba manualmente la funcionalidad que desarrollaste.

  Un build correcto no garantiza que la historia de usuario esté completa. Verifica casos de error, permisos y comportamiento esperado.

  ## 13. Hacer commit y subir tu feature

  Desde la raíz:

  git status
  git diff --check
  git diff

  Revisa que solo aparezcan los archivos de tu tarea y que no contengan credenciales.

  Agrega los archivos o carpetas concretos que revisaste. Por ejemplo, si tu cambio está en componentes de Admin:

  git add restaurante-admin/src/app/

  Adapta esa ruta a tu trabajo. Después revisa lo preparado:

  git diff --cached --stat
  git diff --cached

  Crea el commit:

  git commit -m "feat: implementar gestion de proveedores"

  Sube tu rama:

  git push -u origin feature/crud-proveedores

  Para futuros commits en esa misma rama:

  git push

  El nombre de la rama debe coincidir con la que realmente creaste. Para subir cambios necesitas permisos de escritura en el
  repositorio.

  ## 14. Abrir el PR hacia develop

  En GitHub:

  1. Entra al repositorio.
  2. Selecciona Compare & pull request.
  3. Comprueba:
      - Base: develop
      - Compare: tu rama feature/*.

  4. Escribe un título claro.
  5. Describe:
      - Qué implementaste.
      - Cómo lo probaste.
      - Si incluye una migración.
      - Si requiere nuevas variables.

  6. Crea el PR.
  7. Espera los checks y la revisión de otro integrante.

  No fusiones si hay pruebas fallidas o conflictos pendientes.

  En un PR, Desplegar producción debe aparecer omitido. Eso es normal.

  ## 15. Incorporar cambios nuevos de develop

  Si tu feature sigue abierta y otros compañeros ya integraron cambios:

  Primero revisa:

  git status

  Guarda tu trabajo con un commit apropiado antes de integrar cambios. No cambies de rama con modificaciones sin revisar.

  Desde tu feature:

  git fetch origin
  git merge origin/develop

  Si hay conflictos:

  1. Revisa los archivos indicados por Git.
  2. Conserva el comportamiento necesario de ambos cambios.
  3. Coordina con el autor si hay dudas.
  4. Elimina los marcadores de conflicto.
  5. Agrega los archivos resueltos.
  6. Completa el merge con git commit.
  7. Repite las pruebas y ejecuta git push.

  No elijas “aceptar todo lo mío” o “todo lo entrante” sin revisar, especialmente en migraciones y lockfiles.

  ## 16. Empezar la siguiente tarea

  Cuando tu PR ya esté fusionado y no tengas cambios locales pendientes:

  git switch develop
  git pull --ff-only origin develop
  git switch -c feature/siguiente-tarea

  No reutilices una feature ya fusionada para una tarea diferente.

  Si git pull --ff-only falla, revisa la divergencia con el equipo. No utilices reset --hard ni un push forzado para salir del paso.

  ## 17. Publicación en producción

  La persona responsable de la release:

  1. Parte de develop actualizado y validado.
  2. Crea una rama release/*.
  3. Abre un PR hacia main.
  4. Espera CI y revisión.
  5. Fusiona cuando la publicación esté autorizada.
  6. Comprueba el job Desplegar producción y las aplicaciones.

  Después del merge a main, el pipeline:

  - Ejecuta pruebas y builds.
  - Despliega el backend en Render.
  - Aplica migraciones pendientes mediante Flyway durante el arranque.
  - Publica Admin y POS en Cloudflare Pages.

  Incluso los cambios solo documentales que llegan a main pueden desplegar, porque el workflow actual no filtra por archivos.

  ## 18. Rutina para iniciar y terminar el día

  Para comenzar:

  1. Revisar la rama y git status.
  2. Actualizar o integrar develop según corresponda.
  3. Ejecutar npm ci si cambiaron dependencias.
  4. Cargar .env en la terminal del backend.
  5. Levantar PostgreSQL.
  6. Levantar backend, Admin y POS.

  Para terminar:

  1. Guardar el trabajo y revisar git status.
  2. Hacer commit y push de lo que corresponda.
  3. Detener backend y frontend con Ctrl+C en sus terminales.
  4. Desde restaurante-api, detener PostgreSQL:

     docker compose stop

  Este comando conserva los datos. No utilices docker compose down -v como rutina: elimina los volúmenes y sus datos.

  Las guías del proyecto están en:

  - docs/ARQUITECTURA-EQUIPO.md
  - docs/CONFIGURACION-DESPLIEGUE.md