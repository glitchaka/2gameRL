# 2gameRL Studio 2.3.0-rc1

Primera Release Candidate construida sobre la 2.2.2 validada.

## Autoría

- Tileset Lab para autotile 4-way/8-way, reglas por máscara y variantes ponderadas.
- Herramienta Stamp y recursos `StampPattern` persistentes.
- Animation Lab con preview animado, FPS global, reverse, onion skin, markers y hitbox/hurtbox por frame.
- Animator Lab con parámetros BOOL/INT/FLOAT/TRIGGER, blend trees 1D/2D y Animation Sets de 2/4/8 direcciones.
- UI Skin Lab con normal/hover/pressed/disabled y asignación a botones/TextStyle.
- Asset Browser con favoritos y carpetas virtuales persistentes.
- Auditoría y reemplazo seguro de referencias de assets.
- Menú de botones actualizado para UISkin y estado habilitado/deshabilitado.

## Runtime

- Resolución determinista de autotile y variantes ponderadas.
- `AnimationClip.fps` y `reverse` efectivos.
- Hitbox/hurtbox por frame con señales `hitbox`, `hurtbox`, `animationHit` y DamageOnContact opcional.
- Parámetros de Animator accesibles como `Animator.param.<nombre>`.
- Triggers consumibles al producir una transición.
- Blend tree 1D/2D por clip más cercano.
- Animation Sets direccionales con fallback y flipX.
- UISkin aplicado a botones y TextStyle.
- Camera2D con bounds configurables y zoom suavizado.
- `while` acotado a 10.000 iteraciones.

## Compatibilidad

- Formato de proyecto 9.
- Los datos RC viven en un sidecar `rc-2.3.properties` dentro del `.2grl`.
- Los proyectos anteriores se cargan con defaults RC seguros.
- La 2.2.2 y su hotfix de partículas permanecen como base de regresión obligatoria.

## Gate de calidad

La RC no se publica hasta superar compilación/tests, pruebas aleatorias y de corrupción, smoke JavaFX RC, toda la batería 2.2/2.2.2, runtime/componentes/portal, empaquetado Windows, arranque del Studio, exportación con toolchain embebida y arranque del juego exportado.
