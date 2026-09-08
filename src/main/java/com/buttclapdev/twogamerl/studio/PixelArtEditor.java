package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.function.Consumer;

final class PixelArtEditor {
    private enum Tool { PENCIL, ERASER, FILL }
    private int size = 16;
    private int[][] pixels = new int[size][size];
    private final Canvas canvas = new Canvas(576, 576);
    private final ColorPicker picker = new ColorPicker(Color.web("#55b7ff"));
    private Tool tool = Tool.PENCIL;

    void show(Stage owner, Consumer<Asset> onSave) {
        Stage stage = new Stage(); stage.initOwner(owner); stage.initModality(Modality.APPLICATION_MODAL); stage.setTitle("Pixel Lab · Placeholder");
        BorderPane root = new BorderPane(); root.getStyleClass().add("pixel-editor");
        Label title = new Label("Pixel Lab"); title.getStyleClass().add("panel-title");
        Label subtitle = new Label("Crea sprites rápidos para prototipar sin salir del editor."); subtitle.getStyleClass().add("muted");
        VBox header = new VBox(2,title,subtitle); header.setPadding(new Insets(16,18,8,18)); root.setTop(header);

        StackPane center = new StackPane(canvas); center.setPadding(new Insets(12)); center.getStyleClass().add("canvas-shell"); root.setCenter(center);
        ToggleGroup group = new ToggleGroup(); ToggleButton pencil=toggle("Lápiz",group,true), eraser=toggle("Goma",group,false), fill=toggle("Relleno",group,false);
        pencil.setOnAction(e->tool=Tool.PENCIL);eraser.setOnAction(e->tool=Tool.ERASER);fill.setOnAction(e->tool=Tool.FILL);
        ComboBox<Integer> sizes=new ComboBox<>();sizes.getItems().addAll(8,16,24,32);sizes.setValue(16);
        sizes.setOnAction(e->resize(sizes.getValue()));
        Button clear=new Button("Limpiar");clear.setOnAction(e->{pixels=new int[size][size];draw();});
        Button save=new Button("Guardar como sprite");save.getStyleClass().add("primary-button");save.setOnAction(e->{
            Optional<String> n=new TextInputDialog("placeholder").showAndWait();if(n.isEmpty()||n.get().isBlank())return;
            try{String key=n.get().trim().replaceAll("[^A-Za-z0-9._-]","_");if(!key.toLowerCase().endsWith(".png"))key+=".png";onSave.accept(new Asset(key,key,png()));stage.close();}
            catch(Exception ex){alert("No se pudo guardar",ex.getMessage());}
        });
        HBox tools=new HBox(8,new Label("Herramienta"),pencil,eraser,fill,new Separator(),new Label("Color"),picker,new Separator(),new Label("Tamaño"),sizes,clear,save);tools.setAlignment(Pos.CENTER_LEFT);tools.setPadding(new Insets(10,16,16,16));root.setBottom(tools);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,this::paint);canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,this::paint);
        Scene scene=new Scene(root,900,720);var css=getClass().getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);draw();stage.showAndWait();
    }

    private ToggleButton toggle(String text,ToggleGroup group,boolean selected){ToggleButton b=new ToggleButton(text);b.setToggleGroup(group);b.setSelected(selected);return b;}
    private void paint(MouseEvent e){int x=(int)(e.getX()/(canvas.getWidth()/size)),y=(int)(e.getY()/(canvas.getHeight()/size));if(x<0||y<0||x>=size||y>=size)return;if(tool==Tool.FILL){int old=pixels[y][x],next=argb(picker.getValue());flood(x,y,old,next);}else pixels[y][x]=tool==Tool.ERASER?0:argb(picker.getValue());draw();}
    private void flood(int sx,int sy,int old,int next){if(old==next)return;java.util.ArrayDeque<int[]>q=new java.util.ArrayDeque<>();q.add(new int[]{sx,sy});while(!q.isEmpty()){int[]p=q.removeFirst();int x=p[0],y=p[1];if(x<0||y<0||x>=size||y>=size||pixels[y][x]!=old)continue;pixels[y][x]=next;q.add(new int[]{x+1,y});q.add(new int[]{x-1,y});q.add(new int[]{x,y+1});q.add(new int[]{x,y-1});}}
    private void resize(int n){int[][]next=new int[n][n];for(int y=0;y<Math.min(size,n);y++)System.arraycopy(pixels[y],0,next[y],0,Math.min(size,n));size=n;pixels=next;draw();}
    private void draw(){GraphicsContext g=canvas.getGraphicsContext2D();double cell=canvas.getWidth()/size;g.setFill(Color.web("#111722"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());for(int y=0;y<size;y++)for(int x=0;x<size;x++){int a=pixels[y][x];if((a>>>24)!=0){g.setFill(fromArgb(a));g.fillRect(x*cell,y*cell,cell,cell);}g.setStroke(Color.rgb(255,255,255,.08));g.strokeRect(x*cell,y*cell,cell,cell);}}
    private byte[] png() throws Exception{BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<size;y++)for(int x=0;x<size;x++)image.setRGB(x,y,pixels[y][x]);ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
    private static int argb(Color c){return((int)Math.round(c.getOpacity()*255)<<24)|((int)Math.round(c.getRed()*255)<<16)|((int)Math.round(c.getGreen()*255)<<8)|(int)Math.round(c.getBlue()*255);}
    private static Color fromArgb(int a){return Color.rgb((a>>16)&255,(a>>8)&255,a&255,((a>>>24)&255)/255.0);}
    private static void alert(String title,String msg){Alert a=new Alert(Alert.AlertType.ERROR,msg,ButtonType.OK);a.setHeaderText(title);a.showAndWait();}
}
