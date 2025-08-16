# Guía de Configuración de Google Cloud Console para Bitherm Management

## 1. Crear un Proyecto en Google Cloud Console

### Paso 1: Acceder a Google Cloud Console
1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Inicia sesión con tu cuenta de Google
3. Crea un nuevo proyecto o selecciona uno existente

### Paso 2: Crear el Proyecto
1. Haz clic en el selector de proyectos en la parte superior
2. Haz clic en "Nuevo proyecto"
3. Nombre del proyecto: `bitherm-management-[DELEGACION]` (ej: `bitherm-management-madrid`)
4. Haz clic en "Crear"

## 2. Habilitar las APIs necesarias

### APIs requeridas:
- **Google Sheets API**
- **Google Drive API**
- **Google Docs API** (opcional, para futuras funcionalidades)

### Cómo habilitarlas:
1. Ve a "APIs y servicios" > "Biblioteca"
2. Busca cada API y haz clic en "Habilitar"
3. Repite para todas las APIs listadas arriba

## 3. Crear una Cuenta de Servicio

### Paso 1: Crear la cuenta
1. Ve a "IAM y administración" > "Cuentas de servicio"
2. Haz clic en "Crear cuenta de servicio"
3. Nombre: `bitherm-app`
4. Descripción: `Cuenta de servicio para Bitherm Management`
5. Haz clic en "Crear y continuar"

### Paso 2: Asignar roles
1. En "Otorgar acceso a esta cuenta de servicio", selecciona:
   - **Editor** (para acceso completo a recursos del proyecto)
   - **Usuario de la API de Google Sheets**
   - **Usuario de la API de Google Drive**
2. Haz clic en "Continuar"
3. Haz clic en "Listo"

### Paso 3: Crear clave privada
1. En la lista de cuentas de servicio, haz clic en la que acabas de crear
2. Ve a la pestaña "Claves"
3. Haz clic en "Agregar clave" > "Crear nueva clave"
4. Selecciona "JSON"
5. Haz clic en "Crear"
6. Se descargará automáticamente el archivo de credenciales

## 4. Configurar permisos en Google Sheets

### Paso 1: Crear la hoja de cálculo
1. Ve a [Google Sheets](https://sheets.google.com/)
2. Crea una nueva hoja de cálculo
3. Nombra la primera hoja como "TRABAJADORES"
4. Configura las columnas necesarias (APP, NOMBRE, ROL, etc.)

### Paso 2: Compartir con la cuenta de servicio
1. Haz clic en "Compartir" en la esquina superior derecha
2. Agrega el email de la cuenta de servicio: `bitherm-app@[PROJECT-ID].iam.gserviceaccount.com`
3. Asigna el rol "Editor"
4. Desmarca "Notificar a las personas"
5. Haz clic en "Compartir"

## 5. Configurar permisos en Google Drive

### Paso 1: Crear carpeta para archivos
1. Ve a [Google Drive](https://drive.google.com/)
2. Crea una nueva carpeta llamada "Bitherm Management"
3. Dentro de esta carpeta, crea subcarpetas según necesites:
   - Fotos
   - Documentos
   - Reportes

### Paso 2: Compartir carpeta con la cuenta de servicio
1. Haz clic derecho en la carpeta "Bitherm Management"
2. Selecciona "Compartir"
3. Agrega el email de la cuenta de servicio
4. Asigna el rol "Editor"
5. Desmarca "Notificar a las personas"
6. Haz clic en "Compartir"

## 6. Configurar la aplicación

### Paso 1: Obtener IDs necesarios
1. **Spreadsheet ID**: Copia el ID de la URL de Google Sheets
   - URL: `https://docs.google.com/spreadsheets/d/[SPREADSHEET_ID]/edit`
   - Ejemplo: `1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4`

2. **Drive Folder ID**: Copia el ID de la URL de Google Drive
   - URL: `https://drive.google.com/drive/folders/[FOLDER_ID]`
   - Ejemplo: `1ABC123DEF456GHI789JKL`

### Paso 2: Configurar en la aplicación
1. Abre la aplicación Bitherm Management
2. En la pantalla de login, haz clic en el icono de engranaje (⚙️)
3. Completa la configuración:
   - **Project ID**: `bitherm-management-[DELEGACION]`
   - **Service Account Email**: `bitherm-app@[PROJECT-ID].iam.gserviceaccount.com`
   - **Spreadsheet ID**: El ID copiado del paso anterior
   - **Drive Folder ID**: El ID de la carpeta de Drive
   - **Nombre de la empresa**: Nombre de la delegación

4. Haz clic en "Subir archivo de credenciales" y selecciona el archivo JSON descargado
5. Haz clic en "Guardar configuración"
6. Haz clic en "Probar conexión" para verificar que todo funciona

## 7. Configuración para múltiples delegaciones

### Opción 1: Un proyecto por delegación (Recomendado)
- Cada delegación tiene su propio proyecto de Google Cloud
- Mayor seguridad y aislamiento
- Más fácil de gestionar permisos

### Opción 2: Un proyecto para todas las delegaciones
- Todas las delegaciones comparten el mismo proyecto
- Menor costo pero más complejo de gestionar
- Requiere configuración cuidadosa de permisos

## 8. Estructura recomendada de carpetas en Drive

```
Bitherm Management/
├── Delegacion Madrid/
│   ├── Fotos/
│   ├── Documentos/
│   └── Reportes/
├── Delegacion Barcelona/
│   ├── Fotos/
│   ├── Documentos/
│   └── Reportes/
└── Delegacion Valencia/
    ├── Fotos/
    ├── Documentos/
    └── Reportes/
```

## 9. Troubleshooting

### Error: "Invalid JWT Signature"
- Verifica que el archivo de credenciales sea el correcto
- Asegúrate de que la cuenta de servicio esté habilitada
- Verifica que las APIs estén habilitadas

### Error: "Access denied"
- Verifica que la cuenta de servicio tenga permisos en Sheets y Drive
- Asegúrate de que los IDs de Spreadsheet y Folder sean correctos

### Error: "API not enabled"
- Ve a Google Cloud Console y habilita las APIs necesarias
- Espera unos minutos después de habilitar las APIs

## 10. Seguridad y mejores prácticas

### Seguridad:
- Nunca compartas las credenciales de la cuenta de servicio
- Usa diferentes cuentas de servicio para diferentes entornos (dev, prod)
- Revisa regularmente los permisos de las cuentas de servicio

### Costos:
- Las APIs de Google Sheets y Drive tienen cuotas gratuitas generosas
- Monitorea el uso en Google Cloud Console
- Configura alertas de facturación si es necesario

### Backup:
- Guarda una copia de las credenciales en un lugar seguro
- Documenta la configuración de cada delegación
- Haz backup regular de las hojas de cálculo importantes
