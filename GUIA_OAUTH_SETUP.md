# Guía de Configuración OAuth 2.0 para Bitherm Management

## **¿Qué es OAuth 2.0 y por qué usarlo?**

OAuth 2.0 permite que cada usuario use su propia cuenta de Google para acceder a:
- **Google Sheets** - Sus propias hojas de cálculo
- **Google Drive** - Sus propias carpetas y archivos
- **Google Docs** - Sus propios documentos

### **Ventajas sobre las cuentas de servicio:**
✅ **Más personalizable** - Cada usuario usa su propia cuenta  
✅ **Más seguro** - No necesitas manejar credenciales de servicio  
✅ **Más flexible** - Acceso a todos los recursos del usuario  
✅ **Más fácil de configurar** - No necesitas crear cuentas de servicio  
✅ **Menos costoso** - No necesitas proyectos separados por delegación  

## **1. Configuración en Google Cloud Console**

### **Paso 1: Crear un proyecto (solo una vez)**
1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un proyecto llamado `bitherm-management-oauth`
3. Este proyecto será compartido por todas las delegaciones

### **Paso 2: Habilitar APIs**
1. Ve a "APIs y servicios" > "Biblioteca"
2. Habilita estas APIs:
   - **Google Sheets API**
   - **Google Drive API**
   - **Google Docs API** (opcional)

### **Paso 3: Configurar OAuth 2.0**
1. Ve a "APIs y servicios" > "Pantalla de consentimiento de OAuth"
2. Selecciona "Externo" y haz clic en "Crear"
3. Completa la información:
   - **Nombre de la aplicación**: `Bitherm Management`
   - **Correo electrónico de soporte**: Tu email
   - **Dominio de la aplicación**: `bitherm.com` (o tu dominio)
   - **Correo electrónico del desarrollador**: Tu email

### **Paso 4: Agregar scopes**
1. En "Scopes", haz clic en "Agregar o quitar scopes"
2. Agrega estos scopes:
   - `https://www.googleapis.com/auth/spreadsheets`
   - `https://www.googleapis.com/auth/drive.file`
   - `https://www.googleapis.com/auth/drive.readonly`

### **Paso 5: Crear credenciales OAuth**
1. Ve a "APIs y servicios" > "Credenciales"
2. Haz clic en "Crear credenciales" > "ID de cliente de OAuth 2.0"
3. Selecciona "Aplicación de Android"
4. Nombre: `Bitherm Management Android`
5. **Package name**: `com.bithermmanagement`
6. **SHA-1 fingerprint**: (se genera automáticamente)

## **2. Configuración en la aplicación**

### **Paso 1: Configurar método de autenticación**
1. Abre la aplicación Bitherm Management
2. En la pantalla de login, haz clic en el icono de engranaje (⚙️)
3. En "Configuración de Google Cloud":
   - Selecciona **"Usar cuenta de Google (OAuth)"**
   - Haz clic en **"Conectar con Google"**
   - Selecciona tu cuenta de Google
   - Autoriza los permisos solicitados

### **Paso 2: Configurar recursos**
1. **Spreadsheet ID**: ID de tu hoja de cálculo de Google Sheets
2. **Drive Folder ID**: ID de tu carpeta de Google Drive
3. **Nombre de la empresa**: Nombre de tu delegación

### **Paso 3: Probar conexión**
1. Haz clic en **"Probar conexión"**
2. Si todo está bien, verás "Conexión OAuth exitosa"

## **3. Configuración para cada delegación**

### **Opción A: Cada delegación usa su propia cuenta**
- Cada delegación se conecta con su propia cuenta de Google
- Cada delegación tiene sus propias hojas de cálculo y carpetas
- Máxima seguridad y aislamiento

### **Opción B: Todas las delegaciones comparten una cuenta**
- Una cuenta principal para toda la empresa
- Todas las delegaciones comparten los mismos recursos
- Más fácil de gestionar pero menos seguro

## **4. Estructura recomendada de recursos**

### **Google Sheets - Estructura por delegación:**
```
📊 Bitherm Management - Delegación Madrid
├── 📋 TRABAJADORES
├── 📋 FICHAJES
└── 📋 REPORTES

📊 Bitherm Management - Delegación Barcelona
├── 📋 TRABAJADORES
├── 📋 FICHAJES
└── 📋 REPORTES
```

### **Google Drive - Estructura por delegación:**
```
📁 Bitherm Management
├── 📁 Delegación Madrid
│   ├── 📸 Fotos
│   ├── 📄 Documentos
│   └── 📊 Reportes
├── 📁 Delegación Barcelona
│   ├── 📸 Fotos
│   ├── 📄 Documentos
│   └── 📊 Reportes
└── 📁 Delegación Valencia
    ├── 📸 Fotos
    ├── 📄 Documentos
    └── 📊 Reportes
```

## **5. Cómo obtener IDs de recursos**

### **Spreadsheet ID:**
1. Abre tu hoja de cálculo en Google Sheets
2. Copia el ID de la URL:
   ```
   https://docs.google.com/spreadsheets/d/[SPREADSHEET_ID]/edit
   ```
3. Ejemplo: `1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4`

### **Drive Folder ID:**
1. Abre tu carpeta en Google Drive
2. Copia el ID de la URL:
   ```
   https://drive.google.com/drive/folders/[FOLDER_ID]
   ```
3. Ejemplo: `1ABC123DEF456GHI789JKL`

## **6. Troubleshooting**

### **Error: "Debes conectar con Google primero"**
- Asegúrate de haber seleccionado "Usar cuenta de Google (OAuth)"
- Haz clic en "Conectar con Google" y autoriza la aplicación

### **Error: "Error en la conexión OAuth"**
- Verifica que las APIs estén habilitadas en Google Cloud Console
- Asegúrate de que la cuenta tenga permisos en los recursos
- Revisa que los IDs de Spreadsheet y Folder sean correctos

### **Error: "Access denied"**
- Verifica que la cuenta tenga acceso a las hojas de cálculo y carpetas
- Asegúrate de que los recursos estén compartidos con la cuenta

### **Error: "API not enabled"**
- Ve a Google Cloud Console y habilita las APIs necesarias
- Espera unos minutos después de habilitar las APIs

## **7. Seguridad y mejores prácticas**

### **Seguridad:**
- Cada usuario debe usar su propia cuenta de Google
- No compartas credenciales entre usuarios
- Revisa regularmente los permisos de acceso

### **Gestión de usuarios:**
- Documenta qué cuenta usa cada delegación
- Mantén un registro de los recursos compartidos
- Configura alertas de acceso sospechoso

### **Backup:**
- Haz backup regular de las hojas de cálculo importantes
- Documenta la configuración de cada delegación
- Guarda copias de seguridad en Google Drive

## **8. Migración desde cuentas de servicio**

### **Si ya tienes cuentas de servicio configuradas:**
1. **No elimines** las cuentas de servicio inmediatamente
2. **Configura OAuth** en paralelo
3. **Prueba** que todo funciona con OAuth
4. **Migra** los datos si es necesario
5. **Elimina** las cuentas de servicio cuando estés seguro

### **Ventajas de la migración:**
- Mayor flexibilidad para los usuarios
- Menor complejidad de configuración
- Mejor experiencia de usuario
- Menor costo de mantenimiento

## **9. Soporte y ayuda**

### **Para problemas técnicos:**
- Revisa los logs de la aplicación
- Verifica la configuración en Google Cloud Console
- Consulta la documentación de Google APIs

### **Para problemas de permisos:**
- Verifica que la cuenta tenga acceso a los recursos
- Revisa la configuración de compartir en Google Drive/Sheets
- Asegúrate de que los IDs sean correctos

### **Para problemas de configuración:**
- Sigue la guía paso a paso
- Verifica que todas las APIs estén habilitadas
- Asegúrate de que el proyecto esté correctamente configurado
