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
    private record Lesson(String title, String explanation, String code, String note) {
        @Override public String toString() { return title; }
    }

    private static final List<Lesson> LESSONS = List.of(
            new Lesson(
                    "1 · Primer script",
                    """
                    Cada entidad puede tener su propio 2GameScript. Selecciona una entidad en Escena y abre la pestaña Script; un doble clic sobre ella también abre el editor.

                    Los comandos deben estar dentro de un bloque `on ... end`. `#` inicia un comentario. Los textos con espacios pueden escribirse entre comillas.
                    """,
                    """
                    on start
                      log "Objeto iniciado"
                    end

                    on click
                      log "Me hicieron clic"
                    end
                    """,
                    "Usa Validar script antes de Probar. El validador usa el mismo parser que el runtime."
            ),
            new Lesson(
                    "2 · Eventos",
                    """
                    Eventos disponibles:

                    start — una vez al crear/cargar la entidad.
                    update — cada frame.
                    click — clic sobre la entidad.
                    doubleClick — doble clic.
                    collision — contacto sólido de BoxCollider2D.
                    trigger — entrada en un Trigger permitida por las capas físicas.
                    """,
                    """
                    on start
                      log "Listo"
                    end

                    on collision
                      bounce
                    end
                    """,
                    "Una entidad puede tener varios bloques de eventos en el mismo script."
            ),
            new Lesson(
                    "3 · Movimiento y teclado",
                    """
                    move x y desplaza la entidad inmediatamente. velocity x y asigna velocidad continua. teleport x y cambia su posición. bounce invierte la velocidad actual.

                    ifKey ejecuta mientras la tecla está presionada. ifPressed ejecuta una sola vez al comenzar la pulsación.
                    """,
                    """
                    on update
                      ifKey W move 0 -0.05
                      ifKey S move 0 0.05
                      ifKey A move -0.05 0
                      ifKey D move 0.05 0
                      ifPressed SPACE log "Acción"
                    end
                    """,
                    "Para control estándar es más simple añadir PlayerController y ajustar speed."
            ),
            new Lesson(
                    "4 · Componentes integrados",
                    """
                    Los comportamientos del Inspector funcionan sin script adicional:

                    Rigidbody2D — gravedad, masa, drag y velocidad máxima.
                    BoxCollider2D — colisión rectangular sólida. No necesita Rigidbody2D para bloquear.
                    PlayerController — WASD/flechas.
                    Patrol — patrulla automática.
                    ScenePortal — cambia de escena al contacto con el jugador.
                    Trigger — dispara `trigger`.
                    Health — vida actual/máxima.
                    DamageOnContact — daño al contacto.
                    Clickable — habilita/deshabilita interacción de mouse.

                    Cada componente tiene `enabled`. BoxCollider2D tiene además `solid`.
                    """,
                    """
                    on collision
                      log "COLISIÓN"
                    end
                    """,
                    "Las capas físicas filtran colisiones; por defecto las capas creadas colisionan entre sí."
            ),
            new Lesson(
                    "5 · Variables y propiedades",
                    """
                    setVar nombre valor guarda estado en la entidad. addVar suma un número.

                    set modifica propiedades en runtime. Propiedades directas: x, y, width, height, enabled, layer, renderLayer y physicsLayer. Otros nombres se guardan como variables.
                    """,
                    """
                    on start
                      setVar monedas 0
                      set renderLayer Personajes
                      set physicsLayer Player
                    end

                    on click
                      addVar monedas 1
                    end
                    """,
                    "Las variables pertenecen a cada instancia; las entidades creadas reciben una copia inicial de las variables de su plantilla."
            ),
            new Lesson(
                    "6 · Crear entidades",
                    """
                    `create plantilla` crea una nueva instancia de otra entidad de la escena. Puedes indicar el ID de la entidad o su nombre exacto.

                    Sin coordenadas, la copia nace donde está la entidad que ejecuta el comando. Con `create plantilla x y` nace en la posición indicada.

                    Cada instancia recibe un ID de runtime único y ejecuta su propio evento start inmediatamente después de ser creada.
                    """,
                    """
                    on click
                      create enemigo
                      create particula 12 8
                    end
                    """,
                    "La entidad usada como plantilla conserva sprite, componentes, script, capas, tamaño y variables iniciales."
            ),
            new Lesson(
                    "7 · wait: continuar más tarde",
                    """
                    `wait segundos` pausa solamente la secuencia de ese evento. No congela la ventana, la física, otros scripts ni el juego.

                    El resto del bloque continúa cuando vence el tiempo. Esto permite destruir la entidad origen y seguir la secuencia después: el temporizador conserva el contexto necesario.
                    """,
                    """
                    on click
                      destroy
                      wait 9
                      create pj
                    end
                    """,
                    "En este ejemplo pj aparece nueve segundos después en la posición donde estaba la entidad destruida."
            ),
            new Lesson(
                    "8 · timer: programar sin pausar",
                    """
                    `timer segundos comando` agenda un solo comando para el futuro y continúa inmediatamente con las siguientes líneas del bloque.

                    Úsalo cuando quieras programar una acción sin detener la secuencia actual.
                    """,
                    """
                    on click
                      timer 3 create explosion 8 6
                      setSprite boton-presionado.png
                      log "Explosión programada"
                    end
                    """,
                    "timer puede envolver cualquier comando normal excepto wait. Para secuencias de varios pasos usa wait."
            ),
            new Lesson(
                    "9 · Colisiones, capas y triggers",
                    """
                    Dos entidades con BoxCollider2D activo y solid=true bloquean su movimiento por defecto. Rigidbody2D no es requisito.

                    Las capas físicas sirven para filtrar contactos mediante la matriz de colisiones. Las capas visuales solo controlan el orden de dibujo. Las capas de tiles pueden marcarse como visibles, bloqueadas y con/sin colisión.

                    collision se dispara en contactos sólidos. trigger se dispara en entidades con Trigger cuando el contacto está permitido por la matriz física.
                    """,
                    """
                    on collision
                      log "Choqué"
                    end

                    on trigger
                      log "Entró al área"
                    end
                    """,
                    "Si dos capas físicas están marcadas para ignorarse, BoxCollider2D, Trigger, DamageOnContact y portales ignoran ese par."
            ),
            new Lesson(
                    "10 · Escenas, sprites y destrucción",
                    """
                    loadScene id cambia a otra escena. setSprite asset cambia el gráfico usando la clave de un asset. destroy elimina la instancia actual de la ejecución.

                    Los temporizadores pendientes se cancelan al cambiar de escena, para evitar que scripts de la escena anterior creen objetos después del cambio.
                    """,
                    """
                    on doubleClick
                      setSprite puerta-abierta.png
                      wait 0.25
                      loadScene nivel-2
                    end
                    """,
                    "Usa el ID de la escena y la clave exacta del asset."
            ),
            new Lesson(
                    "11 · Referencia completa",
                    """
                    Eventos:
                    start · update · click · doubleClick · collision · trigger

                    Comandos:
                    log <texto>
                    move <x> <y>
                    velocity <x> <y>
                    teleport <x> <y>
                    bounce
                    destroy
                    wait <segundos>
                    timer <segundos> <comando>
                    create <entidad> [x y]
                    loadScene <id>
                    setSprite <asset>
                    setVar <nombre> <valor>
                    addVar <nombre> <número>
                    set <propiedad> <valor>
                    ifKey <tecla> <comando>
                    ifPressed <tecla> <comando>

                    Los tiempos negativos son inválidos. wait debe ir en una línea propia. timer no puede envolver wait.
                    """,
                    """
                    on click
                      destroy
                      wait 9
                      create pj
                    end
                    """,
                    "Si el validador marca una línea, corrígela antes de probar o exportar."
            )
    );

    private ScriptTutorialDialog() {}

    static void show(Window owner) { show(owner, null); }

    static void show(Window owner, Consumer<String> insertHandler) {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.NONE);
        stage.setTitle("Tutorial de scripting · 2gameRL");
        stage.setMinWidth(820);
        stage.setMinHeight(560);

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("studio-root", "window-frame", "tutorial-window");
        root.setTop(titleBar(stage));

        ObservableList<Lesson> visible = FXCollections.observableArrayList(LESSONS);
        ListView<Lesson> navigation = new ListView<>(visible);
        navigation.getStyleClass().add("tutorial-navigation");
        navigation.setPrefWidth(250);

        TextField search = new TextField();
        search.setPromptText("Buscar en el tutorial…");
        VBox left = new VBox(8, new Label("TUTORIAL 2GAMESCRIPT"), search, navigation);
        left.setPadding(new Insets(14));
        left.getStyleClass().add("side-panel");
        VBox.setVgrow(navigation, Priority.ALWAYS);

        Label heading = new Label(); heading.getStyleClass().add("tutorial-heading");
        Label explanation = new Label(); explanation.setWrapText(true); explanation.getStyleClass().add("tutorial-explanation");
        TextArea code = new TextArea(); code.setEditable(false); code.setWrapText(false); code.getStyleClass().add("tutorial-code"); code.setPrefRowCount(12);
        Label note = new Label(); note.setWrapText(true); note.getStyleClass().add("tutorial-note");
        Label status = new Label("Selecciona una lección."); status.getStyleClass().add("muted");

        Button copy = new Button("Copiar ejemplo");
        Button insert = new Button("Insertar en el script"); insert.getStyleClass().add("primary-button"); insert.setVisible(insertHandler != null); insert.setManaged(insertHandler != null);
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox actions = new HBox(8, status, spacer, copy, insert); actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, heading, explanation, new Separator(), code, note, actions); content.setPadding(new Insets(18)); VBox.setVgrow(code, Priority.ALWAYS);
        ScrollPane contentScroll = new ScrollPane(content); contentScroll.setFitToWidth(true); contentScroll.setFitToHeight(true); contentScroll.getStyleClass().add("tutorial-content-scroll");

        SplitPane split = new SplitPane(left, contentScroll); split.setDividerPositions(.24); root.setCenter(split);

        Runnable update = () -> {
            Lesson lesson = navigation.getSelectionModel().getSelectedItem();
            if (lesson == null) {
                heading.setText("Sin resultados"); explanation.setText("No hay lecciones que coincidan con la búsqueda."); code.clear(); note.setText(""); copy.setDisable(true); insert.setDisable(true); return;
            }
            heading.setText(lesson.title()); explanation.setText(lesson.explanation().strip()); code.setText(lesson.code().strip()); note.setText(lesson.note());
            boolean noCode = lesson.code().isBlank(); copy.setDisable(noCode); insert.setDisable(noCode || insertHandler == null);
            status.setText("Lección " + (LESSONS.indexOf(lesson) + 1) + " de " + LESSONS.size());
        };

        navigation.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> update.run());
        search.textProperty().addListener((o,a,b) -> {
            String q = b == null ? "" : b.trim().toLowerCase(Locale.ROOT);
            visible.setAll(LESSONS.stream().filter(l -> q.isEmpty() || l.title().toLowerCase(Locale.ROOT).contains(q) || l.explanation().toLowerCase(Locale.ROOT).contains(q) || l.code().toLowerCase(Locale.ROOT).contains(q)).toList());
            if (!visible.isEmpty()) navigation.getSelectionModel().selectFirst(); else update.run();
        });

        copy.setOnAction(e -> {
            Lesson lesson = navigation.getSelectionModel().getSelectedItem(); if (lesson == null || lesson.code().isBlank()) return;
            ClipboardContent data = new ClipboardContent(); data.putString(lesson.code().strip()); Clipboard.getSystemClipboard().setContent(data); status.setText("Ejemplo copiado al portapapeles.");
        });
        insert.setOnAction(e -> {
            Lesson lesson = navigation.getSelectionModel().getSelectedItem(); if (lesson == null || lesson.code().isBlank() || insertHandler == null) return;
            insertHandler.accept(lesson.code().strip()); status.setText("Ejemplo insertado en el script seleccionado.");
        });

        Scene scene = new Scene(root, 1080, 720);
        var css = ScriptTutorialDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene); navigation.getSelectionModel().selectFirst(); update.run();
        if (owner != null) { stage.setX(owner.getX() + Math.max(24, (owner.getWidth() - 1080) / 2)); stage.setY(owner.getY() + Math.max(24, (owner.getHeight() - 720) / 2)); }
        stage.show();
    }

    private static HBox titleBar(Stage stage) {
        Label mark = new Label("2G"); mark.getStyleClass().add("window-app-mark");
        Label title = new Label("Tutorial de scripting · 2GameScript"); title.getStyleClass().add("window-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button minimize = chromeButton("—", "Minimizar"), maximize = chromeButton("▢", "Maximizar / restaurar"), close = chromeButton("×", "Cerrar"); close.getStyleClass().add("window-close");
        minimize.setOnAction(e -> stage.setIconified(true)); maximize.setOnAction(e -> stage.setMaximized(!stage.isMaximized())); close.setOnAction(e -> stage.close());
        stage.maximizedProperty().addListener((o,a,b) -> maximize.setText(b ? "❐" : "▢"));
        HBox bar = new HBox(9, mark, title, spacer, minimize, maximize, close); bar.setAlignment(Pos.CENTER_LEFT); bar.getStyleClass().add("title-bar");
        final double[] drag = new double[2];
        bar.setOnMousePressed(e -> { if (e.getButton()!=MouseButton.PRIMARY || isControl(e.getTarget())) return; drag[0]=e.getSceneX(); drag[1]=e.getSceneY(); });
        bar.setOnMouseDragged(e -> { if (e.getButton()!=MouseButton.PRIMARY || stage.isMaximized() || isControl(e.getTarget())) return; stage.setX(e.getScreenX()-drag[0]); stage.setY(e.getScreenY()-drag[1]); });
        bar.setOnMouseClicked(e -> { if (e.getButton()==MouseButton.PRIMARY && e.getClickCount()==2 && !isControl(e.getTarget())) stage.setMaximized(!stage.isMaximized()); });
        return bar;
    }

    private static Button chromeButton(String text, String tooltip) { Button b = new Button(text); b.getStyleClass().add("window-control"); b.setTooltip(new Tooltip(tooltip)); return b; }
    private static boolean isControl(Object target) {
        if (!(target instanceof javafx.scene.Node node)) return false;
        for (javafx.scene.Node current=node; current!=null; current=current.getParent()) if (current.getStyleClass().contains("window-control")) return true;
        return false;
    }
}
