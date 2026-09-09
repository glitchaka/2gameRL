package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.lang.reflect.Field;
import java.util.*;

final class GraphicsBrowserEnhancements {
    private static final String INSTALLED="2rl.browser.2.2.2";
    private record ResourceItem(String kind,String name,String detail,Object value){@Override public String toString(){return detail==null||detail.isBlank()?name:name+"  ·  "+detail;}}
    private GraphicsBrowserEnhancements(){}

    static void install(GraphicsEditorPane pane,StudioApp app){
        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;
        pane.getProperties().put(INSTALLED,true);
        VBox imageBox=findImageBox(pane);if(imageBox==null)return;
        @SuppressWarnings("unchecked") ListView<Asset>list=(ListView<Asset>)findFirst(imageBox,ListView.class);if(list==null)return;
        installImageViews(imageBox,list,app);
        installGlobalBrowser(pane,app);
    }

    private static void installImageViews(VBox imageBox,ListView<Asset>list,StudioApp app){
        int listIndex=imageBox.getChildren().indexOf(list);if(listIndex<0)return;
        TreeView<Object>tree=new TreeView<>();tree.setShowRoot(false);tree.getStyleClass().add("asset-folder-tree");tree.setCellFactory(v->new TreeCell<>(){@Override protected void updateItem(Object item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?null:item instanceof Asset a?a.toString():item.toString());}});
        StackPane browser=new StackPane(list,tree);VBox.setVgrow(browser,Priority.ALWAYS);tree.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);list.setMaxSize(Double.MAX_VALUE,Double.MAX_VALUE);
        ToggleGroup modes=new ToggleGroup();ToggleButton folderMode=new ToggleButton("Carpetas"),listMode=new ToggleButton("Lista");folderMode.setToggleGroup(modes);listMode.setToggleGroup(modes);folderMode.setSelected(true);folderMode.getStyleClass().add("asset-browser-view-toggle");listMode.getStyleClass().add("asset-browser-view-toggle");
        Label modeLabel=new Label("Vista");modeLabel.getStyleClass().add("muted");HBox modeBar=new HBox(6,modeLabel,folderMode,listMode);modeBar.setAlignment(Pos.CENTER_LEFT);
        imageBox.getChildren().set(listIndex,browser);imageBox.getChildren().add(listIndex,modeBar);
        Runnable switchMode=()->{boolean folders=folderMode.isSelected();tree.setVisible(folders);tree.setManaged(folders);list.setVisible(!folders);list.setManaged(!folders);};folderMode.setOnAction(e->switchMode.run());listMode.setOnAction(e->switchMode.run());switchMode.run();
        Runnable rebuild=()->rebuildAssetTree(tree,list.getItems(),app);attachListListener(list,rebuild);rebuild.run();
        tree.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null&&b.getValue() instanceof Asset asset){list.getSelectionModel().clearSelection();list.getSelectionModel().select(asset);list.scrollTo(asset);}});
        Button editPixel=new Button("Editar seleccionado en Pixel Lab");editPixel.setMaxWidth(Double.MAX_VALUE);editPixel.setOnAction(e->{Asset selected=list.getSelectionModel().getSelectedItem();if(selected==null){app.status("Selecciona primero una imagen o región.");return;}Asset source=selected.isRegion()?app.project().getAssets().get(selected.sourceAssetKey):selected;new PixelArtEditor().show(app.owner(),selected,source,key->!app.project().getAssets().containsKey(key),created->{app.project().getAssets().put(created.key,created);app.changed();app.status("Sprite nuevo guardado: "+created.key+". El original se conserva.");app.refreshWorkspace();});});
        int hintIndex=Math.max(0,imageBox.getChildren().size()-1);imageBox.getChildren().add(hintIndex,editPixel);
    }

    private static void installGlobalBrowser(GraphicsEditorPane pane,StudioApp app){
        if(pane.getItems().isEmpty())return;TabPane leftTabs=findFirst(pane.getItems().getFirst(),TabPane.class);if(leftTabs==null||leftTabs.getTabs().stream().anyMatch(t->"Todos los recursos".equals(t.getText())))return;
        TextField search=new TextField();search.setPromptText("Buscar cualquier recurso cargado…");
        ListView<ResourceItem>flat=new ListView<>();TreeView<ResourceItem>tree=new TreeView<>();tree.setShowRoot(false);tree.getStyleClass().add("asset-folder-tree");
        ToggleGroup modes=new ToggleGroup();ToggleButton folders=new ToggleButton("Carpetas"),list=new ToggleButton("Lista");folders.setToggleGroup(modes);list.setToggleGroup(modes);folders.setSelected(true);StackPane views=new StackPane(tree,flat);VBox.setVgrow(views,Priority.ALWAYS);
        Runnable switchView=()->{tree.setVisible(folders.isSelected());tree.setManaged(folders.isSelected());flat.setVisible(list.isSelected());flat.setManaged(list.isSelected());};folders.setOnAction(e->switchView.run());list.setOnAction(e->switchView.run());switchView.run();
        Runnable rebuild=()->rebuildResources(app,search.getText(),tree,flat);search.textProperty().addListener((o,a,b)->rebuild.run());rebuild.run();
        ChangeListener<TreeItem<ResourceItem>>treeSelection=(o,a,b)->{if(b!=null&&b.getValue()!=null)route(pane,b.getValue());};tree.getSelectionModel().selectedItemProperty().addListener(treeSelection);flat.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(b!=null)route(pane,b);});
        HBox controls=new HBox(6,new Label("Vista"),folders,list);controls.setAlignment(Pos.CENTER_LEFT);Label info=new Label("Assets, Prefabs, animaciones, controladores, partículas, paletas, TextStyles y fuentes en un único navegador.");info.getStyleClass().add("muted");info.setWrapText(true);VBox box=new VBox(8,search,controls,views,info);box.setPadding(new javafx.geometry.Insets(2));
        leftTabs.getTabs().add(0,new Tab("Todos los recursos",box));leftTabs.getSelectionModel().selectFirst();
        pane.getProperties().put("2rl.resourceBrowser.refresh",rebuild);
    }

    private static void rebuildResources(StudioApp app,String raw,TreeView<ResourceItem>tree,ListView<ResourceItem>flat){
        String q=raw==null?"":raw.trim().toLowerCase(Locale.ROOT);LinkedHashMap<String,List<ResourceItem>>groups=new LinkedHashMap<>();
        addGroup(groups,"Imágenes",app.project().getAssets().values().stream().map(a->new ResourceItem(assetKind(a),a.sourceName==null?a.key:a.sourceName,a.key,a)).toList());
        addGroup(groups,"Fuentes",app.project().getFonts().values().stream().map(f->new ResourceItem("Fuente",f.sourceName,f.key,f)).toList());
        addGroup(groups,"Pinceles / Prefabs",app.project().getPrefabs().values().stream().map(p->new ResourceItem("Prefab",p.name,p.key,p)).toList());
        addGroup(groups,"Animation Clips",app.project().getAnimationClips().values().stream().map(c->new ResourceItem("AnimationClip",c.name,c.key,c)).toList());
        addGroup(groups,"Animator Controllers",app.project().getAnimatorControllers().values().stream().map(c->new ResourceItem("AnimatorController",c.name,c.key,c)).toList());
        addGroup(groups,"Partículas",unique(app.project().getParticlePresets().values()).stream().map(p->new ResourceItem("ParticlePreset",p.name,p.key,p)).toList());
        addGroup(groups,"Paletas",unique(app.project().getPalettes().values()).stream().map(p->new ResourceItem("Paleta",p.name,p.key,p)).toList());
        addGroup(groups,"TextStyles",unique(app.project().getTextStyles().values()).stream().map(s->new ResourceItem("TextStyle",s.name,s.key,s)).toList());
        ArrayList<ResourceItem>all=new ArrayList<>();TreeItem<ResourceItem>root=new TreeItem<>();root.setExpanded(true);
        for(var entry:groups.entrySet()){List<ResourceItem>items=entry.getValue().stream().filter(i->q.isBlank()||(i.name()+" "+i.detail()+" "+i.kind()).toLowerCase(Locale.ROOT).contains(q)).toList();if(items.isEmpty())continue;TreeItem<ResourceItem>folder=new TreeItem<>(new ResourceItem("Carpeta",entry.getKey(),items.size()+" recursos",null));folder.setExpanded(true);for(ResourceItem item:items){folder.getChildren().add(new TreeItem<>(item));all.add(item);}root.getChildren().add(folder);}tree.setRoot(root);flat.setItems(FXCollections.observableArrayList(all));
    }

    private static void route(GraphicsEditorPane pane,ResourceItem item){if(item==null||item.value()==null)return;Object v=item.value();String field=switch(v){case Asset a->"assets";case FontAsset f->"fonts";case PrefabDef p->"prefabs";case AnimationClip c->"clips";case AnimatorController c->"controllers";case ParticlePreset p->"particles";case PaletteAsset p->"palettes";case TextStyle s->"textStyles";default->"";};if(field.isBlank())return;ListView<?>list=field(pane,field,ListView.class);if(list!=null){@SuppressWarnings({"rawtypes","unchecked"})ListView raw=list;raw.getSelectionModel().clearSelection();raw.getSelectionModel().select(v);raw.scrollTo(v);}if(v instanceof Asset||v instanceof FontAsset){TabPane left=findFirst(pane.getItems().getFirst(),TabPane.class);if(left!=null)selectTab(left,v instanceof FontAsset?"Fuentes":"Imágenes");}else{TabPane right=findFirst(pane.getItems().getLast(),TabPane.class);if(right!=null)selectTab(right,v instanceof PrefabDef?"Pinceles / Prefabs":v instanceof AnimationClip||v instanceof AnimatorController?"Animación":v instanceof TextStyle?"Textos":"Efectos");}}
    private static void selectTab(TabPane tabs,String text){for(Tab t:tabs.getTabs())if(text.equals(t.getText())){tabs.getSelectionModel().select(t);return;}}

    private static void attachListListener(ListView<Asset>list,Runnable rebuild){final ListChangeListener<Asset>listener=c->rebuild.run();if(list.getItems()!=null)list.getItems().addListener(listener);ChangeListener<ObservableList<Asset>>itemsListener=(o,oldItems,newItems)->{if(oldItems!=null)oldItems.removeListener(listener);if(newItems!=null)newItems.addListener(listener);rebuild.run();};list.itemsProperty().addListener(itemsListener);}
    private static void rebuildAssetTree(TreeView<Object>tree,List<Asset>assets,StudioApp app){TreeItem<Object>root=new TreeItem<>("Assets");root.setExpanded(true);LinkedHashMap<String,TreeItem<Object>>categories=new LinkedHashMap<>();for(String name:List.of("Imágenes","Spritesheets","Sprites","Fondos","UI","Tilesets","VFX","Retratos")){TreeItem<Object>folder=new TreeItem<>(name);folder.setExpanded(true);categories.put(name,folder);root.getChildren().add(folder);}Map<String,TreeItem<Object>>sourceFolders=new LinkedHashMap<>();for(Asset asset:assets){String category=categoryName(asset);TreeItem<Object>folder=categories.getOrDefault(category,categories.get("Imágenes"));if(asset.isRegion()){String sourceKey=asset.sourceAssetKey,id=category+"/"+sourceKey;TreeItem<Object>sourceFolder=sourceFolders.get(id);if(sourceFolder==null){Asset source=app.project().getAssets().get(sourceKey);String display=source==null?sourceKey:(source.sourceName==null?source.key:source.sourceName);sourceFolder=new TreeItem<>(display);sourceFolder.setExpanded(true);folder.getChildren().add(sourceFolder);sourceFolders.put(id,sourceFolder);}sourceFolder.getChildren().add(new TreeItem<>(asset));}else folder.getChildren().add(new TreeItem<>(asset));}root.getChildren().removeIf(f->f.getChildren().isEmpty());tree.setRoot(root);}
    private static String assetKind(Asset a){return a.isRegion()?"Sprite/Región":a.sourceOnly?"Spritesheet":categoryName(a);}
    private static String categoryName(Asset a){if(a.sourceOnly&&!a.isRegion())return"Spritesheets";if(a.isRegion())return"Sprites";AssetCategory c=a.category==null?AssetCategory.IMAGE:a.category;return switch(c){case SPRITESHEET->"Spritesheets";case SPRITE->"Sprites";case BACKGROUND->"Fondos";case UI->"UI";case TILESET->"Tilesets";case VFX->"VFX";case PORTRAIT->"Retratos";default->"Imágenes";};}
    private static void addGroup(Map<String,List<ResourceItem>>groups,String name,List<ResourceItem>items){groups.put(name,items);}
    private static <T>List<T>unique(Collection<T>values){Set<T>seen=Collections.newSetFromMap(new IdentityHashMap<>());ArrayList<T>out=new ArrayList<>();for(T v:values)if(v!=null&&seen.add(v))out.add(v);return out;}
    private static VBox findImageBox(GraphicsEditorPane pane){if(pane.getItems().isEmpty())return null;TabPane tabs=findFirst(pane.getItems().getFirst(),TabPane.class);if(tabs==null)return null;for(Tab tab:tabs.getTabs())if("Imágenes".equals(tab.getText())&&tab.getContent() instanceof VBox box)return box;return null;}
    private static <T>T field(Object owner,String name,Class<T>type){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);Object v=f.get(owner);return type.isInstance(v)?type.cast(v):null;}catch(Exception e){return null;}}
    private static <T extends Node>T findFirst(Node root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof Parent parent)for(Node child:parent.getChildrenUnmodifiable()){T found=findFirst(child,type);if(found!=null)return found;}return null;}
}
