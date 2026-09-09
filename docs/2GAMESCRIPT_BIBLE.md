# Biblia de 2GameScript 2.2.1

Manual técnico canónico del lenguaje de scripting de 2gameRL Studio.

2GameScript es un DSL interpretado por el runtime de 2gameRL. Está diseñado para lógica de gameplay, escenas, capas, Prefabs, animaciones, UI y persistencia sin exponer Java, SQL ni la implementación interna del motor. No es Lua, JavaScript, Python ni Java embebido.

Este documento describe **lo que existe realmente en 2.2.1**. Cuando una capacidad no existe, se indica explícitamente. No se documentan APIs hipotéticas como si estuvieran implementadas.

---

# Parte I — Modelo mental del lenguaje

## 1. Qué problema resuelve 2GameScript

2GameScript sirve para expresar comportamiento del juego con frases cortas y rutas legibles:

```text
on update
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

La intención es que el creador piense en:

- eventos;
- entidades;
- componentes;
- propiedades;
- acciones de input;
- señales;
- tiempo;
- estado de sesión;
- estado persistente.

No en clases Java, hilos, punteros, SQL ni APIs gráficas.

## 2. Dónde puede existir un script

Hay tres contextos principales.

### 2.1 Script de entidad

Es el contexto más habitual. `self` es la entidad que posee el script.

```text
on start
  log "Jugador creado"
end
```

Puede acceder a:

- propiedades de `self`;
- componentes de `self`;
- otras entidades;
- `other` durante colisiones/triggers;
- variables locales de la entidad;
- variables `global.*`;
- variables `save.*`;
- escena y señales.

### 2.2 Script de escena

Pertenece al nivel completo. Es apropiado para:

- oleadas;
- clima;
- cambios de fondo;
- lógica de misión;
- señales globales del nivel;
- estado compartido de la escena.

```text
on event anochecer
  scene.background = bosque_noche
  scene.backgroundMode = cover
end
```

### 2.3 Script de capa

Pertenece a una capa de tilemap.

```text
on event congelar
  layer.collision = true
end
```

Sirve para comportamiento colectivo de una capa sin crear miles de entidades.

---

# Parte II — Sintaxis léxica

## 3. Líneas, espacios y bloques

2GameScript es un lenguaje orientado a líneas. La indentación mejora la lectura, pero el cierre semántico de bloques se hace con `end`.

```text
on start
  log "Hola"
end
```

Los bloques principales son:

- `on ... end`;
- condiciones `if... end`;
- `repeat ... end`.

`else` puede aparecer dentro de una condición.

```text
if Health.current <= 0
  destroy
else
  log "Sigo vivo"
end
```

## 4. Comentarios

Un comentario comienza con `#` fuera de una cadena:

```text
# Esto es un comentario
on start
  log "inicio" # comentario al final
end
```

### 4.1 Excepción: colores hexadecimales

Desde 2.2, los literales de color `#RRGGBB` y `#RRGGBBAA` no se interpretan como comentarios cuando aparecen como token completo.

```text
showText free screen "PELIGRO" 2 color #FF2030
SpriteRenderer.tint = #FFFFFFFF
```

## 5. Cadenas

Las cadenas con espacios deben escribirse entre comillas dobles:

```text
log "Puerta abierta"
showText bubble self "No puedes pasar" 3
```

El tokenizer elimina las comillas exteriores antes de entregar el valor al comando.

No existe actualmente un sistema general de escapes estilo Java. Mantén las cadenas simples.

## 6. Números

Los números se interpretan con formato decimal compatible con `Double.parseDouble`:

```text
1
-8
+1
0.5
12.75
```

Por tanto esto es válido:

```text
Rigidbody2D.gravityScale = +1
```

## 7. Booleanos

Se usan principalmente:

```text
true
false
```

Ejemplo:

```text
BoxCollider2D.enabled = false
```

---

# Parte III — Estructura de programa y eventos

## 8. Todo código ejecutable vive dentro de un evento

Un comando fuera de un bloque `on ... end` es error de compilación.

Incorrecto:

```text
log "hola"
```

Correcto:

```text
on start
  log "hola"
end
```

## 9. Eventos estándar

### `on start`

Se dispara al iniciar el script de la instancia/contexto.

```text
on start
  global.score = 0
end
```

### `on update`

Se ejecuta cada actualización del runtime.

```text
on update
  ifAction MoveLeft
    self.vx = -5
  end
end
```

Evita operaciones innecesariamente pesadas aquí: puede ejecutarse muchas veces por segundo.

### `on click`

Clic simple sobre una entidad interactuable.

```text
on click
  showText bubble self "Hola" 2
end
```

### `on doubleClick`

Doble clic.

### `on collision`

Entrada en una colisión física. `other` puede identificar la otra entidad.

```text
on collision
  damage other 10
end
```

### `on trigger`

Entrada en un `Trigger`.

```text
on trigger
  emit alarma
end
```

### `on destroy`

Se ejecuta cuando la entidad va a ser retirada.

```text
on destroy
  log "entidad eliminada"
end
```

### `on animationStart`

Comienza una animación.

### `on animationEnd`

Finaliza una animación no cíclica.

### `on animationLoop`

Un clip completa un ciclo.

## 10. Eventos nombrados

```text
on event puertaAbierta
  log "La puerta se abrió"
end
```

Se disparan con:

```text
emit puertaAbierta
```

Las señales se propagan por los scripts relevantes de la escena actual.

## 11. Eventos de frame de animación

Un `AnimationFrame` puede tener un marcador como `attack_hit`.

El receptor se escribe:

```text
on animationEvent attack_hit
  damage other 20
end
```

Internamente estos eventos se tratan como eventos nombrados de animación.

---

# Parte IV — Modelo de ejecución

## 12. Ejecución secuencial

Los comandos de un evento se procesan en orden.

```text
on start
  log "A"
  log "B"
  log "C"
end
```

## 13. `wait` no congela el juego

`wait` pausa **la secuencia actual**, no el runtime completo.

```text
on click
  log "antes"
  wait 1
  log "después"
end
```

Durante ese segundo continúan:

- render;
- física;
- input;
- otros scripts;
- otras secuencias temporizadas.

## 14. `timer`

Agenda un comando y continúa inmediatamente.

```text
timer 2 create explosion
```

No acepta como comando anidado `wait` ni `every`.

## 15. `every`

```text
every 0.25 4 create chispa
```

Ejecuta el comando cuatro veces separado por 0,25 s.

La cantidad máxima aceptada es 10.000.

## 16. Protección contra ejecución descontrolada

El runtime incluye un guard de instrucciones por evento para evitar bucles internos accidentales. Si se alcanza, se escribe un mensaje en el log.

`repeat` también está limitado a 10.000 iteraciones.

---

# Parte V — Valores, rutas y estado

## 17. El modelo de valores

2GameScript trabaja principalmente con valores representados como texto y convierte a número cuando una operación lo requiere.

Consecuencias prácticas:

- `+=` suma si ambos lados son numéricos;
- `+=` concatena si no son numéricos;
- `-=`, `*=`, `/=` requieren números;
- las comparaciones intentan ser numéricas primero;
- si no pueden ser numéricas, se comparan como texto sin distinguir mayúsculas/minúsculas.

## 18. Asignación por rutas

La sintaxis moderna principal es:

```text
ruta = valor
ruta += valor
ruta -= valor
ruta *= valor
ruta /= valor
```

Ejemplos:

```text
self.vx = 5
Health.current -= 10
Rigidbody2D.gravityScale *= -1
global.score += 100
save.monedas += 1
```

### División por cero

En una asignación `/= 0`, el runtime conserva el valor anterior en lugar de lanzar una excepción.

## 19. `self.*`

Propiedades principales de la entidad actual:

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

Ejemplo:

```text
self.x += 1
self.sprite = hero_walk_01
```

## 20. Componentes de `self`

Si una entidad tiene un componente, puede accederse como:

```text
Componente.propiedad
```

Ejemplos:

```text
Rigidbody2D.gravityScale = 1
Health.current -= 25
Animator.speed = 1.25
```

El componente debe existir. Una asignación de ruta no crea automáticamente un componente faltante.

## 21. Otras entidades

Puede usarse una referencia explícita:

```text
enemigo.x = 10
enemigo.Health.current -= 20
```

Las referencias runtime pueden resolverse por las reglas internas de entidades del motor, incluyendo identificadores y nombres cuando corresponda.

## 22. `other`

Durante colisiones y triggers:

```text
other.Health.current -= 10
```

También puede interpolarse:

```text
log "Colisión con ${other}"
```

## 23. Variables locales

Las variables locales dependen del contexto del script.

Sintaxis legacy:

```text
setVar score 0
addVar score 1
```

Interpolación:

```text
log "score=${score}"
```

## 24. `global.*`

Estado de sesión compartido:

```text
global.score = 0
global.score += 100
```

Se pierde al cerrar la sesión del juego.

## 25. `save.*`

Persistencia entre ejecuciones:

```text
save.monedas = 10
save.monedas += 1
save.jefeMuerto = true
```

El runtime usa SQLite internamente. El autor del juego **no escribe SQL**.

## 26. Interpolación

Formas disponibles:

```text
${variable}
${global:nombre}
${save:nombre}
${prop:x}
${path:Rigidbody2D.gravityScale}
${other}
```

Ejemplo:

```text
log "Vida=${path:Health.current}, monedas=${save:monedas}"
```

---

# Parte VI — Comparaciones y control de flujo

## 27. `if` moderno

```text
if <ruta> <operador> <valor>
  ...
end
```

Ejemplo:

```text
if Health.current <= 0
  destroy
end
```

## 28. Operadores de comparación

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

Las operaciones de texto no distinguen mayúsculas/minúsculas.

## 29. `else`

```text
if save.llave == true
  emit abrir
else
  showText bubble self "Está cerrada" 2
end
```

## 30. Condiciones especializadas

### `ifKey`

```text
ifKey A
  self.vx = -5
end
```

Verdadero mientras la tecla permanece presionada.

### `ifPressed`

```text
ifPressed SPACE
  self.vy = -8
end
```

Verdadero al inicio de la pulsación.

### `ifAction`

```text
ifAction MoveLeft
  self.vx = -5
end
```

Usa el Input Map.

### `ifActionPressed`

```text
ifActionPressed Jump
  ...
end
```

### `ifVar`

Compara una variable local.

### `ifGlobal`

Compara una variable global legacy.

### `ifProperty`

Compara una propiedad expuesta por el contexto.

### `ifEntity`

```text
ifEntity enemigo exists
  log "sigue vivo"
end
```

Modos:

```text
exists
missing
```

### `ifComponent`

```text
ifComponent self Rigidbody2D exists
  log "tiene física"
end
```

Modos:

```text
exists
missing
enabled
disabled
```

### `ifOther`

Verdadero si existe una entidad `other` válida.

### `chance`

```text
chance 0.25
  create premio
end
```

Acepta una probabilidad entre 0 y 1.

## 31. Condicional inline de input

También existe:

```text
ifKey W move 0 -0.05
ifPressed SPACE log "salto"
ifAction MoveLeft move -0.05 0
ifActionPressed Jump log "jump"
```

Para lógica de varias líneas, usa bloques.

## 32. `repeat`

```text
repeat 3
  create chispa
end
```

No es un `while`. Repite una cantidad fija de veces.

## 33. `stop` y `return`

Ambos terminan el evento actual:

```text
if Health.current <= 0
  stop
end
```

---

# Parte VII — Input Map

## 34. Por qué usar acciones

Para gameplay nuevo, prefiere acciones en vez de teclas físicas.

Incorrecto como diseño portable:

```text
ifKey A
```

Preferido:

```text
ifAction MoveLeft
```

Así el mismo script puede funcionar con:

- A/D;
- flechas;
- otras configuraciones;
- futuras fuentes de input compatibles con el mapa.

## 35. Acciones predeterminadas

```text
MoveLeft
MoveRight
MoveUp
MoveDown
Jump
Accept
Cancel
```

Se configuran en **Configuración → Input Map**.

## 36. WASD libre

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

## 37. Plataforma con salto

Entidad recomendada:

- `PlatformerController`;
- `Rigidbody2D`;
- `BoxCollider2D`.

Por script:

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

`Rigidbody2D.grounded` evita el salto aéreo repetido.

---

# Parte VIII — Entidades y Prefabs

## 38. Crear una entidad

```text
create enemigo
create enemigo 12 8
spawn enemigo
```

`create` y `spawn` son formas compatibles del mismo concepto de creación por plantilla/resolución del runtime.

## 39. Instanciar un Prefab explícitamente

```text
spawnPrefab slime
spawnPrefab slime 12 5
```

## 40. Destruir

Entidad actual:

```text
destroy
```

Otra entidad:

```text
destroyEntity enemigo
```

## 41. Mover otra entidad

```text
moveEntity enemigo 1 0
teleportEntity enemigo 8 4
```

## 42. Sprite de otra entidad

```text
setEntitySprite enemigo enemigo_golpeado
```

La forma moderna equivalente puede ser:

```text
enemigo.sprite = enemigo_golpeado
```

---

# Parte IX — Movimiento y física

## 43. Movimiento inmediato

```text
move 1 0
```

Aplica un desplazamiento.

## 44. Velocidad

```text
velocity 3 -1
```

## 45. Teletransporte

```text
teleport 8 4
```

## 46. Rebote

```text
bounce
```

Invierte/resuelve la velocidad según la semántica del runtime para rebote.

## 47. Sistema de coordenadas

En el render 2D, Y positiva apunta hacia abajo. En un plataformas:

- gravedad positiva aumenta `vy` hacia abajo;
- un salto normal usa `self.vy` negativo.

Ejemplo:

```text
self.vy = -8
```

---

# Parte X — Referencia de componentes

## 48. `SpriteRenderer`

Propiedades:

```text
enabled:boolean
opacity:number
flipX:boolean
flipY:boolean
tint:string
palette:string
```

Ejemplo:

```text
SpriteRenderer.opacity = 0.5
SpriteRenderer.flipX = true
SpriteRenderer.tint = #FFFFFFFF
```

## 49. `Animator`

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

Comandos relacionados:

```text
playAnimation Walk
queueAnimation Attack
stopAnimation
```

## 50. `Camera2D`

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

## 51. `ParticleEmitter2D`

```text
enabled:boolean
preset:string
playing:boolean
```

El `ParticlePreset` define los parámetros visuales del emisor.

## 52. `GridMovement`

```text
enabled:boolean
step:number
repeatDelay:number
moveDuration:number
allowDiagonal:boolean
allowArrows:boolean
snap:boolean
```

Movimiento discreto por losetas.

## 53. `PlayerController`

```text
enabled:boolean
speed:number
allowArrows:boolean
```

Movimiento continuo de cuatro direcciones.

## 54. `PlatformerController`

```text
enabled:boolean
speed:number
jumpSpeed:number
leftAction:string
rightAction:string
jumpAction:string
```

Usa Input Map y `Rigidbody2D.grounded`.

## 55. `Rigidbody2D`

```text
enabled:boolean
mass:number
gravityScale:number
drag:number
maxSpeed:number
freezeX:boolean
freezeY:boolean
grounded:boolean
touchingLeft:boolean
touchingRight:boolean
touchingTop:boolean
```

Sensores de contacto calculados por runtime:

```text
grounded
touchingLeft
touchingRight
touchingTop
```

No deben usarse como estado editable de gameplay.

## 56. `BoxCollider2D`

```text
enabled:boolean
width:number
height:number
offsetX:number
offsetY:number
solid:boolean
```

No necesita `Rigidbody2D` para existir como obstáculo estático.

## 57. `CircleCollider2D`

```text
enabled:boolean
radius:number
offsetX:number
offsetY:number
solid:boolean
```

La implementación actual utiliza una aproximación robusta de contacto compatible con el runtime existente.

## 58. `Patrol`

```text
enabled:boolean
axis:x|y
distance:number
speed:number
```

## 59. `ScenePortal`

```text
enabled:boolean
targetScene:string
targetX:number
targetY:number
transition:string
```

## 60. `Trigger`

```text
enabled:boolean
once:boolean
```

## 61. `Health`

```text
enabled:boolean
max:number
current:number
```

## 62. `DamageOnContact`

```text
enabled:boolean
damage:number
```

## 63. `Clickable`

```text
enabled:boolean
```

## 64. `SortingGroup`

```text
enabled:boolean
order:number
ySort:boolean
```

---

# Parte XI — Gestión dinámica de componentes

## 65. Añadir componente

```text
addComponent self Rigidbody2D
```

## 66. Quitar componente

```text
removeComponent self Patrol
```

## 67. Configurar componente — sintaxis legacy

```text
setComponent self Rigidbody2D gravityScale -1
```

## 68. Habilitar/deshabilitar

```text
enableComponent self BoxCollider2D false
```

Para código nuevo, cuando el componente ya existe, suele ser más legible:

```text
BoxCollider2D.enabled = false
```

---

# Parte XII — Animación

## 69. `AnimationClip`

Un clip contiene frames ordenados. Cada frame puede tener:

- sprite/región;
- duración;
- marcador de evento.

El clip puede configurar:

- loop;
- ping-pong;
- inicio aleatorio;
- multiplicador de velocidad;
- categoría.

## 70. Reproducir

```text
playAnimation Walk
```

## 71. Encolar

```text
queueAnimation Attack
```

## 72. Detener

```text
stopAnimation
```

## 73. Animator Controller

Se controla mediante propiedades:

```text
Animator.controller = hero-controller
Animator.state = Walk
Animator.speed = 1.25
```

Los estados y transiciones se configuran en el editor visual.

## 74. Marcadores de frame

Ejemplo de clip de ataque:

```text
frame 0
frame 1
frame 2 -> attack_hit
frame 3
```

Receptor:

```text
on animationEvent attack_hit
  damage other 20
end
```

---

# Parte XIII — Texto y UI narrativa

## 75. `showText`

Modos:

```text
free
bubble
novel
```

Los estilos provienen de `TextStyle`; el renderer no fija colores obligatorios.

## 76. FREE

```text
showText free screen "NIVEL 1" 3
showText free self "-25" 1
showText free screen "PELIGRO" 2 style warning
showText free screen "CRÍTICO" 1 color #FF2030
```

Puede anclarse a `screen`, `self`, `other` o una referencia válida de entidad.

## 77. BUBBLE

```text
showText bubble self "¡Alto!" 4 style guardia
```

## 78. NOVEL

Cerrar con clic:

```text
showText novel Diego "No deberíamos estar aquí." click style dialogo
```

Cerrar con tecla:

```text
showText novel Alessandra "Corre." key SPACE style dialogo
```

Cerrar por tiempo:

```text
showText novel Narrador "Tres horas después..." 5 style narrador
```

## 79. Override de color

```text
showText free screen "PELIGRO" 2 color #FF0000
```

El override puntual no reemplaza la utilidad de un `TextStyle` reutilizable.

---

# Parte XIV — Escena y capa

## 80. Rutas de escena

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

Modos de fondo:

```text
color
stretch
cover
contain
tile
```

## 81. Rutas de capa

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

## 82. Ejemplo de capa destructible por señal

```text
on event derrumbe
  layer.collision = false
  layer.visible = false
end
```

---

# Parte XV — Tilemap y superficies

## 83. Tilemap no significa entidad por celda

Un suelo masivo se almacena como celdas compactas. No se crean miles de entidades.

Una definición de pincel puede aportar:

```text
solid
oneWay
friction
damage
animationClip
```

El comportamiento compartido de una superficie debe preferirse al estado individual cuando no se necesita identidad por celda.

---

# Parte XVI — Escenas, menús y navegación

## 84. Cambiar de escena

```text
loadScene bosque
```

## 85. Reiniciar escena

```text
restartScene
```

## 86. Mostrar menú

```text
showMenu pausa
```

---

# Parte XVII — Daño y curación

## 87. `damage`

```text
damage other 25
```

## 88. `heal`

```text
heal self 10
```

También puede modificarse `Health.current`, pero `damage` y `heal` expresan mejor la intención.

---

# Parte XVIII — Referencia alfabética de comandos

## 89. `addComponent`

```text
addComponent <entidad> <tipo>
```

## 90. `addEntityVar`

```text
addEntityVar <entidad> <nombre> <número>
```

## 91. `addGlobal`

```text
addGlobal <nombre> <número>
```

## 92. `addVar`

```text
addVar <nombre> <número>
```

## 93. `bounce`

```text
bounce
```

## 94. `create` / `spawn`

```text
create <entidad|prefab> [x y]
spawn <entidad|prefab> [x y]
```

## 95. `damage`

```text
damage <entidad> <cantidad>
```

La cantidad debe ser no negativa.

## 96. `destroy`

```text
destroy
```

## 97. `destroyEntity`

```text
destroyEntity <entidad>
```

## 98. `divVar`

```text
divVar <nombre> <número>
```

## 99. `emit`

```text
emit <evento>
```

## 100. `enableComponent`

```text
enableComponent <entidad> <tipo> <true|false>
```

## 101. `every`

```text
every <segundos> <cantidad> <comando>
```

## 102. `heal`

```text
heal <entidad> <cantidad>
```

## 103. `loadScene`

```text
loadScene <id>
```

## 104. `log` / `print`

```text
log <texto>
print <texto>
```

## 105. `move`

```text
move <x> <y>
```

## 106. `moveEntity`

```text
moveEntity <entidad> <x> <y>
```

## 107. `mulVar`

```text
mulVar <nombre> <número>
```

## 108. `playAnimation`

```text
playAnimation <clip|estado>
```

## 109. `queueAnimation`

```text
queueAnimation <clip|estado>
```

## 110. `randomVar`

```text
randomVar <nombre> <min> <max>
```

Requiere `max >= min`.

## 111. `removeComponent`

```text
removeComponent <entidad> <tipo>
```

## 112. `restartScene`

```text
restartScene
```

## 113. `set`

```text
set <propiedad> <valor>
```

Sintaxis legacy.

## 114. `setComponent`

```text
setComponent <entidad> <tipo> <propiedad> <valor>
```

## 115. `setEntity`

```text
setEntity <entidad> <propiedad> <valor>
```

## 116. `setEntitySprite`

```text
setEntitySprite <entidad> <asset>
```

## 117. `setEntityVar`

```text
setEntityVar <entidad> <nombre> <valor>
```

## 118. `setGlobal`

```text
setGlobal <nombre> <valor>
```

## 119. `setSprite`

```text
setSprite <asset>
```

## 120. `setVar`

```text
setVar <nombre> <valor>
```

## 121. `showMenu`

```text
showMenu <id>
```

## 122. `showText`

```text
showText <free|bubble|novel> ... [style nombre] [color #RRGGBB]
```

## 123. `spawnPrefab`

```text
spawnPrefab <prefab> [x y]
```

## 124. `stopAnimation`

```text
stopAnimation
```

## 125. `teleport`

```text
teleport <x> <y>
```

## 126. `teleportEntity`

```text
teleportEntity <entidad> <x> <y>
```

## 127. `timer`

```text
timer <segundos> <comando>
```

## 128. `velocity`

```text
velocity <x> <y>
```

## 129. `wait`

```text
wait <segundos>
```

---

# Parte XIX — Cookbook técnico

## 130. Personaje top-down

Componentes:

```text
PlayerController
BoxCollider2D
```

O script:

```text
on update
  self.vx = 0
  self.vy = 0
  ifAction MoveLeft
    self.vx = -4
  end
  ifAction MoveRight
    self.vx = 4
  end
  ifAction MoveUp
    self.vy = -4
  end
  ifAction MoveDown
    self.vy = 4
  end
end
```

## 131. Personaje de plataformas

Componentes:

```text
PlatformerController
Rigidbody2D
BoxCollider2D
```

Script alternativo:

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

## 132. Moneda persistente

```text
on trigger
  save.monedas += 1
  showText free self "+1" 1
  destroy
end
```

## 133. Llave y puerta

Llave:

```text
on trigger
  save.llaveRoja = true
  emit llaveRoja
  destroy
end
```

Puerta:

```text
on event llaveRoja
  BoxCollider2D.enabled = false
  self.sprite = puerta_abierta
end
```

## 134. Enemigo con vida

```text
on collision
  ifOther
    damage other 10
  end
end

on update
  if Health.current <= 0
    playAnimation Death
  end
end
```

## 135. Ataque sincronizado con animación

```text
on animationEvent attack_hit
  damage other 20
end
```

## 136. Plataforma que cambia de gravedad

```text
on click
  Rigidbody2D.gravityScale *= -1
end
```

## 137. Mensaje narrativo

```text
on start
  showText novel Narrador "La tormenta no había terminado." click style narrador
end
```

## 138. Señal de escena

Entidad:

```text
on trigger
  emit jefeDerrotado
end
```

Escena:

```text
on event jefeDerrotado
  showText free screen "ÁREA DESPEJADA" 3 style victoria
end
```

## 139. Temporizador de aparición

```text
on start
  every 2 5 create enemigo
end
```

## 140. Probabilidad de drop

```text
on destroy
  chance 0.2
    create pocion
  end
end
```

---

# Parte XX — Diagnóstico y errores

## 141. Error: comando fuera de evento

Síntoma:

```text
comando fuera de un bloque 'on ... end'
```

Solución: coloca el código dentro de un evento.

## 142. Error: falta `end`

Cada evento, condición y `repeat` debe cerrarse.

## 143. Error: comando desconocido

2GameScript no ejecuta comandos inventados silenciosamente. Revisa la ortografía y la referencia de este manual.

## 144. Error: componente no existe

Esto no crea el componente:

```text
Rigidbody2D.gravityScale = 1
```

si la entidad no tiene `Rigidbody2D`.

Agrégalo desde el editor o usa:

```text
addComponent self Rigidbody2D
```

## 145. Error de ruta

Comprueba:

1. referencia de entidad;
2. nombre exacto del componente;
3. propiedad existente;
4. contexto correcto.

## 146. División por cero

`/=` conserva el valor anterior si el divisor es 0. No uses esto como mecanismo normal de control; valida antes cuando sea posible.

## 147. Logs

Usa `log` para observar estado:

```text
log "vx=${prop:vx}"
log "vida=${path:Health.current}"
```

---

# Parte XXI — Rendimiento y diseño

## 148. No conviertas suelos masivos en entidades

Para suelo, paredes y decoración repetida usa tilemap/pinceles de tilemap.

Usa entidades cuando cada instancia necesita:

- estado individual;
- script individual;
- vida;
- Animator propio;
- movimiento;
- trigger individual;
- identidad runtime.

## 149. Mantén `on update` pequeño

Evita crear/destrozar grandes cantidades de objetos cada frame sin necesidad.

Prefiere eventos y señales cuando el cambio es discreto.

## 150. Prefiere Input Map

Reduce dependencia del teclado físico y mejora reutilización de scripts.

## 151. Prefiere rutas modernas

Mejor:

```text
Health.current -= 10
```

que:

```text
setComponent self Health current 90
```

La sintaxis legacy existe por compatibilidad, no porque sea la forma recomendada para código nuevo.

---

# Parte XXII — Compatibilidad legacy

## 152. Comandos conservados

Se mantienen, entre otros:

```text
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
addComponent
removeComponent
setComponent
enableComponent
ifVar
ifGlobal
ifProperty
ifEntity
ifComponent
ifOther
ifKey
ifPressed
```

La migración puede hacerse gradualmente.

---

# Parte XXIII — Gramática práctica

## 153. EBNF aproximada

Esta gramática es descriptiva; el parser real es la autoridad final.

```text
program        := { eventBlock }

eventBlock     := "on" eventSpec NEWLINE
                  { statement }
                  "end"

eventSpec      := "start"
                | "update"
                | "click"
                | "doubleClick"
                | "collision"
                | "trigger"
                | "destroy"
                | "animationStart"
                | "animationEnd"
                | "animationLoop"
                | "event" identifier
                | "animationEvent" identifier

statement      := assignment
                | command
                | conditional
                | repeatBlock
                | "stop"
                | "return"

assignment     := path assignOp value
assignOp       := "=" | "+=" | "-=" | "*=" | "/="

conditional    := condition NEWLINE
                  { statement }
                  [ "else" NEWLINE { statement } ]
                  "end"

repeatBlock    := "repeat" integer NEWLINE
                  { statement }
                  "end"
```

## 154. Lo que la gramática NO implica

2GameScript no es un lenguaje de expresiones algebraicas generales.

No asumas que esto funciona:

```text
self.vx = speed * 2 + sin(time)
```

La asignación acepta un valor/interpolación y los operadores compuestos actúan sobre el valor existente; no hay un parser matemático general de expresiones.

---

# Parte XXIV — Límites explícitos de 2.2.1

## 155. No existen todavía

No existen como características generales del lenguaje:

- funciones definidas por usuario;
- clases definidas por usuario;
- imports;
- módulos de código;
- `while` arbitrario;
- `for` arbitrario;
- expresiones booleanas generales con `&&` y `||`;
- parser matemático completo;
- acceso SQL directo;
- acceso a Java desde scripts;
- ejecución de código externo.

## 156. Filosofía

2GameScript prioriza:

- legibilidad;
- seguridad;
- integración con el editor;
- comportamiento de gameplay;
- acceso controlado al motor;
- proyectos fáciles de inspeccionar.

---

# Parte XXV — Tabla rápida

## 157. Eventos

```text
start
update
click
doubleClick
collision
trigger
destroy
event <nombre>
animationStart
animationEnd
animationLoop
animationEvent <nombre>
```

## 158. Asignación

```text
=  +=  -=  *=  /=
```

## 159. Comparación

```text
==  =  !=  >  >=  <  <=  contains  startsWith  endsWith
```

## 160. Tiempo

```text
wait
timer
every
repeat
```

## 161. Entidades

```text
create
spawn
spawnPrefab
destroy
destroyEntity
moveEntity
teleportEntity
```

## 162. Animación

```text
playAnimation
queueAnimation
stopAnimation
```

## 163. Navegación

```text
loadScene
restartScene
showMenu
```

## 164. Comunicación

```text
emit
on event
on animationEvent
```

## 165. Persistencia

```text
global.*
save.*
```

---

# Apéndice A — Checklist para un personaje de plataformas

1. Entidad con sprite.
2. `BoxCollider2D`.
3. `Rigidbody2D`.
4. `PlatformerController` o script equivalente.
5. `MoveLeft`, `MoveRight`, `Jump` en Input Map.
6. Suelo en capa con colisión activa.
7. Pincel de suelo marcado sólido.
8. Capas físicas compatibles.
9. Para salto manual, comprobar `Rigidbody2D.grounded`.

# Apéndice B — Checklist para un personaje top-down

1. Entidad con sprite.
2. Collider si necesita bloquearse.
3. `PlayerController` o script WASD.
4. Input Map configurado.
5. Y-sort si el juego necesita orden por profundidad vertical.

# Apéndice C — Convenciones recomendadas

## Nombres de acciones

```text
MoveLeft
MoveRight
MoveUp
MoveDown
Jump
Attack
Interact
Pause
```

## Nombres de señales

```text
puertaAbierta
jefeDerrotado
llaveObtenida
alarmaActivada
```

## Variables persistentes

```text
save.monedas
save.llaveRoja
save.jefe1Derrotado
```

## Variables de sesión

```text
global.score
global.enemigosVivos
```

# Apéndice D — Autoridad de esta documentación

Orden de autoridad cuando exista una discrepancia:

1. comportamiento validado del runtime 2.2.1;
2. parser `ScriptProgram` de 2.2.1;
3. esta Biblia;
4. referencia rápida/tutorial integrado;
5. documentación histórica de versiones anteriores.

La Biblia debe actualizarse junto con cualquier cambio de sintaxis, evento, componente o semántica pública del lenguaje.
