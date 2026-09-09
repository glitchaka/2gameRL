# 2gameRL Studio 2.2.0 — Visual Asset Pipeline

Esta especificación define la siguiente gran actualización gráfica del motor. El objetivo es unificar imágenes, sprites, tilemaps, prefabs, animaciones, UI, texto y render pixel-perfect bajo un flujo coherente, sin sacrificar rendimiento ni compatibilidad con proyectos 2.1.0.

## 1. Principios

1. Un spritesheet se almacena una sola vez. Las divisiones son regiones virtuales.
2. El usuario trabaja con recursos visuales y pinceles/prefabs; el motor decide si una colocación se guarda como celda de tilemap o como entidad real.
3. Los tilemaps siguen existiendo como estructura optimizada para miles de celdas, pero el antiguo panel `Gráficos > Tiles` deja de ser el flujo principal de autoría.
4. Una entidad o prefab puede tener SpriteRenderer, Animator, física, scripts y otros componentes.
5. El render pixel-art usa nearest-neighbor y escalado entero; la resolución física del monitor no cambia la resolución lógica del juego.
6. Las operaciones de recursos respetan `Ctrl+C`, `Ctrl+V`, `Supr` y selección múltiple.
7. Los cambios visuales no rompen scripts/proyectos 2.1.0 sin una ruta de migración.
8. El runtime no decide la apariencia de un juego mediante valores visuales hardcodeados: colores, tipografías, tamaños, bordes, opacidades, fades y skins son datos editables del proyecto. Los únicos valores internos permitidos son fallbacks técnicos para proyectos antiguos/corruptos.

## 2. Recursos visuales

### 2.1 ImageAsset

Imagen fuente importada.

Propiedades previstas:
- `key`
- nombre visible
- bytes/fuente
- ancho/alto
- filtro `PIXEL | LINEAR`
- categoría `SPRITE | BACKGROUND | UI | TILESET | VFX | PORTRAIT`
- alpha/transparencia
- metadatos de importación/reimportación

### 2.2 SpriteSheetAsset

Referencia una sola ImageAsset y conserva:
- modo de slicing `GRID | CONTOUR | MANUAL`
- tamaño de celda, margen, separación
- tolerancia de alpha/color para contorno
- regiones virtuales
- metadatos de reimportación

### 2.3 SpriteRegion

Región virtual de una fuente:
- `sourceAssetKey`
- `x`, `y`, `width`, `height`
- pivot/origin
- tags
- categoría
- trim opcional
- puntos de anclaje opcionales

No genera PNG nuevos.

### 2.4 FontAsset

TTF/OTF embebida con nombre estable, preview y uso desde UI/texto.

### 2.5 UISkinAsset

Skin reutilizable para botones, paneles, bocadillos y bandas de novela visual:
- normal/hover/pressed/disabled
- nine-slice
- padding
- bordes
- colores
- fuente
- animaciones de estado

## 3. Asset Browser

El panel Gráficos evoluciona a un Asset Browser con:
- Imágenes
- Spritesheets
- Regiones
- Tilesets
- Animaciones
- Animator Controllers
- Prefabs/Pinceles
- UI
- Fuentes
- VFX

Funciones:
- vista lista/grilla
- thumbnails
- carpetas virtuales
- búsqueda
- filtros por tipo/tag
- favoritos
- copiar/pegar/duplicar/eliminar
- renombrado seguro
- `Dónde se usa`
- dependencias
- reemplazo seguro

## 4. Tilemap + Prefab/Pincel unificado

### 4.1 Problema actual

2.1.0 obliga a definir `TileDef` en Gráficos y luego seleccionar un ID numérico en Escena. Las propiedades físicas del tile quedan separadas del inspector de la capa y los tiles no tienen el mismo modelo de componentes que las entidades.

### 4.2 Nuevo recurso Brush/Prefab

Un recurso colocable puede declarar:

```text
Nombre: SueloPasto
Visual: grass_01
Modo de colocación: AUTO | TILEMAP | ENTITY
```

Puede contener propiedades compartidas de superficie:
- sólido
- physicsLayer
- one-way
- fricción
- daño por contacto
- tags
- animación de tile

Y, cuando el modo es `ENTITY`, componentes completos:
- SpriteRenderer
- Animator
- GridMovement
- PlayerController
- Rigidbody2D
- BoxCollider2D
- Trigger
- Patrol
- ScenePortal
- Health
- DamageOnContact
- Clickable
- Script

### 4.3 AUTO

`AUTO` decide internamente:
- recurso estático y compartido -> tilemap
- requiere estado individual/componentes/scripts -> entidad

El usuario usa el mismo gesto de `Dibujar` en ambos casos.

### 4.4 Herramientas de escena

La barra cambia de `Tile: 0 · Suelo` a:

```text
Pincel: [ SueloPasto ▼ ]
```

Herramientas:
- Seleccionar
- Dibujar
- Rellenar
- Borrar
- Rectángulo
- Línea
- Cuentagotas
- Stamp/patrón
- Objeto/Prefab

### 4.5 Tilemap eficiente

Una capa de 200×100 usa una matriz de IDs/celdas, no 20.000 entidades.

Los comportamientos compartidos de superficie (sólido, hielo, lava, one-way, daño, animación) se resuelven por definición. Una celda que necesita estado propio se convierte en entidad/prefab.

## 5. Tilesets avanzados

Un Tileset agrupa regiones y reglas:
- tamaño base
- categorías
- colisión
- one-way
- daño
- superficie
- variantes aleatorias con peso
- autotile 4-way/8-way
- esquinas/bordes/interiores
- tiles animados
- reglas por vecinos

## 6. AnimationClip

Clip por frames con:
- lista ordenada de SpriteRegion
- duración individual por frame
- FPS global opcional
- loop
- ping-pong
- reverse
- speed multiplier
- frame inicial aleatorio opcional
- eventos/markers por frame
- preview

Markers típicos:
- `footstep`
- `attack_start`
- `attack_hit`
- `spawn_projectile`
- `land`

## 7. Editor de animación

Editor dedicado con:
- timeline
- drag/drop de regiones
- selección múltiple
- reordenamiento
- duración por frame
- FPS
- loop/ping-pong
- preview en tiempo real
- onion skin opcional
- pivot
- collider/hitbox preview
- eventos por frame
- crear clip desde selección o patrón de nombres

## 8. Animator

Nuevo componente `Animator`:

```text
Animator.enabled
Animator.controller
Animator.state
Animator.speed
Animator.flipX
Animator.flipY
Animator.frame
Animator.finished
```

Debe permitir también control directo de clips sin Controller.

## 9. AnimatorController

Máquina de estados inspirada en las mejores partes de Unity/Godot/GameMaker:
- estado por defecto
- estados
- Any State
- transiciones
- parámetros bool/int/float/trigger
- condiciones
- exit time opcional
- prioridad
- submáquinas futuras
- blend tree 1D/2D básico planificado

Ejemplo:

```text
Idle -> Walk    speed > 0.1
Walk -> Idle    speed <= 0.1
Any -> Hurt     trigger hurt
Jump -> Fall    vy >= 0
Fall -> Idle    grounded == true
```

## 10. Animation Sets direccionales

Soporte para:
- 2 direcciones
- 4 direcciones
- 8 direcciones

Puede mapear estados como `Walk` a `Walk_Left`, `Walk_Right`, etc. También puede usar `flipX` para evitar duplicación cuando el arte lo permita.

## 11. Integración con 2GameScript

Sintaxis objetivo:

```text
Animator.state = "Walk"
Animator.speed = 1.25
Animator.flipX = true
```

Comandos de conveniencia previstos:

```text
playAnimation "Walk"
queueAnimation "Attack"
stopAnimation
```

Eventos:

```text
on animationStart
on animationEnd
on animationLoop
on animationEvent footstep
```

Lecturas:

```text
if Animator.state == "Walk"
if Animator.finished == true
if Animator.frame == 3
```

### 11.1 Estado físico para scripting de plataformas

2.2.0 debe exponer estado de contacto calculado por el motor para que el usuario pueda programar saltos reales sin variables manuales ni saltos infinitos:

```text
Rigidbody2D.grounded
Rigidbody2D.touchingLeft
Rigidbody2D.touchingRight
Rigidbody2D.touchingTop
```

Son propiedades de solo lectura.

Ejemplo canónico de plataformas:

```text
on update
  self.vx = 0

  ifKey A
    self.vx = -5
  end
  ifKey D
    self.vx = 5
  end

  ifPressed SPACE
    if Rigidbody2D.grounded == true
      self.vy = -8
    end
  end
end
```

Debe documentarse claramente que el eje Y no se deshabilita: la gravedad modifica `vy` y el salto asigna un impulso negativo una vez.

## 12. TextStyle y colores de texto

En 2.1.0 los colores de FREE/BUBBLE/NOVEL están hardcodeados. 2.2.0 introduce `TextStyle` reutilizable y elimina esa decisión del renderer.

Propiedades:
- `textColor`
- `speakerColor`
- `backgroundColor`
- `borderColor`
- `shadowColor`
- `fontKey`
- `fontSize`
- `fontWeight`
- `outlineWidth`
- `outlineColor`
- `shadow`
- `maxWidth`
- `alignment`
- `padding`
- `fadeIn`
- `fadeOut`
- skin de bocadillo/panel

Uso previsto:

```text
showText free screen "NIVEL 1" 3 style title
showText bubble self "¡Eh!" 3 style dialogo
showText novel Diego "No deberíamos estar aquí." click style novela
```

Override puntual previsto:

```text
showText free screen "PELIGRO" 2 color "#FF4040"
```

El editor ofrecerá selector de color y preview; no será necesario escribir hexadecimal para el uso normal.

Los estilos predeterminados se serializan dentro del proyecto. No deben existir colores/tamaños obligatorios en `TextOverlayLayer`; solo un fallback técnico para migración de proyectos antiguos.

## 13. Sistema de texto

FREE:
- anclaje pantalla/entidad
- fade obligatorio
- color/fuente/outline/sombra
- títulos, avisos, daño flotante

BUBBLE:
- seguimiento de entidad
- máximo 10 s
- fade obligatorio
- cola/puntero
- skins normal/pensamiento/grito/susurro
- recolocación automática en bordes

NOVEL:
- panel anclado a pantalla
- speaker/retrato
- tipografía y colores independientes
- typewriter opcional
- click/key/time
- cola de diálogo
- auto/skip e historial previstos

## 14. Pixel-Perfect Renderer

Configuración de proyecto:
- resolución lógica interna
- filtro nearest/linear
- integer scaling
- mantener aspecto
- letterbox
- pixel snap
- escalado UI independiente

Presets sugeridos:
- 256×144
- 320×180
- 384×216
- 426×240
- 640×360
- personalizada

## 15. Probar juego y juego exportado

`Probar` debe abrir por defecto en fullscreen/borderless usando la resolución lógica y el mayor factor entero posible.

Ejemplo:
- juego 320×180
- monitor 1920×1080
- escala 6× exacta

En 1366×768:
- 320×180 -> 1280×720 a 4×
- centrado con letterbox
- nunca blur ni estiramiento no entero en modo pixel-perfect

Controles:
- `F11`: fullscreen/ventana
- `Esc`: salir del fullscreen o cerrar prueba según configuración

Exportado:
- ventana
- fullscreen
- fullscreen borderless

## 16. Camera2D

Nuevo sistema de cámara:

```text
Camera2D.target
Camera2D.follow
Camera2D.zoom
Camera2D.offsetX
Camera2D.offsetY
Camera2D.pixelSnap
```

Previstos:
- límites
- dead zone
- smoothing opcional
- shake
- zoom animado
- parallax

## 17. Fondos

El fondo de escena es independiente del tilemap:
- color
- stretch
- cover
- contain
- tile
- parallax

Puede usar PIXEL o LINEAR según asset.

## 18. UI

UI debe compartir el mismo pipeline visual:
- fuentes personalizadas
- UISkin
- nine-slice
- estados de botón
- iconos
- animaciones hover/pressed
- paneles
- imágenes
- clips de UI
- TextStyle

## 19. Reimportación y dependencias

Al modificar una fuente:
- detectar cambios
- reimportar
- conservar regiones/nombres cuando sea posible
- marcar regiones inválidas
- no romper clips/tilesets sin aviso

Eliminar un asset muestra dependencias y permite:
- cancelar
- reemplazar
- cascada explícita
- dejar referencia faltante visible

## 20. Compatibilidad 2.1.0

Migración prevista:
- `Asset` existente -> ImageAsset/SpriteRegion equivalente
- `TileDef` -> definición interna de tile/Brush
- `TileLayer.cells` se conserva y se migra sin convertir celdas en entidades
- scripts `self.sprite`, `setSprite`, etc. siguen funcionando
- fuentes importadas siguen siendo válidas
- FREE/BUBBLE/NOVEL existentes mantienen apariencia mediante un estilo de migración generado, no mediante constantes permanentes del renderer

## 21. Render backend

2.2.0 debe mantener JavaFX para este bloque. OpenGL/LWJGL no se introduce solo para alpha, fades o nearest-neighbor. Un backend GPU propio se evaluará como una etapa separada cuando se necesiten shaders, iluminación 2D, partículas GPU o postprocesado.

## 22. Tutorial y referencia integrada de 2GameScript

El botón `Tutorial` deja de ser una colección corta de ejemplos. Debe ser una referencia navegable del lenguaje con buscador y filtros.

Debe documentar, uno por uno:
- cada evento
- cada comando
- cada operador de asignación/comparación
- cada ruta (`self`, `other`, `global`, `save`, `scene`, `layer`)
- cada componente/clase integrada
- cada propiedad pública de esos componentes
- cada comando legacy aún compatible
- interpolación
- limitaciones reales de la versión

Cada ficha contiene:
- definición
- firma/sintaxis
- tipos/propiedades
- ejemplo ejecutable
- notas y restricciones

Debe incluir recetas completas, no fragmentos inconexos:
- WASD libre
- WASD con Rigidbody2D
- salto de plataformas con `grounded`
- GridMovement
- colisiones
- trigger
- daño/Health
- cambio de escena
- señales
- persistencia SQLite
- FREE/BUBBLE/NOVEL
- animaciones/Animator cuando estén implementadas
- scripts de escena y capa

El tutorial integrado debe distinguir explícitamente una API existente de una API planificada; nunca debe mostrar una función futura como si ya funcionara.

## 23. Criterio de versión

Esta actualización se considera 2.2.0 porque amplía de forma importante el pipeline gráfico y de autoría sin requerir un cambio incompatible de API mayor.
