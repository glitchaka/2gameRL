@echo off
setlocal
cd /d "%~dp0.."
if "%~1"=="" (echo Uso: export-game.bat proyecto.2grl carpeta-salida& exit /b 2)
if "%~2"=="" (echo Uso: export-game.bat proyecto.2grl carpeta-salida& exit /b 2)
call "%~dp0build-editor.bat"
if errorlevel 1 exit /b %errorlevel%
java -cp "target\app-input\*" com.buttclapdev.twogamerl.export.ExportCli "%~1" "%~2"
exit /b %errorlevel%
