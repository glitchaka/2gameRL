package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.animation.AnimationTimer;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.lang.reflect.Field;
import java.util.Comparator;

/** Draws the logical game viewport over the editor world. */
final class CameraViewport222 {
    private static final String INSTALLED="2rl.camera.viewport.2.2.2";
    private CameraViewport222(){}

    static void install(SceneEditorPane editor,StudioApp app){
        if(Boolean.TRUE.equals(editor.getProperties().get(INSTALLED)))return;editor.getProperties().put(INSTALLED,true);
        Canvas world=field(editor,"canvas",Canvas.class);if(world==null||!(world.getParent() instanceof StackPane shell))return;
        Pane overlay=new Pane();overlay.setMouseTransparent(true);Rectangle frame=new Rectangle();frame.setFill(Color.TRANSPARENT);frame.setStroke(Color.web("#55C6FF"));frame.setStrokeWidth(2);frame.getStrokeDashArray().setAll(8.0,5.0);Label caption=new Label();caption.setStyle("-fx-background-color: rgba(5,15,24,.82);-fx-text-fill:#8fd7ff;-fx-padding:3 6 3 6;-fx-font-size:11px;");overlay.getChildren().addAll(frame,caption);shell.getChildren().add(overlay);overlay.prefWidthProperty().bind(world.widthProperty());overlay.prefHeightProperty().bind(world.heightProperty());overlay.maxWidthProperty().bind(world.widthProperty());overlay.maxHeightProperty().bind(world.heightProperty());
        new AnimationTimer(){@Override public void handle(long now){if(editor.getScene()==null)return;draw(app,editor,world,frame,caption);}}.start();
    }

    private static void draw(StudioApp app,SceneEditorPane editor,Canvas canvas,Rectangle frame,Label caption){
        Level level=currentLevel(editor);if(level==null||level.width<=0||level.height<=0){frame.setVisible(false);caption.setVisible(false);return;}double cell=canvas.getWidth()/Math.max(1,level.width);if(cell<=0){frame.setVisible(false);caption.setVisible(false);return;}
        EntityDef cameraEntity=level.entities.stream().filter(e->{ComponentDef c=e.component("Camera2D");return c!=null&&c.bool("enabled",true);}).max(Comparator.comparingDouble(e->e.component("Camera2D").number("priority",0))).orElse(null);
        double zoom=Math.max(.05,level.cameraZoom),centerX=level.width/2.0,centerY=level.height/2.0,offX=level.cameraOffsetX,offY=level.cameraOffsetY;String target=level.cameraTarget;String source="Escena";
        if(cameraEntity!=null){ComponentDef c=cameraEntity.component("Camera2D");zoom=Math.max(.05,c.number("zoom",zoom));offX=c.number("offsetX",offX);offY=c.number("offsetY",offY);target=c.get("target",target);if(target.isBlank()&&c.bool("follow",true))target=cameraEntity.id;source="Camera2D · "+cameraEntity.name;}
        EntityDef targetEntity=findEntity(level,target);if(targetEntity!=null){centerX=targetEntity.x+targetEntity.width/2;centerY=targetEntity.y+targetEntity.height/2;}else if(cameraEntity!=null&&!cameraEntity.component("Camera2D").bool("follow",true)){centerX=cameraEntity.x+cameraEntity.width/2;centerY=cameraEntity.y+cameraEntity.height/2;}
        double viewW=app.project().getLogicalWidth()/(Math.max(1,app.project().getTileSize())*zoom),viewH=app.project().getLogicalHeight()/(Math.max(1,app.project().getTileSize())*zoom),x=centerX-viewW/2+offX,y=centerY-viewH/2+offY;if(level.width<=viewW)x=(level.width-viewW)/2;else x=Math.max(0,Math.min(level.width-viewW,x));if(level.height<=viewH)y=(level.height-viewH)/2;else y=Math.max(0,Math.min(level.height-viewH,y));
        frame.setX(x*cell);frame.setY(y*cell);frame.setWidth(viewW*cell);frame.setHeight(viewH*cell);frame.setVisible(true);caption.setText(source+"  ·  "+app.project().getLogicalWidth()+"×"+app.project().getLogicalHeight()+"  ·  zoom "+trim(zoom));caption.relocate(Math.max(0,frame.getX()+4),Math.max(0,frame.getY()+4));caption.setVisible(true);
    }

    private static Level currentLevel(SceneEditorPane editor){Object picker=fieldRaw(editor,"scenePicker");if(picker instanceof javafx.scene.control.ComboBox<?> c&&c.getValue() instanceof Level l)return l;return null;}
    private static EntityDef findEntity(Level l,String ref){if(ref==null||ref.isBlank())return null;return l.entities.stream().filter(e->e.id.equalsIgnoreCase(ref)||e.name.equalsIgnoreCase(ref)).findFirst().orElse(null);}
    private static String trim(double v){return Math.abs(v-Math.rint(v))<1e-9?Long.toString(Math.round(v)):String.format(java.util.Locale.ROOT,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","");}
    private static Object fieldRaw(Object owner,String name){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(Exception e){return null;}}
    private static <T>T field(Object owner,String name,Class<T>type){Object v=fieldRaw(owner,name);return type.isInstance(v)?type.cast(v):null;}
}
