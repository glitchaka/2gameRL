package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.model.ParticleVisualMeta;
import com.buttclapdev.twogamerl.model.ResourceRef;
import javafx.animation.AnimationTimer;
import javafx.stage.Window;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 2.2.2 compatibility/runtime layer for particle behavior that was present in
 * the data model but incomplete in the 2.2.1 renderer.
 */
public final class ParticleRuntime222 {
    private static final String ATTACHED="2rl.particles.runtime.2.2.2";
    private ParticleRuntime222(){}

    public static void attach(GameView view){
        if(view==null||Boolean.TRUE.equals(view.getProperties().get(ATTACHED)))return;
        view.getProperties().put(ATTACHED,true);
        GameProject project=field(view,"project",GameProject.class);if(project==null)return;
        ResourceRef.installRuntimeAliases(project);
        new Patch(view,project).start();
    }

    private static final class Patch extends AnimationTimer {
        private final GameView view;private final GameProject project;
        private final IdentityHashMap<Object,ParticleTrack>particles=new IdentityHashMap<>();
        private final IdentityHashMap<Object,EmitterTrack>emitters=new IdentityHashMap<>();
        Patch(GameView view,GameProject project){this.view=view;this.project=project;}

        @Override public void handle(long now){
            Window window=view.getScene()==null?null:view.getScene().getWindow();if(window!=null&&!window.isShowing()){stop();view.getProperties().remove(ATTACHED);return;}
            Collection<?>bodyValues=bodyValues();if(bodyValues==null)return;
            updateEmitters(bodyValues);
            List<?>live=field(view,"particles",List.class);if(live==null)return;
            Set<Object>liveSet=Collections.newSetFromMap(new IdentityHashMap<>());liveSet.addAll(live);particles.keySet().removeIf(p->!liveSet.contains(p));
            for(Object particle:live){ParticleTrack track=particles.get(particle);if(track==null){track=bindParticle(particle,bodyValues);particles.put(particle,track);}applyLocalSpace(particle,track);}
        }

        private Collection<?>bodyValues(){Map<?,?>b=field(view,"bodies",Map.class);return b==null?null:b.values();}
        private void updateEmitters(Collection<?> bodies){
            Set<Object>live=Collections.newSetFromMap(new IdentityHashMap<>());live.addAll(bodies);emitters.keySet().removeIf(b->!live.contains(b));
            for(Object body:bodies){EntityDef def=field(body,"def",EntityDef.class);if(def==null)continue;ComponentDef emitter=def.component("ParticleEmitter2D");if(emitter==null||!emitter.bool("enabled",true))continue;boolean playing=emitter.bool("playing",true);String preset=emitter.get("preset","");EmitterTrack old=emitters.get(body);if(old==null){emitters.put(body,new EmitterTrack(playing,preset));continue;}if((!old.playing&&playing)||!Objects.equals(old.preset,preset)){setBoolean(body,"particleBurstDone",false);setDouble(body,"particleAccumulator",0);old.preset=preset;}old.playing=playing;
            }
        }

        private ParticleTrack bindParticle(Object particle,Collection<?> bodies){
            ParticlePreset preset=field(particle,"preset",ParticlePreset.class);double px=number(particle,"x"),py=number(particle,"y");Object best=null;double bestD=Double.POSITIVE_INFINITY;
            for(Object body:bodies){EntityDef def=field(body,"def",EntityDef.class);if(def==null)continue;ComponentDef e=def.component("ParticleEmitter2D");if(e==null||!e.bool("enabled",true))continue;ParticlePreset candidate=ResourceRef.particlePreset(project,e.get("preset",""));if(candidate!=preset)continue;double cx=def.x+def.width/2,cy=def.y+def.height/2,d=Math.hypot(px-cx,py-cy);if(d<bestD){bestD=d;best=body;}}
            rotateNewParticle(particle,preset);
            if(best==null)return new ParticleTrack(null,0,0);
            EntityDef def=field(best,"def",EntityDef.class);return new ParticleTrack(best,def.x+def.width/2,def.y+def.height/2);
        }

        private void rotateNewParticle(Object particle,ParticlePreset preset){
            if(preset==null||preset.assetKey==null||preset.assetKey.isBlank())return;Asset visual=project.getAssets().get(preset.assetKey);if(visual==null)return;double direction=ParticleVisualMeta.direction(visual),delta=Math.toRadians(direction-270);if(Math.abs(delta)<1e-12)return;double vx=number(particle,"vx"),vy=number(particle,"vy"),cos=Math.cos(delta),sin=Math.sin(delta);setDouble(particle,"vx",vx*cos-vy*sin);setDouble(particle,"vy",vx*sin+vy*cos);
        }

        private void applyLocalSpace(Object particle,ParticleTrack track){
            if(track==null||track.body==null)return;ParticlePreset preset=field(particle,"preset",ParticlePreset.class);if(preset==null||!preset.localSpace)return;EntityDef def=field(track.body,"def",EntityDef.class);if(def==null)return;double cx=def.x+def.width/2,cy=def.y+def.height/2,dx=cx-track.lastX,dy=cy-track.lastY;if(Math.abs(dx)>1e-12)setDouble(particle,"x",number(particle,"x")+dx);if(Math.abs(dy)>1e-12)setDouble(particle,"y",number(particle,"y")+dy);track.lastX=cx;track.lastY=cy;
        }
    }

    private static final class ParticleTrack {final Object body;double lastX,lastY;ParticleTrack(Object body,double x,double y){this.body=body;lastX=x;lastY=y;}}
    private static final class EmitterTrack {boolean playing;String preset;EmitterTrack(boolean playing,String preset){this.playing=playing;this.preset=preset;}}

    private static double number(Object owner,String name){Object v=value(owner,name);return v instanceof Number n?n.doubleValue():0;}
    private static Object value(Object owner,String name){try{Field f=findField(owner.getClass(),name);if(f==null)return null;f.setAccessible(true);return f.get(owner);}catch(Exception e){return null;}}
    private static void setDouble(Object owner,String name,double v){try{Field f=findField(owner.getClass(),name);if(f!=null){f.setAccessible(true);f.setDouble(owner,v);}}catch(Exception ignored){}}
    private static void setBoolean(Object owner,String name,boolean v){try{Field f=findField(owner.getClass(),name);if(f!=null){f.setAccessible(true);f.setBoolean(owner,v);}}catch(Exception ignored){}}
    private static Field findField(Class<?>type,String name){for(Class<?>c=type;c!=null;c=c.getSuperclass())try{return c.getDeclaredField(name);}catch(NoSuchFieldException ignored){}return null;}
    private static <T>T field(Object owner,String name,Class<T>type){Object v=value(owner,name);return type.isInstance(v)?type.cast(v):null;}
}
