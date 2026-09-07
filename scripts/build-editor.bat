@echo off
setlocal
set ROOT=%~dp0..
set BUILD=%ROOT%\build
if exist "%BUILD%\classes" rmdir /s /q "%BUILD%\classes"
mkdir "%BUILD%\classes" 2>nul
if exist "%BUILD%\sources.txt" del /q "%BUILD%\sources.txt"
for /r "%ROOT%\src\main\java" %%f in (*.java) do echo "%%f">>"%BUILD%\sources.txt"
javac --release 21 -encoding UTF-8 -d "%BUILD%\classes" @"%BUILD%\sources.txt" || exit /b 1
jar --create --file "%BUILD%\2gameRL-Studio.jar" --main-class com.buttclapdev.twogamerl.App -C "%BUILD%\classes" . || exit /b 1
echo Creado: %BUILD%\2gameRL-Studio.jar
