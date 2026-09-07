package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;

import javax.swing.*;
import java.io.*;
import java.nio.file.*;

public final class GameLauncher {
    private GameLauncher() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                GameProject project = loadProject(args);
                open(project, true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "No se pudo iniciar el juego:\n" + e.getMessage(), "2gameRL", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    public static JFrame open(GameProject project, boolean exitOnClose) {
        JFrame frame = new JFrame(project.getTitle());
        frame.setDefaultCloseOperation(exitOnClose ? WindowConstants.EXIT_ON_CLOSE : WindowConstants.DISPOSE_ON_CLOSE);
        frame.setContentPane(new GamePanel(project));
        frame.pack();
        frame.setMinimumSize(frame.getSize());
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        return frame;
    }

    private static GameProject loadProject(String[] args) throws IOException {
        if (args.length > 0 && !args[0].isBlank()) return ProjectIO.load(Path.of(args[0]));
        InputStream embedded = GameLauncher.class.getResourceAsStream("/game-project.2grl");
        if (embedded != null) try (embedded) { return ProjectIO.load(embedded); }
        return GameProject.createDefault();
    }
}
