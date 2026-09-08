package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.ComponentDef;
import com.buttclapdev.twogamerl.model.GameProject.EntityDef;
import java.nio.file.Path;

public final class SmokeProjectCreator {
    private SmokeProjectCreator() {}
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Uso: SmokeProjectCreator <salida.2grl>");
        GameProject project=GameProject.createDefault();project.setTitle("2gameRL-Smoke");project.getMenus().clear();project.setStartMenu("");
        var level=project.getLevels().get("level-1");EntityDef player=new EntityDef("player","Jugador",2,2);player.assetKey="placeholder-player.png";player.renderLayer="Personajes";player.physicsLayer="Player";player.components.add(ComponentDef.preset("GridMovement"));player.components.add(ComponentDef.preset("BoxCollider2D"));player.script="on start\n  log \"Jugador iniciado\"\n  save.smoke = true\nend\n";level.entities.add(player);
        ProjectIO.save(project,Path.of(args[0]));
    }
}
