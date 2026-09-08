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

    private static final List<Lesson> LESSONS=List.of(
            lesson("1 · Estructura y eventos","Todo script vive dentro de bloques on...end. Eventos: start, update, click, doubleClick, collision, trigger y destroy.","""
                    on start
                      log "Entidad iniciada"
                    end

                    on destroy
                      log "Entidad destruida"
                    end
                    ""","collision/trigger permiten usar la referencia especial other."),
            lesson("2 · Movimiento y teclado","move desplaza, velocity asigna velocidad, teleport cambia posición y bounce invierte velocidad. ifKey/ifPressed pueden usarse en una línea o como bloque.","""
                    on update
                      ifKey W
                        move 0 -0.05
                        setVar caminando true
                      end
                      ifPressed SPACE log "acción"
                    end
                    ""","PlayerController sigue siendo la forma más rápida para control estándar."),
            lesson("3 · Variables, globales e interpolación","Variables locales pertenecen a cada instancia. Las globales sobreviven al cambio de escena mientras el juego siga abierto. Puedes interpolar valores en textos.","""
                    on start
                      setVar score 1
                      addVar score 4
                      mulVar score 2
                      setGlobal monedas 3
                      log "Score ${score} / global ${global:monedas}"
                    end
                    ""","También existen divVar, addGlobal y randomVar."),
            lesson("4 · Condiciones y else","2GameScript soporta bloques condicionales. Operadores: == != > >= < <= contains startsWith endsWith.","""
                    on click
                      ifVar score >= 10
                        log "ganaste"
                      else
                        log "todavía no"
                      end
                    end
                    ""","Hay ifGlobal, ifProperty, ifEntity, ifComponent, ifOther y chance."),
            lesson("5 · Bucles y temporizadores","repeat repite un bloque. wait suspende solo la secuencia actual. timer agenda una acción sin detener el bloque. every repite una acción programada.","""
                    on click
                      repeat 3
                        create chispa
                      end
                      timer 2 create explosion
                      every 1 5 create humo
                    end
                    ""","wait no congela física, render, input ni otros scripts."),
            lesson("6 · Respawn con wait","La secuencia puede continuar incluso después de destruir la entidad origen.","""
                    on click
                      destroy
                      wait 9
                      create pj
                    end
                    ""","Este es el patrón de respawn solicitado: la nueva entidad aparece 9 segundos después."),
            lesson("7 · Crear y controlar entidades","create/spawn clona una plantilla de la escena. Los comandos remotos aceptan self, other, ID o nombre.","""
                    on collision
                      damage other 10
                      setEntity other physicsLayer Enemy
                      moveEntity other 1 0
                      setEntitySprite other golpeado.png
                    end
                    ""","También: teleportEntity, destroyEntity, setEntityVar y addEntityVar."),
            lesson("8 · Componentes desde script","Puedes añadir, quitar, activar y configurar componentes en runtime.","""
                    on start
                      addComponent self Health
                      setComponent self Health max 250
                      setComponent self Health current 250
                      enableComponent self BoxCollider2D true
                    end
                    ""","Los tipos válidos son los mismos que aparecen en el Inspector."),
            lesson("9 · Vida, colisiones y other","BoxCollider2D bloquea sin requerir Rigidbody2D. En collision/trigger, other es la entidad contraria.","""
                    on collision
                      ifOther
                        damage other 25
                        log "Golpeé a ${other}"
                      end
                    end
                    ""","Las capas físicas solo filtran pares; no habilitan la colisión básica."),
            lesson("10 · Escenas, menús y sprites","Los scripts pueden cambiar escena, reiniciar la escena actual, abrir un menú o cambiar gráficos.","""
                    on doubleClick
                      setSprite puerta-abierta.png
                      wait 0.25
                      loadScene bosque
                    end
                    ""","showMenu pausa el juego y muestra una pantalla de menú existente."),
            lesson("11 · Condiciones adicionales","Puedes consultar existencia de entidades/componentes y propiedades propias.","""
                    on update
                      ifEntity jefe exists
                        ifProperty x > 12
                          setEntity jefe enabled true
                        end
                      end
                    end
                    ""","ifComponent self Health enabled comprueba que el componente exista y esté activo."),
            lesson("12 · Referencia rápida","Comandos principales disponibles en 2GameScript 2.0.11.","""
                    # flujo
                    wait 1
                    timer 2 create fx
                    every 1 5 create humo
                    repeat 3
                      log "x"
                    end

                    # entidades
                    create enemigo
                    destroyEntity other
                    setEntity enemigo x 8
                    addComponent enemigo Health
                    setComponent enemigo Health current 50

                    # estado
                    setVar score 0
                    addVar score 1
                    setGlobal nivel 2
                    damage other 10
                    heal self 5
                    ""","La referencia completa está también en docs/SCRIPTING.md.")
    );

    private static Lesson lesson(String a,String b,String c,String d){return new Lesson(a,b,c,d);}
    private ScriptTutorialDialog(){}
    static void show(Window owner){show(owner,null);}

    static void show(Window owner,Consumer<String> insertHandler){
        Stage stage=new Stage(StageStyle.UNDECORATED);if(owner!=null)stage.initOwner(owner);stage.initModality(Modality.NONE);stage.setTitle("Tutorial de scripting · 2gameRL");stage.setMinWidth(820);stage.setMinHeight(560);
        BorderPane root=new BorderPane();root.getStyleClass().addAll("studio-root","window-frame","tutorial-window");root.setTop(titleBar(stage));
        ObservableList<Lesson>visible=FXCollections.observableArrayList(LESSONS);ListView<Lesson>nav=new ListView<>(visible);nav.setPrefWidth(260);nav.getStyleClass().add("tutorial-navigation");
        TextField search=new TextField();search.setPromptText("Buscar comando, evento o ejemplo…");VBox left=new VBox(8,new Label("TUTORIAL 2GAMESCRIPT"),search,nav);left.setPadding(new Insets(14));left.getStyleClass().add("side-panel");VBox.setVgrow(nav,Priority.ALWAYS);
        Label heading=new Label();heading.getStyleClass().add("tutorial-heading");Label explanation=new Label();explanation.setWrapText(true);explanation.getStyleClass().add("tutorial-explanation");TextArea code=new TextArea();code.setEditable(false);code.setWrapText(false);code.getStyleClass().add("tutorial-code");Label note=new Label();note.setWrapText(true);note.getStyleClass().add("tutorial-note");Label status=new Label();status.getStyleClass().add("muted");
        Button copy=new Button("Copiar ejemplo"),insert=new Button("Insertar en script");insert.getStyleClass().add("primary-button");insert.setVisible(insertHandler!=null);insert.setManaged(insertHandler!=null);Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox actions=new HBox(8,status,spacer,copy,insert);actions.setAlignment(Pos.CENTER_LEFT);
        VBox content=new VBox(10,heading,explanation,new Separator(),code,note,actions);content.setPadding(new Insets(18));VBox.setVgrow(code,Priority.ALWAYS);ScrollPane contentScroll=new ScrollPane(content);contentScroll.setFitToWidth(true);contentScroll.setFitToHeight(true);SplitPane split=new SplitPane(left,contentScroll);split.setDividerPositions(.25);root.setCenter(split);
        Runnable update=()->{Lesson l=nav.getSelectionModel().getSelectedItem();if(l==null){heading.setText("Sin resultados");explanation.setText("");code.clear();note.setText("");return;}heading.setText(l.title());explanation.setText(l.explanation());code.setText(l.code().strip());note.setText(l.note());status.setText("Lección "+(LESSONS.indexOf(l)+1)+" de "+LESSONS.size());};
        nav.getSelectionModel().selectedItemProperty().addListener((o,a,b)->update.run());search.textProperty().addListener((o,a,b)->{String q=b==null?"":b.trim().toLowerCase(Locale.ROOT);visible.setAll(LESSONS.stream().filter(l->q.isEmpty()||l.title().toLowerCase(Locale.ROOT).contains(q)||l.explanation().toLowerCase(Locale.ROOT).contains(q)||l.code().toLowerCase(Locale.ROOT).contains(q)).toList());if(!visible.isEmpty())nav.getSelectionModel().selectFirst();else update.run();});
        copy.setOnAction(e->{Lesson l=nav.getSelectionModel().getSelectedItem();if(l==null)return;ClipboardContent data=new ClipboardContent();data.putString(l.code().strip());Clipboard.getSystemClipboard().setContent(data);status.setText("Copiado.");});
        insert.setOnAction(e->{Lesson l=nav.getSelectionModel().getSelectedItem();if(l==null||insertHandler==null)return;insertHandler.accept(l.code().strip());status.setText("Insertado en el script seleccionado.");});
        Scene scene=new Scene(root,1080,720);var css=ScriptTutorialDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);nav.getSelectionModel().selectFirst();update.run();if(owner!=null){stage.setX(owner.getX()+Math.max(24,(owner.getWidth()-1080)/2));stage.setY(owner.getY()+Math.max(24,(owner.getHeight()-720)/2));}stage.show();
    }

    private static HBox titleBar(Stage stage){Label mark=new Label("2G");mark.getStyleClass().add("window-app-mark");Label title=new Label("Tutorial de scripting · 2GameScript");title.getStyleClass().add("window-title");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button min=chrome("—"),max=chrome("▢"),close=chrome("×");close.getStyleClass().add("window-close");min.setOnAction(e->stage.setIconified(true));max.setOnAction(e->stage.setMaximized(!stage.isMaximized()));close.setOnAction(e->stage.close());HBox bar=new HBox(9,mark,title,spacer,min,max,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("title-bar");double[]drag=new double[2];bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||e.getTarget() instanceof Button)return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});bar.setOnMouseDragged(e->{if(stage.isMaximized()||e.getTarget() instanceof Button)return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});bar.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!(e.getTarget() instanceof Button))stage.setMaximized(!stage.isMaximized());});return bar;}
    private static Button chrome(String text){Button b=new Button(text);b.getStyleClass().add("window-control");return b;}
}
