@echo off
setlocal
cd /d "%~dp0.."
call "%~dp0build-editor.bat"
if errorlevel 1 exit /b %errorlevel%
echo [2gameRL] Iniciando Studio...
java -cp "target\app-input\*" com.buttclapdev.twogamerl.studio.DesktopLauncher
set ERR=%errorlevel%
if not "%ERR%"=="0" echo ERROR: Studio termino con codigo %ERR%.
exit /b %ERR%
