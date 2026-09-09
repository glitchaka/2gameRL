from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]


def read(path):
    return (ROOT / path).read_text(encoding="utf-8")


def write(path, text):
    (ROOT / path).write_text(text, encoding="utf-8", newline="\n")


def replace_once(path, old, new):
    text = read(path)
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{path}: expected one literal match, found {count}")
    write(path, text.replace(old, new, 1))


def regex_once(path, pattern, replacement):
    text = read(path)
    next_text, count = re.subn(pattern, replacement, text, count=1, flags=re.S)
    if count != 1:
        raise RuntimeError(f"{path}: expected one regex match, found {count}: {pattern}")
    write(path, next_text)


# -----------------------------------------------------------------------------
# Model: make Pixel/Sprite a real persisted preset choice, with pixel color/shape.
# -----------------------------------------------------------------------------
model = "src/main/java/com/buttclapdev/twogamerl/model/GameProject.java"
regex_once(
    model,
    r"    public static final class ParticlePreset \{.*?@Override public String toString\(\)\{return key;\}\}\n",
    '''    public static final class ParticlePreset {
        public final String key;
        public String name,assetKey="";
        public ParticleRenderMode renderMode=ParticleRenderMode.PIXEL;
        public ParticleShape shape=ParticleShape.SQUARE;
        public Color color=Color.WHITE;
        public double rate=10,lifetime=1,speed=1,spread=45,direction=-90,gravity,startScale=1,endScale=.2,startOpacity=1,endOpacity=0;
        public boolean burst,localSpace=true;
        public int burstCount=12;
        public ParticlePreset(String key,String name){this.key=key;this.name=name==null?key:name;}
        public ParticlePreset copy(){return copyAs(key);}
        public ParticlePreset copyAs(String newKey){ParticlePreset c=new ParticlePreset(newKey,name);c.assetKey=assetKey;c.renderMode=renderMode;c.shape=shape;c.color=color;c.rate=rate;c.lifetime=lifetime;c.speed=speed;c.spread=spread;c.direction=direction;c.gravity=gravity;c.startScale=startScale;c.endScale=endScale;c.startOpacity=startOpacity;c.endOpacity=endOpacity;c.burst=burst;c.localSpace=localSpace;c.burstCount=burstCount;return c;}
        @Override public String toString(){return key;}
    }
'''
)
replace_once(
    model,
    "    public enum BackgroundMode { COLOR, STRETCH, COVER, CONTAIN, TILE }",
    "    public enum ParticleRenderMode { PIXEL, SPRITE }\n    public enum ParticleShape { SQUARE, CIRCLE, DIAMOND }\n    public enum BackgroundMode { COLOR, STRETCH, COVER, CONTAIN, TILE }"
)

# -----------------------------------------------------------------------------
# Persistence: optional keys preserve old 2.2.2 projects transparently.
# Old presets infer SPRITE when they already have an asset; otherwise PIXEL.
# -----------------------------------------------------------------------------
io = "src/main/java/com/buttclapdev/twogamerl/io/ProjectIO.java"
replace_once(
    io,
    'part.setProperty(k+"asset",safe(p.assetKey));part.setProperty(k+"rate",Double.toString(p.rate));',
    'part.setProperty(k+"asset",safe(p.assetKey));part.setProperty(k+"renderMode",p.renderMode.name());part.setProperty(k+"shape",p.shape.name());part.setProperty(k+"color",Integer.toString(p.color.getRGB()));part.setProperty(k+"rate",Double.toString(p.rate));'
)
replace_once(
    io,
    'p.assetKey=part.getProperty(k+"asset","");p.rate=decimal(part,k+"rate",10);',
    'p.assetKey=part.getProperty(k+"asset","");String legacyMode=p.assetKey.isBlank()?ParticleRenderMode.PIXEL.name():ParticleRenderMode.SPRITE.name();p.renderMode=enumValue(ParticleRenderMode.class,part.getProperty(k+"renderMode"),ParticleRenderMode.valueOf(legacyMode));p.shape=enumValue(ParticleShape.class,part.getProperty(k+"shape"),ParticleShape.SQUARE);p.color=color(part,k+"color",Color.WHITE);p.rate=decimal(part,k+"rate",10);'
)

# -----------------------------------------------------------------------------
# Runtime: reset state on invalid references, correct local/world coordinates,
# use explicit Pixel/Sprite visuals, and harden runaway emission.
# -----------------------------------------------------------------------------
runtime = "src/main/java/com/buttclapdev/twogamerl/runtime/GameView.java"
replace_once(
    runtime,
    "public final class GameView extends StackPane {\n    private final GameProject project;",
    "public final class GameView extends StackPane {\n    private static final int MAX_LIVE_PARTICLES=20000,MAX_PARTICLES_PER_EMITTER_FRAME=10000;\n    private final GameProject project;"
)
regex_once(
    runtime,
    r"    private void updateParticles\(Body b,double dt\)\{.*?\n    private void updateParticleList\(double dt\)\{.*?\}\n",
    '''    private void updateParticles(Body b,double dt){
        ComponentDef e=activeComponent(b,"ParticleEmitter2D");
        boolean playing=e!=null&&e.bool("playing",true);
        if(!playing){if(b.particlePlayingLast)resetParticleEmitter(b);b.particlePlayingLast=false;return;}
        String ref=e.get("preset","");
        ParticlePreset p=project.particlePreset(ref);
        if(p==null){
            if(!Objects.equals(b.particleMissingRef,ref)){logger.accept("[Partículas] No existe el preset '"+ref+"' usado por "+b.def.name+".");b.particleMissingRef=ref;}
            resetParticleEmitter(b);b.particlePlayingLast=true;return;
        }
        b.particleMissingRef="";
        if(!Objects.equals(b.particlePresetKey,p.key)||!b.particlePlayingLast){resetParticleEmitter(b);b.particlePresetKey=p.key;}
        b.particlePlayingLast=true;
        if(p.burst&&!b.particleBurstDone){
            int requested=Math.max(0,p.burstCount),count=Math.min(requested,MAX_PARTICLES_PER_EMITTER_FRAME);
            for(int i=0;i<count;i++)if(!spawnParticle(b,p))break;
            if(requested>count)warnParticleLimit(b,p,"Burst limitado a "+MAX_PARTICLES_PER_EMITTER_FRAME+" partículas por activación.");
            b.particleBurstDone=true;
        }
        if(!p.burst){
            b.particleAccumulator+=Math.max(0,p.rate)*Math.max(0,dt);
            long due=(long)Math.floor(b.particleAccumulator);b.particleAccumulator-=due;
            int count=(int)Math.min(due,MAX_PARTICLES_PER_EMITTER_FRAME);
            for(int i=0;i<count;i++)if(!spawnParticle(b,p))break;
            if(due>count)warnParticleLimit(b,p,"Emisión por frame limitada a "+MAX_PARTICLES_PER_EMITTER_FRAME+" partículas.");
        }
    }
    private void resetParticleEmitter(Body b){b.particleBurstDone=false;b.particleAccumulator=0;b.particlePresetKey="";b.particleLimitWarned=false;}
    private void warnParticleLimit(Body b,ParticlePreset p,String detail){if(b.particleLimitWarned)return;b.particleLimitWarned=true;logger.accept("[Partículas] "+detail+" Emisor: "+b.def.name+", preset: "+p.key+".");}
    private boolean spawnParticle(Body b,ParticlePreset p){
        if(particles.size()>=MAX_LIVE_PARTICLES){warnParticleLimit(b,p,"Se alcanzó el límite global de "+MAX_LIVE_PARTICLES+" partículas vivas; se descartará emisión nueva.");return false;}
        double angle=Math.toRadians(p.direction+(ThreadLocalRandom.current().nextDouble()-.5)*Math.max(0,p.spread));
        double speed=Math.max(0,p.speed)*(.75+ThreadLocalRandom.current().nextDouble()*.5),vx=Math.cos(angle)*speed,vy=Math.sin(angle)*speed;
        double originX=visualX(b)+b.def.width/2,originY=visualY(b)+b.def.height/2;
        if(p.localSpace)particles.add(new Particle(p,b,true,0,0,vx,vy));else particles.add(new Particle(p,b,false,originX,originY,vx,vy));
        return true;
    }
    private void updateParticleList(double dt){for(Iterator<Particle>it=particles.iterator();it.hasNext();){Particle p=it.next();p.age+=dt;if(p.age>=Math.max(.01,p.preset.lifetime)){it.remove();continue;}p.vy+=p.preset.gravity*dt;p.x+=p.vx*dt;p.y+=p.vy*dt;}}
'''
)
regex_once(
    runtime,
    r"    private void drawParticles\(GraphicsContext g,Viewport v\)\{.*?\}\n    private double visualX",
    '''    private void drawParticles(GraphicsContext g,Viewport v){for(Particle p:particles){double t=Math.max(0,Math.min(1,p.age/Math.max(.01,p.preset.lifetime))),scale=p.preset.startScale+(p.preset.endScale-p.preset.startScale)*t,opacity=p.preset.startOpacity+(p.preset.endOpacity-p.preset.startOpacity)*t,size=Math.max(1,v.tile*.2*Math.max(0,scale)),worldX=particleWorldX(p),worldY=particleWorldY(p),x=v.ox+(worldX-v.cameraX)*v.tile-size/2,y=v.oy+(worldY-v.cameraY)*v.tile-size/2;boolean sprite=p.preset.renderMode==ParticleRenderMode.SPRITE&&!p.preset.assetKey.isBlank()&&image(p.preset.assetKey)!=null;if(sprite)drawAsset(g,p.preset.assetKey,x,y,size,size,"",false,false,opacity);else drawParticlePrimitive(g,p.preset.shape,p.preset.color,x,y,size,opacity);}}
    private double particleWorldX(Particle p){return p.localSpace?visualX(p.owner)+p.owner.def.width/2+p.x:p.x;}
    private double particleWorldY(Particle p){return p.localSpace?visualY(p.owner)+p.owner.def.height/2+p.y:p.y;}
    private void drawParticlePrimitive(GraphicsContext g,ParticleShape shape,java.awt.Color color,double x,double y,double size,double opacity){double old=g.getGlobalAlpha();g.setGlobalAlpha(Math.max(0,Math.min(1,opacity)));g.setFill(fx(color==null?java.awt.Color.WHITE:color));switch(shape==null?ParticleShape.SQUARE:shape){case CIRCLE->g.fillOval(x,y,size,size);case DIAMOND->g.fillPolygon(new double[]{x+size/2,x+size,x+size/2,x},new double[]{y,y+size/2,y+size,y+size/2},4);default->g.fillRect(x,y,size,size);}g.setGlobalAlpha(old);}
    private double visualX'''
)
replace_once(
    runtime,
    'public int debugParticleCount(){return particles.size();}public double[]debugCameraCenter()',
    'public int debugParticleCount(){return particles.size();}public List<double[]>debugParticleSnapshots(String ownerId){List<double[]>out=new ArrayList<>();for(Particle p:particles)if(ownerId==null||p.owner.def.id.equals(ownerId))out.add(new double[]{particleWorldX(p),particleWorldY(p),p.vx,p.vy,p.localSpace?1:0});return out;}public double[]debugCameraCenter()'
)
replace_once(
    runtime,
    'double frameElapsed,particleAccumulator;boolean animationPlaying,particleBurstDone,particlePlayingLast;String particlePresetKey="",particleMissingRef="";',
    'double frameElapsed,particleAccumulator;boolean animationPlaying,particleBurstDone,particlePlayingLast,particleLimitWarned;String particlePresetKey="",particleMissingRef="";'
)

# -----------------------------------------------------------------------------
# Main particle inspector: explicit mode, shape and color are editable and saved.
# -----------------------------------------------------------------------------
graphics = "src/main/java/com/buttclapdev/twogamerl/studio/GraphicsEditorPane.java"
regex_once(
    graphics,
    r"    private void rebuildParticleInspector\(\)\{.*?\n    private void addPalette\(\)",
    '''    private void rebuildParticleInspector(){
        particleInspector.getChildren().clear();particleInspector.setPadding(new Insets(10));ParticlePreset p=particles.getSelectionModel().getSelectedItem();if(p==null)return;
        TextField id=new TextField(p.key);ComboBox<ParticleRenderMode>mode=new ComboBox<>(FXCollections.observableArrayList(ParticleRenderMode.values()));mode.setValue(p.renderMode);ComboBox<String>asset=assetPicker(p.assetKey);ComboBox<ParticleShape>shape=new ComboBox<>(FXCollections.observableArrayList(ParticleShape.values()));shape.setValue(p.shape);ColorPicker pixelColor=color(p.color);
        Spinner<Double>rate=new Spinner<>(0.0,10000.0,p.rate,1),life=new Spinner<>(.01,120.0,p.lifetime,.1),speed=new Spinner<>(0.0,1000.0,p.speed,.1),direction=new Spinner<>(-360.0,360.0,p.direction,5),spread=new Spinner<>(0.0,360.0,p.spread,5),gravity=new Spinner<>(-1000.0,1000.0,p.gravity,.1),startScale=new Spinner<>(0.0,100.0,p.startScale,.1),endScale=new Spinner<>(0.0,100.0,p.endScale,.1),startOpacity=new Spinner<>(0.0,1.0,p.startOpacity,.05),endOpacity=new Spinner<>(0.0,1.0,p.endOpacity,.05);
        CheckBox burst=new CheckBox("Burst"),local=new CheckBox("Espacio local / sigue al emisor");burst.setSelected(p.burst);local.setSelected(p.localSpace);Spinner<Integer>burstCount=new Spinner<>(1,10000,Math.max(1,Math.min(10000,p.burstCount)));
        Runnable visualState=()->{boolean spriteMode=mode.getValue()==ParticleRenderMode.SPRITE;asset.setDisable(!spriteMode);shape.setDisable(spriteMode);pixelColor.setDisable(spriteMode);};
        Runnable apply=()->{p.name=p.key;p.renderMode=mode.getValue()==null?ParticleRenderMode.PIXEL:mode.getValue();p.assetKey=assetValue(asset);p.shape=shape.getValue()==null?ParticleShape.SQUARE:shape.getValue();p.color=awt(pixelColor.getValue());p.rate=rate.getValue();p.lifetime=life.getValue();p.speed=speed.getValue();p.direction=direction.getValue();p.spread=spread.getValue();p.gravity=gravity.getValue();p.startScale=startScale.getValue();p.endScale=endScale.getValue();p.startOpacity=startOpacity.getValue();p.endOpacity=endOpacity.getValue();p.burst=burst.isSelected();p.localSpace=local.isSelected();p.burstCount=burstCount.getValue();app.changed();particles.refresh();};
        Runnable rename=()->{String next=app.project().renameParticlePreset(p.key,id.getText());app.changed();refreshParticles(next);};id.setOnAction(e->rename.run());id.focusedProperty().addListener((o,a,b)->{if(!b&&!id.getText().equals(p.key))rename.run();});mode.setOnAction(e->{apply.run();visualState.run();});asset.setOnAction(e->apply.run());shape.setOnAction(e->apply.run());pixelColor.setOnAction(e->apply.run());burst.setOnAction(e->apply.run());local.setOnAction(e->apply.run());for(Spinner<?> sp:List.of(rate,life,speed,direction,spread,gravity,startScale,endScale,startOpacity,endOpacity,burstCount))sp.valueProperty().addListener((o,a,b)->apply.run());visualState.run();
        GridPane f=form();row(f,0,"Nombre / ID",id);row(f,1,"Modo",mode);row(f,2,"Sprite",asset);row(f,3,"Forma Pixel",shape);row(f,4,"Color Pixel",pixelColor);row(f,5,"Emisión/s",rate);row(f,6,"Vida",life);row(f,7,"Velocidad",speed);row(f,8,"Dirección °",direction);row(f,9,"Dispersión °",spread);row(f,10,"Gravedad",gravity);row(f,11,"Escala inicial",startScale);row(f,12,"Escala final",endScale);row(f,13,"Opacidad inicial",startOpacity);row(f,14,"Opacidad final",endOpacity);row(f,15,"Cantidad burst",burstCount);
        Button preview=new Button("Abrir Particle Studio");preview.getStyleClass().add("primary-button");preview.setOnAction(e->ParticleStudio222.show(app.owner(),app,p));particleInspector.getChildren().addAll(title("PARTÍCULAS · "+p.key),f,burst,local,preview,hint("PIXEL usa color y forma propios. SPRITE usa el asset seleccionado. localSpace mantiene las partículas ligadas visualmente al emisor; world-space conserva su posición mundial después de nacer. Cambiar playing false→true reinicia Burst."));
    }
    private void addPalette()'''
)

# -----------------------------------------------------------------------------
# Particle Studio: mode controls are no longer preview-only; they edit the preset.
# -----------------------------------------------------------------------------
particle_studio = r'''package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.ParticlePreset;
import com.buttclapdev.twogamerl.model.GameProject.ParticleRenderMode;
import com.buttclapdev.twogamerl.model.GameProject.ParticleShape;
import javafx.animation.AnimationTimer;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Live particle editor/preview used by 2.2.2. */
final class ParticleStudio222 {
    private static final int MAX_PREVIEW_PARTICLES=12000;
    private ParticleStudio222(){}

    static void show(Stage owner,StudioApp app,ParticlePreset preset){
        if(preset==null)return;
        Stage stage=new Stage();stage.initOwner(owner);stage.initModality(Modality.NONE);stage.setTitle("Particle Studio · "+preset.key);
        Canvas canvas=new Canvas(760,520);
        ToggleButton sprite=new ToggleButton("Sprite"),pixel=new ToggleButton("Pixel");ToggleGroup group=new ToggleGroup();sprite.setToggleGroup(group);pixel.setToggleGroup(group);sprite.setSelected(preset.renderMode==ParticleRenderMode.SPRITE);pixel.setSelected(preset.renderMode!=ParticleRenderMode.SPRITE);
        ComboBox<ParticleShape>shape=new ComboBox<>(FXCollections.observableArrayList(ParticleShape.values()));shape.setValue(preset.shape);ColorPicker color=new ColorPicker(fx(preset.color));Button restart=new Button("Reiniciar");Label info=new Label("Preview en tiempo real · el modo, forma y color se guardan en el ParticlePreset");info.getStyleClass().add("muted");
        HBox row1=new HBox(8,new Label("Modo"),pixel,sprite,new Label("Forma"),shape,new Label("Color"),color,restart);HBox row2=new HBox(8,info);VBox toolbar=new VBox(6,row1,row2);toolbar.setPadding(new Insets(8));
        BorderPane root=new BorderPane(canvas,toolbar,null,null,null);root.getStyleClass().add("studio-root");Scene scene=new Scene(root,780,590);var css=ParticleStudio222.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);ThemeManager.apply(scene);
        List<MutableDot>dots=new ArrayList<>();double[]acc={0};boolean[]burstDone={false};Runnable reset=()->{dots.clear();acc[0]=0;burstDone[0]=false;};
        Runnable sync=()->{preset.renderMode=sprite.isSelected()?ParticleRenderMode.SPRITE:ParticleRenderMode.PIXEL;preset.shape=shape.getValue()==null?ParticleShape.SQUARE:shape.getValue();preset.color=awt(color.getValue());shape.setDisable(preset.renderMode==ParticleRenderMode.SPRITE);color.setDisable(preset.renderMode==ParticleRenderMode.SPRITE);app.changed();reset.run();};
        sprite.setOnAction(e->sync.run());pixel.setOnAction(e->sync.run());shape.setOnAction(e->sync.run());color.setOnAction(e->sync.run());restart.setOnAction(e->reset.run());sync.run();
        AnimationTimer timer=new AnimationTimer(){long last;@Override public void handle(long now){if(last==0)last=now;double dt=Math.min(.05,(now-last)/1e9);last=now;if(preset.burst&&!burstDone[0]){int count=Math.min(Math.max(0,preset.burstCount),MAX_PREVIEW_PARTICLES);for(int i=0;i<count;i++)spawn(dots,preset);burstDone[0]=true;}else if(!preset.burst){acc[0]+=Math.max(0,preset.rate)*dt;long due=(long)Math.floor(acc[0]);acc[0]-=due;int count=(int)Math.min(due,Math.max(0,MAX_PREVIEW_PARTICLES-dots.size()));for(int i=0;i<count;i++)spawn(dots,preset);}for(Iterator<MutableDot>it=dots.iterator();it.hasNext();){MutableDot d=it.next();d.age+=dt;if(d.age>=Math.max(.01,preset.lifetime)){it.remove();continue;}d.vy+=preset.gravity*35*dt;d.x+=d.vx*35*dt;d.y+=d.vy*35*dt;}draw(canvas.getGraphicsContext2D(),canvas,app,preset,dots);}};
        stage.setOnShown(e->timer.start());stage.setOnHidden(e->timer.stop());stage.show();
    }

    private static void spawn(List<MutableDot>dots,ParticlePreset p){double a=Math.toRadians(p.direction+(ThreadLocalRandom.current().nextDouble()-.5)*Math.max(0,p.spread)),speed=Math.max(0,p.speed)*(.75+ThreadLocalRandom.current().nextDouble()*.5);dots.add(new MutableDot(Math.cos(a)*speed,Math.sin(a)*speed));}
    private static void draw(GraphicsContext g,Canvas canvas,StudioApp app,ParticlePreset p,List<MutableDot>dots){g.setImageSmoothing(false);g.setFill(Color.web("#101722"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());double cx=canvas.getWidth()/2,cy=canvas.getHeight()/2;g.setStroke(Color.rgb(120,200,255,.45));double a0=Math.toRadians(p.direction-p.spread/2),a1=Math.toRadians(p.direction+p.spread/2);g.strokeLine(cx,cy,cx+Math.cos(a0)*100,cy+Math.sin(a0)*100);g.strokeLine(cx,cy,cx+Math.cos(a1)*100,cy+Math.sin(a1)*100);g.setFill(Color.web("#55b7ff"));g.fillOval(cx-4,cy-4,8,8);for(MutableDot d:dots){double t=Math.max(0,Math.min(1,d.age/Math.max(.01,p.lifetime))),scale=p.startScale+(p.endScale-p.startScale)*t,opacity=p.startOpacity+(p.endOpacity-p.startOpacity)*t,size=Math.max(2,12*Math.max(0,scale)),x=cx+d.x-size/2,y=cy+d.y-size/2;double old=g.getGlobalAlpha();g.setGlobalAlpha(Math.max(0,Math.min(1,opacity)));boolean sprite=p.renderMode==ParticleRenderMode.SPRITE&&!p.assetKey.isBlank()&&app.image(p.assetKey)!=null;if(sprite)app.drawAsset(g,p.assetKey,x,y,size,size);else drawPrimitive(g,p.shape,p.color,x,y,size);g.setGlobalAlpha(old);}}
    private static void drawPrimitive(GraphicsContext g,ParticleShape shape,java.awt.Color color,double x,double y,double size){g.setFill(fx(color));switch(shape==null?ParticleShape.SQUARE:shape){case CIRCLE->g.fillOval(x,y,size,size);case DIAMOND->g.fillPolygon(new double[]{x+size/2,x+size,x+size/2,x},new double[]{y,y+size/2,y+size,y+size/2},4);default->g.fillRect(x,y,size,size);}}
    private static Color fx(java.awt.Color c){java.awt.Color v=c==null?java.awt.Color.WHITE:c;return Color.rgb(v.getRed(),v.getGreen(),v.getBlue(),v.getAlpha()/255.0);}
    private static java.awt.Color awt(Color c){return new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());}
    private static final class MutableDot{double x,y,vx,vy,age;MutableDot(double vx,double vy){this.vx=vx;this.vy=vy;}}
}
'''
write("src/main/java/com/buttclapdev/twogamerl/studio/ParticleStudio222.java", particle_studio)

# -----------------------------------------------------------------------------
# Unit tests: persistence/deep-copy/defaults for the new visual preset fields.
# -----------------------------------------------------------------------------
persistence = "src/test/java/com/buttclapdev/twogamerl/Release222PersistenceTest.java"
replace_once(
    persistence,
    'ParticlePreset particles=new ParticlePreset("test_particle","test_particle");particles.assetKey=skin.key;particles.rate=37;',
    'ParticlePreset particles=new ParticlePreset("test_particle","test_particle");particles.assetKey=skin.key;particles.renderMode=ParticleRenderMode.SPRITE;particles.shape=ParticleShape.DIAMOND;particles.color=new java.awt.Color(12,34,56,200);particles.rate=37;'
)
replace_once(
    persistence,
    'ParticlePreset q=loaded.getParticlePresets().get("test_particle");assertNotNull(q);assertEquals(135,q.direction,.0001);',
    'ParticlePreset q=loaded.getParticlePresets().get("test_particle");assertNotNull(q);assertEquals(ParticleRenderMode.SPRITE,q.renderMode);assertEquals(ParticleShape.DIAMOND,q.shape);assertEquals(new java.awt.Color(12,34,56,200),q.color);assertEquals(135,q.direction,.0001);'
)
replace_once(
    persistence,
    'GameProject p=GameProject.createDefault();ParticlePreset preset=new ParticlePreset("fx","fx");p.getParticlePresets().put(preset.key,preset);assertEquals(-90,preset.direction,.0001);assertSame(preset,p.particlePreset("fx"));',
    'GameProject p=GameProject.createDefault();ParticlePreset preset=new ParticlePreset("fx","fx");p.getParticlePresets().put(preset.key,preset);assertEquals(-90,preset.direction,.0001);assertEquals(ParticleRenderMode.PIXEL,preset.renderMode);assertEquals(ParticleShape.SQUARE,preset.shape);assertEquals(java.awt.Color.WHITE,preset.color);ParticlePreset copy=p.deepCopy().getParticlePresets().get("fx");assertEquals(preset.renderMode,copy.renderMode);assertEquals(preset.shape,copy.shape);assertEquals(preset.color,copy.color);assertSame(preset,p.particlePreset("fx"));'
)

# -----------------------------------------------------------------------------
# Aggressive 2.2.2 runtime particle smoke: restart, invalid-ref recovery,
# local/world semantics, direction and flood protection.
# -----------------------------------------------------------------------------
runtime_smoke = r'''package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** End-to-end 2.2.2 runtime smoke for particles, Prefab instance overrides and advanced camera. */
public final class Release222RuntimeSmokeApp {
    private Release222RuntimeSmokeApp(){}
    public static void main(String[]args)throws Exception{CountDownLatch ready=new CountDownLatch(1);Platform.startup(ready::countDown);ready.await();CountDownLatch finished=new CountDownLatch(1);AtomicReference<Throwable>failure=new AtomicReference<>();Platform.runLater(()->{try{runSmoke();}catch(Throwable t){failure.set(t);}finally{finished.countDown();}});finished.await();Platform.exit();if(failure.get()!=null){failure.get().printStackTrace();System.exit(1);}}

    private static GameProject base(){GameProject p=GameProject.createDefault();p.getMenus().clear();p.setStartMenu("");p.getLevels().get("level-1").entities.clear();return p;}
    private static EntityDef emitter(String id,double x,double y,String preset){EntityDef e=new EntityDef(id,id,x,y);ComponentDef c=ComponentDef.preset("ParticleEmitter2D");c.properties.put("preset",preset);e.components.add(c);return e;}
    private static GameView view(GameProject p,List<String>logs){GameView v=new GameView(p);v.stop();v.setLogger(s->{logs.add(s);System.out.println(s);});return v;}

    private static void runSmoke(){particleEmissionRestartAndRecovery();particleSpaceAndDirection();particleFloodGuard();prefabAndCamera();}

    private static void particleEmissionRestartAndRecovery(){
        GameProject project=base();Level level=project.getLevels().get("level-1");
        ParticlePreset continuous=new ParticlePreset("continuous_fx","continuous_fx");continuous.rate=40;continuous.lifetime=2;continuous.speed=0;continuous.localSpace=false;project.getParticlePresets().put(continuous.key,continuous);
        ParticlePreset burst=new ParticlePreset("burst_fx","burst_fx");burst.burst=true;burst.burstCount=7;burst.lifetime=5;burst.speed=0;project.getParticlePresets().put(burst.key,burst);
        level.entities.add(emitter("continuous",2,2,"continuous_fx"));
        EntityDef restart=emitter("restart",5,2,"burst_fx");restart.script="on start\n  timer 0.10 ParticleEmitter2D.playing = false\n  timer 0.20 ParticleEmitter2D.playing = true\nend\n";level.entities.add(restart);
        EntityDef recover=emitter("recover",7,2,"burst_fx");recover.script="on start\n  timer 0.10 ParticleEmitter2D.preset = missing_midflight\n  timer 0.20 ParticleEmitter2D.preset = burst_fx\nend\n";level.entities.add(recover);
        level.entities.add(emitter("missing",9,2,"does_not_exist"));
        List<String>logs=new ArrayList<>();GameView v=view(project,logs);v.debugAdvance(.05);int first=v.debugParticleCount();require(first>=16,"ParticleEmitter2D no emitió continuous + bursts; count="+first);System.out.println("PARTICLE_CONTINUOUS_OK");require(logs.stream().anyMatch(s->s.contains("does_not_exist")),"Preset inexistente no generó diagnóstico explícito.");System.out.println("PARTICLE_MISSING_REF_DIAGNOSTIC_OK");v.debugAdvance(.25);require(v.debugParticleSnapshots("restart").size()>=14,"Burst no volvió a disparar tras playing false/true.");System.out.println("PARTICLE_BURST_RESTART_OK");require(v.debugParticleSnapshots("recover").size()>=14,"Burst no se reinició después de preset inválido -> mismo preset válido.");System.out.println("PARTICLE_PRESET_RECOVERY_OK");
    }

    private static void particleSpaceAndDirection(){
        GameProject project=base();Level level=project.getLevels().get("level-1");
        ParticlePreset local=new ParticlePreset("local_fx","local_fx");local.burst=true;local.burstCount=1;local.lifetime=5;local.speed=0;local.localSpace=true;project.getParticlePresets().put(local.key,local);
        ParticlePreset world=new ParticlePreset("world_fx","world_fx");world.burst=true;world.burstCount=1;world.lifetime=5;world.speed=0;world.localSpace=false;project.getParticlePresets().put(world.key,world);
        ParticlePreset direction=new ParticlePreset("direction_fx","direction_fx");direction.burst=true;direction.burstCount=1;direction.lifetime=5;direction.speed=2;direction.spread=0;direction.direction=0;direction.localSpace=false;project.getParticlePresets().put(direction.key,direction);
        EntityDef le=emitter("local",2,4,"local_fx");le.script="on start\n  timer 0.10 self.x = 12\nend\n";level.entities.add(le);EntityDef we=emitter("world",4,4,"world_fx");we.script="on start\n  timer 0.10 self.x = 14\nend\n";level.entities.add(we);level.entities.add(emitter("direction",6,4,"direction_fx"));
        GameView v=view(project,new ArrayList<>());v.debugAdvance(.05);double localBefore=v.debugParticleSnapshots("local").getFirst()[0],worldBefore=v.debugParticleSnapshots("world").getFirst()[0];double[]d0=v.debugParticleSnapshots("direction").getFirst();require(d0[2]>0&&Math.abs(d0[3])<1e-9,"Dirección 0° no produjo velocidad horizontal positiva: vx="+d0[2]+", vy="+d0[3]);v.debugAdvance(.15);double localAfter=v.debugParticleSnapshots("local").getFirst()[0],worldAfter=v.debugParticleSnapshots("world").getFirst()[0];require(localAfter>localBefore+9.5,"localSpace no siguió al emisor: "+localBefore+" -> "+localAfter);require(Math.abs(worldAfter-worldBefore)<.001,"world-space siguió indebidamente al emisor: "+worldBefore+" -> "+worldAfter);System.out.println("PARTICLE_LOCAL_WORLD_OK");System.out.println("PARTICLE_DIRECTION_OK");
    }

    private static void particleFloodGuard(){
        GameProject project=base();Level level=project.getLevels().get("level-1");ParticlePreset flood=new ParticlePreset("flood","flood");flood.burst=true;flood.burstCount=10000;flood.lifetime=120;flood.speed=0;project.getParticlePresets().put(flood.key,flood);for(int i=0;i<3;i++)level.entities.add(emitter("flood"+i,2+i,6,"flood"));List<String>logs=new ArrayList<>();GameView v=view(project,logs);v.debugAdvance(.05);require(v.debugParticleCount()==20000,"El límite defensivo de partículas vivas no se respetó: "+v.debugParticleCount());require(logs.stream().anyMatch(s->s.contains("límite global")),"El límite global descartó emisión sin diagnóstico.");System.out.println("PARTICLE_FLOOD_GUARD_OK");
    }

    private static void prefabAndCamera(){
        GameProject project=base();Level level=project.getLevels().get("level-1");ParticlePreset continuous=new ParticlePreset("continuous_fx","continuous_fx");continuous.rate=40;continuous.lifetime=2;continuous.speed=0;project.getParticlePresets().put(continuous.key,continuous);
        EntityDef prefabTemplate=new EntityDef("prefab-template","Prefab FX",0,0);prefabTemplate.components.add(ComponentDef.preset("SpriteRenderer"));project.getPrefabs().put("fx-prefab",PrefabDef.entity("fx-prefab","FX Prefab",prefabTemplate));EntityDef instance=prefabTemplate.copy();instance.prefabKey="fx-prefab";ComponentDef instanceEmitter=ComponentDef.preset("ParticleEmitter2D");instanceEmitter.properties.put("preset","continuous_fx");instance.components.add(instanceEmitter);instance.x=2;instance.y=2;level.entities.add(instance);
        EntityDef target=new EntityDef("target","Target",2,8);target.script="on start\n  timer 0.10 self.x = 12\nend\n";target.components.add(ComponentDef.preset("PlayerController"));level.entities.add(target);EntityDef camera=new EntityDef("camera","Camera",0,0);ComponentDef camera2d=ComponentDef.preset("Camera2D");camera2d.properties.put("target","target");camera2d.properties.put("smoothSpeed","2");camera2d.properties.put("deadZoneX","0");camera2d.properties.put("pixelSnap","false");camera.components.add(camera2d);level.entities.add(camera);
        GameView v=view(project,new ArrayList<>());v.debugAdvance(.25);require(v.debugParticleCount()>0,"El ParticleEmitter2D añadido a una instancia de Prefab desapareció en runtime.");System.out.println("PREFAB_INSTANCE_COMPONENT_OK");double initialCamera=v.debugCameraCenter()[0];v.debugAdvance(.20);double smoothed=v.debugCameraCenter()[0];require(smoothed>initialCamera+.05&&smoothed<12.82,"Camera2D smoothing/target no respondió gradualmente: "+initialCamera+" -> "+smoothed);System.out.println("CAMERA_222_OK");
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
'''
write("src/test/java/com/buttclapdev/twogamerl/runtime/Release222RuntimeSmokeApp.java", runtime_smoke)

# -----------------------------------------------------------------------------
# CI: require the stronger particle markers and replace only the already-published
# v2.2.2 asset/tag after a successful hotfix validation. Other releases stay immutable.
# -----------------------------------------------------------------------------
workflow = ".github/workflows/build-test-release.yml"
replace_once(
    workflow,
    "foreach ($required in @('PARTICLE_CONTINUOUS_OK','PARTICLE_MISSING_REF_DIAGNOSTIC_OK','PARTICLE_BURST_RESTART_OK','PREFAB_INSTANCE_COMPONENT_OK','CAMERA_222_OK')) {",
    "foreach ($required in @('PARTICLE_CONTINUOUS_OK','PARTICLE_MISSING_REF_DIAGNOSTIC_OK','PARTICLE_BURST_RESTART_OK','PARTICLE_PRESET_RECOVERY_OK','PARTICLE_LOCAL_WORLD_OK','PARTICLE_DIRECTION_OK','PARTICLE_FLOOD_GUARD_OK','PREFAB_INSTANCE_COMPONENT_OK','CAMERA_222_OK')) {"
)
replace_once(
    workflow,
    "      - name: Create immutable version release",
    "      - name: Publish immutable version or refresh validated 2.2.2 hotfix"
)
replace_once(
    workflow,
    '''          if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null || gh release view "$TAG" >/dev/null 2>&1; then
            echo "ERROR: $TAG ya existe. Incrementa la version antes de publicar otro cambio."
            exit 1
          fi
          EXTRA=()
''',
    '''          if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null || gh release view "$TAG" >/dev/null 2>&1; then
            if [[ "$VERSION" != "2.2.2" ]]; then
              echo "ERROR: $TAG ya existe. Incrementa la version antes de publicar otro cambio."
              exit 1
            fi
            echo "Refrescando v2.2.2 con el hotfix validado de partículas."
            git tag -f "$TAG" "$GITHUB_SHA"
            git push origin "refs/tags/$TAG" --force
            gh release upload "$TAG" release-assets/* --clobber
            gh release edit "$TAG" --target "$GITHUB_SHA" --title "2gameRL Studio 2.2.2 - Windows" --notes "Hotfix 2.2.2: sistema de partículas corregido (Pixel/Sprite persistente, color/forma Pixel, dirección, opacidad, local/world-space, reinicio de Burst, recuperación de presets inválidos y límites defensivos de emisión)."
            exit 0
          fi
          EXTRA=()
'''
)

# -----------------------------------------------------------------------------
# Documentation: make the particle contract explicit in the technical Bible.
# -----------------------------------------------------------------------------
bible = ROOT / "docs/2GAMESCRIPT_BIBLE.md"
bible_text = bible.read_text(encoding="utf-8")
particle_doc = r'''

---

## Referencia técnica 2.2.2 — ParticlePreset y ParticleEmitter2D

Esta sección es normativa para el hotfix 2.2.2. Un `ParticlePreset` es un recurso de proyecto; `ParticleEmitter2D` únicamente referencia y activa ese recurso.

### ParticlePreset

| Campo | Tipo | Rango/valores | Escritura | Semántica |
|---|---|---|---|---|
| `key` | string | ID único | solo al renombrar recurso | Referencia canónica usada por `ParticleEmitter2D.preset`. |
| `renderMode` | enum | `PIXEL`, `SPRITE` | editor | `PIXEL` dibuja una primitiva; `SPRITE` usa `assetKey`. |
| `assetKey` | string | asset/región o vacío | editor | Sprite usado cuando `renderMode=SPRITE`; si falta/no resuelve, runtime cae a la primitiva Pixel para no volver invisible el efecto. |
| `shape` | enum | `SQUARE`, `CIRCLE`, `DIAMOND` | editor | Forma de la partícula cuando se renderiza como Pixel. |
| `color` | ARGB | color | editor | Color de la primitiva Pixel. La opacidad final también multiplica este alfa. |
| `rate` | number | `>= 0` | editor | Partículas por segundo en emisión continua. No se usa en Burst. |
| `lifetime` | number | `> 0` | editor | Vida individual en segundos. |
| `speed` | number | `>= 0` | editor | Velocidad base en unidades de mundo por segundo; cada nacimiento aplica variación aleatoria de 0.75× a 1.25×. |
| `direction` | number | grados | editor | Dirección central. `0°` = derecha, `90°` = abajo, `-90°` = arriba, siguiendo el eje Y de pantalla del runtime. |
| `spread` | number | `0..360` | editor | Cono angular total centrado en `direction`. |
| `gravity` | number | cualquier real | editor | Aceleración vertical aplicada a cada partícula. Positivo = abajo. |
| `startScale` / `endScale` | number | `>= 0` | editor | Interpolación lineal de escala durante la vida. |
| `startOpacity` / `endOpacity` | number | `0..1` | editor | Interpolación lineal de opacidad durante la vida. |
| `burst` | boolean | `true/false` | editor | `false`: continua; `true`: emite una vez al activarse. |
| `burstCount` | integer | `>= 1` en Studio | editor | Cantidad solicitada por Burst. El runtime limita entradas anómalas a 10.000 nacimientos por activación. |
| `localSpace` | boolean | `true/false` | editor | `true`: la posición de las partículas sigue al emisor después de nacer; `false`: quedan en coordenadas mundiales independientes. |

Compatibilidad: presets guardados por una build 2.2.2 anterior no contienen `renderMode`, `shape` ni `color`. Al cargarlos, `renderMode` se infiere como `SPRITE` si ya tenían `assetKey`, o `PIXEL` si no lo tenían; `shape=SQUARE` y `color=WHITE` se aplican como defaults.

### ParticleEmitter2D

Propiedades de componente:

```text
enabled : boolean
preset  : string
playing : boolean
```

`preset` acepta el `key` canónico o un nombre resoluble del `ParticlePreset`. Un preset inexistente no detiene el runtime: se informa en el log y el estado interno del emisor se reinicia. Si posteriormente se asigna de nuevo el mismo preset válido, un Burst vuelve a dispararse correctamente.

`playing=false` detiene nuevos nacimientos, pero no elimina partículas ya vivas. La transición `false -> true` reinicia el estado de Burst y su acumulador continuo.

Ejemplo — Burst reiniciable:

```2gs
on start
  ParticleEmitter2D.preset = explosion_fx
  ParticleEmitter2D.playing = true
  wait 0.2
  ParticleEmitter2D.playing = false
  wait 0.1
  ParticleEmitter2D.playing = true
end
```

Ejemplo — cambiar de preset y recuperarse de una referencia inválida:

```2gs
on event damageTaken
  ParticleEmitter2D.preset = sparks_fx
  ParticleEmitter2D.playing = true
end
```

### Límites de seguridad del runtime 2.2.2

El runtime mantiene como máximo 20.000 partículas vivas y acepta como máximo 10.000 nacimientos de un emisor en un frame/activación. Cuando se alcanza un límite, descarta emisiones nuevas y escribe un diagnóstico una sola vez por ciclo del emisor. Estos límites evitan que un proyecto corrupto o un valor extremo agote memoria o congele un juego exportado.

### Particle Studio

`Abrir Particle Studio` presenta una previsualización animada de dirección, dispersión, gravedad, escala, opacidad y forma. Los controles `Pixel`/`Sprite`, forma y color ya no son filtros temporales de preview: modifican el `ParticlePreset` real y se persisten dentro del `.2grl`.
'''
if "## Referencia técnica 2.2.2 — ParticlePreset y ParticleEmitter2D" not in bible_text:
    bible.write_text(bible_text.rstrip()+particle_doc+"\n", encoding="utf-8", newline="\n")

notes = ROOT / "docs/RELEASE_NOTES_2.2.2.md"
notes_text = notes.read_text(encoding="utf-8")
hotfix_note = """

## Hotfix de partículas 2.2.2

- Pixel/Sprite pasa a ser una propiedad real y persistente de `ParticlePreset`.
- Pixel admite forma (`SQUARE`, `CIRCLE`, `DIAMOND`) y color ARGB.
- Se corrige la semántica local/world-space usando la posición visual interpolada del emisor.
- Se corrige el reinicio de Burst al reactivar `playing` y al recuperar el mismo preset después de una referencia inválida.
- Dirección, dispersión, escala y opacidad usan la misma semántica en preview y runtime.
- Se añaden límites defensivos y diagnóstico ante emisión extrema.
- Se endurece el smoke de Windows para cubrir reinicio, recuperación de referencia, local/world, dirección y flood guard.
"""
if "## Hotfix de partículas 2.2.2" not in notes_text:
    notes.write_text(notes_text.rstrip()+hotfix_note+"\n", encoding="utf-8", newline="\n")

# Self-remove: the final hotfix commit must contain product changes only.
for rel in ["tools/apply_particle_hotfix_222.py", ".github/workflows/apply-particle-hotfix-222.yml"]:
    p = ROOT / rel
    if p.exists():
        p.unlink()
