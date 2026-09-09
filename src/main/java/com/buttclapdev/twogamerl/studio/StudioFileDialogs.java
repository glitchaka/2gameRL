package com.buttclapdev.twogamerl.studio;

import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

/** Shared desktop file-dialog state. Every Studio chooser resumes in the last directory the user visited. */
final class StudioFileDialogs {
    private static final String KEY="studio.lastDirectory";
    private static final Preferences PREFS=Preferences.userNodeForPackage(StudioFileDialogs.class);
    private static File sessionDirectory=load();

    private StudioFileDialogs(){}

    static File open(FileChooser chooser,Window owner){prepare(chooser);File result=chooser.showOpenDialog(owner);rememberSelection(result);return result;}
    static List<File> openMultiple(FileChooser chooser,Window owner){prepare(chooser);List<File> result=chooser.showOpenMultipleDialog(owner);if(result!=null&&!result.isEmpty())rememberSelection(result.getFirst());return result;}
    static File save(FileChooser chooser,Window owner){prepare(chooser);File result=chooser.showSaveDialog(owner);rememberSelection(result);return result;}
    static File directory(DirectoryChooser chooser,Window owner){prepare(chooser);File result=chooser.showDialog(owner);rememberDirectory(result);return result;}

    static void prepare(FileChooser chooser){File dir=current();if(dir!=null)try{chooser.setInitialDirectory(dir);}catch(IllegalArgumentException ignored){}}
    static void prepare(DirectoryChooser chooser){File dir=current();if(dir!=null)try{chooser.setInitialDirectory(dir);}catch(IllegalArgumentException ignored){}}

    private static File current(){if(valid(sessionDirectory))return sessionDirectory;sessionDirectory=load();return valid(sessionDirectory)?sessionDirectory:null;}
    private static void rememberSelection(File file){if(file==null)return;File dir=file.isDirectory()?file:file.getParentFile();rememberDirectory(dir);}
    private static void rememberDirectory(File dir){if(!valid(dir))return;try{sessionDirectory=dir.getCanonicalFile();}catch(Exception e){sessionDirectory=dir.getAbsoluteFile();}PREFS.put(KEY,sessionDirectory.getAbsolutePath());}
    private static File load(){String raw=PREFS.get(KEY,"").trim();if(raw.isBlank())return null;File f=new File(raw);return valid(f)?f:null;}
    private static boolean valid(File f){return f!=null&&f.isDirectory()&&f.exists()&&f.canRead();}
}
