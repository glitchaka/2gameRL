package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.ComponentDef;
import com.buttclapdev.twogamerl.model.GameProject.EntityDef;
import com.buttclapdev.twogamerl.model.GameProject.Level;
import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public final class RuntimeFeatureSmokeApp {
    private RuntimeFeatureSmokeApp() {}

    public static void main(String[] args) throws Exception {
        CountDownLatch ready=new CountDownLatch(1);Platform.startup(ready::countDown);ready.await();
        CountDownLatch finished=new CountDownLatch(1);AtomicReference<Throwable>failure=new AtomicReference<>();
        Platform.runLater(()->{try{runSmoke();}catch(Throwable error){failure.set(error);}finally{finished.countDown();}});
        finished.await();Platform.exit();if(failure.get()!=null){failure.get().printStackTrace();System.exit(1);}
    }

    private static void runSmoke() {
        GameProject project=GameProject.createDefault();project.getMenus().clear();project.setStartMenu("");Level level=project.getLevels().get("level-1");level.entities.clear();

        EntityDef template=new EntityDef("pj","PJ",10,10);template.components.add(ComponentDef.preset("BoxCollider2D"));template.script="on start\n  log \"PJ_CREATED\"\nend\n";level.entities.add(template);
        EntityDef spark=new EntityDef("spark","Spark",14,10);level.entities.add(spark);

        EntityDef spawner=new EntityDef("spawner","Spawner",2,2);spawner.script="""
                on click
                  destroy
                  wait 0.25
                  create pj
                end
                """;level.entities.add(spawner);

        EntityDef timerSource=new EntityDef("timer-source","TimerSource",4,4);timerSource.script="""
                on start
                  timer 0.15 create pj 7 8
                  log "TIMER_ARMED"
                end
                """;level.entities.add(timerSource);

        EntityDef mover=new EntityDef("mover","Mover",5,5);mover.components.add(ComponentDef.preset("BoxCollider2D"));mover.script="on click\n  move 1 0\nend\n";level.entities.add(mover);
        EntityDef blocker=new EntityDef("blocker","Blocker",5.9,5);blocker.components.add(ComponentDef.preset("BoxCollider2D"));level.entities.add(blocker);

        EntityDef grid=new EntityDef("grid","Grid",2,12);ComponentDef gridMove=ComponentDef.preset("GridMovement");gridMove.properties.put("step","2");grid.components.add(gridMove);grid.components.add(ComponentDef.preset("BoxCollider2D"));level.entities.add(grid);
        EntityDef gridBlocker=new EntityDef("grid-blocker","GridBlocker",4,12);gridBlocker.components.add(ComponentDef.preset("BoxCollider2D"));level.entities.add(gridBlocker);

        EntityDef target=new EntityDef("target","Target",11,6);level.entities.add(target);
        EntityDef logic=new EntityDef("logic","Logic",9,9);logic.script="""
                on start
                  setVar score 3
                  repeat 2
                    addVar score 2
                  end
                  ifVar score >= 7
                    setGlobal gate open
                    addComponent self Health
                    setComponent self Health max 50
                    setComponent self Health current 50
                    damage self 10
                    setEntity target x 12
                  else
                    setGlobal gate closed
                  end
                  every 0.05 3 create spark 12 12
                end
                """;level.entities.add(logic);

        GameView view=new GameView(project);view.stop();view.setLogger(System.out::println);

        require("7".equals(view.debugEntityVariable("logic","score")),"repeat/addVar no produjo score 7.");
        require("open".equals(view.debugGlobal("gate")),"ifVar/else o setGlobal falló.");
        require(Math.abs(view.debugHealth("logic")-40)<.001,"addComponent/setComponent/damage no se aplicó.");
        require(Math.abs(view.debugEntityPosition("target")[0]-12)<.001,"setEntity no modificó otra entidad.");
        System.out.println("ADVANCED_SCRIPT_STATE_OK");

        double beforeX=view.debugEntityPosition("mover")[0];view.debugFireClick("mover");double afterX=view.debugEntityPosition("mover")[0];require(Math.abs(beforeX-afterX)<1e-9,"BoxCollider2D dejó atravesar una entidad estática sin Rigidbody2D.");System.out.println("BOXCOLLIDER_STATIC_OK");

        double[] gridBefore=view.debugEntityPosition("grid");require(!view.debugGridStep("grid",1,0),"GridMovement atravesó un BoxCollider2D.");double[] gridBlocked=view.debugEntityPosition("grid");require(Math.abs(gridBefore[0]-gridBlocked[0])<1e-9,"GridMovement cambió X pese al bloqueo.");require(view.debugGridStep("grid",0,-1),"GridMovement no pudo mover a una celda libre.");double[] gridAfter=view.debugEntityPosition("grid");require(Math.abs(gridAfter[1]-10)<1e-9,"GridMovement no respetó step=2.");System.out.println("GRID_MOVEMENT_OK");

        view.debugFireClick("spawner");view.debugAdvance(.10);require(view.debugEntityCountByName("PJ")==1,"wait ejecutó create antes del tiempo indicado.");
        view.debugAdvance(.10);require(view.debugHasEntityAt("PJ",7,8),"timer no creó la entidad en la posición explícita.");System.out.println("TIMER_CREATE_OK");
        view.debugAdvance(.10);require(view.debugEntityCountByName("PJ")==3,"wait no continuó después de destruir la entidad origen.");require(view.debugHasEntityAt("PJ",2,2),"create sin coordenadas no usó la posición de la entidad origen.");System.out.println("WAIT_DESTROY_CREATE_OK");
        require(view.debugEntityCountByName("Spark")==4,"every no creó exactamente tres instancias además de la plantilla.");System.out.println("EVERY_REPEAT_OK");
    }

    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
}
