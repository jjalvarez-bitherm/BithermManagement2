@echo off
echo ========================================
echo  BITHERM MANAGEMENT - Iniciar Servidores
echo ========================================
echo.

REM Detener PM2 si está corriendo
pm2 delete all 2>nul

REM Detener procesos Node residuales
taskkill /F /IM node.exe 2>nul
timeout /t 2 /nobreak >nul

echo [1/2] Iniciando Backend con PM2...
pm2 start backend/index.js --name bitherm-backend
timeout /t 3 /nobreak >nul

echo [2/2] Iniciando Frontend en ventana separada...
start "Frontend - Port 5173" powershell -NoExit -Command "npm run dev"

timeout /t 3 /nobreak >nul
echo.
echo ========================================
echo  SERVIDORES INICIADOS CORRECTAMENTE
echo ========================================
echo.
echo  Backend:  http://localhost:3001 (PM2 - Auto-restart)
echo  Frontend: http://localhost:5173 (Ventana separada)
echo.
echo Comandos utiles:
echo   pm2 list          - Ver estado del backend
echo   pm2 logs          - Ver logs del backend
echo   pm2 restart all   - Reiniciar backend
echo   pm2 stop all      - Detener backend
echo.
pause
