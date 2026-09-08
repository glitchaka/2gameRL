package com.buttclapdev.twogamerl.export;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class NativeExporter {
    private NativeExporter() {}

    public record Result(boolean success, Path application, Path zip, String message) {}

    public static Result export(GameProject project, Path destination, Consumer<String> log) {
        Consumer<String> out = log == null ? s -> {} : log;
        try {
            Files.createDirectories(destination);
            Path jpackage = locateJpackage();
            if (jpackage == null) return new Result(false, null, null, "No se encontró jpackage. Usa el editor empaquetado completo o un JDK 21 con JAVA_HOME configurado.");
            Path sourceInput = locateAppInput();
            if (sourceInput == null) return new Result(false, null, null, "No se encontró el paquete interno del runtime. Ejecuta scripts\\build-editor.bat una vez y vuelve a exportar.");

            String name = sanitizeName(project.getTitle());
            Path temp = Files.createTempDirectory("2gamerl-export-");
            Path input = temp.resolve("input"); Files.createDirectories(input);
            try (var files = Files.list(sourceInput)) {
                files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar"))
                        .forEach(p -> copyQuiet(p, input.resolve(p.getFileName())));
            }
            Path mainJar = input.resolve("2gameRL-Studio.jar");
            if (!Files.exists(mainJar)) return new Result(false, null, null, "Falta 2gameRL-Studio.jar en el paquete interno.");
            ProjectIO.save(project, input.resolve("game.2grl"));

            Path appPath = destination.resolve(name);
            deleteTree(appPath);
            List<String> command = new ArrayList<>();
            command.add(jpackage.toString()); command.add("--type"); command.add("app-image");
            command.add("--input"); command.add(input.toString()); command.add("--dest"); command.add(destination.toString());
            command.add("--name"); command.add(name); command.add("--main-jar"); command.add("2gameRL-Studio.jar");
            command.add("--main-class"); command.add("com.buttclapdev.twogamerl.runtime.GameLauncher");
            command.add("--app-version"); command.add("1.0.0");
            command.add("--description"); command.add("Juego creado con 2gameRL Studio");
            out.accept("Exportando aplicación nativa…");
            int code = run(command, out);
            if (code != 0 || !Files.exists(appPath)) return new Result(false, null, null, "jpackage terminó con código " + code + ". El registro de exportación contiene el detalle.");

            Path zip = destination.resolve(name + "-portable.zip"); Files.deleteIfExists(zip); zipDirectory(appPath, zip);
            Files.writeString(destination.resolve(name + "-LEEME.txt"),
                    "Este paquete es autocontenido. En Windows abre " + name + "\\" + name + ".exe.\nNo necesitas instalar Java en el equipo del jugador.\n",
                    StandardCharsets.UTF_8);
            deleteTree(temp);
            out.accept("Exportación completada: " + appPath);
            return new Result(true, appPath, zip, "Aplicación y ZIP portable creados correctamente.");
        } catch (Exception e) {
            out.accept("ERROR: " + e);
            return new Result(false, null, null, e.getMessage() == null ? e.toString() : e.getMessage());
        }
    }

    private static Path locateJpackage() {
        boolean win = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win"); String exe = win ? "jpackage.exe" : "jpackage";
        List<Path> candidates = new ArrayList<>();
        candidates.add(Path.of(System.getProperty("java.home", ""), "bin", exe));
        String javaHome = System.getenv("JAVA_HOME"); if (javaHome != null && !javaHome.isBlank()) candidates.add(Path.of(javaHome, "bin", exe));
        for (Path p : candidates) if (Files.isRegularFile(p)) return p;
        try {
            Process which = new ProcessBuilder(win ? "where" : "which", "jpackage").redirectErrorStream(true).start();
            String line = new BufferedReader(new InputStreamReader(which.getInputStream())).readLine(); if (line != null && Files.isRegularFile(Path.of(line.trim()))) return Path.of(line.trim());
        } catch (Exception ignored) {}
        return null;
    }

    private static Path locateAppInput() {
        try {
            Path code = Path.of(NativeExporter.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            if (Files.isRegularFile(code) && code.getFileName().toString().equals("2gameRL-Studio.jar")) return code.getParent();
        } catch (Exception ignored) {}
        Path p = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath();
        for (int i=0; p!=null && i<6; i++,p=p.getParent()) {
            Path candidate = p.resolve("target/app-input"); if (Files.exists(candidate.resolve("2gameRL-Studio.jar"))) return candidate;
        }
        return null;
    }

    private static int run(List<String> command, Consumer<String> out) throws Exception {
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) { String line; while ((line = r.readLine()) != null) out.accept(line); }
        return process.waitFor();
    }
    private static String sanitizeName(String title) { String n = title == null ? "2gameRL" : title.trim().replaceAll("[\\\\/:*?\"<>|]", "-"); return n.isBlank() ? "2gameRL" : n; }
    private static void copyQuiet(Path from, Path to) { try { Files.copy(from,to,StandardCopyOption.REPLACE_EXISTING); } catch (IOException e) { throw new UncheckedIOException(e); } }
    private static void deleteTree(Path root) throws IOException { if (root==null||!Files.exists(root))return; try(var walk=Files.walk(root)){for(Path p:walk.sorted(Comparator.reverseOrder()).toList())Files.deleteIfExists(p);} }
    private static void zipDirectory(Path root, Path zip) throws IOException {
        try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(zip));var walk=Files.walk(root)){
            for(Path p:walk.filter(Files::isRegularFile).toList()){String name=root.getFileName()+"/"+root.relativize(p).toString().replace('\\','/');out.putNextEntry(new ZipEntry(name));Files.copy(p,out);out.closeEntry();}
        }
    }
}
