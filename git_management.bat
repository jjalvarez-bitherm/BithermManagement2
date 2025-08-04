@echo off
echo ========================================
echo    GESTION GIT - BITHERMMANAGEMENT
echo ========================================
echo.

:menu
echo Selecciona una opcion:
echo 1. Hacer commit con backup automatico
echo 2. Ver historial de cambios
echo 3. Revertir a version anterior
echo 4. Crear rama de desarrollo
echo 5. Conectar con GitHub
echo 6. Salir
echo.
set /p choice="Opcion: "

if "%choice%"=="1" goto commit_with_backup
if "%choice%"=="2" goto show_history
if "%choice%"=="3" goto revert_changes
if "%choice%"=="4" goto create_branch
if "%choice%"=="5" goto connect_github
if "%choice%"=="6" goto exit
goto menu

:commit_with_backup
echo.
echo ========================================
echo    HACIENDO COMMIT CON BACKUP
echo ========================================
echo.

:: Crear backup antes del commit
call backup_system.bat

:: Agregar todos los cambios
git add .

:: Solicitar mensaje de commit
set /p commit_msg="Mensaje del commit: "
git commit -m "%commit_msg% - Fecha: %date% %time%"

echo.
echo ========================================
echo    COMMIT REALIZADO EXITOSAMENTE
echo ========================================
echo.
pause
goto menu

:show_history
echo.
echo ========================================
echo    HISTORIAL DE CAMBIOS
echo ========================================
echo.
git log --oneline --graph --all
echo.
pause
goto menu

:revert_changes
echo.
echo ========================================
echo    REVERTIR CAMBIOS
echo ========================================
echo.
echo Historial de commits:
git log --oneline -10
echo.
set /p commit_hash="Hash del commit al que quieres volver: "
echo.
echo ADVERTENCIA: Esto revertira todos los cambios posteriores al commit %commit_hash%
set /p confirm="¿Estas seguro? (s/n): "
if /i "%confirm%"=="s" (
    git reset --hard %commit_hash%
    echo.
    echo ========================================
    echo    CAMBIOS REVERTIDOS EXITOSAMENTE
    echo ========================================
) else (
    echo Operacion cancelada.
)
echo.
pause
goto menu

:create_branch
echo.
echo ========================================
echo    CREAR RAMA DE DESARROLLO
echo ========================================
echo.
set /p branch_name="Nombre de la rama: "
git checkout -b %branch_name%
echo.
echo Rama %branch_name% creada y activada.
echo.
pause
goto menu

:connect_github
echo.
echo ========================================
echo    CONECTAR CON GITHUB
echo ========================================
echo.
echo Para conectar con GitHub:
echo 1. Ve a https://github.com
echo 2. Crea un nuevo repositorio llamado "BithermManagement2"
echo 3. NO inicialices con README
echo 4. Copia la URL del repositorio
echo.
set /p github_url="URL del repositorio GitHub: "
git remote add origin %github_url%
git push -u origin master
echo.
echo ========================================
echo    CONECTADO CON GITHUB EXITOSAMENTE
echo ========================================
echo.
pause
goto menu

:exit
echo.
echo ========================================
echo    SALIENDO...
echo ========================================
exit 