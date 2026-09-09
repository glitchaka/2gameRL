package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import static org.junit.jupiter.api.Assertions.*;

class RcCorruptionTest {
    @TempDir Path temp;

    @Test void malformedRcFieldsFallBackInsteadOfBreakingProject() throws Exception {
        GameProject p=GameProject.createDefault();AnimationClip c=new AnimationClip("a","A");c.frames.add(new AnimationFrame("placeholder-player.png",.1));p.getAnimationClips().put(c.key,c);Path original=temp.resolve("original.2grl");ProjectIO.save(p,original);Path corrupt=temp.resolve("corrupt.2grl");rewrite(original,corrupt,props->{props.setProperty("tile.0.mode","NOT_A_MODE");props.setProperty("tile.0.variant.0.weight","NaN?no");props.setProperty("clip.0.fps","bad");props.setProperty("skin.count","999999999999999999999");props.setProperty("stamp.count","-20");});GameProject q=assertDoesNotThrow(()->ProjectIO.load(corrupt));assertNotNull(q);assertEquals(TileAutotileMode.NONE,q.getTiles().get(0).autotileMode);assertEquals(0,q.getAnimationClips().get("a").fps,.0001);
    }

    @Test void missingRcSidecarLoadsAsCompatibleBaseProject() throws Exception {
        GameProject p=GameProject.createDefault();Path original=temp.resolve("with.2grl");ProjectIO.save(p,original);Path stripped=temp.resolve("without.2grl");strip(original,stripped);GameProject q=assertDoesNotThrow(()->ProjectIO.load(stripped));assertNotNull(q);assertTrue(q.getUiSkins().isEmpty());assertTrue(q.getStampPatterns().isEmpty());assertEquals(TileAutotileMode.NONE,q.getTiles().get(0).autotileMode);
    }

    private static void rewrite(Path input,Path output,java.util.function.Consumer<Properties>mutator)throws Exception{try(ZipInputStream in=new ZipInputStream(Files.newInputStream(input));ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){for(ZipEntry e;(e=in.getNextEntry())!=null;){byte[]bytes=in.readAllBytes();out.putNextEntry(new ZipEntry(e.getName()));if(e.getName().equals("rc-2.3.properties")){Properties p=new Properties();p.load(new ByteArrayInputStream(bytes));mutator.accept(p);ByteArrayOutputStream b=new ByteArrayOutputStream();p.store(b,"corrupt-test");out.write(b.toByteArray());}else out.write(bytes);out.closeEntry();}}}
    private static void strip(Path input,Path output)throws Exception{try(ZipInputStream in=new ZipInputStream(Files.newInputStream(input));ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(output))){for(ZipEntry e;(e=in.getNextEntry())!=null;){byte[]bytes=in.readAllBytes();if(e.getName().equals("rc-2.3.properties"))continue;out.putNextEntry(new ZipEntry(e.getName()));out.write(bytes);out.closeEntry();}}}
}
