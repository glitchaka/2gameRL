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

## 2GameScript

```text
on start
  log "Jugador iniciado"
end

on update
  ifPressed SPACE velocity 0 -6
end

on collision
  bounce
end
```

Comandos: `log`, `move`, `velocity`, `teleport`, `bounce`, `destroy`, `loadScene`, `setSprite`, `setVar`, `addVar`, `set`, `ifKey`, `ifPressed`.

## Arrancar en Windows

Requisitos de desarrollo: **JDK 21** y **Maven 3.9+**.

```bat
scripts\run-studio.bat
```

## Crear Studio como aplicación autocontenida

Configura `JAVA_HOME` apuntando al JDK 21 completo y ejecuta:

```bat
scripts\package-editor.bat
```

Resultado:

```text
build\2gameRL Studio\2gameRL Studio.exe
```

El editor empaquetado incorpora el JDK completo para conservar `jpackage`; por eso **Exportar juego** funciona también desde Studio instalado/empaquetado.

## Exportar un juego

Desde Studio: botón **Exportar**. La salida contiene una aplicación autocontenida, un ZIP portable y un LEEME. También puede ejecutarse:

```bat
scripts\export-game.bat proyecto.2grl carpeta-salida
```

## Interacción principal

- **Clic**: seleccionar objeto.
- **Arrastrar**: mover objeto seleccionado.
- **Doble clic**: abrir Script de ese objeto.
- **Tile**: activa pintura de tiles.
- **Borrar**: vuelve la celda a suelo.
- **Objeto**: crea una entidad nueva y vuelve a Seleccionar.
- **Inspector → Añadir comportamiento**: incorpora componentes configurables.
- **Gráficos → Pixel Lab**: crea sprites placeholder sin salir de Studio.
