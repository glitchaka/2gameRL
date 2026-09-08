package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class GameProjectTest {
    @Test
    void defaultProjectContainsRealPlayerEntityAndValidPlaceholder() {
        GameProject p = GameProject.createDefault();
        Level level = p.getLevels().get("level-1");
        assertNotNull(level);
        EntityDef player = level.entity("player");
        assertNotNull(player);
        assertTrue(player.has("PlayerController"));
        assertTrue(player.has("Rigidbody2D"));
        assertTrue(player.has("BoxCollider2D"));
        Asset placeholder = p.getAssets().get("placeholder-player.png");
        assertNotNull(placeholder);
        assertNotNull(placeholder.image());
        assertTrue(placeholder.data.length > 0);
        assertEquals(1, level.get(0, 0));
        assertEquals(0, level.get(2, 2));
        assertNotNull(p.getAssets().get("sample-sheet-32.png"));
        assertEquals(102, p.getAssets().size(), "placeholder + sheet + 100 sprites de muestra");
        assertNotNull(p.getAssets().get("sample-00-00.png").image());
        assertNotNull(p.getAssets().get("sample-09-09.png").image());
    }

    @Test
    void projectRoundTripPreservesEntitiesComponentsVariablesScriptsAndAssets() throws Exception {
        GameProject p = GameProject.createDefault();
        EntityDef enemy = new EntityDef("enemy-1", "Enemigo", 5.25, 6.5);
        enemy.assetKey = "placeholder-player.png";
        enemy.components.add(ComponentDef.preset("Patrol"));
        enemy.components.add(ComponentDef.preset("Health"));
        enemy.variables.put("score", "7");
        enemy.script = "on start\n  setVar ready yes\nend\non update\n  addVar ticks 1\nend\n";
        p.getLevels().get("level-1").entities.add(enemy);

        Path file = Files.createTempFile("2gamerl-roundtrip-", ".2grl");
        try {
            ProjectIO.save(p, file);
            GameProject loaded = ProjectIO.load(file);
            assertEquals(p.getTitle(), loaded.getTitle());
            assertNotNull(loaded.getAssets().get("placeholder-player.png").image());
            assertEquals(102, loaded.getAssets().size());
            EntityDef loadedEnemy = loaded.getLevels().get("level-1").entity("enemy-1");
            assertNotNull(loadedEnemy);
            assertEquals(5.25, loadedEnemy.x, 1e-9);
            assertEquals(6.5, loadedEnemy.y, 1e-9);
            assertTrue(loadedEnemy.has("Patrol"));
            assertEquals("100", loadedEnemy.component("Health").get("max", ""));
            assertEquals("7", loadedEnemy.variables.get("score"));
            assertTrue(loadedEnemy.script.contains("addVar ticks 1"));
        } finally {
            Files.deleteIfExists(file);
        }
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
