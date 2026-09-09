package com.buttclapdev.twogamerl.studio;

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
    private static void draw(GraphicsContext g,Canvas canvas,StudioApp app,ParticlePreset p,List<MutableDot>dots){g.setImageSmoothing(false);g.setFill(Color.web("#11110f"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());double cx=canvas.getWidth()/2,cy=canvas.getHeight()/2;g.setStroke(Color.rgb(201,164,95,.45));double a0=Math.toRadians(p.direction-p.spread/2),a1=Math.toRadians(p.direction+p.spread/2);g.strokeLine(cx,cy,cx+Math.cos(a0)*100,cy+Math.sin(a0)*100);g.strokeLine(cx,cy,cx+Math.cos(a1)*100,cy+Math.sin(a1)*100);g.setFill(Color.web("#c9a45f"));g.fillOval(cx-4,cy-4,8,8);for(MutableDot d:dots){double t=Math.max(0,Math.min(1,d.age/Math.max(.01,p.lifetime))),scale=p.startScale+(p.endScale-p.startScale)*t,opacity=p.startOpacity+(p.endOpacity-p.startOpacity)*t,size=Math.max(2,12*Math.max(0,scale)),x=cx+d.x-size/2,y=cy+d.y-size/2;double old=g.getGlobalAlpha();g.setGlobalAlpha(Math.max(0,Math.min(1,opacity)));boolean sprite=p.renderMode==ParticleRenderMode.SPRITE&&!p.assetKey.isBlank()&&app.image(p.assetKey)!=null;if(sprite)app.drawAsset(g,p.assetKey,x,y,size,size);else drawPrimitive(g,p.shape,p.color,x,y,size);g.setGlobalAlpha(old);}}
    private static void drawPrimitive(GraphicsContext g,ParticleShape shape,java.awt.Color color,double x,double y,double size){g.setFill(fx(color));switch(shape==null?ParticleShape.SQUARE:shape){case CIRCLE->g.fillOval(x,y,size,size);case DIAMOND->g.fillPolygon(new double[]{x+size/2,x+size,x+size/2,x},new double[]{y,y+size/2,y+size,y+size/2},4);default->g.fillRect(x,y,size,size);}}
    private static Color fx(java.awt.Color c){java.awt.Color v=c==null?java.awt.Color.WHITE:c;return Color.rgb(v.getRed(),v.getGreen(),v.getBlue(),v.getAlpha()/255.0);}
    private static java.awt.Color awt(Color c){return new java.awt.Color((float)c.getRed(),(float)c.getGreen(),(float)c.getBlue(),(float)c.getOpacity());}
    private static final class MutableDot{double x,y,vx,vy,age;MutableDot(double vx,double vy){this.vx=vx;this.vy=vy;}}
}
