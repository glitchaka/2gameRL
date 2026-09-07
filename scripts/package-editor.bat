@echo off
setlocal
set ROOT=%~dp0..
call "%~dp0build-editor.bat" || exit /b 1
set PKG=%ROOT%\build\editor-package
if exist "%PKG%" rmdir /s /q "%PKG%"
if exist "%ROOT%\build\jpackage-input" rmdir /s /q "%ROOT%\build\jpackage-input"
mkdir "%ROOT%\build\jpackage-input" 2>nul
copy /y "%ROOT%\build\2gameRL-Studio.jar" "%ROOT%\build\jpackage-input\2gameRL-Studio.jar" >nul
jpackage --type app-image --name "2gameRL Studio" --input "%ROOT%\build\jpackage-input" --main-jar "2gameRL-Studio.jar" --main-class com.buttclapdev.twogamerl.App --dest "%PKG%" || exit /b 1
mkdir "%PKG%\2gameRL Studio\authoring" 2>nul
xcopy /e /i /y "%ROOT%\src" "%PKG%\2gameRL Studio\authoring\src" >nul
xcopy /e /i /y "%ROOT%\scripts" "%PKG%\2gameRL Studio\authoring\scripts" >nul
echo Editor ejecutable: %PKG%\2gameRL Studio\2gameRL Studio.exe
