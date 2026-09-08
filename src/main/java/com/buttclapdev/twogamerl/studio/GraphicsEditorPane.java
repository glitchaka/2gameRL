package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.TileDef;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

final class GraphicsEditorPane extends SplitPane {
    private static final double PREVIEW_LIMIT = 640;

    private final StudioApp app;
    private final ListView<Asset> assets = new ListView<>();
    private final Canvas preview = new Canvas();
    private final ComboBox<String> previewZoom = new ComboBox<>();
    private final ListView<TileDef> tiles = new ListView<>();
    private final VBox tileInspector = new VBox(10);
    private final Button regionButton = new Button("Definir regiones de spritesheet");
    private Asset previewAsset;

    GraphicsEditorPane(StudioApp app) {
        this.app = app;
        getItems().addAll(left(), center(), right());
        setDividerPositions(.2, .7);
        refresh();
    }

    private Node left() {
        VBox box = new VBox(8, title("ASSETS"), assets);
        box.getStyleClass().add("side-panel"); box.setPadding(new Insets(12)); box.setPrefWidth(320); VBox.setVgrow(assets, Priority.ALWAYS);

        Button importImage = new Button("Importar imagen");
        Button importSheet = new Button("Importar spritesheet"); importSheet.getStyleClass().add("primary-button");
        Button pixel = new Button("Pixel Lab");
        regionButton.setDisable(true);
        importImage.setOnAction(e -> importAssets());
        importSheet.setOnAction(e -> importSpritesheet());
        pixel.setOnAction(e -> new PixelArtEditor().show(app.owner(), a -> {
            String key=unique(a.key); app.project().getAssets().put(key,new Asset(key,key,a.data)); app.changed(); refreshAssets(key);
        }));
        regionButton.setOnAction(e -> defineRegionsForSelected());
        importImage.setMaxWidth(Double.MAX_VALUE); importSheet.setMaxWidth(Double.MAX_VALUE); pixel.setMaxWidth(Double.MAX_VALUE); regionButton.setMaxWidth(Double.MAX_VALUE);
        box.getChildren().addAll(importImage, importSheet, pixel, regionButton,
                hint("Una spritesheet se guarda UNA sola vez. Cada sprite es una región virtual x/y/ancho/alto. Puedes definir regiones por cuadrícula o detectarlas por contorno; no se generan PNG separados."));
        return box;
    }

    private Node center() {
        previewZoom.getItems().addAll("Auto pixel", "1×", "2×", "4×", "8×", "16×"); previewZoom.setValue("Auto pixel");
        previewZoom.setOnAction(e -> redrawPreview());
        Label zoomLabel=new Label("Vista"); HBox toolbar=new HBox(8,zoomLabel,previewZoom); toolbar.setPadding(new Insets(8,12,0,12));
        StackPane shell=new StackPane(preview); shell.getStyleClass().add("canvas-shell"); shell.setPadding(new Insets(28));
        ScrollPane scroll=new ScrollPane(shell); scroll.setFitToWidth(true); scroll.setFitToHeight(true); scroll.setPannable(true); scroll.getStyleClass().add("editor-scroll");
        VBox center=new VBox(8,toolbar,scroll); VBox.setVgrow(scroll,Priority.ALWAYS);
        assets.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{showPreview(b);regionButton.setDisable(b==null||sourceFor(b)==null);});
        return center;
    }

    private void showPreview(Asset asset){previewAsset=asset;redrawPreview();}

    private void redrawPreview(){
        Asset asset=previewAsset;GraphicsContext g=preview.getGraphicsContext2D();g.setImageSmoothing(false);g.clearRect(0,0,preview.getWidth(),preview.getHeight());
        if(asset==null||app.image(asset.key)==null){preview.setWidth(1);preview.setHeight(1);return;}
        int nativeW=app.assetWidth(asset.key),nativeH=app.assetHeight(asset.key);if(nativeW<=0||nativeH<=0){preview.setWidth(1);preview.setHeight(1);return;}
        int scale=selectedScale(nativeW,nativeH);double w=Math.max(1,Math.rint(nativeW*scale)),h=Math.max(1,Math.rint(nativeH*scale));preview.setWidth(w);preview.setHeight(h);g=preview.getGraphicsContext2D();g.setImageSmoothing(false);g.clearRect(0,0,w,h);app.drawAsset(g,asset.key,0,0,w,h);
    }

    private int selectedScale(int width,int height){String value=previewZoom.getValue();if(value!=null&&!value.startsWith("Auto")){try{return Integer.parseInt(value.replace("×",""));}catch(Exception ignored){}}
        int scale=1;while(scale<16&&width*scale*2<=PREVIEW_LIMIT&&height*scale*2<=PREVIEW_LIMIT)scale*=2;return scale;
    }

    private Node right(){
        VBox box=new VBox(8,title("TILES"),tiles,tileInspector);box.getStyleClass().add("side-panel");box.setPadding(new Insets(12));box.setPrefWidth(390);VBox.setVgrow(tiles,Priority.ALWAYS);
        tiles.getSelectionModel().selectedItemProperty().addListener((o,a,b)->rebuildInspector());
        Button add=new Button("＋ Tile");add.setOnAction(e->{int id=app.project().nextTileId();TileDef t=new TileDef(id,"Tile "+id,new java.awt.Color(80,88,104),true,"");app.project().getTiles().put(id,t);app.changed();refresh();tiles.getSelectionModel().select(t);});box.getChildren().add(add);return box;
    }

    private void refresh(){
        Asset selectedAsset=assets.getSelectionModel().getSelectedItem();TileDef selectedTile=tiles.getSelectionModel().getSelectedItem();
        assets.setItems(FXCollections.observableArrayList(app.project().getAssets().values()));tiles.setItems(FXCollections.observableArrayList(app.project().getTiles().values()));
        if(selectedAsset!=null)assets.getSelectionModel().select(app.project().getAssets().get(selectedAsset.key));else if(!assets.getItems().isEmpty())assets.getSelectionModel().selectFirst();
        if(selectedTile!=null)tiles.getSelectionModel().select(app.project().getTiles().get(selectedTile.id));else if(!tiles.getItems().isEmpty())tiles.getSelectionModel().selectFirst();
        regionButton.setDisable(assets.getSelectionModel().getSelectedItem()==null);rebuildInspector();
    }

    private void refreshAssets(String selectKey){assets.setItems(FXCollections.observableArrayList(app.project().getAssets().values()));Asset asset=app.project().getAssets().get(selectKey);if(asset!=null)assets.getSelectionModel().select(asset);showPreview(asset);regionButton.setDisable(asset==null);rebuildInspector();}

    private void rebuildInspector(){
        tileInspector.getChildren().clear();TileDef t=tiles.getSelectionModel().getSelectedItem();if(t==null)return;
        TextField name=new TextField(t.name);CheckBox walk=new CheckBox("Transitable");walk.setSelected(t.walkable);ColorPicker color=new ColorPicker(Color.rgb(t.color.getRed(),t.color.getGreen(),t.color.getBlue(),t.color.getAlpha()/255.0));
        ComboBox<String>sprite=new ComboBox<>();sprite.getItems().add("(sin imagen)");sprite.getItems().addAll(app.project().getDrawableAssetKeys());sprite.setValue(t.assetKey.isBlank()?"(sin imagen)":t.assetKey);
        Runnable apply=()->{t.name=name.getText().isBlank()?"Tile "+t.id:name.getText();t.walkable=walk.isSelected();Color c=color.getValue();t.color=new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());t.assetKey="(sin imagen)".equals(sprite.getValue())?"":sprite.getValue();app.changed();tiles.refresh();};
        name.setOnAction(e->apply.run());name.focusedProperty().addListener((o,a,b)->{if(!b)apply.run();});walk.setOnAction(e->apply.run());color.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());
        GridPane form=form();row(form,0,"Nombre",name);row(form,1,"Sprite/región",sprite);form.add(walk,1,2);form.add(color,1,3);
        tileInspector.getChildren().addAll(form,hint("El tile referencia una imagen completa o una región virtual. El render usa nearest-neighbor: no se aplica blur al pixel-art."));
    }

    private void importAssets(){
        FileChooser fc=chooser("Importar gráficos");List<java.io.File>files=fc.showOpenMultipleDialog(app.owner());if(files==null)return;String last=null;
        for(java.io.File f:files){try{String key=unique(f.getName());app.project().getAssets().put(key,new Asset(key,f.getName(),Files.readAllBytes(f.toPath())));last=key;}catch(Exception ex){app.error("No se pudo importar "+f.getName(),ex.getMessage());}}
        if(last!=null){app.changed();refreshAssets(last);app.status("Imagen importada completa. Para hojas usa 'Importar spritesheet'.");}
    }

    private void importSpritesheet(){
        FileChooser fc=chooser("Importar spritesheet");java.io.File file=fc.showOpenDialog(app.owner());if(file==null)return;
        try{
            byte[]data=Files.readAllBytes(file.toPath());String sourceKey=unique(file.getName());Asset source=new Asset(sourceKey,file.getName(),data,true);
            if(source.image()==null){app.error("Spritesheet inválida","El archivo no pudo decodificarse como imagen.");return;}
            javafx.scene.image.Image image=new javafx.scene.image.Image(new ByteArrayInputStream(data),0,0,true,false);
            SpritesheetSliceDialog.show(app.owner(),source,image,regions->{
                if(regions==null||regions.isEmpty())return;
                app.project().getAssets().put(source.key,source);List<String>added=new ArrayList<>();
                for(Asset r:regions){String key=unique(r.key);Asset region=new Asset(key,key,source.key,r.regionX,r.regionY,r.regionWidth,r.regionHeight);app.project().getAssets().put(key,region);added.add(key);}
                app.changed();refreshAssets(added.getFirst());app.status("Spritesheet registrada una sola vez con "+added.size()+" regiones virtuales.");
            });
        }catch(Exception ex){app.error("No se pudo importar la spritesheet",ex.getMessage());}
    }

    private void defineRegionsForSelected(){
        Asset selected=assets.getSelectionModel().getSelectedItem();Asset source=sourceFor(selected);if(source==null||source.image()==null)return;
        javafx.scene.image.Image image=app.image(source.key);if(image==null)return;
        SpritesheetSliceDialog.show(app.owner(),source,image,regions->{
            if(regions==null||regions.isEmpty())return;List<String>added=new ArrayList<>();
            if(selected==source&&source.data!=null&&source.sourceOnly==false)source.sourceOnly=true;
            for(Asset r:regions){String key=unique(r.key);Asset region=new Asset(key,key,source.key,r.regionX,r.regionY,r.regionWidth,r.regionHeight);app.project().getAssets().put(key,region);added.add(key);}
            app.changed();refreshAssets(added.getFirst());app.status("Añadidas "+added.size()+" regiones virtuales; la textura original no se duplicó.");
        });
    }

    private Asset sourceFor(Asset asset){if(asset==null)return null;if(asset.isRegion())return app.project().getAssets().get(asset.sourceAssetKey);return asset.data==null?null:asset;}
    private FileChooser chooser(String title){FileChooser fc=new FileChooser();fc.setTitle(title);fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes","*.png","*.jpg","*.jpeg","*.gif"));return fc;}
    private String unique(String base){String clean=base.replaceAll("[^A-Za-z0-9._-]","_");String key=clean;int n=2;while(app.project().getAssets().containsKey(key))key=n+++"_"+clean;return key;}
    private static Label hint(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(90);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}
}
