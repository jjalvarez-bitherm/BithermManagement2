# 📋 LISTA DE TAREAS TO-DO - BithermManagement

## 🎯 Estado General
- **Total de tareas:** 5
- **Pendientes:** 0
- **En progreso:** 0
- **Completadas:** 5

---

## ✅ TAREAS COMPLETADAS

### ✅ Problema con fotos no sincronizadas con purgador seleccionado
- **Descripción:** Las fotos mostradas no dependen del purgador seleccionado. Las imágenes se muestran de forma estática sin cambiar según el purgador activo
- **Ubicación:** Pantalla de visualización de purgadores con fotos - MiniGaleriaFragment
- **Fecha añadida:** 19/12/2024
- **Fecha completada:** 16/11/2025
- **Estado:** ✅ COMPLETADO
- **Solución implementada:** La función `mostrarEquipo()` llama correctamente a `miniGaleriaFragment.setEquipoId(equipoId)` que ejecuta `recargarFotos()`, actualizando las imágenes dinámicamente cuando se navega entre purgadores.

### ✅ TextView debe mostrar información de inspección actual
- **Descripción:** El TextView entre el spinner de estados y el botón "GUARDAR Y SIGUIENTE" debe mostrar datos de la BD local en formato: <FECHA-ESTADO> - <INSPECTOR> (<DETECTOR_UTILIZADO>)
- **Ubicación:** Pantalla de inspección de purgadores (TextView tvInspectorFecha)
- **Fecha añadida:** 19/12/2024
- **Fecha completada:** 16/11/2025
- **Estado:** ✅ COMPLETADO
- **Solución implementada:** 
  - El `tvInspectorFecha` muestra el formato correcto: `"$fecha - $inspector ($detector)"`
  - Al guardar, se actualiza con `obtenerFechaActual()`, `obtenerInspectorActual()`, `obtenerDetectorActual()`
  - La función `actualizarInformacionInspector()` actualiza la UI tras guardar
  - Los datos se persisten en BD local mediante `inspeccionDao.actualizarEquipo()`

### ✅ Spinner de instalación debe tener valores fijos
- **Descripción:** El spinner de instalación debe mostrar siempre los mismos 4 valores: "MANIFOLD VAPOR", "MANIFOLD CONDENSADO", "EN LINEA", "PIANILLO"
- **Ubicación:** Pantalla de inspección de purgadores (spinner de instalación)
- **Fecha añadida:** 19/12/2024
- **Fecha completada:** 16/11/2025
- **Estado:** ✅ COMPLETADO
- **Solución implementada:** 
  - Se definieron `valoresInstalacionPorDefecto` en el companion object
  - El spinner se inicializa con estos valores fijos en `setupSpinners()`
  - Los valores son independientes de datos externos

### ✅ Campo AISLAMIENTO debe ser spinner con valores fijos
- **Descripción:** El campo de texto "AISLAMIENTO" debe convertirse en un spinner que permita seleccionar entre valores fijos específicos
- **Ubicación:** Pantalla de inspección de purgadores (campo AISLAMIENTO)
- **Fecha añadida:** 19/12/2024
- **Fecha completada:** 16/11/2025
- **Estado:** ✅ COMPLETADO
- **Solución implementada:** 
  - Se definieron `valoresAislamientoPorDefecto` con todos los valores requeridos:
    - SIN BLOQUEO, V/V VAPOR, V/V CONDENSADO, V/V ANT-POST
    - TVS 4V/V, TVS (OTRA)
    - V/V VAPOR GRIPADA, V/V COND. GRIPADA, V/Vs GRIPADAS
    - TVS TRASPASADA, TVS GRIPADA
  - El campo ya era un spinner en el XML, solo se actualizó la lista de valores
  - Los valores se cargan en `setupSpinners()` de forma estática

### ✅ Reorganización de campos de instalación y aislamiento
- **Descripción:** Reorganizar la disposición de campos en la pantalla de inspección según nueva estructura solicitada
- **Ubicación:** Pantalla de inspección de purgadores (formulario de datos)
- **Fecha añadida:** 19/12/2024
- **Fecha completada:** 16/11/2025
- **Estado:** ✅ COMPLETADO
- **Solución implementada:**
  - **Estructura implementada:**
    - Línea 1: INSTALACIÓN (spinner) | NOMBRE MANIFOLD (autoInstalacionMf - condicional)
    - Línea 2: AISLAMIENTO (spinner) | NOMBRE DE LÍNEA (etLinea)
  - **Lógica condicional:** El campo etNombreManifold se activa/desactiva dinámicamente según la selección:
    - Activo si instalación == "MANIFOLD VAPOR" o "MANIFOLD CONDENSADO"
    - Desactivado y limpio para otros valores
  - **Mapeo BD local:**
    - `instalacion` → INSTALACION_TYPE
    - `instalacionMf` → INSTALACION_MF
    - `aislamiento` → AISLAMIENTO
    - `linea` → INSTALACION_LINE
    - `byPass` → BY_PASS
  - **Funciones actualizadas:**
    - `initializeViews()`: Se agregaron etNombreManifold y checkboxByPass
    - `configurarListenersBasicos()`: Listener para activar/desactivar nombre manifold
    - `mostrarEquipo()`: Se llenan etNombreManifold y checkboxByPass
    - `guardarYAvanzar()`: Se guardan instalacion, instalacionMf, aislamiento, linea, byPass

---

## 📝 TAREAS PENDIENTES
<!-- No hay tareas pendientes actualmente -->

---

## 🚧 TAREAS EN PROGRESO
<!-- No hay tareas en progreso actualmente -->

---

## 📋 FORMATO PARA NUEVAS TAREAS

```markdown
### [PRIORIDAD] Título de la tarea
- **Descripción:** Descripción detallada del problema o mejora
- **Ubicación:** Pantalla/Componente específico
- **Imagen:** [Ruta a la imagen de referencia]
- **Fecha añadida:** DD/MM/YYYY
- **Estado:** Pendiente
- **Notas adicionales:** Cualquier información extra relevante
```

---

## 🎯 INSTRUCCIONES DE USO

1. **Añadir tarea:** El usuario describe el problema/mejora
2. **Asignar prioridad:** Alta/Media/Baja según impacto
3. **Incluir imagen:** Si está disponible para referencia visual
4. **Ejecutar:** Cuando el usuario diga "¡Adelante!" o "Ejecuta las tareas"
5. **Actualizar estado:** Marcar como completada cuando se resuelva

---

*Última actualización: [Fecha actual]* 