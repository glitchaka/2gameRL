package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.TileDef;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.nio.file.Files;
import java.util.List;

final class GraphicsEditorPane extends SplitPane {
    private final StudioApp app;
    private final ListView<Asset> assets=new ListView<>();
    private final ImageView preview=new ImageView();
    private final ListView<TileDef> tiles=new ListView<>();
    private final VBox tileInspector=new VBox(10);

    GraphicsEditorPane(StudioApp app){this.app=app;getItems().addAll(left(),center(),right());setDividerPositions(.2,.7);refresh();}
    private Node left(){VBox box=new VBox(8,title("ASSETS"),assets);box.getStyleClass().add("side-panel");box.setPadding(new Insets(12));box.setPrefWidth(280);VBox.setVgrow(assets,Priority.ALWAYS);Button importB=new Button("Importar imagen"),pixel=new Button("Pixel Lab");pixel.getStyleClass().add("primary-button");importB.setOnAction(e->importAssets());pixel.setOnAction(e->new PixelArtEditor().show(app.owner(),a->{String key=unique(a.key);app.project().getAssets().put(key,new Asset(key,key,a.data));app.changed();refresh();assets.getSelectionModel().select(app.project().getAssets().get(key));}));box.getChildren().add(new HBox(6,importB,pixel));return box;}
    private Node center(){preview.setPreserveRatio(true);preview.setFitWidth(480);preview.setFitHeight(480);StackPane shell=new StackPane(preview);shell.getStyleClass().add("canvas-shell");shell.setPadding(new Insets(28));assets.getSelectionModel().selectedItemProperty().addListener((o,a,b)->preview.setImage(b==null?null:app.image(b.key)));return shell;}
    private Node right(){VBox box=new VBox(8,title("TILES"),tiles,tileInspector);box.getStyleClass().add("side-panel");box.setPadding(new Insets(12));box.setPrefWidth(370);VBox.setVgrow(tiles,Priority.ALWAYS);tiles.getSelectionModel().selectedItemProperty().addListener((o,a,b)->rebuildInspector());Button add=new Button("＋ Tile");add.setOnAction(e->{int id=app.project().nextTileId();TileDef t=new TileDef(id,"Tile "+id,new java.awt.Color(80,88,104),true,"");app.project().getTiles().put(id,t);app.changed();refresh();tiles.getSelectionModel().select(t);});box.getChildren().add(add);return box;}
    private void refresh(){assets.setItems(FXCollections.observableArrayList(app.project().getAssets().values()));tiles.setItems(FXCollections.observableArrayList(app.project().getTiles().values()));if(!tiles.getItems().isEmpty())tiles.getSelectionModel().selectFirst();rebuildInspector();}
    private void rebuildInspector(){tileInspector.getChildren().clear();TileDef t=tiles.getSelectionModel().getSelectedItem();if(t==null)return;TextField name=new TextField(t.name);CheckBox walk=new CheckBox("Transitable");walk.setSelected(t.walkable);ColorPicker color=new ColorPicker(Color.rgb(t.color.getRed(),t.color.getGreen(),t.color.getBlue(),t.color.getAlpha()/255.0));ComboBox<String>sprite=new ComboBox<>();sprite.getItems().add("(sin imagen)");sprite.getItems().addAll(app.project().getAssets().keySet());sprite.setValue(t.assetKey.isBlank()?"(sin imagen)":t.assetKey);Runnable apply=()->{t.name=name.getText().isBlank()?"Tile "+t.id:name.getText();t.walkable=walk.isSelected();Color c=color.getValue();t.color=new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());t.assetKey="(sin imagen)".equals(sprite.getValue())?"":sprite.getValue();app.changed();tiles.refresh();};name.setOnAction(e->apply.run());name.focusedProperty().addListener((o,a,b)->{if(!b)apply.run();});walk.setOnAction(e->apply.run());color.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());GridPane form=form();row(form,0,"Nombre",name);row(form,1,"Imagen",sprite);form.add(walk,1,2);form.add(color,1,3);tileInspector.getChildren().add(form);}
    private void importAssets(){FileChooser fc=new FileChooser();fc.setTitle("Importar gráficos");fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes","*.png","*.jpg","*.jpeg","*.gif"));List<java.io.File>files=fc.showOpenMultipleDialog(app.owner());if(files==null)return;for(java.io.File f:files)try{String key=unique(f.getName());app.project().getAssets().put(key,new Asset(key,f.getName(),Files.readAllBytes(f.toPath())));}catch(Exception ex){app.error("No se pudo importar "+f.getName(),ex.getMessage());}app.changed();refresh();}
    private String unique(String base){String clean=base.replaceAll("[^A-Za-z0-9._-]","_");String key=clean;int n=2;while(app.project().getAssets().containsKey(key))key=n+++"_"+clean;return key;}
    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(75);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}
}
