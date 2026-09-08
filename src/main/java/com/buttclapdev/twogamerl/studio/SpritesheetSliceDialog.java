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
    private final Asset source;
    private final Consumer<List<Asset>> consumer;
    private final Image previewImage;
    private final BufferedImage sourceImage;
    private final Canvas canvas=new Canvas();
    private final Spinner<Integer> cellWidth,cellHeight,marginX,marginY,spacingX,spacingY;
    private final Label info=new Label();
    private final Set<Integer> selectedCells=new LinkedHashSet<>();
    private int columns,rows,previewScale=1;

    private SpritesheetSliceDialog(Asset source,Image previewImage,Consumer<List<Asset>>consumer){this.source=source;this.previewImage=previewImage;this.consumer=consumer;this.sourceImage=source.image();int suggested=suggestCell(sourceImage);cellWidth=spinner(1,sourceImage.getWidth(),suggested);cellHeight=spinner(1,sourceImage.getHeight(),suggested);marginX=spinner(0,Math.max(0,sourceImage.getWidth()-1),0);marginY=spinner(0,Math.max(0,sourceImage.getHeight()-1),0);spacingX=spinner(0,Math.max(0,sourceImage.getWidth()-1),0);spacingY=spinner(0,Math.max(0,sourceImage.getHeight()-1),0);}
    static void show(Window owner,Asset source,Image previewImage,Consumer<List<Asset>>consumer){if(source==null||source.image()==null||previewImage==null)return;new SpritesheetSliceDialog(source,previewImage,consumer).show(owner);}

    private void show(Window owner){Stage stage=new Stage(StageStyle.UNDECORATED);if(owner!=null)stage.initOwner(owner);stage.initModality(Modality.WINDOW_MODAL);stage.setTitle("Cortar spritesheet · 2gameRL");stage.setMinWidth(900);stage.setMinHeight(650);BorderPane root=new BorderPane();root.getStyleClass().addAll("studio-root","window-frame");root.setTop(titleBar(stage));
        GridPane form=new GridPane();form.setHgap(8);form.setVgap(8);form.setPadding(new Insets(12));addRow(form,0,"Celda",new HBox(6,cellWidth,new Label("×"),cellHeight));addRow(form,1,"Margen",new HBox(6,marginX,new Label("X"),marginY));addRow(form,2,"Separación",new HBox(6,spacingX,new Label("X"),spacingY));Button p8=new Button("8×8"),p16=new Button("16×16"),p32=new Button("32×32"),p64=new Button("64×64");p8.setOnAction(e->setCell(8));p16.setOnAction(e->setCell(16));p32.setOnAction(e->setCell(32));p64.setOnAction(e->setCell(64));form.add(new HBox(6,p8,p16,p32,p64),1,3);Label hint=new Label("La rejilla representa el corte exacto. Los PNG resultantes copian cada píxel sin resize ni interpolación. Haz clic para seleccionar celdas.");hint.setWrapText(true);hint.getStyleClass().add("muted");VBox controls=new VBox(10,title("CORTE EXACTO"),form,new Separator(),info,hint);controls.setPadding(new Insets(14));controls.setPrefWidth(320);controls.getStyleClass().add("side-panel");
        StackPane shell=new StackPane(canvas);shell.getStyleClass().add("canvas-shell");shell.setPadding(new Insets(24));ScrollPane scroll=new ScrollPane(shell);scroll.setPannable(true);scroll.setFitToWidth(true);scroll.setFitToHeight(true);root.setLeft(controls);root.setCenter(scroll);
        Button clear=new Button("Limpiar selección"),selected=new Button("Cortar seleccionadas"),all=new Button("Cortar toda la hoja"),cancel=new Button("Cancelar");all.getStyleClass().add("primary-button");clear.setOnAction(e->{selectedCells.clear();redraw();});selected.setOnAction(e->cut(false,stage));all.setOnAction(e->cut(true,stage));cancel.setOnAction(e->stage.close());Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);HBox bottom=new HBox(8,clear,spacer,cancel,selected,all);bottom.setPadding(new Insets(10,14,14,14));bottom.setAlignment(Pos.CENTER_RIGHT);root.setBottom(bottom);
        ChangeListener<Integer>changed=(o,a,b)->{selectedCells.clear();rebuildGeometry();};for(Spinner<Integer>s:List.of(cellWidth,cellHeight,marginX,marginY,spacingX,spacingY))s.valueProperty().addListener(changed);
        canvas.setOnMouseClicked(e->{if(e.getButton()!=MouseButton.PRIMARY||columns<=0||rows<=0)return;double ix=e.getX()/previewScale,iy=e.getY()/previewScale;int cw=cellWidth.getValue(),ch=cellHeight.getValue(),sx=spacingX.getValue(),sy=spacingY.getValue(),mx=marginX.getValue(),my=marginY.getValue();int col=(int)Math.floor((ix-mx)/Math.max(1,cw+sx)),row=(int)Math.floor((iy-my)/Math.max(1,ch+sy));if(col<0||row<0||col>=columns||row>=rows)return;double lx=ix-(mx+col*(cw+sx)),ly=iy-(my+row*(ch+sy));if(lx<0||ly<0||lx>=cw||ly>=ch)return;int index=row*columns+col;if(!selectedCells.add(index))selectedCells.remove(index);redraw();});
        Scene scene=new Scene(root,1120,760);var css=getClass().getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);rebuildGeometry();if(owner!=null){stage.setX(owner.getX()+Math.max(20,(owner.getWidth()-1120)/2));stage.setY(owner.getY()+Math.max(20,(owner.getHeight()-760)/2));}stage.showAndWait();}

    private void rebuildGeometry(){int w=sourceImage.getWidth(),h=sourceImage.getHeight();columns=SpriteSheetSlicer.countCells(w,marginX.getValue(),cellWidth.getValue(),spacingX.getValue());rows=SpriteSheetSlicer.countCells(h,marginY.getValue(),cellHeight.getValue(),spacingY.getValue());previewScale=powerOfTwoScale(w,h,720);canvas.setWidth(w*previewScale);canvas.setHeight(h*previewScale);info.setText(columns+" × "+rows+" = "+(columns*rows)+" celdas · "+w+"×"+h+" px · vista "+previewScale+"×");redraw();}
    private static int powerOfTwoScale(int w,int h,int limit){int scale=1;while(scale<16&&w*scale*2<=limit&&h*scale*2<=limit)scale*=2;return scale;}
    private void redraw(){GraphicsContext g=canvas.getGraphicsContext2D();g.setImageSmoothing(false);g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());g.drawImage(previewImage,0,0,canvas.getWidth(),canvas.getHeight());int cw=cellWidth.getValue(),ch=cellHeight.getValue(),mx=marginX.getValue(),my=marginY.getValue(),sx=spacingX.getValue(),sy=spacingY.getValue();for(int row=0;row<rows;row++)for(int col=0;col<columns;col++){double x=(mx+col*(cw+sx))*previewScale,y=(my+row*(ch+sy))*previewScale,w=cw*previewScale,h=ch*previewScale;int index=row*columns+col;if(selectedCells.contains(index)){g.setFill(Color.rgb(72,184,255,.25));g.fillRect(x,y,w,h);g.setStroke(Color.web("#7ed1ff"));g.setLineWidth(2);}else{g.setStroke(Color.rgb(255,255,255,.55));g.setLineWidth(1);}g.strokeRect(x+.5,y+.5,w-1,h-1);}}
    private void cut(boolean all,Stage stage){if(columns<=0||rows<=0)return;if(!all&&selectedCells.isEmpty()){Alert a=new Alert(Alert.AlertType.INFORMATION,"Selecciona al menos una celda, o usa 'Cortar toda la hoja'.",ButtonType.OK);a.initOwner(stage);a.showAndWait();return;}try{List<Asset>result=SpriteSheetSlicer.slice(source,cellWidth.getValue(),cellHeight.getValue(),marginX.getValue(),marginY.getValue(),spacingX.getValue(),spacingY.getValue(),selectedCells,all);consumer.accept(result);stage.close();}catch(Exception ex){Alert a=new Alert(Alert.AlertType.ERROR,ex.getMessage(),ButtonType.OK);a.initOwner(stage);a.setHeaderText("No se pudo cortar la spritesheet");a.showAndWait();}}
    private void setCell(int size){cellWidth.getValueFactory().setValue(Math.min(size,sourceImage.getWidth()));cellHeight.getValueFactory().setValue(Math.min(size,sourceImage.getHeight()));}
    private static int suggestCell(BufferedImage image){for(int c:new int[]{32,16,64,8,24})if(image.getWidth()>=c&&image.getHeight()>=c&&image.getWidth()%c==0&&image.getHeight()%c==0)return c;return Math.max(1,Math.min(32,Math.min(image.getWidth(),image.getHeight())));}
    private static Spinner<Integer>spinner(int min,int max,int value){Spinner<Integer>s=new Spinner<>(min,Math.max(min,max),Math.max(min,Math.min(max,value)));s.setEditable(true);s.setPrefWidth(86);return s;}
    private static void addRow(GridPane g,int row,String label,javafx.scene.Node value){g.add(new Label(label),0,row);g.add(value,1,row);}private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static HBox titleBar(Stage stage){Label mark=new Label("2G"),title=new Label("Cortar spritesheet");mark.getStyleClass().add("window-app-mark");title.getStyleClass().add("window-title");Region spacer=new Region();HBox.setHgrow(spacer,Priority.ALWAYS);Button close=new Button("×");close.getStyleClass().addAll("window-control","window-close");close.setOnAction(e->stage.close());HBox bar=new HBox(9,mark,title,spacer,close);bar.setAlignment(Pos.CENTER_LEFT);bar.getStyleClass().add("title-bar");double[]drag=new double[2];bar.setOnMousePressed(e->{if(e.getButton()!=MouseButton.PRIMARY||e.getTarget() instanceof Button)return;drag[0]=e.getSceneX();drag[1]=e.getSceneY();});bar.setOnMouseDragged(e->{if(e.getTarget() instanceof Button)return;stage.setX(e.getScreenX()-drag[0]);stage.setY(e.getScreenY()-drag[1]);});return bar;}
}
