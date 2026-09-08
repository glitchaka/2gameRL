package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.InputStream;
import java.nio.file.*;

public final class GameApp extends Application {
    @Override public void start(Stage stage) throws Exception {
        GameProject project = loadBundledProject();
        GameView view = new GameView(project);
        Scene scene = new Scene(view, 960, 640);
        var css = GameApp.class.getResource("/com/buttclapdev/twogamerl/studio.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle(project.getTitle()); stage.setScene(scene); stage.setMinWidth(720); stage.setMinHeight(480); stage.show();
        stage.setOnHidden(e -> view.stop());
    }

    private GameProject loadBundledProject() throws Exception {
        String external = System.getProperty("twogamerl.project", "");
        if (!external.isBlank() && Files.exists(Path.of(external))) return ProjectIO.load(Path.of(external));
        try {
            Path code = Path.of(GameApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            Path dir = Files.isDirectory(code) ? code : code.getParent(); Path adjacent = dir.resolve("game.2grl");
            if (Files.exists(adjacent)) return ProjectIO.load(adjacent);
        } catch (Exception ignored) {}
        try (InputStream in = GameApp.class.getResourceAsStream("/game.2grl")) { if (in != null) return ProjectIO.load(in); }
        return GameProject.createDefault();
    }

    public static void main(String[] args){ launch(args); }
}
