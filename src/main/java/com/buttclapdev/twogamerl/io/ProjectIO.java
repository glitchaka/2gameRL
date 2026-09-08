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
            writeLayerConfig(zip, project);

            Properties tiles = new Properties();
            tiles.setProperty("count", Integer.toString(project.getTiles().size()));
            int ti = 0;
            for (TileDef t : project.getTiles().values()) {
                String k = "tile." + ti++ + ".";
                tiles.setProperty(k + "id", Integer.toString(t.id));
                tiles.setProperty(k + "name", safe(t.name));
                tiles.setProperty(k + "rgb", Integer.toString(t.color.getRGB()));
                tiles.setProperty(k + "walkable", Boolean.toString(t.walkable));
                tiles.setProperty(k + "asset", safe(t.assetKey));
            }
            putProperties(zip, "tiles.properties", tiles);

            for (Level level : project.getLevels().values()) writeLevel(zip, level);
            for (MenuScreen menu : project.getMenus().values()) writeMenu(zip, menu);

            for (Asset asset : project.getAssets().values()) {
                Properties meta = new Properties();
                meta.setProperty("sourceName", safe(asset.sourceName));
                meta.setProperty("sourceOnly", Boolean.toString(asset.sourceOnly));
                meta.setProperty("region", Boolean.toString(asset.isRegion()));
                meta.setProperty("sourceAsset", safe(asset.sourceAssetKey));
                meta.setProperty("regionX", Integer.toString(asset.regionX));
                meta.setProperty("regionY", Integer.toString(asset.regionY));
                meta.setProperty("regionWidth", Integer.toString(asset.regionWidth));
                meta.setProperty("regionHeight", Integer.toString(asset.regionHeight));
                putProperties(zip, "asset-meta/" + asset.key + ".properties", meta);
                if (asset.data != null) putBytes(zip, "assets/" + asset.key, asset.data);
            }
        }
    }

    private static void writeLayerConfig(ZipOutputStream zip, GameProject project) throws IOException {
        Properties p = new Properties();
        p.setProperty("renderCount", Integer.toString(project.getRenderLayers().size()));
        for (int i = 0; i < project.getRenderLayers().size(); i++) p.setProperty("render." + i, project.getRenderLayers().get(i));
        p.setProperty("physicsCount", Integer.toString(project.getPhysicsLayers().size()));
        int i = 0;
        for (PhysicsLayerDef layer : project.getPhysicsLayers().values()) {
            String k = "physics." + i++ + ".";
            p.setProperty(k + "name", layer.name);
            p.setProperty(k + "collisionCount", Integer.toString(layer.collidesWith.size()));
            int c = 0;
            for (String target : layer.collidesWith) p.setProperty(k + "collision." + c++, target);
        }
        putProperties(zip, "layers.properties", p);
    }

    private static void writeLevel(ZipOutputStream zip, Level level) throws IOException {
        Properties p = new Properties();
        p.setProperty("id", level.id);
        p.setProperty("name", safe(level.name));
        p.setProperty("width", Integer.toString(level.width));
        p.setProperty("height", Integer.toString(level.height));
        p.setProperty("tileLayerCount", Integer.toString(level.tileLayers.size()));
        for (int i = 0; i < level.tileLayers.size(); i++) {
            TileLayer layer = level.tileLayers.get(i);
            String k = "tileLayer." + i + ".";
            p.setProperty(k + "id", layer.id);
            p.setProperty(k + "name", safe(layer.name));
            p.setProperty(k + "visible", Boolean.toString(layer.visible));
            p.setProperty(k + "locked", Boolean.toString(layer.locked));
            p.setProperty(k + "collision", Boolean.toString(layer.collision));
            p.setProperty(k + "physicsLayer", safe(layer.physicsLayer));
            p.setProperty(k + "order", Integer.toString(layer.order));
            StringJoiner cells = new StringJoiner(",");
            for (int cell : layer.cells()) cells.add(Integer.toString(cell));
            p.setProperty(k + "cells", cells.toString());
        }
        p.setProperty("entityCount", Integer.toString(level.entities.size()));
        for (int i = 0; i < level.entities.size(); i++) writeEntity(zip, level, p, i, level.entities.get(i));
        putProperties(zip, "levels/" + level.id + ".properties", p);
    }

    private static void writeEntity(ZipOutputStream zip, Level level, Properties p, int i, EntityDef e) throws IOException {
        String k = "entity." + i + ".";
        p.setProperty(k + "id", e.id);
        p.setProperty(k + "name", safe(e.name));
        p.setProperty(k + "x", Double.toString(e.x));
        p.setProperty(k + "y", Double.toString(e.y));
        p.setProperty(k + "width", Double.toString(e.width));
        p.setProperty(k + "height", Double.toString(e.height));
        p.setProperty(k + "enabled", Boolean.toString(e.enabled));
        p.setProperty(k + "layer", Integer.toString(e.layer));
        p.setProperty(k + "group", safe(e.group));
        p.setProperty(k + "renderLayer", safe(e.renderLayer));
        p.setProperty(k + "physicsLayer", safe(e.physicsLayer));
        p.setProperty(k + "asset", safe(e.assetKey));
        p.setProperty(k + "componentCount", Integer.toString(e.components.size()));
        for (int c = 0; c < e.components.size(); c++) {
            ComponentDef component = e.components.get(c);
            String ck = k + "component." + c + ".";
            p.setProperty(ck + "type", component.type);
            p.setProperty(ck + "propertyCount", Integer.toString(component.properties.size()));
            int pi = 0;
            for (var prop : component.properties.entrySet()) {
                p.setProperty(ck + "property." + pi + ".name", prop.getKey());
                p.setProperty(ck + "property." + pi + ".value", prop.getValue());
                pi++;
            }
        }
        p.setProperty(k + "variableCount", Integer.toString(e.variables.size()));
        int vi = 0;
        for (var v : e.variables.entrySet()) {
            p.setProperty(k + "variable." + vi + ".name", v.getKey());
            p.setProperty(k + "variable." + vi + ".value", v.getValue());
            vi++;
        }
        putBytes(zip, "scripts/" + level.id + "/" + e.id + ".2gs", safe(e.script).getBytes(StandardCharsets.UTF_8));
    }

    private static void writeMenu(ZipOutputStream zip, MenuScreen menu) throws IOException {
        Properties p = new Properties();
        p.setProperty("id", menu.id);
        p.setProperty("title", safe(menu.title));
        p.setProperty("background", Integer.toString(menu.background.getRGB()));
        p.setProperty("backgroundAsset", safe(menu.backgroundAssetKey));
        p.setProperty("canvasWidth", Integer.toString(menu.canvasWidth));
        p.setProperty("canvasHeight", Integer.toString(menu.canvasHeight));
        p.setProperty("titleX", Integer.toString(menu.titleX));
        p.setProperty("titleY", Integer.toString(menu.titleY));
        p.setProperty("titleWidth", Integer.toString(menu.titleWidth));
        p.setProperty("titleHeight", Integer.toString(menu.titleHeight));
        p.setProperty("titleFontSize", Integer.toString(menu.titleFontSize));
        p.setProperty("titleColor", Integer.toString(menu.titleColor.getRGB()));
        p.setProperty("titleAsset", safe(menu.titleAssetKey));
        p.setProperty("titleAnimation", menu.titleAnimation.name());
        p.setProperty("titleAnimationSpeed", Double.toString(menu.titleAnimationSpeed));
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
            p.setProperty(k + "asset", safe(b.assetKey));
            p.setProperty(k + "hoverAsset", safe(b.hoverAssetKey));
            p.setProperty(k + "fontSize", Integer.toString(b.fontSize));
            p.setProperty(k + "textColor", Integer.toString(b.textColor.getRGB()));
            p.setProperty(k + "backgroundColor", Integer.toString(b.backgroundColor.getRGB()));
            p.setProperty(k + "animation", b.animation.name());
            p.setProperty(k + "hoverEffect", b.hoverEffect.name());
            p.setProperty(k + "animationSpeed", Double.toString(b.animationSpeed));
        }
        putProperties(zip, "menus/" + menu.id + ".properties", p);
    }

    public static GameProject load(Path file) throws IOException { try (InputStream in = Files.newInputStream(file)) { return load(in); } }

    public static GameProject load(InputStream input) throws IOException {
        GameProject project = new GameProject();
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory()) entries.put(entry.getName(), zip.readAllBytes());
                zip.closeEntry();
            }
        }
        Properties root = props(entries.get("project.properties"));
        if (root.isEmpty()) throw new IOException("El archivo no contiene un proyecto 2gameRL válido.");
        int version = integer(root, "formatVersion", 1);
        project.setTitle(root.getProperty("title", "2gameRL"));
        project.setStartLevel(root.getProperty("startLevel", ""));
        project.setStartMenu(root.getProperty("startMenu", ""));
        project.setTileSize(integer(root, "tileSize", 32));
        readLayerConfig(project, entries.get("layers.properties"));

        Properties tiles = props(entries.get("tiles.properties"));
        int tileCount = integer(tiles, "count", 0);
        for (int i = 0; i < tileCount; i++) {
            String k = "tile." + i + ".";
            int id = integer(tiles, k + "id", i);
            project.getTiles().put(id, new TileDef(id,
                    tiles.getProperty(k + "name", "Tile " + id),
                    new Color(integer(tiles, k + "rgb", Color.GRAY.getRGB()), true),
                    Boolean.parseBoolean(tiles.getProperty(k + "walkable", "true")),
                    tiles.getProperty(k + "asset", "")));
        }

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (name.startsWith("levels/") && name.endsWith(".properties")) readLevel(project, entries, entry.getValue(), name, version);
            else if (name.startsWith("menus/") && name.endsWith(".properties")) readMenu(project, entry.getValue(), name);
            else if (name.startsWith("assets/") && !name.endsWith("/")) {
                String key = name.substring("assets/".length());
                Properties meta = props(entries.get("asset-meta/" + key + ".properties"));
                Asset asset = new Asset(key, meta.getProperty("sourceName", key), entry.getValue(), Boolean.parseBoolean(meta.getProperty("sourceOnly", "false")));
                project.getAssets().put(key, asset);
            }
        }

        for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("asset-meta/") || !name.endsWith(".properties")) continue;
            String key = name.substring("asset-meta/".length(), name.length()-".properties".length());
            Properties meta = props(entry.getValue());
            boolean region = Boolean.parseBoolean(meta.getProperty("region", "false"));
            Asset existing = project.getAssets().get(key);
            if (region) {
                Asset virtual = new Asset(key, meta.getProperty("sourceName", key), meta.getProperty("sourceAsset", ""), integer(meta,"regionX",0), integer(meta,"regionY",0), integer(meta,"regionWidth",1), integer(meta,"regionHeight",1));
                virtual.sourceOnly = Boolean.parseBoolean(meta.getProperty("sourceOnly", "false"));
                project.getAssets().put(key, virtual);
            } else if (existing != null) existing.sourceOnly = Boolean.parseBoolean(meta.getProperty("sourceOnly", "false"));
        }

        if (project.getTiles().isEmpty()) project.getTiles().putAll(GameProject.createDefault().getTiles());
        if (version < 2) migrateV1(project);
        inferLegacyLayers(project, version);
        return project;
    }

    private static void readLayerConfig(GameProject project, byte[] data) throws IOException {
        if (data == null) return;
        Properties p = props(data);
        int renderCount = integer(p, "renderCount", 0);
        if (renderCount > 0) {
            project.getRenderLayers().clear();
            for (int i = 0; i < renderCount; i++) {
                String name = p.getProperty("render." + i, "").trim();
                if (!name.isBlank()) project.getRenderLayers().add(name);
            }
        }
        int physicsCount = integer(p, "physicsCount", 0);
        if (physicsCount > 0) {
            project.getPhysicsLayers().clear();
            for (int i = 0; i < physicsCount; i++) {
                String k = "physics." + i + ".";
                String name = p.getProperty(k + "name", "Layer " + (i + 1));
                PhysicsLayerDef layer = new PhysicsLayerDef(name);
                int collisions = integer(p, k + "collisionCount", 0);
                for (int c = 0; c < collisions; c++) layer.collidesWith.add(p.getProperty(k + "collision." + c, ""));
                project.getPhysicsLayers().put(name, layer);
            }
        }
    }

    private static void readLevel(GameProject project, Map<String, byte[]> entries, byte[] data, String path, int version) throws IOException {
        Properties p = props(data);
        String id = p.getProperty("id", fileStem(path));
        Level level = new Level(id, p.getProperty("name", id), integer(p, "width", 20), integer(p, "height", 15));
        level.tileLayers.clear();
        int layerCount = integer(p, "tileLayerCount", 0);
        if (layerCount > 0) {
            for (int i = 0; i < layerCount; i++) {
                String k = "tileLayer." + i + ".";
                TileLayer layer = new TileLayer(p.getProperty(k + "id", "layer-" + (i + 1)), p.getProperty(k + "name", "Capa " + (i + 1)), level.width, level.height, integer(p, k + "order", i));
                layer.visible = Boolean.parseBoolean(p.getProperty(k + "visible", "true"));
                layer.locked = Boolean.parseBoolean(p.getProperty(k + "locked", "false"));
                layer.collision = Boolean.parseBoolean(p.getProperty(k + "collision", "true"));
                layer.physicsLayer = p.getProperty(k + "physicsLayer", "World");
                layer.replaceCells(parseCells(p.getProperty(k + "cells", ""), level.width * level.height));
                level.tileLayers.add(layer);
            }
        } else {
            TileLayer base = new TileLayer("base", "Suelo", level.width, level.height, 0);
            base.replaceCells(parseCells(p.getProperty("cells", ""), level.width * level.height));
            level.tileLayers.add(base);
        }

        int entityCount = integer(p, "entityCount", 0);
        for (int i = 0; i < entityCount; i++) level.entities.add(readEntity(entries, level.id, p, i));
        if (version < 2 && level.entities.isEmpty()) level.entities.add(legacyPlayer(integer(p,"spawnX",2), integer(p,"spawnY",2)));
        project.getLevels().put(id, level);
    }

    private static EntityDef readEntity(Map<String, byte[]> entries, String levelId, Properties p, int i) {
        String k = "entity." + i + ".";
        String id = p.getProperty(k + "id", "entity-" + (i + 1));
        EntityDef e = new EntityDef(id, p.getProperty(k + "name", id), decimal(p, k + "x", 1), decimal(p, k + "y", 1));
        e.width = decimal(p, k + "width", .82); e.height = decimal(p, k + "height", .82);
        e.enabled = Boolean.parseBoolean(p.getProperty(k + "enabled", "true"));
        e.layer = integer(p, k + "layer", 0); e.group = p.getProperty(k + "group", "");
        e.renderLayer = p.getProperty(k + "renderLayer", "Objetos"); e.physicsLayer = p.getProperty(k + "physicsLayer", "Default"); e.assetKey = p.getProperty(k + "asset", "");
        int componentCount = integer(p, k + "componentCount", 0);
        for (int c = 0; c < componentCount; c++) {
            String ck = k + "component." + c + ".";
            ComponentDef component = new ComponentDef(p.getProperty(ck + "type", "Component"));
            int pc = integer(p, ck + "propertyCount", 0);
            for (int pi = 0; pi < pc; pi++) component.properties.put(p.getProperty(ck + "property." + pi + ".name", "property" + pi), p.getProperty(ck + "property." + pi + ".value", ""));
            if (!component.properties.containsKey("enabled")) component.properties.put("enabled", "true");
            e.components.add(component);
        }
        int variableCount = integer(p, k + "variableCount", 0);
        for (int vi = 0; vi < variableCount; vi++) e.variables.put(p.getProperty(k + "variable." + vi + ".name", "var" + vi), p.getProperty(k + "variable." + vi + ".value", ""));
        byte[] script = entries.get("scripts/" + levelId + "/" + id + ".2gs");
        e.script = script == null ? "# Script de " + e.name + "\n" : new String(script, StandardCharsets.UTF_8);
        return e;
    }

    private static void inferLegacyLayers(GameProject project, int version) {
        if (version >= 4) return;
        for (Level level : project.getLevels().values()) for (EntityDef e : level.entities) {
            if (e.has("PlayerController") || e.has("GridMovement")) { e.renderLayer = "Personajes"; e.physicsLayer = "Player"; }
            else if (e.has("Trigger") || e.has("ScenePortal")) e.physicsLayer = "Trigger";
            else if (e.has("Patrol") || e.has("DamageOnContact")) { e.renderLayer = "Personajes"; e.physicsLayer = "Enemy"; }
        }
    }

    private static EntityDef legacyPlayer(double x, double y) {
        EntityDef player = new EntityDef("player", "Jugador", x, y);
        player.assetKey = "placeholder-player.png"; player.renderLayer = "Personajes"; player.physicsLayer = "Player";
        player.components.add(ComponentDef.preset("PlayerController"));
        ComponentDef body = ComponentDef.preset("Rigidbody2D"); body.properties.put("gravityScale", "0"); player.components.add(body);
        player.components.add(ComponentDef.preset("BoxCollider2D")); return player;
    }

    private static void readMenu(GameProject project, byte[] data, String path) throws IOException {
        Properties p = props(data);
        String id = p.getProperty("id", fileStem(path));
        MenuScreen menu = new MenuScreen(id, p.getProperty("title", id));
        menu.background = new Color(integer(p, "background", new Color(19,24,34).getRGB()), true); menu.backgroundAssetKey = p.getProperty("backgroundAsset", "");
        menu.canvasWidth = integer(p, "canvasWidth", 640); menu.canvasHeight = integer(p, "canvasHeight", 480);
        menu.titleX = integer(p, "titleX", 30); menu.titleY = integer(p, "titleY", 30); menu.titleWidth = integer(p, "titleWidth", 360); menu.titleHeight = integer(p, "titleHeight", 80);
        menu.titleFontSize = integer(p, "titleFontSize", 34); menu.titleColor = new Color(integer(p, "titleColor", Color.WHITE.getRGB()), true); menu.titleAssetKey = p.getProperty("titleAsset", "");
        menu.titleAnimation = enumValue(MenuAnimation.class, p.getProperty("titleAnimation"), MenuAnimation.NONE); menu.titleAnimationSpeed = decimal(p, "titleAnimationSpeed", 1);
        int count = integer(p, "buttonCount", 0);
        for (int i = 0; i < count; i++) {
            String k = "button." + i + ".";
            MenuButton b = new MenuButton(p.getProperty(k + "text", "Botón"), integer(p,k+"x",100), integer(p,k+"y",100), integer(p,k+"width",180), integer(p,k+"height",44), enumValue(MenuAction.class,p.getProperty(k+"action"),MenuAction.START_GAME), p.getProperty(k+"target",""));
            b.assetKey = p.getProperty(k+"asset",""); b.hoverAssetKey = p.getProperty(k+"hoverAsset",""); b.fontSize = integer(p,k+"fontSize",16);
            b.textColor = new Color(integer(p,k+"textColor",Color.WHITE.getRGB()),true); b.backgroundColor = new Color(integer(p,k+"backgroundColor",new Color(31,115,170).getRGB()),true);
            b.animation = enumValue(MenuAnimation.class,p.getProperty(k+"animation"),MenuAnimation.NONE); b.hoverEffect = enumValue(MenuHoverEffect.class,p.getProperty(k+"hoverEffect"),MenuHoverEffect.SCALE); b.animationSpeed = decimal(p,k+"animationSpeed",1);
            menu.buttons.add(b);
        }
        project.getMenus().put(id, menu);
    }

    private static void migrateV1(GameProject project) {
        Asset placeholder = GameProject.createDefault().getAssets().get("placeholder-player.png");
        if (placeholder != null && !project.getAssets().containsKey(placeholder.key)) project.getAssets().put(placeholder.key, placeholder.copy());
    }

    private static int[] parseCells(String raw, int size) {
        String[] values = raw == null ? new String[0] : raw.split(","); int[] result = new int[size];
        for (int i = 0; i < Math.min(size, values.length); i++) try { result[i] = Integer.parseInt(values[i]); } catch (NumberFormatException ignored) {}
        return result;
    }

    private static void putProperties(ZipOutputStream zip, String path, Properties p) throws IOException {ByteArrayOutputStream out = new ByteArrayOutputStream();p.store(new OutputStreamWriter(out, StandardCharsets.UTF_8), "2gameRL");putBytes(zip, path, out.toByteArray());}
    private static void putBytes(ZipOutputStream zip, String path, byte[] data) throws IOException {ZipEntry entry = new ZipEntry(path);zip.putNextEntry(entry);zip.write(data);zip.closeEntry();}
    private static Properties props(byte[] data) throws IOException {Properties p = new Properties();if (data != null) p.load(new InputStreamReader(new ByteArrayInputStream(data), StandardCharsets.UTF_8));return p;}
    private static int integer(Properties p, String key, int def) {try{return Integer.parseInt(p.getProperty(key, Integer.toString(def)));}catch(NumberFormatException e){return def;}}
    private static double decimal(Properties p, String key, double def) {try{return Double.parseDouble(p.getProperty(key, Double.toString(def)));}catch(NumberFormatException e){return def;}}
    private static String safe(String value){return value==null?"":value;}
    private static String fileStem(String path){String n=path.substring(path.lastIndexOf('/')+1);int dot=n.lastIndexOf('.');return dot>0?n.substring(0,dot):n;}
    private static <E extends Enum<E>> E enumValue(Class<E> type,String raw,E def){if(raw==null)return def;try{return Enum.valueOf(type,raw);}catch(IllegalArgumentException ex){return def;}}
}
