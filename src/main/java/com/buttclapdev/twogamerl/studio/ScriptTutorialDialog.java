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
                    Cada objeto de una escena puede tener su propio 2GameScript. Selecciona el objeto en la jerarquía o en el lienzo y abre la pestaña Script. Un doble clic sobre el objeto también abre esa pestaña.

                    Todo comando ejecutable debe vivir dentro de un bloque `on ... end`. Usa `#` para comentarios. Si un texto contiene espacios, escríbelo entre comillas.

                    Antes de probar el juego, pulsa `Validar script`: el editor te indicará el número de línea de cualquier error de sintaxis.
                    """,
                    """
                    # Mi primer 2GameScript
                    on start
                      log "Objeto iniciado"
                    end

                    on click
                      log "Me hicieron clic"
                    end
                    """,
                    "`start` se ejecuta una vez al cargar la escena; `click` responde al clic sobre el objeto."
            ),
            new Lesson(
                    "2 · Eventos",
                    """
                    Los scripts reaccionan a eventos del motor:

                    • start — una vez al cargar la escena y crear el objeto.
                    • update — cada frame mientras el objeto está activo.
                    • click — un clic sobre el objeto.
                    • doubleClick — doble clic sobre el objeto.
                    • collision — al comenzar una colisión con otro collider o al chocar contra un tile no transitable.
                    • trigger — una entidad con Trigger entra en contacto con un objeto que tenga PlayerController.

                    Puedes tener varios bloques de eventos en el mismo script. Cada bloque debe cerrarse con `end`.
                    """,
                    """
                    on start
                      log "Listo"
                    end

                    on collision
                      log "Choque"
                    end
                    """,
                    "No pongas comandos fuera de un bloque `on ... end`."
            ),
            new Lesson(
                    "3 · Movimiento",
                    """
                    `move x y` desplaza inmediatamente el objeto en unidades del mundo.

                    `velocity x y` define una velocidad continua. `teleport x y` coloca directamente el objeto en una posición.

                    `bounce` invierte la velocidad actual y está pensado para usarse dentro de `collision`.
                    """,
                    """
                    on start
                      velocity 3 2
                    end

                    on collision
                      bounce
                    end
                    """,
                    "Las coordenadas del mundo se expresan en unidades de tile, no en píxeles de pantalla."
            ),
            new Lesson(
                    "4 · Teclado",
                    """
                    `ifKey TECLA comando` ejecuta el comando mientras la tecla permanezca presionada.

                    `ifPressed TECLA comando` lo ejecuta solamente al comenzar la pulsación. Úsalo para disparos, abrir puertas o acciones que no deban repetirse cada frame.

                    Nombres habituales: W, A, S, D, UP, DOWN, LEFT, RIGHT, SPACE, ENTER, SHIFT y ESCAPE.
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
                    "Para un jugador normal es más cómodo añadir PlayerController y ajustar `speed`."
            ),
            new Lesson(
                    "5 · Variables y propiedades",
                    """
                    `setVar nombre valor` guarda estado dentro del objeto. `addVar nombre número` suma un valor numérico; si la variable no existe o no es numérica, parte desde 0.

                    `set propiedad valor` modifica propiedades del objeto en ejecución. Propiedades directas: x, y, width, height, enabled y layer. Cualquier otro nombre se guarda como variable.

                    2GameScript todavía no incorpora comparadores generales ni bloques if/else basados en variables.
                    """,
                    """
                    on start
                      setVar monedas 0
                      set layer 3
                    end

                    on click
                      addVar monedas 1
                      set width 1.25
                      set height 1.25
                    end
                    """,
                    "`set enabled false` desactiva la entidad durante la ejecución actual."
            ),
            new Lesson(
                    "6 · Escenas y sprites",
                    """
                    `loadScene id` carga otra escena usando su ID.

                    `setSprite asset` cambia el gráfico del objeto por la clave exacta de un asset existente en el proyecto.

                    Si quieres un portal sin escribir código, usa el componente ScenePortal: targetScene carga la escena y targetX/targetY colocan al PlayerController en el destino.
                    """,
                    """
                    on doubleClick
                      setSprite puerta-abierta.png
                      loadScene nivel-2
                    end
                    """,
                    "Usa el ID real de la escena, no necesariamente su nombre visible."
            ),
            new Lesson(
                    "7 · Colisiones y triggers",
                    """
                    BoxCollider2D bloquea contra tiles no transitables y contra otros BoxCollider2D sólidos.

                    Trigger no bloquea por sí mismo: dispara `trigger` cuando entra un objeto con PlayerController. `once=true` hace que se ejecute una sola vez durante esa instancia de la escena.

                    `destroy` elimina el objeto durante la ejecución actual.
                    """,
                    """
                    # Objeto recogible: añade también Trigger
                    on trigger
                      log "Objeto recogido"
                      addVar recogidos 1
                      destroy
                    end
                    """,
                    "Los eventos de contacto se disparan al entrar en contacto, no cada frame mientras dos entidades permanecen superpuestas."
            ),
            new Lesson(
                    "8 · Componentes integrados",
                    """
                    Los componentes se añaden desde Inspector > Comportamientos y se ejecutan sin necesidad de script. Todos tienen `enabled`; usa false para desactivar ese comportamiento.

                    Rigidbody2D: gravityScale, drag y maxSpeed afectan al movimiento. En entidades nuevas gravityScale empieza en 1. El jugador de muestra lo usa en 0 porque es top-down.

                    BoxCollider2D: width, height y solid. Bloquea tanto contra mapa como contra otros colliders sólidos.

                    PlayerController: speed y allowArrows. Añade control WASD/flechas inmediatamente.

                    Patrol: axis, distance y speed. Recorre desde la posición inicial hasta la distancia indicada y vuelve; al chocar invierte dirección.

                    ScenePortal: targetScene, targetX y targetY. Carga la escena y coloca al jugador en esas coordenadas.

                    Trigger: dispara `on trigger`; once=true lo consume tras la primera entrada.

                    Health: max y current.

                    DamageOnContact: resta damage a Health al comenzar el contacto y destruye el objetivo si llega a 0.

                    Clickable: enabled=false bloquea click y doubleClick para esa entidad.
                    """,
                    """
                    # Complemento opcional para una entidad física
                    on collision
                      log "La física detectó una colisión"
                    end
                    """,
                    "No necesitas repetir en script lo que ya resuelve un componente: usa scripts para personalizar."
            ),
            new Lesson(
                    "9 · Recetas útiles",
                    """
                    Combinaciones frecuentes:

                    • Jugador top-down: PlayerController + BoxCollider2D + Rigidbody2D con gravityScale=0.
                    • Objeto con gravedad: Rigidbody2D + BoxCollider2D.
                    • Enemigo patrulla: Patrol + BoxCollider2D.
                    • Enemigo dañino: Patrol + BoxCollider2D + DamageOnContact.
                    • Objetivo dañable: Health + BoxCollider2D.
                    • Recogible: Trigger + script on trigger + destroy.
                    • Portal: ScenePortal con targetScene/targetX/targetY.
                    """,
                    """
                    # Proyectil sencillo
                    on start
                      velocity 6 0
                    end

                    on collision
                      destroy
                    end
                    """,
                    "Empieza con componentes y añade script solo donde necesites lógica específica."
            ),
            new Lesson(
                    "10 · Referencia completa",
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
                    loadScene <id>
                    setSprite <asset>
                    setVar <nombre> <valor>
                    addVar <nombre> <número>
                    set <propiedad> <valor>
                    ifKey <tecla> <comando>
                    ifPressed <tecla> <comando>

                    Errores frecuentes:
                    • comando fuera de on ... end;
                    • olvidar end;
                    • evento inexistente;
                    • texto donde se esperaba un número;
                    • ID de escena incorrecto;
                    • clave de sprite inexistente;
                    • usar ifKey cuando se quería una sola pulsación.
                    """,
                    """
                    on start
                      log "Script válido"
                    end
                    """,
                    "El botón `Validar script` usa el mismo parser que ejecutará el runtime."
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
        navigation.setPrefWidth(245);

        TextField search = new TextField();
        search.setPromptText("Buscar en el tutorial…");
        Label section = new Label("TUTORIAL 2GAMESCRIPT"); section.getStyleClass().add("panel-title");
        VBox left = new VBox(8, section, search, navigation);
        left.setPadding(new Insets(14));
        left.getStyleClass().add("side-panel");
        VBox.setVgrow(navigation, Priority.ALWAYS);

        Label heading = new Label(); heading.getStyleClass().add("tutorial-heading");
        Label explanation = new Label(); explanation.setWrapText(true); explanation.getStyleClass().add("tutorial-explanation");
        TextArea code = new TextArea(); code.setEditable(false); code.setWrapText(false); code.getStyleClass().add("tutorial-code"); code.setPrefRowCount(12);
        Label note = new Label(); note.setWrapText(true); note.getStyleClass().add("tutorial-note");
        Label status = new Label("Selecciona una lección."); status.getStyleClass().add("muted");

        Button copy = new Button("Copiar ejemplo");
        Button insert = new Button("Insertar en el script"); insert.getStyleClass().add("primary-button");
        insert.setVisible(insertHandler != null); insert.setManaged(insertHandler != null);
        Region buttonSpacer = new Region(); HBox.setHgrow(buttonSpacer, Priority.ALWAYS);
        HBox actions = new HBox(8, status, buttonSpacer, copy, insert); actions.setAlignment(Pos.CENTER_LEFT);

        VBox content = new VBox(10, heading, explanation, new Separator(), code, note, actions);
        content.setPadding(new Insets(18)); VBox.setVgrow(code, Priority.ALWAYS);
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

        navigation.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> update.run());
        search.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase(Locale.ROOT);
            visible.setAll(LESSONS.stream().filter(l -> q.isEmpty() || l.title().toLowerCase(Locale.ROOT).contains(q) || l.explanation().toLowerCase(Locale.ROOT).contains(q) || l.code().toLowerCase(Locale.ROOT).contains(q)).toList());
            if (!visible.isEmpty()) navigation.getSelectionModel().selectFirst(); else update.run();
        });

        copy.setOnAction(e -> {
            Lesson lesson = navigation.getSelectionModel().getSelectedItem();
            if (lesson == null || lesson.code().isBlank()) return;
            ClipboardContent data = new ClipboardContent(); data.putString(lesson.code().strip()); Clipboard.getSystemClipboard().setContent(data); status.setText("Ejemplo copiado al portapapeles.");
        });
        insert.setOnAction(e -> {
            Lesson lesson = navigation.getSelectionModel().getSelectedItem();
            if (lesson == null || lesson.code().isBlank() || insertHandler == null) return;
            insertHandler.accept(lesson.code().strip()); status.setText("Ejemplo insertado en el script seleccionado.");
        });

        Scene scene = new Scene(root, 1080, 720);
        var css = ScriptTutorialDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene); navigation.getSelectionModel().selectFirst(); update.run();
        if (owner != null) {
            stage.setX(owner.getX() + Math.max(24, (owner.getWidth() - 1080) / 2));
            stage.setY(owner.getY() + Math.max(24, (owner.getHeight() - 720) / 2));
        }
        stage.show();
    }

    private static HBox titleBar(Stage stage) {
        Label mark = new Label("2G"); mark.getStyleClass().add("window-app-mark");
        Label title = new Label("Tutorial de scripting · 2GameScript"); title.getStyleClass().add("window-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button minimize = chromeButton("—", "Minimizar");
        Button maximize = chromeButton("▢", "Maximizar / restaurar");
        Button close = chromeButton("×", "Cerrar"); close.getStyleClass().add("window-close");
        minimize.setOnAction(e -> stage.setIconified(true)); maximize.setOnAction(e -> stage.setMaximized(!stage.isMaximized())); close.setOnAction(e -> stage.close());
        stage.maximizedProperty().addListener((o, a, b) -> maximize.setText(b ? "❐" : "▢"));
        HBox bar = new HBox(9, mark, title, spacer, minimize, maximize, close); bar.setAlignment(Pos.CENTER_LEFT); bar.getStyleClass().add("title-bar");
        final double[] drag = new double[2];
        bar.setOnMousePressed(e -> { if (e.getButton() != MouseButton.PRIMARY || isControl(e.getTarget())) return; drag[0] = e.getSceneX(); drag[1] = e.getSceneY(); });
        bar.setOnMouseDragged(e -> { if (e.getButton() != MouseButton.PRIMARY || stage.isMaximized() || isControl(e.getTarget())) return; stage.setX(e.getScreenX() - drag[0]); stage.setY(e.getScreenY() - drag[1]); });
        bar.setOnMouseClicked(e -> { if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2 && !isControl(e.getTarget())) stage.setMaximized(!stage.isMaximized()); });
        return bar;
    }

    private static Button chromeButton(String text, String tooltip) {
        Button b = new Button(text); b.getStyleClass().add("window-control"); b.setTooltip(new Tooltip(tooltip)); return b;
    }

    private static boolean isControl(Object target) {
        if (!(target instanceof javafx.scene.Node node)) return false;
        javafx.scene.Node current = node;
        while (current != null) { if (current.getStyleClass().contains("window-control")) return true; current = current.getParent(); }
        return false;
    }
}
