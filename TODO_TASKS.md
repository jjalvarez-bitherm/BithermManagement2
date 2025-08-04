# 📋 LISTA DE TAREAS TO-DO - BithermManagement

## 🎯 Estado General
- **Total de tareas:** 5
- **Pendientes:** 5
- **En progreso:** 0
- **Completadas:** 0

---

## 📝 TAREAS PENDIENTES

### 🔴 Prioridad Alta
<!-- Tareas críticas que afectan funcionalidad principal -->

### 🟡 Prioridad Media  
<!-- Tareas importantes pero no críticas -->

### 🟢 Prioridad Baja
<!-- Mejoras y optimizaciones -->

### 🔴 Prioridad Alta
#### Problema con fotos no sincronizadas con purgador seleccionado
- **Descripción:** Las fotos mostradas no dependen del purgador seleccionado. Las imágenes se muestran de forma estática sin cambiar según el purgador activo (A6-00157 en la imagen)
- **Ubicación:** Pantalla de visualización de purgadores con fotos
- **Imagen:** Captura de pantalla mostrando purgador A6-00157 con fotos fijas
- **Fecha añadida:** 19/12/2024
- **Estado:** Pendiente
- **Notas adicionales:** Las fotos deberían cambiar dinámicamente según el purgador seleccionado (anterior/siguiente)

#### TextView debe mostrar información de inspección actual
- **Descripción:** El TextView entre el spinner de estados y el botón "GUARDAR Y SIGUIENTE" debe mostrar datos de la BD local en formato: <FECHA-ESTADO> - <INSPECTOR> (<DETECTOR_UTILIZADO>). Al guardar, debe actualizar con fecha actual, inspector logueado y detector asignado al usuario
- **Ubicación:** Pantalla de inspección de purgadores (TextView visible en la imagen)
- **Imagen:** Captura mostrando TextView vacío entre spinner y botón guardar
- **Fecha añadida:** 19/12/2024
- **Estado:** Pendiente
- **Notas adicionales:** 
  - Formato requerido: <FECHA-ESTADO> - <INSPECTOR> (<DETECTOR_UTILIZADO>)
  - Al guardar: actualizar con fecha actual + inspector logueado + detector del usuario
  - Al navegar hacia atrás: mostrar datos actualizados, no los de la descarga original
  - Debe actualizar BD local para sincronización posterior con Sheets

#### Spinner de instalación debe tener valores fijos
- **Descripción:** El spinner de instalación debe mostrar siempre los mismos 4 valores: "MANIFOLD VAPOR", "MANIFOLD CONDENSADO", "EN LINEA", "PIANILLO". Estos son los únicos valores posibles y no deben cambiar
- **Ubicación:** Pantalla de inspección de purgadores (spinner de instalación)
- **Imagen:** Captura mostrando diálogo de selección con los 4 valores fijos
- **Fecha añadida:** 19/12/2024
- **Estado:** Pendiente
- **Notas adicionales:** 
  - Valores fijos: MANIFOLD VAPOR, MANIFOLD CONDENSADO, EN LINEA, PIANILLO
  - No deben depender de datos externos o cambiar dinámicamente
  - Deben estar siempre disponibles para selección

#### Campo AISLAMIENTO debe ser spinner con valores fijos
- **Descripción:** El campo de texto "AISLAMIENTO" debe convertirse en un spinner que permita seleccionar entre valores fijos específicos
- **Ubicación:** Pantalla de inspección de purgadores (campo AISLAMIENTO)
- **Imagen:** Captura mostrando campo de texto con borde azul junto a "AISLAMIENTO"
- **Fecha añadida:** 19/12/2024
- **Estado:** Pendiente
- **Notas adicionales:** 
  - Valores fijos requeridos:
    - SIN BLOQUEO
    - V/V VAPOR
    - V/V CONDENSADO
    - V/V ANT - POST
    - " " (espacio en blanco)
    - TVS 4V/V
    - TVS (OTRA)
    - " " (espacio en blanco)
    - V/V VAPOR GRIPADA
    - V/V COND. GRIPADA
    - V/V GRIPADAS
    - " " (espacio en blanco)
    - TVS TRASPASAAD
    - TVS GRIPADA
  - Debe reemplazar el campo de texto actual
  - Valores siempre disponibles, no dinámicos

#### Reorganización de campos de instalación y aislamiento
- **Descripción:** Reorganizar la disposición de campos en la pantalla de inspección según nueva estructura solicitada
- **Ubicación:** Pantalla de inspección de purgadores (formulario de datos)
- **Imagen:** Captura mostrando campos actuales: INSTALACIÓN* y NOMBRE DE LÍNEA en línea 1, AISLAMIENTO en línea 2
- **Fecha añadida:** 19/12/2024
- **Estado:** Pendiente
- **Notas adicionales:** 
  - **Nueva estructura:**
    - **Línea 1:** INSTALACIÓN (spinner fijo) | NOMBRE MANIFOLD (solo activo si se selecciona manifold vapor/condensado)
    - **Línea 2:** AISLAMIENTO (spinner fijo) | NOMBRE DE LÍNEA (campo texto)
  - **Mapeo BD local:**
    - INSTALACIÓN → INSTALACION_TYPE
    - NOMBRE MANIFOLD → INSTALACION_MF  
    - AISLAMIENTO → AISLAMIENTO
    - NOMBRE DE LÍNEA → INSTALACION_LINE
  - **Verificaciones requeridas:**
    - Confirmar que valores de Google Sheets se guardan en BD local
    - Confirmar que datos de BD local se importan al modelo de cards en inspeccionActualEquipo
  - **Adaptaciones necesarias:** Modificar DAOs para mapeo correcto de datos

---

## 🚧 TAREAS EN PROGRESO
<!-- Tareas que están siendo trabajadas actualmente -->

---

## ✅ TAREAS COMPLETADAS
<!-- Tareas finalizadas con fecha de completado -->

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