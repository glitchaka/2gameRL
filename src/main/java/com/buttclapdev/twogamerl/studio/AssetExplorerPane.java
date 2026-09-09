package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.AssetCategory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.*;
import java.util.stream.Collectors;

/** Project resource browser with persistent folders and desktop file-manager semantics. */
final class AssetExplorerPane extends BorderPane {
    private enum EntryKind { FOLDER, ASSET }
    private record Entry(EntryKind kind, String path, Asset asset) {
        static Entry folder(String path){ return new Entry(EntryKind.FOLDER,path,null); }
        static Entry asset(Asset asset){ return new Entry(EntryKind.ASSET,"",asset); }
    }
    private record Place(String label,String path,boolean favorites){ @Override public String toString(){return label;} }

    private static final DataFormat ASSET_KEYS = new DataFormat("application/x-2gamerl-asset-keys");
    private static final DataFormat FOLDER_PATH = new DataFormat("application/x-2gamerl-folder-path");
    private static final String EXPLICIT_ROOT = "~";
    private static final List<String> STANDARD_FOLDERS = List.of("Imágenes","Spritesheets","Sprites","Fondos","UI","Tilesets","VFX","Retratos");

    private final StudioApp app;
    private final ListView<Asset> selectionBridge;
    private final TextField search;
    private final LinkedHashSet<String> selectedKeys = new LinkedHashSet<>();
    private final HBox breadcrumbs = new HBox(2);
    private final ListView<Place> places = new ListView<>();
    private final TableView<Entry> details = new TableView<>();
    private final VBox gridRoot = new VBox(8);
    private final FlowPane assetGrid = new FlowPane(10,10);
    private final VBox folderRows = new VBox(2);
    private final ScrollPane gridScroll = new ScrollPane(gridRoot);
    private final StackPane content = new StackPane();
    private final Label status = new Label();
    private final ToggleButton detailsMode = new ToggleButton("Lista");
    private final ToggleButton gridMode = new ToggleButton("Miniaturas");
    private final Button up = new Button("Subir");
    private final Button newFolder = new Button("Nueva carpeta");
    private final Button renameFolder = new Button("Renombrar");
    private final Button deleteFolder = new Button("Eliminar");
    private String currentFolder = "";
    private String selectedFolder = "";
    private boolean favoritesMode;
    private boolean rebuildingPlaces;

    AssetExplorerPane(StudioApp app,ListView<Asset> selectionBridge,TextField search){
        this.app=app;this.selectionBridge=selectionBridge;this.search=search;
        getStyleClass().add("asset-explorer");setFocusTraversable(true);

        ToggleGroup viewModes=new ToggleGroup();detailsMode.setToggleGroup(viewModes);gridMode.setToggleGroup(viewModes);detailsMode.setSelected(true);
        detailsMode.getStyleClass().add("compact-toggle");gridMode.getStyleClass().add("compact-toggle");
        detailsMode.setOnAction(e->{if(!detailsMode.isSelected())detailsMode.setSelected(true);rebuild();});
        gridMode.setOnAction(e->{if(!gridMode.isSelected())gridMode.setSelected(true);rebuild();});

        up.getStyleClass().add("resource-command");newFolder.getStyleClass().add("resource-command");renameFolder.getStyleClass().add("resource-command");deleteFolder.getStyleClass().add("resource-command");
        up.setOnAction(e->openFolder(parent(currentFolder)));newFolder.setOnAction(e->createFolder());renameFolder.setOnAction(e->renameSelectedFolder());deleteFolder.setOnAction(e->deleteSelectedFolder());

        HBox toolbar=new HBox(6,up,breadcrumbs,new Region(),newFolder,renameFolder,deleteFolder,new Separator(),detailsMode,gridMode);
        HBox.setHgrow(toolbar.getChildren().get(2),Priority.ALWAYS);toolbar.setAlignment(Pos.CENTER_LEFT);toolbar.getStyleClass().add("resource-toolbar");
        breadcrumbs.setAlignment(Pos.CENTER_LEFT);

        configurePlaces();configureDetails();configureGrid();
        SplitPane browser=new SplitPane(sidebar(),content);browser.setDividerPositions(.22);browser.getStyleClass().add("resource-browser-split");
        status.getStyleClass().add("resource-status");BorderPane.setMargin(status,new Insets(4,9,7,9));
        setTop(toolbar);setCenter(browser);setBottom(status);

        selectionBridge.itemsProperty().addListener((o,a,b)->rebuild());
        search.textProperty().addListener((o,a,b)->rebuild());
        setOnKeyPressed(this::handleKey);
        rebuild();
    }

    void refresh(){rebuild();}

    private Node sidebar(){
        Label title=new Label("PROYECTO");title.getStyleClass().add("resource-sidebar-title");
        VBox box=new VBox(6,title,places);box.setPadding(new Insets(8,6,8,8));box.getStyleClass().add("resource-sidebar");VBox.setVgrow(places,Priority.ALWAYS);box.setPrefWidth(190);return box;
    }

    private void configurePlaces(){
        places.getStyleClass().add("resource-places");
        places.setCellFactory(v->new ListCell<>(){
            @Override protected void updateItem(Place p,boolean empty){super.updateItem(p,empty);if(empty||p==null){setText(null);setGraphic(null);return;}setText(p.label());setGraphic(p.favorites()?starIcon(14):folderIcon(15));getStyleClass().add("resource-place-cell");
                setOnDragOver(e->{if(!p.favorites())acceptTarget(e,p.path());});setOnDragDropped(e->{if(!p.favorites())dropOnPath(e,p.path());});}
        });
        places.getSelectionModel().selectedItemProperty().addListener((o,a,b)->{if(rebuildingPlaces||b==null)return;if(b.favorites()){favoritesMode=true;currentFolder="";selectedFolder="";selectedKeys.clear();syncBridgeSelection();rebuild();}else openFolder(b.path());});
    }

    private void configureDetails(){
        details.getStyleClass().add("resource-details");details.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);details.setPlaceholder(new Label("Esta carpeta está vacía"));details.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        TableColumn<Entry,String> nameCol=new TableColumn<>("Nombre");nameCol.setPrefWidth(310);nameCol.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(entryName(d.getValue())));
        nameCol.setCellFactory(c->new TableCell<>(){@Override protected void updateItem(String value,boolean empty){super.updateItem(value,empty);if(empty||value==null){setText(null);setGraphic(null);return;}Entry e=getTableView().getItems().get(getIndex());setText(value);setGraphic(e.kind()==EntryKind.FOLDER?folderIcon(17):thumbnail(e.asset(),30,24));}});
        TableColumn<Entry,String> typeCol=new TableColumn<>("Tipo");typeCol.setPrefWidth(150);typeCol.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(d.getValue().kind()==EntryKind.FOLDER?"Carpeta":typeName(d.getValue().asset())));
        TableColumn<Entry,String> whereCol=new TableColumn<>("Ubicación");whereCol.setPrefWidth(210);whereCol.setCellValueFactory(d->new javafx.beans.property.SimpleStringProperty(entryLocation(d.getValue())));
        details.getColumns().setAll(nameCol,typeCol,whereCol);
        details.setRowFactory(tv->{TableRow<Entry> row=new TableRow<>();
            row.setOnMouseClicked(e->{if(row.isEmpty())return;Entry item=row.getItem();requestFocus();if(item.kind()==EntryKind.FOLDER){selectedFolder=item.path();selectedKeys.clear();syncBridgeSelection();if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()>=2)openFolder(item.path());}else{selectedFolder="";syncSelectedAssetsFromDetails();}updateButtons();});
            row.setOnDragDetected(e->{if(row.isEmpty())return;Entry item=row.getItem();if(item.kind()==EntryKind.FOLDER){if(!app.project().getAssetFolders().contains(item.path()))return;Dragboard db=row.startDragAndDrop(TransferMode.MOVE);ClipboardContent cc=new ClipboardContent();cc.put(FOLDER_PATH,item.path());db.setContent(cc);}else{syncSelectedAssetsFromDetails();if(!selectedKeys.contains(item.asset().key)){selectedKeys.clear();selectedKeys.add(item.asset().key);syncBridgeSelection();}Dragboard db=row.startDragAndDrop(TransferMode.MOVE);ClipboardContent cc=new ClipboardContent();cc.put(ASSET_KEYS,String.join("\n",selectedKeys));db.setContent(cc);}e.consume();});
            row.setOnDragOver(e->{if(row.isEmpty())acceptTarget(e,currentFolder);else if(row.getItem().kind()==EntryKind.FOLDER)acceptTarget(e,row.getItem().path());});
            row.setOnDragEntered(e->{if(!row.isEmpty()&&row.getItem().kind()==EntryKind.FOLDER)addClass(row,"drop-target");});row.setOnDragExited(e->row.getStyleClass().remove("drop-target"));
            row.setOnDragDropped(e->{row.getStyleClass().remove("drop-target");dropOnPath(e,row.isEmpty()?currentFolder:row.getItem().kind()==EntryKind.FOLDER?row.getItem().path():currentFolder);});
            row.setContextMenu(buildDetailsContextMenu(row));return row;});
        details.setOnDragOver(e->acceptTarget(e,currentFolder));details.setOnDragDropped(e->dropOnPath(e,currentFolder));
    }

    private ContextMenu buildDetailsContextMenu(TableRow<Entry> row){
        ContextMenu menu=new ContextMenu();MenuItem create=new MenuItem("Nueva carpeta");create.setOnAction(e->createFolder());menu.getItems().add(create);
        menu.setOnShowing(e->{while(menu.getItems().size()>1)menu.getItems().remove(1);if(row.isEmpty())return;Entry item=row.getItem();if(item.kind()==EntryKind.FOLDER){MenuItem open=new MenuItem("Abrir");open.setOnAction(x->openFolder(item.path()));menu.getItems().add(open);if(app.project().getAssetFolders().contains(item.path())){MenuItem rename=new MenuItem("Renombrar");rename.setOnAction(x->{selectedFolder=item.path();renameSelectedFolder();});MenuItem delete=new MenuItem("Eliminar carpeta");delete.setOnAction(x->{selectedFolder=item.path();deleteSelectedFolder();});menu.getItems().addAll(new SeparatorMenuItem(),rename,delete);}}else{Asset asset=item.asset();MenuItem favorite=new MenuItem(asset.favorite?"Quitar de favoritos":"Marcar favorito");favorite.setOnAction(x->{asset.favorite=!asset.favorite;app.changed();rebuild();});Menu move=moveMenu(Set.of(asset.key));menu.getItems().addAll(favorite,move);}});return menu;
    }

    private void configureGrid(){
        assetGrid.getStyleClass().add("resource-grid");folderRows.getStyleClass().add("resource-folder-rows");gridRoot.getStyleClass().add("resource-grid-root");gridRoot.setPadding(new Insets(8));assetGrid.setPadding(new Insets(4,0,0,0));
        gridRoot.getChildren().addAll(folderRows,assetGrid);gridScroll.setFitToWidth(true);gridScroll.getStyleClass().add("resource-scroll");gridScroll.setOnDragOver(e->acceptTarget(e,currentFolder));gridScroll.setOnDragDropped(e->dropOnPath(e,currentFolder));
    }

    private void handleKey(KeyEvent e){
        if(e.isControlDown()&&e.getCode()==KeyCode.A){selectedKeys.clear();currentAssets().forEach(a->selectedKeys.add(a.key));syncBridgeSelection();selectDetailsForKeys();rebuildGridSelection();e.consume();}
        else if(e.getCode()==KeyCode.BACK_SPACE&&!favoritesMode&&!currentFolder.isBlank()){openFolder(parent(currentFolder));e.consume();}
        else if(e.getCode()==KeyCode.F2&&!selectedFolder.isBlank()){renameSelectedFolder();e.consume();}
        else if(e.getCode()==KeyCode.ENTER&&!selectedFolder.isBlank()){openFolder(selectedFolder);e.consume();}
    }

    private void rebuild(){
        rebuildBreadcrumbs();rebuildPlaces();
        boolean searching=isSearching();List<Entry> entries=new ArrayList<>();
        if(!searching&&!favoritesMode)for(String f:contentChildFolders(currentFolder))entries.add(Entry.folder(f));
        for(Asset a:currentAssets())entries.add(Entry.asset(a));
        entries.sort((a,b)->{if(a.kind()!=b.kind())return a.kind()==EntryKind.FOLDER?-1:1;return String.CASE_INSENSITIVE_ORDER.compare(entryName(a),entryName(b));});
        details.setItems(FXCollections.observableArrayList(entries));selectDetailsForKeys();
        rebuildGrid(entries);content.getChildren().setAll(detailsMode.isSelected()?details:gridScroll);
        long folderCount=entries.stream().filter(e->e.kind()==EntryKind.FOLDER).count(),assetCount=entries.size()-folderCount;
        status.setText(searching?assetCount+" resultado"+(assetCount==1?"":"s")+" · búsqueda en todo el proyecto":favoritesMode?assetCount+" favorito"+(assetCount==1?"":"s"):folderCount+" carpeta"+(folderCount==1?"":"s")+" · "+assetCount+" archivo"+(assetCount==1?"":"s"));
        updateButtons();
    }

    private void rebuildGrid(List<Entry> entries){
        folderRows.getChildren().clear();assetGrid.getChildren().clear();
        for(Entry e:entries)if(e.kind()==EntryKind.FOLDER)folderRows.getChildren().add(folderStrip(e.path()));else assetGrid.getChildren().add(assetTile(e.asset()));
    }

    private Node folderStrip(String path){
        Label label=new Label(name(path));label.getStyleClass().add("resource-file-name");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Label count=new Label(folderItemCount(path)+" elementos");count.getStyleClass().add("resource-file-type");
        HBox row=new HBox(9,folderIcon(18),label,spacer,count);row.setAlignment(Pos.CENTER_LEFT);row.getStyleClass().addAll("resource-folder-strip","resource-row");
        row.setOnMouseClicked(e->{selectedFolder=path;selectedKeys.clear();syncBridgeSelection();if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()>=2)openFolder(path);else updateButtons();});
        row.setOnDragOver(e->acceptTarget(e,path));row.setOnDragEntered(e->addClass(row,"drop-target"));row.setOnDragExited(e->row.getStyleClass().remove("drop-target"));row.setOnDragDropped(e->{row.getStyleClass().remove("drop-target");dropOnPath(e,path);});
        if(app.project().getAssetFolders().contains(path))row.setOnDragDetected(e->{Dragboard db=row.startDragAndDrop(TransferMode.MOVE);ClipboardContent c=new ClipboardContent();c.put(FOLDER_PATH,path);db.setContent(c);e.consume();});return row;
    }

    private Node assetTile(Asset asset){
        StackPane thumb=thumbnail(asset,86,62);Label label=new Label(displayName(asset));label.setWrapText(true);label.setMaxWidth(118);label.getStyleClass().add("resource-file-name");Label type=new Label(typeName(asset));type.getStyleClass().add("resource-file-type");VBox box=new VBox(6,thumb,label,type);box.setAlignment(Pos.TOP_LEFT);box.setPrefSize(132,126);box.getStyleClass().add("resource-asset-tile");box.getProperties().put("asset-key",asset.key);if(selectedKeys.contains(asset.key))box.getStyleClass().add("selected");
        box.setOnMouseClicked(e->{requestFocus();if(e.isControlDown()||e.isShiftDown()){if(!selectedKeys.add(asset.key))selectedKeys.remove(asset.key);}else{selectedKeys.clear();selectedKeys.add(asset.key);}selectedFolder="";syncBridgeSelection();rebuildGridSelection();});
        box.setOnDragDetected(e->{if(!selectedKeys.contains(asset.key)){selectedKeys.clear();selectedKeys.add(asset.key);syncBridgeSelection();}Dragboard db=box.startDragAndDrop(TransferMode.MOVE);ClipboardContent c=new ClipboardContent();c.put(ASSET_KEYS,String.join("\n",selectedKeys));db.setContent(c);e.consume();});
        ContextMenu menu=new ContextMenu();MenuItem favorite=new MenuItem(asset.favorite?"Quitar de favoritos":"Marcar favorito");favorite.setOnAction(e->{asset.favorite=!asset.favorite;app.changed();rebuild();});menu.getItems().addAll(favorite,moveMenu(Set.of(asset.key)));box.setOnContextMenuRequested(e->menu.show(box,e.getScreenX(),e.getScreenY()));return box;
    }

    private void rebuildGridSelection(){for(Node n:assetGrid.getChildren()){n.getStyleClass().remove("selected");if(n.getProperties().get("asset-key") instanceof String key&&selectedKeys.contains(key))n.getStyleClass().add("selected");}}

    private Menu moveMenu(Collection<String> keys){Menu move=new Menu("Mover a");MenuItem root=new MenuItem("Assets");root.setOnAction(e->moveAssets(keys,""));move.getItems().add(root);for(String folder:allFolders()){MenuItem item=new MenuItem(folder);item.setOnAction(e->moveAssets(keys,folder));move.getItems().add(item);}return move;}

    private void rebuildBreadcrumbs(){breadcrumbs.getChildren().clear();Button root=breadcrumb("Assets","");breadcrumbs.getChildren().add(root);if(favoritesMode){Button favorites=breadcrumb("Favoritos","");favorites.setOnAction(e->{favoritesMode=true;rebuild();});breadcrumbs.getChildren().addAll(new Label("›"),favorites);return;}String path="";for(String part:currentFolder.split("/")){if(part.isBlank())continue;breadcrumbs.getChildren().add(new Label("›"));path=path.isBlank()?part:path+"/"+part;breadcrumbs.getChildren().add(breadcrumb(part,path));}}

    private Button breadcrumb(String text,String path){Button b=new Button(text);b.getStyleClass().add("breadcrumb-button");b.setOnAction(e->openFolder(path));b.setOnDragOver(e->acceptTarget(e,path));b.setOnDragDropped(e->dropOnPath(e,path));return b;}

    private void rebuildPlaces(){
        rebuildingPlaces=true;Place selected=places.getSelectionModel().getSelectedItem();List<Place> list=new ArrayList<>();list.add(new Place("Assets","",false));list.add(new Place("Favoritos","",true));
        for(String f:STANDARD_FOLDERS)list.add(new Place(f,f,false));for(String f:customTopFolders())if(!STANDARD_FOLDERS.contains(f))list.add(new Place(f,f,false));places.setItems(FXCollections.observableArrayList(list));
        if(favoritesMode)places.getSelectionModel().select(list.stream().filter(Place::favorites).findFirst().orElse(null));else{Place match=list.stream().filter(p->!p.favorites()&&(p.path().equals(currentFolder)||(!p.path().isBlank()&&currentFolder.startsWith(p.path()+"/")))).max(Comparator.comparingInt(p->p.path().length())).orElse(list.getFirst());places.getSelectionModel().select(match);}rebuildingPlaces=false;
    }

    private List<String> customTopFolders(){TreeSet<String> result=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(String f:app.project().getAssetFolders()){String n=normalize(f);if(!n.isBlank())result.add(n.contains("/")?n.substring(0,n.indexOf('/')):n);}return new ArrayList<>(result);}

    private List<String> contentChildFolders(String parent){List<String> children=childFolders(parent);if(normalize(parent).isBlank())children.removeIf(STANDARD_FOLDERS::contains);return children;}

    private List<Asset> currentAssets(){
        List<Asset> assets=visibleAssets();String q=search.getText()==null?"":search.getText().trim().toLowerCase(Locale.ROOT);if(!q.isBlank())return assets.stream().filter(a->displayName(a).toLowerCase(Locale.ROOT).contains(q)||a.key.toLowerCase(Locale.ROOT).contains(q)||typeName(a).toLowerCase(Locale.ROOT).contains(q)||effectiveFolder(a).toLowerCase(Locale.ROOT).contains(q)).toList();if(favoritesMode)return assets.stream().filter(a->a.favorite).toList();return assets.stream().filter(a->effectiveFolder(a).equals(currentFolder)).toList();
    }

    private boolean isSearching(){return search.getText()!=null&&!search.getText().trim().isBlank();}
    private List<Asset> visibleAssets(){ObservableList<Asset> items=selectionBridge.getItems();return items==null?List.of():new ArrayList<>(items);}

    private void syncSelectedAssetsFromDetails(){selectedKeys.clear();for(Entry e:details.getSelectionModel().getSelectedItems())if(e!=null&&e.kind()==EntryKind.ASSET)selectedKeys.add(e.asset().key);syncBridgeSelection();}
    private void selectDetailsForKeys(){details.getSelectionModel().clearSelection();ObservableList<Entry> items=details.getItems();for(int i=0;i<items.size();i++){Entry e=items.get(i);if(e.kind()==EntryKind.ASSET&&selectedKeys.contains(e.asset().key))details.getSelectionModel().select(i);}}
    private void syncBridgeSelection(){MultipleSelectionModel<Asset> sm=selectionBridge.getSelectionModel();sm.clearSelection();ObservableList<Asset> items=selectionBridge.getItems();if(items==null)return;for(int i=0;i<items.size();i++)if(selectedKeys.contains(items.get(i).key))sm.select(i);}

    private void openFolder(String path){favoritesMode=false;currentFolder=normalize(path);selectedFolder="";selectedKeys.clear();syncBridgeSelection();rebuild();}

    private void updateButtons(){up.setDisable(favoritesMode||currentFolder.isBlank());boolean editable=!selectedFolder.isBlank()&&app.project().getAssetFolders().contains(selectedFolder);renameFolder.setDisable(!editable);deleteFolder.setDisable(!editable);}

    private void createFolder(){TextInputDialog d=new TextInputDialog();d.setTitle("Nueva carpeta");d.setHeaderText(currentFolder.isBlank()?"Crear carpeta en Assets":"Crear carpeta dentro de "+currentFolder);d.setContentText("Nombre:");d.showAndWait().map(String::trim).filter(s->!s.isBlank()).ifPresent(raw->{String clean=cleanName(raw);if(clean.isBlank())return;String path=join(currentFolder,clean);if(allFolders().contains(path)){app.status("Ya existe la carpeta: "+path);return;}app.project().getAssetFolders().add(path);app.changed();selectedFolder=path;rebuild();});}

    private void renameSelectedFolder(){String old=selectedFolder;if(old.isBlank()||!app.project().getAssetFolders().contains(old))return;TextInputDialog d=new TextInputDialog(name(old));d.setTitle("Renombrar carpeta");d.setHeaderText(old);d.setContentText("Nuevo nombre:");d.showAndWait().map(String::trim).filter(s->!s.isBlank()).ifPresent(value->{String next=join(parent(old),cleanName(value));if(next.equals(old)||allFolders().contains(next))return;remapFolder(old,next);selectedFolder=next;currentFolder=remapPath(currentFolder,old,next);app.changed();rebuild();});}

    private void deleteSelectedFolder(){String old=selectedFolder;if(old.isBlank()||!app.project().getAssetFolders().contains(old))return;Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Los archivos y subcarpetas se moverán a "+(parent(old).isBlank()?"Assets":parent(old))+".",ButtonType.CANCEL,ButtonType.OK);a.setHeaderText("Eliminar carpeta “"+name(old)+"”");if(a.showAndWait().orElse(ButtonType.CANCEL)!=ButtonType.OK)return;String target=parent(old);for(Asset asset:app.project().getAssets().values()){String f=effectiveFolder(asset);if(f.equals(old)||f.startsWith(old+"/"))asset.folder=externalFolder(remapPath(f,old,target));}LinkedHashSet<String> next=new LinkedHashSet<>();for(String folder:app.project().getAssetFolders())if(!folder.equals(old))next.add(folder.startsWith(old+"/")?remapPath(folder,old,target):folder);app.project().getAssetFolders().clear();app.project().getAssetFolders().addAll(next);if(currentFolder.equals(old)||currentFolder.startsWith(old+"/"))currentFolder=target;selectedFolder="";app.changed();rebuild();}

    private void moveAssets(Collection<String> keys,String target){String normalized=normalize(target);int moved=0;for(String key:keys){Asset a=app.project().getAssets().get(key);if(a==null)continue;a.folder=externalFolder(normalized);moved++;}if(moved>0){app.changed();app.status("Movidos "+moved+" recurso"+(moved==1?"":"s")+" a "+(normalized.isBlank()?"Assets":normalized));rebuild();}}
    private void moveFolder(String source,String targetParent){source=normalize(source);targetParent=normalize(targetParent);if(source.isBlank()||!app.project().getAssetFolders().contains(source)||targetParent.equals(source)||targetParent.startsWith(source+"/"))return;String target=join(targetParent,name(source));if(allFolders().contains(target)){app.status("Ya existe "+target);return;}remapFolder(source,target);currentFolder=remapPath(currentFolder,source,target);selectedFolder=target;app.changed();rebuild();}
    private void remapFolder(String old,String next){LinkedHashSet<String> folders=new LinkedHashSet<>();for(String f:app.project().getAssetFolders())folders.add(f.equals(old)||f.startsWith(old+"/")?remapPath(f,old,next):f);app.project().getAssetFolders().clear();app.project().getAssetFolders().addAll(folders);for(Asset asset:app.project().getAssets().values()){String f=effectiveFolder(asset);if(f.equals(old)||f.startsWith(old+"/"))asset.folder=externalFolder(remapPath(f,old,next));}}

    private void acceptTarget(DragEvent e,String path){Dragboard db=e.getDragboard();if(db.hasContent(ASSET_KEYS)||db.hasContent(FOLDER_PATH)){e.acceptTransferModes(TransferMode.MOVE);e.consume();}}
    private void dropOnPath(DragEvent e,String path){Dragboard db=e.getDragboard();boolean ok=false;if(db.hasContent(ASSET_KEYS)){String raw=Objects.toString(db.getContent(ASSET_KEYS),"");moveAssets(Arrays.stream(raw.split("\\R")).filter(s->!s.isBlank()).toList(),path);ok=true;}else if(db.hasContent(FOLDER_PATH)){moveFolder(Objects.toString(db.getContent(FOLDER_PATH),""),path);ok=true;}e.setDropCompleted(ok);e.consume();}

    private List<String> childFolders(String parent){String base=normalize(parent);TreeSet<String> children=new TreeSet<>(String.CASE_INSENSITIVE_ORDER);for(String folder:allFolders()){if(folder.equals(base))continue;if(parent(folder).equals(base))children.add(folder);}return new ArrayList<>(children);}
    private Set<String> allFolders(){LinkedHashSet<String> folders=new LinkedHashSet<>(STANDARD_FOLDERS);folders.addAll(app.project().getAssetFolders().stream().map(AssetExplorerPane::normalize).filter(s->!s.isBlank()).toList());for(Asset asset:app.project().getAssets().values()){String path=effectiveFolder(asset);while(!path.isBlank()){folders.add(path);path=parent(path);}}return folders;}
    private int folderItemCount(String path){int assets=(int)app.project().getAssets().values().stream().filter(a->effectiveFolder(a).equals(path)).count();int children=(int)allFolders().stream().filter(f->parent(f).equals(path)).count();return assets+children;}

    private StackPane thumbnail(Asset asset,double w,double h){StackPane pane=new StackPane();pane.setPrefSize(w,h);pane.setMinSize(w,h);pane.setMaxSize(w,h);pane.getStyleClass().add("asset-thumbnail");Image image=app.image(asset.key);if(image!=null){ImageView iv=new ImageView(image);iv.setPreserveRatio(true);iv.setSmooth(asset.filterMode!=null&&asset.filterMode.name().equals("LINEAR"));iv.setFitWidth(w-6);iv.setFitHeight(h-6);pane.getChildren().add(iv);}else{Label fallback=new Label(asset.sourceOnly?"SHEET":"IMG");fallback.getStyleClass().add("asset-thumbnail-fallback");pane.getChildren().add(fallback);}if(asset.favorite){Label star=new Label("★");star.getStyleClass().add("favorite-badge");StackPane.setAlignment(star,Pos.TOP_RIGHT);pane.getChildren().add(star);}pane.getProperties().put("asset-key",asset.key);return pane;}
    private SVGPath folderIcon(double size){SVGPath icon=new SVGPath();icon.setContent("M2 6.5h7.2l2-2H22v14.5H2z");icon.getStyleClass().add("resource-folder-icon");icon.setScaleX(size/24.0);icon.setScaleY(size/24.0);return icon;}
    private SVGPath starIcon(double size){SVGPath icon=new SVGPath();icon.setContent("M12 2l3.1 6.3 6.9 1-5 4.9 1.2 6.8-6.2-3.2L5.8 21 7 14.2 2 9.3l6.9-1z");icon.getStyleClass().add("favorite-icon");icon.setScaleX(size/24.0);icon.setScaleY(size/24.0);return icon;}

    private String entryName(Entry e){return e.kind()==EntryKind.FOLDER?name(e.path()):displayName(e.asset());}
    private String entryLocation(Entry e){if(e.kind()==EntryKind.FOLDER)return parent(e.path()).isBlank()?"Assets":parent(e.path());String f=effectiveFolder(e.asset());return f.isBlank()?"Assets":f;}
    private String effectiveFolder(Asset a){String folder=a.folder==null?"":a.folder.trim();if(EXPLICIT_ROOT.equals(folder))return "";if(!folder.isBlank())return normalize(folder);return categoryFolder(a);}
    private String externalFolder(String displayPath){return normalize(displayPath).isBlank()?EXPLICIT_ROOT:normalize(displayPath);}
    private static String categoryFolder(Asset a){if(a.sourceOnly&&!a.isRegion())return "Spritesheets";if(a.isRegion())return "Sprites";AssetCategory c=a.category==null?AssetCategory.IMAGE:a.category;return switch(c){case SPRITESHEET->"Spritesheets";case SPRITE->"Sprites";case BACKGROUND->"Fondos";case UI->"UI";case TILESET->"Tilesets";case VFX->"VFX";case PORTRAIT->"Retratos";default->"Imágenes";};}
    private static String typeName(Asset a){if(a.sourceOnly&&!a.isRegion())return "Hoja de sprites";if(a.isRegion())return "Región de sprite";AssetCategory c=a.category==null?AssetCategory.IMAGE:a.category;return switch(c){case IMAGE->"Imagen";case SPRITESHEET->"Hoja de sprites";case SPRITE->"Sprite";case BACKGROUND->"Fondo";case UI->"Interfaz";case TILESET->"Tileset";case VFX->"Efecto visual";case PORTRAIT->"Retrato";};}
    private static String displayName(Asset a){return a.sourceName==null||a.sourceName.isBlank()?a.key:a.sourceName;}
    private static String normalize(String path){if(path==null)return "";return Arrays.stream(path.replace('\\','/').split("/+")) .map(String::trim).filter(s->!s.isBlank()&&!s.equals(".")).collect(Collectors.joining("/"));}
    private static String parent(String path){String n=normalize(path);int i=n.lastIndexOf('/');return i<0?"":n.substring(0,i);}
    private static String name(String path){String n=normalize(path);int i=n.lastIndexOf('/');return i<0?n:n.substring(i+1);}
    private static String join(String parent,String child){String p=normalize(parent),c=cleanName(child);return p.isBlank()?c:c.isBlank()?p:p+"/"+c;}
    private static String cleanName(String name){if(name==null)return "";return name.replace('/',' ').replace('\\',' ').replace(':',' ').replace('*',' ').replace('?',' ').replace('"',' ').replace('<',' ').replace('>',' ').replace('|',' ').trim().replaceAll("\\s+"," ");}
    private static String remapPath(String path,String oldRoot,String newRoot){String p=normalize(path),old=normalize(oldRoot),next=normalize(newRoot);if(p.equals(old))return next;if(p.startsWith(old+"/"))return next.isBlank()?p.substring(old.length()+1):next+p.substring(old.length());return p;}
    private static void addClass(Node n,String cls){if(!n.getStyleClass().contains(cls))n.getStyleClass().add(cls);}
}
