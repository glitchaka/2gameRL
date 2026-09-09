package com.buttclapdev.twogamerl.studio;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.*;
import java.util.function.Consumer;

/** Tutorial y referencia integrada de 2GameScript. */
final class ScriptTutorialDialog {
    private record Topic(String group,String title,String definition,String syntax,String example,String notes){
        @Override public String toString(){return title;}
        String searchable(){return (group+" "+title+" "+definition+" "+syntax+" "+example+" "+notes).toLowerCase(Locale.ROOT);}
    }

    private static Topic topic(String group,String title,String definition,String syntax,String example,String notes){
        return new Topic(group,title,definition,syntax,example,notes);
    }

    private static final List<Topic>TOPICS=List.of(
        topic("GUÍAS","00 · Cómo funciona 2GameScript","2GameScript es el lenguaje propio de 2gameRL. Un script pertenece a una entidad, una capa o una escena y siempre ejecuta instrucciones dentro de eventos.","on <evento>\n  <instrucciones>\nend","""
on start
  log "El script comenzó"
end

on update
  # se ejecuta cada frame
end
""","No es Java, JavaScript, Lua ni Python. Los comandos, rutas y componentes documentados aquí son la API del motor."),
        topic("GUÍAS","01 · WASD libre","Movimiento continuo mediante acciones del Input Map. El script describe intención de gameplay y no queda atado a un teclado concreto.","ifAction <acción> ... end\nself.vx = <velocidad>\nself.vy = <velocidad>","""
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
""","Configura MoveLeft/MoveRight/MoveUp/MoveDown en Configuración > Input Map. ifKey sigue disponible, pero las acciones son la API recomendada para gameplay nuevo."),
        topic("GUÍAS","02 · Plataformas: A/D + gravedad + salto","En un plataformas el jugador controla X, Rigidbody2D aplica gravedad y grounded evita saltos en el aire. También puedes añadir PlatformerController y configurar estas acciones sin escribir el movimiento a mano.","Rigidbody2D.grounded:boolean # solo lectura\nifActionPressed Jump ... end","""
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
""","La entidad necesita normalmente Rigidbody2D y BoxCollider2D. Rigidbody2D.grounded, touchingLeft, touchingRight y touchingTop son sensores de solo lectura calculados por el runtime."),
        topic("GUÍAS","03 · Movimiento por cuadrícula","GridMovement implementa movimiento discreto por casillas y usa las acciones MoveLeft/MoveRight/MoveUp/MoveDown del Input Map.","GridMovement.<propiedad> = <valor>","""
on start
  GridMovement.enabled = true
  GridMovement.step = 1
  GridMovement.allowDiagonal = false
  GridMovement.repeatDelay = 0.16
  GridMovement.moveDuration = 0.10
  GridMovement.snap = true
end
""","No necesitas escribir WASD manualmente. Cambia los bindings en Configuración > Input Map. BoxCollider2D y las capas físicas resuelven el bloqueo."),
        topic("GUÍAS","04 · Clic que invierte gravedad","Ejemplo mínimo de asignación matemática sobre un componente.","Rigidbody2D.gravityScale *= -1","""
on start
  Rigidbody2D.gravityScale = 1
end

on click
  Rigidbody2D.gravityScale *= -1
end
""","*= -1 cambia 1 a -1 y -1 a 1."),
        topic("GUÍAS","05 · Colisión, daño y other","Dentro de collision/trigger, other representa la otra entidad participante.","on collision ... end\ndamage <entidad> <cantidad>","""
on collision
  damage other 10
  showText free other "-10" 1
end
""","damage expresa intención de gameplay. También existe other.Health.current -= 10 si quieres editar la propiedad directamente."),
        topic("GUÍAS","06 · Variables globales y guardado","global.* vive durante la ejecución actual. save.* persiste entre ejecuciones mediante SQLite.","global.<nombre> = <valor>\nsave.<nombre> = <valor>","""
on start
  global.score = 0
  save.nombre = "Adarvio"
end

on event moneda
  global.score += 10
  save.monedas += 1
end
""","El creador del juego no escribe SQL; save.* es la API pública de persistencia."),
        topic("GUÍAS","07 · Señales entre scripts","emit permite comunicar entidad, capa y escena sin crear objetos invisibles de coordinación.","emit <nombre>\non event <nombre> ... end","""
# En una llave
on trigger
  save.llaveRoja = true
  emit llaveRojaObtenida
  destroy
end

# En una puerta
on event llaveRojaObtenida
  BoxCollider2D.enabled = false
  self.sprite = puerta_abierta
end
""","La señal se distribuye en la escena actual."),
        topic("GUÍAS","08 · Script de escena","La escena puede controlar fondo, límites, temporizadores, señales y lógica de nivel.","scene.<propiedad> = <valor>","""
on start
  timer 30 emit anochecer
end

on event anochecer
  scene.background = bosque_noche
  scene.backgroundMode = cover
  showText free screen "ANOCHECE" 4
end
""","Selecciona ESCENA > Propiedades y script."),
        topic("GUÍAS","09 · Script de capa","Una capa de tiles puede reaccionar a señales y modificar visibilidad/colisión sin crear entidades.","layer.<propiedad> = <valor>","""
on event descongelar
  layer.visible = false
  layer.collision = false
end
""","El script de capa afecta a la capa completa, no a una celda individual."),
        topic("GUÍAS","10 · Texto FREE, BUBBLE y NOVEL","Los tres modos de texto consumen TextStyle editable: fuente, tamaños, colores, fondo, borde, sombra, padding y fades pueden configurarse sin modificar Java.","showText <free|bubble|novel> ... [style nombre] [color #RRGGBB]","""
showText free screen "NIVEL 1" 3
showText free screen "PELIGRO" 2 style warning
showText bubble self "¿Qué fue eso?" 4 style guardia
showText novel Diego "No deberíamos estar aquí." click style dialogo
""","color permite un override puntual. Para estilos reutilizables crea un TextStyle en Gráficos > Texto."),
        topic("GUÍAS","11 · Temporizadores e iteraciones","wait pausa una secuencia; timer agenda una acción; every repite temporalmente; repeat repite inmediatamente.","wait <s>\ntimer <s> <comando>\nevery <s> <cantidad> <comando>\nrepeat <cantidad> ... end","""
on start
  timer 2 create explosion
  every 1 5 create humo

  repeat 3
    create chispa
  end
end
""","wait/timer/every no congelan el juego completo."),

        topic("EVENTOS","on start","Evento ejecutado al iniciar la instancia/script.","on start\n  ...\nend","""
on start
  Health.current = 100
end
""","Disponible en entidad, capa y escena."),
        topic("EVENTOS","on update","Evento ejecutado continuamente durante la actualización del juego.","on update\n  ...\nend","""
on update
  ifKey D
    self.vx = 5
  end
end
""","Úsalo para input continuo y lógica por frame. Evita trabajo pesado innecesario."),
        topic("EVENTOS","on click","Evento al hacer clic sobre una entidad clicable.","on click\n  ...\nend","""
on click
  showText bubble self "Hola" 3
end
""","Clickable.enabled=false puede impedir la interacción."),
        topic("EVENTOS","on doubleClick","Evento de doble clic sobre una entidad.","on doubleClick\n  ...\nend","""
on doubleClick
  emit inspeccionProfunda
end
""","Principalmente útil en entidades."),
        topic("EVENTOS","on collision","Se dispara al comenzar una colisión física; puede exponer other.","on collision\n  ...\nend","""
on collision
  damage other 10
end
""","No equivale a ejecutar cada frame mientras ambos permanecen solapados."),
        topic("EVENTOS","on trigger","Se dispara al entrar en un Trigger; puede exponer other.","on trigger\n  ...\nend","""
on trigger
  emit entroZona
end
""","Trigger.once permite limitar activaciones."),
        topic("EVENTOS","on destroy","Se ejecuta antes de retirar una entidad destruida.","on destroy\n  ...\nend","""
on destroy
  create explosion
end
""","Útil para efectos, loot y señales."),
        topic("EVENTOS","on event <nombre>","Escucha una señal nombrada emitida con emit.","on event alarma\n  ...\nend","""
on event alarma
  showText free screen "ALARMA" 2
end
""","Puede existir en entidades, capas y escena."),

        topic("EVENTOS","on animationStart","Se ejecuta cuando Animator comienza un AnimationClip.","on animationStart\n  ...\nend","""
on animationStart
  log "animación iniciada"
end
""","Disponible en entidades con Animator."),
        topic("EVENTOS","on animationEnd","Se ejecuta al terminar un clip no cíclico.","on animationEnd\n  ...\nend","""
on animationEnd
  emit ataqueTerminado
end
""","Un clip en loop no termina hasta detenerse o cambiarse."),
        topic("EVENTOS","on animationLoop","Se ejecuta al completar un ciclo de animación.","on animationLoop\n  ...\nend","""
on animationLoop
  log "nuevo ciclo"
end
""","Sirve para lógica sincronizada con ciclos completos."),
        topic("EVENTOS","on animationEvent <nombre>","Escucha un marcador colocado sobre un frame de AnimationClip.","on animationEvent <nombre>\n  ...\nend","""
on animationEvent footstep
  emit paso
end
""","Úsalo para sincronizar golpes, sonidos, proyectiles y efectos con el frame exacto."),

        topic("FLUJO","if <ruta> <operador> <valor>","Condicional principal. Lee una ruta y la compara con un valor.","if <ruta> <op> <valor>\n  ...\nelse\n  ...\nend","""
if Health.current <= 0
  destroy
else
  log "sigue vivo"
end
""","Operadores: ==, =, !=, >, >=, <, <=, contains, startsWith, endsWith."),
        topic("FLUJO","ifKey <tecla>","Verdadero mientras la tecla permanece pulsada.","ifKey <tecla>\n  ...\nend","""
ifKey A
  self.vx = -5
end
""","Ideal para movimiento continuo."),
        topic("FLUJO","ifPressed <tecla>","Verdadero solamente en la transición de tecla no pulsada a pulsada.","ifPressed <tecla>\n  ...\nend","""
ifPressed SPACE
  self.vy = -8
end
""","Ideal para salto, ataque, abrir menú y acciones que no deben repetirse cada frame."),
        topic("FLUJO","ifAction <acción>","Verdadero mientras cualquiera de los bindings de una acción del Input Map permanece activo.","ifAction <acción>\n  ...\nend","""
ifAction MoveLeft
  self.vx = -5
end
""","Preferible a ifKey para controles configurables."),
        topic("FLUJO","ifActionPressed <acción>","Verdadero solamente cuando comienza una acción del Input Map.","ifActionPressed <acción>\n  ...\nend","""
ifActionPressed Jump
  if Rigidbody2D.grounded == true
    self.vy = -8
  end
end
""","Ideal para salto, ataque, interactuar y otras acciones de una sola pulsación."),

        topic("FLUJO","chance <0..1>","Ejecuta el bloque con una probabilidad entre 0 y 1.","chance <probabilidad>\n  ...\nend","""
chance 0.25
  create premio
end
""","0.25 equivale a 25 %."),
        topic("FLUJO","repeat <cantidad>","Repite inmediatamente un bloque una cantidad fija de veces.","repeat <cantidad>\n  ...\nend","""
repeat 3
  create chispa
end
""","Límite de seguridad: 10.000 repeticiones."),
        topic("FLUJO","stop / return","Finaliza la ejecución del evento actual.","stop\nreturn","""
if save.pausa == true
  return
end
""","Ambos cumplen la misma función de corte de la secuencia actual."),

        topic("COMANDOS","log / print","Escribe texto en el log del runtime/editor.","log <texto>\nprint <texto>","""
log "X=${prop:x}"
""","No dibuja texto en pantalla; para eso usa showText."),
        topic("COMANDOS","move","Desplaza la entidad actual de forma relativa.","move <dx> <dy>","""
move 1 0
""","Usa unidades lógicas del mundo."),
        topic("COMANDOS","velocity","Asigna vx y vy de la entidad actual.","velocity <vx> <vy>","""
velocity 5 -8
""","Equivale conceptualmente a establecer la velocidad completa de una vez."),
        topic("COMANDOS","teleport","Coloca la entidad actual en coordenadas absolutas.","teleport <x> <y>","""
teleport 8 4
""","No es un desplazamiento relativo."),
        topic("COMANDOS","bounce","Invierte vx y vy de la entidad actual.","bounce","""
on collision
  bounce
end
""","Útil para proyectiles o rebotes simples."),
        topic("COMANDOS","create / spawn","Clona una entidad plantilla de la escena.","create <plantilla> [x y]\nspawn <plantilla> [x y]","""
create enemigo
create enemigo 12 8
""","La copia recibe un ID runtime único."),
        topic("COMANDOS","destroy","Destruye la entidad que ejecuta el script.","destroy","""
on trigger
  destroy
end
""","Dispara on destroy antes de retirarla."),
        topic("COMANDOS","destroyEntity","Destruye otra entidad por referencia.","destroyEntity <entidad>","""
destroyEntity other
""","La referencia puede ser other, ID o nombre."),
        topic("COMANDOS","moveEntity","Mueve otra entidad relativamente.","moveEntity <entidad> <dx> <dy>","""
moveEntity enemigo 1 0
""","Comando legacy/remoto; las rutas directas son preferibles para propiedades."),
        topic("COMANDOS","teleportEntity","Teletransporta otra entidad.","teleportEntity <entidad> <x> <y>","""
teleportEntity enemigo 10 4
""","Referencia por ID o nombre."),
        topic("COMANDOS","setSprite / setEntitySprite","Cambia el sprite de self u otra entidad.","setSprite <asset>\nsetEntitySprite <entidad> <asset>","""
self.sprite = hero_idle
other.sprite = enemigo_hit
""","La sintaxis por rutas es la recomendada en 2.1+."),
        topic("COMANDOS","damage","Aplica daño semántico a una entidad con Health.","damage <entidad> <cantidad>","""
damage other 25
""","Preferible a editar Health.current cuando quieres expresar daño."),
        topic("COMANDOS","heal","Cura una entidad con Health.","heal <entidad> <cantidad>","""
heal self 20
""","No uses cantidades negativas para simular daño."),
        topic("COMANDOS","spawnPrefab","Crea una instancia de un Prefab por clave y opcionalmente indica posición.","spawnPrefab <prefab> [x y]","""
spawnPrefab slime
spawnPrefab slime 12 5
""","La instancia mantiene el vínculo lógico con la definición del Prefab."),
        topic("COMANDOS","playAnimation / queueAnimation / stopAnimation","Control explícito de Animator desde 2GameScript.","playAnimation <clip|estado>\nqueueAnimation <clip|estado>\nstopAnimation","""
playAnimation Walk
queueAnimation Attack
""","Los marcadores de frame llegan mediante on animationEvent <nombre>."),

        topic("COMANDOS","loadScene","Carga una escena por ID.","loadScene <id>","""
loadScene bosque
""","El editor puede mostrar nombres, pero el runtime resuelve el ID almacenado."),
        topic("COMANDOS","restartScene","Recarga la escena actual.","restartScene","""
on destroy
  restartScene
end
""","Reinicia el nivel actual."),
        topic("COMANDOS","showMenu","Abre una pantalla de menú por ID.","showMenu <id>","""
showMenu pausa
""","Útil para pausa, inventario o pantallas propias."),
        topic("COMANDOS","emit","Publica una señal nombrada.","emit <evento>","""
emit jefeMuerto
""","Los listeners usan on event <nombre>."),
        topic("COMANDOS","wait","Pausa solamente la secuencia actual y la reanuda después.","wait <segundos>","""
wait 2
create enemigo
""","No bloquea render, input, física ni otros scripts."),
        topic("COMANDOS","timer","Agenda un comando para más tarde y continúa inmediatamente.","timer <segundos> <comando>","""
timer 2 create explosion
""","El comando anidado debe ser una instrucción normal; no puede ser wait/every."),
        topic("COMANDOS","every","Ejecuta un comando N veces separado por un intervalo.","every <segundos> <cantidad> <comando>","""
every 1 5 create humo
""","Máximo 10.000 repeticiones."),
        topic("COMANDOS","showText FREE","Muestra texto libre, anclado a pantalla o entidad, con desaparición automática.","showText free <screen|entidad> <texto> <segundos>","""
showText free screen "NIVEL 1" 3
showText free self "-25" 1
""","Admite style <TextStyle> y color <#RRGGBB>; la apariencia base es editable desde el proyecto."),
        topic("COMANDOS","showText BUBBLE","Muestra un bocadillo que puede seguir a una entidad.","showText bubble <entidad> <texto> <segundos>","""
showText bubble self "¡Alto!" 4
""","La duración efectiva está limitada a 10 segundos y siempre termina con fade."),
        topic("COMANDOS","showText NOVEL","Muestra banda tipo novela visual con cierre por clic, tecla o tiempo.","showText novel <hablante> <texto> click\nshowText novel <hablante> <texto> key <TECLA>\nshowText novel <hablante> <texto> <segundos>","""
showText novel Diego "No deberíamos estar aquí." click
showText novel Alessandra "Mira atrás." key SPACE
""","Puede permanecer más de 10 segundos o esperar interacción."),

        topic("ASIGNACIONES","=","Asigna un valor a una ruta.","<ruta> = <valor>","""
Rigidbody2D.gravityScale = 1
self.vx = 5
save.nombre = "Diego"
""","La ruta debe ser reconocida por el contexto/runtime."),
        topic("ASIGNACIONES","+=","Suma numéricamente; si no son números, concatena texto.","<ruta> += <valor>","""
global.score += 100
""","Para valores numéricos realiza suma real."),
        topic("ASIGNACIONES","-=","Resta un valor numérico.","<ruta> -= <valor>","""
Health.current -= 10
""","Requiere valores convertibles a número."),
        topic("ASIGNACIONES","*=","Multiplica un valor numérico.","<ruta> *= <valor>","""
Rigidbody2D.gravityScale *= -1
""","Útil para invertir signos."),
        topic("ASIGNACIONES","/=","Divide un valor numérico.","<ruta> /= <valor>","""
global.factor /= 2
""","Una división por cero conserva el valor previo en la implementación actual."),

        topic("RUTAS","self.*","Propiedades de la entidad que ejecuta el script.","self.x | y | width | height | enabled | layer | group | renderLayer | physicsLayer | sprite | vx | vy","""
self.x = 4
self.sprite = hero_idle
self.vx = -5
""","En componentes de self puede omitirse self: Rigidbody2D.gravityScale = 1."),
        topic("RUTAS","other.*","Accede a la entidad contraparte de collision/trigger.","other.<propiedad>\nother.<Componente>.<propiedad>","""
other.Health.current -= 25
other.sprite = enemigo_hit
""","Solo tiene sentido cuando el evento proporciona other."),
        topic("RUTAS","<entidad>.*","Acceso remoto por ID o nombre.","<idONombre>.<propiedad>\n<idONombre>.<Componente>.<propiedad>","""
enemigo.x = 12
enemigo.Rigidbody2D.gravityScale = 0
""","El runtime intenta resolver primero las referencias disponibles de la escena."),
        topic("RUTAS","global.*","Variables globales de sesión.","global.<nombre>","""
global.score = 0
global.score += 10
""","Se pierden al cerrar el juego."),
        topic("RUTAS","save.*","Valores persistentes del juego respaldados por SQLite.","save.<nombre>","""
save.nombre = "Adarvio"
save.monedas += 1
""","Persisten entre ejecuciones."),
        topic("RUTAS","scene.*","Propiedades del nivel/escena actual.","scene.name\nscene.background\nscene.backgroundMode\nscene.boundary.left|right|top|bottom","""
scene.background = bosque_noche
scene.backgroundMode = cover
""","backgroundMode: color, stretch, cover, contain, tile."),
        topic("RUTAS","layer.*","Propiedades de la capa que posee el script.","layer.name\nlayer.enabled\nlayer.visible\nlayer.locked\nlayer.collision\nlayer.physicsLayer\nlayer.renderLayer","""
layer.visible = false
layer.collision = false
""","Solo es la capa actual del script; no una celda individual."),

        topic("COMPONENTES","GridMovement","Movimiento discreto por grilla usando Input Map.","enabled:boolean\nstep:number\nrepeatDelay:number\nmoveDuration:number\nallowDiagonal:boolean\nallowArrows:boolean\nsnap:boolean","""
GridMovement.step = 1
GridMovement.moveDuration = 0.10
""","Respeta colliders, tiles sólidos y capas físicas."),
        topic("COMPONENTES","PlayerController","Movimiento continuo preconstruido en cuatro direcciones usando MoveLeft/MoveRight/MoveUp/MoveDown.","enabled:boolean\nspeed:number\nallowArrows:boolean","""
PlayerController.speed = 5
""","Para plataformas usa PlatformerController o control horizontal por script."),
        topic("COMPONENTES","PlatformerController","Control horizontal y salto listo para plataformas. Usa Input Map y Rigidbody2D.grounded.","enabled:boolean\nspeed:number\njumpSpeed:number\nleftAction:string\nrightAction:string\njumpAction:string","""
PlatformerController.speed = 5
PlatformerController.jumpSpeed = 8
PlatformerController.jumpAction = Jump
""","Combínalo normalmente con Rigidbody2D y BoxCollider2D."),
        topic("COMPONENTES","Rigidbody2D","Física básica, gravedad y sensores de contacto.","enabled:boolean\nmass:number\ngravityScale:number\ndrag:number\nmaxSpeed:number\nfreezeX:boolean\nfreezeY:boolean\ngrounded:boolean # solo lectura\ntouchingLeft:boolean # solo lectura\ntouchingRight:boolean # solo lectura\ntouchingTop:boolean # solo lectura","""
Rigidbody2D.gravityScale = 1
if Rigidbody2D.grounded == true
  log "en suelo"
end
""","Los cuatro sensores de contacto son calculados por el runtime y no pueden escribirse desde 2GameScript."),
        topic("COMPONENTES","BoxCollider2D","Collider rectangular sólido o no sólido con offset editable.","enabled:boolean\nwidth:number\nheight:number\noffsetX:number\noffsetY:number\nsolid:boolean","""
BoxCollider2D.width = 0.8
BoxCollider2D.height = 0.9
BoxCollider2D.solid = true
""","No necesita Rigidbody2D para bloquear."),
        topic("COMPONENTES","CircleCollider2D","Collider circular básico con radio y offset.","enabled:boolean\nradius:number\noffsetX:number\noffsetY:number\nsolid:boolean","""
CircleCollider2D.radius = 0.45
""","El runtime 2.2 usa su envolvente para resolución de contactos."),
        topic("COMPONENTES","SpriteRenderer","Apariencia de una entidad: opacidad, flip y paleta.","enabled:boolean\nopacity:number\nflipX:boolean\nflipY:boolean\ntint:string\npalette:string","""
SpriteRenderer.opacity = 0.8
SpriteRenderer.flipX = true
SpriteRenderer.palette = red
""","El sprite base sigue siendo self.sprite; PaletteAsset permite recolorear sin duplicar la imagen."),
        topic("COMPONENTES","Animator","Reproduce AnimationClip directamente o mediante AnimatorController.","enabled:boolean\ncontroller:string\nclip:string\nstate:string\nspeed:number\nflipX:boolean\nflipY:boolean\nframe:number\nfinished:boolean","""
Animator.controller = hero-controller
Animator.state = Walk
Animator.speed = 1.25
""","También existen playAnimation, queueAnimation, stopAnimation y eventos de animación."),
        topic("COMPONENTES","Camera2D","Cámara 2D con objetivo, seguimiento, zoom, offset, pixel snap y prioridad.","enabled:boolean\ntarget:string\nfollow:boolean\nzoom:number\noffsetX:number\noffsetY:number\npixelSnap:boolean\npriority:number","""
Camera2D.target = player
Camera2D.zoom = 1.5
Camera2D.pixelSnap = true
""","La resolución lógica permanece independiente de la resolución física del monitor."),
        topic("COMPONENTES","ParticleEmitter2D","Emisor que usa un ParticlePreset del proyecto.","enabled:boolean\npreset:string\nplaying:boolean","""
ParticleEmitter2D.preset = sparks
ParticleEmitter2D.playing = true
""","El preset controla sprite, tasa, burst, vida, velocidad, dispersión, gravedad, escala y opacidad."),
        topic("COMPONENTES","SortingGroup","Control de orden visual y Y-sort.","enabled:boolean\norder:number\nySort:boolean","""
SortingGroup.ySort = true
""","Útil para top-down y grupos visuales que deben conservar orden."),
        topic("COMPONENTES","Patrol","Movimiento automático de patrulla en un eje.","enabled:boolean\naxis:x|y\ndistance:number\nspeed:number","""
Patrol.axis = x
Patrol.distance = 6
Patrol.speed = 2
""","Útil para enemigos sencillos y plataformas móviles básicas."),
        topic("COMPONENTES","ScenePortal","Transfiere al jugador a otra escena y posición.","enabled:boolean\ntargetScene:string\ntargetX:number\ntargetY:number","""
ScenePortal.targetScene = bosque
ScenePortal.targetX = 2
ScenePortal.targetY = 3
""","Se activa al contactar con una entidad controlada por jugador."),
        topic("COMPONENTES","Trigger","Zona de activación con opción de una sola ejecución.","enabled:boolean\nonce:boolean","""
Trigger.once = true
""","Usa on trigger para reaccionar."),
        topic("COMPONENTES","Health","Vida actual y máxima.","enabled:boolean\nmax:number\ncurrent:number","""
Health.max = 150
Health.current = 150
""","damage/heal operan sobre este componente."),
        topic("COMPONENTES","DamageOnContact","Aplica daño al entrar en contacto.","enabled:boolean\ndamage:number","""
DamageOnContact.damage = 20
""","El objetivo debe tener Health."),
        topic("COMPONENTES","Clickable","Controla interacción por clic.","enabled:boolean","""
Clickable.enabled = false
""","Afecta click y doubleClick."),

        topic("LEGACY","addComponent","Añade un componente a una entidad en runtime.","addComponent <entidad> <tipo>","""
addComponent self Rigidbody2D
""","Sintaxis legacy válida; los componentes del Inspector son preferibles cuando son permanentes."),
        topic("LEGACY","removeComponent","Quita un componente.","removeComponent <entidad> <tipo>","""
removeComponent self Patrol
""","La entidad deja de tener ese comportamiento."),
        topic("LEGACY","setComponent","Modifica una propiedad de componente.","setComponent <entidad> <tipo> <propiedad> <valor>","""
setComponent self Rigidbody2D gravityScale -1
""","Forma recomendada moderna: Rigidbody2D.gravityScale = -1."),
        topic("LEGACY","enableComponent","Activa/desactiva un componente sin eliminarlo.","enableComponent <entidad> <tipo> <true|false>","""
enableComponent self BoxCollider2D false
""","Forma moderna: BoxCollider2D.enabled = false."),
        topic("LEGACY","setVar / addVar / mulVar / divVar / randomVar","API de variables locales legacy.","setVar <nombre> <valor>\naddVar <nombre> <n>\nmulVar <nombre> <n>\ndivVar <nombre> <n>\nrandomVar <nombre> <min> <max>","""
setVar vida 10
addVar vida -1
randomVar suerte 1 100
""","Se mantiene por compatibilidad."),
        topic("LEGACY","setGlobal / addGlobal","API global legacy.","setGlobal <nombre> <valor>\naddGlobal <nombre> <n>","""
setGlobal score 0
addGlobal score 10
""","Forma moderna: global.score = 0 / global.score += 10."),
        topic("LEGACY","set / setEntity","Modifica propiedades de entidad con sintaxis 2.0.x.","set <propiedad> <valor>\nsetEntity <entidad> <propiedad> <valor>","""
set x 4
setEntity enemigo enabled false
""","Forma moderna: self.x = 4 / enemigo.enabled = false."),
        topic("LEGACY","setEntityVar / addEntityVar","Modifica variables locales de otra entidad.","setEntityVar <entidad> <nombre> <valor>\naddEntityVar <entidad> <nombre> <n>","""
setEntityVar enemigo estado alerta
addEntityVar enemigo furia 5
""","Se mantiene por compatibilidad."),
        topic("LEGACY","ifVar / ifGlobal / ifProperty","Condicionales legacy por ámbito.","ifVar <nombre> <op> <valor>\nifGlobal <nombre> <op> <valor>\nifProperty <propiedad> <op> <valor>","""
ifGlobal score >= 100
  log "ganó"
end
""","Forma moderna: if global.score >= 100."),
        topic("LEGACY","ifEntity","Comprueba existencia de entidad.","ifEntity <entidad> <exists|missing>","""
ifEntity jefe missing
  emit victoria
end
""","Útil cuando la referencia puede no existir."),
        topic("LEGACY","ifComponent","Comprueba existencia/estado de componente.","ifComponent <entidad> <tipo> [exists|missing|enabled|disabled]","""
ifComponent self Health enabled
  log "Health activo"
end
""","No compara propiedades arbitrarias; para eso usa if <ruta> <op> <valor>."),
        topic("LEGACY","ifOther","Comprueba si el evento tiene una contraparte other válida.","ifOther\n  ...\nend","""
ifOther
  damage other 10
end
""","Principalmente collision/trigger."),

        topic("REFERENCIA","Interpolación","Inserta valores dentro de texto.","${variable}\n${global:nombre}\n${save:nombre}\n${prop:x}\n${path:Rigidbody2D.gravityScale}\n${other}","""
log "X=${prop:x}, gravedad=${path:Rigidbody2D.gravityScale}"
showText free screen "Puntos: ${global:score}" 2
""","La interpolación no convierte automáticamente todos los argumentos numéricos de comandos en expresiones."),
        topic("REFERENCIA","Operadores de comparación","Operadores válidos para if y condiciones legacy.","==  =  !=  >  >=  <  <=  contains  startsWith  endsWith","""
if save.nombre contains "Ada"
  log "coincide"
end
""","Cuando ambos operandos son numéricos se comparan numéricamente; de lo contrario como texto."),
        topic("REFERENCIA","Limitaciones actuales","Lista de cosas que NO debes asumir que existen en 2GameScript 2.1.x.","No hay: for arbitrario, while, funciones de usuario, clases de usuario, imports, &&, ||, SQL directo, Rigidbody2D.grounded.","""
# Esto NO es válido en 2.1.x:
# while true
# if a && b
# function saltar()
# if Rigidbody2D.grounded == true
""","La referencia debe distinguir capacidades reales de funciones planificadas para evitar ejemplos falsos.")
    );

    private ScriptTutorialDialog(){}
    static void show(Window owner){show(owner,null);}

    static void show(Window owner,Consumer<String>insertHandler){
        Stage stage=new Stage(StageStyle.UNDECORATED);if(owner!=null)stage.initOwner(owner);stage.initModality(Modality.NONE);
        stage.setTitle("Tutorial y referencia · 2GameScript");stage.setMinWidth(980);stage.setMinHeight(640);
        BorderPane root=new BorderPane();root.getStyleClass().addAll("studio-root","window-frame","tutorial-window");root.setTop(titleBar(stage));

        ObservableList<Topic>visible=FXCollections.observableArrayList(TOPICS);
        ListView<Topic>nav=new ListView<>(visible);nav.setPrefWidth(330);nav.getStyleClass().add("tutorial-navigation");
        TextField search=new TextField();search.setPromptText("Buscar WASD, salto, Rigidbody2D, wait, showText…");
        ComboBox<String>group=new ComboBox<>();group.getItems().add("TODOS");TOPICS.stream().map(Topic::group).distinct().forEach(group.getItems()::add);group.setValue("TODOS");group.setMaxWidth(Double.MAX_VALUE);
        Label navTitle=new Label("2GAMESCRIPT · TUTORIAL + API");navTitle.getStyleClass().add("tutorial-heading");
        VBox left=new VBox(8,navTitle,search,group,nav);left.setPadding(new Insets(14));left.getStyleClass().add("side-panel");VBox.setVgrow(nav,Priority.ALWAYS);

        Label badge=new Label();badge.getStyleClass().add("muted");
        Label heading=new Label();heading.getStyleClass().add("tutorial-heading");
        Label definition=new Label();definition.setWrapText(true);definition.getStyleClass().add("tutorial-explanation");
        Label syntaxTitle=new Label("SINTAXIS / PROPIEDADES");syntaxTitle.getStyleClass().add("section-title");
        TextArea syntax=new TextArea();syntax.setEditable(false);syntax.setWrapText(false);syntax.getStyleClass().add("tutorial-code");syntax.setPrefRowCount(5);
        Label exampleTitle=new Label("EJEMPLO");exampleTitle.getStyleClass().add("section-title");
        TextArea example=new TextArea();example.setEditable(false);example.setWrapText(false);example.getStyleClass().add("tutorial-code");
        Label notes=new Label();notes.setWrapText(true);notes.getStyleClass().add("tutorial-note");
        Label status=new Label();status.getStyleClass().add("muted");
        Button copy=new Button("Copiar ejemplo"),insert=new Button("Insertar ejemplo en script");insert.getStyleClass().add("primary-button");insert.setVisible(insertHandler!=null);insert.setManaged(insertHandler!=null);
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox actions=new HBox(8,status,spacer,copy,insert);actions.setAlignment(Pos.CENTER_LEFT);
        VBox content=new VBox(10,badge,heading,definition,new Separator(),syntaxTitle,syntax,exampleTitle,example,notes,actions);content.setPadding(new Insets(18));VBox.setVgrow(example,Priority.ALWAYS);
        ScrollPane contentScroll=new ScrollPane(content);contentScroll.setFitToWidth(true);contentScroll.setFitToHeight(true);
        SplitPane split=new SplitPane(left,contentScroll);split.setDividerPositions(.30);root.setCenter(split);

        Runnable filter=()->{
            String q=search.getText()==null?"":search.getText().trim().toLowerCase(Locale.ROOT);String g=group.getValue()==null?"TODOS":group.getValue();
            visible.setAll(TOPICS.stream().filter(t->(g.equals("TODOS")||t.group().equals(g))&&(q.isEmpty()||t.searchable().contains(q))).toList());
            if(!visible.isEmpty())nav.getSelectionModel().selectFirst();
        };
        Runnable update=()->{
            Topic t=nav.getSelectionModel().getSelectedItem();if(t==null){badge.setText("");heading.setText("Sin resultados");definition.setText("");syntax.clear();example.clear();notes.setText("");status.setText("");return;}
            badge.setText(t.group());heading.setText(t.title());definition.setText(t.definition());syntax.setText(t.syntax().strip());example.setText(t.example().strip());notes.setText(t.notes());status.setText((TOPICS.indexOf(t)+1)+" / "+TOPICS.size());
        };
        nav.getSelectionModel().selectedItemProperty().addListener((o,a,b)->update.run());search.textProperty().addListener((o,a,b)->filter.run());group.valueProperty().addListener((o,a,b)->filter.run());
        copy.setOnAction(e->{Topic t=nav.getSelectionModel().getSelectedItem();if(t==null)return;ClipboardContent data=new ClipboardContent();data.putString(t.example().strip());Clipboard.getSystemClipboard().setContent(data);status.setText("Ejemplo copiado.");});
        insert.setOnAction(e->{Topic t=nav.getSelectionModel().getSelectedItem();if(t==null||insertHandler==null||t.example().isBlank())return;insertHandler.accept(t.example().strip());status.setText("Ejemplo insertado en el script seleccionado.");});

        Scene scene=new Scene(root,1180,780);var css=ScriptTutorialDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);nav.getSelectionModel().selectFirst();update.run();
        if(owner!=null){stage.setX(owner.getX()+Math.max(24,(owner.getWidth()-1180)/2));stage.setY(owner.getY()+Math.max(24,(owner.getHeight()-780)/2));}stage.show();
    }

    private static HBox titleBar(Stage stage){
        Label mark=new Label("2G"),title=new Label("Tutorial + Referencia API · 2GameScript");mark.getStyleClass().add("window-app-mark");title.getStyleClass().add("window-title");
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button min=chrome("—"),max=chrome("▢"),close=chrome("×");close.getStyleClass().add("window-close");
        min.setOnAction(e->stage.setIconified(true));max.setOnAction(e->stage.setMaximized(!stage.isMaximized()));close.setOnAction(e->stage.close());
        HBox bar=new HBox(9,mark,title,spacer,min,max,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("title-bar");double[]drag=new double[2];
        bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||e.getTarget() instanceof Button)return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});
        bar.setOnMouseDragged(e->{if(stage.isMaximized()||e.getTarget() instanceof Button)return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});
        bar.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!(e.getTarget() instanceof Button))stage.setMaximized(!stage.isMaximized());});return bar;
    }
    private static Button chrome(String text){Button b=new Button(text);b.getStyleClass().add("window-control");return b;}
}
