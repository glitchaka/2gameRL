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
                p.setProperty("entityCount", Integer.toString(level.entities.size()));
                for (int i = 0; i < level.entities.size(); i++) writeEntity(zip, level, p, i, level.entities.get(i));
                putProperties(zip, "levels/" + level.id + ".properties", p);
            }

            for (MenuScreen menu : project.getMenus().values()) {
                Properties p = new Properties();
                p.setProperty("id", menu.id);
                p.setProperty("title", safe(menu.title));
                p.setProperty("background", Integer.toString(menu.background.getRGB()));
                p.setProperty("buttonCount", Integer.toString(menu.buttons.size()));
                for (int i = 0; i < menu.buttons.size(); i++) {
                    MenuButton b = menu.buttons.get(i); String k = "button." + i + ".";
                    p.setProperty(k + "text", safe(b.text)); p.setProperty(k + "x", Integer.toString(b.x)); p.setProperty(k + "y", Integer.toString(b.y));
                    p.setProperty(k + "width", Integer.toString(b.width)); p.setProperty(k + "height", Integer.toString(b.height));
                    p.setProperty(k + "action", b.action.name()); p.setProperty(k + "target", safe(b.target));
                }
                putProperties(zip, "menus/" + menu.id + ".properties", p);
            }

            for (Asset asset : project.getAssets().values()) {
                if (asset.data == null) continue;
                putBytes(zip, "assets/" + asset.key, asset.data);
                Properties meta = new Properties(); meta.setProperty("sourceName", safe(asset.sourceName));
                putProperties(zip, "asset-meta/" + asset.key + ".properties", meta);
            }
        }
    }

    private static void writeEntity(ZipOutputStream zip, Level level, Properties p, int i, EntityDef e) throws IOException {
        String k = "entity." + i + ".";
        p.setProperty(k + "id", e.id); p.setProperty(k + "name", safe(e.name));
        p.setProperty(k + "x", Double.toString(e.x)); p.setProperty(k + "y", Double.toString(e.y));
        p.setProperty(k + "width", Double.toString(e.width)); p.setProperty(k + "height", Double.toString(e.height));
        p.setProperty(k + "enabled", Boolean.toString(e.enabled)); p.setProperty(k + "layer", Integer.toString(e.layer));
        p.setProperty(k + "asset", safe(e.assetKey));
        p.setProperty(k + "componentCount", Integer.toString(e.components.size()));
        for (int c = 0; c < e.components.size(); c++) {
            ComponentDef component = e.components.get(c); String ck = k + "component." + c + ".";
            p.setProperty(ck + "type", component.type); p.setProperty(ck + "propertyCount", Integer.toString(component.properties.size()));
            int pi = 0;
            for (var prop : component.properties.entrySet()) {
                p.setProperty(ck + "property." + pi + ".name", prop.getKey());
                p.setProperty(ck + "property." + pi + ".value", prop.getValue()); pi++;
            }
        }
        p.setProperty(k + "variableCount", Integer.toString(e.variables.size()));
        int vi = 0;
        for (var v : e.variables.entrySet()) {
            p.setProperty(k + "variable." + vi + ".name", v.getKey()); p.setProperty(k + "variable." + vi + ".value", v.getValue()); vi++;
        }
        putBytes(zip, "scripts/" + level.id + "/" + e.id + ".2gs", safe(e.script).getBytes(StandardCharsets.UTF_8));
    }

    public static GameProject load(Path file) throws IOException { try (InputStream in = Files.newInputStream(file)) { return load(in); } }

    public static GameProject load(InputStream input) throws IOException {
        GameProject p = new GameProject(); Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry e; while ((e = zip.getNextEntry()) != null) { if (!e.isDirectory()) entries.put(e.getName(), zip.readAllBytes()); zip.closeEntry(); }
        }
        Properties root = props(entries.get("project.properties"));
        if (root.isEmpty()) throw new IOException("El archivo no contiene un proyecto 2gameRL válido.");
        int version = integer(root, "formatVersion", 1);
        p.setTitle(root.getProperty("title", "2gameRL")); p.setStartLevel(root.getProperty("startLevel", "")); p.setStartMenu(root.getProperty("startMenu", ""));
        p.setTileSize(integer(root, "tileSize", 32));

        Properties tiles = props(entries.get("tiles.properties")); int tileCount = integer(tiles, "count", 0);
        for (int i = 0; i < tileCount; i++) {
            String k = "tile." + i + "."; int id = integer(tiles, k + "id", i);
            p.getTiles().put(id, new TileDef(id, tiles.getProperty(k + "name", "Tile " + id), new Color(integer(tiles, k + "rgb", Color.GRAY.getRGB()), true),
                    Boolean.parseBoolean(tiles.getProperty(k + "walkable", "true")), tiles.getProperty(k + "asset", "")));
        }

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("levels/") && name.endsWith(".properties")) readLevel(p, entries, entry.getValue(), name, version);
            else if (name.startsWith("menus/") && name.endsWith(".properties")) readMenu(p, entry.getValue(), name);
            else if (name.startsWith("assets/") && !name.endsWith("/")) {
                String key = name.substring("assets/".length()); Properties meta = props(entries.get("asset-meta/" + key + ".properties"));
                p.getAssets().put(key, new Asset(key, meta.getProperty("sourceName", key), entry.getValue()));
            }
        }
        if (p.getTiles().isEmpty()) p.getTiles().putAll(GameProject.createDefault().getTiles());
        if (version < 2) migrateV1(p);
        return p;
    }

    private static void readLevel(GameProject p, Map<String, byte[]> entries, byte[] data, String path, int version) throws IOException {
        Properties lp = props(data); String id = lp.getProperty("id", fileStem(path));
        Level level = new Level(id, lp.getProperty("name", id), integer(lp, "width", 20), integer(lp, "height", 15));
        level.spawnX = integer(lp, "spawnX", 1); level.spawnY = integer(lp, "spawnY", 1);
        String[] raw = lp.getProperty("cells", "").split(","); int[] cells = new int[level.width * level.height];
        for (int i = 0; i < Math.min(cells.length, raw.length); i++) try { cells[i] = Integer.parseInt(raw[i]); } catch (NumberFormatException ignored) {}
        level.replaceCells(cells);
        int entityCount = integer(lp, "entityCount", 0);
        for (int i = 0; i < entityCount; i++) level.entities.add(readEntity(entries, level.id, lp, i));
        p.getLevels().put(id, level);
    }

    private static EntityDef readEntity(Map<String, byte[]> entries, String levelId, Properties p, int i) {
        String k = "entity." + i + "."; String id = p.getProperty(k + "id", "entity-" + (i + 1));
        EntityDef e = new EntityDef(id, p.getProperty(k + "name", id), decimal(p, k + "x", 1), decimal(p, k + "y", 1));
        e.width = decimal(p, k + "width", .82); e.height = decimal(p, k + "height", .82); e.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled", "true"));
        e.layer = integer(p, k + "layer", 0); e.assetKey = p.getProperty(k + "asset", "");
        int componentCount = integer(p, k + "componentCount", 0);
        for (int c = 0; c < componentCount; c++) {
            String ck = k + "component." + c + "."; ComponentDef component = new ComponentDef(p.getProperty(ck + "type", "Component"));
            int pc = integer(p, ck + "propertyCount", 0);
            for (int pi = 0; pi < pc; pi++) component.properties.put(p.getProperty(ck + "property." + pi + ".name", "property" + pi), p.getProperty(ck + "property." + pi + ".value", ""));
            e.components.add(component);
        }
        int vc = integer(p, k + "variableCount", 0);
        for (int vi = 0; vi < vc; vi++) e.variables.put(p.getProperty(k + "variable." + vi + ".name", "var" + vi), p.getProperty(k + "variable." + vi + ".value", ""));
        byte[] script = entries.get("scripts/" + levelId + "/" + id + ".2gs"); e.script = script == null ? "# Script de " + e.name + "\n" : new String(script, StandardCharsets.UTF_8);
        return e;
    }

    private static void readMenu(GameProject p, byte[] data, String path) throws IOException {
        Properties mp = props(data); String id = mp.getProperty("id", fileStem(path)); MenuScreen menu = new MenuScreen(id, mp.getProperty("title", id));
        menu.background = new Color(integer(mp, "background", new Color(19,24,34).getRGB()), true); int count = integer(mp, "buttonCount", 0);
        for (int i = 0; i < count; i++) {
            String k = "button." + i + "."; MenuAction action;
            try { action = MenuAction.valueOf(mp.getProperty(k + "action", "START_GAME")); } catch (IllegalArgumentException ex) { action = MenuAction.START_GAME; }
            menu.buttons.add(new MenuButton(mp.getProperty(k + "text", "Botón"), integer(mp, k + "x", 100), integer(mp, k + "y", 100),
                    integer(mp, k + "width", 180), integer(mp, k + "height", 44), action, mp.getProperty(k + "target", "")));
        }
        p.getMenus().put(id, menu);
    }

    private static void migrateV1(GameProject p) {
        Asset placeholder = GameProject.createDefault().getAssets().get("placeholder-player.png");
        if (placeholder != null && !p.getAssets().containsKey(placeholder.key)) p.getAssets().put(placeholder.key, placeholder.copy());
        for (Level level : p.getLevels().values()) if (level.entities.isEmpty()) {
            EntityDef player = new EntityDef("player", "Jugador", level.spawnX, level.spawnY); player.assetKey = "placeholder-player.png";
            player.components.add(ComponentDef.preset("PlayerController")); player.components.add(ComponentDef.preset("Rigidbody2D")); player.components.add(ComponentDef.preset("BoxCollider2D"));
            player.script = "# Proyecto migrado desde 2gameRL v1\non start\n  log \"Jugador iniciado\"\nend\n"; level.entities.add(player);
        }
    }

    private static void putProperties(ZipOutputStream zip, String path, Properties p) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "2gameRL"); putBytes(zip, path, out.toByteArray());
    }
    private static void putBytes(ZipOutputStream zip, String path, byte[] data) throws IOException { ZipEntry entry = new ZipEntry(path); zip.putNextEntry(entry); zip.write(data); zip.closeEntry(); }
    private static Properties props(byte[] data) throws IOException { Properties p = new Properties(); if (data != null) p.load(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8)); return p; }
    private static int integer(Properties p, String key, int def) { try { return Integer.parseInt(p.getProperty(key, Integer.toString(def))); } catch (NumberFormatException e) { return def; } }
    private static double decimal(Properties p, String key, double def) { try { return Double.parseDouble(p.getProperty(key, Double.toString(def))); } catch (NumberFormatException e) { return def; } }
    private static String safe(String s) { return s == null ? "" : s; }
    private static String fileStem(String path) { String n = path.substring(path.lastIndexOf('/') + 1); int dot = n.lastIndexOf('.'); return dot > 0 ? n.substring(0, dot) : n; }
}
