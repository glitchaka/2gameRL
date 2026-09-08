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

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

final class ScriptTutorialDialog {
    private record Lesson(String title,String explanation,String code,String note){@Override public String toString(){return title;}}
    private static final List<Lesson>LESSONS=List.of(
        lesson("1 · Eventos y contextos","2GameScript puede vivir en una entidad, una capa de tiles o la escena. Todos usan on...end.","""
on start
  log "iniciado"
end

on update
  # cada frame
end
""","Entidades además reciben click, doubleClick, collision, trigger y destroy."),
        lesson("2 · Sintaxis de propiedades","2.1.0 incorpora asignación directa por rutas. La sintaxis antigua sigue siendo compatible.","""
on click
  Rigidbody2D.gravityScale = -1
  Rigidbody2D.maxSpeed += 2
  Health.current -= 10
  self.x += 1
end
""","También existen *= y /=. Ejemplo útil: Rigidbody2D.gravityScale *= -1."),
        lesson("3 · Otras entidades","self es la entidad actual y other la contraparte de collision/trigger. También puedes usar ID o nombre.","""
on collision
  other.Health.current -= 25
  other.sprite = enemigo_golpeado
  enemigo.Rigidbody2D.gravityScale = 0
end
""","damage/heal siguen disponibles cuando quieres semántica de daño en vez de una asignación cruda."),
        lesson("4 · Movimiento por losetas","GridMovement es configurable por objeto; PlayerController mantiene movimiento libre.","""
on start
  GridMovement.step = 1
  GridMovement.repeatDelay = 0.16
  GridMovement.moveDuration = 0.10
  GridMovement.allowDiagonal = false
  GridMovement.snap = true
end
""","BoxCollider2D bloquea GridMovement y no necesita Rigidbody2D."),
        lesson("5 · Variables, globales y guardado","global.* dura mientras corre el juego. save.* usa SQLite y sobrevive al cerrar el juego.","""
on start
  global.score = 0
  save.nombre = "Adarvio"
  save.monedas += 1
  log "Guardado: ${save:nombre}"
end
""","Cada juego exportado guarda su base en la carpeta local de datos del usuario."),
        lesson("6 · Condiciones intuitivas","if acepta rutas además de los comandos ifVar/ifGlobal heredados.","""
on click
  if save.llaveRoja == true
    BoxCollider2D.enabled = false
  else
    showText bubble self "Está cerrada" 3
  end
end
""","Operadores: == != > >= < <= contains startsWith endsWith."),
        lesson("7 · wait, timer y every","wait pausa solo la secuencia. timer agenda una acción y continúa. every repite una acción N veces.","""
on click
  destroy
  wait 9
  create pj
end

on start
  timer 2 create explosion
  every 1 5 create humo
end
""","Ninguno de estos comandos congela el juego."),
        lesson("8 · Señales entre sistemas","emit publica una señal. Cualquier script de escena, capa o entidad puede escucharla con on event.","""
on trigger
  emit entroAlTemplo
end

on event jefeMuerto
  showText free screen "VICTORIA" 4
end
""","Esto evita crear entidades invisibles solo para coordinar lógica global."),
        lesson("9 · Script de escena","Selecciona ESCENA > Propiedades y script. Puede controlar fondo, límites, señales, oleadas y estado global.","""
on start
  timer 30 emit anochecer
end

on event anochecer
  scene.background = bosque_noche
  scene.backgroundMode = cover
  showText free screen "ANOCHECE" 4
end
""","También: scene.boundary.left/right/top/bottom = true|false."),
        lesson("10 · Script de capa","Selecciona una capa de tiles y abre Script. Las capas pueden reaccionar a señales.","""
on event descongelar
  layer.enabled = false
  layer.collision = false
end
""","layer.visible/enabled, locked, collision, physicsLayer y renderLayer son modificables."),
        lesson("11 · Texto libre","FREE sirve para títulos, avisos y texto flotante. Siempre se desvanece.","""
on start
  showText free screen "NIVEL 1" 3
end

on collision
  showText free self "-25" 1.5
end
""","Si usas self/other/ID/nombre, el texto sigue a la entidad mientras se mueve."),
        lesson("12 · Bocadillos","BUBBLE es diálogo tipo cómic y sigue a una entidad. Su duración está limitada a 10 segundos.","""
on click
  showText bubble self "¿Qué demonios fue eso?" 4
end

on collision
  showText bubble other "¡Eh!" 2
end
""","El motor fuerza fade-out; no puede quedar un bocadillo permanente."),
        lesson("13 · Novela visual","NOVEL dibuja una banda de diálogo. Puede cerrarse por clic, tecla o tiempo.","""
on click
  showText novel Diego "No deberíamos estar aquí." click
  wait 1
  showText novel Alessandra "Entonces no mires atrás." key SPACE
end
""","También: showText novel Narrador " + "\"Texto\" 30 para cierre automático."),
        lesson("14 · Crear y controlar entidades","create/spawn clona una plantilla. Los comandos legacy remotos siguen funcionando.","""
on event oleada
  create enemigo 12 8
  setEntity enemigo physicsLayer Enemy
  addComponent enemigo Health
  setComponent enemigo Health current 50
end
""","create sin coordenadas usa la posición de la entidad ejecutora o la de la plantilla en scripts globales."),
        lesson("15 · Referencia rápida","La forma recomendada es la sintaxis con puntos; los comandos 2.0.x siguen aceptados.","""
# propiedades
Rigidbody2D.gravityScale *= -1
self.sprite = hero_idle
global.score += 100
save.monedas += 1
scene.background = noche
layer.enabled = false

# flujo
emit alarma
wait 1
timer 2 create fx
every 1 5 create humo

# texto
showText free screen "TÍTULO" 3
showText bubble self "Hola" 4
showText novel Diego "Texto" click
""","La Biblia completa y canónica está en docs/SCRIPTING.md.")
    );
    private static Lesson lesson(String a,String b,String c,String d){return new Lesson(a,b,c,d);}private ScriptTutorialDialog(){}static void show(Window owner){show(owner,null);}
    static void show(Window owner,Consumer<String>insertHandler){Stage stage=new Stage(StageStyle.UNDECORATED);if(owner!=null)stage.initOwner(owner);stage.initModality(Modality.NONE);stage.setTitle("Tutorial de scripting · 2gameRL");stage.setMinWidth(820);stage.setMinHeight(560);BorderPane root=new BorderPane();root.getStyleClass().addAll("studio-root","window-frame","tutorial-window");root.setTop(titleBar(stage));ObservableList<Lesson>visible=FXCollections.observableArrayList(LESSONS);ListView<Lesson>nav=new ListView<>(visible);nav.setPrefWidth(280);nav.getStyleClass().add("tutorial-navigation");TextField search=new TextField();search.setPromptText("Buscar comando, evento o ejemplo…");VBox left=new VBox(8,new Label("TUTORIAL 2GAMESCRIPT 2.1"),search,nav);left.setPadding(new Insets(14));left.getStyleClass().add("side-panel");VBox.setVgrow(nav,Priority.ALWAYS);Label heading=new Label();heading.getStyleClass().add("tutorial-heading");Label explanation=new Label();explanation.setWrapText(true);explanation.getStyleClass().add("tutorial-explanation");TextArea code=new TextArea();code.setEditable(false);code.setWrapText(false);code.getStyleClass().add("tutorial-code");Label note=new Label();note.setWrapText(true);note.getStyleClass().add("tutorial-note");Label status=new Label();status.getStyleClass().add("muted");Button copy=new Button("Copiar ejemplo"),insert=new Button("Insertar en script");insert.getStyleClass().add("primary-button");insert.setVisible(insertHandler!=null);insert.setManaged(insertHandler!=null);Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox actions=new HBox(8,status,spacer,copy,insert);actions.setAlignment(Pos.CENTER_LEFT);VBox content=new VBox(10,heading,explanation,new Separator(),code,note,actions);content.setPadding(new Insets(18));VBox.setVgrow(code,Priority.ALWAYS);ScrollPane contentScroll=new ScrollPane(content);contentScroll.setFitToWidth(true);contentScroll.setFitToHeight(true);SplitPane split=new SplitPane(left,contentScroll);split.setDividerPositions(.27);root.setCenter(split);Runnable update=()->{Lesson l=nav.getSelectionModel().getSelectedItem();if(l==null){heading.setText("Sin resultados");explanation.setText("");code.clear();note.setText("");return;}heading.setText(l.title());explanation.setText(l.explanation());code.setText(l.code().strip());note.setText(l.note());status.setText("Lección "+(LESSONS.indexOf(l)+1)+" de "+LESSONS.size());};nav.getSelectionModel().selectedItemProperty().addListener((o,a,b)->update.run());search.textProperty().addListener((o,a,b)->{String q=b==null?"":b.trim().toLowerCase(Locale.ROOT);visible.setAll(LESSONS.stream().filter(l->q.isEmpty()||l.title().toLowerCase(Locale.ROOT).contains(q)||l.explanation().toLowerCase(Locale.ROOT).contains(q)||l.code().toLowerCase(Locale.ROOT).contains(q)).toList());if(!visible.isEmpty())nav.getSelectionModel().selectFirst();else update.run();});copy.setOnAction(e->{Lesson l=nav.getSelectionModel().getSelectedItem();if(l==null)return;ClipboardContent data=new ClipboardContent();data.putString(l.code().strip());Clipboard.getSystemClipboard().setContent(data);status.setText("Copiado.");});insert.setOnAction(e->{Lesson l=nav.getSelectionModel().getSelectedItem();if(l==null||insertHandler==null)return;insertHandler.accept(l.code().strip());status.setText("Insertado en el script seleccionado.");});Scene scene=new Scene(root,1080,720);var css=ScriptTutorialDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);nav.getSelectionModel().selectFirst();update.run();if(owner!=null){stage.setX(owner.getX()+Math.max(24,(owner.getWidth()-1080)/2));stage.setY(owner.getY()+Math.max(24,(owner.getHeight()-720)/2));}stage.show();}
    private static HBox titleBar(Stage stage){Label mark=new Label("2G"),title=new Label("Tutorial de scripting · 2GameScript 2.1");mark.getStyleClass().add("window-app-mark");title.getStyleClass().add("window-title");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button min=chrome("—"),max=chrome("▢"),close=chrome("×");close.getStyleClass().add("window-close");min.setOnAction(e->stage.setIconified(true));max.setOnAction(e->stage.setMaximized(!stage.isMaximized()));close.setOnAction(e->stage.close());HBox bar=new HBox(9,mark,title,spacer,min,max,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("title-bar");double[]drag=new double[2];bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||e.getTarget() instanceof Button)return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});bar.setOnMouseDragged(e->{if(stage.isMaximized()||e.getTarget() instanceof Button)return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});bar.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!(e.getTarget() instanceof Button))stage.setMaximized(!stage.isMaximized());});return bar;}private static Button chrome(String text){Button b=new Button(text);b.getStyleClass().add("window-control");return b;}
}
