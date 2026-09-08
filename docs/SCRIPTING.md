# 2GameScript — referencia completa

2GameScript es el lenguaje de eventos de 2gameRL. Cada entidad puede tener su propio script y controlar otras entidades, componentes, variables, escenas y temporizadores.

## Abrir y validar

1. Abre **Escena**.
2. Selecciona una entidad.
3. Abre **Script** o haz doble clic sobre la entidad.
4. Escribe el script.
5. Pulsa **Validar script**.
6. Usa **Probar**.

El tutorial también está integrado en **Ayuda > Tutorial de scripting**, **F1** y el botón **Tutorial** del editor.

## Eventos

```text
on start
end

on update
end

on click
end

on doubleClick
end

on collision
end

on trigger
end

on destroy
end
```

`collision` y `trigger` exponen la entidad contraria mediante la referencia especial `other`.

## Movimiento

Movimiento directo desde script:

```text
move 1 0
velocity 3 -1
teleport 8 4
bounce
```

Las coordenadas usan unidades de loseta.

### Movimiento configurable por losetas

Añade **GridMovement** desde el Inspector para que el objeto se mueva por pasos discretos. Sus propiedades se pueden editar visualmente o desde script:

```text
setComponent self GridMovement step 1
setComponent self GridMovement repeatDelay 0.16
setComponent self GridMovement moveDuration 0
setComponent self GridMovement allowDiagonal false
setComponent self GridMovement allowArrows true
setComponent self GridMovement snap true
```

- `step`: distancia de cada movimiento, en losetas. Puede ser `0.5`, `1`, `2`, etc.
- `repeatDelay`: tiempo entre pasos al mantener una tecla.
- `moveDuration`: transición visual entre una loseta y otra. `0` = instantáneo.
- `allowDiagonal`: permite movimientos diagonales.
- `allowArrows`: habilita flechas además de WASD.
- `snap`: mantiene la posición lógica alineada al tamaño de paso.

`GridMovement` respeta `BoxCollider2D`, tiles sólidos y capas físicas. `PlayerController` sigue disponible para movimiento libre continuo.

## Teclado

Forma corta:

```text
on update
  ifKey W move 0 -0.05
  ifPressed SPACE log "acción"
end
```

Forma de bloque:

```text
on update
  ifKey W
    move 0 -0.05
    setVar caminando true
  end
end
```

## Variables locales

```text
setVar score 0
addVar score 5
mulVar score 2
divVar score 4
randomVar suerte 1 100
```

Interpolación:

```text
log "Score: ${score}"
log "X: ${prop:x}"
log "Global: ${global:monedas}"
log "Otra entidad: ${other}"
```

## Variables globales

```text
setGlobal monedas 0
addGlobal monedas 1
```

Persisten mientras el juego siga abierto, incluso al cambiar de escena.

## Condiciones y else

```text
ifVar score >= 10
  log "ganaste"
else
  log "faltan puntos"
end
```

Operadores: `==`, `!=`, `>`, `>=`, `<`, `<=`, `contains`, `startsWith`, `endsWith`.

También:

```text
ifGlobal monedas >= 5
  log "puedes comprar"
end

ifProperty x > 10
  teleport 2 2
end

ifEntity enemigo exists
  log "sigue vivo"
end

ifComponent self Health enabled
  log "tengo vida"
end

ifOther
  log "evento con otra entidad"
end

chance 0.25
  create premio
end
```

## Bucles

```text
repeat 5
  create chispa
end
```

## Temporizadores

Pausar solo la secuencia actual:

```text
on click
  destroy
  wait 9
  create pj
end
```

Programar sin detener el bloque:

```text
timer 3 create explosion 8 6
```

Repetir una acción:

```text
every 1 5 create chispa
```

## Crear entidades

```text
create enemigo
spawn enemigo
create enemigo 12 8
```

Se puede usar el ID o el nombre exacto de una entidad plantilla de la escena. Cada instancia recibe un ID de runtime único y ejecuta `on start`.

## Referencias de entidad

Muchos comandos aceptan:

- `self`: entidad que ejecuta el script.
- `other`: entidad contraria en `collision`, `trigger` o `destroy`.
- ID exacto.
- nombre de entidad.

```text
destroyEntity other
moveEntity enemigo 1 0
teleportEntity enemigo 8 4
setEntity enemigo enabled false
setEntitySprite enemigo hero_idle_01
setEntityVar enemigo estado dormido
addEntityVar enemigo furia 1
```

## Componentes desde script

```text
addComponent self Health
removeComponent self Patrol
setComponent self Rigidbody2D gravityScale 0
enableComponent self BoxCollider2D false
```

También funciona sobre otras entidades:

```text
setComponent other Health current 25
setComponent enemigo GridMovement step 2
```

Tipos integrados:

- `GridMovement`
- `PlayerController`
- `Rigidbody2D`
- `BoxCollider2D`
- `Patrol`
- `ScenePortal`
- `Trigger`
- `Health`
- `DamageOnContact`
- `Clickable`

## Vida

```text
damage other 10
heal self 20
```

## Escenas y menús

```text
loadScene bosque
restartScene
showMenu pausa
```

## Sprites y spritesheets

```text
setSprite hero_idle_01
setEntitySprite enemigo slime_ataque_03
```

El nombre puede apuntar tanto a una imagen independiente como a una **región virtual de una spritesheet**. La hoja se almacena una sola vez; los sprites definidos dentro de ella son referencias `x/y/ancho/alto` y se renderizan con nearest-neighbor, sin blur.

## Propiedades

```text
set x 10
set y 4
set width 1
set height 1
set enabled true
set layer 3
set group Enemigos
set renderLayer Personajes
set physicsLayer Player
set sprite hero_idle_01
set vx 2
set vy 0
```

Sobre otra entidad:

```text
setEntity enemigo x 12
setEntity enemigo physicsLayer Enemy
```

## stop / return

```text
ifVar muerto == true
  stop
end
```

`return` es alias de `stop`.

## BoxCollider2D

Dos entidades activas con `BoxCollider2D`, `solid=true` y capas físicas compatibles se bloquean entre sí. `Rigidbody2D` no es requisito. Los tiles no transitables de una capa con colisión también bloquean.

## Referencia rápida

```text
log <texto>
print <texto>
move <x> <y>
velocity <x> <y>
teleport <x> <y>
bounce
destroy
destroyEntity <entidad>
wait <segundos>
timer <segundos> <comando>
every <segundos> <cantidad> <comando>
repeat <cantidad> ... end
create|spawn <entidad> [x y]
loadScene <escena>
restartScene
showMenu <menú>
setSprite <asset-o-region>
setEntitySprite <entidad> <asset-o-region>
setVar <nombre> <valor>
addVar <nombre> <número>
mulVar <nombre> <número>
divVar <nombre> <número>
randomVar <nombre> <min> <max>
setGlobal <nombre> <valor>
addGlobal <nombre> <número>
set <propiedad> <valor>
setEntity <entidad> <propiedad> <valor>
moveEntity <entidad> <x> <y>
teleportEntity <entidad> <x> <y>
setEntityVar <entidad> <nombre> <valor>
addEntityVar <entidad> <nombre> <número>
addComponent <entidad> <tipo>
removeComponent <entidad> <tipo>
setComponent <entidad> <tipo> <propiedad> <valor>
enableComponent <entidad> <tipo> <true|false>
damage <entidad> <cantidad>
heal <entidad> <cantidad>
ifKey <tecla> [comando]
ifPressed <tecla> [comando]
ifVar <nombre> <op> <valor> ... [else ...] end
ifGlobal <nombre> <op> <valor> ... [else ...] end
ifProperty <propiedad> <op> <valor> ... [else ...] end
ifEntity <entidad> <exists|missing> ... end
ifComponent <entidad> <tipo> [exists|missing|enabled|disabled] ... end
ifOther ... end
chance <0..1> ... end
stop
return
```
