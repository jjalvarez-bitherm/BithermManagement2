@echo off
echo ========================================
echo    SISTEMA DE BACKUP BITHERMMANAGEMENT
echo ========================================
echo.

:: Crear directorio de backup con fecha y hora
set TIMESTAMP=%date:~-4,4%-%date:~-7,2%-%date:~-10,2%_%time:~0,2%-%time:~3,2%-%time:~6,2%
set TIMESTAMP=%TIMESTAMP: =0%
set BACKUP_DIR=C:\Users\jjalv\AndroidStudioProjects\BithermManagement2\backups\backup_%TIMESTAMP%

echo Creando backup en: %BACKUP_DIR%
mkdir "%BACKUP_DIR%"

:: Copiar todo el proyecto excepto backups y .git
xcopy "C:\Users\jjalv\AndroidStudioProjects\BithermManagement2\*" "%BACKUP_DIR%\" /E /I /Y /EXCLUDE:backup_exclude.txt

:: Crear archivo de información del backup
echo Backup creado: %TIMESTAMP% > "%BACKUP_DIR%\backup_info.txt"
echo Fecha: %date% %time% >> "%BACKUP_DIR%\backup_info.txt"
echo Usuario: %USERNAME% >> "%BACKUP_DIR%\backup_info.txt"
echo Proyecto: BithermManagement2 >> "%BACKUP_DIR%\backup_info.txt"

echo.
echo ========================================
echo    BACKUP COMPLETADO EXITOSAMENTE
echo ========================================
echo Backup guardado en: %BACKUP_DIR%
echo.
pause 