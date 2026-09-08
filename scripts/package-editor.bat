@echo off
setlocal
cd /d "%~dp0.."
call "%~dp0build-editor.bat"
if errorlevel 1 exit /b %errorlevel%
if "%JAVA_HOME%"=="" (echo ERROR: JAVA_HOME debe apuntar a un JDK 21 completo para crear el editor autocontenido.& exit /b 1)
if not exist "%JAVA_HOME%\bin\jpackage.exe" (echo ERROR: %JAVA_HOME% no contiene jpackage.exe.& exit /b 1)
for /f "usebackq delims=" %%v in (`mvn -q -DforceStdout help:evaluate -Dexpression=project.version`) do set "APP_VERSION=%%v"
if "%APP_VERSION%"=="" (echo ERROR: No se pudo obtener la version desde pom.xml.& exit /b 1)
if exist "build\2gameRL Studio" rmdir /s /q "build\2gameRL Studio"
if not exist build mkdir build
echo [2gameRL] Creando 2gameRL Studio %APP_VERSION% autocontenido...
"%JAVA_HOME%\bin\jpackage.exe" --type app-image --input "target\app-input" --dest "build" --name "2gameRL Studio" --main-jar "2gameRL-Studio.jar" --main-class "com.buttclapdev.twogamerl.studio.DesktopLauncher" --runtime-image "%JAVA_HOME%" --app-version "%APP_VERSION%" --description "Editor visual 2gameRL"
if errorlevel 1 exit /b %errorlevel%
echo.
echo LISTO: build\2gameRL Studio\2gameRL Studio.exe
echo El JDK completo queda integrado para que Exportar funcione desde el propio editor.
endlocal
