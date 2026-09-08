# Tutorial de scripting de 2gameRL

2gameRL usa **2GameScript**, un lenguaje pequeño y orientado a eventos. Cada objeto de una escena puede tener su propio script.

## Dónde se edita

1. Abre **Escena**.
2. Selecciona un objeto en la jerarquía o directamente en el lienzo.
3. Haz doble clic sobre el objeto, o abre la pestaña **Script** del inspector.
4. Escribe el script y usa **Validar script** antes de probar.
5. Pulsa **Probar** para ejecutar el proyecto sin exportarlo.

El tutorial interactivo también está disponible dentro de Studio desde **Ayuda > Tutorial de scripting**, con **F1**, y desde el botón **Tutorial** del editor de scripts. Los ejemplos del tutorial pueden insertarse directamente en el script del objeto seleccionado.

## Estructura básica

Todo comando ejecutable debe estar dentro de un bloque `on ... end`:

```text
# Esto es un comentario
on start
  log "Objeto iniciado"
end

on update
  ifKey W move 0 -0.05
end
```

- `#` inicia un comentario, salvo que esté dentro de comillas.
- Usa comillas para textos con espacios.
- Los nombres de comandos no distinguen mayúsculas/minúsculas.
- Un bloque `on` siempre debe cerrarse con `end`.

## Eventos

| Evento | Cuándo se ejecuta |
| --- | --- |
| `start` | Una vez al cargar la escena y crear el objeto. |
| `update` | Cada frame mientras el objeto está activo. |
| `click` | Al hacer clic sobre el objeto. |
| `doubleClick` | Al hacer doble clic sobre el objeto. |
| `collision` | Al comenzar una colisión con otro collider o al chocar contra un tile no transitable. |
| `trigger` | En una entidad con `Trigger`, al entrar en contacto con un objeto que tenga `PlayerController`. |

Ejemplo:

```text
on start
  log "Listo"
end

on click
  log "Me hicieron clic"
end

on collision
  log "Choque"
end
```

## Movimiento

### `move <x> <y>`

Desplaza inmediatamente el objeto en unidades del mundo.

```text
move 1 0
move 0 -0.25
```

### `velocity <x> <y>`

Define la velocidad continua del objeto.

```text
velocity 3 0
```

### `teleport <x> <y>`

Coloca el objeto directamente en una posición.

```text
teleport 10 6
```

### `bounce`

Invierte la velocidad actual. Está pensado para responder a `collision`.

```text
on collision
  bounce
end
```

## Teclado

### `ifKey <tecla> <comando>`

Ejecuta el comando mientras la tecla permanezca presionada.

```text
on update
  ifKey W move 0 -0.05
  ifKey S move 0 0.05
  ifKey A move -0.05 0
  ifKey D move 0.05 0
end
```

### `ifPressed <tecla> <comando>`

Ejecuta el comando una sola vez cuando la tecla comienza a presionarse.

```text
on update
  ifPressed SPACE log "Acción"
end
```

Nombres habituales: `W`, `A`, `S`, `D`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `SPACE`, `ENTER`, `SHIFT`, `ESCAPE`.

## Variables

### `setVar <nombre> <valor>`

Guarda un valor en el objeto.

```text
setVar monedas 0
setVar estado despierto
```

### `addVar <nombre> <número>`

Suma un número a una variable. Si la variable no existe o no es numérica, parte desde 0.

```text
addVar monedas 1
addVar vida -10
```

2GameScript todavía no incorpora comparadores generales ni bloques `if/else` basados en variables.

## Propiedades del objeto

`set <propiedad> <valor>` cambia propiedades del objeto en ejecución.

Propiedades directas soportadas:

- `x`
- `y`
- `width`
- `height`
- `enabled`
- `layer`

```text
set x 12
set y 4
set enabled false
set layer 5
```

Cualquier otro nombre pasado a `set` se guarda como variable del objeto.

## Escenas y gráficos

### `loadScene <id>`

Carga otra escena usando su ID.

```text
on trigger
  loadScene nivel-2
end
```

### `setSprite <asset>`

Cambia el sprite del objeto por la clave exacta de un asset existente.

```text
on click
  setSprite puerta-abierta.png
end
```

## Utilidad y ciclo de vida

### `log <texto>`

Escribe en la consola de prueba.

```text
log "Entró al trigger"
```

### `destroy`

Elimina el objeto durante la ejecución actual.

```text
on trigger
  log "Objeto recogido"
  destroy
end
```

## Componentes integrados

Los componentes se añaden desde **Inspector > Comportamientos** y se ejecutan sin necesidad de script. Cada componente tiene una propiedad `enabled`; con `false` se desactiva solo ese comportamiento.

### `Rigidbody2D`

Aplica física de movimiento.

- `enabled`: activa/desactiva el componente.
- `gravityScale`: gravedad aplicada. En entidades nuevas parte en `1`.
- `drag`: frenado progresivo.
- `maxSpeed`: límite de velocidad.
- `mass`: queda disponible para evolución futura del sistema de fuerzas.

El jugador del proyecto por defecto usa `gravityScale=0` porque está configurado como jugador top-down.

### `BoxCollider2D`

Colisión rectangular real.

- `width`: ancho del collider en unidades del mundo.
- `height`: alto del collider.
- `solid`: si bloquea movimiento.

Un collider sólido bloquea contra tiles no transitables y contra otros `BoxCollider2D` sólidos.

### `PlayerController`

Convierte el objeto en una entidad controlable inmediatamente.

- `speed`: velocidad.
- `allowArrows`: permite flechas además de WASD.

No es necesario escribir un script de movimiento para un jugador estándar.

### `Patrol`

Mueve automáticamente la entidad desde su posición inicial hasta la distancia indicada y luego vuelve.

- `axis`: `x` o `y`.
- `distance`: distancia desde el punto inicial.
- `speed`: velocidad.

Si la entidad tiene `BoxCollider2D` y choca, invierte la dirección.

### `ScenePortal`

Cambia de escena al entrar en contacto con una entidad que tenga `PlayerController`.

- `targetScene`: ID de la escena destino.
- `targetX`: posición X del jugador al entrar.
- `targetY`: posición Y del jugador al entrar.

### `Trigger`

Dispara el evento de script `trigger` cuando entra un objeto con `PlayerController`.

- `tag`: etiqueta disponible para identificar el trigger.
- `once`: con `true`, se consume después de la primera activación durante esa instancia de la escena.

### `Health`

Vida de la entidad.

- `max`: vida máxima.
- `current`: vida actual.

El runtime mantiene `current` entre `0` y `max`.

### `DamageOnContact`

Aplica daño al comenzar contacto con una entidad que tenga `Health`.

- `damage`: cantidad de vida restada.

Si `current` llega a 0, la entidad objetivo se destruye durante la ejecución actual.

### `Clickable`

Controla la interacción del ratón.

- `enabled=true`: permite `click` y `doubleClick`.
- `enabled=false`: bloquea esos eventos.

Para compatibilidad con proyectos anteriores, una entidad sin componente `Clickable` sigue siendo clickeable.

## Recetas

### Jugador top-down

Añade:

- `PlayerController`
- `BoxCollider2D`
- `Rigidbody2D` con `gravityScale=0`

No necesitas script para el movimiento básico.

### Objeto con gravedad

Añade:

- `Rigidbody2D`
- `BoxCollider2D`

Con los valores por defecto caerá y chocará contra tiles o colliders sólidos.

### Enemigo de patrulla

Añade:

- `Patrol`
- `BoxCollider2D`

Ajusta `axis`, `distance` y `speed`.

### Enemigo que hace daño

Añade:

- `Patrol`
- `BoxCollider2D`
- `DamageOnContact`

El objetivo debe tener `Health`.

### Objeto recogible

Añade `Trigger` y usa:

```text
on trigger
  log "Objeto recogido"
  destroy
end
```

### Portal sin script

Añade `ScenePortal` y define:

```text
targetScene = nivel-2
targetX = 3
targetY = 5
```

### Proyectil sencillo

```text
on start
  velocity 6 0
end

on collision
  destroy
end
```

## Referencia de comandos

```text
log <texto>
move <x> <y>
velocity <x> <y>
teleport <x> <y>
bounce
destroy
loadScene <id>
setSprite <asset>
setVar <nombre> <valor>
addVar <nombre> <número>
set <propiedad> <valor>
ifKey <tecla> <comando>
ifPressed <tecla> <comando>
```

## Errores frecuentes

- Escribir un comando fuera de `on ... end`.
- Olvidar `end`.
- Usar un evento que no existe.
- Pasar texto donde se espera un número.
- Usar el nombre visible de una escena cuando `loadScene` necesita su ID.
- Escribir una clave de sprite inexistente.
- Esperar que `ifKey` se ejecute una sola vez: para eso está `ifPressed`.
- Añadir `Rigidbody2D` al jugador top-down y olvidar poner `gravityScale=0`.
- Esperar que `DamageOnContact` haga algo si el objetivo no tiene `Health`.

El botón **Validar script** usa el mismo parser que ejecuta el runtime.
