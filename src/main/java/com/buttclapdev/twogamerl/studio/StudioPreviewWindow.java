package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.AppIcon;
import com.buttclapdev.twogamerl.runtime.GameView;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/** Preview window with 2gameRL chrome only; never delegates decoration to the OS. */
final class StudioPreviewWindow {
    private StudioPreviewWindow(){}

    static void show(StudioApp app){
        Stage preview=new Stage(StageStyle.UNDECORATED);
        preview.initOwner(app.owner());
        preview.setTitle("Probar · "+app.project().getTitle());
        try{preview.getIcons().setAll(AppIcon.create(256));}catch(Exception ignored){}

        GameView view=new GameView(app.project());
        BorderPane root=new BorderPane(view);
        HBox titleBar=titleBar(preview);
        root.setTop(titleBar);
        root.setStyle("-fx-background-color:#08090b;-fx-border-color:#32363d;-fx-border-width:1;");

        Scene scene=new Scene(root,Math.max(640,app.project().getLogicalWidth()*3),Math.max(360,app.project().getLogicalHeight()*3));
        scene.setOnKeyPressed(e->{
            if(e.getCode()==KeyCode.F11){preview.setFullScreen(!preview.isFullScreen());e.consume();}
            else if(e.getCode()==KeyCode.F10){preview.close();e.consume();}
        });
        preview.fullScreenProperty().addListener((o,a,b)->root.setTop(b?null:titleBar));
        preview.setScene(scene);
        preview.setMinWidth(480);preview.setMinHeight(300);
        preview.setOnShown(e->{preview.setFullScreen(true);view.requestFocus();});
        preview.setOnHidden(e->view.stop());
        preview.show();
    }

    private static HBox titleBar(Stage stage){
        ImageView icon=new ImageView(AppIcon.create(18));icon.setFitWidth(18);icon.setFitHeight(18);icon.setPreserveRatio(true);
        Label title=new Label();title.textProperty().bind(stage.titleProperty());title.setStyle("-fx-text-fill:#e8e8e8;-fx-font-weight:bold;");
        Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);
        Button min=chrome("—"),max=chrome("▢"),close=chrome("×");
        min.setOnAction(e->stage.setIconified(true));
        max.setOnAction(e->stage.setMaximized(!stage.isMaximized()));
        close.setOnAction(e->stage.close());
        close.setStyle("-fx-background-color:transparent;-fx-text-fill:#e8e8e8;-fx-border-color:transparent;-fx-background-radius:0;");
        close.setOnMouseEntered(e->close.setStyle("-fx-background-color:#b43b3b;-fx-text-fill:white;-fx-border-color:transparent;-fx-background-radius:0;"));
        close.setOnMouseExited(e->close.setStyle("-fx-background-color:transparent;-fx-text-fill:#e8e8e8;-fx-border-color:transparent;-fx-background-radius:0;"));
        stage.maximizedProperty().addListener((o,a,b)->max.setText(b?"❐":"▢"));
        HBox bar=new HBox(8,icon,title,spacer,min,max,close);bar.setAlignment(Pos.CENTER_LEFT);bar.setStyle("-fx-background-color:#111318;-fx-padding:0 0 0 8;-fx-border-color:transparent transparent #2d3138 transparent;-fx-min-height:32;-fx-pref-height:32;");
        final double[]drag={0,0};
        bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||e.getTarget() instanceof Button)return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});
        bar.setOnMouseDragged(e->{if(e.getButton()!=MouseButton.PRIMARY||stage.isMaximized()||e.getTarget() instanceof Button)return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});
        bar.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!(e.getTarget() instanceof Button))stage.setMaximized(!stage.isMaximized());});
        return bar;
    }

    private static Button chrome(String text){Button b=new Button(text);b.setMinSize(42,31);b.setPrefSize(42,31);b.setMaxSize(42,31);b.setStyle("-fx-background-color:transparent;-fx-text-fill:#e8e8e8;-fx-border-color:transparent;-fx-background-radius:0;");b.setOnMouseEntered(e->b.setStyle("-fx-background-color:#242830;-fx-text-fill:white;-fx-border-color:transparent;-fx-background-radius:0;"));b.setOnMouseExited(e->b.setStyle("-fx-background-color:transparent;-fx-text-fill:#e8e8e8;-fx-border-color:transparent;-fx-background-radius:0;"));return b;}
}
