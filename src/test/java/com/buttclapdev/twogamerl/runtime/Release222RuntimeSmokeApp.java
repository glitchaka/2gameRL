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

    private static void runSmoke(){
        GameProject project=GameProject.createDefault();project.getMenus().clear();project.setStartMenu("");Level level=project.getLevels().get("level-1");level.entities.clear();
        ParticlePreset continuous=new ParticlePreset("continuous_fx","continuous_fx");continuous.rate=40;continuous.lifetime=2;continuous.speed=0;continuous.localSpace=false;project.getParticlePresets().put(continuous.key,continuous);
        ParticlePreset burst=new ParticlePreset("burst_fx","burst_fx");burst.burst=true;burst.burstCount=7;burst.lifetime=5;burst.speed=0;project.getParticlePresets().put(burst.key,burst);

        EntityDef prefabTemplate=new EntityDef("prefab-template","Prefab FX",0,0);prefabTemplate.components.add(ComponentDef.preset("SpriteRenderer"));project.getPrefabs().put("fx-prefab",PrefabDef.entity("fx-prefab","FX Prefab",prefabTemplate));
        EntityDef instance=prefabTemplate.copy();instance.prefabKey="fx-prefab";ComponentDef instanceEmitter=ComponentDef.preset("ParticleEmitter2D");instanceEmitter.properties.put("preset","continuous_fx");instance.components.add(instanceEmitter);instance.x=2;instance.y=2;level.entities.add(instance);

        EntityDef burstEntity=new EntityDef("burst","Burst",5,2);ComponentDef burstEmitter=ComponentDef.preset("ParticleEmitter2D");burstEmitter.properties.put("preset","burst_fx");burstEntity.components.add(burstEmitter);burstEntity.script="""
                on start
                  timer 0.10 ParticleEmitter2D.playing = false
                  timer 0.20 ParticleEmitter2D.playing = true
                end
                """;level.entities.add(burstEntity);

        EntityDef missing=new EntityDef("missing","Missing",8,2);ComponentDef missingEmitter=ComponentDef.preset("ParticleEmitter2D");missingEmitter.properties.put("preset","does_not_exist");missing.components.add(missingEmitter);level.entities.add(missing);

        EntityDef target=new EntityDef("target","Target",2,8);target.script="on start\n  timer 0.10 self.x = 12\nend\n";target.components.add(ComponentDef.preset("PlayerController"));level.entities.add(target);
        EntityDef camera=new EntityDef("camera","Camera",0,0);ComponentDef camera2d=ComponentDef.preset("Camera2D");camera2d.properties.put("target","target");camera2d.properties.put("smoothSpeed","2");camera2d.properties.put("deadZoneX","0");camera2d.properties.put("pixelSnap","false");camera.components.add(camera2d);level.entities.add(camera);

        GameView view=new GameView(project);view.stop();List<String>logs=new ArrayList<>();view.setLogger(s->{logs.add(s);System.out.println(s);});
        view.debugAdvance(.05);int first=view.debugParticleCount();require(first>=9,"ParticleEmitter2D no emitió continuous + burst; count="+first);System.out.println("PARTICLE_CONTINUOUS_OK");
        require(logs.stream().anyMatch(s->s.contains("does_not_exist")),"Preset inexistente no generó diagnóstico explícito.");System.out.println("PARTICLE_MISSING_REF_DIAGNOSTIC_OK");
        int beforeRestart=view.debugParticleCount();view.debugAdvance(.25);int afterRestart=view.debugParticleCount();require(afterRestart>=beforeRestart+7,"Burst no volvió a disparar tras playing false/true.");System.out.println("PARTICLE_BURST_RESTART_OK");
        require(afterRestart>14,"El ParticleEmitter2D añadido a una instancia de Prefab desapareció en runtime.");System.out.println("PREFAB_INSTANCE_COMPONENT_OK");
        double initialCamera=view.debugCameraCenter()[0];view.debugAdvance(.20);double smoothed=view.debugCameraCenter()[0];require(smoothed>initialCamera+.05&&smoothed<12.82,"Camera2D smoothing/target no respondió gradualmente: "+initialCamera+" -> "+smoothed);System.out.println("CAMERA_222_OK");
    }
    private static void require(boolean ok,String message){if(!ok)throw new IllegalStateException(message);}
}
