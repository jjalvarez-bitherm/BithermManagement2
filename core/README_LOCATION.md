# Sistema de Tracking de Ubicación - Módulo CORE

## 📋 Descripción

El módulo CORE incluye un sistema completo de tracking de ubicación en segundo plano que mantiene el GPS activo de forma silenciosa y envía logs de ubicación a Google Sheets según la configuración de cada usuario.

## 🚀 Características

- **Servicio en segundo plano**: Funciona como Foreground Service con notificación mínima
- **Configuración por usuario**: Cada usuario puede tener su propio intervalo de tracking
- **Modos de tracking**: Siempre activo, solo durante trabajo, o deshabilitado
- **Optimización de batería**: Usa GPS optimizado para consumo energético
- **Integración con Google Sheets**: Envía logs automáticamente
- **Detección de lugares**: Identifica el lugar más cercano automáticamente
- **Filtro de distancia**: Solo envía si el usuario se movió más de 100 metros

## 📁 Estructura de Archivos

```
core/src/main/java/com/bithermmanagement/core/
├── location/
│   ├── LocationTrackerService.kt    # Servicio principal de tracking
│   ├── LocationManager.kt           # Interfaz para gestionar el servicio
│   └── LocationSettingsFragment.kt  # Fragmento de configuración
├── di/
│   └── LocationModule.kt            # Módulo de inyección de dependencias
├── utils/
│   └── Constants.kt                 # Constantes del sistema
└── res/layout/
    └── fragment_location_settings.xml # Layout de configuración
```

## 🔧 Configuración

### 1. Permisos Requeridos

El sistema requiere los siguientes permisos (ya incluidos en el AndroidManifest.xml):

```xml
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

### 2. Configuración en Google Sheets

El sistema lee la configuración desde la hoja **"LOCALE-LOGS"**:

| Columna A | Columna B | Columna C |
|-----------|-----------|-----------|
| username  | visible   | T_LOGS    |
| juan      | SI        | 15        |
| maria     | WK        | 30        |

**Modos de tracking:**
- **"SI"**: Tracking siempre activo
- **"WK"**: Solo cuando tiene check-in activo
- **"NO"**: Sin tracking

**T_LOGS**: Intervalo en minutos para enviar logs

### 3. Estructura de Logs

Los logs se envían a la misma hoja **"LOCALE-LOGS"**:

| Username | Fecha/Hora | Lugar | GPS Link |
|----------|------------|-------|----------|
| juan     | 15/01/2024 10:30:00 | Oficina Central | https://maps.google.com/... |

## 💻 Uso del LocationManager

### Inyección de Dependencias

```kotlin
@Inject
lateinit var locationManager: LocationManager
```

### Métodos Principales

```kotlin
// Iniciar tracking
locationManager.startLocationTracking()

// Detener tracking
locationManager.stopLocationTracking()

// Verificar si está habilitado
val isEnabled = locationManager.isLocationTrackingEnabled()

// Habilitar/deshabilitar
locationManager.setLocationTrackingEnabled(true)

// Configurar intervalo (en minutos)
locationManager.setTrackingInterval(15L)

// Obtener intervalo actual
val interval = locationManager.getTrackingInterval()

// Verificar permisos
val hasPermissions = locationManager.hasLocationPermissions()

// Reiniciar servicio (útil para aplicar cambios)
locationManager.restartLocationTracking()
```

### Ejemplo de Uso en un Fragmento

```kotlin
@AndroidEntryPoint
class MyFragment : Fragment() {
    
    @Inject
    lateinit var locationManager: LocationManager
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Iniciar tracking si está habilitado
        if (locationManager.isLocationTrackingEnabled()) {
            locationManager.startLocationTracking()
        }
    }
}
```

## 🔄 Flujo de Funcionamiento

1. **Inicio del Servicio**: Se inicia como Foreground Service con notificación mínima
2. **Lectura de Configuración**: Lee la configuración del usuario desde Google Sheets
3. **Verificación de Modo**: 
   - Si es "SI": Inicia tracking inmediatamente
   - Si es "WK": Verifica si tiene check-in activo
   - Si es "NO": Se detiene
4. **Tracking de Ubicación**: Obtiene ubicación según el intervalo configurado
5. **Filtro de Distancia**: Solo procesa si se movió más de 100 metros
6. **Envío a Sheets**: Guarda la ubicación con lugar más cercano y enlace de Google Maps
7. **Repetición**: Continúa el ciclo según el intervalo configurado

## ⚙️ Configuración Avanzada

### Personalizar Intervalos

```kotlin
// Configurar intervalo personalizado
locationManager.setTrackingInterval(30L) // 30 minutos

// Reiniciar para aplicar cambios
locationManager.restartLocationTracking()
```

### Verificar Estado del Servicio

```kotlin
// Verificar si el servicio está activo
val isActive = locationManager.isLocationTrackingEnabled()

// Obtener configuración actual
val interval = locationManager.getTrackingInterval()
val mode = locationManager.getTrackingMode()
```

## 🛠️ Troubleshooting

### Problemas Comunes

1. **Servicio no inicia**: Verificar permisos de ubicación
2. **No envía logs**: Verificar conexión a internet y credenciales de Google Sheets
3. **Consumo de batería alto**: El sistema está optimizado, pero puede ajustar el intervalo
4. **Lugar desconocido**: Verificar que la hoja "EQUIPOS" tenga coordenadas válidas

### Logs de Debug

El sistema incluye logs detallados con el tag "LocationService":

```bash
adb logcat | grep LocationService
```

## 📱 Integración con Otros Módulos

Para usar el LocationManager en otros módulos:

1. **Agregar dependencia al módulo CORE**:
```kotlin
implementation(project(":core"))
```

2. **Inyectar LocationManager**:
```kotlin
@Inject
lateinit var locationManager: LocationManager
```

3. **Usar los métodos disponibles** según las necesidades del módulo.

## 🔒 Seguridad

- El servicio funciona con notificación mínima (VISIBILITY_SECRET)
- Los datos se envían de forma segura a Google Sheets
- No almacena ubicaciones localmente de forma permanente
- Respeta los permisos del usuario

## 📈 Optimizaciones

- **GPS optimizado**: Usa `PRIORITY_BALANCED_POWER_ACCURACY`
- **Filtro de distancia**: Evita logs innecesarios
- **Gestión de jobs**: Cancela jobs anteriores antes de crear nuevos
- **Manejo de errores**: Recuperación automática de errores de red 