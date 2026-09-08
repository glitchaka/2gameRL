package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;

import java.nio.file.Path;

public final class SmokeProjectCreator {
    private SmokeProjectCreator() {}
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("Uso: SmokeProjectCreator <salida.2grl>");
        GameProject project = GameProject.createDefault();
        project.setTitle("2gameRL-Smoke");
        project.getMenus().clear();
        project.setStartMenu("");
        ProjectIO.save(project, Path.of(args[0]));
    }
}
