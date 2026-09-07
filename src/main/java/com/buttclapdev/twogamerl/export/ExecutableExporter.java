package com.buttclapdev.twogamerl.export;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class ExecutableExporter {
    private ExecutableExporter() {}

    public static void export(Component parent, GameProject project) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Carpeta de exportación");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) return;
        Path output = chooser.getSelectedFile().toPath().toAbsolutePath();
        Path root = findAuthoringRoot();
        if (root == null) {
            JOptionPane.showMessageDialog(parent, "No encontré las herramientas de exportación (carpeta scripts).", "Exportar", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        Path script = root.resolve("scripts").resolve(windows ? "export-game.bat" : "export-game.sh");
        try {
            Path temp = Files.createTempFile("2gamerl-export-", ".2grl");
            ProjectIO.save(project, temp);
            JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Exportando juego", Dialog.ModalityType.APPLICATION_MODAL);
            JTextArea log = new JTextArea(16, 72); log.setEditable(false);
            dialog.add(new JScrollPane(log)); dialog.pack(); dialog.setLocationRelativeTo(parent);
            SwingWorker<Integer, String> worker = new SwingWorker<>() {
                @Override protected Integer doInBackground() throws Exception {
                    ProcessBuilder pb = windows
                            ? new ProcessBuilder("cmd", "/c", script.toString(), temp.toString(), output.toString())
                            : new ProcessBuilder("bash", script.toString(), temp.toString(), output.toString());
                    pb.directory(root.toFile()); pb.redirectErrorStream(true);
                    Process process = pb.start();
                    try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                        String line; while ((line = r.readLine()) != null) publish(line);
                    }
                    return process.waitFor();
                }
                @Override protected void process(java.util.List<String> chunks) { chunks.forEach(s -> log.append(s + "\n")); }
                @Override protected void done() {
                    try {
                        int code = get();
                        if (code == 0) JOptionPane.showMessageDialog(dialog, "Exportación terminada en:\n" + output, "Exportar", JOptionPane.INFORMATION_MESSAGE);
                        else JOptionPane.showMessageDialog(dialog, "La exportación terminó con código " + code + ". Revisa el registro.", "Exportar", JOptionPane.ERROR_MESSAGE);
                    } catch (Exception e) { JOptionPane.showMessageDialog(dialog, e.getMessage(), "Exportar", JOptionPane.ERROR_MESSAGE); }
                    finally { try { Files.deleteIfExists(temp); } catch (IOException ignored) {} dialog.dispose(); }
                }
            };
            worker.execute(); dialog.setVisible(true);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Exportar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Path findAuthoringRoot() {
        java.util.List<Path> starts = new ArrayList<>();
        starts.add(Path.of(System.getProperty("user.dir", ".")).toAbsolutePath());
        try { starts.add(Path.of(ExecutableExporter.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath()); } catch (Exception ignored) {}
        for (Path start : starts) {
            Path p = Files.isRegularFile(start) ? start.getParent() : start;
            for (int i=0; p!=null && i<8; i++, p=p.getParent()) {
                if (Files.exists(p.resolve("scripts/export-game.bat")) || Files.exists(p.resolve("scripts/export-game.sh"))) return p;
                Path authoring = p.resolve("authoring");
                if (Files.exists(authoring.resolve("scripts/export-game.bat")) || Files.exists(authoring.resolve("scripts/export-game.sh"))) return authoring;
            }
        }
        return null;
    }
}
