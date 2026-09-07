# Revisión de dependencias npm — 7 de septiembre de 2026

Alcance: Admin y POS. Los dos lockfiles originales tenían el mismo árbol de
dependencias y npm reportaba siete paquetes afectados: cuatro de severidad alta
y tres moderada. Todos estaban marcados como dependencias de desarrollo.
`npm audit --omit=dev` reportó cero vulnerabilidades en ambos proyectos.
Esto no certifica ausencia de riesgos: las herramientas también se ejecutan en CI.

## Corrección aplicada

| Paquete o grupo | Versión anterior del lockfile | Nueva versión |
|---|---|---|
| Angular y compiler-cli | 21.2.19 | 21.2.22 |
| Angular build, CLI y herramientas asociadas | 21.2.20 | 21.2.21 |
| Tailwind y plugin PostCSS | 4.3.1 | 4.3.2 |
| PostCSS | 8.5.15 | 8.5.23 |
| undici | 7.28.0 | 7.29.0 |
| fast-uri | 3.1.5 | 3.1.7 |
| nanoid | 3.3.17 | 3.3.18 |
| qs | 6.15.3 | 6.16.0 |

Los avisos de Angular build y del plugin de Tailwind procedían de undici y PostCSS,
respectivamente. Se actualizaron sus paquetes padres para respetar sus dependencias.
La nueva CLI también requiere actualizar su dependencia `@modelcontextprotocol/sdk`
de 1.26.0 a 1.30.0; esto no configura ni activa un servidor MCP en el proyecto.

Los intentos iniciales sobre copias temporales del lockfile produjeron ERESOLVE.
Se resolvió un árbol limpio con versiones directas explícitas, se conservaron las
entradas originales ajenas a la corrección y npm volvió a validar el lockfile resultante.
Angular y su compilador quedaron alineados en la serie 21.2.
No se utilizó `--force`, `--legacy-peer-deps`, overrides ni `npm audit fix`.

Cambios funcionales limitados a `package.json` y `package-lock.json` de ambos
frontend. Los scripts, fuentes, configuración de API y cambios previos se conservan.
Las dependencias locales instaladas no se reemplazaron: la validación usa Docker
con montajes temporales para node_modules, dist y la caché Angular.

## Validación

Ambos proyectos pasaron con Node 24.20.0 y npm 11.6.2 dentro de Docker:

- Instalación limpia mediante `npm ci`.
- `npm audit`: cero vulnerabilidades, incluyendo dependencias de desarrollo.
- `npm test -- --watch=false`: una prueba aprobada en cada frontend.
- `npm run build -- --configuration production`: build aprobado en ambos.
- Confirmada la existencia de `dist/<proyecto>/browser/index.html` en ambos.

El contenedor de validación terminó con código 0. No se repitieron las pruebas Java,
porque esta corrección no modifica el backend. No hubo pruebas de navegador ni de
conexión cloud; las pruebas existentes de Angular tienen cobertura limitada.
El resultado de auditoría depende de los avisos disponibles en la fecha de consulta.

## Fuentes oficiales

- [PostCSS: lectura de mapas de fuentes](https://github.com/postcss/postcss/security/advisories/GHSA-fxqj-rqcc-2cmp).
- [undici: caché y errores de análisis](https://github.com/nodejs/undici/security/advisories/GHSA-4cwx-7wf7-3272).
- [fast-uri: normalización de IPv6](https://github.com/fastify/fast-uri/security/advisories/GHSA-f65p-4m7j-42xc).
- [qs: denegación de servicio](https://github.com/ljharb/qs/security/advisories/GHSA-4mjr-xmp4-gh2g).
- [nanoid 3.3.18](https://github.com/ai/nanoid/releases/tag/3.3.18).
- [Tailwind 4.3.2](https://github.com/tailwindlabs/tailwindcss/releases/tag/v4.3.2).
- Metadatos del registro oficial consultados mediante `npm view` y `npm audit`.
