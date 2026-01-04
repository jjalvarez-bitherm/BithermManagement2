# 📊 FLUJO COMPLETO DE DATOS - SISTEMA DE INSPECCIÓN

## 🔄 VISIÓN GENERAL DEL SISTEMA

El sistema de inspección maneja datos de equipos (purgadores) que se mueven entre:
- **Google Sheets** (origen y destino)
- **Base de datos local (Room)** (almacenamiento temporal)
- **Interfaz de usuario** (edición y visualización)

---

## 📥 1. ENTRADA DE DATOS (Carga Inicial)

### **A. Desde Google Sheets → Base de Datos Local**

**Ubicación:** `FragmentInspeccionConfiguracion.kt`

**Proceso:**
1. **Botón "DESCARGAR"** en configuración
2. **Configuración necesaria:**
   - Libro Google Sheets (ID almacenado en `db_libro_id`)
   - Hoja de inspección (almacenada en `db_inspecciones_hoja`, por defecto "INSPECCIÓN" o "FLOTA")
   - Fecha de inspección (almacenada en `date_key`)
   - Número de inspección (almacenada en `numero_inspeccion`, por defecto "25")

3. **Lectura de Google Sheets:**
   ```kotlin
   // Lee cabeceras desde fila 2 (A2:ZZ2)
   val headerRow = sheetsManager.get(dbLibroId, "$dbHoja!A2:ZZ2")
   
   // Lee datos desde fila 3 (A3:ZZ)
   val dataRows = sheetsManager.get(dbLibroId, "$dbHoja!A3:$ultimaColumna")
   ```

4. **Mapeo de columnas:**
   - Usa `mapeo_columnas.json` (assets) para mapear nombres de columnas
   - Permite mapeo manual si faltan campos
   - Detecta campos de orden disponibles (`orden_`, `orden_juan`, etc.)

5. **Procesamiento:**
   - Convierte filas de Google Sheets a entidades `Equipo`
   - Guarda en base de datos local Room (`InspeccionDao.insertEquipo()`)

**Campos mapeados:**
- `id` → "TAG"
- `area` → "AREA"
- `unidad` → "UNIDAD"
- `marca` → "MARCA"
- `modelo` → "MODELO"
- `estado` → Estado del equipo
- `ubicacion` → "UBICACIÓN"
- `gpsCoord` → "GPS_COORD"
- Y todos los campos definidos en `Equipo.kt`

---

## 💾 2. ALMACENAMIENTO LOCAL (Base de Datos)

### **A. Estructura de la Base de Datos**

**Entidad:** `Equipo` (tabla `equipos`)
**Ubicación:** `database/entities/Equipo.kt`

**Campos principales:**
```kotlin
- id: String (Primary Key)
- area, unidad, instalacion, linea
- marca, modelo, tipo
- diametro, conexion, aislamiento
- presEntrada, presSalida
- ubicacion, estado
- fechaInspeccion
- identidadInspector
- detectorUtilizado
- nota, incidencias
- gpsCoord, gpsAcc
- modificadoLocal: Boolean (indica si fue modificado)
- urlFotoEquipo, urlFotoUbicacion, urlFotoManifold, urlFotosExtra
```

**DAO:** `InspeccionDao`
- `getAllEquipos()` - Obtiene todos los equipos
- `insertEquipo()` - Inserta/actualiza equipo
- `actualizarEquipo()` - Actualiza equipo existente
- `getModificadasLocal()` - Obtiene equipos modificados localmente

---

## ✏️ 3. EDICIÓN DE DATOS

### **A. Interfaz de Edición**

**Fragmento:** `FragmentInspeccionEquipo.kt`

**Campos editables:**
1. **Spinners (selección):**
   - Estado
   - Área, Unidad, Instalación
   - Marca, Modelo, Tipo
   - Diámetro, Conexión
   - Presión entrada/salida
   - Periodicidad

2. **EditText (texto libre):**
   - Ubicación
   - Nota/Observaciones
   - Incidencias
   - Línea

3. **Automáticos (no editables directamente):**
   - Fecha inspección (fecha actual al guardar)
   - Inspector (usuario logueado)
   - Detector (detector del usuario)
   - GPS (coordenadas actuales al guardar)
   - Fotos (se toman con la cámara)

### **B. Detección de Cambios**

El sistema detecta qué campos fueron modificados:
```kotlin
// CAMPOS AUTOMÁTICOS (NO cuentan como modificados):
- fechaInspeccion
- identidadInspector
- detectorUtilizado

// CAMPOS MANUALES (SÍ cuentan como modificados):
- estado, ubicacion, nota, incidencias, linea, gpsCoord
```

Si no hay cambios manuales → `modificadoLocal = false` (solo inspección)
Si hay cambios manuales → `modificadoLocal = true` (equipo modificado)

---

## 💾 4. GUARDADO DE DATOS

### **A. Guardado Local (inmediato)**

**Ubicación:** `FragmentInspeccionEquipo.kt` → `guardarYAvanzar()`

**Proceso:**
1. Al presionar "Guardar y Avanzar":
   - Crea `Equipo` actualizado con nuevos valores
   - Actualiza en base de datos: `inspeccionDao.actualizarEquipo(equipoActualizado)`
   - Actualiza lista local: `equipos[indexActual] = equipoActualizado`

### **B. Guardado en Google Sheets (inmediato)**

**Ubicación:** `FragmentInspeccionEquipo.kt` → `guardarInspeccionEnGoogleSheets()`

**Proceso:**
1. Obtiene configuración:
   - Libro ID: `db_libro_id`
   - Hoja: `db_inspecciones_hoja` (por defecto "INSPECCIÓN")
   - Número inspección: `numero_inspeccion` (por defecto "25")

2. Busca la fila del equipo:
   - Lee toda la hoja
   - Busca por TAG (columna B) = `equipo.id`

3. Busca la columna de inspección:
   - Lee fila 2 (números de inspección)
   - Encuentra columna que coincide con `numeroInspeccion`

4. Escribe datos en columnas relativas:
   ```
   Columna Inspección = columna del número
   Estado = columnaInspeccion + 1
   Fecha = columnaInspeccion + 2
   Nota = columnaInspeccion + 3
   Inspector = columnaInspeccion + 4
   Detector = columnaInspeccion + 5
   ```

5. Campos escritos:
   - Estado
   - Fecha inspección
   - Nota
   - Inspector
   - Detector

**⚠️ IMPORTANTE:** Solo escribe en la hoja de **INSPECCIÓN ACTUAL**, NO en FLOTA

---

## 📤 5. SALIDA DE DATOS

### **A. Visualización en la App**

**Fragmentos que muestran datos:**
- `FragmentInspeccionEquipo.kt` - Formulario de edición
- `FragmentDetalleEquipo.kt` - Vista detallada
- `FragmentInspeccionConfiguracion.kt` - Resumen y configuración

### **B. Sincronización a Google Sheets**

**Método 1: Guardado inmediato** (ya explicado arriba)
- Se guarda cada vez que se inspecciona un equipo

**Método 2: Sincronización masiva** (futuro)
- `GoogleSheetsManager.sincronizarEquiposModificados()`
- Sincroniza todos los equipos con `modificadoLocal = true`

### **C. Transferencia INSPECCIÓN → FLOTA** (NO IMPLEMENTADO AÚN)

**Estado actual:** 
- Los datos se guardan solo en la hoja de INSPECCIÓN actual
- NO hay transferencia automática a FLOTA histórica

**Estructura esperada:**
```
INSPECCIÓN (hoja actual):
  - Datos de inspección diaria
  - Cada inspección en su columna (número de inspección)
  
FLOTA (hoja histórica):
  - Datos consolidados de todas las inspecciones
  - Historial completo del equipo
```

---

## 🔄 6. FLUJOS ESPECÍFICOS

### **A. Carga Inicial de Equipos**

```
Google Sheets (INSPECCIÓN/FLOTA)
    ↓
[Botón DESCARGAR]
    ↓
Leer cabeceras (fila 2)
    ↓
Leer datos (fila 3+)
    ↓
Mapeo de columnas (mapeo_columnas.json)
    ↓
Convertir a entidades Equipo
    ↓
Guardar en Room Database
    ↓
Mostrar en FragmentInspeccionEquipo
```

### **B. Inspección de un Equipo**

```
Usuario abre FragmentInspeccionEquipo
    ↓
Carga equipo desde Room Database
    ↓
Usuario edita campos
    ↓
[Guardar y Avanzar]
    ↓
Actualizar en Room Database
    ↓
Guardar en Google Sheets (INSPECCIÓN actual)
    ↓
Navegar al siguiente equipo
```

### **C. Actualización desde Google Sheets**

```
[Botón ACTUALIZAR en configuración]
    ↓
Leer datos desde INSPECCIÓN actual
    ↓
Sobrescribir datos en Room Database
    ↓
Actualizar colores de estados
    ↓
Mostrar resumen actualizado
```

---

## 📋 7. CONFIGURACIÓN Y PREFERENCIAS

**SharedPreferences:** `"configuracion_inspeccion"`

**Claves importantes:**
- `date_key` - Fecha de inspección
- `libro_key` - Nombre del libro Google Sheets
- `hoja_key` - Nombre de la hoja
- `db_libro_id` - ID del libro para DB inspecciones
- `db_inspecciones_hoja` - Hoja para datos de inspección (por defecto "INSPECCIÓN")
- `numero_inspeccion` - Número de inspección actual (por defecto "25")
- `campo_orden_seleccionado` - Campo de orden seleccionado
- `libro_flota` - Libro para FLOTA histórica
- `hoja_flota` - Hoja para FLOTA histórica

---

## 🎨 8. COLORES DE ESTADOS

**Ubicación:** `estados_colores.json` (assets)

**Actualización:**
- Se actualiza desde Google Sheets al descargar datos
- `GoogleSheetsManager.actualizarColoresEstados()`
- Lee desde una columna especial en la hoja

**Uso:**
- Se aplican colores automáticamente según el estado del equipo
- `FragmentInspeccionEquipo.aplicarColorEstado()`

---

## 📸 9. GESTIÓN DE FOTOS

**Tabla:** `foto_equipo` (Room Database)

**Estados:**
- `LOCAL` - Foto guardada localmente
- `SUBIDA` - Foto subida a Google Drive
- `PENDIENTE` - Pendiente de subir

**Proceso:**
1. Foto tomada → Guardada en `fotos/` (sistema de archivos)
2. Entrada creada en `foto_equipo`
3. [Botón ACTUALIZAR FOTOS] → Sube fotos pendientes a Google Drive
4. Actualiza estado a `SUBIDA`

---

## ⚠️ 10. PUNTOS CRÍTICOS Y LIMITACIONES

### **Actual:**
1. ✅ Datos se guardan en INSPECCIÓN actual
2. ❌ NO hay transferencia automática a FLOTA
3. ✅ Datos se guardan inmediatamente en Google Sheets
4. ✅ Base de datos local como caché

### **Estructura de Google Sheets:**
```
Fila 1: Datos generales del equipo (TAG, AREA, UNIDAD, etc.)
Fila 2: Números de inspección (25, 26, 27, ...)
Fila 3+: Datos de cada equipo

Para cada número de inspección:
  - Estado
  - Fecha
  - Nota
  - Inspector
  - Detector
```

---

## 🔧 11. PRÓXIMOS PASOS SUGERIDOS

1. **Implementar transferencia INSPECCIÓN → FLOTA:**
   - Al finalizar una inspección
   - Mover datos completos de INSPECCIÓN a FLOTA
   - Limpiar hoja INSPECCIÓN para próxima inspección

2. **Sincronización bidireccional:**
   - Detectar conflictos entre local y remoto
   - Resolver cambios simultáneos

3. **Historial completo:**
   - Ver todas las inspecciones de un equipo
   - Comparar entre inspecciones

---

## 📝 RESUMEN RÁPIDO

```
ENTRADA:  Google Sheets (INSPECCIÓN/FLOTA) → Room Database
EDICIÓN:  FragmentInspeccionEquipo (interfaz usuario)
GUARDADO: Room Database + Google Sheets (INSPECCIÓN actual)
SALIDA:   Google Sheets (INSPECCIÓN para trabajo actual)
         FLOTA (histórico - NO transferido automáticamente aún)
```

**Flujo principal:**
1. Descargar datos desde Google Sheets → BD local
2. Editar equipos en la app
3. Guardar cambios → BD local + Google Sheets (INSPECCIÓN)
4. [FUTURO] Transferir a FLOTA al finalizar inspección

---

*Última actualización: Basado en análisis del código actual*

