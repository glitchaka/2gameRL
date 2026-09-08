# 2GameScript 2.1.0 — Biblia del lenguaje

Documento canónico del scripting de 2gameRL Studio. 2GameScript es un DSL interpretado por el motor; no es Lua, JavaScript, Python ni Java embebido.

## 1. Dónde puede existir un script

En 2.1.0 hay tres contextos:

- **Entidad**: lógica local del objeto.
- **Capa de tiles**: lógica asociada al suelo/capa seleccionada.
- **Escena**: lógica global de nivel, clima, oleadas, fondos, señales y estado.

Todo script se organiza en eventos:

```text
on start
  log "iniciado"
end

on update
  # cada frame
end
```

Comentarios: `# comentario`.

## 2. Eventos

Eventos normales:

```text
on start
on update
on click
on doubleClick
on collision
on trigger
on destroy
```

`click`, `doubleClick`, `collision`, `trigger` y `destroy` tienen sentido principalmente en entidades. `collision` y `trigger` pueden exponer `other`.

Eventos nombrados:

```text
on event alarma
  log "alarma recibida"
end
```

Se disparan con:

```text
emit alarma
```

Una señal se entrega a los scripts de escena, capas y entidades de la escena actual.

## 3. Sintaxis recomendada 2.1

La forma principal es asignación por rutas:

```text
Rigidbody2D.gravityScale = -1
Rigidbody2D.maxSpeed += 2
Health.current -= 10
self.x += 1
self.sprite = hero_idle
```

Operadores de asignación:

```text
=
+=
-=
*=
/=
```

Ejemplo para invertir gravedad:

```text
on click
  Rigidbody2D.gravityScale *= -1
end
```

La sintaxis legacy 2.0.x (`setComponent`, `setEntity`, etc.) sigue aceptada para compatibilidad.

## 4. Rutas

### Entidad actual

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

En el script de una entidad también puede omitirse `self` para componentes:

```text
Rigidbody2D.gravityScale = 1
BoxCollider2D.solid = true
GridMovement.step = 2
```

### Otra entidad

```text
other.Health.current -= 25
enemigo.x = 12
enemigo.Rigidbody2D.gravityScale = 0
```

Las referencias pueden resolverse por ID o por nombre.

### Globales de sesión

```text
global.score = 0
global.score += 100
```

Duran mientras el juego siga abierto.

### Persistencia SQLite

```text
save.nombre = "Adarvio"
save.monedas = 10
save.monedas += 1
save.jefeMuerto = true
```

`save.*` persiste entre ejecuciones mediante SQLite. El juego guarda sus valores en una base propia dentro de la carpeta local de datos del usuario. El creador del juego no escribe SQL.

Interpolación:

```text
log "Jugador: ${save:nombre}"
log "Puntos: ${global:score}"
```

### Escena

```text
scene.name
scene.background
scene.backgroundMode
scene.boundary.left
scene.boundary.right
scene.boundary.top
scene.boundary.bottom
```

Ejemplo:

```text
on event anochecer
  scene.background = bosque_noche
  scene.backgroundMode = cover
end
```

Modos de fondo:

```text
color
stretch
cover
contain
tile
```

### Capa

En un script perteneciente a una capa:

```text
layer.name
layer.enabled
layer.visible
layer.locked
layer.collision
layer.physicsLayer
layer.renderLayer
```

Ejemplo:

```text
on event descongelar
  layer.enabled = false
  layer.collision = false
end
```

## 5. Condiciones

Forma recomendada:

```text
if save.llaveRoja == true
  BoxCollider2D.enabled = false
else
  showText bubble self "Está cerrada" 3
end
```

Operadores:

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

También siguen disponibles:

```text
ifVar
ifGlobal
ifProperty
ifEntity
ifComponent
ifOther
chance
ifKey
ifPressed
```

Ejemplo:

```text
chance 0.25
  create premio
end
```

## 6. Movimiento

Movimiento directo:

```text
move 1 0
velocity 3 -1
teleport 8 4
bounce
```

Otra entidad:

```text
moveEntity enemigo 1 0
teleportEntity enemigo 8 4
```

### GridMovement

Movimiento discreto por losetas:

```text
GridMovement.enabled = true
GridMovement.step = 1
GridMovement.repeatDelay = 0.16
GridMovement.moveDuration = 0.10
GridMovement.allowDiagonal = false
GridMovement.allowArrows = true
GridMovement.snap = true
```

`GridMovement` respeta `BoxCollider2D`, tiles sólidos y capas físicas.

### PlayerController

Movimiento libre:

```text
PlayerController.enabled = true
PlayerController.speed = 4
PlayerController.allowArrows = true
```

## 7. Física y componentes

### Rigidbody2D

```text
Rigidbody2D.enabled = true
Rigidbody2D.mass = 1
Rigidbody2D.gravityScale = 1
Rigidbody2D.drag = 0.4
Rigidbody2D.maxSpeed = 12
```

### BoxCollider2D

```text
BoxCollider2D.enabled = true
BoxCollider2D.width = 0.82
BoxCollider2D.height = 0.82
BoxCollider2D.solid = true
```

`BoxCollider2D` bloquea por sí mismo; `Rigidbody2D` no es requisito.

### Patrol

```text
Patrol.enabled = true
Patrol.axis = x
Patrol.distance = 4
Patrol.speed = 1.5
```

### ScenePortal

```text
ScenePortal.enabled = true
ScenePortal.targetScene = bosque
ScenePortal.targetX = 2
ScenePortal.targetY = 2
```

### Trigger

```text
Trigger.enabled = true
Trigger.once = false
```

### Health

```text
Health.enabled = true
Health.max = 100
Health.current = 100
```

### DamageOnContact

```text
DamageOnContact.enabled = true
DamageOnContact.damage = 10
```

### Clickable

```text
Clickable.enabled = true
```

## 8. Añadir y quitar componentes

Compatibilidad legacy:

```text
addComponent self Rigidbody2D
removeComponent self Patrol
setComponent self Rigidbody2D gravityScale -1
enableComponent self BoxCollider2D false
```

Componentes integrados:

```text
GridMovement
PlayerController
Rigidbody2D
BoxCollider2D
Patrol
ScenePortal
Trigger
Health
DamageOnContact
Clickable
```

## 9. Variables legacy

Variables locales:

```text
setVar score 0
addVar score 5
mulVar score 2
divVar score 4
randomVar suerte 1 100
```

Globales legacy:

```text
setGlobal monedas 3
addGlobal monedas 1
```

## 10. Input

```text
on update
  ifKey W move 0 -0.05
  ifPressed SPACE log "acción"
end
```

Bloques:

```text
ifKey W
  move 0 -0.05
end
```

## 11. Temporización

### wait

Pausa solo la secuencia actual:

```text
on click
  destroy
  wait 9
  create pj
end
```

No congela física, render, input ni otros scripts.

### timer

Agenda un comando y continúa:

```text
timer 2 create explosion
```

### every

```text
every 1 5 create humo
```

Ejecuta `create humo` cinco veces, separadas por un segundo.

### repeat

```text
repeat 3
  create chispa
end
```

## 12. Crear/destruir entidades

```text
create enemigo
spawn enemigo
create enemigo 12 8
destroy
destroyEntity other
```

`create` clona una entidad plantilla de la escena y genera un ID runtime único.

## 13. Daño y curación

```text
damage other 25
heal self 10
```

También puede editarse `Health.current` directamente, pero `damage`/`heal` expresan mejor la intención del juego.

## 14. Sprites

```text
self.sprite = hero_idle
other.sprite = enemigo_golpeado
```

Legacy:

```text
setSprite hero_idle
setEntitySprite enemigo enemigo_golpeado
```

Los nombres pueden apuntar a imágenes completas o regiones virtuales de una spritesheet. La hoja no necesita recortarse físicamente.

## 15. Texto en pantalla

Hay tres soportes.

### FREE

Texto suelto, útil para títulos, avisos y números flotantes. Siempre se desvanece.

```text
showText free screen "NIVEL 1" 3
showText free self "-25" 1.5
showText free other "+10" 1
```

`screen` lo ancla a pantalla. `self`, `other`, ID o nombre lo hacen seguir a una entidad.

### BUBBLE

Bocadillo de cómic. Sigue a una entidad y siempre se desvanece. La duración efectiva nunca supera 10 segundos.

```text
showText bubble self "¿Qué fue eso?" 4
showText bubble other "¡Eh!" 2
```

### NOVEL

Banda tipo novela visual. El primer argumento después del tipo es el hablante.

Cerrar al hacer clic:

```text
showText novel Diego "No deberíamos estar aquí." click
```

Cerrar con una tecla:

```text
showText novel Alessandra "Entonces no mires atrás." key SPACE
```

Cerrar por tiempo:

```text
showText novel Narrador "Han pasado muchos años." 30
```

NOVEL puede permanecer más de 10 segundos o hasta interacción.

## 16. Señales y lógica global

Entidad:

```text
on trigger
  save.llaveRoja = true
  emit llaveRojaObtenida
  destroy
end
```

Escena:

```text
on event llaveRojaObtenida
  showText free screen "LLAVE OBTENIDA" 3
end
```

Puerta:

```text
on event llaveRojaObtenida
  BoxCollider2D.enabled = false
  self.sprite = puerta_abierta
end
```

## 17. Escenas y menús

```text
loadScene bosque
restartScene
showMenu pausa
```

Los IDs siguen siendo la referencia runtime de estos comandos. El editor visual muestra nombres y guarda el ID internamente para acciones de menú.

## 18. Interpolación

```text
${variable}
${global:monedas}
${save:nombre}
${prop:x}
${path:Rigidbody2D.gravityScale}
${other}
```

Ejemplo:

```text
log "X=${prop:x}, gravedad=${path:Rigidbody2D.gravityScale}"
```

## 19. Comandos legacy completos

```text
log
print
move
velocity
teleport
bounce
destroy
destroyEntity
wait
timer
every
create
spawn
loadScene
showMenu
restartScene
setSprite
setEntitySprite
setVar
addVar
mulVar
divVar
randomVar
setGlobal
addGlobal
set
setEntity
moveEntity
teleportEntity
setEntityVar
addEntityVar
addComponent
removeComponent
setComponent
enableComponent
damage
heal
ifKey
ifPressed
ifVar
ifGlobal
ifProperty
ifEntity
ifComponent
ifOther
chance
repeat
stop
return
emit
showText
```

## 20. Ejemplo completo

```text
on start
  addComponent self Rigidbody2D
  Rigidbody2D.gravityScale = 1
  save.intentos += 1
  showText free screen "INTENTO ${save:intentos}" 2
end

on click
  Rigidbody2D.gravityScale *= -1
  showText bubble self "¡Cambio!" 2
end

on collision
  other.Health.current -= 10
  emit choque
end

on event choque
  global.choques += 1
end
```

## 21. Límites actuales

2GameScript 2.1.0 no es un lenguaje de propósito general. No implementa clases, imports, funciones de usuario, `while`, `for` arbitrario, expresiones booleanas complejas con `&&/||` ni acceso directo a SQL. Para lógica compleja se combinan eventos, señales, condiciones, `repeat`, variables y componentes.

La persistencia se expone exclusivamente mediante `save.*`; SQLite es un detalle interno del runtime.
