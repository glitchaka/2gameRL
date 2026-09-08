# 2gameRL Studio 2

Editor visual de juegos 2D/roguelike construido con **JavaFX 21**. La interfaz Swing anterior queda como código legado no utilizado por la aplicación principal.

## Qué incluye

- Entorno moderno de escritorio con tres espacios: **Escena**, **Menús** y **Gráficos**.
- Editor de escenas sobre cuadrícula con selección, movimiento de objetos, pintura de tiles, borrado y colocación de entidades.
- Jerarquía de objetos por escena.
- Doble clic sobre una entidad para abrir inmediatamente su script.
- Inspector de entidad con posición, tamaño, sprite y estado.
- Sistema de componentes/comportamientos inspirado en Unity: `Rigidbody2D`, `BoxCollider2D`, `PlayerController`, `Patrol`, `ScenePortal`, `Trigger`, `Health`, `DamageOnContact` y `Clickable`.
- Scripts 2GameScript ejecutados por el runtime con eventos `start`, `update`, `click`, `doubleClick`, `collision` y `trigger`.
- Validación de scripts dentro del editor.
- Editor visual de menús con botones arrastrables.
- Biblioteca de assets.
- **Pixel Lab**, mini editor de pixel-art de 8×8 a 32×32 para crear placeholders rápidamente.
- Proyecto único `.2grl` que conserva escenas, entidades, componentes, scripts, menús, tiles y gráficos.
- Migración automática de proyectos v1: convierte el antiguo spawn en una entidad Jugador real.
- Vista previa jugable con consola de scripts.
- Exportación nativa desde el editor mediante `jpackage`, sin depender de la antigua carpeta `scripts` en tiempo de ejecución.
- Exportación adicional a ZIP portable.

## Arrancar en Windows

Requisitos de desarrollo: **JDK 21** y **Maven 3.9+**.

```bat
scripts\run-studio.bat
```

Para uso normal descarga el ZIP de Windows desde **Releases**: el Studio viene autocontenido y no requiere instalar Maven.

## Crear 2gameRL Studio como aplicación autocontenida

Configura `JAVA_HOME` apuntando al JDK 21 completo y ejecuta:

```bat
scripts\package-editor.bat
```

Resultado: `build\2gameRL Studio\2gameRL Studio.exe`.

El empaquetado del editor incluye el JDK completo para conservar `jpackage`; de esta forma **Exportar juego** también funciona desde el Studio empaquetado.

## Exportar un juego

Desde Studio: botón **Exportar**. La salida contiene la aplicación autocontenida, un ZIP portable y un LEEME.

También puede ejecutarse por consola:

```bat
scripts\export-game.bat proyecto.2grl carpeta-salida
```

## Interacción principal

- **Clic**: seleccionar objeto.
- **Arrastrar**: mover objeto seleccionado.
- **Doble clic**: abrir Script de ese objeto.
- **Tile**: activa pintura de tiles.
- **Borrar**: vuelve la celda a suelo.
- **Objeto**: crea una entidad nueva y regresa automáticamente a Seleccionar.
- **Inspector → Añadir comportamiento**: incorpora componentes configurables.
- **Gráficos → Pixel Lab**: crea sprites placeholder sin salir de Studio.

## Recursos de muestra heredados

La versión 2.0.2 reincorpora la hoja `test.png` de la rama histórica `agregamos-los-sprites`. El Studio la carga como **Sample Texture Sheet 32px** y expone automáticamente sus 100 celdas de 32×32 como sprites individuales (`sample-00-00.png` … `sample-09-09.png`) dentro de la biblioteca gráfica de todo proyecto nuevo.

## Versionado y Releases

Cada cambio que llega a `master` pasa por GitHub Actions. Si compilación, tests, arranque del Studio, arranque del runtime y exportación terminan correctamente, el workflow crea un Release inmutable con tag `v<versión>-<sha>` y adjunta el paquete Windows correspondiente. Un cambio no sustituye silenciosamente al Release anterior.
