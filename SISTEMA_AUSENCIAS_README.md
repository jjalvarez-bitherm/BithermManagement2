# Sistema de Gestión de Ausencias - BithermManagement

## Descripción General

El Sistema de Gestión de Ausencias es un módulo completo integrado en la aplicación BithermManagement que permite gestionar solicitudes de ausencias, aprobaciones, rechazos y seguimiento de todo el proceso de gestión de personal.

## Características Principales

### 🗓️ **Cuadrante de Ausencias**
- **Calendario visual** con indicadores de ausencias por día
- **Navegación por meses** con botones de anterior/siguiente
- **Filtros rápidos** para hoy, esta semana, este mes
- **Contadores estadísticos** por tipo y estado de ausencias
- **Botón flotante** para crear nuevas solicitudes

### 📋 **Gestión de Solicitudes**
- **Lista completa** de todas las solicitudes de ausencias
- **Filtros avanzados** por estado, tipo, prioridad y fechas
- **Acciones en tiempo real** (aprobar, rechazar, cancelar, editar)
- **Estadísticas en tiempo real** de solicitudes pendientes
- **Exportación de datos** para análisis externo

### ✏️ **Formulario de Nueva Ausencia**
- **Campos completos** para información del empleado
- **Selector de fechas** con validación automática
- **Cálculo automático** de días totales
- **Validación de campos** obligatorios
- **Tipos de ausencia** predefinidos (vacaciones, permisos, enfermedad, etc.)

### 🔄 **Sincronización con Google Sheets**
- **Integración** con hojas `AUSENCIAS-CUADRO` (lectura) y `AUSENCIAS-LOGS` (escritura)
- **Sincronización bidireccional** entre app local y Google Sheets
- **Logs de auditoría** de todas las acciones realizadas

## Arquitectura del Sistema

### **Modelos de Datos**

#### `AbsenceRecord`
- Registro principal de ausencias
- Campos: empleado, fechas, tipo, estado, descripción
- Metadatos: creación, modificación, sincronización

#### `AbsenceRequest`
- Solicitudes de ausencias pendientes de aprobación
- Campos: empleado, fechas, tipo, prioridad, descripción
- Estados: pendiente, en revisión, aprobada, rechazada, cancelada

#### `AbsenceLog`
- Registro de auditoría de todas las acciones
- Campos: acción, usuario, timestamp, detalles
- Acciones: crear, modificar, aprobar, rechazar, cancelar

### **Base de Datos**
- **Room Database** con entidades para ausencias, solicitudes y logs
- **DAOs especializados** para cada tipo de entidad
- **Migraciones automáticas** para actualizaciones de esquema
- **Índices optimizados** para consultas frecuentes

### **Repositorio y ViewModel**
- **`AbsenceRepository`**: Lógica de negocio y operaciones CRUD
- **`AbsenceViewModel`**: Estado de la UI y comunicación con el repositorio
- **Inyección de dependencias** con Hilt para modularidad

## Estructura de Archivos

```
app/src/main/java/com/bithermmanagement/ausencias/
├── models/
│   ├── AbsenceRecord.kt          # Modelo principal de ausencias
│   ├── AbsenceRequest.kt         # Modelo de solicitudes
│   └── AbsenceLog.kt             # Modelo de logs de auditoría
├── dao/
│   ├── AbsenceRecordDao.kt       # DAO para ausencias
│   ├── AbsenceRequestDao.kt      # DAO para solicitudes
│   └── AbsenceLogDao.kt          # DAO para logs
├── repository/
│   └── AbsenceRepository.kt      # Repositorio principal
├── viewmodels/
│   └── AbsenceViewModel.kt       # ViewModel principal
├── fragments/
│   ├── FragmentAusenciasCuadrante.kt    # Cuadrante principal
│   ├── FragmentAusenciasGestion.kt      # Gestión de solicitudes
│   └── FragmentNuevaAusencia.kt         # Formulario de nueva ausencia
├── adapters/
│   ├── AbsenceCalendarAdapter.kt        # Adaptador del calendario
│   └── AbsenceRequestAdapter.kt         # Adaptador de solicitudes
└── di/
    └── AbsenceModule.kt          # Módulo de Hilt
```

## Flujo de Trabajo

### 1. **Creación de Solicitud**
```
Empleado → Formulario → Validación → Base de datos local → Google Sheets
```

### 2. **Proceso de Aprobación**
```
Solicitud pendiente → Revisión → Aprobación/Rechazo → Log de auditoría → Actualización de estado
```

### 3. **Sincronización**
```
Cambios locales → Cola de sincronización → Google Sheets → Confirmación → Estado sincronizado
```

## Integración con Google Sheets

### **Hojas Utilizadas**
- **`AUSENCIAS-CUADRO`**: Datos históricos de ausencias (solo lectura)
- **`AUSENCIAS-LOGS`**: Registro de todas las acciones (lectura/escritura)

### **Estructura de Datos**
- **Formato JSON** para transferencia de datos
- **Campos mapeados** entre modelos locales y hojas de Google
- **Validación de integridad** antes de sincronización

## Permisos y Roles

### **Sistema de Roles**
- **Empleado**: Crear solicitudes, ver propias ausencias
- **Supervisor**: Aprobar/rechazar solicitudes de su equipo
- **Administrador**: Acceso completo a todas las funcionalidades

### **Control de Acceso**
- **Verificación de permisos** en cada operación
- **Logs de auditoría** para todas las acciones
- **Validación de roles** en la interfaz de usuario

## Configuración y Personalización

### **Tipos de Ausencia Configurables**
- Vacaciones
- Permisos
- Enfermedad
- Asuntos personales
- Formación
- Otros

### **Prioridades Configurables**
- Baja
- Normal
- Alta
- Urgente

### **Estados de Solicitud**
- Pendiente
- En revisión
- Aprobada
- Rechazada
- Cancelada

## Funcionalidades Técnicas

### **Validaciones**
- **Fechas**: Inicio no posterior a fin
- **Campos obligatorios**: Nombre, email, fechas, descripción
- **Lógica de negocio**: Días hábiles, límites de ausencias

### **Manejo de Errores**
- **Errores de red**: Reintentos automáticos
- **Errores de validación**: Mensajes claros al usuario
- **Errores de sincronización**: Cola de reintentos

### **Performance**
- **Lazy loading** de datos
- **Paginación** para listas grandes
- **Caché local** para consultas frecuentes
- **Índices de base de datos** optimizados

## Instalación y Configuración

### **Requisitos**
- Android API 21+
- Google Play Services
- Permisos de internet y almacenamiento

### **Dependencias**
```gradle
implementation 'androidx.room:room-runtime:2.5.0'
implementation 'androidx.room:room-ktx:2.5.0'
implementation 'com.google.android.material:material:1.9.0'
implementation 'com.google.android.gms:play-services-auth:20.6.0'
implementation 'dagger.hilt.android:hilt-android:2.44'
```

### **Configuración de Google Sheets**
1. Crear proyecto en Google Cloud Console
2. Habilitar Google Sheets API
3. Configurar credenciales de servicio
4. Compartir hojas con la cuenta de servicio

## Uso del Sistema

### **Para Empleados**
1. Acceder al cuadrante de ausencias
2. Pulsar botón flotante "+"
3. Completar formulario de solicitud
4. Enviar para aprobación
5. Seguir estado de la solicitud

### **Para Supervisores/Administradores**
1. Acceder a gestión de ausencias
2. Revisar solicitudes pendientes
3. Aprobar/rechazar según políticas
4. Ver estadísticas y reportes
5. Exportar datos para análisis

## Mantenimiento y Soporte

### **Logs y Monitoreo**
- **Logs de aplicación** para debugging
- **Métricas de uso** para análisis
- **Alertas automáticas** para errores críticos

### **Backup y Recuperación**
- **Sincronización automática** con Google Sheets
- **Backup local** de base de datos
- **Procedimientos de recuperación** documentados

### **Actualizaciones**
- **Migraciones automáticas** de base de datos
- **Compatibilidad hacia atrás** mantenida
- **Notas de versión** para cada actualización

## Roadmap Futuro

### **Próximas Funcionalidades**
- **Notificaciones push** para cambios de estado
- **Integración con calendario** del sistema
- **Reportes avanzados** y análisis
- **API REST** para integración externa
- **App web** complementaria

### **Mejoras Técnicas**
- **Offline mode** completo
- **Sincronización en tiempo real**
- **Machine learning** para aprobaciones automáticas
- **Integración con sistemas de RRHH**

## Contribución y Desarrollo

### **Estándares de Código**
- **Kotlin** como lenguaje principal
- **MVVM** como patrón arquitectónico
- **Material Design** para la interfaz
- **Testing** unitario y de integración

### **Proceso de Desarrollo**
1. Crear rama feature
2. Implementar funcionalidad
3. Tests y documentación
4. Pull request y review
5. Merge a rama principal

## Contacto y Soporte

Para soporte técnico o consultas sobre el sistema:
- **Email**: soporte@bitherm.com
- **Documentación**: [Wiki interno]
- **Issues**: [GitHub Issues]

---

**Versión**: 1.0.0  
**Última actualización**: Diciembre 2024  
**Desarrollado por**: Equipo de Desarrollo Bitherm
