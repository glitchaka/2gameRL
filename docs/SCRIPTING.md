# 2GameScript — referencia del motor

2GameScript es el lenguaje de eventos de 2gameRL. Cada entidad de una escena puede tener su propio script.

## Abrir y validar un script

1. Abre **Escena**.
2. Selecciona una entidad en la jerarquía o directamente en el lienzo.
3. Abre la pestaña **Script** o haz doble clic sobre la entidad.
4. Escribe el script.
5. Pulsa **Validar script**.
6. Usa **Probar** para ejecutarlo sin exportar.

El mismo tutorial está integrado en Studio desde **Ayuda > Tutorial de scripting**, con **F1** y desde el botón **Tutorial** junto al editor. Los ejemplos del tutorial pueden insertarse directamente en el script seleccionado.

## Estructura

Todo comando debe estar dentro de un bloque `on ... end`:

```text
# comentario
on start
  log "Objeto iniciado"
end

on click
  log "clic"
end
```

## Eventos

- `start`: al cargar o crear la entidad.
- `update`: cada frame.
- `click`: clic sobre la entidad.
- `doubleClick`: doble clic.
- `collision`: contacto sólido de `BoxCollider2D`.
- `trigger`: entrada en un `Trigger` permitida por la matriz física.

## Movimiento

```text
move 1 0
velocity 3 -1
teleport 8 4
bounce
```

Las posiciones y movimientos usan unidades de tile, no píxeles de pantalla.

## Teclado

```text
ifKey W move 0 -0.05
ifPressed SPACE log "acción"
```

`ifKey` se repite mientras la tecla siga presionada. `ifPressed` se ejecuta una vez al comenzar la pulsación.

## Variables y propiedades

```text
setVar monedas 0
addVar monedas 1
set width 1.5
set enabled false
set renderLayer Personajes
set physicsLayer Player
```

Propiedades directas soportadas: `x`, `y`, `width`, `height`, `enabled`, `layer`, `renderLayer` y `physicsLayer`. Otros nombres usados con `set` se guardan como variables.

## Sprites y escenas

```text
setSprite hero.png
loadScene nivel-2
```

Usa la clave exacta del asset y el ID exacto de la escena.

## Destruir una entidad

```text
destroy
```

Elimina la instancia actual. Una secuencia pendiente por `wait` puede continuar después de que la instancia haya sido retirada del mundo.

## Crear entidades

```text
create enemigo
create enemigo 12 8
```

`create <entidad>` busca primero el ID de una entidad plantilla de la escena y después su nombre exacto. La nueva instancia copia sprite, componentes, script, tamaño, capas y variables iniciales.

Sin coordenadas, nace en la posición de la entidad que ejecutó el comando. Con `x y`, nace en esa posición. Cada instancia recibe un ID único de runtime y ejecuta su evento `start`.

## `wait`: continuar una secuencia más tarde

```text
on click
  destroy
  wait 9
  create pj
end
```

`wait 9` **no congela el juego**. Solo suspende el resto de ese bloque durante nueve segundos. Física, render, input y otros scripts continúan normalmente.

Los tiempos negativos son inválidos. `wait` debe ir en una línea propia.

## `timer`: programar una acción sin pausar

```text
on click
  timer 3 create explosion 8 6
  setSprite boton-presionado.png
  log "explosión programada"
end
```

`timer <segundos> <comando>` programa un comando y continúa inmediatamente con las líneas siguientes. `timer` no puede envolver `wait`.

Al cambiar de escena se cancelan los temporizadores pendientes de la escena anterior.

## Colisiones y capas

`BoxCollider2D` bloquea por sí solo cuando ambos colliders están activos y tienen `solid=true`; **Rigidbody2D no es requisito**.

Las capas visuales controlan delante/detrás. Las capas físicas solo filtran qué categorías pueden colisionar. Por defecto las capas creadas interactúan entre sí. La matriz física también filtra `Trigger`, `DamageOnContact` y `ScenePortal`.

Las capas de tiles pueden ser visibles/ocultas, bloqueadas/desbloqueadas y participar o no en colisión.

## Componentes integrados

- `Rigidbody2D`: `enabled`, `mass`, `gravityScale`, `drag`, `maxSpeed`.
- `BoxCollider2D`: `enabled`, `width`, `height`, `solid`.
- `PlayerController`: `enabled`, `speed`, `allowArrows`.
- `Patrol`: `enabled`, `axis`, `distance`, `speed`.
- `ScenePortal`: `enabled`, `targetScene`, `targetX`, `targetY`.
- `Trigger`: `enabled`, `once`.
- `Health`: `enabled`, `max`, `current`.
- `DamageOnContact`: `enabled`, `damage`.
- `Clickable`: `enabled`.

## Referencia de comandos

```text
log <texto>
move <x> <y>
velocity <x> <y>
teleport <x> <y>
bounce
destroy
wait <segundos>
timer <segundos> <comando>
create <entidad> [x y]
loadScene <id>
setSprite <asset>
setVar <nombre> <valor>
addVar <nombre> <número>
set <propiedad> <valor>
ifKey <tecla> <comando>
ifPressed <tecla> <comando>
```

## Ejemplo de respawn solicitado

```text
on click
  destroy
  wait 9
  create pj
end
```

La entidad actual desaparece, el juego continúa normalmente y nueve segundos después se crea una nueva instancia de `pj` en la posición original.
