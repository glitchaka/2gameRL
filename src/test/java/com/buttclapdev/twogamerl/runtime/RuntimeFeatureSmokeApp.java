package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.ComponentDef;
import com.buttclapdev.twogamerl.model.GameProject.EntityDef;
import com.buttclapdev.twogamerl.model.GameProject.Level;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public final class RuntimeFeatureSmokeApp extends Application {
    @Override public void start(Stage stage) {
        try {
            GameProject project = GameProject.createDefault();
            project.getMenus().clear();
            project.setStartMenu("");
            Level level = project.getLevels().get("level-1");
            level.entities.clear();

            EntityDef template = new EntityDef("pj", "PJ", 10, 10);
            template.components.add(ComponentDef.preset("BoxCollider2D"));
            template.script = "on start\n  log \"PJ_CREATED\"\nend\n";
            level.entities.add(template);

            EntityDef spawner = new EntityDef("spawner", "Spawner", 2, 2);
            spawner.script = """
                    on click
                      destroy
                      wait 0.25
                      create pj
                    end
                    """;
            level.entities.add(spawner);

            EntityDef timerSource = new EntityDef("timer-source", "TimerSource", 4, 4);
            timerSource.script = """
                    on start
                      timer 0.15 create pj 7 8
                      log "TIMER_ARMED"
                    end
                    """;
            level.entities.add(timerSource);

            EntityDef mover = new EntityDef("mover", "Mover", 5, 5);
            mover.components.add(ComponentDef.preset("BoxCollider2D"));
            mover.script = "on click\n  move 1 0\nend\n";
            level.entities.add(mover);

            EntityDef blocker = new EntityDef("blocker", "Blocker", 5.9, 5);
            blocker.components.add(ComponentDef.preset("BoxCollider2D"));
            level.entities.add(blocker);

            GameView view = new GameView(project);
            view.stop();
            view.setLogger(System.out::println);

            double beforeX = view.debugEntityPosition("mover")[0];
            view.debugFireClick("mover");
            double afterX = view.debugEntityPosition("mover")[0];
            require(Math.abs(beforeX - afterX) < 1e-9, "BoxCollider2D dejó atravesar una entidad estática sin Rigidbody2D.");
            System.out.println("BOXCOLLIDER_STATIC_OK");

            view.debugFireClick("spawner");
            view.debugAdvance(.10);
            require(view.debugEntityCountByName("PJ") == 1, "wait ejecutó create antes del tiempo indicado.");
            view.debugAdvance(.10);
            require(view.debugHasEntityAt("PJ", 7, 8), "timer no creó la entidad en la posición explícita.");
            System.out.println("TIMER_CREATE_OK");
            view.debugAdvance(.10);
            require(view.debugEntityCountByName("PJ") == 3, "wait no continuó después de destruir la entidad origen.");
            require(view.debugHasEntityAt("PJ", 2, 2), "create sin coordenadas no usó la posición de la entidad origen.");
            System.out.println("WAIT_DESTROY_CREATE_OK");

            Platform.exit();
        } catch (Throwable error) {
            error.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static void main(String[] args) { launch(args); }
}
