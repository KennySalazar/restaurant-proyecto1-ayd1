# Proyecto Restaurante POS - Estado Detallado

## Información General
- **Proyecto**: Restaurante POS Angular 21 + Backend Java 21/Postgres
- **Backend**: `restaurante-api` (puerto 8090, perfil dev)
- **Frontend**: `restaurante-pos` (puerto 4201)
- **Base de datos**: PostgreSQL 18.6, `restaurante_db` esquema `public`
- **Usuarios**:
  - Admin: `admin@restaurante.com` / `Admin2026Seguro`
  - Mesero: `mesero1@test.com` / `test123`

---

## CORRECCIONES IMPORTANTES DETECTADAS EN LA CONVERSACIÓN

### Fusión de mesas (HU-B-006)
- La fusión debe permitir que **ambas mesas queden ocupadas** (no una LIBRE)
- Indicador visual: "Unida a Mesa X"
- No permitir fusionar mesas ya fusionadas (cadena A→B→C no permitida)

### Borrado de BD
- El comando `TRUNCATE ... CASCADE` borra `zonas_mesa`
- Solución: usar `TRUNCATE ... EXCLUDING zonas_mesa` o restauración manual

### Validación de transferencia (HU-B-005)
- Capacidad destino >= personas de la cuenta origen

### Tarjetas de mesa
- Mostrar `cantidadPersonas`, info reserva, badge "Unida a Mesa X" para mesas fusionadas

---

## ESTADO ACTUAL

### ✅ COMPLETADO

#### Fase 0 (Backend)
- Endpoints de menú y reservas implementados y compilan
- Base de datos y usuarios configurados

#### Fase 1 POS (`tables`)
- Build exitoso: `npm run build` ✅
- Tests: 1 passed ✅
- HU-B-001: Visualización de mesas con estado y zona

#### HU-025
- Frontend admin + fix backend corregidos

#### Backend `AccountService.java` y `AccountMergeRepository.java`
- Validaciones de fusión y transferencia implementadas

### 🟡 EN PROGRESO (Fase 3)

#### HU-B-002/003/004 - UI completada
- POS con acciones por estado (LIBRE/RESERVADA/OCUPADA/CUENTA_SOLICITADA)
- Diálogos de reserva/lista espera/cuenta
- Persistencia en `GET /operacion/mesas/fusiones-activas`

#### HU-B-005/006/007/008 - Implementación en progreso
- **Backend**: `AccountService.mergeAccounts()` con validaciones de fusión
  - Máx 2 mesas para fusionar
  - No permitir fusionar mesas ya fusionadas
- **Backend**: `AccountService.transferAccount()` con validación de capacidad
  - `capacidad >= personas` en mesa destino
- **Frontend**: `tables.ts` - Signals
  - `fusedTables`, `fusedDestination()`, `getFusedDestination()`
  - `getTotalPeopleForTable()`, `getAccountForTable()`, `getReservationForTable()`
- **Frontend**: `tables.html` - Badge "Unida a Mesa X", botones condicionales
- **Frontend**: `tables.scss` - `.table-card__fused-badge` estilos
- **Frontend**: `i18n/es.json` - `tables.fused.with: "Unida a Mesa"`

### ❌ BLOQUEADO

#### Trigger BD `fn_proteger_cuenta_terminal`
- Bloquea UPDATE en cuentas con estado FUSIONADA/CERRADA/CANCELADA
- Solución: usar native query `WHERE id = :id AND estado = 'ABIERTA'` en lugar de entity save

#### Trigger en BD `fn_ejecutar_fusion_cuenta`
- En `fusiones_cuenta` ejecuta automáticamente la fusión al insertar
- Código Java debe flush antes y refrescar entidades post-trigger

### 📋 POR VALIDAR

#### HU-B-005 (transferir)
- Transferir cuenta de mesa origen OCUPADA → mesa destino LIBRE con capacidad >= personas
- Mesa origen queda OCUPADA (no LIBRE)

#### HU-B-006 (fusionar)
- Fusionar mesa 2 → mesa 9
- Verificar que ambas quedan OCUPADAS (no LIBRE)
- Badge "Unida a Mesa 2" en mesa 2

#### HU-B-007 (dividir)
- Dividir cuenta de una mesa en dos

#### HU-B-008 (solicitar cobro)
- Validar que haya al menos un platillo en la cuenta
- Estado de la cuenta debe permitir solicitar cobro

---

## PRÓXIMOS PASOS SUGERIDOS

1. **Probar fusión de mesas** con usuario mesero:
   - Fusionar mesa 2 → mesa 9
   - Verificar que ambas quedan OCUPADAS (no LIBRE)
   - Verificar badge "Unida a Mesa 2" en mesa 2

2. **Probar transferencia de cuenta**:
   - Mesa origen OCUPADA → mesa destino LIBRE con capacidad >= personas
   - Mesa origen queda OCUPADA (no LIBRE)

3. **Verificar persistencia**:
   - Recargar página (F5) - fusionaciones y datos de cuenta deben persistir
   - Endpoint `GET /operacion/mesas/fusiones-activas`

4. **Probar todos los casos borde**:
   - Validaciones de capacidad
   - Mesas ya fusionadas (no permitir cadena A→B→C)
   - Mesas sin platillos para solicitar cobro
   - División de cuenta

---

## ARCHIVOS RELEVANTES

### Backend
- `restaurante-api/src/main/java/com/restaurante/application/account/AccountService.java`
  - `mergeAccounts()` con validaciones nuevas
  - `transferAccount()` con validación de capacidad
- `restaurante-api/src/main/java/com/restaurante/domain/repository/AccountMergeRepository.java`
  - `findActiveFusions()`, `existsActiveFusionForTable()`
- `restaurante-api/src/main/java/com/restaurante/domain/model/AccountMerge.java`
  - Entidad de auditoría
- `restaurante-api/src/main/java/com/restaurante/web/operation/TableOperationController.java`
  - `GET /operacion/mesas/fusiones-activas`
- `restaurante-api/src/main/java/com/restaurante/domain/model/ActiveFusionInfo.java`
  - Nuevo DTO (después de revertido)

### Frontend
- `restaurante-pos/src/app/pages/protected/tables/tables.ts`
  - Signals `fusedTables`, `fusedTables`, `getFusedDestination()`, `getTotalPeopleForTable()`, `getAccountForTable()`, `getReservationForTable()`
- `restaurante-pos/src/app/pages/protected/tables/tables.html`
  - Badge "Unida a Mesa X", botones condicionales
- `restaurante-pos/src/app/pages/protected/tables/tables.scss`
  - Estilos `.table-card__fused-badge`
- `restaurante-pos/public/i18n/es.json`
  - `tables.fused.with: "Unida a Mesa"`

### BD
- `restaurante/.../fusion_cuenta` - Trigger fusión
- `restaurante/.../cuentas` - Table de cuentas
- `restaurante/.../mesas` - Table de mesas

---

## ESTADO DEL BUILD Y TESTS

### Backend
```
./gradlew compileJava
BUILD SUCCESSFUL in 3s ✅
```

### Frontend
```
npm run build
Application bundle generation complete ✅
[n WARNING about bundle budget]

npm run test
1 passed ✅
```

---

## NOTAS DE IMPLEMENTACIÓN RECENTES (antes del revert)

1. Se agregó `getActiveAccounts()` a AccountService y llamado en tables.ts
2. Se agregaron signals de fusión en tables.ts: `fusedTables`, `fusedDestination()`, `getFusedDestination()`, `getTotalPeopleForTable()`
3. Se agregó badge visual "Unida a Mesa X" en tables.html con estilos .table-card__fused-badge
4. Se agregó validación en mergeAccounts: máximo 2 mesas, no fusionar mesas ya fusionadas
5. Se agregó validación en transferAccount: capacidad destino >= personas de origen

**Estado actual**: Revertido a versión estable anterior a estos cambios. Ambos proyectos compilan y tests pasan.