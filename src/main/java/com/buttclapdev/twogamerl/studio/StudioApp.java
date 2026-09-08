package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.export.NativeExporter;
import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.runtime.GameView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StudioApp extends Application {
    private Stage stage;
    private GameProject project = GameProject.createDefault();
    private Path currentFile;
    private boolean dirty;
    private final BorderPane root = new BorderPane();
    private final StackPane workspace = new StackPane();
    private final Label status = new Label("Listo");
    private final Map<String, Image> imageCache = new HashMap<>();
    private ToggleButton sceneWorkspaceButton;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        root.getStyleClass().add("studio-root");
        root.setTop(buildTop());
        root.setCenter(workspace);
        status.getStyleClass().add("status-bar");
        status.setMaxWidth(Double.MAX_VALUE);
        status.setPadding(new Insets(5, 12, 6, 12));
        root.setBottom(status);
        showSceneWorkspace();
        Scene scene = new Scene(root, 1480, 900);
        var css = getClass().getResource("/com/buttclapdev/twogamerl/studio.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.setMinWidth(1120);
        stage.setMinHeight(720);
        updateTitle();
        stage.show();
        stage.setOnCloseRequest(e -> { if (!confirmDiscard()) e.consume(); });
    }

    GameProject project() { return project; }
    Stage owner() { return stage; }
    void changed() { dirty = true; imageCache.clear(); updateTitle(); }
    void status(String text) { status.setText(text); }
    Image image(String key) {
        if (key == null || key.isBlank()) return null;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        Asset a = project.getAssets().get(key);
        if (a == null || a.data == null) return null;
        try { Image image = new Image(new ByteArrayInputStream(a.data)); imageCache.put(key, image); return image; }
        catch (Exception e) { return null; }
    }

    private Node buildTop() {
        Menu file = new Menu("Archivo");
        MenuItem n = item("Nuevo", "Ctrl+N", e -> newProject());
        MenuItem open = item("Abrir…", "Ctrl+O", e -> openProject());
        MenuItem save = item("Guardar", "Ctrl+S", e -> save(false));
        MenuItem saveAs = item("Guardar como…", "Ctrl+Shift+S", e -> save(true));
        MenuItem export = item("Exportar juego…", "Ctrl+E", e -> exportGame());
        MenuItem exit = item("Salir", "", e -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
        file.getItems().addAll(n, open, new SeparatorMenuItem(), save, saveAs, new SeparatorMenuItem(), export, new SeparatorMenuItem(), exit);
        Menu help = new Menu("Ayuda");
        MenuItem quick = new MenuItem("Guía de 60 segundos"); quick.setOnAction(e -> quickGuide()); help.getItems().add(quick);
        MenuBar menuBar = new MenuBar(file, help);

        Button bNew = button("＋ Nuevo", e -> newProject());
        Button bOpen = button("Abrir", e -> openProject());
        Button bSave = button("Guardar", e -> save(false));
        Button play = button("▶ Probar", e -> playPreview()); play.getStyleClass().add("primary-button");
        Button exportB = button("Exportar", e -> exportGame());
        ToggleGroup group = new ToggleGroup();
        sceneWorkspaceButton = workspaceButton("Escena", group, true);
        ToggleButton menus = workspaceButton("Menús", group, false);
        ToggleButton graphics = workspaceButton("Gráficos", group, false);
        sceneWorkspaceButton.setOnAction(e -> showSceneWorkspace());
        menus.setOnAction(e -> showMenuWorkspace());
        graphics.setOnAction(e -> showGraphicsWorkspace());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(7, bNew, bOpen, bSave, new Separator(), sceneWorkspaceButton, menus, graphics, spacer, play, exportB);
        toolbar.setAlignment(Pos.CENTER_LEFT); toolbar.setPadding(new Insets(8,12,9,12)); toolbar.getStyleClass().add("main-toolbar");
        return new VBox(menuBar, toolbar);
    }

    private MenuItem item(String text, String accelerator, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        MenuItem item = new MenuItem(text);
        if (!accelerator.isBlank()) item.setAccelerator(javafx.scene.input.KeyCombination.keyCombination(accelerator));
        item.setOnAction(handler); return item;
    }
    private Button button(String text, javafx.event.EventHandler<javafx.event.ActionEvent> handler) { Button b = new Button(text); b.setOnAction(handler); return b; }
    private ToggleButton workspaceButton(String text, ToggleGroup group, boolean selected) {
        ToggleButton b = new ToggleButton(text); b.setToggleGroup(group); b.setSelected(selected); b.getStyleClass().add("workspace-button"); return b;
    }

    private void showSceneWorkspace() {
        workspace.getChildren().setAll(new SceneEditorPane(this));
        status("Seleccionar es el modo seguro: clic selecciona, arrastre mueve y doble clic abre Script.");
    }
    private void showMenuWorkspace() {
        workspace.getChildren().setAll(new MenuEditorPane(this));
        status("Diseña las pantallas y arrastra sus botones directamente sobre el lienzo.");
    }
    private void showGraphicsWorkspace() {
        workspace.getChildren().setAll(new GraphicsEditorPane(this));
        status("Importa imágenes o crea placeholders con Pixel Lab; luego asígnalos a tiles u objetos.");
    }

    private void newProject() {
        if (!confirmDiscard()) return;
        project = GameProject.createDefault(); currentFile = null; dirty = false; imageCache.clear(); updateTitle();
        sceneWorkspaceButton.setSelected(true); showSceneWorkspace();
    }
    private void openProject() {
        if (!confirmDiscard()) return;
        FileChooser chooser = projectChooser("Abrir proyecto");
        java.io.File f = chooser.showOpenDialog(stage); if (f == null) return;
        try {
            project = ProjectIO.load(f.toPath()); currentFile = f.toPath(); dirty = false; imageCache.clear(); updateTitle();
            sceneWorkspaceButton.setSelected(true); showSceneWorkspace(); status("Proyecto cargado: " + f.getName());
        } catch (Exception e) { error("No se pudo abrir", e.getMessage()); }
    }
    private boolean save(boolean choose) {
        try {
            if (currentFile == null || choose) {
                FileChooser chooser = projectChooser("Guardar proyecto");
                java.io.File f = chooser.showSaveDialog(stage); if (f == null) return false;
                Path p = f.toPath(); if (!p.getFileName().toString().toLowerCase().endsWith(".2grl")) p = p.resolveSibling(p.getFileName() + ".2grl");
                currentFile = p;
            }
            ProjectIO.save(project, currentFile); dirty = false; updateTitle(); status("Guardado: " + currentFile); return true;
        } catch (Exception e) { error("No se pudo guardar", e.getMessage()); return false; }
    }
    private FileChooser projectChooser(String title) {
        FileChooser fc = new FileChooser(); fc.setTitle(title);
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Proyecto 2gameRL (*.2grl)", "*.2grl")); return fc;
    }

    private void playPreview() {
        Stage preview = new Stage();
        GameView view = new GameView(project);
        TextArea console = new TextArea(); console.setEditable(false); console.setPrefRowCount(5);
        view.setLogger(s -> Platform.runLater(() -> console.appendText(s + "\n")));
        VBox box = new VBox(view, console); VBox.setVgrow(view, Priority.ALWAYS);
        Scene scene = new Scene(box, 1000, 760); applyCss(scene);
        preview.setScene(scene); preview.setTitle("Probar · " + project.getTitle()); preview.show(); preview.setOnHidden(e -> view.stop());
    }

    private void exportGame() {
        if (dirty && !save(false)) return;
        DirectoryChooser dc = new DirectoryChooser(); dc.setTitle("Carpeta donde exportar el juego");
        java.io.File dir = dc.showDialog(stage); if (dir == null) return;
        Stage progress = new Stage(); progress.initOwner(stage); progress.initModality(Modality.APPLICATION_MODAL); progress.setTitle("Exportar");
        TextArea log = new TextArea(); log.setEditable(false);
        ProgressIndicator indicator = new ProgressIndicator(); Label title = new Label("EXPORTANDO JUEGO"); title.getStyleClass().add("panel-title");
        VBox box = new VBox(10, new HBox(10, indicator, title), log); box.setPadding(new Insets(16));
        Scene sc = new Scene(box, 720, 460); applyCss(sc); progress.setScene(sc);
        Task<NativeExporter.Result> task = new Task<>() {
            @Override protected NativeExporter.Result call() { return NativeExporter.export(project.deepCopy(), dir.toPath(), s -> Platform.runLater(() -> log.appendText(s + "\n"))); }
        };
        task.setOnSucceeded(e -> {
            progress.close(); NativeExporter.Result r = task.getValue();
            if (r.success()) info("Juego exportado", "Aplicación: " + r.application() + "\nZIP portable: " + r.zip());
            else error("Falló la exportación", r.message());
        });
        task.setOnFailed(e -> { progress.close(); error("Falló la exportación", task.getException().toString()); });
        Thread thread = new Thread(task, "2gameRL-export"); thread.setDaemon(true); thread.start(); progress.showAndWait();
    }

    private boolean confirmDiscard() {
        if (!dirty) return true;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Hay cambios sin guardar. ¿Quieres descartarlos?", ButtonType.CANCEL, ButtonType.OK);
        a.setHeaderText("Cambios sin guardar"); return a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }
    private void quickGuide() {
        info("2gameRL Studio en 60 segundos",
                "1. Escena: Seleccionar es el modo normal. Tile pinta el suelo.\n" +
                "2. Doble clic en un objeto: abre su Script.\n" +
                "3. Inspector > Comportamientos: añade Rigidbody2D, Collider, PlayerController, Patrol, etc.\n" +
                "4. Gráficos: importa imágenes o usa Pixel Lab.\n" +
                "5. ▶ Probar ejecuta el proyecto sin exportarlo.\n" +
                "6. Exportar crea la aplicación autocontenida y un ZIP portable.");
    }
    private void updateTitle() { if (stage != null) stage.setTitle("2gameRL Studio · " + project.getTitle() + (dirty ? "  ●" : "")); }
    private void applyCss(Scene scene) { var css = getClass().getResource("/com/buttclapdev/twogamerl/studio.css"); if (css != null) scene.getStylesheets().add(css.toExternalForm()); }
    void error(String header, String message) { Alert a = new Alert(Alert.AlertType.ERROR, message == null ? "Error desconocido" : message, ButtonType.OK); a.setHeaderText(header); a.showAndWait(); }
    void info(String header, String message) { Alert a = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK); a.setHeaderText(header); a.showAndWait(); }
    public static void main(String[] args) { launch(args); }
}
