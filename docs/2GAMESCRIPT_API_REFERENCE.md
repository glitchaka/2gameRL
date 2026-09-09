# 2GameScript 2.2 — Referencia API

Esta referencia describe la API disponible en 2gameRL Studio 2.2.0. El tutorial integrado del Studio usa los mismos nombres y ejemplos.

## Movimiento recomendado

### Plataformas: Input Map + componente

La forma más simple es añadir a la entidad:

- `PlatformerController`
- `Rigidbody2D`
- `BoxCollider2D`

`PlatformerController` usa por defecto las acciones `MoveLeft`, `MoveRight` y `Jump` del Input Map. `Rigidbody2D.grounded` se calcula por el runtime y es de solo lectura.

### Plataformas por script

```text
on update
  self.vx = 0

  ifAction MoveLeft
    self.vx = -5
  end

  ifAction MoveRight
    self.vx = 5
  end

  ifActionPressed Jump
    if Rigidbody2D.grounded == true
      self.vy = -8
    end
  end
end
```

### Movimiento libre 4 direcciones

Puedes usar `PlayerController`, o hacerlo explícitamente:

```text
on update
  self.vx = 0
  self.vy = 0

  ifAction MoveLeft
    self.vx = -5
  end
  ifAction MoveRight
    self.vx = 5
  end
  ifAction MoveUp
    self.vy = -5
  end
  ifAction MoveDown
    self.vy = 5
  end
end
```

Las acciones se configuran en **Configuración → Input Map**. Las predeterminadas son `MoveLeft`, `MoveRight`, `MoveUp`, `MoveDown`, `Jump`, `Accept` y `Cancel`.

## Eventos

| Evento | Definición |
|---|---|
| `on start` | Se ejecuta al iniciar la instancia/script. |
| `on update` | Se ejecuta en cada actualización del runtime. |
| `on click` | Clic sobre la entidad. |
| `on doubleClick` | Doble clic sobre la entidad. |
| `on collision` | Entrada en una colisión física. `other` puede identificar la otra entidad. |
| `on trigger` | Entrada en un Trigger. |
| `on destroy` | Se ejecuta antes de retirar la entidad. |
| `on event nombre` | Recibe una señal enviada con `emit`. |
| `on animationStart` | Comienza un AnimationClip. |
| `on animationEnd` | Termina un clip no cíclico. |
| `on animationLoop` | Un clip completa un ciclo. |
| `on animationEvent nombre` | Recibe un marcador de frame de un AnimationClip. |

## Input

```text
ifKey A
  ...
end

ifPressed SPACE
  ...
end

ifAction MoveLeft
  ...
end

ifActionPressed Jump
  ...
end
```

- `ifKey`: verdadero mientras la tecla está pulsada.
- `ifPressed`: solo durante el inicio de la pulsación.
- `ifAction`: lo mismo usando una acción configurable.
- `ifActionPressed`: inicio de una acción configurable.

Para gameplay nuevo se recomienda `ifAction`/`ifActionPressed` para no acoplar scripts a un teclado concreto.

## Asignaciones y rutas

```text
Rigidbody2D.gravityScale = 1
Rigidbody2D.gravityScale *= -1
Health.current -= 10
self.vx = 5
global.score += 100
save.monedas += 1
```

Operadores:

```text
=  +=  -=  *=  /=
```

Comparaciones:

```text
==  !=  >  >=  <  <=  contains  startsWith  endsWith
```

### `self.*`

```text
self.id
self.name
self.x
self.y
self.width
self.height
self.enabled
self.layer
self.group
self.renderLayer
self.physicsLayer
self.sprite
self.vx
self.vy
self.prefab
```

### Componentes

Cuando una entidad posee un componente, sus propiedades se acceden con `Componente.propiedad`:

```text
Rigidbody2D.gravityScale
Rigidbody2D.grounded
Health.current
Animator.state
Animator.frame
```

También puede usarse una referencia explícita:

```text
enemigo.Health.current -= 25
other.Health.current -= 10
```

### Variables de sesión y persistentes

```text
global.score = 0
save.monedas += 1
```

`global.*` vive durante la sesión actual. `save.*` usa la persistencia SQLite del juego.

### Escena

```text
scene.name
scene.background
scene.backgroundMode
scene.boundary.left
scene.boundary.right
scene.boundary.top
scene.boundary.bottom
scene.camera.target
scene.camera.zoom
```

### Capa

```text
layer.name
layer.enabled
layer.visible
layer.locked
layer.collision
layer.physicsLayer
layer.renderLayer
layer.opacity
layer.parallaxX
layer.parallaxY
layer.ySort
```

## Flujo

```text
if Health.current <= 0
  destroy
else
  log "sigue vivo"
end
```

Otras condiciones:

```text
ifVar
ifGlobal
ifProperty
ifEntity
ifComponent
ifOther
chance
```

Iteración:

```text
repeat 3
  create chispa
end
```

`repeat` tiene un límite de 10.000 iteraciones por bloque. `stop` y `return` terminan el evento actual.

## Tiempo

```text
wait 0.5
timer 1 create enemigo
every 0.25 4 create chispa
```

- `wait`: pausa únicamente la secuencia actual; no congela el juego.
- `timer`: agenda un comando y continúa.
- `every`: repite un comando un número determinado de veces.

## Entidades y Prefabs

```text
create enemigo
create enemigo 10 6
spawnPrefab slime
spawnPrefab slime 12 5
destroy
destroyEntity enemigo
moveEntity enemigo 1 0
teleportEntity enemigo 4 8
```

`create` conserva compatibilidad con entidades plantilla y también puede resolver Prefabs. `spawnPrefab` es la forma explícita para instanciar un Prefab.

## Animación

```text
playAnimation Walk
queueAnimation Attack
stopAnimation
```

Con componente `Animator`:

```text
Animator.controller = hero-controller
Animator.state = Walk
Animator.speed = 1.25
Animator.flipX = true
```

Propiedades principales:

```text
Animator.enabled
Animator.controller
Animator.clip
Animator.state
Animator.speed
Animator.flipX
Animator.flipY
Animator.frame
Animator.finished
```

Los frames de `AnimationClip` pueden llevar marcadores. Ejemplo de receptor:

```text
on animationEvent attack_hit
  damage other 20
end
```

## Texto

Los tres modos consumen `TextStyle` editable; el renderer no impone colores fijos.

### FREE

```text
showText free screen "NIVEL 1" 3
showText free screen "PELIGRO" 2 style warning
showText free screen "CRÍTICO" 1 color #FF2030
```

### BUBBLE

```text
showText bubble self "¡Alto!" 4 style guardia
```

### NOVEL

```text
showText novel Diego "No deberíamos estar aquí." click style dialogo
showText novel Diego "Corre." key SPACE style dialogo
showText novel Narrador "Tres horas después..." 5 style narrador
```

Un `TextStyle` puede configurar fuente, tamaños, color del texto y del hablante, fondo, borde, sombra, padding, radio, fade y dimensiones del panel NOVEL.

## Componentes / clases integradas

### `GridMovement`
Movimiento discreto por grilla.

```text
enabled:boolean
step:number
repeatDelay:number
moveDuration:number
allowDiagonal:boolean
allowArrows:boolean
snap:boolean
```

### `PlayerController`
Movimiento continuo 4 direcciones usando Input Map.

```text
enabled:boolean
speed:number
allowArrows:boolean
```

### `PlatformerController`
Control horizontal + salto. Requiere normalmente `Rigidbody2D` y collider.

```text
enabled:boolean
speed:number
jumpSpeed:number
leftAction:string
rightAction:string
jumpAction:string
```

### `Rigidbody2D`

```text
enabled:boolean
mass:number
gravityScale:number
drag:number
maxSpeed:number
freezeX:boolean
freezeY:boolean
grounded:boolean       # solo lectura
touchingLeft:boolean   # solo lectura
touchingRight:boolean  # solo lectura
touchingTop:boolean    # solo lectura
```

### `BoxCollider2D`

```text
enabled:boolean
width:number
height:number
offsetX:number
offsetY:number
solid:boolean
```

### `CircleCollider2D`
Collider circular básico; el runtime actual usa su envolvente para la resolución de contactos.

```text
enabled:boolean
radius:number
offsetX:number
offsetY:number
solid:boolean
```

### `SpriteRenderer`

```text
enabled:boolean
opacity:number
flipX:boolean
flipY:boolean
tint:string
palette:string
```

### `Animator`
Controla clips o un AnimatorController.

```text
enabled:boolean
controller:string
clip:string
state:string
speed:number
flipX:boolean
flipY:boolean
frame:number
finished:boolean
```

### `Camera2D`

```text
enabled:boolean
target:string
follow:boolean
zoom:number
offsetX:number
offsetY:number
pixelSnap:boolean
priority:number
```

### `ParticleEmitter2D`

```text
enabled:boolean
preset:string
playing:boolean
```

El preset define sprite, emisión, vida, velocidad, dispersión, gravedad, escalas, opacidades y burst.

### `Patrol`

```text
enabled:boolean
axis:x|y
distance:number
speed:number
```

### `ScenePortal`

```text
enabled:boolean
targetScene:string
targetX:number
targetY:number
```

### `Trigger`

```text
enabled:boolean
once:boolean
```

### `Health`

```text
enabled:boolean
max:number
current:number
```

### `DamageOnContact`

```text
enabled:boolean
damage:number
```

### `Clickable`

```text
enabled:boolean
```

### `SortingGroup`

```text
enabled:boolean
order:number
ySort:boolean
```

## Tiles y superficies

Un Tilemap sigue almacenando miles de celdas de forma compacta. La definición del pincel puede aportar:

```text
solid
oneWay
friction
damage
animationClip
```

No es necesario convertir un suelo masivo en miles de entidades.

## Interpolación

```text
${variable}
${global:nombre}
${save:nombre}
${prop:x}
${path:Rigidbody2D.gravityScale}
${other}
```

## Compatibilidad legacy

Siguen aceptándose, entre otros:

```text
addComponent
removeComponent
setComponent
enableComponent
setVar
addVar
mulVar
divVar
randomVar
setGlobal
addGlobal
set
setEntity
setEntityVar
addEntityVar
ifVar
ifGlobal
ifProperty
ifEntity
ifComponent
ifOther
```

## Límites actuales del lenguaje

2GameScript 2.2 no pretende ser Java/Lua de propósito general. No incluye `while` arbitrario, funciones/clases definidas por usuario, imports ni SQL directo. El objetivo es ofrecer una DSL de gameplay legible, con rutas de componentes, eventos, señales, temporización, Input Map, Prefabs y animación sin exponer la implementación interna del motor.
