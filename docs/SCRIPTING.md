# Tutorial de scripting de 2gameRL

2gameRL usa **2GameScript**, un lenguaje pequeño y orientado a eventos. Cada objeto de una escena puede tener su propio script.

## Dónde se edita

1. Abre **Escena**.
2. Selecciona un objeto en la jerarquía o directamente en el lienzo.
3. Haz doble clic sobre el objeto, o abre la pestaña **Script** del inspector.
4. Escribe el script y usa **Validar script** antes de probar.
5. Pulsa **Probar** para ejecutar el proyecto sin exportarlo.

El tutorial interactivo también está disponible dentro de Studio desde **Ayuda > Tutorial de scripting** y desde el botón **Tutorial** del editor de scripts.

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
| `update` | Cada frame mientras el juego está ejecutándose y el objeto está activo. |
| `click` | Al hacer un clic sobre el objeto. |
| `doubleClick` | Al hacer doble clic sobre el objeto. |
| `collision` | Cuando el objeto colisiona con otra entidad o un movimiento queda bloqueado por colisión. |
| `trigger` | En una entidad con componente `Trigger`, al solaparse con un objeto que tenga `PlayerController`. |

Ejemplo:

```text
on start
  log "Listo"
end

on click
  log "Me hicieron clic"
end

on collision
  bounce
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

Invierte las velocidades X e Y actuales.

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
  ifPressed SPACE velocity 0 -6
end
```

Nombres habituales de teclas: `W`, `A`, `S`, `D`, `UP`, `DOWN`, `LEFT`, `RIGHT`, `SPACE`, `ENTER`, `SHIFT`, `ESCAPE`.

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

En la versión actual las variables sirven como estado interno y para operaciones de `addVar`; 2GameScript todavía no incorpora comparadores generales, interpolación de variables ni bloques `if/else` basados en variables.

## Propiedades del objeto

`set <propiedad> <valor>` cambia propiedades del objeto en ejecución.

Propiedades directas soportadas:

- `x`
- `y`
- `width`
- `height`
- `enabled`
- `layer`

Ejemplos:

```text
set x 12
set y 4
set enabled false
set layer 5
```

Cualquier otro nombre pasado a `set` se guarda como variable del objeto.

## Escenas y gráficos

### `loadScene <id>`

Solicita cargar otra escena.

```text
on trigger
  loadScene nivel-2
end
```

Usa el **ID** de la escena, no necesariamente el texto visible de su nombre.

### `setSprite <asset>`

Cambia el sprite del objeto por la clave de un asset existente.

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

Destruye el objeto durante la ejecución actual.

```text
on trigger
  log "Objeto recogido"
  destroy
end
```

## Componentes integrados

Los componentes se añaden desde **Inspector > Comportamientos**. No reemplazan el script: aportan comportamiento listo para usar y el script permite complementarlo.

### `Rigidbody2D`

- `gravityScale`: cantidad de gravedad aplicada.
- `drag`: frenado progresivo cuando no controla la velocidad otro comportamiento.
- `maxSpeed`: límite de velocidad.
- `mass`: se conserva como propiedad del componente; la simulación actual no la usa todavía para calcular fuerzas o impulsos.

### `BoxCollider2D`

- `width`: ancho del collider en unidades del mundo.
- `height`: alto del collider.
- `solid`: si bloquea movimiento cuando corresponde.

### `PlayerController`

- `speed`: velocidad del jugador.
- `allowArrows`: permite usar flechas además de WASD.

Añadirlo es la forma más rápida de convertir un objeto en jugador controlable.

### `Patrol`

- `axis`: `x` o `y`.
- `distance`: distancia del recorrido.
- `speed`: velocidad.

### `ScenePortal`

- `targetScene`: escena que se carga cuando el jugador toca el portal.
- `targetX` y `targetY`: se almacenan en el componente, pero la versión actual todavía no reposiciona automáticamente al jugador con esos dos valores después de cargar la escena.

### `Trigger`

Hace que el objeto pueda recibir el evento `trigger` cuando se solapa con un objeto con `PlayerController`.

- `tag` y `once` se almacenan como propiedades; en la versión actual el evento se dispara por solapamiento y todavía no consume automáticamente el trigger cuando `once=true`.

### `Health`

- `max`: vida máxima.
- `current`: vida actual.

### `DamageOnContact`

- `damage`: daño aplicado al tocar otra entidad que tenga `Health`.

### `Clickable`

El componente está disponible en el inspector. En la versión actual el runtime ya entrega `click`/`doubleClick` a cualquier entidad activa bajo el cursor, incluso si no tiene `Clickable`; por tanto este componente funciona por ahora como marcador de intención.

## Recetas

### Jugador controlado solo con script

```text
on start
  log "Jugador listo"
end

on update
  ifKey W move 0 -0.06
  ifKey S move 0 0.06
  ifKey A move -0.06 0
  ifKey D move 0.06 0
end
```

Para movimiento estándar no hace falta escribir esto: añade `PlayerController` y ajusta `speed`.

### Objeto recogible

Añade el componente `Trigger` al objeto:

```text
on trigger
  log "Objeto recogido"
  addVar recogidos 1
  destroy
end
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

### Cambiar de escena con doble clic

```text
on doubleClick
  loadScene nivel-2
end
```

### Objeto que rebota

```text
on start
  velocity 3 2
end

on collision
  bounce
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
- Escribir una clave de sprite que no coincide con el asset importado.
- Esperar que `ifKey` se ejecute una sola vez: para eso está `ifPressed`.

El botón **Validar script** muestra los errores de sintaxis con su número de línea antes de ejecutar el proyecto.
