@echo off
setlocal EnableExtensions
cd /d "%~dp0.."
call "%~dp0build-editor.bat"
if errorlevel 1 exit /b %errorlevel%
if "%JAVA_HOME%"=="" (echo ERROR: JAVA_HOME debe apuntar a un JDK 21 completo.& exit /b 1)
if not exist "%JAVA_HOME%\bin\jpackage.exe" (echo ERROR: %JAVA_HOME% no contiene jpackage.exe.& exit /b 1)
if not exist "%JAVA_HOME%\bin\java.exe" (echo ERROR: %JAVA_HOME% no contiene java.exe.& exit /b 1)
set "APP_VERSION=%~1"
if not "%APP_VERSION%"=="" goto version_ready
set "VERSION_FILE=%TEMP%\2gamerl-version.txt"
powershell.exe -NoProfile -Command "([xml](Get-Content -Raw 'pom.xml')).project.version" > "%VERSION_FILE%"
if errorlevel 1 (echo ERROR: No se pudo leer pom.xml con PowerShell.& exit /b 1)
set /p APP_VERSION=<"%VERSION_FILE%"
del /q "%VERSION_FILE%" >nul 2>nul
:version_ready
if "%APP_VERSION%"=="" (echo ERROR: No se pudo obtener la version desde pom.xml.& exit /b 1)
set "APP_VERSION=%APP_VERSION: =%"
set "STUDIO_DIR=build\2gameRL Studio"
set "ICON_FILE=build\2rl-icon.ico"
if exist "%STUDIO_DIR%" rmdir /s /q "%STUDIO_DIR%"
if not exist build mkdir build
echo [2gameRL] Generando icono 2RL...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0make-icon.ps1" -Output "%ICON_FILE%"
if errorlevel 1 (echo ERROR: No se pudo generar el icono 2RL.& exit /b %errorlevel%)
if not exist "%ICON_FILE%" (echo ERROR: No se genero %ICON_FILE%.& exit /b 1)
echo [2gameRL] Creando 2gameRL Studio %APP_VERSION% para Windows...
"%JAVA_HOME%\bin\jpackage.exe" --type app-image --input "target\app-input" --dest "build" --name "2gameRL Studio" --main-jar "2gameRL-Studio.jar" --main-class "com.buttclapdev.twogamerl.studio.DesktopLauncher" --app-version "%APP_VERSION%" --description "Editor visual 2gameRL" --icon "%ICON_FILE%"
if errorlevel 1 exit /b %errorlevel%
echo [2gameRL] Integrando toolchain JDK para exportar juegos desde el Studio...
if exist "%STUDIO_DIR%\toolchain" rmdir /s /q "%STUDIO_DIR%\toolchain"
robocopy "%JAVA_HOME%" "%STUDIO_DIR%\toolchain" /E /NFL /NDL /NJH /NJS /NC /NS >nul
if errorlevel 8 (echo ERROR: No se pudo integrar el JDK de exportacion.& exit /b %errorlevel%)
if not exist "%STUDIO_DIR%\toolchain\bin\jpackage.exe" (echo ERROR: El paquete final no contiene toolchain\bin\jpackage.exe.& exit /b 1)
if not exist "%STUDIO_DIR%\2gameRL Studio.exe" (echo ERROR: No se genero 2gameRL Studio.exe.& exit /b 1)
echo.
echo LISTO: %STUDIO_DIR%\2gameRL Studio.exe
echo El runtime del Studio y el JDK usado para exportar juegos estan separados.
endlocal
exit /b 0
