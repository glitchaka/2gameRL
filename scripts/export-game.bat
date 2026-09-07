@echo off
setlocal
if "%~1"=="" (echo Falta archivo .2grl & exit /b 2)
if "%~2"=="" (echo Falta carpeta de destino & exit /b 2)
set PROJECT=%~1
set OUT=%~2
set ROOT=%~dp0..
set BUILD=%ROOT%\build\game-export
if exist "%BUILD%" rmdir /s /q "%BUILD%"
mkdir "%BUILD%\classes" "%BUILD%\embed" "%BUILD%\input" 2>nul
for /r "%ROOT%\src\main\java" %%f in (*.java) do echo "%%f">>"%BUILD%\sources.txt"
echo Compilando runtime...
javac --release 21 -encoding UTF-8 -d "%BUILD%\classes" @"%BUILD%\sources.txt" || exit /b 1
copy /y "%PROJECT%" "%BUILD%\embed\game-project.2grl" >nul
jar --create --file "%BUILD%\input\2gameRL-game.jar" --main-class com.buttclapdev.twogamerl.runtime.GameLauncher -C "%BUILD%\classes" . || exit /b 1
jar --update --file "%BUILD%\input\2gameRL-game.jar" -C "%BUILD%\embed" game-project.2grl || exit /b 1
if not exist "%OUT%" mkdir "%OUT%"
echo Empaquetando ejecutable...
jpackage --type app-image --name "2gameRL" --input "%BUILD%\input" --main-jar "2gameRL-game.jar" --main-class com.buttclapdev.twogamerl.runtime.GameLauncher --dest "%OUT%" || exit /b 1
echo Listo: %OUT%\2gameRL\2gameRL.exe
