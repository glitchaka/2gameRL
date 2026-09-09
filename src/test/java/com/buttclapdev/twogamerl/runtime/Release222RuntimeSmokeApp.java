package com.buttclapdev.twogamerl.runtime;

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
