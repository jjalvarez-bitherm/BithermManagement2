@echo off
REM Script para configurar Java 21 LTS para este proyecto
echo Configurando Java 21 LTS para BithermManagement2...

REM Configurar variables de entorno para esta sesión
set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot\"
set "PATH=%JAVA_HOME%\bin;%PATH%"

REM Mostrar versión de Java
echo.
echo Versión de Java configurada:
java -version

echo.
echo Versión de Gradle:
gradlew --version

echo.
echo ¡Java 21 LTS configurado correctamente!
echo Para usar permanentemente, ejecuta este script antes de abrir Android Studio.
pause