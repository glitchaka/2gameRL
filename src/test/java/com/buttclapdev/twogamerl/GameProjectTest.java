package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class GameProjectTest {
    @Test
    void defaultProjectUsesGridMovementAndOnePhysicalSampleSheet() {
        GameProject p = GameProject.createDefault();
        Level level = p.getLevels().get("level-1");
        assertNotNull(level);
        EntityDef player = level.entity("player");
        assertNotNull(player);
        assertTrue(player.has("GridMovement"));
        assertTrue(player.has("BoxCollider2D"));
        assertFalse(player.has("PlayerController"));
        assertEquals("1", player.component("GridMovement").get("step", ""));
        assertEquals("Player", player.physicsLayer);
        assertEquals("Personajes", player.renderLayer);
        assertTrue(p.canCollide("Player", "World"));
        assertEquals("level-1", p.getMenus().get("main").buttons.getFirst().target);

        Asset placeholder = p.getAssets().get("placeholder-player.png");
        assertNotNull(placeholder);
        assertNotNull(placeholder.image());
        assertTrue(placeholder.data.length > 0);

        Asset sheet = p.getAssets().get("sample-sheet-32.png");
        Asset first = p.getAssets().get("sample-00-00");
        Asset last = p.getAssets().get("sample-09-09");
        assertNotNull(sheet);
        assertTrue(sheet.sourceOnly);
        assertNotNull(sheet.data);
        assertNotNull(first);
        assertNotNull(last);
        assertTrue(first.isRegion());
        assertTrue(last.isRegion());
        assertNull(first.data, "Una región virtual no debe duplicar bytes de la hoja.");
        assertEquals("sample-sheet-32.png", first.sourceAssetKey);
        assertEquals(0, first.regionX);
        assertEquals(0, first.regionY);
        assertEquals(32, first.regionWidth);
        assertEquals(32, first.regionHeight);
        assertEquals(102, p.getAssets().size(), "placeholder + una hoja + 100 regiones virtuales");
        assertEquals(101, p.getDrawableAssetKeys().size(), "La hoja fuente no debe aparecer como sprite asignable.");
        assertEquals(1, level.get(0, 0));
        assertEquals(0, level.get(2, 2));
    }

    @Test
    void projectRoundTripPreservesGroupsRegionsLayersMenusScriptsAndComponents() throws Exception {
        GameProject p = GameProject.createDefault();
        Level level = p.getLevels().get("level-1");

        EntityDef enemy = new EntityDef("enemy-1", "Enemigo", 5.25, 6.5);
        enemy.group = "Enemigos";
        enemy.assetKey = "sample-02-00";
        enemy.renderLayer = "Personajes";
        enemy.physicsLayer = "Enemy";
        enemy.components.add(ComponentDef.preset("Patrol"));
        enemy.components.add(ComponentDef.preset("Health"));
        enemy.variables.put("score", "7");
        enemy.script = "on start\n  setVar ready yes\nend\non update\n  addVar ticks 1\nend\n";
        level.entities.add(enemy);

        TileLayer foreground = new TileLayer("foreground", "Frente", level.width, level.height, 4);
        int[] empty = new int[level.width * level.height];
        Arrays.fill(empty, -1);
        foreground.replaceCells(empty);
        foreground.set(3, 3, 1);
        foreground.visible = true;
        foreground.locked = true;
        foreground.collision = false;
        foreground.physicsLayer = "World";
        level.tileLayers.add(foreground);
        p.setCollision("Player", "Enemy", false);

        MenuScreen menu = p.getMenus().get("main");
        menu.backgroundAssetKey = "sample-00-00";
        menu.titleAssetKey = "sample-01-00";
        menu.titleAnimation = MenuAnimation.PULSE;
        menu.titleAnimationSpeed = 1.75;
        menu.titleFontSize = 40;
        MenuButton play = menu.buttons.getFirst();
        play.assetKey = "sample-02-00";
        play.hoverAssetKey = "sample-03-00";
        play.animation = MenuAnimation.FLOAT;
        play.hoverEffect = MenuHoverEffect.GLOW;
        play.animationSpeed = 1.4;
        play.fontSize = 19;

        Path file = Files.createTempFile("2gamerl-roundtrip-", ".2grl");
        try {
            ProjectIO.save(p, file);
            GameProject loaded = ProjectIO.load(file);
            assertEquals(p.getTitle(), loaded.getTitle());
            assertEquals(102, loaded.getAssets().size());
            assertFalse(loaded.canCollide("Player", "Enemy"));

            Asset loadedSheet = loaded.getAssets().get("sample-sheet-32.png");
            Asset loadedRegion = loaded.getAssets().get("sample-02-00");
            assertNotNull(loadedSheet.data);
            assertTrue(loadedSheet.sourceOnly);
            assertTrue(loadedRegion.isRegion());
            assertNull(loadedRegion.data);
            assertEquals("sample-sheet-32.png", loadedRegion.sourceAssetKey);

            Level loadedLevel = loaded.getLevels().get("level-1");
            assertEquals(2, loadedLevel.tileLayers.size());
            TileLayer loadedForeground = loadedLevel.tileLayers.stream().filter(l -> l.id.equals("foreground")).findFirst().orElseThrow();
            assertEquals(1, loadedForeground.get(3,3));
            assertTrue(loadedForeground.locked);
            assertFalse(loadedForeground.collision);
            assertEquals(4, loadedForeground.order);

            EntityDef loadedEnemy = loadedLevel.entity("enemy-1");
            assertNotNull(loadedEnemy);
            assertEquals("Enemigos", loadedEnemy.group);
            assertEquals(5.25, loadedEnemy.x, 1e-9);
            assertEquals(6.5, loadedEnemy.y, 1e-9);
            assertEquals("sample-02-00", loadedEnemy.assetKey);
            assertEquals("Personajes", loadedEnemy.renderLayer);
            assertEquals("Enemy", loadedEnemy.physicsLayer);
            assertTrue(loadedEnemy.has("Patrol"));
            assertEquals("100", loadedEnemy.component("Health").get("max", ""));
            assertEquals("7", loadedEnemy.variables.get("score"));
            assertTrue(loadedEnemy.script.contains("addVar ticks 1"));

            MenuScreen loadedMenu = loaded.getMenus().get("main");
            assertEquals("sample-00-00", loadedMenu.backgroundAssetKey);
            assertEquals("sample-01-00", loadedMenu.titleAssetKey);
            assertEquals(MenuAnimation.PULSE, loadedMenu.titleAnimation);
            assertEquals(1.75, loadedMenu.titleAnimationSpeed, 1e-9);
            MenuButton loadedPlay = loadedMenu.buttons.getFirst();
            assertEquals("level-1", loadedPlay.target);
            assertEquals("sample-02-00", loadedPlay.assetKey);
            assertEquals("sample-03-00", loadedPlay.hoverAssetKey);
            assertEquals(MenuAnimation.FLOAT, loadedPlay.animation);
            assertEquals(MenuHoverEffect.GLOW, loadedPlay.hoverEffect);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void collisionMatrixIsSymmetricAndFiltersOnlyWhenRequested() {
        GameProject p = GameProject.createDefault();
        assertTrue(p.canCollide("Player", "Enemy"));
        p.setCollision("Player", "Enemy", false);
        assertFalse(p.canCollide("Player", "Enemy"));
        assertFalse(p.canCollide("Enemy", "Player"));
        p.setCollision("Enemy", "Player", true);
        assertTrue(p.canCollide("Player", "Enemy"));
        assertTrue(p.canCollide("Enemy", "Player"));
    }

    @Test
    void legacyV1ProjectMigratesSpawnToPlayerEntity() throws Exception {
        Path file = Files.createTempFile("2gamerl-v1-", ".2grl");
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(file))) {
            Properties root = new Properties();
            root.setProperty("formatVersion", "1");
            root.setProperty("title", "Legacy");
            root.setProperty("startLevel", "old");
            root.setProperty("startMenu", "");
            root.setProperty("tileSize", "32");
            put(zip, "project.properties", root);

            Properties tiles = new Properties();
            tiles.setProperty("count", "1");
            tiles.setProperty("tile.0.id", "0");
            tiles.setProperty("tile.0.name", "Suelo");
            tiles.setProperty("tile.0.rgb", Integer.toString(java.awt.Color.GRAY.getRGB()));
            tiles.setProperty("tile.0.walkable", "true");
            tiles.setProperty("tile.0.asset", "");
            put(zip, "tiles.properties", tiles);

            Properties level = new Properties();
            level.setProperty("id", "old");
            level.setProperty("name", "Viejo");
            level.setProperty("width", "4");
            level.setProperty("height", "4");
            level.setProperty("spawnX", "2");
            level.setProperty("spawnY", "1");
            level.setProperty("cells", "0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0");
            put(zip, "levels/old.properties", level);
        }

        try {
            GameProject loaded = ProjectIO.load(file);
            EntityDef player = loaded.getLevels().get("old").entity("player");
            assertNotNull(player);
            assertEquals(2.0, player.x);
            assertEquals(1.0, player.y);
            assertEquals("Player", player.physicsLayer);
            assertTrue(loaded.getAssets().containsKey("placeholder-player.png"));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private static void put(ZipOutputStream zip, String name, Properties properties) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        properties.store(new OutputStreamWriter(bytes, StandardCharsets.UTF_8), "");
        zip.write(bytes.toByteArray());
        zip.closeEntry();
    }
}
