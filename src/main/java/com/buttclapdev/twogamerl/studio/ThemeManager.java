package com.buttclapdev.twogamerl.studio;

import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.stage.Window;

import java.net.URL;
import java.util.Arrays;
import java.util.Locale;
import java.util.prefs.Preferences;

final class ThemeManager {
    enum Theme {
        GRAPHITE("Graphite Brass","theme-graphite"),LIGHT("Paper Light","theme-light"),EMBER("Ember","theme-ember"),AMETHYST("Amethyst","theme-amethyst"),FOREST("Forest","theme-forest");
        final String displayName,styleClass;Theme(String displayName,String styleClass){this.displayName=displayName;this.styleClass=styleClass;}@Override public String toString(){return displayName;}
    }
    private static final String PREF_KEY="studio.theme";private static final Preferences PREFS=Preferences.userNodeForPackage(ThemeManager.class);private static Theme current=loadPreference();private static boolean installed;
    private ThemeManager(){}
    static Theme current(){return current;}
    static void install(){if(installed)return;installed=true;Window.getWindows().addListener((ListChangeListener<Window>)change->{while(change.next())for(Window window:change.getAddedSubList()){if(window.getScene()!=null)apply(window.getScene());window.sceneProperty().addListener((o,a,b)->{if(b!=null)apply(b);});}});for(Window window:Window.getWindows())if(window.getScene()!=null)apply(window.getScene());}
    static void set(Theme theme){if(theme==null)return;current=theme;PREFS.put(PREF_KEY,theme.name());try{PREFS.flush();}catch(Exception ignored){}for(Window window:Window.getWindows())if(window.getScene()!=null)apply(window.getScene());}
    static void apply(Scene scene){if(scene==null||scene.getRoot()==null)return;String rootType=scene.getRoot().getClass().getName();if(rootType.contains(".runtime.GameView"))return;addCss(scene,"/com/buttclapdev/twogamerl/themes.css");addCss(scene,"/com/buttclapdev/twogamerl/studio-rc4.css");addCss(scene,"/com/buttclapdev/twogamerl/theme-popup-fix.css");scene.getRoot().getStyleClass().removeIf(ThemeManager::isThemeClass);scene.getRoot().getStyleClass().add(current.styleClass);StudioControlPolish.install(scene.getRoot());scene.getRoot().applyCss();}
    private static void addCss(Scene scene,String path){URL css=ThemeManager.class.getResource(path);if(css!=null&&!scene.getStylesheets().contains(css.toExternalForm()))scene.getStylesheets().add(css.toExternalForm());}
    private static boolean isThemeClass(String value){return value!=null&&value.startsWith("theme-");}
    private static Theme loadPreference(){String raw=PREFS.get(PREF_KEY,"").trim().toUpperCase(Locale.ROOT);if(raw.isBlank()||raw.equals("MIDNIGHT"))return Theme.GRAPHITE;return Arrays.stream(Theme.values()).filter(t->t.name().equals(raw)).findFirst().orElse(Theme.GRAPHITE);}
}
