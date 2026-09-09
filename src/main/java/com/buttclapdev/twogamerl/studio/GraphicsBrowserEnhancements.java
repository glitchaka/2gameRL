package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Installs the desktop-style resource explorer into the Graphics workspace. */
final class GraphicsBrowserEnhancements {
    private static final String INSTALLED="2rl.browser.desktop.2.3";
    private GraphicsBrowserEnhancements(){}

    static void install(GraphicsEditorPane pane,StudioApp app){
        if(findFirst(pane,AssetExplorerPane.class)!=null)return;
        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;
        pane.getProperties().put(INSTALLED,true);
        VBox imageBox=findImageBox(pane);if(imageBox==null)return;
        @SuppressWarnings("unchecked") ListView<Asset> list=(ListView<Asset>)findFirst(imageBox,ListView.class);if(list==null)return;
        TextField search=findFirst(imageBox,TextField.class);if(search==null)return;
        int listIndex=imageBox.getChildren().indexOf(list);if(listIndex<0)return;

        AssetExplorerPane explorer=new AssetExplorerPane(app,list,search);
        VBox.setVgrow(explorer,Priority.ALWAYS);
        imageBox.getChildren().set(listIndex,explorer);

        Button editPixel=new Button("Editar seleccionado en Pixel Lab");
        editPixel.setMaxWidth(Double.MAX_VALUE);
        editPixel.setOnAction(e->{
            Asset selected=list.getSelectionModel().getSelectedItem();
            if(selected==null){app.status("Selecciona primero una imagen o región.");return;}
            Asset source=selected.isRegion()?app.project().getAssets().get(selected.sourceAssetKey):selected;
            new PixelArtEditor().show(app.owner(),selected,source,key->!app.project().getAssets().containsKey(key),created->{
                app.project().getAssets().put(created.key,created);app.changed();app.status("Sprite nuevo guardado: "+created.key+". El original se conserva.");app.refreshWorkspace();explorer.refresh();
            });
        });
        HBox quickActions=new HBox(6,editPixel);quickActions.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(editPixel,Priority.ALWAYS);
        int hintIndex=Math.max(0,imageBox.getChildren().size()-1);imageBox.getChildren().add(hintIndex,quickActions);
    }

    private static VBox findImageBox(GraphicsEditorPane pane){
        if(pane.getItems().isEmpty())return null;
        TabPane tabs=findFirst(pane.getItems().getFirst(),TabPane.class);if(tabs==null)return null;
        for(Tab tab:tabs.getTabs())if("Imágenes".equals(tab.getText())&&tab.getContent() instanceof VBox box)return box;
        return null;
    }
    private static <T extends Node>T findFirst(Node root,Class<T>type){
        if(type.isInstance(root))return type.cast(root);
        if(root instanceof javafx.scene.Parent parent)for(Node child:parent.getChildrenUnmodifiable()){T found=findFirst(child,type);if(found!=null)return found;}
        return null;
    }
}
