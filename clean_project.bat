@echo off
echo Limpiando proyecto BithermManagement...

REM Limpiar carpetas build
echo Limpiando carpetas build...
for /d /r . %%d in (build) do @if exist "%%d" rd /s /q "%%d" 2>nul

REM Limpiar archivos de lint
echo Limpiando archivos de lint...
if exist "app\lint-baseline.xml" del /q "app\lint-baseline.xml"

REM Limpiar esquemas de Room antiguos (mantener solo el más reciente)
echo Limpiando esquemas de Room antiguos...
if exist "app\schemas" (
    for /d %%d in ("app\schemas\*") do (
        pushd "%%d"
        for /f "tokens=*" %%f in ('dir /b /o-n *.json 2^>nul') do (
            set "first=%%f"
            goto :found
        )
        :found
        for %%f in (*.json) do (
            if not "%%f"=="!first!" del "%%f"
        )
        popd
    )
)

REM Limpiar archivos temporales de Gradle
echo Limpiando archivos temporales de Gradle...
if exist ".gradle" rd /s /q ".gradle" 2>nul

echo Limpieza completada!
pause 