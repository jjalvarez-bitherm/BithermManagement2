# 🔐 Configuración de Credenciales - Bitherm Management

## ⚠️ IMPORTANTE: Seguridad

**NUNCA subas archivos de credenciales reales a GitHub.** Estos archivos contienen claves privadas que pueden comprometer tu proyecto.

## 📁 Archivos de Credenciales

### Service Account (Cuenta de Servicio)
- **Ubicación:** `app/src/main/assets/credentials_default.json`
- **Propósito:** Autenticación automática para Google APIs
- **Formato:** Ver `CREDENTIALS_EXAMPLE.json`

### Credenciales Personalizadas
- **Ubicación:** `app/filesDir/credentials.json`
- **Propósito:** Credenciales específicas del usuario
- **Configuración:** Desde la pantalla de Configuración de la app

## 🚀 Configuración Inicial

### 1. Crear Proyecto en Google Cloud Console
1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto o selecciona uno existente
3. Habilita las APIs necesarias:
   - Google Sheets API
   - Google Drive API

### 2. Crear Service Account
1. En "IAM & Admin" > "Service Accounts"
2. Crea una nueva cuenta de servicio
3. Descarga el archivo JSON de credenciales
4. **RENOMBRA** el archivo a `credentials_default.json`
5. **COPIA** el archivo a `app/src/main/assets/`

### 3. Configurar Permisos
1. Comparte tu Google Sheet con el email de la Service Account
2. Asigna permisos de "Editor" o "Lector" según necesites

## 🔧 Configuración en la App

### Opción 1: Usar Credenciales por Defecto
- Selecciona "Usar cuenta de servicio" en Configuración
- Selecciona "Usar credenciales por defecto"
- La app usará automáticamente `credentials_default.json`

### Opción 2: Usar Credenciales Personalizadas
- Selecciona "Usar cuenta de servicio" en Configuración
- Selecciona "Usar credenciales personalizadas"
- Sube tu archivo `credentials.json` desde la app

### Opción 3: Usar OAuth 2.0
- Selecciona "Usar cuenta de Google (OAuth)" en Configuración
- Conecta tu cuenta de Google
- No se requieren archivos de credenciales

## 📋 Verificación

Para verificar que las credenciales funcionan:
1. Ve a Configuración en la app
2. Haz clic en "Probar Conexión"
3. Deberías ver "Conexión exitosa"

## 🚨 Solución de Problemas

### Error: "Invalid JWT Signature"
- Verifica que el archivo de credenciales esté completo
- Asegúrate de que la Service Account tenga permisos en el Sheet

### Error: "403 Forbidden"
- Verifica que la Service Account esté compartida con el Google Sheet
- Comprueba que las APIs estén habilitadas en Google Cloud Console

### Error: "No se pudieron obtener credenciales"
- Verifica que `credentials_default.json` esté en `app/src/main/assets/`
- Comprueba que la configuración en la app sea correcta

## 🔒 Seguridad Adicional

- **NUNCA** commits credenciales reales
- **SIEMPRE** usa `.gitignore` para excluir archivos sensibles
- **ROTAR** las claves periódicamente
- **LIMITAR** permisos de la Service Account al mínimo necesario

## 📞 Soporte

Si tienes problemas con la configuración:
1. Revisa los logs de la aplicación
2. Verifica la configuración en Google Cloud Console
3. Comprueba que las APIs estén habilitadas
4. Asegúrate de que los permisos estén configurados correctamente
