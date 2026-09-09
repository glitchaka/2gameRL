package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.AssetCategory;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.*;

final class GraphicsBrowserEnhancements {
    private static final String INSTALLED="2rl.browser.2.2.1";
    private GraphicsBrowserEnhancements(){}

    static void install(GraphicsEditorPane pane,StudioApp app){
        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;
        pane.getProperties().put(INSTALLED,true);
        VBox imageBox=findImageBox(pane);if(imageBox==null)return;
        @SuppressWarnings("unchecked") ListView<Asset>list=(ListView<Asset>)findFirst(imageBox,ListView.class);if(list==null)return;
        int listIndex=imageBox.getChildren().indexOf(list);if(listIndex<0)return;

        TreeView<Object>tree=new TreeView<>();tree.setShowRoot(false);tree.getStyleClass().add("asset-folder-tree");
        tree.setCellFactory(v->new TreeCell<>(){@Override protected void updateItem(Object item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?null:item instanceof Asset a?a.toString():item.toString());}});
        StackPane browser=new StackPane(list,tree);VBox.setVgrow(browser,Priority.ALWAYS);tree.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);list.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        ToggleGroup modes=new ToggleGroup();ToggleButton folderMode=new ToggleButton("Carpetas"),listMode=new ToggleButton("Lista");folderMode.setToggleGroup(modes);listMode.setToggleGroup(modes);folderMode.setSelected(true);folderMode.getStyleClass().add("asset-browser-view-toggle");listMode.getStyleClass().add("asset-browser-view-toggle");
        Label modeLabel=new Label("Vista");modeLabel.getStyleClass().add("muted");HBox modeBar=new HBox(6,modeLabel,folderMode,listMode);modeBar.setAlignment(Pos.CENTER_LEFT);
        imageBox.getChildren().set(listIndex,browser);imageBox.getChildren().add(listIndex,modeBar);

        Runnable switchMode=()->{boolean folders=folderMode.isSelected();tree.setVisible(folders);tree.setManaged(folders);list.setVisible(!folders);list.setManaged(!folders);};folderMode.setOnAction(e->switchMode.run());listMode.setOnAction(e->switchMode.run());switchMode.run();
        Runnable rebuild=()->rebuildTree(tree,list.getItems(),app);attachListListener(list,rebuild);rebuild.run();
        tree.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null&&b.getValue() instanceof Asset asset){list.getSelectionModel().clearSelection();list.getSelectionModel().select(asset);list.scrollTo(asset);}});ContextMenu menu=new ContextMenu();MenuItem favorite=new MenuItem("Alternar favorito"),folder=new MenuItem("Mover a carpeta virtual…");favorite.setOnAction(e->{Asset a=list.getSelectionModel().getSelectedItem();if(a!=null){a.favorite=!a.favorite;app.changed();rebuild.run();}});folder.setOnAction(e->{Asset a=list.getSelectionModel().getSelectedItem();if(a!=null){TextInputDialog d=new TextInputDialog(a.folder);d.setHeaderText("Carpeta virtual (vacío = categoría automática)");d.showAndWait().ifPresent(v->{a.folder=v.trim();app.changed();rebuild.run();});}});menu.getItems().addAll(favorite,folder);list.setContextMenu(menu);tree.setContextMenu(menu);

        Button editPixel=new Button("Editar seleccionado en Pixel Lab");editPixel.setMaxWidth(Double.MAX_VALUE);editPixel.setOnAction(e->{Asset selected=list.getSelectionModel().getSelectedItem();if(selected==null){app.status("Selecciona primero una imagen o región.");return;}Asset source=selected.isRegion()?app.project().getAssets().get(selected.sourceAssetKey):selected;new PixelArtEditor().show(app.owner(),selected,source,key->!app.project().getAssets().containsKey(key),created->{app.project().getAssets().put(created.key,created);app.changed();app.status("Sprite nuevo guardado: "+created.key+". El original se conserva.");app.refreshWorkspace();});});
        int hintIndex=Math.max(0,imageBox.getChildren().size()-1);imageBox.getChildren().add(hintIndex,editPixel);
    }

    private static void attachListListener(ListView<Asset>list,Runnable rebuild){
        final ListChangeListener<Asset>listener=c->rebuild.run();
        if(list.getItems()!=null)list.getItems().addListener(listener);
        ChangeListener<ObservableList<Asset>>itemsListener=(o,oldItems,newItems)->{if(oldItems!=null)oldItems.removeListener(listener);if(newItems!=null)newItems.addListener(listener);rebuild.run();};
        list.itemsProperty().addListener(itemsListener);
    }

    private static void rebuildTree(TreeView<Object>tree,List<Asset>assets,StudioApp app){
        TreeItem<Object>root=new TreeItem<>("Assets");root.setExpanded(true);TreeItem<Object>favorites=new TreeItem<>("★ Favoritos");favorites.setExpanded(true);root.getChildren().add(favorites);LinkedHashMap<String,TreeItem<Object>>virtual=new LinkedHashMap<>();LinkedHashMap<String,TreeItem<Object>>categories=new LinkedHashMap<>();for(String name:List.of("Imágenes","Spritesheets","Sprites","Fondos","UI","Tilesets","VFX","Retratos")){TreeItem<Object>folder=new TreeItem<>(name);folder.setExpanded(true);categories.put(name,folder);root.getChildren().add(folder);}Map<String,TreeItem<Object>>sourceFolders=new LinkedHashMap<>();for(Asset asset:assets){if(asset.favorite)favorites.getChildren().add(new TreeItem<>(asset));if(asset.folder!=null&&!asset.folder.isBlank()){TreeItem<Object>vf=virtual.computeIfAbsent(asset.folder,k->{TreeItem<Object>x=new TreeItem<>("📁 "+k);x.setExpanded(true);root.getChildren().add(1,x);return x;});vf.getChildren().add(new TreeItem<>(asset));continue;}String category=categoryName(asset);TreeItem<Object>folder=categories.getOrDefault(category,categories.get("Imágenes"));if(asset.isRegion()){String sourceKey=asset.sourceAssetKey,id=category+"/"+sourceKey;TreeItem<Object>sourceFolder=sourceFolders.get(id);if(sourceFolder==null){Asset source=app.project().getAssets().get(sourceKey);String display=source==null?sourceKey:(source.sourceName==null?source.key:source.sourceName);sourceFolder=new TreeItem<>(display);sourceFolder.setExpanded(true);folder.getChildren().add(sourceFolder);sourceFolders.put(id,sourceFolder);}sourceFolder.getChildren().add(new TreeItem<>(asset));}else folder.getChildren().add(new TreeItem<>(asset));}root.getChildren().removeIf(f->f.getChildren().isEmpty());tree.setRoot(root);
    }

    private static String categoryName(Asset a){if(a.sourceOnly&&!a.isRegion())return"Spritesheets";if(a.isRegion())return"Sprites";AssetCategory c=a.category==null?AssetCategory.IMAGE:a.category;return switch(c){case SPRITESHEET->"Spritesheets";case SPRITE->"Sprites";case BACKGROUND->"Fondos";case UI->"UI";case TILESET->"Tilesets";case VFX->"VFX";case PORTRAIT->"Retratos";default->"Imágenes";};}

    private static VBox findImageBox(GraphicsEditorPane pane){if(pane.getItems().isEmpty())return null;TabPane tabs=findFirst(pane.getItems().getFirst(),TabPane.class);if(tabs==null)return null;for(Tab tab:tabs.getTabs())if("Imágenes".equals(tab.getText())&&tab.getContent() instanceof VBox box)return box;return null;}
    private static <T extends Node>T findFirst(Node root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof javafx.scene.Parent parent)for(Node child:parent.getChildrenUnmodifiable()){T found=findFirst(child,type);if(found!=null)return found;}return null;}
}
