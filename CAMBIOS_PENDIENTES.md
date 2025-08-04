# CAMBIOS PENDIENTES - BithermManagement

## 1. Nueva Distribución de Columnas en Spreadsheets

### Problema Identificado
- Los spreadsheets de Google Sheets tienen una nueva estructura de columnas que no coincide con el mapeo actual
- Necesitamos actualizar el mapeo de datos en `GoogleSheetsManager.kt`

### Cambios Requeridos
- [ ] Revisar la nueva estructura de columnas en los spreadsheets
- [ ] Actualizar el mapeo en `GoogleSheetsManager.kt`
- [ ] Probar la sincronización con la nueva estructura
- [ ] Actualizar las entidades de base de datos si es necesario

## 2. Funcionalidad de Imágenes

### Problemas Identificados
- [ ] Visor de imágenes con zoom (PhotoView)
- [ ] Galería de fotos con navegación
- [ ] Carga de imágenes desde cámara/galería
- [ ] Almacenamiento de imágenes en Google Drive

### Archivos Relacionados
- `VisorImagenesAdapter.kt` - Necesita dependencias de Glide y PhotoView
- `ImageUtils.kt` - Utilidades para manejo de imágenes
- `FotosPagerAdapter.kt` - Adaptador para galería de fotos

## 3. Interfaz de Usuario

### Cambios en Layout
- [ ] `fragment_inspeccion_actual.xml` - Nueva distribución con ConstraintLayout
- [ ] `fragment_detalle_equipo.xml` - Visor de imágenes
- [ ] `item_foto.xml` - Items de galería
- [ ] `item_mini_foto.xml` - Miniaturas de fotos

### Fragmentos
- [ ] `FragmentDetalleEquipo.kt` - Detalles del equipo con galería
- [ ] `VisorImagenesFragment.kt` - Visor de imágenes con zoom
- [ ] `FragmentInspeccionActual.kt` - Formulario de inspección

## 4. Base de Datos

### Entidades a Revisar
- [ ] Estructura de tablas para almacenar rutas de imágenes
- [ ] Relaciones entre equipos e imágenes
- [ ] Campos adicionales identificados en el formulario

## 5. Funcionalidades Pendientes

### Autenticación y Seguridad
- [ ] Biometría para acceso
- [ ] Gestión de usuarios y permisos

### Sincronización
- [ ] Sincronización bidireccional con Google Sheets
- [ ] Manejo de conflictos de datos
- [ ] Logs de sincronización

### Reportes y Análisis
- [ ] Generación de reportes PDF
- [ ] Gráficos y estadísticas
- [ ] Exportación de datos

## 6. Optimizaciones

### Performance
- [ ] Carga lazy de imágenes
- [ ] Compresión de imágenes
- [ ] Cache de datos

### UX/UI
- [ ] Animaciones y transiciones
- [ ] Temas y estilos consistentes
- [ ] Accesibilidad

## 7. Testing

### Pruebas Pendientes
- [ ] Pruebas unitarias
- [ ] Pruebas de integración
- [ ] Pruebas de UI
- [ ] Pruebas de sincronización

## 8. Documentación

### Pendiente
- [ ] Documentación de API
- [ ] Manual de usuario
- [ ] Guía de desarrollo
- [ ] Diagramas de arquitectura

## Notas Importantes

### Dependencias Agregadas
- Glide 4.16.0 para carga de imágenes
- PhotoView 2.3.0 para zoom de imágenes
- JitPack repository configurado

### Estado Actual
- ✅ Compilación exitosa
- ✅ Instalación en dispositivo
- ✅ Dependencias básicas configuradas
- ⏳ Pendiente: Resolver mapeo de columnas de spreadsheets
- ⏳ Pendiente: Implementar funcionalidad de imágenes

### Próximos Pasos
1. Probar la aplicación instalada
2. Identificar la nueva estructura de columnas en spreadsheets
3. Actualizar el mapeo de datos
4. Implementar funcionalidad de imágenes paso a paso 