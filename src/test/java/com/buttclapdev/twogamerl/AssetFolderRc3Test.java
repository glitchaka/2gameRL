package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class AssetFolderRc3Test {
    @TempDir Path temp;

    @Test void emptyAndNestedFoldersRoundTrip() throws Exception {
        GameProject p=GameProject.createDefault();
        p.getAssetFolders().add("Personajes");
        p.getAssetFolders().add("Personajes/Heroe");
        p.getAssetFolders().add("UI/HUD");
        var asset=p.getAssets().get("placeholder-player.png");
        asset.folder="Personajes/Heroe";
        Path file=temp.resolve("folders.2grl");
        ProjectIO.save(p,file);
        GameProject q=ProjectIO.load(file);
        assertTrue(q.getAssetFolders().contains("Personajes"));
        assertTrue(q.getAssetFolders().contains("Personajes/Heroe"));
        assertTrue(q.getAssetFolders().contains("UI/HUD"));
        assertEquals("Personajes/Heroe",q.getAssets().get("placeholder-player.png").folder);
    }

    @Test void foldersSurviveDeepCopyIncludingEmptyFolders(){
        GameProject p=GameProject.createDefault();
        p.getAssetFolders().add("VFX/Impactos");
        p.getAssetFolders().add("Vacía");
        GameProject copy=p.deepCopy();
        assertEquals(p.getAssetFolders(),copy.getAssetFolders());
        copy.getAssetFolders().add("Solo copia");
        assertFalse(p.getAssetFolders().contains("Solo copia"));
    }
}
