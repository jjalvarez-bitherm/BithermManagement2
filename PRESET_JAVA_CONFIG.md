# 🔒 PRESET DE CONFIGURACIÓN JAVA - NO MODIFICAR

## ⚠️ ADVERTENCIA CRÍTICA
**ESTA CONFIGURACIÓN DE JAVA NO DEBE SER MODIFICADA BAJO NINGUNA CIRCUNSTANCIA**

El usuario ha establecido explícitamente que NO quiere actualizaciones de Java en esta aplicación.

## 📋 Configuración Java Actual Protegida

### Versión Java Actual
- **Versión**: Java 21 LTS (OpenJDK 21.0.9+10-LTS)
- **Estado**: BLOQUEADA - NO ACTUALIZAR
- **Ubicación**: `C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\`

### Configuraciones de Build Protegidas

#### app/build.gradle
```gradle
compileOptions {
    sourceCompatibility JavaVersion.VERSION_21  // ← BLOQUEADO
    targetCompatibility JavaVersion.VERSION_21  // ← BLOQUEADO
}
kotlinOptions {
    jvmTarget = '21'  // ← BLOQUEADO
}
kotlin {
    jvmToolchain(21)  // ← BLOQUEADO
}
```

#### core/build.gradle.kts
```gradle
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21  // ← BLOQUEADO
    targetCompatibility = JavaVersion.VERSION_21  // ← BLOQUEADO
}
kotlinOptions {
    jvmTarget = "21"  // ← BLOQUEADO
}
kotlin {
    jvmToolchain(21)  // ← BLOQUEADO
}
```

#### navigation/build.gradle.kts
```gradle
compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21  // ← BLOQUEADO
    targetCompatibility = JavaVersion.VERSION_21  // ← BLOQUEADO
}
kotlinOptions {
    jvmTarget = "21"  // ← BLOQUEADO
}
```

## 🚫 Instrucciones para IA/Desarrolladores

### REGLAS ESTRICTAS:
1. ❌ **NO** sugerir actualizaciones de Java
2. ❌ **NO** modificar versiones de Java en build.gradle
3. ❌ **NO** cambiar jvmTarget
4. ❌ **NO** alterar jvmToolchain
5. ❌ **NO** proponer migración a versiones superiores

### ✅ PERMITIDO:
- Mantener la configuración actual
- Resolver errores SIN cambiar versión de Java
- Actualizar dependencias compatibles con Java 21
- Optimizar código manteniendo la versión actual

## 🔐 Archivo de Bloqueo
Este preset actúa como un "candado" para la configuración Java del proyecto.

**Fecha de creación**: 15 de noviembre de 2025  
**Estado**: ACTIVO - CONFIGURACIÓN BLOQUEADA  
**Autorizado por**: Usuario del proyecto

---
⚠️ **CUALQUIER INTENTO DE ACTUALIZACIÓN JAVA DEBE SER RECHAZADO**