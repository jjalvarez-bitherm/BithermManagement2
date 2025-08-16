# Guía Completa de Configuración - Bitherm Management

## **🎯 Opciones de Autenticación Disponibles**

La aplicación Bitherm Management ofrece **3 opciones de autenticación** para que cada delegación pueda elegir la que mejor se adapte a sus necesidades:

---

## **1. 🔐 Cuenta de Servicio por Defecto (RECOMENDADO)**

### **¿Qué es?**
- Usa las credenciales preconfiguradas de Bitherm Management
- No requiere configuración adicional
- Funciona inmediatamente después de la instalación

### **Ventajas:**
✅ **Configuración automática** - No necesitas hacer nada  
✅ **Funciona inmediatamente** - Listo para usar  
✅ **Mantenimiento centralizado** - Gestionado por el desarrollador  
✅ **Sin costos adicionales** - Usa el proyecto compartido  

### **Configuración:**
1. **Selecciona "Cuenta de servicio"**
2. **Selecciona "Usar credenciales por defecto (recomendado)"**
3. **Configura tu Spreadsheet ID y Drive Folder ID**
4. **¡Listo!**

### **Credenciales por defecto:**
- **Project ID**: `bithermmanagement-469102`
- **Service Account**: `bitherm-management@bithermmanagement-469102.iam.gserviceaccount.com`

---

## **2. 🔑 Cuenta de Servicio Personalizada**

### **¿Qué es?**
- Usa tus propias credenciales de Google Cloud
- Control total sobre tu proyecto de Google Cloud
- Aislamiento completo de datos

### **Ventajas:**
✅ **Control total** - Tu propio proyecto de Google Cloud  
✅ **Aislamiento de datos** - Completamente separado  
✅ **Personalización** - Configuración específica para tu delegación  
✅ **Seguridad** - Credenciales propias  

### **Configuración:**
1. **Selecciona "Cuenta de servicio"**
2. **Selecciona "Subir credenciales personalizadas"**
3. **Sube tu archivo JSON de credenciales**
4. **Configura Project ID y Service Account Email**
5. **Configura tu Spreadsheet ID y Drive Folder ID**

### **Cómo obtener credenciales personalizadas:**
1. Ve a [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuevo proyecto
3. Habilita las APIs (Sheets, Drive, Docs)
4. Crea una cuenta de servicio
5. Descarga el archivo JSON de credenciales

---

## **3. 👤 Cuenta de Google Personal (OAuth 2.0)**

### **¿Qué es?**
- Usa tu propia cuenta de Google
- Accede a tus propios recursos de Google Sheets y Drive
- Autenticación nativa de Google

### **Ventajas:**
✅ **Más personalizable** - Usa tu propia cuenta  
✅ **Más seguro** - No hay credenciales de servicio  
✅ **Más flexible** - Acceso a todos tus recursos  
✅ **Más fácil de configurar** - Solo autorizar la aplicación  

### **Configuración:**
1. **Selecciona "Usar cuenta de Google (OAuth)"**
2. **Haz clic en "Conectar con Google"**
3. **Selecciona tu cuenta y autoriza**
4. **Configura tu Spreadsheet ID y Drive Folder ID**

---

## **⚙️ Configuración en la Aplicación**

### **Acceso a la configuración:**
1. Abre la aplicación Bitherm Management
2. En la pantalla de login, haz clic en el icono de engranaje (⚙️)
3. Se abrirá la pantalla de configuración

### **Pantalla de configuración:**

#### **Sección 1: Método de Autenticación**
```
☐ Usar cuenta de Google (OAuth)
☑️ Cuenta de servicio
```

#### **Sección 2: Opciones de Credenciales (solo para cuenta de servicio)**
```
☑️ Usar credenciales por defecto (recomendado)
☐ Subir credenciales personalizadas
```

#### **Sección 3: Configuración de Google Cloud**
- **Project ID**: ID de tu proyecto de Google Cloud
- **Service Account Email**: Email de la cuenta de servicio
- **Spreadsheet ID**: ID de tu hoja de cálculo
- **Drive Folder ID**: ID de tu carpeta de Drive

#### **Sección 4: Configuración de la Empresa**
- **Nombre de la empresa**: Nombre de tu delegación
- **Logo de la empresa**: Subir logo personalizado

#### **Sección 5: Configuración de Horarios**
- **Horario de trabajo**: Configurar horarios de entrada y salida

---

## **📋 Escenarios de Uso Recomendados**

### **Escenario 1: Delegación Nueva (RECOMENDADO)**
```
Configuración: Cuenta de servicio por defecto
Ventajas: Funciona inmediatamente, sin configuración
Ideal para: Delegaciones que quieren empezar rápido
```

### **Escenario 2: Delegación con Recursos Propios**
```
Configuración: Cuenta de servicio personalizada
Ventajas: Control total, aislamiento de datos
Ideal para: Delegaciones con datos sensibles
```

### **Escenario 3: Delegación con Cuenta de Google**
```
Configuración: OAuth 2.0 con cuenta personal
Ventajas: Más flexible, usa recursos existentes
Ideal para: Delegaciones que ya usan Google Workspace
```

### **Escenario 4: Empresa Centralizada**
```
Configuración: OAuth 2.0 con cuenta administrativa
Ventajas: Control centralizado, recursos compartidos
Ideal para: Empresas con gestión centralizada
```

---

## **🔄 Cambio de Configuración**

### **Puedes cambiar en cualquier momento:**

#### **De credenciales por defecto a personalizadas:**
1. Cambia a "Subir credenciales personalizadas"
2. Sube tu archivo JSON
3. Configura Project ID y Service Account Email
4. Guarda configuración

#### **De cuenta de servicio a OAuth:**
1. Cambia a "Usar cuenta de Google (OAuth)"
2. Haz clic en "Conectar con Google"
3. Autoriza con tu cuenta
4. Guarda configuración

#### **De OAuth a cuenta de servicio:**
1. Cambia a "Cuenta de servicio"
2. Selecciona tipo de credenciales
3. Configura según corresponda
4. Guarda configuración

---

## **🔧 Configuración Avanzada**

### **Configuración por defecto actual:**
```json
{
  "project_id": "bithermmanagement-469102",
  "service_account_email": "bitherm-management@bithermmanagement-469102.iam.gserviceaccount.com",
  "spreadsheet_id": "1IyWGyxYDDTWY5SHh2xLBxtakSZX_xhZFo2jta4JeSW4",
  "company_name": "Bitherm Management"
}
```

### **APIs habilitadas en el proyecto por defecto:**
- Google Sheets API
- Google Drive API
- Google Docs API

### **Permisos de la cuenta de servicio por defecto:**
- Editor en Google Sheets
- Editor en Google Drive
- Acceso a recursos compartidos

---

## **🚨 Troubleshooting**

### **Error: "No se pueden obtener las credenciales"**
- Verifica que el archivo de credenciales esté correcto
- Asegúrate de que las APIs estén habilitadas
- Revisa que la cuenta de servicio esté activa

### **Error: "Debes conectar con Google primero"**
- Selecciona "Usar cuenta de Google (OAuth)"
- Haz clic en "Conectar con Google"
- Autoriza la aplicación

### **Error: "Error en la conexión"**
- Verifica que los IDs de Spreadsheet y Folder sean correctos
- Asegúrate de que los recursos estén compartidos
- Revisa que las credenciales tengan permisos

### **Error: "Access denied"**
- Verifica que la cuenta tenga acceso a los recursos
- Revisa la configuración de compartir en Google Drive/Sheets
- Asegúrate de que los IDs sean correctos

---

## **💡 Recomendaciones**

### **Para la mayoría de casos:**
1. **Empieza con credenciales por defecto** - Funciona inmediatamente
2. **Si necesitas aislamiento** - Usa credenciales personalizadas
3. **Si ya usas Google Workspace** - Considera OAuth 2.0

### **Seguridad:**
- **No compartas credenciales** entre delegaciones
- **Usa credenciales por defecto** para delegaciones pequeñas
- **Usa credenciales personalizadas** para datos sensibles
- **Usa OAuth 2.0** para máxima flexibilidad

### **Mantenimiento:**
- **Documenta la configuración** de cada delegación
- **Haz backup** de las configuraciones importantes
- **Revisa regularmente** los permisos de acceso
- **Actualiza credenciales** cuando sea necesario

---

## **📞 Soporte**

### **Para problemas técnicos:**
- Revisa los logs de la aplicación
- Verifica la configuración en Google Cloud Console
- Consulta la documentación de Google APIs

### **Para problemas de configuración:**
- Sigue esta guía paso a paso
- Verifica que todas las APIs estén habilitadas
- Asegúrate de que el proyecto esté correctamente configurado

### **Para problemas de permisos:**
- Verifica que la cuenta tenga acceso a los recursos
- Revisa la configuración de compartir en Google Drive/Sheets
- Asegúrate de que los IDs sean correctos

---

## **🎉 ¡Listo para usar!**

Con estas opciones, cada delegación puede elegir la configuración que mejor se adapte a sus necesidades:

- **🚀 Rápido y fácil**: Credenciales por defecto
- **🔒 Seguro y aislado**: Credenciales personalizadas  
- **🔄 Flexible y personalizable**: OAuth 2.0

¡La aplicación está lista para funcionar con cualquiera de estas opciones!
