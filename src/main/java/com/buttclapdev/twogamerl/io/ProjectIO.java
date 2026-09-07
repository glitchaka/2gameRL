package com.buttclapdev.twogamerl.io;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;

import java.awt.Color;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

public final class ProjectIO {
    private ProjectIO() {}

    public static void save(GameProject project, Path file) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            Properties root = new Properties();
            root.setProperty("formatVersion", Integer.toString(GameProject.FORMAT_VERSION));
            root.setProperty("title", project.getTitle());
            root.setProperty("startLevel", safe(project.getStartLevel()));
            root.setProperty("startMenu", safe(project.getStartMenu()));
            root.setProperty("tileSize", Integer.toString(project.getTileSize()));
            putProperties(zip, "project.properties", root);

            Properties tiles = new Properties();
            tiles.setProperty("count", Integer.toString(project.getTiles().size()));
            int ti = 0;
            for (TileDef t : project.getTiles().values()) {
                String p = "tile." + ti++ + ".";
                tiles.setProperty(p + "id", Integer.toString(t.id));
                tiles.setProperty(p + "name", safe(t.name));
                tiles.setProperty(p + "rgb", Integer.toString(t.color.getRGB()));
                tiles.setProperty(p + "walkable", Boolean.toString(t.walkable));
                tiles.setProperty(p + "asset", safe(t.assetKey));
            }
            putProperties(zip, "tiles.properties", tiles);

            for (Level level : project.getLevels().values()) {
                Properties p = new Properties();
                p.setProperty("id", level.id);
                p.setProperty("name", safe(level.name));
                p.setProperty("width", Integer.toString(level.width));
                p.setProperty("height", Integer.toString(level.height));
                p.setProperty("spawnX", Integer.toString(level.spawnX));
                p.setProperty("spawnY", Integer.toString(level.spawnY));
                StringJoiner cells = new StringJoiner(",");
                for (int v : level.cells()) cells.add(Integer.toString(v));
                p.setProperty("cells", cells.toString());
                putProperties(zip, "levels/" + level.id + ".properties", p);
            }

            for (MenuScreen menu : project.getMenus().values()) {
                Properties p = new Properties();
                p.setProperty("id", menu.id);
                p.setProperty("title", safe(menu.title));
                p.setProperty("background", Integer.toString(menu.background.getRGB()));
                p.setProperty("buttonCount", Integer.toString(menu.buttons.size()));
                for (int i = 0; i < menu.buttons.size(); i++) {
                    MenuButton b = menu.buttons.get(i);
                    String k = "button." + i + ".";
                    p.setProperty(k + "text", safe(b.text));
                    p.setProperty(k + "x", Integer.toString(b.x));
                    p.setProperty(k + "y", Integer.toString(b.y));
                    p.setProperty(k + "width", Integer.toString(b.width));
                    p.setProperty(k + "height", Integer.toString(b.height));
                    p.setProperty(k + "action", b.action.name());
                    p.setProperty(k + "target", safe(b.target));
                }
                putProperties(zip, "menus/" + menu.id + ".properties", p);
            }

            for (Asset asset : project.getAssets().values()) {
                if (asset.data == null) continue;
                ZipEntry entry = new ZipEntry("assets/" + asset.key);
                zip.putNextEntry(entry);
                zip.write(asset.data);
                zip.closeEntry();
                Properties meta = new Properties();
                meta.setProperty("sourceName", safe(asset.sourceName));
                putProperties(zip, "asset-meta/" + asset.key + ".properties", meta);
            }
        }
    }

    public static GameProject load(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) { return load(in); }
    }

    public static GameProject load(InputStream input) throws IOException {
        GameProject p = new GameProject();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) {
                if (!e.isDirectory()) entries.put(e.getName(), zip.readAllBytes());
                zip.closeEntry();
            }
        }
        Properties root = props(entries.get("project.properties"));
        if (root.isEmpty()) throw new IOException("El archivo no contiene un proyecto 2gameRL válido.");
        p.setTitle(root.getProperty("title", "2gameRL"));
        p.setStartLevel(root.getProperty("startLevel", ""));
        p.setStartMenu(root.getProperty("startMenu", ""));
        p.setTileSize(integer(root, "tileSize", 32));

        Properties tiles = props(entries.get("tiles.properties"));
        int tileCount = integer(tiles, "count", 0);
        for (int i = 0; i < tileCount; i++) {
            String k = "tile." + i + ".";
            int id = integer(tiles, k + "id", i);
            TileDef t = new TileDef(id, tiles.getProperty(k + "name", "Tile " + id),
                    new Color(integer(tiles, k + "rgb", Color.GRAY.getRGB()), true),
                    Boolean.parseBoolean(tiles.getProperty(k + "walkable", "true")),
                    tiles.getProperty(k + "asset", ""));
            p.getTiles().put(id, t);
        }

        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("levels/") && name.endsWith(".properties")) {
                Properties lp = props(e.getValue());
                String id = lp.getProperty("id", fileStem(name));
                Level level = new Level(id, lp.getProperty("name", id), integer(lp, "width", 20), integer(lp, "height", 15));
                level.spawnX = integer(lp, "spawnX", 1);
                level.spawnY = integer(lp, "spawnY", 1);
                String[] raw = lp.getProperty("cells", "").split(",");
                int[] cells = new int[level.width * level.height];
                for (int i = 0; i < Math.min(cells.length, raw.length); i++) {
                    try { cells[i] = Integer.parseInt(raw[i]); } catch (NumberFormatException ignored) {}
                }
                level.replaceCells(cells);
                p.getLevels().put(id, level);
            } else if (name.startsWith("menus/") && name.endsWith(".properties")) {
                Properties mp = props(e.getValue());
                String id = mp.getProperty("id", fileStem(name));
                MenuScreen menu = new MenuScreen(id, mp.getProperty("title", id));
                menu.background = new Color(integer(mp, "background", new Color(27,31,36).getRGB()), true);
                int count = integer(mp, "buttonCount", 0);
                for (int i = 0; i < count; i++) {
                    String k = "button." + i + ".";
                    MenuAction action;
                    try { action = MenuAction.valueOf(mp.getProperty(k + "action", "START_GAME")); }
                    catch (IllegalArgumentException ex) { action = MenuAction.START_GAME; }
                    menu.buttons.add(new MenuButton(mp.getProperty(k + "text", "Botón"),
                            integer(mp, k + "x", 100), integer(mp, k + "y", 100),
                            integer(mp, k + "width", 180), integer(mp, k + "height", 44),
                            action, mp.getProperty(k + "target", "")));
                }
                p.getMenus().put(id, menu);
            } else if (name.startsWith("assets/") && !name.endsWith("/")) {
                String key = name.substring("assets/".length());
                Properties meta = props(entries.get("asset-meta/" + key + ".properties"));
                p.getAssets().put(key, new Asset(key, meta.getProperty("sourceName", key), e.getValue()));
            }
        }
        if (p.getTiles().isEmpty()) p.getTiles().putAll(GameProject.createDefault().getTiles());
        return p;
    }

    private static void putProperties(ZipOutputStream zip, String path, Properties p) throws IOException {
        ZipEntry entry = new ZipEntry(path);
        zip.putNextEntry(entry);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "2gameRL");
        zip.write(out.toByteArray());
        zip.closeEntry();
    }
    private static Properties props(byte[] data) throws IOException {
        Properties p = new Properties();
        if (data != null) p.load(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8));
        return p;
    }
    private static int integer(Properties p, String key, int def) {
        try { return Integer.parseInt(p.getProperty(key, Integer.toString(def))); }
        catch (NumberFormatException e) { return def; }
    }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String fileStem(String path) {
        String n = path.substring(path.lastIndexOf('/') + 1);
        int dot = n.lastIndexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }
}
