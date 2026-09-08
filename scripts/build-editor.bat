@echo off
setlocal
cd /d "%~dp0.."
where java >nul 2>nul || (echo ERROR: Java no esta instalado o no esta en PATH.& exit /b 1)
where mvn >nul 2>nul || (echo ERROR: Maven no esta instalado o no esta en PATH.& echo Instala Maven 3.9+ y vuelve a ejecutar.& exit /b 1)
echo [2gameRL] Compilando Studio...
call mvn -DskipTests package
if errorlevel 1 exit /b %errorlevel%
echo [2gameRL] OK: target\app-input\2gameRL-Studio.jar
endlocal
