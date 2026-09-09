package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.AssetCategory;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;

final class PixelArtEditor {
    private enum Tool { PENCIL, ERASER, FILL }
    private int width=16,height=16;
    private int[][] pixels=new int[height][width];
    private final Canvas canvas=new Canvas(640,640);
    private final ColorPicker picker=new ColorPicker(Color.web("#55b7ff"));
    private Tool tool=Tool.PENCIL;
    private Asset editing;

    void show(Stage owner,Consumer<Asset> onSave){show(owner,null,null,key->true,onSave);}

    void show(Stage owner,Asset selected,Asset source,Predicate<String> available,Consumer<Asset> onSave){
        editing=selected;
        if(selected!=null)load(selected,source);
        Stage stage=new Stage();stage.initOwner(owner);stage.initModality(Modality.APPLICATION_MODAL);stage.setTitle(selected==null?"Pixel Lab · Nuevo sprite":"Pixel Lab · "+selected);
        BorderPane root=new BorderPane();root.getStyleClass().add("pixel-editor");
        Label title=new Label("Pixel Lab");title.getStyleClass().add("panel-title");
        Label subtitle=new Label(selected==null?"Crea sprites pixel-art sin salir del editor.":"Editando una copia de «"+selected+"». Guardar como crea un recurso nuevo y conserva el original.");subtitle.getStyleClass().add("muted");subtitle.setWrapText(true);
        VBox header=new VBox(2,title,subtitle);header.setPadding(new Insets(16,18,8,18));root.setTop(header);

        StackPane center=new StackPane(canvas);center.setPadding(new Insets(12));center.getStyleClass().add("canvas-shell");root.setCenter(center);
        ToggleGroup group=new ToggleGroup();ToggleButton pencil=toggle("Lápiz",group,true),eraser=toggle("Goma",group,false),fill=toggle("Relleno",group,false);
        pencil.setOnAction(e->tool=Tool.PENCIL);eraser.setOnAction(e->tool=Tool.ERASER);fill.setOnAction(e->tool=Tool.FILL);
        Spinner<Integer>w=new Spinner<>(1,256,width),h=new Spinner<>(1,256,height);w.setEditable(true);h.setEditable(true);w.setPrefWidth(80);h.setPrefWidth(80);
        Button applySize=new Button("Aplicar tamaño");applySize.setOnAction(e->resize(w.getValue(),h.getValue()));
        Button clear=new Button("Limpiar");clear.setOnAction(e->{pixels=new int[height][width];draw();});
        Button save=new Button(selected==null?"Guardar como sprite":"Guardar copia como…");save.getStyleClass().add("primary-button");save.setOnAction(e->{
            String suggested=selected==null?"sprite":baseName(selected.key)+"_edit";
            TextInputDialog dialog=new TextInputDialog(suggested);dialog.setTitle("Guardar sprite");dialog.setHeaderText("Nombre del nuevo recurso");dialog.setContentText("Nombre:");Optional<String>n=dialog.showAndWait();if(n.isEmpty()||n.get().isBlank())return;
            String key=n.get().trim().replaceAll("[^A-Za-z0-9._-]","_");if(!key.toLowerCase().endsWith(".png"))key+=".png";
            if(selected!=null&&key.equalsIgnoreCase(selected.key)){alert("Usa otro nombre","Pixel Lab no sobrescribe el sprite original. Elige otro nombre.");return;}
            if(!available.test(key)){alert("Nombre ya utilizado","Ya existe un recurso llamado «"+key+"». Elige otro nombre.");return;}
            try{Asset out=new Asset(key,key,png());out.category=selected==null?AssetCategory.SPRITE:(selected.category==AssetCategory.SPRITESHEET?AssetCategory.SPRITE:selected.category);if(selected!=null){out.filterMode=selected.filterMode;out.pivotX=selected.pivotX;out.pivotY=selected.pivotY;out.tags.addAll(selected.tags);}onSave.accept(out);stage.close();}
            catch(Exception ex){alert("No se pudo guardar",ex.getMessage());}
        });
        HBox tools=new HBox(8,new Label("Herramienta"),pencil,eraser,fill,new Separator(),new Label("Color"),picker,new Separator(),new Label("Ancho"),w,new Label("Alto"),h,applySize,clear,save);tools.setAlignment(Pos.CENTER_LEFT);tools.setPadding(new Insets(10,16,16,16));root.setBottom(tools);

        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,this::paint);canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,this::paint);
        Scene scene=new Scene(root,1120,780);var css=getClass().getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);ThemeManager.apply(scene);draw();stage.showAndWait();
    }

    private void load(Asset selected,Asset source){
        try{
            Asset physical=selected.isRegion()?source:selected;if(physical==null||physical.data==null)return;
            BufferedImage image=ImageIO.read(new ByteArrayInputStream(physical.data));if(image==null)return;
            int sx=selected.isRegion()?selected.regionX:0,sy=selected.isRegion()?selected.regionY:0;
            int sw=selected.isRegion()?selected.regionWidth:image.getWidth(),sh=selected.isRegion()?selected.regionHeight:image.getHeight();
            sw=Math.max(1,Math.min(256,Math.min(sw,image.getWidth()-sx)));sh=Math.max(1,Math.min(256,Math.min(sh,image.getHeight()-sy)));
            width=sw;height=sh;pixels=new int[height][width];for(int y=0;y<height;y++)for(int x=0;x<width;x++)pixels[y][x]=image.getRGB(sx+x,sy+y);
        }catch(Exception ex){pixels=new int[height][width];}
    }

    private ToggleButton toggle(String text,ToggleGroup group,boolean selected){ToggleButton b=new ToggleButton(text);b.setToggleGroup(group);b.setSelected(selected);return b;}
    private void paint(MouseEvent e){double cell=cell(),ox=(canvas.getWidth()-width*cell)/2,oy=(canvas.getHeight()-height*cell)/2;int x=(int)((e.getX()-ox)/cell),y=(int)((e.getY()-oy)/cell);if(e.getX()<ox||e.getY()<oy||x<0||y<0||x>=width||y>=height)return;if(tool==Tool.FILL){int old=pixels[y][x],next=argb(picker.getValue());flood(x,y,old,next);}else pixels[y][x]=tool==Tool.ERASER?0:argb(picker.getValue());draw();}
    private void flood(int sx,int sy,int old,int next){if(old==next)return;java.util.ArrayDeque<int[]>q=new java.util.ArrayDeque<>();q.add(new int[]{sx,sy});while(!q.isEmpty()){int[]p=q.removeFirst();int x=p[0],y=p[1];if(x<0||y<0||x>=width||y>=height||pixels[y][x]!=old)continue;pixels[y][x]=next;q.add(new int[]{x+1,y});q.add(new int[]{x-1,y});q.add(new int[]{x,y+1});q.add(new int[]{x,y-1});}}
    private void resize(int nw,int nh){nw=Math.max(1,Math.min(256,nw));nh=Math.max(1,Math.min(256,nh));int[][]next=new int[nh][nw];for(int y=0;y<Math.min(height,nh);y++)System.arraycopy(pixels[y],0,next[y],0,Math.min(width,nw));width=nw;height=nh;pixels=next;draw();}
    private double cell(){return Math.max(1,Math.min(canvas.getWidth()/width,canvas.getHeight()/height));}
    private void draw(){GraphicsContext g=canvas.getGraphicsContext2D();g.setImageSmoothing(false);g.setFill(Color.web("#111722"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());double cell=cell(),ox=(canvas.getWidth()-width*cell)/2,oy=(canvas.getHeight()-height*cell)/2;for(int y=0;y<height;y++)for(int x=0;x<width;x++){double px=ox+x*cell,py=oy+y*cell;boolean checker=((x+y)&1)==0;g.setFill(checker?Color.web("#202936"):Color.web("#18212d"));g.fillRect(px,py,cell,cell);int a=pixels[y][x];if((a>>>24)!=0){g.setFill(fromArgb(a));g.fillRect(px,py,cell,cell);}if(cell>=5){g.setStroke(Color.rgb(255,255,255,.07));g.strokeRect(px,py,cell,cell);}}g.setStroke(Color.rgb(255,255,255,.22));g.strokeRect(ox,oy,width*cell,height*cell);}
    private byte[]png()throws Exception{BufferedImage image=new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);for(int y=0;y<height;y++)for(int x=0;x<width;x++)image.setRGB(x,y,pixels[y][x]);ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}
    private static String baseName(String key){String n=key==null?"sprite":key;int slash=Math.max(n.lastIndexOf('/'),n.lastIndexOf('\\'));if(slash>=0)n=n.substring(slash+1);int dot=n.lastIndexOf('.');return dot>0?n.substring(0,dot):n;}
    private static int argb(Color c){return((int)Math.round(c.getOpacity()*255)<<24)|((int)Math.round(c.getRed()*255)<<16)|((int)Math.round(c.getGreen()*255)<<8)|(int)Math.round(c.getBlue()*255);}
    private static Color fromArgb(int a){return Color.rgb((a>>16)&255,(a>>8)&255,a&255,((a>>>24)&255)/255.0);}
    private static void alert(String title,String msg){Alert a=new Alert(Alert.AlertType.ERROR,msg==null?"":msg,ButtonType.OK);a.setHeaderText(title);a.showAndWait();}
}
