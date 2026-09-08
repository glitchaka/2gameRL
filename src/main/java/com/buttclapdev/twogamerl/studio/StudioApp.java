package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.export.NativeExporter;
import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.runtime.GameView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class StudioApp extends Application {
    private static final double RESIZE_MARGIN = 6;

    private Stage stage;
    private GameProject project = GameProject.createDefault();
    private Path currentFile;
    private boolean dirty;
    private final BorderPane root = new BorderPane();
    private final StackPane workspace = new StackPane();
    private final Label status = new Label("Listo");
    private final Label windowTitle = new Label();
    private final Map<String, Image> imageCache = new HashMap<>();
    private ToggleButton sceneWorkspaceButton;

    private double windowDragX,windowDragY;
    private boolean resizing;
    private Cursor resizeCursor = Cursor.DEFAULT;
    private double resizeScreenX,resizeScreenY,resizeStageX,resizeStageY,resizeStageWidth,resizeStageHeight;

    @Override public void start(Stage primaryStage) {
        stage = primaryStage;
        stage.initStyle(StageStyle.UNDECORATED);
        root.getStyleClass().addAll("studio-root", "window-frame");
        root.setTop(buildTop()); root.setCenter(workspace);
        status.getStyleClass().add("status-bar"); status.setMaxWidth(Double.MAX_VALUE); status.setPadding(new Insets(5,12,6,12)); root.setBottom(status);
        showSceneWorkspace();
        Scene scene = new Scene(root,1480,900); applyCss(scene); installWindowResize(scene); stage.setScene(scene); stage.setMinWidth(1120); stage.setMinHeight(720); updateTitle(); stage.show();
        stage.setOnCloseRequest(e -> { if (!confirmDiscard()) e.consume(); });
    }

    GameProject project(){return project;} Stage owner(){return stage;}
    void changed(){dirty=true;imageCache.clear();updateTitle();}
    void status(String text){status.setText(text);}

    Image image(String key) {
        if (key == null || key.isBlank()) return null;
        Asset asset = project.getAssets().get(key);
        if (asset == null) return null;
        if (asset.isRegion()) return image(asset.sourceAssetKey);
        if (imageCache.containsKey(key)) return imageCache.get(key);
        if (asset.data == null) return null;
        try {Image image=new Image(new ByteArrayInputStream(asset.data),0,0,true,false);imageCache.put(key,image);return image;}catch(Exception e){return null;}
    }

    int assetWidth(String key){Asset a=project.getAssets().get(key);if(a==null)return 0;if(a.isRegion())return a.regionWidth;Image i=image(key);return i==null?0:(int)Math.round(i.getWidth());}
    int assetHeight(String key){Asset a=project.getAssets().get(key);if(a==null)return 0;if(a.isRegion())return a.regionHeight;Image i=image(key);return i==null?0:(int)Math.round(i.getHeight());}

    void drawAsset(GraphicsContext g,String key,double dx,double dy,double dw,double dh){
        if(g==null||key==null||key.isBlank())return;Asset a=project.getAssets().get(key);if(a==null)return;Image image=image(key);if(image==null)return;g.setImageSmoothing(false);
        if(a.isRegion())g.drawImage(image,a.regionX,a.regionY,a.regionWidth,a.regionHeight,Math.rint(dx),Math.rint(dy),Math.max(1,Math.rint(dw)),Math.max(1,Math.rint(dh)));
        else g.drawImage(image,Math.rint(dx),Math.rint(dy),Math.max(1,Math.rint(dw)),Math.max(1,Math.rint(dh)));
    }

    ImageView assetView(String key,double width,double height){
        Asset a=project.getAssets().get(key);Image image=image(key);if(a==null||image==null)return null;ImageView iv=new ImageView(image);iv.setSmooth(false);iv.setCache(false);iv.setPreserveRatio(false);iv.setFitWidth(Math.max(1,Math.rint(width)));iv.setFitHeight(Math.max(1,Math.rint(height)));
        if(a.isRegion())iv.setViewport(new Rectangle2D(a.regionX,a.regionY,a.regionWidth,a.regionHeight));return iv;
    }

    private Node buildTop(){
        Menu file=new Menu("Archivo");MenuItem n=item("Nuevo","Ctrl+N",e->newProject()),open=item("Abrir…","Ctrl+O",e->openProject()),save=item("Guardar","Ctrl+S",e->save(false)),saveAs=item("Guardar como…","Ctrl+Shift+S",e->save(true)),export=item("Exportar juego…","Ctrl+E",e->exportGame()),exit=item("Salir","",e->requestClose());
        file.getItems().addAll(n,open,new SeparatorMenuItem(),save,saveAs,new SeparatorMenuItem(),export,new SeparatorMenuItem(),exit);
        Menu help=new Menu("Ayuda");MenuItem quick=new MenuItem("Guía de 60 segundos");quick.setOnAction(e->quickGuide());MenuItem scripting=item("Tutorial de scripting","F1",e->ScriptTutorialDialog.show(stage));help.getItems().addAll(quick,scripting);MenuBar menuBar=new MenuBar(file,help);
        Button bNew=button("＋ Nuevo",e->newProject()),bOpen=button("Abrir",e->openProject()),bSave=button("Guardar",e->save(false)),play=button("▶ Probar",e->playPreview()),exportB=button("Exportar",e->exportGame());play.getStyleClass().add("primary-button");
        ToggleGroup group=new ToggleGroup();sceneWorkspaceButton=workspaceButton("Escena",group,true);ToggleButton menus=workspaceButton("Menús",group,false),graphics=workspaceButton("Gráficos",group,false);sceneWorkspaceButton.setOnAction(e->showSceneWorkspace());menus.setOnAction(e->showMenuWorkspace());graphics.setOnAction(e->showGraphicsWorkspace());Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox toolbar=new HBox(7,bNew,bOpen,bSave,new Separator(),sceneWorkspaceButton,menus,graphics,spacer,play,exportB);toolbar.setAlignment(Pos.CENTER_LEFT);toolbar.setPadding(new Insets(8,12,9,12));toolbar.getStyleClass().add("main-toolbar");return new VBox(buildTitleBar(),menuBar,toolbar);
    }

    private Node buildTitleBar(){Label mark=new Label("2G");mark.getStyleClass().add("window-app-mark");windowTitle.getStyleClass().add("window-title");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button minimize=chromeButton("—","Minimizar"),maximize=chromeButton("▢","Maximizar / restaurar"),close=chromeButton("×","Cerrar");close.getStyleClass().add("window-close");minimize.setOnAction(e->stage.setIconified(true));maximize.setOnAction(e->stage.setMaximized(!stage.isMaximized()));close.setOnAction(e->requestClose());stage.maximizedProperty().addListener((o,a,b)->maximize.setText(b?"❐":"▢"));HBox titleBar=new HBox(9,mark,windowTitle,spacer,minimize,maximize,close);titleBar.setAlignment(Pos.CENTER_LEFT);titleBar.getStyleClass().add("title-bar");titleBar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||isWindowControl(e.getTarget()))return;windowDragX=e.getSceneX();windowDragY=e.getSceneY();});titleBar.setOnMouseDragged(e->{if(e.getButton()!=MouseButton.PRIMARY||stage.isMaximized()||isWindowControl(e.getTarget()))return;stage.setX(e.getScreenX()-windowDragX);stage.setY(e.getScreenY()-windowDragY);});titleBar.setOnMouseClicked(e->{if(e.getButton()==MouseButton.PRIMARY&&e.getClickCount()==2&&!isWindowControl(e.getTarget()))stage.setMaximized(!stage.isMaximized());});return titleBar;}
    private Button chromeButton(String text,String tooltip){Button b=new Button(text);b.getStyleClass().add("window-control");b.setTooltip(new Tooltip(tooltip));return b;}
    private boolean isWindowControl(Object target){if(!(target instanceof Node node))return false;for(Node current=node;current!=null;current=current.getParent())if(current.getStyleClass().contains("window-control"))return true;return false;}
    private void requestClose(){stage.fireEvent(new WindowEvent(stage,WindowEvent.WINDOW_CLOSE_REQUEST));}

    private void installWindowResize(Scene scene){scene.addEventFilter(MouseEvent.MOUSE_MOVED,e->{if(resizing||stage.isMaximized())return;scene.setCursor(edgeCursor(e.getSceneX(),e.getSceneY(),scene.getWidth(),scene.getHeight()));});scene.addEventFilter(MouseEvent.MOUSE_PRESSED,e->{if(e.getButton()!=MouseButton.PRIMARY||stage.isMaximized())return;Cursor cursor=edgeCursor(e.getSceneX(),e.getSceneY(),scene.getWidth(),scene.getHeight());if(cursor==Cursor.DEFAULT)return;resizing=true;resizeCursor=cursor;resizeScreenX=e.getScreenX();resizeScreenY=e.getScreenY();resizeStageX=stage.getX();resizeStageY=stage.getY();resizeStageWidth=stage.getWidth();resizeStageHeight=stage.getHeight();e.consume();});scene.addEventFilter(MouseEvent.MOUSE_DRAGGED,e->{if(!resizing)return;resizeWindow(e.getScreenX()-resizeScreenX,e.getScreenY()-resizeScreenY);e.consume();});scene.addEventFilter(MouseEvent.MOUSE_RELEASED,e->{if(!resizing)return;resizing=false;resizeCursor=Cursor.DEFAULT;scene.setCursor(edgeCursor(e.getSceneX(),e.getSceneY(),scene.getWidth(),scene.getHeight()));e.consume();});}
    private Cursor edgeCursor(double x,double y,double width,double height){boolean left=x<=RESIZE_MARGIN,right=x>=width-RESIZE_MARGIN,top=y<=RESIZE_MARGIN,bottom=y>=height-RESIZE_MARGIN;if(top&&left)return Cursor.NW_RESIZE;if(top&&right)return Cursor.NE_RESIZE;if(bottom&&left)return Cursor.SW_RESIZE;if(bottom&&right)return Cursor.SE_RESIZE;if(left)return Cursor.W_RESIZE;if(right)return Cursor.E_RESIZE;if(top)return Cursor.N_RESIZE;if(bottom)return Cursor.S_RESIZE;return Cursor.DEFAULT;}
    private void resizeWindow(double dx,double dy){double minW=stage.getMinWidth(),minH=stage.getMinHeight();boolean west=resizeCursor==Cursor.W_RESIZE||resizeCursor==Cursor.NW_RESIZE||resizeCursor==Cursor.SW_RESIZE,east=resizeCursor==Cursor.E_RESIZE||resizeCursor==Cursor.NE_RESIZE||resizeCursor==Cursor.SE_RESIZE,north=resizeCursor==Cursor.N_RESIZE||resizeCursor==Cursor.NW_RESIZE||resizeCursor==Cursor.NE_RESIZE,south=resizeCursor==Cursor.S_RESIZE||resizeCursor==Cursor.SW_RESIZE||resizeCursor==Cursor.SE_RESIZE;if(east)stage.setWidth(Math.max(minW,resizeStageWidth+dx));if(south)stage.setHeight(Math.max(minH,resizeStageHeight+dy));if(west){double width=Math.max(minW,resizeStageWidth-dx);stage.setX(resizeStageX+resizeStageWidth-width);stage.setWidth(width);}if(north){double height=Math.max(minH,resizeStageHeight-dy);stage.setY(resizeStageY+resizeStageHeight-height);stage.setHeight(height);}}

    private MenuItem item(String text,String accelerator,javafx.event.EventHandler<javafx.event.ActionEvent>handler){MenuItem item=new MenuItem(text);if(!accelerator.isBlank())item.setAccelerator(javafx.scene.input.KeyCombination.keyCombination(accelerator));item.setOnAction(handler);return item;}
    private Button button(String text,javafx.event.EventHandler<javafx.event.ActionEvent>handler){Button b=new Button(text);b.setOnAction(handler);return b;}
    private ToggleButton workspaceButton(String text,ToggleGroup group,boolean selected){ToggleButton b=new ToggleButton(text);b.setToggleGroup(group);b.setSelected(selected);b.getStyleClass().add("workspace-button");return b;}

    private void showSceneWorkspace(){workspace.getChildren().setAll(new SceneEditorPane(this));status("Seleccionar objetos o usar Dibujar, Rellenar y Borrar sobre la capa de tiles activa.");}
    private void showMenuWorkspace(){workspace.getChildren().setAll(new MenuEditorPane(this));status("Diseña las pantallas y arrastra sus botones directamente sobre el lienzo.");}
    private void showGraphicsWorkspace(){workspace.getChildren().setAll(new GraphicsEditorPane(this));status("Las spritesheets se conservan como una textura y los sprites son regiones virtuales sin blur.");}

    private void newProject(){if(!confirmDiscard())return;project=GameProject.createDefault();currentFile=null;dirty=false;imageCache.clear();updateTitle();sceneWorkspaceButton.setSelected(true);showSceneWorkspace();}
    private void openProject(){if(!confirmDiscard())return;FileChooser chooser=projectChooser("Abrir proyecto");java.io.File f=chooser.showOpenDialog(stage);if(f==null)return;try{project=ProjectIO.load(f.toPath());currentFile=f.toPath();dirty=false;imageCache.clear();updateTitle();sceneWorkspaceButton.setSelected(true);showSceneWorkspace();status("Proyecto cargado: "+f.getName());}catch(Exception e){error("No se pudo abrir",e.getMessage());}}
    private boolean save(boolean choose){try{if(currentFile==null||choose){FileChooser chooser=projectChooser("Guardar proyecto");java.io.File f=chooser.showSaveDialog(stage);if(f==null)return false;Path p=f.toPath();if(!p.getFileName().toString().toLowerCase().endsWith(".2grl"))p=p.resolveSibling(p.getFileName()+".2grl");currentFile=p;}ProjectIO.save(project,currentFile);dirty=false;updateTitle();status("Guardado: "+currentFile);return true;}catch(Exception e){error("No se pudo guardar",e.getMessage());return false;}}
    private FileChooser projectChooser(String title){FileChooser fc=new FileChooser();fc.setTitle(title);fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Proyecto 2gameRL (*.2grl)","*.2grl"));return fc;}

    private void playPreview(){Stage preview=new Stage();GameView view=new GameView(project);TextArea console=new TextArea();console.setEditable(false);console.setPrefRowCount(5);view.setLogger(s->Platform.runLater(()->console.appendText(s+"\n")));VBox box=new VBox(view,console);VBox.setVgrow(view,Priority.ALWAYS);Scene scene=new Scene(box,1000,760);applyCss(scene);preview.setScene(scene);preview.setTitle("Probar · "+project.getTitle());preview.show();preview.setOnHidden(e->view.stop());}
    private void exportGame(){if(dirty&&!save(false))return;DirectoryChooser dc=new DirectoryChooser();dc.setTitle("Carpeta donde exportar el juego");java.io.File dir=dc.showDialog(stage);if(dir==null)return;Stage progress=new Stage();progress.initOwner(stage);progress.initModality(Modality.APPLICATION_MODAL);progress.setTitle("Exportar");TextArea log=new TextArea();log.setEditable(false);ProgressIndicator indicator=new ProgressIndicator();Label title=new Label("EXPORTANDO JUEGO");title.getStyleClass().add("panel-title");VBox box=new VBox(10,new HBox(10,indicator,title),log);box.setPadding(new Insets(16));Scene sc=new Scene(box,720,460);applyCss(sc);progress.setScene(sc);Task<NativeExporter.Result>task=new Task<>(){@Override protected NativeExporter.Result call(){return NativeExporter.export(project.deepCopy(),dir.toPath(),s->Platform.runLater(()->log.appendText(s+"\n")));}};task.setOnSucceeded(e->{progress.close();NativeExporter.Result r=task.getValue();if(r.success())info("Juego exportado","Aplicación: "+r.application()+"\nZIP portable: "+r.zip());else error("Falló la exportación",r.message());});task.setOnFailed(e->{progress.close();error("Falló la exportación",task.getException().toString());});Thread thread=new Thread(task,"2gameRL-export");thread.setDaemon(true);thread.start();progress.showAndWait();}

    private boolean confirmDiscard(){if(!dirty)return true;Alert a=new Alert(Alert.AlertType.CONFIRMATION,"Hay cambios sin guardar. ¿Quieres descartarlos?",ButtonType.CANCEL,ButtonType.OK);a.setHeaderText("Cambios sin guardar");return a.showAndWait().orElse(ButtonType.CANCEL)==ButtonType.OK;}
    private void quickGuide(){info("2gameRL Studio en 60 segundos","1. Escena: Seleccionar edita objetos. Dibujar pinta tiles, Rellenar llena regiones y Borrar elimina celdas.\n2. Doble clic en un objeto abre Script.\n3. Inspector > Comportamientos: GridMovement mueve por losetas; PlayerController permite movimiento libre.\n4. Gráficos: Importar spritesheet conserva una sola textura y registra regiones virtuales.\n5. Ayuda > Tutorial de scripting (F1).\n6. ▶ Probar ejecuta el proyecto sin exportarlo.\n7. Exportar crea la aplicación autocontenida y un ZIP portable.");}
    private void updateTitle(){if(stage==null)return;String title="2gameRL Studio · "+project.getTitle()+(dirty?"  ●":"");stage.setTitle(title);windowTitle.setText(title);}
    private void applyCss(Scene scene){var css=getClass().getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());}
    void error(String header,String message){Alert a=new Alert(Alert.AlertType.ERROR,message==null?"Error desconocido":message,ButtonType.OK);a.setHeaderText(header);a.showAndWait();}
    void info(String header,String message){Alert a=new Alert(Alert.AlertType.INFORMATION,message,ButtonType.OK);a.setHeaderText(header);a.showAndWait();}
    public static void main(String[]args){launch(args);}
}
