# Biblia de 2GameScript 2.2.2

## Especificación técnica canónica de 2gameRL Studio

Esta Biblia describe el comportamiento público de 2GameScript y de los recursos que el lenguaje puede controlar en 2gameRL Studio 2.2.2. No es una colección de ideas futuras: salvo que una sección indique explícitamente una limitación, las firmas y propiedades aquí descritas corresponden al runtime 2.2.2.

La documentación usa `nombre_recurso` para indicar un ID creado por el usuario. Nunca debe interpretarse como un recurso incluido automáticamente. Por ejemplo, si un ejemplo dice `playSound golpe`, el proyecto debe contener un SoundAsset cuyo ID sea `golpe`.

---

# 1. Modelo de ejecución

2GameScript es un DSL orientado a eventos. Un script se compila antes de ejecutarse y contiene uno o más bloques `on ... end`. Los scripts pueden pertenecer a una entidad, a una capa TileLayer o a una escena Level. Cada ámbito expone rutas diferentes, pero comparte comandos, variables globales, persistencia y señales.

El runtime actualiza una escena aproximadamente una vez por frame. El orden relevante para una entidad es: comportamientos integrados, `on update`, integración física, Animator, ParticleEmitter2D y, al final del frame, actualización de la lista de partículas. Esto significa que una escritura hecha en `on update` sobre un componente afecta al mismo frame cuando el subsistema correspondiente se ejecuta después.

Ejemplo:

```2gs
on update
  ParticleEmitter2D.preset = test_particle
  ParticleEmitter2D.playing = true
end
```

`test_particle` debe existir en el proyecto. En 2.2.2 el Studio valida esta referencia y el inspector usa un selector de ParticlePreset; no es necesario adivinar IDs internos.

## 1.1 Ámbitos

- Entidad: puede usar `self`, `other`, componentes y variables locales de la entidad.
- Capa: puede usar `layer.*`, variables de capa, `scene.*`, entidades nombradas, globales y persistencia.
- Escena: puede usar `scene.*`, entidades nombradas, globales y persistencia.
- `global.*`: vive mientras dura la ejecución actual.
- `save.*`: persiste entre ejecuciones mediante el PersistentStore.

## 1.2 Identificadores

Los IDs de recursos son sensibles a su identidad canónica aunque la resolución de algunos recursos también acepta el nombre visible por compatibilidad. Para ParticlePreset 2.2.2 elimina la dualidad de edición: el campo Nombre/ID es la referencia canónica. Renombrar un ParticlePreset desde el Studio migra las referencias del proyecto.

## 1.3 Comentarios

Un comentario empieza con `#` y continúa hasta el final de línea. Los colores hexadecimales `#RRGGBB` y `#RRGGBBAA` no se interpretan como comentarios cuando aparecen como valores válidos.

```2gs
on start
  # comentario
  showText free screen "PELIGRO" 2 color #FF4040
end
```

---

# 2. Gramática práctica

```text
programa        := bloque_evento*
bloque_evento  := "on" evento NL instrucciones "end"
evento          := start | update | click | doubleClick | collision | trigger
                | destroy | animationStart | animationEnd | animationLoop
                | "event" IDENT | "animationEvent" IDENT
instrucciones   := instruccion*
instruccion     := comando
                | asignacion
                | condicional
                | repeat
asignacion      := ruta ("=" | "+=" | "-=" | "*=" | "/=") valor
condicional     := condicion instrucciones ("else" instrucciones)? "end"
repeat          := "repeat" ENTERO instrucciones "end"
ruta            := IDENT ("." IDENT)+
```

Los strings que contienen espacios se escriben entre comillas dobles. La comparación de strings de las condiciones es insensible a mayúsculas/minúsculas. Las asignaciones aritméticas requieren valores numéricos, salvo `+=`, que concatena cuando los operandos no son ambos numéricos.

## 2.1 Operadores de comparación

`==`, `=`, `!=`, `>`, `>=`, `<`, `<=`, `contains`, `startsWith`, `endsWith`.

```2gs
on update
  if Health.current <= 25
    showText bubble self "Estoy herido" 2
  end
end
```

## 2.2 Operadores de asignación

```2gs
self.x = 4
self.x += 1
self.x -= 1
Rigidbody2D.gravityScale *= -1
global.score += 10
save.monedas += 1
```

La división por cero de una asignación `/=` conserva el valor anterior en vez de producir infinito.

---

# 3. Eventos

## 3.1 `on start`

Se dispara cuando comienza el script de una instancia o cuando una entidad es creada durante runtime.

```2gs
on start
  Health.current = 100
  log "Entidad iniciada"
end
```

Uso: inicialización, temporizadores iniciales, estado inicial, música o señales de arranque.

## 3.2 `on update`

Se ejecuta en cada actualización mientras la entidad/escena/capa está activa.

```2gs
on update
  ifAction MoveRight
    self.vx = 5
  end
end
```

No se debe usar para crear recursos sin límite salvo que ese comportamiento sea intencional.

## 3.3 `on click`

Se dispara al hacer clic sobre una entidad clicable.

```2gs
on click
  showText bubble self "Hola" 2
end
```

`Clickable.enabled = false` deshabilita la interacción.

## 3.4 `on doubleClick`

```2gs
on doubleClick
  emit inspeccionProfunda
end
```

## 3.5 `on collision`

Se dispara al comenzar un contacto físico. `other` referencia la otra entidad cuando existe.

```2gs
on collision
  damage other 10
end
```

## 3.6 `on trigger`

Se dispara al entrar en un Trigger.

```2gs
on trigger
  emit zonaEntrenamiento
end
```

## 3.7 `on destroy`

Se ejecuta antes de retirar una entidad destruida.

```2gs
on destroy
  spawnPrefab explosion
end
```

## 3.8 `on animationStart`

Se dispara al comenzar un AnimationClip mediante Animator.

```2gs
on animationStart
  log "animación iniciada"
end
```

## 3.9 `on animationEnd`

Se dispara al terminar un clip no cíclico.

```2gs
on animationEnd
  emit ataqueTerminado
end
```

## 3.10 `on animationLoop`

Se dispara al completar un ciclo o un rebote de ping-pong.

```2gs
on animationLoop
  log "nuevo ciclo"
end
```

## 3.11 `on animationEvent <nombre>`

Escucha un marker de frame.

```2gs
on animationEvent footstep
  playSound paso 0.7
end
```

`paso` debe ser un SoundAsset existente.

## 3.12 `on event <nombre>`

Escucha una señal emitida con `emit`.

```2gs
on event puertaAbierta
  BoxCollider2D.enabled = false
end
```

---

# 4. Control de flujo y condiciones

## 4.1 `if <ruta> <operador> <valor>`

Firma:

```text
if <ruta> <operador> <valor>
  ...
else
  ...
end
```

Ejemplo:

```2gs
if Health.current <= 0
  destroy
else
  log "sigue vivo"
end
```

## 4.2 `ifKey <tecla>`

Verdadero mientras la tecla permanece pulsada.

```2gs
ifKey A
  self.vx = -5
end
```

## 4.3 `ifPressed <tecla>`

Verdadero en el frame de transición a pulsado.

```2gs
ifPressed SPACE
  emit saltoSolicitado
end
```

## 4.4 `ifAction <acción>`

Consulta el Input Map.

```2gs
ifAction MoveLeft
  self.vx = -5
end
```

## 4.5 `ifActionPressed <acción>`

```2gs
ifActionPressed Jump
  if Rigidbody2D.grounded == true
    self.vy = -8
  end
end
```

## 4.6 `ifVar <nombre> <op> <valor>`

```2gs
ifVar fase == 2
  emit jefe
end
```

## 4.7 `ifGlobal <nombre> <op> <valor>`

```2gs
ifGlobal score >= 100
  emit premio
end
```

## 4.8 `ifProperty <propiedad> <op> <valor>`

Compara una propiedad base del objeto actual.

```2gs
ifProperty enabled == true
  log "activo"
end
```

## 4.9 `ifEntity <entidad> <exists|missing>`

```2gs
ifEntity boss exists
  log "el jefe sigue en escena"
end
```

## 4.10 `ifComponent <entidad> <tipo> [modo]`

Modos: `exists`, `missing`, `enabled`, `disabled`.

```2gs
ifComponent player Health enabled
  heal player 10
end
```

## 4.11 `ifOther`

```2gs
on collision
  ifOther
    damage other 5
  end
end
```

## 4.12 `chance <0..1>`

```2gs
chance 0.25
  spawnPrefab moneda
end
```

## 4.13 `repeat <cantidad>`

Repite inmediatamente; no introduce tiempo entre iteraciones.

```2gs
repeat 3
  spawnPrefab chispa
end
```

## 4.14 `stop` / `return`

Detiene el evento actual.

```2gs
if Health.current <= 0
  stop
end
```

---

# 5. Tiempo

## 5.1 `wait <segundos>`

Pausa la continuación del evento sin congelar el juego.

```2gs
on start
  showText free screen "3" 1
  wait 1
  showText free screen "2" 1
end
```

## 5.2 `timer <segundos> <comando>`

Agenda un comando normal.

```2gs
timer 2 emit abrirPuerta
```

También acepta asignaciones:

```2gs
timer 0.2 ParticleEmitter2D.playing = true
```

## 5.3 `every <segundos> <cantidad> <comando>`

```2gs
every 1 5 spawnPrefab humo
```

`every` siempre requiere una cantidad finita en 2.2.2.

---

# 6. Variables

## 6.1 Locales

### `setVar <nombre> <valor>`

```2gs
setVar fase 1
```

### `addVar <nombre> <número>`

```2gs
addVar contador 1
```

### `mulVar <nombre> <número>`

```2gs
mulVar daño 1.5
```

### `divVar <nombre> <número>`

Dividir por cero conserva el valor.

### `randomVar <nombre> <min> <max>`

```2gs
randomVar tirada 1 6
```

## 6.2 Globales

### `setGlobal <nombre> <valor>`
### `addGlobal <nombre> <número>`

Equivalente por ruta:

```2gs
global.score = 0
global.score += 10
```

## 6.3 Persistencia

```2gs
save.nombre = "Adarvio"
save.monedas += 1
```

`save.*` es persistente. No requiere SQL por parte del creador.

## 6.4 Interpolación

Formas soportadas:

```text
${variable}
${global:nombre}
${save:nombre}
${prop:nombre}
${path:ruta}
${other}
```

Ejemplo:

```2gs
showText free screen "Puntos: ${global:score}" 2
```

---

# 7. Comandos de entidad y mundo

## 7.1 `log <texto>` / `print <texto>`

```2gs
log "checkpoint alcanzado"
```

## 7.2 `move <dx> <dy>`

Mueve la entidad actual respetando colisiones.

```2gs
move 1 0
```

## 7.3 `velocity <vx> <vy>`

```2gs
velocity 5 -3
```

## 7.4 `teleport <x> <y>`

Cambia posición sin recorrido intermedio.

```2gs
teleport 10 4
```

## 7.5 `bounce`

Invierte `vx` y `vy`.

## 7.6 `destroy`

Destruye `self`, ejecutando antes `on destroy`.

## 7.7 `destroyEntity <entidad>`

```2gs
destroyEntity puertaTemporal
```

## 7.8 `create <entidad|prefab> [x y]` / `spawn`

Busca primero Prefab y luego entidad de escena por ID/nombre.

```2gs
create enemigo 12 4
```

## 7.9 `spawnPrefab <prefab> [x y]`

Exige un Prefab.

```2gs
spawnPrefab slime 11 2
```

## 7.10 `moveEntity <entidad> <dx> <dy>`

```2gs
moveEntity plataforma 1 0
```

## 7.11 `teleportEntity <entidad> <x> <y>`

```2gs
teleportEntity jugador 4 8
```

## 7.12 `set <propiedad> <valor>`

Escribe una propiedad base de `self`.

```2gs
set name "Guardia herido"
```

## 7.13 `setEntity <entidad> <propiedad> <valor>`

```2gs
setEntity puerta enabled false
```

## 7.14 `setEntityVar` / `addEntityVar`

```2gs
setEntityVar enemigo fase 2
addEntityVar enemigo alerta 1
```

## 7.15 `setSprite <asset>`

```2gs
setSprite heroe_herido
```

El Asset debe existir.

## 7.16 `setEntitySprite <entidad> <asset>`

```2gs
setEntitySprite guardia guardia_alerta
```

## 7.17 `addComponent <entidad> <tipo>`

Añade únicamente componentes integrados reconocidos.

```2gs
addComponent enemigo Health
```

## 7.18 `removeComponent <entidad> <tipo>`

```2gs
removeComponent enemigo Patrol
```

## 7.19 `setComponent <entidad> <tipo> <propiedad> <valor>`

```2gs
setComponent player Rigidbody2D gravityScale 0.5
```

## 7.20 `enableComponent <entidad> <tipo> <true|false>`

```2gs
enableComponent player PlayerController false
```

## 7.21 `damage <entidad> <cantidad>`

Necesita Health en el objetivo.

```2gs
damage other 10
```

## 7.22 `heal <entidad> <cantidad>`

```2gs
heal player 25
```

---

# 8. Escenas y menús

## 8.1 `loadScene <id>`

```2gs
loadScene mazmorra_02
```

El validador semántico de 2.2.2 marca IDs inexistentes.

## 8.2 `restartScene`

Recarga la escena actual.

## 8.3 `showMenu <id>`

```2gs
showMenu pausa
```

## 8.4 Rutas `scene.*`

Lectura/escritura:

```text
scene.name:string
scene.background:string
scene.backgroundMode: COLOR|STRETCH|COVER|CONTAIN|TILE
scene.boundary.left:boolean
scene.boundary.right:boolean
scene.boundary.top:boolean
scene.boundary.bottom:boolean
scene.camera.target:string
scene.camera.zoom:number
```

Ejemplos:

```2gs
scene.background = bosque_noche
scene.backgroundMode = cover
scene.boundary.left = true
scene.camera.target = player
scene.camera.zoom = 1.5
```

Las variables no reservadas pueden vivir bajo `scene.<nombre>`.

## 8.5 Rutas `layer.*`

```text
layer.name:string
layer.enabled:boolean
layer.visible:boolean
layer.locked:boolean
layer.collision:boolean
layer.physicsLayer:string
layer.renderLayer:string
layer.opacity:number 0..1 recomendado
layer.parallaxX:number
layer.parallaxY:number
layer.ySort:boolean
```

Ejemplo:

```2gs
on event ocultarNiebla
  layer.visible = false
  layer.collision = false
end
```

---

# 9. Señales

## `emit <nombre>`

Distribuye un evento nombrado por la escena actual.

```2gs
# llave
on trigger
  emit llaveRojaObtenida
  destroy
end
```

```2gs
# puerta
on event llaveRojaObtenida
  BoxCollider2D.enabled = false
end
```

---

# 10. Animación

## 10.1 `playAnimation <clip|estado>`

Si Animator tiene controller y el nombre coincide con un estado, reproduce el clip de ese estado. Si no, intenta usarlo como AnimationClip.

```2gs
playAnimation attack
```

## 10.2 `queueAnimation <clip|estado>`

Agenda la animación para después del clip no cíclico actual.

```2gs
queueAnimation idle
```

## 10.3 `stopAnimation`

Detiene Animator y marca `finished=true`.

---

# 11. Audio 2.2.2

Los SoundAsset se importan desde WAV. El runtime usa `javax.sound.sampled`. SFX admite varias instancias simultáneas; Music usa un canal musical exclusivo.

## 11.1 `playSound <sonido> [volumen]`

Volumen de 0 a 1.

```2gs
playSound golpe 0.8
```

## 11.2 `stopSound <sonido>`

Detiene las instancias de ese SFX.

```2gs
stopSound lluvia
```

## 11.3 `playMusic <sonido> [volumen]`

Detiene la música anterior y reproduce el SoundAsset indicado.

```2gs
playMusic tema_bosque 0.55
```

## 11.4 `stopMusic`

```2gs
stopMusic
```

## 11.5 Propiedades de SoundAsset

```text
key:string            # ID canónico
name:string
category:SFX|MUSIC
volume:number 0..1
loop:boolean
data:WAV embebido
```

No existe una ruta `SoundAsset.volume` dentro de 2GameScript 2.2.2; el volumen puntual se controla por los argumentos de `playSound`/`playMusic` y el volumen base se configura en el Studio.

---

# 12. Cámara

## 12.1 `cameraShake <intensidad> <segundos>`

```2gs
cameraShake 8 0.30
```

La intensidad se expresa en píxeles lógicos aproximados. El shake decae durante su duración y se combina con seguimiento/smoothing.

## 12.2 `Camera2D`

Definición: componente que selecciona un objetivo, controla zoom/offset, suavizado, dead zone y shake inicial. Cuando hay más de uno activo, gana el de mayor `priority`.

### Propiedades

```text
enabled:boolean
target:string
follow:boolean
zoom:number
offsetX:number
offsetY:number
pixelSnap:boolean
priority:number
smoothSpeed:number
deadZoneX:number
deadZoneY:number
shakeIntensity:number
shakeDuration:number
```

### Lectura/escritura desde 2GameScript

```2gs
Camera2D.target = player
Camera2D.zoom = 1.5
Camera2D.smoothSpeed = 5
Camera2D.deadZoneX = 2
Camera2D.deadZoneY = 1
Camera2D.pixelSnap = true
```

### Comportamiento

- `target`: ID o nombre de entidad. El inspector usa selector.
- `follow=true`: si `target` está vacío y Camera2D está en una entidad, puede seguir esa entidad.
- `zoom`: mínimo runtime 0.05.
- `offsetX/Y`: desplazamiento en unidades de mundo.
- `pixelSnap`: redondea la posición de cámara a pasos de píxel lógico.
- `priority`: selecciona cámara activa entre varias.
- `smoothSpeed=0`: seguimiento inmediato. Valores mayores interpolan exponencialmente.
- `deadZoneX/Y=0`: desactiva el eje de dead zone.
- `shakeIntensity/shakeDuration`: shake inicial del componente; `cameraShake` permite activarlo durante gameplay.

El editor de escena 2.2.2 dibuja el viewport lógico y dead zone como guías; estas guías no forman parte del juego exportado.

---

# 13. Texto

## `showText`

Tres modos: `free`, `bubble`, `novel`.

### FREE

```text
showText free [ancla] <texto> <segundos> [style ID] [color #RRGGBB]
```

```2gs
showText free screen "NIVEL 1" 3 style titulo
```

### BUBBLE

```text
showText bubble [ancla] <texto> <segundos> [style ID] [color #RRGGBB]
```

```2gs
showText bubble self "¿Qué fue eso?" 4 style guardia
```

### NOVEL

```text
showText novel <hablante> <texto> <click|key [TECLA]|segundos> [style ID] [color #RRGGBB]
```

```2gs
showText novel Diego "No deberíamos estar aquí." click style dialogo
```

## 13.1 TextStyle

```text
key
name
fontKey
skinAssetKey
fontSize
speakerFontSize
textColor
speakerColor
backgroundColor
borderColor
outlineColor
shadowColor
bold
boldSpeaker
shadow
tail
borderWidth
radius
paddingX
paddingY
outlineWidth
fadeIn
fadeOut
maxWidth
panelHeight
alignment
```

`skinAssetKey` puede apuntar a un Asset con márgenes Nine-Slice. Si existe un skin válido, se usa para el panel; fondo/borde/radio quedan como fallback.

---

# 14. Componentes integrados

Toda propiedad de un componente puede leerse como `Componente.propiedad`. Las propiedades de solo lectura se indican explícitamente.

## 14.1 SpriteRenderer

Propósito: visual básico de una entidad.

```text
enabled:boolean
opacity:number
flipX:boolean
flipY:boolean
tint:string
palette:string
```

Ejemplos:

```2gs
SpriteRenderer.opacity = 0.5
SpriteRenderer.flipX = true
SpriteRenderer.palette = paleta_noche
```

`palette` referencia PaletteAsset. `tint` forma parte del modelo del componente; 2.2.2 no garantiza una mezcla compleja de color equivalente a shaders.

## 14.2 Animator

```text
enabled:boolean
controller:string
clip:string
state:string
speed:number
flipX:boolean
flipY:boolean
frame:number          # runtime
finished:boolean      # runtime
```

Ejemplos:

```2gs
Animator.state = run
Animator.speed = 1.2
```

Métodos relacionados: `playAnimation`, `queueAnimation`, `stopAnimation`.

Eventos relacionados: `animationStart`, `animationEnd`, `animationLoop`, `animationEvent`.

## 14.3 Camera2D

Véase capítulo 12. Todas sus propiedades están listadas allí.

## 14.4 ParticleEmitter2D

```text
enabled:boolean
preset:string
playing:boolean
```

`preset` debe ser el Nombre/ID canónico de un ParticlePreset existente.

```2gs
on start
  ParticleEmitter2D.preset = test_particle
  ParticleEmitter2D.playing = true
end
```

Para reiniciar un Burst:

```2gs
on event explosion
  ParticleEmitter2D.playing = false
  ParticleEmitter2D.playing = true
end
```

En 2.2.2 el runtime detecta transición off→on y cambio de preset, reinicia el estado Burst y el acumulador correspondiente.

## 14.5 GridMovement

```text
enabled:boolean
step:number
repeatDelay:number
moveDuration:number
allowDiagonal:boolean
allowArrows:boolean
snap:boolean
```

```2gs
GridMovement.step = 1
GridMovement.moveDuration = 0.10
GridMovement.allowDiagonal = false
```

Usa las acciones MoveLeft/MoveRight/MoveUp/MoveDown.

## 14.6 PlayerController

```text
enabled:boolean
speed:number
allowArrows:boolean
```

```2gs
PlayerController.speed = 4
```

## 14.7 PlatformerController

```text
enabled:boolean
speed:number
jumpSpeed:number
leftAction:string
rightAction:string
jumpAction:string
```

```2gs
PlatformerController.speed = 5
PlatformerController.jumpSpeed = 8
PlatformerController.jumpAction = Jump
```

Necesita un suelo detectable y normalmente Rigidbody2D + collider.

## 14.8 Rigidbody2D

```text
enabled:boolean
mass:number
gravityScale:number
drag:number
maxSpeed:number
freezeX:boolean
freezeY:boolean
grounded:boolean        # solo lectura
touchingLeft:boolean    # solo lectura
touchingRight:boolean   # solo lectura
touchingTop:boolean     # solo lectura
```

```2gs
Rigidbody2D.gravityScale = 1
Rigidbody2D.drag = 0.4
if Rigidbody2D.grounded == true
  self.vy = -8
end
```

Las cuatro propiedades de contacto son calculadas por runtime; intentos de escritura directa son ignorados por los contextos protegidos.

## 14.9 BoxCollider2D

```text
enabled:boolean
width:number
height:number
offsetX:number
offsetY:number
solid:boolean
```

```2gs
BoxCollider2D.solid = false
```

## 14.10 CircleCollider2D

```text
enabled:boolean
radius:number
offsetX:number
offsetY:number
solid:boolean
```

El runtime 2.2.2 utiliza una aproximación robusta para contactos en su sistema de colisión discreto; no debe confundirse con un motor de física continua de propósito general.

## 14.11 Patrol

```text
enabled:boolean
axis:x|y
distance:number
speed:number
```

```2gs
Patrol.axis = x
Patrol.distance = 6
Patrol.speed = 2
```

## 14.12 ScenePortal

```text
enabled:boolean
targetScene:string
targetX:number
targetY:number
transition:string
```

```2gs
ScenePortal.targetScene = nivel_2
ScenePortal.targetX = 7
ScenePortal.targetY = 8
```

El cambio se activa por contacto con un jugador controlado.

## 14.13 Trigger

```text
enabled:boolean
once:boolean
```

```2gs
Trigger.once = true
```

Produce `on trigger`.

## 14.14 Health

```text
enabled:boolean
max:number
current:number
```

```2gs
Health.max = 150
Health.current = 150
damage self 20
heal self 10
```

`current` se normaliza entre cero y `max` al pasar por las operaciones de Health del runtime.

## 14.15 DamageOnContact

```text
enabled:boolean
damage:number
```

```2gs
DamageOnContact.damage = 15
```

## 14.16 Clickable

```text
enabled:boolean
```

Controla recepción de `click` y `doubleClick`.

## 14.17 SortingGroup

```text
enabled:boolean
order:number
ySort:boolean
```

```2gs
SortingGroup.order = 5
SortingGroup.ySort = true
```

---

# 15. Rutas de entidad

Las rutas base disponibles en `self` y entidades nombradas son:

```text
id              # lectura
name
x
y
width
height
enabled
layer
group
renderLayer
physicsLayer
sprite / asset / assetKey
vx
vy
prefab           # lectura práctica
```

Ejemplos:

```2gs
self.x += 1
self.sprite = heroe_idle
other.enabled = false
player.vx = 5
```

Una ruta de componente puede escribirse como:

```2gs
self.Health.current -= 10
player.Rigidbody2D.gravityScale = 0
```

Dentro de un script de entidad también es válido omitir `self.` en la referencia directa al componente:

```2gs
Health.current -= 10
```

---

# 16. ParticlePreset: especificación completa

ParticlePreset es el recurso de configuración consumido por ParticleEmitter2D.

```text
key:string                    ID canónico
name:string                   igualado al ID en la edición 2.2.2
assetKey:string               sprite/región opcional
rate:number                   partículas por segundo en modo continuo
lifetime:number               segundos de vida
speed:number                  velocidad base
spread:number                 apertura angular en grados
direction:number              dirección central en grados; -90 apunta arriba
gravity:number                aceleración vertical
startScale:number
endScale:number
startOpacity:number 0..1 recomendado
endOpacity:number 0..1 recomendado
burst:boolean
burstCount:integer
localSpace:boolean
```

## 16.1 Emisión continua

```text
burst = false
rate = 30
```

El emisor acumula `rate * dt`. Cada unidad completa genera una partícula. Existe un guard de seguridad por frame para evitar emisiones patológicas infinitas.

## 16.2 Burst

```text
burst = true
burstCount = 20
```

Se dispara una vez por activación/preset. En 2.2.2 apagar y volver a encender `playing`, o cambiar de preset, rearma el Burst.

## 16.3 Dirección y dispersión

La dirección de cada partícula se calcula alrededor de `direction ± spread/2`.

```text
direction = -90
spread = 30
```

produce un cono centrado hacia arriba.

## 16.4 `localSpace`

- `true`: la posición de la partícula conserva su offset respecto del emisor y visualmente sigue el movimiento del owner.
- `false`: la partícula nace en coordenadas de mundo y continúa independiente del movimiento posterior del emisor.

## 16.5 Escala y opacidad

Se interpolan linealmente durante `age/lifetime`.

```text
startScale = 1
endScale = .2
startOpacity = 1
endOpacity = 0
```

## 16.6 Sprite y fallback

Si `assetKey` está vacío, el runtime dibuja un cuadrado blanco. Esto permite comprobar el motor sin preparar un sprite. Si existe Asset, se renderiza respetando opacidad y escala.

## 16.7 Particle Studio

El editor 2.2.2 dispone de preview en tiempo real con modos Sprite/Pixel, reinicio, dirección, spread, gravedad, escala, opacidad, continuous y Burst. El preview es una herramienta de autoría; no modifica el reloj del juego.

---

# 17. Assets, regiones y Nine-Slice

## 17.1 Asset

Campos relevantes:

```text
key
sourceName
data
sourceOnly
sourceAssetKey
regionX / regionY / regionWidth / regionHeight
filterMode: PIXEL|LINEAR
category
pivotX / pivotY
sourcePath / sourceModified
tags
sliceTop / sliceRight / sliceBottom / sliceLeft
```

## 17.2 Regiones virtuales

Una región referencia la imagen fuente y no duplica sus bytes. AnimationClip, Prefab, Tile, ParticlePreset y otros recursos pueden apuntar a su key.

## 17.3 Nine-Slice

Se activa cuando al menos un margen es mayor que cero. Los cuatro bordes se conservan mientras el centro y los laterales se estiran. 2.2.2 lo usa en el renderer y en skins de TextStyle.

El editor muestra guías sobre el sprite para los cuatro márgenes.

---

# 18. Prefabs

Un Prefab puede ser TILEMAP, ENTITY o AUTO.

```text
key
name
parentPrefab
placement
 tileId
template
variantOverrides
```

AUTO usa Tilemap para casos simples y Entity cuando existe estado/comportamiento individual.

Las instancias conservan `prefabKey`. En 2.2.2 el runtime conserva componentes añadidos explícitamente a una instancia aunque el Prefab base no los tenga; esto evita que un ParticleEmitter2D añadido en la escena desaparezca al comenzar el juego.

### `spawnPrefab`

```2gs
spawnPrefab enemigo 12 5
```

### Herencia

Los componentes del hijo sustituyen componentes del mismo tipo del padre durante la composición. Los overrides se aplican después.

### Límite

Los ciclos de herencia se cortan mediante el conjunto de Prefabs visitados; no constituyen una jerarquía válida y deben corregirse en autoría.

---

# 19. AnimationClip y AnimatorController

## AnimationClip

```text
key
name
category
frames[]
loop
pingPong
randomStart
speed
```

Cada frame:

```text
assetKey
duration
event
```

`event` produce `on animationEvent <event>` al entrar al frame.

## AnimatorController

```text
key
name
defaultState
states
transitions
```

Estado:

```text
name
clipKey
speed
```

Transición:

```text
from
to
path
operator
value
exitTime
priority
```

`AnyState` puede actuar como origen. `exitTime=-1` desactiva el requisito temporal.

---

# 20. Input Map

InputAction:

```text
key
name
bindings[]
deadZone
```

Acciones por defecto:

```text
MoveLeft  = A, LEFT
MoveRight = D, RIGHT
MoveUp    = W, UP
MoveDown  = S, DOWN
Jump      = SPACE
Accept    = ENTER, SPACE
Cancel    = ESCAPE
```

2GameScript recomendado:

```2gs
ifAction MoveLeft
  self.vx = -5
end
```

No acoples gameplay nuevo a `ifKey` si quieres que el usuario pueda remapear controles.

---

# 21. TileLayer y física de superficie

TileLayer guarda una matriz compacta de IDs de Tile. No crea una Entity por cada celda.

Propiedades:

```text
visible
locked
collision
ySort
renderLayer
physicsLayer
order
opacity
parallaxX
parallaxY
script
variables
```

TileDef:

```text
id
name
walkable / solid
assetKey
oneWay
friction
damage
animationClip
tags
```

La solidez se define en el Tile/Pincel; la capa decide si participa en colisiones y mediante qué physicsLayer.

---

# 22. Herramientas de escena 2.2.2

Además de Seleccionar/Dibujar/Rellenar/Borrar/Objeto vacío, el Studio expone:

- Línea: Bresenham sobre la capa Tilemap activa.
- Rectángulo: contorno; con Shift, relleno.
- Cuentagotas: toma el Tile de la celda y selecciona el Prefab/Pincel asociado.
- Guía de viewport Camera2D.
- Guía de dead zone.
- selectores de ParticlePreset y target Camera2D.

Estas herramientas afectan autoría, no añaden nuevas instrucciones a 2GameScript.

---

# 23. Navegador global de recursos

`Recursos > Navegador global de recursos` presenta en un árbol único:

- Assets
- Fuentes
- Audio
- Prefabs
- AnimationClips
- AnimatorControllers
- TextStyle
- ParticlePresets
- Paletas
- Input Map
- Escenas
- Menús

Incluye búsqueda y usos directos detectados. Su función es reducir referencias huérfanas y permitir inspección transversal sin recorrer cada workspace.

---

# 24. Validación

La validación tiene dos capas.

## 24.1 Sintáctica

La produce `ScriptProgram.compile(...).validation()` y detecta comandos desconocidos, bloques sin `end`, argumentos inválidos, números inválidos y estructuras no admitidas.

## 24.2 Semántica de proyecto

2.2.2 valida referencias estáticas a:

```text
loadScene
showMenu
spawnPrefab
playAnimation / queueAnimation
ifAction / ifActionPressed
setSprite
playSound / stopSound / playMusic
ParticleEmitter2D.preset
TextStyle usado por showText
```

Ejemplo de error:

```text
Línea 3: ParticlePreset 'explosion' no existe en el proyecto
```

Las referencias dinámicas que contienen `${...}` no pueden comprobarse estáticamente y se dejan para runtime.

---

# 25. Errores y diagnóstico runtime

Los subsistemas no deben fallar silenciosamente cuando existe un diagnóstico útil. Ejemplos 2.2.2:

- escena inexistente en `loadScene`: log de script/runtime;
- menú inexistente: log;
- Prefab inexistente: log;
- componente desconocido en `addComponent`: log;
- ParticlePreset inexistente: `[Partículas] No existe el preset '...' usado por ...`;
- valor numérico inválido al escribir propiedad base: log.

El validador del Studio debe capturar la mayoría de referencias estáticas antes de ejecutar.

---

# 26. Límites explícitos

1. 2gameRL 2.2.2 no es un motor de física continua de propósito general. Su colisión está orientada a juegos 2D tile/entity y puede necesitar velocidades/tamaños razonables para evitar tunneling.
2. CircleCollider2D usa una aproximación dentro del sistema de contactos actual.
3. `every` requiere cantidad finita.
4. `repeat` es inmediato; un valor enorme puede consumir el presupuesto de instrucciones del evento.
5. Existe un guard de 100000 instrucciones por cola de evento.
6. Existe un guard de acciones programadas por frame.
7. Existe un guard de emisión de partículas por frame.
8. Audio 2.2.2 se centra en WAV compatible con `javax.sound.sampled`; MP3/OGG no son formatos de importación pública de esta versión.
9. No existe scripting arbitrario de Java desde 2GameScript.
10. El sistema no expone shaders programables como API de usuario.
11. Las referencias dinámicas construidas con interpolación no pueden garantizarse en validación estática.
12. Renombrar recursos distintos de ParticlePreset no tiene todavía una migración canónica universal equivalente para todos los tipos.
13. La guía de Camera2D del editor es una representación de autoría y no un segundo runtime.
14. `global.*` no persiste entre ejecuciones; `save.*` sí.
15. Los IDs de entidades de runtime generadas pueden incluir sufijos para mantener unicidad.

---

# 27. Ejemplos completos

## 27.1 Personaje de plataformas

```2gs
on start
  Health.max = 100
  Health.current = 100
end

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
      playSound salto 0.7
    end
  end
end

on collision
  ifOther
    if other.DamageOnContact.enabled == true
      damage self 10
      cameraShake 5 0.15
    end
  end
end

on destroy
  spawnPrefab explosion
end
```

Requisitos: Rigidbody2D, BoxCollider2D, Health; SoundAsset `salto`; Prefab `explosion` si se usa ese evento.

## 27.2 Explosión con partículas Burst

Configuración de ParticlePreset `explosion_fx`:

```text
burst=true
burstCount=32
lifetime=.7
speed=4
direction=-90
spread=360
gravity=2
startScale=1
endScale=.1
startOpacity=1
endOpacity=0
localSpace=false
```

Script:

```2gs
on start
  ParticleEmitter2D.preset = explosion_fx
  ParticleEmitter2D.playing = true
  playSound explosion 0.9
  cameraShake 9 0.25
  timer 1 destroy
end
```

## 27.3 Humo que sigue al emisor

```text
ParticlePreset smoke_fx:
rate=12
lifetime=1.8
speed=.6
direction=-90
spread=25
gravity=-.2
localSpace=true
```

```2gs
on start
  ParticleEmitter2D.preset = smoke_fx
  ParticleEmitter2D.playing = true
end
```

## 27.4 Música de escena

```2gs
on start
  playMusic bosque_loop 0.45
end

on event combate
  stopMusic
  playMusic combate_loop 0.65
end
```

## 27.5 Cámara suave

```2gs
on start
  Camera2D.target = player
  Camera2D.zoom = 1.25
  Camera2D.smoothSpeed = 4
  Camera2D.deadZoneX = 2
  Camera2D.deadZoneY = 1
end
```

## 27.6 Puerta persistente

```2gs
on start
  if save.puertaRoja == true
    BoxCollider2D.enabled = false
    self.sprite = puerta_abierta
  end
end

on event llaveRojaObtenida
  save.puertaRoja = true
  BoxCollider2D.enabled = false
  self.sprite = puerta_abierta
  playSound puerta 0.8
end
```

---

# 28. Matriz de referencia rápida

| Elemento | Lee | Escribe | Comando asociado | Evento asociado |
|---|---|---|---|---|
| SpriteRenderer | sí | sí | setSprite | — |
| Animator | sí | sí | play/queue/stopAnimation | animationStart/End/Loop/Event |
| Camera2D | sí | sí | cameraShake | — |
| ParticleEmitter2D | sí | sí | — | — |
| GridMovement | sí | sí | — | — |
| PlayerController | sí | sí | — | — |
| PlatformerController | sí | sí | — | — |
| Rigidbody2D | sí | parcial; sensores RO | velocity/move | collision |
| BoxCollider2D | sí | sí | — | collision |
| CircleCollider2D | sí | sí | — | collision |
| Patrol | sí | sí | — | collision |
| ScenePortal | sí | sí | loadScene relacionado | trigger/contacto |
| Trigger | sí | sí | — | trigger |
| Health | sí | sí | damage/heal | destroy indirecto |
| DamageOnContact | sí | sí | damage relacionado | collision |
| Clickable | sí | sí | — | click/doubleClick |
| SortingGroup | sí | sí | — | — |
| SoundAsset | por ID | Studio | playSound/playMusic | — |
| ParticlePreset | por ID | Studio | usado por ParticleEmitter2D | — |
| TextStyle | por ID | Studio | showText | — |
| AnimationClip | por ID | Studio | playAnimation | animation* |
| Prefab | por ID | Studio | spawnPrefab | start al instanciar |
| Scene | scene.* | sí | loadScene/restartScene | start/update/event |
| TileLayer | layer.* | sí | — | update/event |

---

# 29. Contrato de compatibilidad 2.2.2

El formato de proyecto es versión 8. ProjectIO mantiene defaults al leer propiedades nuevas ausentes, de manera que proyectos previos pueden abrirse sin exigir que ya contengan audio, nine-slice, direction o skinAssetKey. Los recursos nuevos se serializan dentro del `.2grl`, incluido audio bajo `audio/` y metadatos asociados.

La publicación Windows pasa por el pipeline de CI del repositorio: tests Java, smokes deterministas, runtime Windows, empaquetado del Studio, lanzamiento del ejecutable, exportación de un juego con el toolchain embebido y lanzamiento del juego exportado. La presencia de un tag/release no sustituye este contrato: el release se crea solo después de pasar el pipeline configurado.

---

# 30. Índice de comandos

```text
log / print
move
velocity
teleport
bounce
destroy
destroyEntity
wait
timer
every
create / spawn
spawnPrefab
playAnimation
queueAnimation
stopAnimation
playSound
stopSound
playMusic
stopMusic
cameraShake
loadScene
showMenu
restartScene
setSprite
setEntitySprite
emit
showText
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
ifAction
ifActionPressed
ifVar
ifGlobal
ifProperty
ifEntity
ifComponent
ifOther
chance
repeat
stop / return
```

Cada comando aparece documentado en capítulos anteriores con firma, semántica y al menos un ejemplo de uso. Esta Biblia es la referencia canónica de 2GameScript 2.2.2 y debe viajar embebida en 2gameRL Studio.

---

## Referencia técnica 2.2.2 — ParticlePreset y ParticleEmitter2D

Esta sección es normativa para el hotfix 2.2.2. Un `ParticlePreset` es un recurso de proyecto; `ParticleEmitter2D` únicamente referencia y activa ese recurso.

### ParticlePreset

| Campo | Tipo | Rango/valores | Escritura | Semántica |
|---|---|---|---|---|
| `key` | string | ID único | solo al renombrar recurso | Referencia canónica usada por `ParticleEmitter2D.preset`. |
| `renderMode` | enum | `PIXEL`, `SPRITE` | editor | `PIXEL` dibuja una primitiva; `SPRITE` usa `assetKey`. |
| `assetKey` | string | asset/región o vacío | editor | Sprite usado cuando `renderMode=SPRITE`; si falta/no resuelve, runtime cae a la primitiva Pixel para no volver invisible el efecto. |
| `shape` | enum | `SQUARE`, `CIRCLE`, `DIAMOND` | editor | Forma de la partícula cuando se renderiza como Pixel. |
| `color` | ARGB | color | editor | Color de la primitiva Pixel. La opacidad final también multiplica este alfa. |
| `rate` | number | `>= 0` | editor | Partículas por segundo en emisión continua. No se usa en Burst. |
| `lifetime` | number | `> 0` | editor | Vida individual en segundos. |
| `speed` | number | `>= 0` | editor | Velocidad base en unidades de mundo por segundo; cada nacimiento aplica variación aleatoria de 0.75× a 1.25×. |
| `direction` | number | grados | editor | Dirección central. `0°` = derecha, `90°` = abajo, `-90°` = arriba, siguiendo el eje Y de pantalla del runtime. |
| `spread` | number | `0..360` | editor | Cono angular total centrado en `direction`. |
| `gravity` | number | cualquier real | editor | Aceleración vertical aplicada a cada partícula. Positivo = abajo. |
| `startScale` / `endScale` | number | `>= 0` | editor | Interpolación lineal de escala durante la vida. |
| `startOpacity` / `endOpacity` | number | `0..1` | editor | Interpolación lineal de opacidad durante la vida. |
| `burst` | boolean | `true/false` | editor | `false`: continua; `true`: emite una vez al activarse. |
| `burstCount` | integer | `>= 1` en Studio | editor | Cantidad solicitada por Burst. El runtime limita entradas anómalas a 10.000 nacimientos por activación. |
| `localSpace` | boolean | `true/false` | editor | `true`: la posición de las partículas sigue al emisor después de nacer; `false`: quedan en coordenadas mundiales independientes. |

Compatibilidad: presets guardados por una build 2.2.2 anterior no contienen `renderMode`, `shape` ni `color`. Al cargarlos, `renderMode` se infiere como `SPRITE` si ya tenían `assetKey`, o `PIXEL` si no lo tenían; `shape=SQUARE` y `color=WHITE` se aplican como defaults.

### ParticleEmitter2D

Propiedades de componente:

```text
enabled : boolean
preset  : string
playing : boolean
```

`preset` acepta el `key` canónico o un nombre resoluble del `ParticlePreset`. Un preset inexistente no detiene el runtime: se informa en el log y el estado interno del emisor se reinicia. Si posteriormente se asigna de nuevo el mismo preset válido, un Burst vuelve a dispararse correctamente.

`playing=false` detiene nuevos nacimientos, pero no elimina partículas ya vivas. La transición `false -> true` reinicia el estado de Burst y su acumulador continuo.

Ejemplo — Burst reiniciable:

```2gs
on start
  ParticleEmitter2D.preset = explosion_fx
  ParticleEmitter2D.playing = true
  wait 0.2
  ParticleEmitter2D.playing = false
  wait 0.1
  ParticleEmitter2D.playing = true
end
```

Ejemplo — cambiar de preset y recuperarse de una referencia inválida:

```2gs
on event damageTaken
  ParticleEmitter2D.preset = sparks_fx
  ParticleEmitter2D.playing = true
end
```

### Límites de seguridad del runtime 2.2.2

El runtime mantiene como máximo 20.000 partículas vivas y acepta como máximo 10.000 nacimientos de un emisor en un frame/activación. Cuando se alcanza un límite, descarta emisiones nuevas y escribe un diagnóstico una sola vez por ciclo del emisor. Estos límites evitan que un proyecto corrupto o un valor extremo agote memoria o congele un juego exportado.

### Particle Studio

`Abrir Particle Studio` presenta una previsualización animada de dirección, dispersión, gravedad, escala, opacidad y forma. Los controles `Pixel`/`Sprite`, forma y color ya no son filtros temporales de preview: modifican el `ParticlePreset` real y se persisten dentro del `.2grl`.

