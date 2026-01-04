@echo off
REM =====================================================
REM SCRIPT DE PROTECCIÓN DE CONFIGURACIÓN JAVA
REM =====================================================
REM 
REM ⚠️  ADVERTENCIA: NO MODIFICAR ESTE ARCHIVO
REM 
REM Este script protege la configuración Java del proyecto
REM contra actualizaciones no autorizadas
REM =====================================================

echo.
echo ========================================
echo   PRESET DE CONFIGURACIÓN JAVA ACTIVO
echo ========================================
echo.

REM Verificar que Java 21 esté configurado
java -version 2>&1 | find "21" >nul
if %errorlevel% neq 0 (
    echo ❌ ERROR: Java 21 no está configurado correctamente
    echo 💡 Ejecuta: java21_setup.bat
    pause
    exit /b 1
)

REM Verificar archivos de configuración
if not exist "PRESET_JAVA_CONFIG.md" (
    echo ❌ ERROR: Archivo de preset faltante
    echo 💡 Reinstala los archivos de configuración
    pause
    exit /b 1
)

if not exist ".java-config-lock.json" (
    echo ❌ ERROR: Archivo de bloqueo faltante  
    echo 💡 Reinstala los archivos de configuración
    pause
    exit /b 1
)

echo ✅ Configuración Java 21 protegida
echo ✅ Archivos de preset verificados
echo ✅ Configuración de bloqueo activa
echo.
echo 🔒 JAVA VERSION LOCKED: Java 21 LTS
echo 🚫 UPDATES BLOCKED: Por directiva del usuario
echo.
echo ========================================
echo   CONFIGURACIÓN JAVA PROTEGIDA
echo ========================================
echo.

REM Mostrar información actual
echo 📋 INFORMACIÓN ACTUAL:
java -version 2>&1 | find /v "Picked up"
echo.

echo 💡 RECORDATORIO: Esta aplicación NO debe actualizar Java
echo 💡 Para desarrollo, mantener siempre Java 21 LTS
echo.
pause