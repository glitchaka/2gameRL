package com.buttclapdev.twogamerl.model;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

public final class GameProject {
    public static final int FORMAT_VERSION = 2;

    private String title = "2gameRL";
    private String startLevel = "level-1";
    private String startMenu = "main";
    private int tileSize = 32;
    private final LinkedHashMap<Integer, TileDef> tiles = new LinkedHashMap<>();
    private final LinkedHashMap<String, Level> levels = new LinkedHashMap<>();
    private final LinkedHashMap<String, MenuScreen> menus = new LinkedHashMap<>();
    private final LinkedHashMap<String, Asset> assets = new LinkedHashMap<>();

    public GameProject() {}

    public static GameProject createDefault() {
        GameProject p = new GameProject();
        p.tiles.put(0, new TileDef(0, "Suelo", new Color(42, 48, 58), true, ""));
        p.tiles.put(1, new TileDef(1, "Muro", new Color(80, 88, 104), false, ""));
        p.assets.put("placeholder-player.png", new Asset("placeholder-player.png", "placeholder-player.png", placeholderPlayerPng()));

        Level level = new Level("level-1", "Nivel 1", 24, 16);
        for (int y = 0; y < level.height; y++) {
            for (int x = 0; x < level.width; x++) {
                level.set(x, y, x == 0 || y == 0 || x == level.width - 1 || y == level.height - 1 ? 1 : 0);
            }
        }
        level.spawnX = 2;
        level.spawnY = 2;
        EntityDef player = new EntityDef("player", "Jugador", 2, 2);
        player.assetKey = "placeholder-player.png";
        player.components.add(ComponentDef.preset("PlayerController"));
        player.components.add(ComponentDef.preset("Rigidbody2D"));
        player.components.add(ComponentDef.preset("BoxCollider2D"));
        player.script = "# Los scripts se ejecutan realmente en el juego.\n" +
                "on start\n" +
                "  log \"Jugador iniciado\"\n" +
                "end\n\n" +
                "on doubleClick\n" +
                "  log \"Doble clic sobre el jugador\"\n" +
                "end\n";
        level.entities.add(player);
        p.levels.put(level.id, level);

        MenuScreen menu = new MenuScreen("main", "2gameRL");
        menu.buttons.add(new MenuButton("Jugar", 220, 190, 200, 48, MenuAction.START_GAME, ""));
        menu.buttons.add(new MenuButton("Salir", 220, 250, 200, 48, MenuAction.EXIT, ""));
        p.menus.put(menu.id, menu);
        return p;
    }

    private static byte[] placeholderPlayerPng() {
        try {
            int[][] px = {
                    {0,0,0,1,1,1,1,0,0,0},
                    {0,0,1,2,2,2,2,1,0,0},
                    {0,1,2,2,2,2,2,2,1,0},
                    {0,1,2,3,2,2,3,2,1,0},
                    {0,1,2,2,2,2,2,2,1,0},
                    {0,0,1,2,3,3,2,1,0,0},
                    {0,1,1,4,4,4,4,1,1,0},
                    {1,4,4,4,4,4,4,4,4,1},
                    {0,0,1,4,1,1,4,1,0,0},
                    {0,0,1,1,0,0,1,1,0,0}
            };
            int[] colors = {0x00000000, 0xFF172033, 0xFFF2C7A5, 0xFF34445F, 0xFF4FA8FF};
            BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < 10; y++) for (int x = 0; x < 10; x++) image.setRGB(x, y, colors[px[y][x]]);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public GameProject deepCopy() {
        GameProject c = new GameProject();
        c.title = title;
        c.startLevel = startLevel;
        c.startMenu = startMenu;
        c.tileSize = tileSize;
        tiles.forEach((k, v) -> c.tiles.put(k, v.copy()));
        levels.forEach((k, v) -> c.levels.put(k, v.copy()));
        menus.forEach((k, v) -> c.menus.put(k, v.copy()));
        assets.forEach((k, v) -> c.assets.put(k, v.copy()));
        return c;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null || title.isBlank() ? "2gameRL" : title.trim(); }
    public String getStartLevel() { return startLevel; }
    public void setStartLevel(String startLevel) { this.startLevel = startLevel; }
    public String getStartMenu() { return startMenu; }
    public void setStartMenu(String startMenu) { this.startMenu = startMenu; }
    public int getTileSize() { return tileSize; }
    public void setTileSize(int tileSize) { this.tileSize = Math.max(8, Math.min(128, tileSize)); }
    public Map<Integer, TileDef> getTiles() { return tiles; }
    public Map<String, Level> getLevels() { return levels; }
    public Map<String, MenuScreen> getMenus() { return menus; }
    public Map<String, Asset> getAssets() { return assets; }
    public int nextTileId() { return tiles.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1; }

    public static final class TileDef {
        public final int id;
        public String name;
        public Color color;
        public boolean walkable;
        public String assetKey;
        public TileDef(int id, String name, Color color, boolean walkable, String assetKey) {
            this.id = id; this.name = name; this.color = color; this.walkable = walkable; this.assetKey = assetKey == null ? "" : assetKey;
        }
        public TileDef copy() { return new TileDef(id, name, color, walkable, assetKey); }
        @Override public String toString() { return id + " · " + name; }
    }

    public static final class Asset {
        public final String key;
        public String sourceName;
        public byte[] data;
        private transient BufferedImage image;
        public Asset(String key, String sourceName, byte[] data) { this.key = key; this.sourceName = sourceName; this.data = data; }
        public BufferedImage image() {
            if (image == null && data != null) {
                try { image = ImageIO.read(new ByteArrayInputStream(data)); } catch (Exception ignored) { image = null; }
            }
            return image;
        }
        public Asset copy() { return new Asset(key, sourceName, data == null ? null : data.clone()); }
        @Override public String toString() { return sourceName == null ? key : sourceName; }
    }

    public static final class Level {
        public final String id;
        public String name;
        public int width;
        public int height;
        public int spawnX;
        public int spawnY;
        private int[] cells;
        public final List<EntityDef> entities = new ArrayList<>();

        public Level(String id, String name, int width, int height) {
            this.id = id; this.name = name; this.width = Math.max(4, width); this.height = Math.max(4, height);
            this.cells = new int[this.width * this.height];
        }
        public int get(int x, int y) { return cells[y * width + x]; }
        public void set(int x, int y, int value) { if (x >= 0 && y >= 0 && x < width && y < height) cells[y * width + x] = value; }
        public int[] cells() { return cells; }
        public void replaceCells(int[] values) { if (values != null && values.length == width * height) cells = values.clone(); }
        public void resize(int newWidth, int newHeight) {
            newWidth = Math.max(4, Math.min(256, newWidth)); newHeight = Math.max(4, Math.min(256, newHeight));
            int[] next = new int[newWidth * newHeight];
            int copyW = Math.min(width, newWidth), copyH = Math.min(height, newHeight);
            for (int y = 0; y < copyH; y++) System.arraycopy(cells, y * width, next, y * newWidth, copyW);
            width = newWidth; height = newHeight; cells = next;
            spawnX = Math.max(0, Math.min(width - 1, spawnX)); spawnY = Math.max(0, Math.min(height - 1, spawnY));
            for (EntityDef e : entities) { e.x = Math.max(0, Math.min(width - 1, e.x)); e.y = Math.max(0, Math.min(height - 1, e.y)); }
        }
        public EntityDef entity(String id) { return entities.stream().filter(e -> e.id.equals(id)).findFirst().orElse(null); }
        public Level copy() {
            Level c = new Level(id, name, width, height); c.spawnX = spawnX; c.spawnY = spawnY; c.cells = cells.clone();
            entities.forEach(e -> c.entities.add(e.copy())); return c;
        }
        @Override public String toString() { return name; }
    }

    public static final class EntityDef {
        public final String id;
        public String name;
        public double x, y;
        public double width = 0.82, height = 0.82;
        public boolean enabled = true;
        public int layer = 0;
        public String assetKey = "";
        public String script = "# Doble clic sobre una entidad para editar su script.\n";
        public final List<ComponentDef> components = new ArrayList<>();
        public final LinkedHashMap<String, String> variables = new LinkedHashMap<>();

        public EntityDef(String id, String name, double x, double y) { this.id = id; this.name = name; this.x = x; this.y = y; }
        public ComponentDef component(String type) { return components.stream().filter(c -> c.type.equals(type)).findFirst().orElse(null); }
        public boolean has(String type) { return component(type) != null; }
        public EntityDef copy() {
            EntityDef c = new EntityDef(id, name, x, y); c.width = width; c.height = height; c.enabled = enabled; c.layer = layer;
            c.assetKey = assetKey; c.script = script; components.forEach(v -> c.components.add(v.copy())); c.variables.putAll(variables); return c;
        }
        @Override public String toString() { return name; }
    }

    public static final class ComponentDef {
        public final String type;
        public final LinkedHashMap<String, String> properties = new LinkedHashMap<>();
        public ComponentDef(String type) { this.type = type; }
        public ComponentDef(String type, Map<String, String> defaults) { this.type = type; this.properties.putAll(defaults); }
        public ComponentDef copy() { return new ComponentDef(type, properties); }
        public String get(String key, String def) { return properties.getOrDefault(key, def); }
        public double number(String key, double def) { try { return Double.parseDouble(get(key, Double.toString(def))); } catch (NumberFormatException e) { return def; } }
        public boolean bool(String key, boolean def) { return Boolean.parseBoolean(get(key, Boolean.toString(def))); }
        public static ComponentDef preset(String type) {
            LinkedHashMap<String,String> p = new LinkedHashMap<>();
            switch (type) {
                case "Rigidbody2D" -> { p.put("mass", "1"); p.put("gravityScale", "0"); p.put("drag", "8"); p.put("maxSpeed", "8"); }
                case "BoxCollider2D" -> { p.put("width", "0.82"); p.put("height", "0.82"); p.put("solid", "true"); }
                case "PlayerController" -> { p.put("speed", "4"); p.put("allowArrows", "true"); }
                case "Patrol" -> { p.put("axis", "x"); p.put("distance", "4"); p.put("speed", "1.5"); }
                case "ScenePortal" -> { p.put("targetScene", "level-1"); p.put("targetX", "2"); p.put("targetY", "2"); }
                case "Trigger" -> { p.put("tag", "trigger"); p.put("once", "false"); }
                case "Health" -> { p.put("max", "100"); p.put("current", "100"); }
                case "DamageOnContact" -> p.put("damage", "10");
                case "Clickable" -> p.put("enabled", "true");
                default -> {}
            }
            return new ComponentDef(type, p);
        }
        @Override public String toString() { return type; }
    }

    public static final List<String> BUILTIN_COMPONENTS = List.of(
            "Rigidbody2D", "BoxCollider2D", "PlayerController", "Patrol", "ScenePortal", "Trigger", "Health", "DamageOnContact", "Clickable"
    );

    public enum MenuAction { START_GAME, OPEN_MENU, EXIT }
    public static final class MenuButton {
        public String text; public int x, y, width, height; public MenuAction action; public String target;
        public MenuButton(String text, int x, int y, int width, int height, MenuAction action, String target) {
            this.text = text; this.x = x; this.y = y; this.width = width; this.height = height; this.action = action; this.target = target == null ? "" : target;
        }
        public MenuButton copy() { return new MenuButton(text, x, y, width, height, action, target); }
        @Override public String toString() { return text; }
    }
    public static final class MenuScreen {
        public final String id; public String title; public Color background = new Color(19, 24, 34); public final List<MenuButton> buttons = new ArrayList<>();
        public MenuScreen(String id, String title) { this.id = id; this.title = title; }
        public MenuScreen copy() { MenuScreen c = new MenuScreen(id, title); c.background = background; buttons.forEach(b -> c.buttons.add(b.copy())); return c; }
        @Override public String toString() { return title; }
    }
}
