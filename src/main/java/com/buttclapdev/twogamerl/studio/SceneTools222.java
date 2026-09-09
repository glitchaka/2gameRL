package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.lang.reflect.Method;
import java.util.*;

/** 2.2.2 scene hardening: tile shape tools, typed resource selectors, semantic validation and camera guides. */
final class SceneTools222 {
    private static final String INSTALLED="2rl.scene.tools.2.2.2",ENHANCED="2rl.scene.card.2.2.2";
    private SceneTools222(){}

    static void install(SceneEditorPane pane,StudioApp app){
        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;pane.getProperties().put(INSTALLED,true);
        HBox toolbar=findByStyle(pane,HBox.class,"context-toolbar");Canvas base=findFirst(pane,Canvas.class);if(toolbar==null||base==null)return;
        ToggleGroup group=toolbar.getChildren().stream().filter(ToggleButton.class::isInstance).map(ToggleButton.class::cast).map(ToggleButton::getToggleGroup).filter(Objects::nonNull).findFirst().orElseGet(ToggleGroup::new);
        ToggleButton line=new ToggleButton("Línea"),rect=new ToggleButton("Rectángulo"),pick=new ToggleButton("Cuentagotas");for(ToggleButton b:List.of(line,rect,pick)){b.setToggleGroup(group);b.getStyleClass().add("scene-tool-222");}
        int insert=Math.max(0,toolbar.getChildren().size()-4);toolbar.getChildren().addAll(insert,line,rect,pick);
        double[]start={0,0};boolean[]armed={false};
        base.addEventFilter(MouseEvent.MOUSE_PRESSED,e->{if(!customSelected(line,rect,pick))return;if(e.getButton()!=javafx.scene.input.MouseButton.PRIMARY)return;int x=cellX(base,pane,app,e.getX()),y=cellY(base,pane,app,e.getY());if(pick.isSelected()){eyedrop(pane,app,x,y);e.consume();return;}start[0]=x;start[1]=y;armed[0]=true;e.consume();});
        base.addEventFilter(MouseEvent.MOUSE_DRAGGED,e->{if(customSelected(line,rect,pick)){e.consume();}});
        base.addEventFilter(MouseEvent.MOUSE_RELEASED,e->{if(!armed[0]||(!line.isSelected()&&!rect.isSelected()))return;armed[0]=false;int x=cellX(base,pane,app,e.getX()),y=cellY(base,pane,app,e.getY());if(line.isSelected())paintLine(pane,app,(int)start[0],(int)start[1],x,y);else paintRect(pane,app,(int)start[0],(int)start[1],x,y,e.isShiftDown());invokeRedraw(pane);e.consume();});
        installSemanticValidation(pane,app);
        installCameraOverlay(pane,app,base);
        AnimationTimer watcher=new AnimationTimer(){@Override public void handle(long now){enhanceTypedEditors(pane,app);}};watcher.start();pane.parentProperty().addListener((o,a,b)->{if(a!=null&&b==null)watcher.stop();});
        Platform.runLater(()->enhanceTypedEditors(pane,app));
    }

    private static boolean customSelected(ToggleButton...buttons){for(ToggleButton b:buttons)if(b.isSelected())return true;return false;}
    private static int cellX(Canvas base,SceneEditorPane pane,StudioApp app,double x){return(int)Math.floor(x/cellSize(pane,app));}private static int cellY(Canvas base,SceneEditorPane pane,StudioApp app,double y){return(int)Math.floor(y/cellSize(pane,app));}
    private static double cellSize(SceneEditorPane pane,StudioApp app){ComboBox<?>zoom=comboOf(pane,Integer.class);int z=zoom!=null&&zoom.getValue() instanceof Integer i?Math.max(1,i):1;return app.project().getTileSize()*z;}
    @SuppressWarnings("unchecked") private static Level level(SceneEditorPane pane){ComboBox<?>c=comboOf(pane,Level.class);return c==null?null:(Level)c.getValue();}
    @SuppressWarnings("unchecked") private static PrefabDef prefab(SceneEditorPane pane){ComboBox<?>c=comboOf(pane,PrefabDef.class);return c==null?null:(PrefabDef)c.getValue();}
    @SuppressWarnings("unchecked") private static ListView<TileLayer>layerList(SceneEditorPane pane){for(ListView<?>v:findAll(pane,ListView.class)){Object value=v.getSelectionModel().getSelectedItem();if(value instanceof TileLayer)return(ListView<TileLayer>)v;if(!v.getItems().isEmpty()&&v.getItems().getFirst() instanceof TileLayer)return(ListView<TileLayer>)v;}return null;}
    private static TileLayer activeLayer(SceneEditorPane pane){Level l=level(pane);ListView<TileLayer>v=layerList(pane);TileLayer selected=v==null?null:v.getSelectionModel().getSelectedItem();return selected!=null?selected:l==null?null:l.baseLayer();}
    private static int tileId(SceneEditorPane pane,StudioApp app){PrefabDef p=prefab(pane);if(p==null||p.placement==PlacementMode.ENTITY)return-1;if(p.tileId>=0&&app.project().getTiles().containsKey(p.tileId))return p.tileId;int id=app.project().nextTileId();String asset=p.template==null?"":p.template.assetKey;app.project().getTiles().put(id,new TileDef(id,p.name,new java.awt.Color(80,88,104),true,asset));p.tileId=id;return id;}
    private static void put(SceneEditorPane pane,StudioApp app,int x,int y,int id){Level l=level(pane);TileLayer layer=activeLayer(pane);if(l==null||layer==null||layer.locked||id<0||x<0||y<0||x>=l.width||y>=l.height)return;layer.set(x,y,id);}
    private static void paintLine(SceneEditorPane pane,StudioApp app,int x0,int y0,int x1,int y1){int id=tileId(pane,app);if(id<0){app.status("Línea requiere un Pincel TILEMAP/AUTO.");return;}int dx=Math.abs(x1-x0),sx=x0<x1?1:-1,dy=-Math.abs(y1-y0),sy=y0<y1?1:-1,err=dx+dy;for(;;){put(pane,app,x0,y0,id);if(x0==x1&&y0==y1)break;int e2=2*err;if(e2>=dy){err+=dy;x0+=sx;}if(e2<=dx){err+=dx;y0+=sy;}}app.changed();}
    private static void paintRect(SceneEditorPane pane,StudioApp app,int x0,int y0,int x1,int y1,boolean fill){int id=tileId(pane,app);if(id<0){app.status("Rectángulo requiere un Pincel TILEMAP/AUTO.");return;}int minX=Math.min(x0,x1),maxX=Math.max(x0,x1),minY=Math.min(y0,y1),maxY=Math.max(y0,y1);for(int y=minY;y<=maxY;y++)for(int x=minX;x<=maxX;x++)if(fill||x==minX||x==maxX||y==minY||y==maxY)put(pane,app,x,y,id);app.changed();}
    @SuppressWarnings("unchecked") private static void eyedrop(SceneEditorPane pane,StudioApp app,int x,int y){TileLayer layer=activeLayer(pane);if(layer==null)return;int id=layer.get(x,y);if(id<0){app.status("Cuentagotas: celda vacía.");return;}ComboBox<?>box=comboOf(pane,PrefabDef.class);if(box==null)return;for(Object o:box.getItems())if(o instanceof PrefabDef p&&p.tileId==id){((ComboBox<PrefabDef>)box).setValue(p);app.status("Pincel seleccionado: "+p.name);return;}app.status("Cuentagotas: el tile "+id+" no tiene Pincel asociado.");}

    private static void installSemanticValidation(SceneEditorPane pane,StudioApp app){TextArea script=findAll(pane,TextArea.class).stream().filter(a->{String s=Objects.toString(a.getStyle(),"").toLowerCase(Locale.ROOT);return s.contains("monospace")||s.contains("consolas");}).findFirst().orElse(null);if(script==null||!(script.getParent() instanceof VBox box))return;Label semantic=new Label();semantic.getStyleClass().add("muted");semantic.setWrapText(true);semantic.getProperties().put(INSTALLED,true);int at=Math.max(0,box.getChildren().indexOf(script));box.getChildren().add(at,semantic);Runnable validate=()->{List<String>errors=ScriptSemantic222.validate(app.project(),script.getText());semantic.setText(errors.isEmpty()?"✓ Referencias del proyecto válidas":String.join(" | ",errors));semantic.setStyle(errors.isEmpty()?"-fx-text-fill:#74d99f;":"-fx-text-fill:#ff7d7d;");};script.textProperty().addListener((o,a,b)->validate.run());validate.run();}

    private static void enhanceTypedEditors(SceneEditorPane pane,StudioApp app){for(VBox card:findAll(pane,VBox.class))if(card.getStyleClass().contains("component-card")&&!Boolean.TRUE.equals(card.getProperties().get(ENHANCED))){String type=componentType(card);if(type==null)continue;if(type.equals("ParticleEmitter2D"))replaceField(card,"preset",particleRefs(app),app);if(type.equals("Camera2D"))replaceField(card,"target",entityRefs(pane),app);card.getProperties().put(ENHANCED,true);}for(HBox row:findAll(pane,HBox.class)){if(Boolean.TRUE.equals(row.getProperties().get(ENHANCED))||row.getChildren().size()<2||!(row.getChildren().getFirst() instanceof Label label)||!"Camera target".equals(label.getText())||!(row.getChildren().get(1) instanceof TextField field))continue;replace(row,field,entityRefs(pane));row.getProperties().put(ENHANCED,true);}}
    private static String componentType(VBox card){for(Node n:card.getChildren())if(n instanceof HBox h)for(Node c:h.getChildren())if(c instanceof Label l){String text=l.getText();if(text!=null){for(String type:GameProject.BUILTIN_COMPONENTS)if(text.startsWith(type))return type;}}return null;}
    private static void replaceField(VBox card,String property,List<String>values,StudioApp app){for(HBox row:findAll(card,HBox.class)){if(row.getChildren().size()<2||!(row.getChildren().getFirst() instanceof Label label)||!property.equals(label.getText())||!(row.getChildren().get(1) instanceof TextField field))continue;replace(row,field,values);return;}}
    private static void replace(HBox row,TextField field,List<String>values){ComboBox<String>picker=new ComboBox<>();picker.getItems().add("(ninguno)");picker.getItems().addAll(values);picker.setMaxWidth(Double.MAX_VALUE);HBox.setHgrow(picker,javafx.scene.layout.Priority.ALWAYS);String current=field.getText();picker.setValue(current==null||current.isBlank()?"(ninguno)":current);picker.setOnAction(e->{field.setText("(ninguno)".equals(picker.getValue())?"":Objects.toString(picker.getValue(),""));field.fireEvent(new ActionEvent());});row.getChildren().set(1,picker);}
    private static List<String>particleRefs(StudioApp app){return new ArrayList<>(app.project().getParticlePresets().keySet());}
    private static List<String>entityRefs(SceneEditorPane pane){Level l=level(pane);return l==null?List.of():l.entities.stream().map(e->e.id).toList();}

    private static void installCameraOverlay(SceneEditorPane pane,StudioApp app,Canvas base){if(!(base.getParent() instanceof StackPane stack))return;Canvas overlay=new Canvas();overlay.setMouseTransparent(true);overlay.widthProperty().bind(base.widthProperty());overlay.heightProperty().bind(base.heightProperty());stack.getChildren().add(overlay);AnimationTimer timer=new AnimationTimer(){@Override public void handle(long now){drawCameraGuides(overlay,pane,app);}};timer.start();pane.parentProperty().addListener((o,a,b)->{if(a!=null&&b==null)timer.stop();});}
    private static void drawCameraGuides(Canvas canvas,SceneEditorPane pane,StudioApp app){GraphicsContext g=canvas.getGraphicsContext2D();g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());Level l=level(pane);if(l==null)return;double cell=cellSize(pane,app);ComponentDef camera=null;EntityDef cameraEntity=null;double best=-Double.MAX_VALUE;for(EntityDef e:l.entities){ComponentDef c=e.component("Camera2D");if(c!=null&&c.bool("enabled",true)&&c.number("priority",0)>=best){best=c.number("priority",0);camera=c;cameraEntity=e;}}String target=l.cameraTarget;double zoom=Math.max(.05,l.cameraZoom),offX=l.cameraOffsetX,offY=l.cameraOffsetY,deadX=0,deadY=0;if(camera!=null){target=camera.get("target",target);if(target.isBlank()&&camera.bool("follow",true)&&cameraEntity!=null)target=cameraEntity.id;zoom=Math.max(.05,camera.number("zoom",zoom));offX=camera.number("offsetX",offX);offY=camera.number("offsetY",offY);deadX=Math.max(0,camera.number("deadZoneX",0));deadY=Math.max(0,camera.number("deadZoneY",0));}EntityDef t=null;for(EntityDef e:l.entities)if(e.id.equalsIgnoreCase(target)||e.name.equalsIgnoreCase(target)){t=e;break;}if(t==null)for(EntityDef e:l.entities)if(e.has("PlayerController")||e.has("PlatformerController")||e.has("GridMovement")){t=e;break;}double cx=t==null?l.width/2.0:t.x+t.width/2,cy=t==null?l.height/2.0:t.y+t.height/2,viewW=(double)app.project().getLogicalWidth()/(app.project().getTileSize()*zoom),viewH=(double)app.project().getLogicalHeight()/(app.project().getTileSize()*zoom),x=cx-viewW/2+offX,y=cy-viewH/2+offY;if(l.width<=viewW)x=(l.width-viewW)/2;else x=Math.max(0,Math.min(l.width-viewW,x));if(l.height<=viewH)y=(l.height-viewH)/2;else y=Math.max(0,Math.min(l.height-viewH,y));g.setStroke(Color.rgb(66,199,255,.9));g.setLineWidth(2);g.strokeRect(x*cell,y*cell,viewW*cell,viewH*cell);g.setStroke(Color.rgb(255,191,85,.85));g.strokeLine((x+viewW/2)*cell,y*cell,(x+viewW/2)*cell,(y+viewH)*cell);g.strokeLine(x*cell,(y+viewH/2)*cell,(x+viewW)*cell,(y+viewH/2)*cell);if(deadX>0||deadY>0){g.setStroke(Color.rgb(255,106,168,.85));g.strokeRect((cx-deadX/2)*cell,(cy-deadY/2)*cell,deadX*cell,deadY*cell);}}

    private static void invokeRedraw(SceneEditorPane pane){try{Method m=SceneEditorPane.class.getDeclaredMethod("redraw");m.setAccessible(true);m.invoke(pane);}catch(Exception ignored){}}
    private static <T extends Node>T findByStyle(Node root,Class<T>type,String style){for(T n:findAll(root,type))if(n.getStyleClass().contains(style))return n;return null;}
    private static <T extends Node>T findFirst(Node root,Class<T>type){if(type.isInstance(root))return type.cast(root);if(root instanceof Parent p)for(Node c:p.getChildrenUnmodifiable()){T f=findFirst(c,type);if(f!=null)return f;}return null;}
    private static <T extends Node>List<T>findAll(Node root,Class<T>type){ArrayList<T>out=new ArrayList<>();walk(root,type,out);return out;}private static <T extends Node>void walk(Node n,Class<T>type,List<T>out){if(type.isInstance(n))out.add(type.cast(n));if(n instanceof Parent p)for(Node c:p.getChildrenUnmodifiable())walk(c,type,out);}
    private static ComboBox<?>comboOf(Node root,Class<?>kind){for(ComboBox<?>c:findAll(root,ComboBox.class)){Object v=c.getValue();if(v!=null&&kind.isInstance(v))return c;for(Object i:c.getItems())if(i!=null&&kind.isInstance(i))return c;}return null;}
}
