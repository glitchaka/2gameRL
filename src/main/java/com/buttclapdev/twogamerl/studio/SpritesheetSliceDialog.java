package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.*;
import java.util.function.Consumer;

final class SpritesheetSliceDialog {
    private record Region(int x,int y,int width,int height) {}

    private final Asset source;
    private final Consumer<List<Asset>> consumer;
    private final Image previewImage;
    private final BufferedImage sourceImage;
    private final Canvas canvas=new Canvas();
    private final RadioButton gridMode=new RadioButton("Cuadrícula"), contourMode=new RadioButton("Por contorno");
    private final Spinner<Integer> cellWidth,cellHeight,marginX,marginY,spacingX,spacingY;
    private final Spinner<Integer> alphaThreshold,colorTolerance,minPixels,mergeGap,padding;
    private final Label info=new Label();
    private final Set<Integer> selectedRegions=new LinkedHashSet<>();
    private final List<Region> regions=new ArrayList<>();
    private int previewScale=1;

    private SpritesheetSliceDialog(Asset source,Image previewImage,Consumer<List<Asset>>consumer){
        this.source=source;this.previewImage=previewImage;this.consumer=consumer;this.sourceImage=source.image();
        int suggested=suggestCell(sourceImage);
        cellWidth=spinner(1,sourceImage.getWidth(),suggested);cellHeight=spinner(1,sourceImage.getHeight(),suggested);
        marginX=spinner(0,Math.max(0,sourceImage.getWidth()-1),0);marginY=spinner(0,Math.max(0,sourceImage.getHeight()-1),0);
        spacingX=spinner(0,Math.max(0,sourceImage.getWidth()-1),0);spacingY=spinner(0,Math.max(0,sourceImage.getHeight()-1),0);
        alphaThreshold=spinner(0,254,8);colorTolerance=spinner(0,255,12);minPixels=spinner(1,Math.max(1,sourceImage.getWidth()*sourceImage.getHeight()),12);
        mergeGap=spinner(0,128,3);padding=spinner(0,64,0);
    }

    static void show(Window owner,Asset source,Image previewImage,Consumer<List<Asset>>consumer){
        if(source==null||source.image()==null||previewImage==null)return;
        new SpritesheetSliceDialog(source,previewImage,consumer).show(owner);
    }

    private void show(Window owner){
        Stage stage=new Stage(StageStyle.UNDECORATED);if(owner!=null)stage.initOwner(owner);stage.initModality(Modality.WINDOW_MODAL);stage.setTitle("Definir spritesheet · 2gameRL");stage.setMinWidth(940);stage.setMinHeight(680);
        BorderPane root=new BorderPane();root.getStyleClass().addAll("studio-root","window-frame");root.setTop(titleBar(stage));

        ToggleGroup modes=new ToggleGroup();gridMode.setToggleGroup(modes);contourMode.setToggleGroup(modes);gridMode.setSelected(true);
        HBox modeBar=new HBox(12,gridMode,contourMode);modeBar.setAlignment(Pos.CENTER_LEFT);

        GridPane gridForm=form();addRow(gridForm,0,"Celda",new HBox(6,cellWidth,new Label("×"),cellHeight));addRow(gridForm,1,"Margen",new HBox(6,marginX,new Label("X"),marginY));addRow(gridForm,2,"Separación",new HBox(6,spacingX,new Label("X"),spacingY));
        Button p8=new Button("8×8"),p16=new Button("16×16"),p32=new Button("32×32"),p64=new Button("64×64");p8.setOnAction(e->setCell(8));p16.setOnAction(e->setCell(16));p32.setOnAction(e->setCell(32));p64.setOnAction(e->setCell(64));gridForm.add(new HBox(6,p8,p16,p32,p64),1,3);

        GridPane contourForm=form();addRow(contourForm,0,"Alpha ≤",alphaThreshold);addRow(contourForm,1,"Tolerancia fondo",colorTolerance);addRow(contourForm,2,"Píxeles mínimos",minPixels);addRow(contourForm,3,"Unir a distancia",mergeGap);addRow(contourForm,4,"Padding",padding);
        Label contourHint=hint("Si la hoja tiene transparencia, se detecta por alpha. Si es opaca, se usa el color de la esquina superior izquierda como fondo. 'Unir a distancia' agrupa partes cercanas del mismo sprite.");
        VBox contourBox=new VBox(6,contourForm,contourHint);contourBox.setDisable(true);

        Label hint=hint("La imagen NO se corta. Cada selección guarda solo x/y/ancho/alto sobre una única spritesheet. Haz clic sobre una región para activarla/desactivarla.");
        VBox controls=new VBox(10,title("SPRITESHEET"),modeBar,new Separator(),gridForm,contourBox,new Separator(),info,hint);controls.setPadding(new Insets(14));controls.setPrefWidth(360);controls.getStyleClass().add("side-panel");

        StackPane shell=new StackPane(canvas);shell.getStyleClass().add("canvas-shell");shell.setPadding(new Insets(24));ScrollPane scroll=new ScrollPane(shell);scroll.setPannable(true);scroll.setFitToWidth(true);scroll.setFitToHeight(true);root.setLeft(controls);root.setCenter(scroll);

        Button clear=new Button("Limpiar selección"),selected=new Button("Usar seleccionadas"),all=new Button("Usar todas"),cancel=new Button("Cancelar");all.getStyleClass().add("primary-button");
        clear.setOnAction(e->{selectedRegions.clear();redraw();});selected.setOnAction(e->accept(false,stage));all.setOnAction(e->accept(true,stage));cancel.setOnAction(e->stage.close());
        javafx.scene.layout.Region spacer=new javafx.scene.layout.Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox bottom=new HBox(8,clear,spacer,cancel,selected,all);bottom.setPadding(new Insets(10,14,14,14));bottom.setAlignment(Pos.CENTER_RIGHT);root.setBottom(bottom);

        ChangeListener<Integer>gridChanged=(o,a,b)->{if(gridMode.isSelected()){selectedRegions.clear();rebuildGeometry();}};
        for(Spinner<Integer>s:List.of(cellWidth,cellHeight,marginX,marginY,spacingX,spacingY))s.valueProperty().addListener(gridChanged);
        ChangeListener<Integer>contourChanged=(o,a,b)->{if(contourMode.isSelected()){selectedRegions.clear();rebuildGeometry();}};
        for(Spinner<Integer>s:List.of(alphaThreshold,colorTolerance,minPixels,mergeGap,padding))s.valueProperty().addListener(contourChanged);
        modes.selectedToggleProperty().addListener((o,a,b)->{boolean contour=contourMode.isSelected();gridForm.setDisable(contour);contourBox.setDisable(!contour);selectedRegions.clear();rebuildGeometry();});

        canvas.setOnMouseClicked(e->{
            if(e.getButton()!=MouseButton.PRIMARY)return;double ix=e.getX()/previewScale,iy=e.getY()/previewScale;
            for(int i=regions.size()-1;i>=0;i--){Region r=regions.get(i);if(ix>=r.x&&iy>=r.y&&ix<r.x+r.width&&iy<r.y+r.height){if(!selectedRegions.add(i))selectedRegions.remove(i);redraw();return;}}
        });

        Scene scene=new Scene(root,1160,780);var css=getClass().getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);rebuildGeometry();
        if(owner!=null){stage.setX(owner.getX()+Math.max(20,(owner.getWidth()-1160)/2));stage.setY(owner.getY()+Math.max(20,(owner.getHeight()-780)/2));}stage.showAndWait();
    }

    private void rebuildGeometry(){
        regions.clear();int w=sourceImage.getWidth(),h=sourceImage.getHeight();
        if(gridMode.isSelected()){
            int cw=cellWidth.getValue(),ch=cellHeight.getValue(),mx=marginX.getValue(),my=marginY.getValue(),sx=spacingX.getValue(),sy=spacingY.getValue();
            for(int y=my;y+ch<=h;y+=ch+sy)for(int x=mx;x+cw<=w;x+=cw+sx)regions.add(new Region(x,y,cw,ch));
            info.setText(regions.size()+" regiones por cuadrícula · "+w+"×"+h+" px");
        }else{
            List<SpriteRegionDetector.Box> found=SpriteRegionDetector.detect(sourceImage,alphaThreshold.getValue(),colorTolerance.getValue(),minPixels.getValue(),mergeGap.getValue(),padding.getValue());
            for(SpriteRegionDetector.Box b:found)regions.add(new Region(b.x(),b.y(),b.width(),b.height()));
            info.setText(regions.size()+" regiones detectadas por contorno · "+w+"×"+h+" px");
        }
        previewScale=powerOfTwoScale(w,h,760);canvas.setWidth(w*previewScale);canvas.setHeight(h*previewScale);redraw();
    }

    private void redraw(){
        GraphicsContext g=canvas.getGraphicsContext2D();g.setImageSmoothing(false);g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());g.drawImage(previewImage,0,0,canvas.getWidth(),canvas.getHeight());
        for(int i=0;i<regions.size();i++){Region r=regions.get(i);double x=r.x*previewScale,y=r.y*previewScale,w=r.width*previewScale,h=r.height*previewScale;boolean on=selectedRegions.contains(i);if(on){g.setFill(Color.rgb(72,184,255,.22));g.fillRect(x,y,w,h);g.setStroke(Color.web("#7ed1ff"));g.setLineWidth(2);}else{g.setStroke(Color.rgb(255,255,255,.60));g.setLineWidth(1);}g.strokeRect(x+.5,y+.5,Math.max(1,w-1),Math.max(1,h-1));g.setFill(on?Color.web("#7ed1ff"):Color.WHITE);g.fillText(Integer.toString(i+1),x+4,y+12);}
    }

    private void accept(boolean all,Stage stage){
        if(regions.isEmpty())return;if(!all&&selectedRegions.isEmpty()){Alert a=new Alert(Alert.AlertType.INFORMATION,"Selecciona al menos una región, o usa 'Usar todas'.",ButtonType.OK);a.initOwner(stage);a.showAndWait();return;}
        List<Asset> result=new ArrayList<>();String stem=source.key.replaceFirst("(?i)\\.[a-z0-9]+$","");int out=1;
        for(int i=0;i<regions.size();i++){if(!all&&!selectedRegions.contains(i))continue;Region r=regions.get(i);String key=String.format(Locale.ROOT,"%s_sprite_%03d",stem,out++);result.add(new Asset(key,key,source.key,r.x,r.y,r.width,r.height));}
        consumer.accept(result);stage.close();
    }

    private static int powerOfTwoScale(int w,int h,int limit){int scale=1;while(scale<16&&w*scale*2<=limit&&h*scale*2<=limit)scale*=2;return scale;}
    private void setCell(int size){cellWidth.getValueFactory().setValue(Math.min(size,sourceImage.getWidth()));cellHeight.getValueFactory().setValue(Math.min(size,sourceImage.getHeight()));}
    private static int suggestCell(BufferedImage image){for(int c:new int[]{32,16,64,8,24})if(image.getWidth()>=c&&image.getHeight()>=c&&image.getWidth()%c==0&&image.getHeight()%c==0)return c;return Math.max(1,Math.min(32,Math.min(image.getWidth(),image.getHeight())));}
    private static Spinner<Integer>spinner(int min,int max,int value){Spinner<Integer>s=new Spinner<>(min,Math.max(min,max),Math.max(min,Math.min(max,value)));s.setEditable(true);s.setPrefWidth(92);return s;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(8);g.setPadding(new Insets(6,0,6,0));return g;}
    private static void addRow(GridPane g,int row,String label,javafx.scene.Node value){g.add(new Label(label),0,row);g.add(value,1,row);}
    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static Label hint(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private static HBox titleBar(Stage stage){Label mark=new Label("2G"),title=new Label("Definir spritesheet");mark.getStyleClass().add("window-app-mark");title.getStyleClass().add("window-title");javafx.scene.layout.Region spacer=new javafx.scene.layout.Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button close=new Button("×");close.getStyleClass().addAll("window-control","window-close");close.setOnAction(e->stage.close());HBox bar=new HBox(9,mark,title,spacer,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("title-bar");double[]drag=new double[2];bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||e.getTarget() instanceof Button)return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});bar.setOnMouseDragged(e->{if(e.getTarget() instanceof Button)return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});return bar;}
}
