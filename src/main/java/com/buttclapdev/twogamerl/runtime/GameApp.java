package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.AppIcon;
import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.ScreenMode;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.InputStream;
import java.nio.file.*;

public final class GameApp extends Application {
    @Override public void start(Stage stage) throws Exception {
        GameProject project=loadBundledProject();
        if(project.getScreenMode()==ScreenMode.FULLSCREEN_BORDERLESS)stage.initStyle(StageStyle.UNDECORATED);
        GameView view=new GameView(project);
        int scale=3;
        Scene scene=new Scene(view,Math.max(640,project.getLogicalWidth()*scale),Math.max(360,project.getLogicalHeight()*scale));
        var css=GameApp.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());
        stage.getIcons().add(AppIcon.create(256));
        stage.setTitle(project.getTitle());stage.setScene(scene);stage.setMinWidth(Math.min(720,Math.max(320,project.getLogicalWidth())));stage.setMinHeight(Math.min(480,Math.max(180,project.getLogicalHeight())));
        scene.setOnKeyPressed(e->{if(e.getCode()==KeyCode.F11){stage.setFullScreen(!stage.isFullScreen());e.consume();}});
        stage.show();
        if(project.getScreenMode()!=ScreenMode.WINDOWED)stage.setFullScreen(true);
        stage.setOnHidden(e->view.stop());
        view.requestFocus();
    }

    private GameProject loadBundledProject() throws Exception {
        String external=System.getProperty("twogamerl.project","");
        if(!external.isBlank()&&Files.exists(Path.of(external)))return ProjectIO.load(Path.of(external));
        try{Path code=Path.of(GameApp.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();Path dir=Files.isDirectory(code)?code:code.getParent();Path adjacent=dir.resolve("game.2grl");if(Files.exists(adjacent))return ProjectIO.load(adjacent);}catch(Exception ignored){}
        try(InputStream in=GameApp.class.getResourceAsStream("/game.2grl")){if(in!=null)return ProjectIO.load(in);}return GameProject.createDefault();
    }

    public static void main(String[]args){launch(args);}
}
