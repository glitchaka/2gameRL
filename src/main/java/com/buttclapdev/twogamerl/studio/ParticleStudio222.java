package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.ParticlePreset;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Live particle preview used by the 2.2.2 particle editor. */
final class ParticleStudio222 {
    private record Dot(double born,double vx,double vy,double x,double y){}
    private ParticleStudio222(){}

    static void show(Stage owner,StudioApp app,ParticlePreset preset){if(preset==null)return;Stage stage=new Stage();stage.initOwner(owner);stage.initModality(Modality.NONE);stage.setTitle("Particle Studio · "+preset.key);Canvas canvas=new Canvas(760,520);ToggleButton sprite=new ToggleButton("Sprite"),pixel=new ToggleButton("Pixel");ToggleGroup group=new ToggleGroup();sprite.setToggleGroup(group);pixel.setToggleGroup(group);sprite.setSelected(true);Button restart=new Button("Reiniciar");Label info=new Label("Preview en tiempo real · dirección, dispersión, gravedad, escala y opacidad");info.getStyleClass().add("muted");HBox toolbar=new HBox(8,sprite,pixel,restart,info);toolbar.setPadding(new Insets(8));BorderPane root=new BorderPane(canvas,toolbar,null,null,null);root.getStyleClass().add("studio-root");Scene scene=new Scene(root,780,570);var css=ParticleStudio222.class.getResource("/com/buttclapdev/twogamerl/studio.css");if(css!=null)scene.getStylesheets().add(css.toExternalForm());stage.setScene(scene);ThemeManager.apply(scene);
        List<MutableDot>dots=new ArrayList<>();double[]clock={0},acc={0};boolean[]burstDone={false};Runnable reset=()->{dots.clear();clock[0]=0;acc[0]=0;burstDone[0]=false;};restart.setOnAction(e->reset.run());AnimationTimer timer=new AnimationTimer(){long last;@Override public void handle(long now){if(last==0)last=now;double dt=Math.min(.05,(now-last)/1e9);last=now;clock[0]+=dt;if(preset.burst&&!burstDone[0]){for(int i=0;i<preset.burstCount;i++)spawn(dots,preset);burstDone[0]=true;}else if(!preset.burst){acc[0]+=Math.max(0,preset.rate)*dt;int guard=0;while(acc[0]>=1&&guard++<5000){acc[0]--;spawn(dots,preset);}}for(Iterator<MutableDot>it=dots.iterator();it.hasNext();){MutableDot d=it.next();d.age+=dt;if(d.age>=Math.max(.01,preset.lifetime)){it.remove();continue;}d.vy+=preset.gravity*35*dt;d.x+=d.vx*35*dt;d.y+=d.vy*35*dt;}draw(canvas.getGraphicsContext2D(),canvas,app,preset,dots,sprite.isSelected());}};stage.setOnShown(e->timer.start());stage.setOnHidden(e->timer.stop());stage.show();}

    private static void spawn(List<MutableDot>dots,ParticlePreset p){double a=Math.toRadians(p.direction+(ThreadLocalRandom.current().nextDouble()-.5)*p.spread),speed=p.speed*(.75+ThreadLocalRandom.current().nextDouble()*.5);dots.add(new MutableDot(Math.cos(a)*speed,Math.sin(a)*speed));}
    private static void draw(GraphicsContext g,Canvas canvas,StudioApp app,ParticlePreset p,List<MutableDot>dots,boolean sprite){g.setImageSmoothing(false);g.setFill(Color.web("#101722"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());double cx=canvas.getWidth()/2,cy=canvas.getHeight()/2;g.setStroke(Color.rgb(120,200,255,.45));double a0=Math.toRadians(p.direction-p.spread/2),a1=Math.toRadians(p.direction+p.spread/2);g.strokeLine(cx,cy,cx+Math.cos(a0)*100,cy+Math.sin(a0)*100);g.strokeLine(cx,cy,cx+Math.cos(a1)*100,cy+Math.sin(a1)*100);g.setFill(Color.web("#55b7ff"));g.fillOval(cx-4,cy-4,8,8);for(MutableDot d:dots){double t=Math.max(0,Math.min(1,d.age/Math.max(.01,p.lifetime))),scale=p.startScale+(p.endScale-p.startScale)*t,opacity=p.startOpacity+(p.endOpacity-p.startOpacity)*t,size=Math.max(2,12*scale),x=cx+d.x-size/2,y=cy+d.y-size/2;double old=g.getGlobalAlpha();g.setGlobalAlpha(Math.max(0,Math.min(1,opacity)));if(sprite&&!p.assetKey.isBlank()&&app.image(p.assetKey)!=null)app.drawAsset(g,p.assetKey,x,y,size,size);else{g.setFill(Color.WHITE);g.fillRect(x,y,size,size);}g.setGlobalAlpha(old);}}
    private static final class MutableDot{double x,y,vx,vy,age;MutableDot(double vx,double vy){this.vx=vx;this.vy=vy;}}
}
