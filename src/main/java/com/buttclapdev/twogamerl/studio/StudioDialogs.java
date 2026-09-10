package com.buttclapdev.twogamerl.studio;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Optional;

/** Studio-owned modal dialogs. No OS/native title bars. */
final class StudioDialogs {
    private StudioDialogs(){}

    static void info(Stage owner,String title,String message){message(owner,title,message,false);}
    static void error(Stage owner,String title,String message){message(owner,title,message,true);}

    private static void message(Stage owner,String title,String message,boolean error){
        Stage stage=base(owner,title);Label heading=new Label(title==null?"":title);heading.getStyleClass().add("panel-title");Label body=new Label(message==null?"":message);body.setWrapText(true);body.setMaxWidth(520);if(error)body.setStyle("-fx-text-fill:#e77d78;");Button ok=new Button("Aceptar");ok.getStyleClass().add("primary-button");ok.setOnAction(e->stage.close());Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);VBox root=new VBox(12,heading,body,new HBox(8,spacer,ok));root.setPadding(new Insets(18));Scene scene=new Scene(root,Math.max(420,Math.min(620,180+(message==null?0:message.length()*2.2))),190);stage.setScene(scene);StudioStageChrome.install(stage,scene,title);stage.showAndWait();
    }

    static Optional<String> prompt(Stage owner,String title,String label,String initial){
        Stage stage=base(owner,title);Label heading=new Label(title==null?"":title);heading.getStyleClass().add("panel-title");Label caption=new Label(label==null?"Valor":label);TextField field=new TextField(initial==null?"":initial);Button cancel=new Button("Cancelar"),ok=new Button("Aceptar");ok.getStyleClass().add("primary-button");final String[]result={null};ok.setOnAction(e->{result[0]=field.getText();stage.close();});cancel.setOnAction(e->stage.close());field.setOnAction(e->ok.fire());Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);VBox root=new VBox(10,heading,caption,field,new HBox(8,spacer,cancel,ok));root.setPadding(new Insets(18));Scene scene=new Scene(root,520,190);stage.setScene(scene);StudioStageChrome.install(stage,scene,title);stage.setOnShown(e->{field.requestFocus();field.selectAll();});stage.showAndWait();return Optional.ofNullable(result[0]);
    }

    static boolean confirm(Stage owner,String title,String message,String acceptLabel){
        Stage stage=base(owner,title);Label heading=new Label(title==null?"":title);heading.getStyleClass().add("panel-title");Label body=new Label(message==null?"":message);body.setWrapText(true);body.setMaxWidth(520);Button cancel=new Button("Cancelar"),ok=new Button(acceptLabel==null||acceptLabel.isBlank()?"Aceptar":acceptLabel);ok.getStyleClass().add("primary-button");final boolean[]result={false};ok.setOnAction(e->{result[0]=true;stage.close();});cancel.setOnAction(e->stage.close());Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox actions=new HBox(8,spacer,cancel,ok);actions.setAlignment(Pos.CENTER_RIGHT);VBox root=new VBox(12,heading,body,actions);root.setPadding(new Insets(18));Scene scene=new Scene(root,540,205);stage.setScene(scene);StudioStageChrome.install(stage,scene,title);stage.showAndWait();return result[0];
    }

    private static Stage base(Stage owner,String title){Stage stage=new Stage();if(owner!=null)stage.initOwner(owner);stage.initModality(Modality.APPLICATION_MODAL);stage.setTitle(title==null?"2gameRL Studio":title);stage.setResizable(false);return stage;}
}
