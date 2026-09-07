package com.buttclapdev.twogamerl.model;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;
import javax.imageio.ImageIO;

public final class GameProject {
    public static final int FORMAT_VERSION = 1;

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
        p.tiles.put(0, new TileDef(0, "Suelo", new Color(54, 61, 70), true, ""));
        p.tiles.put(1, new TileDef(1, "Muro", new Color(98, 87, 78), false, ""));
        Level level = new Level("level-1", "Nivel 1", 24, 16);
        for (int y = 0; y < level.height; y++) {
            for (int x = 0; x < level.width; x++) {
                level.set(x, y, x == 0 || y == 0 || x == level.width - 1 || y == level.height - 1 ? 1 : 0);
            }
        }
        level.spawnX = 2;
        level.spawnY = 2;
        p.levels.put(level.id, level);
        MenuScreen menu = new MenuScreen("main", "Menú principal");
        menu.buttons.add(new MenuButton("Jugar", 220, 190, 200, 48, MenuAction.START_GAME, ""));
        menu.buttons.add(new MenuButton("Salir", 220, 250, 200, 48, MenuAction.EXIT, ""));
        p.menus.put(menu.id, menu);
        return p;
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

    public int nextTileId() {
        return tiles.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1) + 1;
    }

    public static final class TileDef {
        public final int id;
        public String name;
        public Color color;
        public boolean walkable;
        public String assetKey;

        public TileDef(int id, String name, Color color, boolean walkable, String assetKey) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.walkable = walkable;
            this.assetKey = assetKey == null ? "" : assetKey;
        }
        public TileDef copy() { return new TileDef(id, name, color, walkable, assetKey); }
        @Override public String toString() { return id + " · " + name; }
    }

    public static final class Asset {
        public final String key;
        public String sourceName;
        public byte[] data;
        private transient BufferedImage image;

        public Asset(String key, String sourceName, byte[] data) {
            this.key = key;
            this.sourceName = sourceName;
            this.data = data;
        }
        public BufferedImage image() {
            if (image == null && data != null) {
                try { image = ImageIO.read(new ByteArrayInputStream(data)); }
                catch (Exception ignored) { image = null; }
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

        public Level(String id, String name, int width, int height) {
            this.id = id;
            this.name = name;
            this.width = Math.max(4, width);
            this.height = Math.max(4, height);
            this.cells = new int[this.width * this.height];
        }
        public int get(int x, int y) { return cells[y * width + x]; }
        public void set(int x, int y, int value) {
            if (x >= 0 && y >= 0 && x < width && y < height) cells[y * width + x] = value;
        }
        public int[] cells() { return cells; }
        public void replaceCells(int[] values) {
            if (values != null && values.length == width * height) cells = values.clone();
        }
        public void resize(int newWidth, int newHeight) {
            newWidth = Math.max(4, Math.min(256, newWidth));
            newHeight = Math.max(4, Math.min(256, newHeight));
            int[] next = new int[newWidth * newHeight];
            int copyW = Math.min(width, newWidth);
            int copyH = Math.min(height, newHeight);
            for (int y = 0; y < copyH; y++) System.arraycopy(cells, y * width, next, y * newWidth, copyW);
            width = newWidth;
            height = newHeight;
            cells = next;
            spawnX = Math.max(0, Math.min(width - 1, spawnX));
            spawnY = Math.max(0, Math.min(height - 1, spawnY));
        }
        public Level copy() {
            Level c = new Level(id, name, width, height);
            c.spawnX = spawnX;
            c.spawnY = spawnY;
            c.cells = cells.clone();
            return c;
        }
        @Override public String toString() { return name; }
    }

    public enum MenuAction { START_GAME, OPEN_MENU, EXIT }

    public static final class MenuButton {
        public String text;
        public int x, y, width, height;
        public MenuAction action;
        public String target;

        public MenuButton(String text, int x, int y, int width, int height, MenuAction action, String target) {
            this.text = text;
            this.x = x; this.y = y; this.width = width; this.height = height;
            this.action = action; this.target = target == null ? "" : target;
        }
        public MenuButton copy() { return new MenuButton(text, x, y, width, height, action, target); }
        @Override public String toString() { return text; }
    }

    public static final class MenuScreen {
        public final String id;
        public String title;
        public Color background = new Color(27, 31, 36);
        public final List<MenuButton> buttons = new ArrayList<>();

        public MenuScreen(String id, String title) { this.id = id; this.title = title; }
        public MenuScreen copy() {
            MenuScreen c = new MenuScreen(id, title);
            c.background = background;
            buttons.forEach(b -> c.buttons.add(b.copy()));
            return c;
        }
        @Override public String toString() { return title; }
    }
}
