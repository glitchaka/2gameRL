# 2gameRL Studio

Motor 2D y editor visual de escritorio escrito únicamente con Java 21, Swing y AWT.

## Funciones actuales

- Editor visual de niveles sobre cuadrícula.
- Herramientas lápiz, rectángulo, relleno y posición inicial del jugador.
- Creación, eliminación y redimensionado de niveles.
- Biblioteca gráfica con importación de PNG/JPG/GIF y previsualización.
- Definición visual de tiles, color de respaldo, colisión/transitabilidad y sprite asignado.
- Editor visual de menús con múltiples pantallas, botones arrastrables y acciones.
- Guardado y carga en un único archivo `.2grl` que contiene proyecto, niveles, menús y gráficos.
- Previsualización jugable desde el editor con F5.
- Runtime con movimiento WASD/flechas, colisión, menús y retorno al menú con ESC.
- Exportación desde el editor a una aplicación autocontenida mediante `jpackage`.
- Empaquetado del propio editor como aplicación de Windows.

## Requisito

JDK 21 completo. La exportación usa las herramientas estándar `javac`, `jar` y `jpackage` incluidas en el JDK.

## Ejecutar el editor

Windows:

```bat
scripts\build-editor.bat
java -jar build\2gameRL-Studio.jar
```

Linux/macOS:

```bash
bash scripts/build-editor.sh
java -jar build/2gameRL-Studio.jar
```

## Crear el editor como `.exe`

En Windows:

```bat
scripts\package-editor.bat
```

Resultado:

`build\editor-package\2gameRL Studio\2gameRL Studio.exe`

## Exportar un juego

Desde **Archivo > Exportar juego ejecutable…** o con:

```bat
scripts\export-game.bat ruta\proyecto.2grl ruta\salida
```

En Windows se genera una imagen de aplicación con runtime Java incluido y el launcher `2gameRL.exe`; el jugador final no necesita instalar Java.
