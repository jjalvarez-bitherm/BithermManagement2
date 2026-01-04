# Estado del Proyecto: Bitherm Management 2.0 (Web Admin)

Este documento resume el estado actual del proyecto, las funcionalidades críticas implementadas y la lógica de negocio para que cualquier otro agente pueda continuar el desarrollo sin fricciones.

## 🚀 Funcionalidades Principales

### 1. Sistema de Ubicación Híbrido (GPS + Red)
*   **Problema**: Los portátiles no tienen GPS real y daban "Timeout".
*   **Solución**: El sistema intenta `Alta Precisión` durante 10s. Si falla o detecta señal de red (como en PCs), permite un **"Registro Forzado"** mediante un botón de emergencia.
*   **Lógica de Distancia**: Se ha corregido el cálculo Haversine para ser exacto. Se ha implementado un "Margen de Confianza": si la precisión es baja (PCs/WiFi), el sistema es más flexible para asignar la ubicación "OFICINA" si las coordenadas caen cerca, evitando el marcado como "EXTERNO" erróneo.

### 2. Gestión de Ausencias (Vacaciones y Baja Médica)
*   **Botones Especiales**: En el dashboard existen botones dedicados para "VACACIONES" y "BAJAMED" (si el usuario tiene esas OTs).
*   **Backfill Automático**: Al iniciar una ausencia, el sistema guarda el estado en la tabla `TRABAJADORES`. Cuando el usuario vuelve a fichar una "ENTRADA" normal (días después), el backend detecta el estado anterior y **rellena automáticamente** todos los días intermedios con jornadas de 8h en la hoja `FICHAJE-OT-MES`.
*   **Exclusividad**: En el selector de OTs, si se elige Vacaciones o Baja, se desmarcan el resto de OTs y se asignan 8h fijas ignorando el contador real.

### 3. Distribución de Horas y Horas Extra
*   **Regla de las 8.1h**: El sistema solo pide distribuir horas extra si la jornada supera las **8.1 horas**. Si es menor o igual, se considera jornada normal y el flujo es más rápido.
*   **Fichaje de Salida**: Tras pulsar Salida, se abre un modal de 3 pasos (Selección OT -> Horas Normales -> Horas Extra).

### 4. UI / UX Optimizada
*   **Selector de OTs**: Sin parpadeos, sin scrollbars visibles (pero con scroll funcional) y con textos truncados para evitar descuadres.
*   **Cronómetro**: Corregido para sincronizarse con la hora del servidor y actualizarse segundo a segundo sin bloqueos por stale-state.

## 🛠️ Estructura Técnica

### Backend (`/web-management/backend/index.js`)
*   **Google Sheets API**: Actúa como base de datos.
*   **Endpoints Clave**:
    *   `/api/stats/:username`: Calcula horas, extras, días trabajados, vacaciones y bajas reales leyendo de `FICHAJE-OT-MES`.
    *   `/api/fichaje`: Gestiona el log y el backfill de ausencias.
    *   `/api/leave/start`: Inicia el estado de ausencia persistente en el Excel.

### Frontend (`/web-management/src/App.jsx`)
*   **React + Framer Motion**: Para animaciones de modales y transiciones.
*   **Geolocalización**: Uso de `watchPosition` para monitorización activa durante el fichaje.

## 📋 Pendiente / Notas para el Futuro
*   **Validación de Fechas**: El backfill actual asume que los días son naturales. Podría refinarse para ignorar fines de semana si fuera necesario.
*   **Caché**: Si la conexión con Google Sheets flaquea, se podría implementar una pequeña caché local para las estadísticas.

---
*BackUp generado el 02/01/2026 por Antigravity AI.*
