package com.buttclapdev.twogamerl.studio;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.prefs.Preferences;

/** Shared desktop file-dialog state. Every Studio chooser resumes in the last real directory the user visited. */
final class StudioFileDialogs {
    private static final String KEY="studio.lastDirectory";
    private static final Preferences PREFS=Preferences.userNodeForPackage(StudioFileDialogs.class);
    private static final Path FALLBACK_FILE=Path.of(System.getProperty("user.home","."),".2gamerl","studio-last-directory.txt");
    private static File sessionDirectory=load();

    private StudioFileDialogs(){}

    static File open(FileChooser chooser,Window owner){prepare(chooser);File result=chooser.showOpenDialog(owner);rememberSelection(result);return result;}
    static List<File> openMultiple(FileChooser chooser,Window owner){prepare(chooser);List<File> result=chooser.showOpenMultipleDialog(owner);if(result!=null&&!result.isEmpty())rememberSelection(result.getFirst());return result;}
    static File save(FileChooser chooser,Window owner){prepare(chooser);File result=chooser.showSaveDialog(owner);rememberSelection(result);return result;}
    static File directory(DirectoryChooser chooser,Window owner){prepare(chooser);File result=chooser.showDialog(owner);rememberDirectory(result);return result;}

    static void prepare(FileChooser chooser){File dir=current();if(dir!=null){try{chooser.setInitialDirectory(dir);}catch(IllegalArgumentException ignored){sessionDirectory=null;}}}
    static void prepare(DirectoryChooser chooser){File dir=current();if(dir!=null){try{chooser.setInitialDirectory(dir);}catch(IllegalArgumentException ignored){sessionDirectory=null;}}}

    private static File current(){if(valid(sessionDirectory))return sessionDirectory;sessionDirectory=load();return valid(sessionDirectory)?sessionDirectory:null;}
    private static void rememberSelection(File file){if(file==null)return;rememberDirectory(file.isDirectory()?file:file.getParentFile());}
    private static void rememberDirectory(File dir){
        if(!valid(dir))return;
        try{sessionDirectory=dir.getCanonicalFile();}catch(Exception e){sessionDirectory=dir.getAbsoluteFile();}
        String value=sessionDirectory.getAbsolutePath();
        try{PREFS.put(KEY,value);PREFS.flush();}catch(Exception ignored){}
        try{Files.createDirectories(FALLBACK_FILE.getParent());Files.writeString(FALLBACK_FILE,value,StandardCharsets.UTF_8);}catch(Exception ignored){}
    }
    private static File load(){
        try{if(Files.isRegularFile(FALLBACK_FILE)){File f=new File(Files.readString(FALLBACK_FILE,StandardCharsets.UTF_8).trim());if(valid(f))return f;}}catch(Exception ignored){}
        try{String raw=PREFS.get(KEY,"").trim();if(!raw.isBlank()){File f=new File(raw);if(valid(f))return f;}}catch(Exception ignored){}
        return null;
    }
    private static boolean valid(File f){return f!=null&&f.exists()&&f.isDirectory()&&f.canRead();}
}
