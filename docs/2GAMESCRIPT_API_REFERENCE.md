# 2GameScript — Referencia API

Referencia técnica complementaria a `docs/SCRIPTING.md`. El tutorial integrado del Studio usa la misma organización conceptual: guías, eventos, flujo, comandos, asignaciones, rutas, componentes y compatibilidad legacy.

## Guías esenciales

### WASD libre

```text
on update
  self.vx = 0
  self.vy = 0

  ifKey A
    self.vx = -5
  end
  ifKey D
    self.vx = 5
  end
  ifKey W
    self.vy = -5
  end
  ifKey S
    self.vy = 5
  end
end
```

### Plataformas: A/D + gravedad + salto

2.1.x permite el impulso de salto, pero todavía no expone `grounded`, por lo que un script puede volver a saltar en el aire si se pulsa SPACE otra vez.

```text
on start
  Rigidbody2D.gravityScale = 1
end

on update
  self.vx = 0

  ifKey A
    self.vx = -5
  end
  ifKey D
    self.vx = 5
  end

  ifPressed SPACE
    self.vy = -8
  end
end
```

2.2.0 debe exponer como solo lectura:

```text
Rigidbody2D.grounded
Rigidbody2D.touchingLeft
Rigidbody2D.touchingRight
Rigidbody2D.touchingTop
```

Entonces el salto canónico será:

```text
ifPressed SPACE
  if Rigidbody2D.grounded == true
    self.vy = -8
  end
end
```

## Eventos

| Evento | Definición |
|---|---|
| `on start` | Inicio de la instancia/script. |
| `on update` | Ejecución continua por frame. |
| `on click` | Clic sobre entidad. |
| `on doubleClick` | Doble clic. |
| `on collision` | Inicio de colisión física; puede exponer `other`. |
| `on trigger` | Entrada en Trigger; puede exponer `other`. |
| `on destroy` | Antes de retirar una entidad destruida. |
| `on event nombre` | Señal nombrada recibida desde `emit`. |

## Operadores de asignación

```text
=
+=
-=
*=
/=
```

Ejemplos:

```text
Rigidbody2D.gravityScale = 1
Rigidbody2D.gravityScale *= -1
Health.current -= 10
global.score += 100
save.monedas += 1
```

## Operadores de comparación

```text
==
=
!=
>
>=
<
<=
contains
startsWith
endsWith
```

## Rutas

### `self.*`

```text
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
```

### `other.*`

Disponible principalmente en `collision` y `trigger`:

```text
other.Health.current -= 25
other.sprite = enemigo_hit
```

### Otra entidad

```text
enemigo.x = 12
enemigo.Rigidbody2D.gravityScale = 0
```

### `global.*`

Estado de sesión:

```text
global.score = 0
global.score += 10
```

### `save.*`

Persistencia SQLite:

```text
save.nombre = "Adarvio"
save.monedas += 1
```

### `scene.*`

```text
scene.name
scene.background
scene.backgroundMode
scene.boundary.left
scene.boundary.right
scene.boundary.top
scene.boundary.bottom
```

### `layer.*`

```text
layer.name
layer.enabled
layer.visible
layer.locked
layer.collision
layer.physicsLayer
layer.renderLayer
```

## Flujo

### `if`

```text
if Health.current <= 0
  destroy
else
  log "sigue vivo"
end
```

### `ifKey`

Verdadero mientras una tecla permanece pulsada.

```text
ifKey A
  self.vx = -5
end
```

### `ifPressed`

Verdadero solamente al comenzar la pulsación.

```text
ifPressed SPACE
  self.vy = -8
end
```

### `chance`

```text
chance 0.25
  create premio
end
```

### `repeat`

```text
repeat 3
  create chispa
end
```

Límite: 10.000.

### `stop` / `return`

Finalizan el evento actual.

## Temporización

| Comando | Definición |
|---|---|
| `wait s` | Pausa solo la secuencia actual. |
| `timer s comando` | Agenda una acción y continúa. |
| `every s n comando` | Ejecuta el comando `n` veces con intervalo. |

## Comandos

| Firma | Definición |
|---|---|
| `log texto` / `print texto` | Escribe en log. |
| `move dx dy` | Movimiento relativo de self. |
| `velocity vx vy` | Asigna velocidad completa. |
| `teleport x y` | Posición absoluta. |
| `bounce` | Invierte `vx` y `vy`. |
| `create plantilla [x y]` / `spawn ...` | Clona una entidad plantilla. |
| `destroy` | Destruye self. |
| `destroyEntity ref` | Destruye otra entidad. |
| `moveEntity ref dx dy` | Movimiento remoto relativo. |
| `teleportEntity ref x y` | Teletransporte remoto. |
| `setSprite asset` | Cambia sprite de self. |
| `setEntitySprite ref asset` | Cambia sprite remoto. |
| `damage ref cantidad` | Daño semántico a Health. |
| `heal ref cantidad` | Curación. |
| `loadScene id` | Carga escena. |
| `restartScene` | Reinicia escena actual. |
| `showMenu id` | Abre menú. |
| `emit nombre` | Publica señal. |
| `showText ...` | Texto FREE/BUBBLE/NOVEL. |

## Texto

### FREE

```text
showText free screen "NIVEL 1" 3
showText free self "-25" 1
```

### BUBBLE

```text
showText bubble self "¡Alto!" 4
```

Máximo efectivo: 10 segundos.

### NOVEL

```text
showText novel Diego "Texto" click
showText novel Diego "Texto" key SPACE
showText novel Narrador "Texto" 30
```

2.1.x fija visualmente colores/tamaños en runtime. 2.2.0 debe reemplazarlo por `TextStyle` editable.

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

Movimiento continuo preconstruido.

```text
enabled:boolean
speed:number
allowArrows:boolean
```

### `Rigidbody2D`

Física básica.

```text
enabled:boolean
mass:number
gravityScale:number
drag:number
maxSpeed:number
```

2.2.0 añade lecturas planificadas:

```text
grounded:boolean          # solo lectura
touchingLeft:boolean      # solo lectura
touchingRight:boolean     # solo lectura
touchingTop:boolean       # solo lectura
```

### `BoxCollider2D`

```text
enabled:boolean
width:number
height:number
solid:boolean
```

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

### `Animator` — planificado para 2.2.0

```text
enabled:boolean
controller:string
state:string
speed:number
flipX:boolean
flipY:boolean
frame:number
finished:boolean
```

## Legacy compatible

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

## Interpolación

```text
${variable}
${global:nombre}
${save:nombre}
${prop:x}
${path:Rigidbody2D.gravityScale}
${other}
```

## Limitaciones reales de 2.1.x

No existen todavía:

```text
for arbitrario
while
funciones de usuario
clases de usuario
imports
&&
||
SQL directo
Rigidbody2D.grounded
Animator
```

La documentación y el tutorial deben diferenciar siempre API existente de API planificada.
