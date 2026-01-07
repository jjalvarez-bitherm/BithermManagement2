@echo off
echo ========================================
echo  BITHERM MANAGEMENT - Detener Servidores
echo ========================================
echo.

echo Deteniendo PM2...
pm2 delete all

echo Deteniendo procesos Node...
taskkill /F /IM node.exe 2>nul

echo.
echo Servidores detenidos correctamente.
echo.
pause
