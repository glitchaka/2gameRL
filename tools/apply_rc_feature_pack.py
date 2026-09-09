from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def read(p):return (ROOT/p).read_text(encoding='utf-8')
def write(p,s):(ROOT/p).write_text(s,encoding='utf-8',newline='\n')
def rep(p,o,n):
 s=read(p);c=s.count(o)
 if c!=1:raise RuntimeError(f'{p}: expected 1 match got {c}: {o[:120]}')
 write(p,s.replace(o,n,1))

b='docs/2GAMESCRIPT_BIBLE.md'
s=read(b)
s=s.replace('# Biblia de 2GameScript 2.2.2','# Biblia de 2GameScript 2.3.0-rc1',1).replace('en 2gameRL Studio 2.2.2. No es una colección','en 2gameRL Studio 2.3.0-rc1. No es una colección',1).replace('corresponden al runtime 2.2.2.','corresponden al runtime 2.3.0-rc1.',1)
append=r'''

---

# 30. Referencia técnica RC 2.3.0-rc1

Esta sección define la API incorporada después de 2.2.2. Los nombres de recursos mostrados en ejemplos (`hero-controller`, `suelo_cesped`, `ui-primary`) son IDs creados por el usuario; no existen automáticamente.

## 30.1 `TileDef` avanzado

Campos nuevos:

```text
autotileMode : TileAutotileMode = NONE
connectTag   : string = ""
variants[]   : TileVariant
autotileAssets[mask] : assetKey
```

`TileAutotileMode`:

```text
NONE
FOUR_WAY
EIGHT_WAY
```

Máscaras cardinales:

```text
N = 1
E = 2
S = 4
W = 8
```

Diagonales en `EIGHT_WAY`:

```text
NE = 16
SE = 32
SW = 64
NW = 128
```

La máscara se obtiene sumando los bits de los vecinos conectados. Un vecino conecta cuando usa el mismo `TileDef` o cuando `connectTag` no está vacío y el Tile vecino contiene ese tag. En `EIGHT_WAY`, si no existe una regla exacta de 8 vías, el resolver intenta la máscara cardinal `mask & 15`.

Método de resolución público:

```java
AdvancedTileResolver.assetFor(project, level, layer, x, y, tile)
AdvancedTileResolver.mask(project, layer, x, y, tile)
```

`assetFor` sigue este orden: regla autotile exacta, fallback cardinal para 8-way, variante ponderada, `assetKey` base. Las variantes ponderadas son deterministas por `(tileId, x, y, layer.id)`: una celda no cambia de sprite entre frames.

### `TileVariant`

```text
assetKey : string
weight   : number >= 0
```

Ejemplo conceptual:

```text
asset grass_a, weight 6
asset grass_b, weight 3
asset flowers, weight 1
```

No significa 60/30/10 exacto en una muestra pequeña; `weight` define peso relativo.

## 30.2 `StampPattern`

Recurso persistente para pintar patrones multi-celda en el Tilemap.

```text
key       : string, solo lectura
name      : string
width     : integer >= 1
height    : integer >= 1
cells[]   : tileId; -1 significa transparente
```

Métodos del modelo:

```java
int get(int x, int y)
void set(int x, int y, int tileId)
int[] cells()
void replaceCells(int[] values)
void resize(int width, int height)
StampPattern copy()
```

En Studio se edita desde `RC 2.3 → Stamp / patrones` y se coloca con la herramienta `Stamp`. Las celdas `-1` no sobrescriben el mapa.

## 30.3 `AnimationClip` RC

Campos públicos completos relevantes:

```text
key         : string, solo lectura
name        : string
category    : AnimationCategory
frames[]    : AnimationFrame
loop        : boolean
pingPong    : boolean
randomStart : boolean
reverse     : boolean
speed       : number >= 0
fps         : number >= 0
```

Métodos:

```java
double frameDuration(int index)
double duration()
AnimationClip copy()
```

Si `fps > 0`, `frameDuration(i)` devuelve `1 / fps` y reemplaza la duración individual para reproducción. Si `fps == 0`, se usa `AnimationFrame.duration`. `reverse=true` comienza en el último frame y avanza hacia índices menores. `pingPong` invierte el sentido en los extremos.

### `AnimationFrame`

```text
assetKey : string
duration : number > 0
event    : string
shapes[] : FrameShape
```

Los markers `event` disparan:

```2gs
on animationEvent attack_hit
  ...
end
```

### `FrameShape`

```text
name   : string
role   : HITBOX | HURTBOX
x      : number
 y     : number
width  : number > 0
height : number > 0
```

Las coordenadas son locales a la entidad y se reflejan con `Animator.flipX`/`flipY`. Al solaparse un `HITBOX` activo con un `HURTBOX` de otra entidad, se emiten dos señales de entidad:

```2gs
on event hitbox
  log "mi ataque conectó"
end

on event hurtbox
  log "mi hurtbox recibió impacto"
end
```

Además se emite la señal de mundo `animationHit`. Si el atacante posee `DamageOnContact`, su `damage` se aplica al `Health` del objetivo. Una combinación atacante/objetivo solo conecta una vez por frame de animación; al cambiar de frame el registro se reinicia.

## 30.4 Animation Lab

`RC 2.3 → Animation Lab` es el editor técnico de clips. Incluye preview animado, timeline, reordenamiento, FPS, speed, loop, ping-pong, random start, reverse, onion skin previo/siguiente, crosshair de pivot, markers y edición/overlay de hitboxes y hurtboxes.

El onion skin solo afecta la previsualización del Studio; no altera el recurso ni el runtime.

## 30.5 `AnimatorController.parameters`

Tipos:

```text
BOOL
INT
FLOAT
TRIGGER
```

Modelo:

```text
AnimatorParameter.name
AnimatorParameter.type
AnimatorParameter.defaultValue
```

Acceso desde 2GameScript:

```2gs
on start
  Animator.param.speed = 0
  Animator.param.grounded = true
end

on update
  Animator.param.speed = 2.5
end
```

Lectura condicional:

```2gs
if Animator.param.speed > 0.1
  log "movimiento"
end
```

Una transición del `AnimatorController` usa la ruta:

```text
param.speed
param.attack
```

Cuando una transición satisfecha lee un parámetro `TRIGGER`, el runtime lo restablece a `false` después de consumirlo.

Coerción al escribir desde script:

```text
BOOL/TRIGGER -> boolean
INT          -> entero redondeado
FLOAT        -> número
```

Una escritura inválida se rechaza y genera diagnóstico.

## 30.6 `BlendTree`

Cada `AnimatorState` posee `blendTree`.

```text
type       : NONE | ONE_D | TWO_D
parameterX : string
parameterY : string
children[] : BlendChild
```

`BlendChild`:

```text
clipKey : string
x       : number
y       : number
```

Semántica RC:

- `NONE`: usa `AnimatorState.clipKey`.
- `ONE_D`: selecciona el hijo con menor `abs(parameterX - child.x)`.
- `TWO_D`: selecciona el hijo con menor distancia euclidiana a `(parameterX, parameterY)`.

La RC utiliza selección discreta del clip más cercano; no interpola píxeles ni mezcla dos sprites simultáneamente.

## 30.7 `AnimationSet`

Mapea un estado lógico del Animator a clips direccionales.

```text
key           : string, solo lectura
name          : string
directionMode : TWO | FOUR | EIGHT
useFlipX      : boolean
clips         : map<string,string>
```

Slots:

```text
Walk|LEFT
Walk|RIGHT
Walk|UP
Walk|DOWN
Walk|UP_LEFT
Walk|UP_RIGHT
Walk|DOWN_LEFT
Walk|DOWN_RIGHT
Walk|DEFAULT
```

Métodos:

```java
String clip(String state, String direction)
AnimationSet copy()
```

Componente `Animator` añade:

```text
animationSet : string
direction    : string
```

`direction` refleja la dirección resuelta por velocidad. En `useFlipX=true`, un clip LEFT puede reutilizarse hacia RIGHT cuando no existe un slot RIGHT.

Ejemplo de componente:

```text
Animator.controller = hero-controller
Animator.animationSet = hero-directions
Animator.state = Walk
```

## 30.8 `UISkinAsset`

Recurso reutilizable para estados de UI:

```text
key              : string, solo lectura
name             : string
normalAssetKey   : string
hoverAssetKey    : string
pressedAssetKey  : string
disabledAssetKey : string
fontKey          : string
textColor        : ARGB
disabledTextColor: ARGB
backgroundColor  : ARGB
borderColor      : ARGB
paddingX         : number
paddingY         : number
borderWidth      : number
radius           : number
animation        : MenuAnimation
```

Los assets pueden ser regiones y pueden llevar márgenes Nine-Slice. Si un estado no tiene asset, se usa el fallback correspondiente del botón o su estilo de color/borde.

`MenuButton` añade:

```text
uiSkinKey : string
enabled   : boolean
```

`TextStyle` añade:

```text
uiSkinKey : string
```

En un botón deshabilitado no se ejecuta la acción. El runtime usa `disabledAssetKey` y `disabledTextColor` cuando existen.

## 30.9 `Camera2D` RC

Propiedades completas de la extensión RC:

```text
enabled
target
follow
zoom
zoomSpeed
offsetX
offsetY
pixelSnap
priority
smoothSpeed
deadZoneX
deadZoneY
minX
minY
maxX
maxY
shakeIntensity
shakeDuration
```

`zoomSpeed=0` aplica zoom inmediatamente. Un valor positivo interpola exponencialmente hacia `zoom`. `minX/minY/maxX/maxY` pueden quedar vacíos; en ese caso se usan los límites de la escena. El viewport nunca se desplaza fuera de los bounds resueltos cuando el área es mayor que la vista.

Ejemplo:

```2gs
on start
  Camera2D.zoom = 2
  Camera2D.zoomSpeed = 3
  Camera2D.minX = 0
  Camera2D.maxX = 80
end
```

## 30.10 `while`

Sintaxis:

```2gs
while <ruta> <operador> <valor>
  ...
end
```

Ejemplo:

```2gs
on start
  self.x = 0
  while self.x < 5
    self.x += 1
  end
end
```

`while` admite los mismos operadores de comparación que `if`:

```text
== != > >= < <= contains startsWith endsWith
```

Puede contener `wait`; la continuación conserva el estado del loop sin bloquear el juego.

Protección: cada instancia de `while` se corta después de 10.000 iteraciones y escribe un diagnóstico. Además sigue vigente el límite global de 100.000 instrucciones por evento. No existe un `while` sin guardas capaz de congelar indefinidamente el hilo del runtime.

## 30.11 `ResourceIntegrity`

API pública para auditoría/reemplazo:

```java
List<String> assetUses(GameProject project, String assetKey)
int replaceAsset(GameProject project, String oldKey, String newKey)
List<String> brokenAssetReferences(GameProject project)
```

`replaceAsset` migra referencias de tiles, variantes, autotile, clips, escena, entidades, Prefabs, menús, TextStyle, partículas y UISkin; también reemplaza literales de asset en scripts cuando coinciden como token completo.

El Studio expone esta API en `RC 2.3 → Integridad y reemplazo de recursos`.

## 30.12 Favoritos y carpetas virtuales

`Asset` añade:

```text
favorite : boolean
folder   : string
```

Son metadatos de autoría persistentes. No duplican bytes ni cambian IDs. El Asset Browser muestra `★ Favoritos` y carpetas virtuales; una carpeta vacía devuelve el asset a su agrupación automática.

## 30.13 Persistencia RC y compatibilidad

`FORMAT_VERSION = 9`.

Los campos RC se guardan en `rc-2.3.properties` dentro del contenedor `.2grl`. La persistencia base de 2.2 se conserva; la ausencia del sidecar produce defaults seguros. Esto permite abrir proyectos anteriores sin exigir que contengan recursos RC.

Valores RC corruptos se convierten a defaults cuando es posible: enums desconocidos, números inválidos y referencias ausentes no deben impedir que se abra el resto del proyecto.

## 30.14 Límites deliberados del lenguaje en RC

2GameScript sigue siendo una DSL de gameplay. La RC incorpora `while` acotado, pero no introduce clases ni imports definidos por usuario, reflexión Java ni SQL directo. Las submáquinas jerárquicas de Animator y un backend GPU propio permanecen fuera de este bloque porque requieren una decisión arquitectónica separada, no porque falte una opción de interfaz.
'''
if '# 30. Referencia técnica RC 2.3.0-rc1' not in s:s=s.rstrip()+append+'\n'
write(b,s)

a='docs/2GAMESCRIPT_API_REFERENCE.md'
s=read(a).replace('# 2GameScript 2.2 — Referencia API','# 2GameScript 2.3.0-rc1 — Referencia API',1).replace('Esta referencia describe la API disponible en 2gameRL Studio 2.2.0.','Esta referencia describe la API disponible en 2gameRL Studio 2.3.0-rc1.',1)
s=s.replace('''Iteración:\n\n```text\nrepeat 3\n  create chispa\nend\n```\n\n`repeat` tiene un límite de 10.000 iteraciones por bloque. `stop` y `return` terminan el evento actual.''','''Iteración:\n\n```text\nrepeat 3\n  create chispa\nend\n\nwhile self.x < 10\n  self.x += 1\nend\n```\n\n`repeat` y `while` tienen un límite de 10.000 iteraciones por bloque. `while` usa una ruta, operador y valor como `if`, y puede contener `wait`. `stop` y `return` terminan el evento actual.''')
s=s.replace('''Animator.controller\nAnimator.clip\nAnimator.state\nAnimator.speed''','''Animator.controller\nAnimator.animationSet\nAnimator.clip\nAnimator.state\nAnimator.direction\nAnimator.speed''')
s=s.replace('''priority:number\n```''','''priority:number\nsmoothSpeed:number\ndeadZoneX:number\ndeadZoneY:number\nzoomSpeed:number\nminX:number|string vacío\nminY:number|string vacío\nmaxX:number|string vacío\nmaxY:number|string vacío\nshakeIntensity:number\nshakeDuration:number\n```''',1)
s=s.replace('''## Límites actuales del lenguaje\n\n2GameScript 2.2 no pretende ser Java/Lua de propósito general. No incluye `while` arbitrario, funciones/clases definidas por usuario, imports ni SQL directo. El objetivo es ofrecer una DSL de gameplay legible, con rutas de componentes, eventos, señales, temporización, Input Map, Prefabs y animación sin exponer la implementación interna del motor.''','''## Límites actuales del lenguaje\n\n2GameScript 2.3.0-rc1 no pretende ser Java/Lua de propósito general. Incluye `while` acotado a 10.000 iteraciones, pero no clases/imports definidos por usuario, reflexión Java ni SQL directo. El objetivo sigue siendo una DSL de gameplay legible y segura.\n\n## API RC añadida\n\n- `Animator.param.<nombre>`: lectura/escritura de parámetros BOOL/INT/FLOAT/TRIGGER.\n- Blend trees 1D/2D discretos por clip más cercano.\n- `AnimationSet`: 2/4/8 direcciones con fallback `DEFAULT` y flipX opcional.\n- `FrameShape`: HITBOX/HURTBOX por frame; señales `on event hitbox` y `on event hurtbox`.\n- `Camera2D.zoomSpeed`, bounds `minX/minY/maxX/maxY`.\n- `UISkinAsset`: normal/hover/pressed/disabled.\n- Autotile 4/8 vías, variantes ponderadas y StampPattern.\n\nLa especificación completa de campos, métodos, máscaras, coerciones, límites y ejemplos está en la Biblia técnica, sección 30.''')
write(a,s)

audit=r'''# Auditoría de Release Candidate — 2gameRL Studio 2.3.0-rc1

## Alcance

Auditoría agresiva realizada contra `docs/VISUAL_PIPELINE_2.2.0.md`, la Biblia técnica, el runtime, persistencia y herramientas de autoría. La RC parte de la 2.2.2 validada y conserva JavaFX como backend, tal como exige la especificación.

## Matriz de cierre

| Requisito | Estado RC | Implementación | Validación prevista |
|---|---|---|---|
| Autotile 4-way/8-way | Implementado | `TileDef`, `AdvancedTileResolver`, Tileset Lab | máscaras, fallback 8→4, gran grilla |
| Variantes aleatorias ponderadas | Implementado | `TileVariant`, resolver determinista | distribución/estabilidad por celda |
| Stamp/patrón | Implementado | `StampPattern`, Stamp Lab, herramienta de escena | persistencia + colocación |
| Animation reverse/FPS | Implementado | `AnimationClip.reverse/fps` | runtime smoke |
| Preview animación | Implementado | Animation Lab | arranque Studio + unit/runtime |
| Onion skin | Implementado | Animation Lab | autoría, sin efecto runtime |
| Pivot preview | Implementado | Animation Lab | overlay visual |
| Hitbox/Hurtbox por frame | Implementado | `FrameShape`, runtime de solapes | daño único/frame + eventos |
| Animator parameters | Implementado | BOOL/INT/FLOAT/TRIGGER | transición + trigger |
| Blend tree 1D/2D | Implementado | nearest-child determinista | runtime smoke |
| Animation Sets 2/4/8 | Implementado | `AnimationSet` | dirección + flip fallback |
| UISkin reutilizable | Implementado | `UISkinAsset`, UI Skin Lab | persistencia + runtime |
| Estados UI pressed/disabled | Implementado | UISkin + MenuButton.enabled | runtime/menu |
| Nine-slice | Ya existía en 2.2.2 | Asset/TextOverlay | regresión 2.2.2 |
| Favoritos/carpetas virtuales | Implementado | Asset metadata + Browser | persistencia |
| Dónde se usa | Ya existía; ampliado | `ResourceIntegrity` | cobertura de RC resources |
| Reemplazo seguro | Implementado | `ResourceIntegrity.replaceAsset` | test transversal |
| Reimportación | Ya existía | GraphicsEditorPane | regresión existente |
| Camera bounds | Implementado | Camera2D min/max | runtime smoke |
| Zoom animado | Implementado | Camera2D.zoomSpeed | runtime smoke |
| Dead zone/smoothing/shake | Ya existía en 2.2.2 | Camera2D | regresión 2.2.2 |
| `while` | Implementado acotado | ScriptProgram | stop condition/wait/runaway |
| Partículas | Cerrado en 2.2.2 | hotfix publicado | batería 2.2.2 obligatoria |
| Audio | Cerrado en 2.2.2 | AudioManager | regresión existente |
| Backend GPU | Fuera del RC | especificación lo separa | no aplica |
| Submáquinas Animator | Fuera del RC | la especificación las marca futuras | no aplica |

## Gate de RC

La rama no se considera candidata publicable hasta superar, en Windows:

1. `mvn clean test package` con tests existentes + RC.
2. Smoke determinista de timers/colisiones.
3. Pipeline visual 2.2.
4. Smoke agresivo 2.2.2 de partículas/cámara/prefab.
5. Smoke 2.3 RC de Animator, direcciones, reverse/FPS, hitbox/hurtbox y cámara.
6. 100 round-trips aleatorios de proyectos RC.
7. sidecar RC corrupto/ausente.
8. grilla autotile 256×256 resuelta repetidamente de forma determinista.
9. runtime general, componentes y ScenePortal.
10. empaquetado de Studio Windows.
11. arranque real del Studio empaquetado.
12. exportación con toolchain embebida.
13. arranque real del juego exportado.
14. auditoría de referencias rotas.

Cualquier fallo bloquea el merge/release y debe corregirse en la rama RC antes de repetir el gate.
'''
write('docs/RC_AUDIT_2.3.0-rc1.md',audit)

print('RC phase E technical docs staged')
