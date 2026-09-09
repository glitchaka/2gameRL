package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ParticleVisualMeta;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Complete, author-facing ParticlePreset editor for 2.2.2. */
final class ParticleStudio222 {
    private static final String INSTALLED="2rl.particle.editor.2.2.2";
    private ParticleStudio222(){}

    static void install(GraphicsEditorPane pane,StudioApp app){
        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;pane.getProperties().put(INSTALLED,true);
        @SuppressWarnings("unchecked")ListView<ParticlePreset>list=field(pane,"particles",ListView.class);VBox inspector=field(pane,"particleInspector",VBox.class);if(list==null||inspector==null)return;
        final boolean[]scheduled={false};Runnable schedule=()->{if(scheduled[0])return;scheduled[0]=true;Platform.runLater(()->{scheduled[0]=false;rebuild(pane,app,list,inspector);});};
        list.getSelectionModel().selectedItemProperty().addListener((o,a,b)->schedule.run());inspector.getChildren().addListener((ListChangeListener<Node>)c->{if(!Boolean.TRUE.equals(inspector.getProperties().get(INSTALLED+".building")))schedule.run();});schedule.run();
    }

    private static void rebuild(GraphicsEditorPane pane,StudioApp app,ListView<ParticlePreset>list,VBox out){
        ParticlePreset p=list.getSelectionModel().getSelectedItem();if(p==null)return;out.getProperties().put(INSTALLED+".building",true);try{out.getChildren().clear();out.setPadding(new Insets(12));
            TextField name=new TextField(p.name);name.setPromptText("Nombre del preset");Label nameError=new Label();nameError.getStyleClass().add("muted");
            Runnable saveName=()->{String next=name.getText()==null?"":name.getText().trim();boolean duplicate=unique(app.project().getParticlePresets().values()).stream().anyMatch(x->x!=p&&x.name.equalsIgnoreCase(next));if(next.isBlank()||duplicate){nameError.setText(next.isBlank()?"El nombre no puede quedar vacío.":"Ya existe otro ParticlePreset llamado «"+next+"».");name.setText(p.name);return;}p.name=next;nameError.setText("");app.changed();list.refresh();refreshBrowser(pane);};name.setOnAction(e->saveName.run());name.focusedProperty().addListener((o,a,b)->{if(!b)saveName.run();});

            VisualState state=readVisual(app,p);ComboBox<String>mode=new ComboBox<>(FXCollections.observableArrayList("Pixel","Sprite"));mode.setValue(state.mode);ComboBox<String>shape=new ComboBox<>(FXCollections.observableArrayList("Cuadrado","Círculo pixelado"));shape.setValue(state.shape);ColorPicker color=new ColorPicker(parseColor(state.color));
            ComboBox<Asset>sprite=new ComboBox<>();sprite.getItems().addAll(app.project().getAssets().values().stream().filter(a->!ParticleVisualMeta.internal(a)&&(!a.sourceOnly||a.isRegion())).toList());sprite.setCellFactory(v->assetCell());sprite.setButtonCell(assetCell());sprite.setValue(state.source);sprite.setPromptText("Selecciona sprite/región…");
            Spinner<Double>direction=spin(0,359,state.direction,5),rate=spin(0,10000,p.rate,1),life=spin(.01,120,p.lifetime,.1),speed=spin(0,1000,p.speed,.1),spread=spin(0,360,p.spread,5),gravity=spin(-1000,1000,p.gravity,.1),startScale=spin(0,100,p.startScale,.1),endScale=spin(0,100,p.endScale,.1),startOpacity=spin(0,1,p.startOpacity,.05),endOpacity=spin(0,1,p.endOpacity,.05);Spinner<Integer>burstCount=new Spinner<>(1,100000,Math.max(1,p.burstCount),1);burstCount.setEditable(true);CheckBox burst=new CheckBox("Burst: emitir un grupo una vez"),local=new CheckBox("Espacio local: las partículas acompañan al emisor");burst.setSelected(p.burst);local.setSelected(p.localSpace);
            Label directionHelp=hint("Dirección: 0° derecha · 90° abajo · 180° izquierda · 270° arriba. Dispersión abre el cono alrededor de esa dirección.");
            ParticlePreview preview=new ParticlePreview(app,p);preview.setWidth(420);preview.setHeight(230);Button restart=new Button("Reiniciar preview");restart.setOnAction(e->preview.restart());

            Runnable controlState=()->{boolean pixel="Pixel".equals(mode.getValue());shape.setDisable(!pixel);color.setDisable(!pixel);sprite.setDisable(pixel);};controlState.run();
            Runnable visualApply=()->{controlState.run();boolean pixel="Pixel".equals(mode.getValue());if(!pixel&&sprite.getValue()==null)return;installVisualAsset(app,p,pixel,shape.getValue(),color.getValue(),sprite.getValue(),direction.getValue());preview.restart();app.changed();refreshBrowser(pane);};
            mode.setOnAction(e->visualApply.run());shape.setOnAction(e->visualApply.run());color.setOnAction(e->visualApply.run());sprite.setOnAction(e->visualApply.run());direction.valueProperty().addListener((o,a,b)->visualApply.run());
            Runnable numeric=()->{p.rate=rate.getValue();p.lifetime=life.getValue();p.speed=speed.getValue();p.spread=spread.getValue();p.gravity=gravity.getValue();p.startScale=startScale.getValue();p.endScale=endScale.getValue();p.startOpacity=startOpacity.getValue();p.endOpacity=endOpacity.getValue();p.burst=burst.isSelected();p.burstCount=burstCount.getValue();p.localSpace=local.isSelected();app.changed();preview.restart();};
            for(Spinner<Double>s:List.of(rate,life,speed,spread,gravity,startScale,endScale,startOpacity,endOpacity))s.valueProperty().addListener((o,a,b)->numeric.run());burstCount.valueProperty().addListener((o,a,b)->numeric.run());burst.setOnAction(e->numeric.run());local.setOnAction(e->numeric.run());

            GridPane visual=form();row(visual,0,"Tipo visual",mode);row(visual,1,"Sprite / región",sprite);row(visual,2,"Forma pixel",shape);row(visual,3,"Color pixel",color);row(visual,4,"Dirección",direction);GridPane emission=form();row(emission,0,"Emisión / s",rate);row(emission,1,"Vida (s)",life);row(emission,2,"Velocidad",speed);row(emission,3,"Dispersión (°)",spread);row(emission,4,"Gravedad",gravity);GridPane appearance=form();row(appearance,0,"Escala inicial",startScale);row(appearance,1,"Escala final",endScale);row(appearance,2,"Opacidad inicial",startOpacity);row(appearance,3,"Opacidad final",endOpacity);row(appearance,4,"Cantidad Burst",burstCount);
            Label title=title("PARTÍCULAS · "+p.name);out.getChildren().addAll(title,line("Nombre",name),nameError,new Separator(),title("VISUAL"),visual,directionHelp,new Separator(),title("EMISIÓN Y MOVIMIENTO"),emission,new Separator(),title("APARIENCIA"),appearance,burst,local,new Separator(),title("PREVIEW EN TIEMPO REAL"),new StackPane(preview),restart,hint("ParticleEmitter2D solo necesita seleccionar este preset y dejar playing activo. El ID técnico permanece interno."));
        }finally{out.getProperties().remove(INSTALLED+".building");}}
    }

    private record VisualState(String mode,String shape,String color,double direction,Asset source){}
    private static VisualState readVisual(StudioApp app,ParticlePreset p){Asset a=app.project().getAssets().get(p.assetKey);if(a==null)return new VisualState("Pixel","Cuadrado","#FFFFFFFF",270,null);if(ParticleVisualMeta.internal(a)){String mode=ParticleVisualMeta.mode(a).equals("PIXEL")?"Pixel":"Sprite",shape=ParticleVisualMeta.shape(a).equals("CIRCLE")?"Círculo pixelado":"Cuadrado";Asset source=app.project().getAssets().get(ParticleVisualMeta.source(a));return new VisualState(mode,shape,ParticleVisualMeta.color(a),ParticleVisualMeta.direction(a),source);}return new VisualState("Sprite","Cuadrado","#FFFFFFFF",270,a);}

    private static void installVisualAsset(StudioApp app,ParticlePreset p,boolean pixel,String shape,Color color,Asset source,double direction){try{String key="__2rl_particle_visual_"+p.key.replaceAll("[^A-Za-z0-9_-]","_");Asset alias;if(pixel){boolean circle="Círculo pixelado".equals(shape);String hex=hex(color);alias=new Asset(key,"Particle visual · "+p.name,pixelPng(color,circle));alias.category=AssetCategory.VFX;alias.filterMode=FilterMode.PIXEL;ParticleVisualMeta.mark(alias,direction,"PIXEL","",circle?"CIRCLE":"SQUARE",hex);}else{if(source==null)return;String sourceKey=source.isRegion()?source.sourceAssetKey:source.key;int x=source.isRegion()?source.regionX:0,y=source.isRegion()?source.regionY:0,w=source.isRegion()?source.regionWidth:Math.max(1,app.assetWidth(source.key)),h=source.isRegion()?source.regionHeight:Math.max(1,app.assetHeight(source.key));alias=new Asset(key,"Particle visual · "+p.name,sourceKey,x,y,w,h);alias.category=AssetCategory.VFX;alias.filterMode=source.filterMode;alias.pivotX=source.pivotX;alias.pivotY=source.pivotY;ParticleVisualMeta.mark(alias,direction,"SPRITE",source.key,"","#FFFFFFFF");}app.project().getAssets().put(key,alias);p.assetKey=key;}catch(Exception ignored){}}
    private static byte[]pixelPng(Color c,boolean circle)throws Exception{int n=circle?7:1;BufferedImage image=new BufferedImage(n,n,BufferedImage.TYPE_INT_ARGB);int argb=((int)Math.round(c.getOpacity()*255)<<24)|((int)Math.round(c.getRed()*255)<<16)|((int)Math.round(c.getGreen()*255)<<8)|(int)Math.round(c.getBlue()*255);for(int y=0;y<n;y++)for(int x=0;x<n;x++){if(!circle||Math.pow(x-(n-1)/2.0,2)+Math.pow(y-(n-1)/2.0,2)<=Math.pow(n/2.0,2))image.setRGB(x,y,argb);}ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}

    private static final class ParticlePreview extends Canvas {private record P(double born,double vx,double vy){}private final StudioApp app;private final ParticlePreset preset;private final List<P>ps=new ArrayList<>();private double start,lastSpawn;private boolean burstDone;ParticlePreview(StudioApp app,ParticlePreset preset){this.app=app;this.preset=preset;new AnimationTimer(){@Override public void handle(long now){draw(now/1e9);}}.start();}void restart(){start=0;lastSpawn=0;burstDone=false;ps.clear();}
        private void draw(double now){if(start==0){start=now;lastSpawn=now;}double elapsed=now-start;if(preset.burst&&!burstDone){for(int i=0;i<preset.burstCount&&i<300;i++)spawn(elapsed);burstDone=true;}else if(!preset.burst&&preset.rate>0){double step=1.0/preset.rate;while(elapsed-lastSpawn+start>=step&&ps.size()<400){lastSpawn+=step;spawn(lastSpawn-start);}}ps.removeIf(p->elapsed-p.born>preset.lifetime);GraphicsContext g=getGraphicsContext2D();g.setFill(Color.web("#101720"));g.fillRect(0,0,getWidth(),getHeight());Asset visual=app.project().getAssets().get(preset.assetKey);double cx=getWidth()/2,cy=getHeight()/2;for(P p:ps){double age=elapsed-p.born,t=Math.max(0,Math.min(1,age/Math.max(.01,preset.lifetime))),scale=preset.startScale+(preset.endScale-preset.startScale)*t,opacity=preset.startOpacity+(preset.endOpacity-preset.startOpacity)*t,x=cx+p.vx*age*22,y=cy+(p.vy*age+.5*preset.gravity*age*age)*22,size=Math.max(1,8*scale);if(visual!=null){double old=g.getGlobalAlpha();g.setGlobalAlpha(Math.max(0,Math.min(1,opacity)));app.drawAsset(g,preset.assetKey,x-size/2,y-size/2,size,size);g.setGlobalAlpha(old);}else{g.setGlobalAlpha(opacity);g.setFill(Color.WHITE);g.fillRect(x-size/2,y-size/2,size,size);g.setGlobalAlpha(1);}}g.setStroke(Color.rgb(255,255,255,.18));g.strokeLine(cx-8,cy,cx+8,cy);g.strokeLine(cx,cy-8,cx,cy+8);}
        private void spawn(double born){Asset visual=app.project().getAssets().get(preset.assetKey);double dir=visual==null?270:ParticleVisualMeta.direction(visual),angle=Math.toRadians(dir+(ThreadLocalRandom.current().nextDouble()-.5)*preset.spread),speed=preset.speed*(.75+ThreadLocalRandom.current().nextDouble()*.5);ps.add(new P(born,Math.cos(angle)*speed,Math.sin(angle)*speed));}}

    private static ListCell<Asset>assetCell(){return new ListCell<>(){@Override protected void updateItem(Asset a,boolean empty){super.updateItem(a,empty);setText(empty||a==null?null:(a.sourceName==null?a.key:a.sourceName));}};}
    private static Spinner<Double>spin(double min,double max,double value,double step){Spinner<Double>s=new Spinner<>(min,max,Math.max(min,Math.min(max,value)),step);s.setEditable(true);return s;}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(9);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(125);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int row,String label,Node value){g.add(new Label(label),0,row);g.add(value,1,row);if(value instanceof Region r)r.setMaxWidth(Double.MAX_VALUE);}
    private static HBox line(String label,Node value){Label l=new Label(label);l.setMinWidth(125);HBox h=new HBox(8,l,value);h.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(value,Priority.ALWAYS);return h;}
    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}private static Label hint(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private static String hex(Color c){return String.format(Locale.ROOT,"#%02X%02X%02X%02X",(int)Math.round(c.getRed()*255),(int)Math.round(c.getGreen()*255),(int)Math.round(c.getBlue()*255),(int)Math.round(c.getOpacity()*255));}
    private static Color parseColor(String raw){try{return Color.web(raw);}catch(Exception e){return Color.WHITE;}}
    private static <T>List<T>unique(Collection<T>values){Set<T>seen=Collections.newSetFromMap(new IdentityHashMap<>());ArrayList<T>out=new ArrayList<>();for(T v:values)if(v!=null&&seen.add(v))out.add(v);return out;}
    private static void refreshBrowser(GraphicsEditorPane pane){Object r=pane.getProperties().get("2rl.resourceBrowser.refresh");if(r instanceof Runnable run)run.run();}
    private static <T>T field(Object owner,String name,Class<T>type){try{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);Object v=f.get(owner);return type.isInstance(v)?type.cast(v):null;}catch(Exception e){return null;}}
}
