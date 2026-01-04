# Análisis de Mapeo de Datos - Bitherm Management

## Tabla de Relación: Google Sheets → Base de Datos → UI

| # | Campo Interno | Columna Google Sheets (mapeo) | Campo Equipo (BD) | Campo EquipoView | FragmentInspeccionEquipo (UI) | Estado | Observaciones |
|---|---------------|-------------------------------|-------------------|------------------|-------------------------------|--------|---------------|
| 1 | `id` | `TAG` | `id` (PK) | `id` | `tvIdEquipoFixed`, `tvIdEquipo` | ✅ OK | Se muestra en header fijo |
| 2 | `status` | `STATUS` | `status` | ❌ **HUÉRFANO** | `tvStatus` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** → Cards en blanco |
| 3 | `estado` | `ESTADO N` (dinámico) | `estado` | `estado` | `spinnerEstado` | ✅ OK | Usado para colorear cards |
| 4 | `area` | `AREA` | `area` | `area` | `spinnerArea` | ✅ OK | |
| 5 | `unidad` | `UNIDAD` | `unidad` | `unidad` | `spinnerUnidad` | ✅ OK | |
| 6 | `instalacion` | `MANIFOLD` | `instalacion` | ❌ **HUÉRFANO** | `spinnerInstalacion` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** |
| 7 | `linea` | `LINEA/EQUIPO` | `linea` | `linea` | `etLinea` | ✅ OK | |
| 8 | `ubicacion` | `UBICACIÓN` | `ubicacion` | `ubicacion` | `etUbicacion` | ✅ OK | |
| 9 | `gpsCoord` | `GPS_COORD` | `gpsCoord` | `gps` | `tvGpsCoord` | ✅ OK | Mapeo correcto |
| 10 | `gpsAcc` | `GPS_ACC` | `gpsAcc` | ❌ **HUÉRFANO** | `tvGpsAcc` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** |
| 11 | `marca` | `MARCA` | `marca` | `marca` | `spinnerMarca`, `textView` | ✅ OK | |
| 12 | `modelo` | `MODELO` | `modelo` | `modelo` | `spinnerModelo`, `textView` | ✅ OK | |
| 13 | `tipo` | `TIPO` | `tipo` | `tipo` | `spinnerTipo` | ✅ OK | |
| 14 | `diametro` | `DIAM.` | `diametro` | `diametro` | `spinnerDiametro`, `textView` | ✅ OK | |
| 15 | `conexion` | `CONEX` | `conexion` | `conexion` | `spinnerConexion`, `textView` | ✅ OK | |
| 16 | `presEntrada` | `P.IN` | `presEntrada` | `presEntrada` | `spinnerPIN` | ✅ OK | |
| 17 | `presSalida` | `P.OUT` | `presSalida` | `presSalida` | `spinnerPOUT` | ✅ OK | |
| 18 | `byPass` | `BY-PASS` | `byPass` (Boolean) | ❌ **HUÉRFANO** | ❌ No se muestra | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView ni se muestra** |
| 19 | `aislamiento` | `AISLAMIENTO` / `BLOQUEO` | `aislamiento` | `aislamiento` | `spinnerAislamiento` | ✅ OK | Mapeo automático implementado |
| 20 | `descarga` | `DESCARGA` | `descarga` | `descarga` | `spinnerDescarga` | ✅ OK | |
| 21 | `aplicacion` | `APLICACIÓN` | `aplicacion` | `aplicacion` | `spinnerAplicacion` | ✅ OK | |
| 22 | `servicio` | ❌ **NO MAPEADO** | `servicio` | `servicio` | `spinnerServicio` | ⚠️ **PROBLEMA** | **NO hay mapeo en JSON** |
| 23 | `periodicidad` | ❌ **NO MAPEADO** (usa `p`) | `periodicidad` | ❌ **HUÉRFANO** | `spinnerPeriodicidad` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** |
| 24 | `fechaInspeccion` | `FECHA N` (dinámico) | `fechaInspeccion` | `fechasteado` | `tvInspectorFecha` | ✅ OK | Mapeo correcto |
| 25 | `identidadInspector` | `INSPECTOR N` (dinámico) | `identidadInspector` | `inspector` | `tvInspectorFecha` | ✅ OK | Mapeo correcto |
| 26 | `detectorUtilizado` | `DETECTOR N` (dinámico) | `detectorUtilizado` | `detector` | `tvInspectorFecha` | ✅ OK | Mapeo correcto |
| 27 | `nota` | `NOTA N` (dinámico) | `nota` | `nota` | `etObservaciones` | ✅ OK | |
| 28 | `incidencias` | ❌ **NO MAPEADO** | `incidencias` | `incidencias` | `etIncidencias` | ⚠️ **PROBLEMA** | **NO hay mapeo en JSON** |
| 29 | `urlFotoEquipo` | `FOTO` | `urlFotoEquipo` | `foto` | `miniGaleriaFragment` | ✅ OK | |
| 30 | `urlFotoUbicacion` | `FOTO_UBIC` | `urlFotoUbicacion` | ❌ **HUÉRFANO** | `miniGaleriaFragment` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** |
| 31 | `urlFotoManifold` | `FOTO_MF` | `urlFotoManifold` | ❌ **HUÉRFANO** | `miniGaleriaFragment` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** |
| 32 | `urlFotosExtra` | `FOTO_EXTRA` | `urlFotosExtra` | ❌ **HUÉRFANO** | `miniGaleriaFragment` | ⚠️ **PROBLEMA** | **NO se mapea a EquipoView** |
| 33 | `orden` | `ORDEN` / `ORDEN_JUAN` / etc. | `orden` (Double) | `orden` | ❌ No se muestra | ✅ OK | Usado para ordenar lista |
| 34 | `instalacionMf` | ❌ **NO MAPEADO** | `instalacionMf` | ❌ **HUÉRFANO** | ❌ No se muestra | ⚠️ **PROBLEMA** | **NO hay mapeo ni se muestra** |
| 35 | `modificadoLocal` | ❌ N/A | `modificadoLocal` (Boolean) | ❌ **HUÉRFANO** | ❌ No se muestra | ✅ OK | Solo para control interno |
| 36 | `extra` | ❌ N/A | `extra` | ❌ **HUÉRFANO** | ❌ No se muestra | ✅ OK | Campo reservado |

## Problemas Identificados

### 🔴 CRÍTICOS (Causan cards en blanco)

1. **`status` NO se mapea a `EquipoView`**
   - **Ubicación**: `FragmentInspeccionActual.kt` línea 92-120
   - **Problema**: El campo `status` de `Equipo` no se copia a `EquipoView`
   - **Impacto**: Los cards no pueden obtener el estado para colorear
   - **Solución**: Agregar `status` a `EquipoView` y mapearlo en la conversión

### 🟡 IMPORTANTES (Datos no se muestran correctamente)

2. **`instalacion` NO se mapea a `EquipoView`**
   - **Impacto**: No se puede filtrar/ver por instalación en la lista
   - **Solución**: Agregar `instalacion` a `EquipoView`

3. **`gpsAcc` NO se mapea a `EquipoView`**
   - **Impacto**: La precisión GPS no está disponible en la lista
   - **Solución**: Agregar `gpsAcc` a `EquipoView`

4. **`periodicidad` NO se mapea a `EquipoView`**
   - **Impacto**: No se puede filtrar por periodicidad en la lista
   - **Solución**: Agregar `periodicidad` a `EquipoView`

5. **`byPass` NO se mapea a `EquipoView`**
   - **Impacto**: Información de bypass no disponible
   - **Solución**: Agregar `byPass` a `EquipoView`

6. **Fotos adicionales NO se mapean a `EquipoView`**
   - **Campos**: `urlFotoUbicacion`, `urlFotoManifold`, `urlFotosExtra`
   - **Impacto**: Fotos adicionales no disponibles en la lista
   - **Solución**: Agregar campos de fotos a `EquipoView` o consolidar en un array

### 🟠 MENORES (Mapeos faltantes en JSON)

7. **`servicio` NO tiene mapeo en `mapeo_columnas.json`**
   - **Impacto**: Depende del mapeo automático
   - **Solución**: Agregar `"servicio": "SERVICIO"` al JSON

8. **`incidencias` NO tiene mapeo en `mapeo_columnas.json`**
   - **Impacto**: Depende del mapeo automático
   - **Solución**: Agregar `"incidencias": "INCIDENCIAS"` al JSON

9. **`instalacionMf` NO tiene mapeo en `mapeo_columnas.json`**
   - **Impacto**: Campo no se importa correctamente
   - **Solución**: Agregar mapeo si es necesario

## Flujo de Datos Actual

### 1. Importación desde Google Sheets (`FragmentInspeccionConfiguracion.kt`)
```
Google Sheets → getRow("campo") → Equipo (BD)
```

### 2. Carga en Lista (`FragmentInspeccionActual.kt`)
```
Equipo (BD) → EquipoView → EquipoAdapter → Cards
```
**PROBLEMA**: `status` no se copia en esta conversión

### 3. Carga en Detalle (`FragmentInspeccionEquipo.kt`)
```
Equipo (BD) → mostrarEquipo() → UI Components
```
**PROBLEMA**: Algunos campos pueden estar vacíos si no se importaron correctamente

## Campos Huérfanos (en BD pero no en EquipoView)

1. `status` ⚠️ **CRÍTICO**
2. `instalacion`
3. `gpsAcc`
4. `periodicidad`
5. `byPass`
6. `urlFotoUbicacion`
7. `urlFotoManifold`
8. `urlFotosExtra`
9. `instalacionMf`
10. `modificadoLocal` (OK, es interno)
11. `extra` (OK, es reservado)

## Campos Faltantes en Mapeo JSON

1. `servicio` → Debería mapear a `SERVICIO`
2. `incidencias` → Debería mapear a `INCIDENCIAS`
3. `periodicidad` → Actualmente usa `p` (debería ser `PERIODICIDAD` o `P`)

