# 2GameScript — Biblia del lenguaje

> Documento canónico del scripting de 2gameRL.
>
> **Estado:** la sección **2.0.12** documenta únicamente lo que existe en la versión publicada. Las secciones marcadas **2.0.13 / diseño** describen la sintaxis y subsistemas aprobados para la siguiente evolución; no deben confundirse con funcionalidad ya publicada.

2GameScript es el lenguaje de eventos propio de 2gameRL. No es Lua, JavaScript, Python ni Java embebido. El motor compila cada script a instrucciones internas y lo ejecuta sobre la entidad que posee el script.

---

# 1. Reglas generales — 2.0.12

Todo comando debe estar dentro de un evento:

```text
on start
  log "Hola"
end
```

Los comentarios comienzan con `#`:

```text
# comentario
on start
  log "inicio"
end
```

Las comillas agrupan texto con espacios:

```text
log "Hola mundo"
```

La sintaxis 2.0.12 no usa `;`, `{}` ni asignaciones con `=`. Los comandos se separan por espacios.

## Eventos disponibles

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

- `start`: al crearse o cargarse la entidad.
- `update`: cada actualización del runtime.
- `click`: clic sobre la entidad.
- `doubleClick`: doble clic.
- `collision`: al comenzar una colisión física.
- `trigger`: al entrar en un Trigger.
- `destroy`: justo antes de destruir la entidad.

`collision`, `trigger` y determinados casos de destrucción pueden exponer `other`, que representa la otra entidad involucrada.

---

# 2. Referencias a entidades

Muchos comandos aceptan una referencia de entidad:

- `self` o `this`: entidad que ejecuta el script.
- `other`: contraparte del evento actual.
- ID exacto de entidad.
- nombre de entidad.

Ejemplos:

```text
destroyEntity other
moveEntity enemigo 1 0
teleportEntity player 8 4
setEntity jefe enabled false
```

El runtime intenta resolver primero el ID y luego el nombre sin distinguir mayúsculas/minúsculas.

---

# 3. Propiedades de entidad

`set` modifica propiedades de la entidad ejecutora, **no propiedades de componentes**.

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
set sprite hero_idle
set vx 2
set vy 0
```

Sobre otra entidad:

```text
setEntity enemigo x 12
setEntity enemigo y 6
setEntity enemigo physicsLayer Enemy
setEntity enemigo enabled false
```

Propiedades reconocidas por el runtime:

- `id` — solo lectura práctica.
- `name`
- `x`
- `y`
- `width`
- `height`
- `enabled`
- `layer`
- `group`
- `renderLayer`
- `physicsLayer`
- `sprite`, `asset`, `assetKey`
- `vx`
- `vy`

Un nombre no reconocido por `set` se guarda actualmente como variable de entidad.

---

# 4. Componentes

La sintaxis 2.0.12 para componentes es explícita:

```text
addComponent <entidad> <tipo>
removeComponent <entidad> <tipo>
setComponent <entidad> <tipo> <propiedad> <valor>
enableComponent <entidad> <tipo> <true|false>
```

Ejemplos:

```text
addComponent self Rigidbody2D
setComponent self Rigidbody2D gravityScale -1
enableComponent self BoxCollider2D false
removeComponent self Patrol
```

## Componentes integrados

### GridMovement

Movimiento discreto por losetas.

Propiedades:

- `enabled` — `true`
- `step` — `1`
- `repeatDelay` — `0.16`
- `moveDuration` — `0`
- `allowDiagonal` — `false`
- `allowArrows` — `true`
- `snap` — `true`

Ejemplo:

```text
on start
  setComponent self GridMovement step 2
  setComponent self GridMovement allowDiagonal true
  setComponent self GridMovement moveDuration 0.15
end
```

`GridMovement` respeta `BoxCollider2D`, tiles sólidos y capas físicas.

### PlayerController

Movimiento libre continuo.

Propiedades:

- `enabled` — `true`
- `speed` — `4`
- `allowArrows` — `true`

```text
setComponent self PlayerController speed 6
```

### Rigidbody2D

Física básica.

Propiedades:

- `enabled` — `true`
- `mass` — `1`
- `gravityScale` — `1`
- `drag` — `0.4`
- `maxSpeed` — `12`

Ejemplos:

```text
setComponent self Rigidbody2D gravityScale 1
setComponent self Rigidbody2D gravityScale 0
setComponent self Rigidbody2D gravityScale -1
setComponent self Rigidbody2D maxSpeed 20
```

La gravedad del runtime usa `9.81 * gravityScale` como aceleración vertical base.

### BoxCollider2D

Propiedades:

- `enabled` — `true`
- `width` — `0.82`
- `height` — `0.82`
- `solid` — `true`

```text
setComponent self BoxCollider2D width 1
setComponent self BoxCollider2D height 1
setComponent self BoxCollider2D solid true
```

Dos entidades activas con `BoxCollider2D`, `solid=true` y capas físicas compatibles se bloquean entre sí. `Rigidbody2D` no es requisito para que exista bloqueo físico.

### Patrol

Propiedades:

- `enabled` — `true`
- `axis` — `x`
- `distance` — `4`
- `speed` — `1.5`

```text
setComponent self Patrol axis y
setComponent self Patrol distance 8
setComponent self Patrol speed 2
```

### ScenePortal

Propiedades:

- `enabled` — `true`
- `targetScene` — `level-1`
- `targetX` — `2`
- `targetY` — `2`

```text
setComponent self ScenePortal targetScene bosque
setComponent self ScenePortal targetX 10
setComponent self ScenePortal targetY 4
```

El portal reacciona a entidades controladas mediante `PlayerController` o `GridMovement`.

### Trigger

Propiedades:

- `enabled` — `true`
- `once` — `false`

```text
setComponent self Trigger once true
```

### Health

Propiedades:

- `enabled` — `true`
- `max` — `100`
- `current` — `100`

```text
setComponent self Health max 200
setComponent self Health current 150
```

### DamageOnContact

Propiedades:

- `enabled` — `true`
- `damage` — `10`

```text
setComponent self DamageOnContact damage 25
```

### Clickable

Permite controlar explícitamente si la entidad acepta clics.

```text
enableComponent self Clickable false
```

Sin `Clickable`, una entidad es clickeable por defecto.

---

# 5. Movimiento directo

```text
move 1 0
move -1 0
velocity 3 -1
teleport 8 4
bounce
```

- `move <dx> <dy>`: desplazamiento relativo en unidades de loseta.
- `velocity <vx> <vy>`: establece velocidad.
- `teleport <x> <y>`: posición absoluta.
- `bounce`: invierte `vx` y `vy`.

Otra entidad:

```text
moveEntity enemigo 1 0
teleportEntity enemigo 8 4
```

---

# 6. Teclado

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

- `ifKey`: verdadero mientras la tecla permanezca pulsada.
- `ifPressed`: verdadero solo en el instante de pulsación.

---

# 7. Variables locales

Cada instancia tiene sus propias variables.

```text
setVar score 0
addVar score 5
mulVar score 2
divVar score 4
randomVar suerte 1 100
```

Restar se hace mediante una suma negativa:

```text
addVar vida -10
```

Las variables se almacenan internamente como texto; las operaciones aritméticas intentan convertirlas a número.

---

# 8. Variables globales

```text
setGlobal monedas 0
addGlobal monedas 1
```

Las globales sobreviven a cambios de escena mientras el runtime siga abierto. En 2.0.12 **no son persistentes entre ejecuciones del juego**.

---

# 9. Interpolación

Formas disponibles:

```text
${variable}
${global:monedas}
${prop:x}
${other}
```

Ejemplo:

```text
log "Score ${score}; monedas ${global:monedas}; X ${prop:x}; otro ${other}"
```

La interpolación existe para cadenas. En 2.0.12 varios comandos numéricos esperan números literales durante compilación; por ejemplo no debe asumirse que `move ${velocidad} 0` sea válido.

---

# 10. Condiciones

## Variables locales

```text
ifVar score >= 10
  log "ganaste"
else
  log "faltan puntos"
end
```

## Globales

```text
ifGlobal monedas >= 5
  log "puedes comprar"
end
```

## Propiedades de la entidad

```text
ifProperty x > 10
  teleport 2 2
end
```

## Existencia de entidad

```text
ifEntity jefe exists
  log "sigue vivo"
end

ifEntity jefe missing
  log "ya no existe"
end
```

## Componentes

```text
ifComponent self Health exists
  log "tengo vida"
end

ifComponent self Health missing
end

ifComponent self Health enabled
end

ifComponent self Health disabled
end
```

## `other`

```text
ifOther
  damage other 10
end
```

## Probabilidad

```text
chance 0.25
  create premio
end
```

`chance` usa un valor entre `0` y `1`.

## Operadores

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

Si ambos operandos son numéricos se comparan como números. En caso contrario se comparan como texto sin distinguir mayúsculas/minúsculas.

---

# 11. Bucles

```text
repeat 5
  create chispa
end
```

Límite actual: 10.000 repeticiones.

---

# 12. Temporizadores

## `wait`

Pausa únicamente la secuencia actual; no congela física, render, input ni otros scripts.

```text
on click
  destroy
  wait 9
  create pj
end
```

## `timer`

Agenda una acción futura y continúa inmediatamente.

```text
timer 3 create explosion 8 6
```

`timer` no admite `wait` o `every` como comando anidado directo.

## `every`

```text
every 1 5 create chispa
```

Significa: ejecutar el comando cada 1 segundo, 5 veces. Límite actual: 10.000 ejecuciones.

---

# 13. Crear entidades

```text
create enemigo
spawn enemigo
create enemigo 12 8
```

Se puede usar el ID o nombre de una entidad plantilla existente en la escena. Si no se entregan coordenadas, la instancia aparece en la posición de la entidad ejecutora.

Cada instancia recibe un ID runtime único, por ejemplo:

```text
enemy#1
enemy#2
enemy#3
```

y ejecuta su propio `on start`.

---

# 14. Destruir entidades

```text
destroy
destroyEntity other
destroyEntity enemigo
```

Antes de eliminarse se dispara `on destroy`.

---

# 15. Vida

```text
damage other 10
heal self 20
```

El objetivo necesita `Health` activo. Cuando `current` llega a `0`, el runtime destruye la entidad.

---

# 16. Variables de otras entidades

```text
setEntityVar enemigo estado dormido
addEntityVar enemigo furia 1
```

---

# 17. Sprites y spritesheets

```text
setSprite hero_idle_01
setEntitySprite enemigo slime_ataque_03
```

El nombre puede apuntar a una imagen independiente o a una región virtual de spritesheet. Una spritesheet se almacena una sola vez; cada sprite registra `x/y/ancho/alto` sobre esa textura.

---

# 18. Escenas y menús

```text
loadScene bosque
restartScene
showMenu pausa
```

En 2.0.12 los comandos de script trabajan con IDs internos. La UI del editor debe resolver nombres visibles a IDs para evitar que el usuario tenga que conocerlos.

---

# 19. Capas y colisiones

La capa visual y la física son conceptos distintos.

```text
set renderLayer Frente
set layer 20
set physicsLayer Player
```

- `renderLayer`: capa visual general.
- `layer`: orden fino dentro del render.
- `physicsLayer`: categoría usada por la matriz de colisiones.

Las capas físicas **filtran** contactos; no son requisito para que `BoxCollider2D` exista.

---

# 20. `stop` / `return`

```text
ifVar muerto == true
  stop
end
```

`return` es alias de `stop`.

---

# 21. Qué NO soporta 2.0.12

No existe todavía sintaxis orientada a objetos como:

```text
Rigidbody2D.gravityScale = -1
player.position.x = 4
Health.current -= 10
```

No hay expresiones matemáticas generales:

```text
x = x + 1
gravity = gravity * -1
```

No existen actualmente:

```text
while
for
function
class
switch
and
or
not
```

Tampoco hay un comando 2.0.12 para dibujar texto en pantalla. `log` y `print` escriben al log, no al HUD.

---

# 22. Ejemplos completos — 2.0.12

## Gravedad reversible con variable de estado

```text
on start
  addComponent self Rigidbody2D
  addComponent self BoxCollider2D
  setComponent self Rigidbody2D gravityScale 1
  setVar gravedad normal
end

on click
  ifVar gravedad == normal
    setComponent self Rigidbody2D gravityScale -1
    setVar gravedad invertida
  else
    setComponent self Rigidbody2D gravityScale 1
    setVar gravedad normal
  end
end
```

## Puerta

```text
on start
  setVar abierta false
end

on click
  ifVar abierta == false
    setSprite puerta_abierta
    enableComponent self BoxCollider2D false
    setVar abierta true
  else
    setSprite puerta_cerrada
    enableComponent self BoxCollider2D true
    setVar abierta false
  end
end
```

## Enemigo con vida

```text
on start
  addComponent self Health
  setComponent self Health max 100
  setComponent self Health current 100
end

on collision
  ifOther
    damage other 10
  end
end

on destroy
  create explosion
  addGlobal puntuacion 100
end
```

## Generador

```text
on start
  every 2 10 create enemigo
end
```

## Recogible

```text
on trigger
  addGlobal monedas 1
  log "Monedas ${global:monedas}"
  destroy
end
```

## Respawn

```text
on destroy
  wait 9
  create pj
end
```

---

# 23. Referencia rápida — 2.0.12

```text
# eventos
on start
on update
on click
on doubleClick
on collision
on trigger
on destroy
end

# log
log <texto>
print <texto>

# movimiento
move <dx> <dy>
velocity <vx> <vy>
teleport <x> <y>
bounce
moveEntity <entidad> <dx> <dy>
teleportEntity <entidad> <x> <y>

# entidades
create|spawn <entidad> [x y]
destroy
destroyEntity <entidad>
set <propiedad> <valor>
setEntity <entidad> <propiedad> <valor>
setSprite <asset-o-region>
setEntitySprite <entidad> <asset-o-region>

# variables
setVar <nombre> <valor>
addVar <nombre> <número>
mulVar <nombre> <número>
divVar <nombre> <número>
randomVar <nombre> <min> <max>
setEntityVar <entidad> <nombre> <valor>
addEntityVar <entidad> <nombre> <número>
setGlobal <nombre> <valor>
addGlobal <nombre> <número>

# componentes
addComponent <entidad> <tipo>
removeComponent <entidad> <tipo>
setComponent <entidad> <tipo> <propiedad> <valor>
enableComponent <entidad> <tipo> <true|false>

# salud
damage <entidad> <cantidad>
heal <entidad> <cantidad>

# tiempo
wait <segundos>
timer <segundos> <comando>
every <segundos> <cantidad> <comando>
repeat <cantidad> ... end

# input
ifKey <tecla> [comando]
ifPressed <tecla> [comando]

# condiciones
ifVar <nombre> <op> <valor> ... [else ...] end
ifGlobal <nombre> <op> <valor> ... [else ...] end
ifProperty <propiedad> <op> <valor> ... [else ...] end
ifEntity <entidad> <exists|missing> ... end
ifComponent <entidad> <tipo> [exists|missing|enabled|disabled] ... end
ifOther ... end
chance <0..1> ... end

# escenas / menús
loadScene <escena>
restartScene
showMenu <menú>

# control
stop
return
```

---

# 24. Diseño 2.0.13 — sintaxis de propiedad con punto

La sintaxis explícita 2.0.12 debe mantenerse como compatibilidad, pero la sintaxis principal nueva será la forma orientada a propiedades por ser más legible.

La forma deseada:

```text
Rigidbody2D.gravityScale = -1
```

será equivalente a:

```text
setComponent self Rigidbody2D gravityScale -1
```

Ejemplos previstos:

```text
Rigidbody2D.gravityScale = -1
Rigidbody2D.maxSpeed = 20
BoxCollider2D.solid = false
GridMovement.step = 2
self.x = 8
self.sprite = hero_idle
other.Health.current -= 10
enemigo.Rigidbody2D.gravityScale = 0
global.monedas += 1
```

Objetivos del parser nuevo:

- `=` asignación.
- `+=`, `-=`, `*=`, `/=` para valores numéricos.
- `Componente.propiedad` = componente de `self`.
- `entidad.Componente.propiedad` = componente de otra entidad.
- `self.propiedad` / `other.propiedad` / `entidad.propiedad`.
- `global.nombre` para estado global de sesión.
- `save.nombre` para persistencia SQLite.

Los comandos antiguos (`setComponent`, `setEntity`, etc.) se conservarán para no romper proyectos existentes.

---

# 25. Diseño 2.0.13 — sistema de texto en pantalla

El motor tendrá tres familias de texto.

## FREE — texto suelto

Usos:

- títulos de juego;
- título de nivel;
- avisos;
- texto flotante;
- daño;
- nombres temporales;
- mensajes asociados a una entidad.

Reglas:

- puede estar anclado a pantalla o a una entidad;
- si sigue una entidad, mantiene un offset en coordenadas de mundo;
- siempre debe desaparecer;
- siempre debe tener una fase de desvanecimiento;
- puede usarse con duración configurable.

## BUBBLE — bocadillo de cómic

Usos:

- diálogo en mundo;
- pensamiento;
- exclamaciones;
- texto sobre personajes.

Reglas obligatorias:

- puede seguir una entidad mientras se mueve;
- posee offset configurable;
- cola del bocadillo orientada al hablante cuando corresponda;
- debe desaparecer siempre;
- duración configurable pero **nunca superior a 10 segundos**;
- incluye fade de salida obligatorio;
- al destruirse la entidad asociada, el bocadillo termina o completa un fade corto según configuración.

## NOVEL — banda de novela visual

Usos:

- diálogo extenso;
- narración;
- conversaciones cinemáticas.

Reglas:

- se ancla principalmente a la pantalla, no al mundo;
- puede asociarse a una entidad como hablante para obtener nombre/sprite/retrato;
- duración máxima mucho mayor que BUBBLE;
- puede cerrar por tiempo;
- puede cerrar por clic;
- puede cerrar por una tecla concreta o por cualquier tecla;
- puede esperar indefinidamente a interacción cuando se configure así;
- admite fade más lento;
- debe soportar cola de líneas para conversaciones.

La API exacta se documentará aquí cuando pase a implementación; las reglas anteriores son parte del contrato funcional.

---

# 26. Diseño 2.0.13 — persistencia SQLite

SQLite se utilizará como persistencia local de cada juego exportado, no como almacén de estado por frame.

Casos de uso:

- nombre del jugador;
- puntuaciones;
- flags de historia;
- nivel desbloqueado;
- opciones;
- inventario serializable;
- estadísticas;
- valores definidos por el creador del juego;
- múltiples slots de guardado.

Sintaxis prevista:

```text
save.playerName = "Diego"
save.score = 125
save.score += 10
save.bossDefeated = true
```

`save.*` será persistente entre ejecuciones del juego. `global.*` seguirá siendo estado de sesión.

El juego exportado debe escribir la base en el directorio de datos del usuario y no junto al ejecutable, para evitar permisos de `Program Files` en Windows.

Esquema base recomendado:

```text
save_slots
  slot
  label
  created_at
  updated_at

save_values
  slot
  namespace
  key
  type
  value
  updated_at
```

El script no expondrá SQL directamente. El motor se encargará de tipos, consultas y transacciones.

---

# 27. Render y transparencia

La transparencia alfa no requiere por sí sola OpenGL. El renderer JavaFX actual ya soporta PNG con alpha, opacidad y fades.

OpenGL/LWJGL debe introducirse solo cuando el motor necesite capacidades que justifiquen un renderer dedicado, por ejemplo:

- shaders;
- iluminación 2D;
- blend modes avanzados;
- partículas masivas;
- postprocesado;
- máscaras;
- efectos de distorsión;
- control explícito del pipeline GPU.

No se debe migrar a OpenGL únicamente para hacer transparentes bocadillos o textos: sería complejidad innecesaria y obligaría a duplicar o reemplazar partes del renderer JavaFX, incluida la rasterización de texto.

---

# 28. Regla de compatibilidad

La evolución de 2GameScript seguirá esta regla:

1. la sintaxis nueva puede ser más intuitiva;
2. los scripts existentes deben seguir funcionando;
3. la Biblia debe distinguir siempre lo **publicado** de lo **diseñado**;
4. cualquier comando documentado como publicado debe tener test de parser y test de runtime cuando corresponda.
