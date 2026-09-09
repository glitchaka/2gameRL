package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/** Windows/JavaFX smoke for the gameplay-facing parts of the 2.2 visual pipeline. */
public final class VisualPipeline22SmokeApp {
    private VisualPipeline22SmokeApp() {}

    public static void main(String[] args) throws Exception {
        CountDownLatch ready=new CountDownLatch(1);Platform.startup(ready::countDown);ready.await();
        CountDownLatch finished=new CountDownLatch(1);AtomicReference<Throwable>failure=new AtomicReference<>();
        Platform.runLater(()->{try{runSmoke();}catch(Throwable error){failure.set(error);}finally{finished.countDown();}});
        finished.await();Platform.exit();if(failure.get()!=null){failure.get().printStackTrace();System.exit(1);}
    }

    private static void runSmoke(){
        GameProject project=GameProject.createDefault();project.getMenus().clear();project.setStartMenu("");
        Level level=project.getLevels().get("level-1");level.entities.clear();
        TileLayer floor=level.baseLayer();for(int x=0;x<level.width;x++)floor.set(x,4,1);

        EntityDef player=new EntityDef("player","Player",2,3.18);player.physicsLayer="Player";
        player.components.add(ComponentDef.preset("PlatformerController"));
        player.components.add(ComponentDef.preset("Rigidbody2D"));
        player.components.add(ComponentDef.preset("BoxCollider2D"));
        level.entities.add(player);

        AnimationClip blink=new AnimationClip("blink","Blink");blink.loop=true;blink.frames.add(new AnimationFrame("sample-00-00",.05));AnimationFrame second=new AnimationFrame("sample-01-00",.05);second.event="frame2";blink.frames.add(second);project.getAnimationClips().put(blink.key,blink);
        EntityDef animated=new EntityDef("animated","Animated",6,2);ComponentDef animator=ComponentDef.preset("Animator");animator.properties.put("clip","blink");animated.components.add(animator);animated.script="on animationEvent frame2\n  setVar marker yes\nend\n";level.entities.add(animated);

        EntityDef slimeTemplate=new EntityDef("slime-template","Slime",0,0);slimeTemplate.components.add(ComponentDef.preset("Health"));project.getPrefabs().put("slime",PrefabDef.entity("slime","Slime",slimeTemplate));
        EntityDef spawner=new EntityDef("spawner","Spawner",10,2);spawner.script="on start\n  spawnPrefab slime 11 2\nend\n";level.entities.add(spawner);

        GameView view=new GameView(project);view.stop();view.setLogger(System.out::println);

        view.debugAdvance(.01);
        require(view.debugGrounded("player"),"Rigidbody2D.grounded no detectó el suelo sólido antes de procesar input.");
        double before=view.debugEntityPosition("player")[1];
        view.debugPressKey("SPACE");view.debugAdvance(.01);view.debugReleaseKey("SPACE");
        double after=view.debugEntityPosition("player")[1];
        require(after<before,"PlatformerController no aplicó el salto con la acción Jump estando grounded.");
        System.out.println("PLATFORMER_GROUNDED_JUMP_OK");

        view.debugAdvance(.06);
        require(view.debugAnimationFrame("animated")==1,"Animator no avanzó al segundo frame.");
        require("yes".equals(view.debugEntityVariable("animated","marker")),"El evento de frame de AnimationClip no llegó a 2GameScript.");
        System.out.println("ANIMATOR_FRAME_EVENT_OK");

        require(view.debugHasEntityAt("Slime",11,2),"spawnPrefab no creó la instancia en la posición indicada.");
        System.out.println("PREFAB_SPAWN_OK");
    }

    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
