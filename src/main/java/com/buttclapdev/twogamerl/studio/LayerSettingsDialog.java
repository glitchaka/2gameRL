package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.PhysicsLayerDef;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Window;

import java.util.*;

final class LayerSettingsDialog {
    private LayerSettingsDialog() {}

    static void show(Window owner, GameProject project, Runnable changed) {
        Dialog<Void> dialog = new Dialog<>();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle("Capas y matriz de colisiones");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefSize(900, 650);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(
                new Tab("Capas visuales", renderLayers(project, changed)),
                new Tab("Capas físicas", physicsLayers(project, changed))
        );
        dialog.getDialogPane().setContent(tabs);
        dialog.showAndWait();
    }

    private static Pane renderLayers(GameProject project, Runnable changed) {
        BorderPane root = new BorderPane(); root.setPadding(new Insets(12));
        ListView<String> list = new ListView<>(FXCollections.observableArrayList(project.getRenderLayers()));
        list.setPrefWidth(300);
        Label help = hint("Las capas visuales controlan delante/detrás de las entidades. El orden de la lista va de fondo a frente.");

        Button add = new Button("＋ Capa"), rename = new Button("Renombrar"), remove = new Button("Eliminar"), back = new Button("Enviar atrás"), front = new Button("Traer delante");
        add.setOnAction(e -> {
            TextInputDialog d = new TextInputDialog("Nueva capa"); d.setHeaderText("Nombre de la capa visual");
            d.showAndWait().ifPresent(name -> {
                String clean = name.trim(); if (clean.isBlank() || project.getRenderLayers().contains(clean)) return;
                project.getRenderLayers().add(clean); list.getItems().add(clean); list.getSelectionModel().select(clean); changed.run();
            });
        });
        rename.setOnAction(e -> {
            String old = list.getSelectionModel().getSelectedItem(); if (old == null) return;
            TextInputDialog d = new TextInputDialog(old); d.setHeaderText("Renombrar capa visual");
            d.showAndWait().ifPresent(name -> {
                String clean=name.trim(); if(clean.isBlank()||(!clean.equals(old)&&project.getRenderLayers().contains(clean)))return;
                int idx=project.getRenderLayers().indexOf(old); project.getRenderLayers().set(idx,clean);
                for (var level:project.getLevels().values()) for (var entity:level.entities) if (old.equals(entity.renderLayer)) entity.renderLayer=clean;
                list.getItems().set(idx,clean); list.getSelectionModel().select(idx); changed.run();
            });
        });
        remove.setOnAction(e -> {
            String old=list.getSelectionModel().getSelectedItem(); if(old==null||project.getRenderLayers().size()<=1)return;
            int idx=project.getRenderLayers().indexOf(old); project.getRenderLayers().remove(old); list.getItems().remove(old);
            String fallback=project.getRenderLayers().get(Math.max(0,Math.min(idx-1,project.getRenderLayers().size()-1)));
            for(var level:project.getLevels().values())for(var entity:level.entities)if(old.equals(entity.renderLayer))entity.renderLayer=fallback;
            changed.run();
        });
        back.setOnAction(e -> moveRender(project,list,-1,changed));
        front.setOnAction(e -> moveRender(project,list,1,changed));

        VBox controls = new VBox(8, help, new HBox(7, add, rename, remove), new HBox(7, back, front)); controls.setPadding(new Insets(10,0,0,0));
        root.setCenter(list); root.setBottom(controls); return root;
    }

    private static void moveRender(GameProject project, ListView<String> list, int direction, Runnable changed) {
        int i=list.getSelectionModel().getSelectedIndex(), j=i+direction; if(i<0||j<0||j>=project.getRenderLayers().size())return;
        Collections.swap(project.getRenderLayers(),i,j); FXCollections.copy(list.getItems(),FXCollections.observableArrayList(project.getRenderLayers())); list.getSelectionModel().select(j); changed.run();
    }

    private static Pane physicsLayers(GameProject project, Runnable changed) {
        BorderPane root = new BorderPane(); root.setPadding(new Insets(12));
        VBox shell = new VBox(10);
        Label help = hint("BoxCollider2D colisiona por defecto. Esta matriz solo sirve para hacer que dos categorías se ignoren entre sí.");
        ListView<String> list = new ListView<>(FXCollections.observableArrayList(project.getPhysicsLayers().keySet())); list.setPrefHeight(150);
        GridPane matrix = new GridPane(); matrix.setHgap(5); matrix.setVgap(5);

        Runnable rebuild = () -> rebuildMatrix(project,matrix,changed);
        Button add = new Button("＋ Capa física"), rename = new Button("Renombrar"), remove = new Button("Eliminar");
        add.setOnAction(e -> {
            TextInputDialog d=new TextInputDialog("Layer");d.setHeaderText("Nombre de la capa física");d.showAndWait().ifPresent(name->{String clean=name.trim();if(clean.isBlank()||project.getPhysicsLayers().containsKey(clean))return;PhysicsLayerDef layer=new PhysicsLayerDef(clean);for(String existing:project.getPhysicsLayers().keySet()){layer.collidesWith.add(existing);project.getPhysicsLayers().get(existing).collidesWith.add(clean);}layer.collidesWith.add(clean);project.getPhysicsLayers().put(clean,layer);list.getItems().add(clean);list.getSelectionModel().select(clean);rebuild.run();changed.run();});
        });
        rename.setOnAction(e -> {
            String old=list.getSelectionModel().getSelectedItem();if(old==null)return;TextInputDialog d=new TextInputDialog(old);d.setHeaderText("Renombrar capa física");d.showAndWait().ifPresent(name->{String clean=name.trim();if(clean.isBlank()||(!clean.equals(old)&&project.getPhysicsLayers().containsKey(clean)))return;LinkedHashMap<String,PhysicsLayerDef> copy=new LinkedHashMap<>();for(var entry:project.getPhysicsLayers().entrySet()){String key=entry.getKey().equals(old)?clean:entry.getKey();PhysicsLayerDef layer=entry.getValue();if(layer.name.equals(old))layer.name=clean;if(layer.collidesWith.remove(old))layer.collidesWith.add(clean);copy.put(key,layer);}project.getPhysicsLayers().clear();project.getPhysicsLayers().putAll(copy);for(var level:project.getLevels().values()){for(var entity:level.entities)if(old.equals(entity.physicsLayer))entity.physicsLayer=clean;for(var tile:level.tileLayers)if(old.equals(tile.physicsLayer))tile.physicsLayer=clean;}int idx=list.getSelectionModel().getSelectedIndex();list.setItems(FXCollections.observableArrayList(project.getPhysicsLayers().keySet()));list.getSelectionModel().select(idx);rebuild.run();changed.run();});
        });
        remove.setOnAction(e -> {
            String old=list.getSelectionModel().getSelectedItem();if(old==null||project.getPhysicsLayers().size()<=1)return;project.getPhysicsLayers().remove(old);for(var layer:project.getPhysicsLayers().values())layer.collidesWith.remove(old);String fallback=project.getPhysicsLayers().keySet().iterator().next();for(var level:project.getLevels().values()){for(var entity:level.entities)if(old.equals(entity.physicsLayer))entity.physicsLayer=fallback;for(var tile:level.tileLayers)if(old.equals(tile.physicsLayer))tile.physicsLayer=fallback;}list.setItems(FXCollections.observableArrayList(project.getPhysicsLayers().keySet()));rebuild.run();changed.run();
        });

        shell.getChildren().addAll(help,list,new HBox(7,add,rename,remove),new Separator(),new Label("MATRIZ DE COLISIONES"),matrix);
        ScrollPane scroll=new ScrollPane(shell);scroll.setFitToWidth(true);root.setCenter(scroll);rebuild.run();return root;
    }

    private static void rebuildMatrix(GameProject project, GridPane grid, Runnable changed) {
        grid.getChildren().clear();
        List<String> names=new ArrayList<>(project.getPhysicsLayers().keySet());
        for(int x=0;x<names.size();x++){Label h=new Label(names.get(x));h.setRotate(-45);h.setMinWidth(70);grid.add(h,x+1,0);}
        for(int y=0;y<names.size();y++){
            String a=names.get(y);grid.add(new Label(a),0,y+1);
            for(int x=0;x<names.size();x++){
                String b=names.get(x);CheckBox cb=new CheckBox();cb.setSelected(project.canCollide(a,b));cb.setOnAction(e->{project.setCollision(a,b,cb.isSelected());changed.run();});grid.add(cb,x+1,y+1);
            }
        }
    }

    private static Label hint(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
}
