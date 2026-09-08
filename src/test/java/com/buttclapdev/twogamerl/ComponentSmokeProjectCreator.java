package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.ComponentDef;
import com.buttclapdev.twogamerl.model.GameProject.EntityDef;
import com.buttclapdev.twogamerl.model.GameProject.Level;

import java.nio.file.Path;

public final class ComponentSmokeProjectCreator {
    private ComponentSmokeProjectCreator() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new IllegalArgumentException("Uso: ComponentSmokeProjectCreator <components|portal> <salida.2grl>");
        GameProject project = switch (args[0]) {
            case "components" -> componentsProject();
            case "portal" -> portalProject();
            default -> throw new IllegalArgumentException("Modo desconocido: " + args[0]);
        };
        ProjectIO.save(project, Path.of(args[1]));
    }

    private static GameProject componentsProject() {
        GameProject project = GameProject.createDefault();
        project.setTitle("2gameRL-Components-Smoke");
        project.getMenus().clear();
        project.setStartMenu("");
        Level level = project.getLevels().get("level-1");

        EntityDef player = level.entity("player");
        player.components.add(ComponentDef.preset("Health"));

        EntityDef trigger = new EntityDef("trigger-smoke", "TriggerSmoke", 2, 2);
        ComponentDef triggerComponent = ComponentDef.preset("Trigger");
        triggerComponent.properties.put("once", "true");
        trigger.components.add(triggerComponent);
        trigger.script = "on trigger\n  log \"TRIGGER_OK\"\nend\n";
        level.entities.add(trigger);

        EntityDef damage = new EntityDef("damage-smoke", "DamageSmoke", 2, 2);
        damage.width = .4;
        damage.height = .4;
        ComponentDef damageComponent = ComponentDef.preset("DamageOnContact");
        damageComponent.properties.put("damage", "10");
        damage.components.add(damageComponent);
        level.entities.add(damage);

        EntityDef rigid = new EntityDef("rigid-smoke", "RigidSmoke", 5, 12.5);
        rigid.components.add(ComponentDef.preset("Rigidbody2D"));
        rigid.components.add(ComponentDef.preset("BoxCollider2D"));
        rigid.script = "on collision\n  log \"RIGIDBODY_OK\"\nend\n";
        level.entities.add(rigid);

        EntityDef patrol = new EntityDef("patrol-smoke", "PatrolSmoke", 18, 5);
        ComponentDef patrolComponent = ComponentDef.preset("Patrol");
        patrolComponent.properties.put("distance", "8");
        patrolComponent.properties.put("speed", "6");
        patrol.components.add(patrolComponent);
        patrol.components.add(ComponentDef.preset("BoxCollider2D"));
        patrol.script = "on collision\n  log \"PATROL_COLLIDER_OK\"\nend\n";
        level.entities.add(patrol);

        EntityDef blocker = new EntityDef("blocker-smoke", "BlockerSmoke", 20.2, 5);
        blocker.components.add(ComponentDef.preset("BoxCollider2D"));
        level.entities.add(blocker);
        return project;
    }

    private static GameProject portalProject() {
        GameProject project = GameProject.createDefault();
        project.setTitle("2gameRL-Portal-Smoke");
        project.getMenus().clear();
        project.setStartMenu("");
        Level source = project.getLevels().get("level-1");

        EntityDef portal = new EntityDef("portal-smoke", "PortalSmoke", 2, 2);
        ComponentDef portalComponent = ComponentDef.preset("ScenePortal");
        portalComponent.properties.put("targetScene", "portal-target");
        portalComponent.properties.put("targetX", "7");
        portalComponent.properties.put("targetY", "8");
        portal.components.add(portalComponent);
        source.entities.add(portal);

        Level target = new Level("portal-target", "Portal Target", 24, 16);
        for (int y = 0; y < target.height; y++) {
            for (int x = 0; x < target.width; x++) {
                target.set(x, y, x == 0 || y == 0 || x == target.width - 1 || y == target.height - 1 ? 1 : 0);
            }
        }
        EntityDef player = new EntityDef("portal-player", "PortalPlayer", 2, 2);
        player.components.add(ComponentDef.preset("PlayerController"));
        player.components.add(ComponentDef.preset("BoxCollider2D"));
        player.script = "on start\n  log \"PORTAL_TARGET_OK\"\nend\n";
        target.entities.add(player);
        project.getLevels().put(target.id, target);
        return project;
    }
}
