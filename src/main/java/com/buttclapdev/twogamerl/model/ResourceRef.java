package com.buttclapdev.twogamerl.model;

import com.buttclapdev.twogamerl.model.GameProject.*;

import java.util.*;
import java.util.function.Function;

/**
 * Public resource reference resolver.
 *
 * Internal keys stay stable for persistence, but authors are allowed to use the
 * names they actually see in the Studio.  Exact keys still resolve so old
 * projects remain compatible.  Ambiguous display names deliberately resolve to
 * null instead of picking an arbitrary resource.
 */
public final class ResourceRef {
    private ResourceRef() {}

    public static String clean(String ref) {
        if (ref == null) return "";
        String value = ref.trim();
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\""))
            value = value.substring(1, value.length() - 1);
        return value.trim();
    }

    public static Level level(GameProject project, String ref) {
        return resolve(project.getLevels(), ref, l -> l.name);
    }
    public static MenuScreen menu(GameProject project, String ref) {
        return resolve(project.getMenus(), ref, m -> m.title);
    }
    public static PrefabDef prefab(GameProject project, String ref) {
        return resolve(project.getPrefabs(), ref, p -> p.name);
    }
    public static ParticlePreset particlePreset(GameProject project, String ref) {
        return resolve(project.getParticlePresets(), ref, p -> p.name);
    }
    public static AnimationClip animationClip(GameProject project, String ref) {
        return resolve(project.getAnimationClips(), ref, c -> c.name);
    }
    public static AnimatorController animatorController(GameProject project, String ref) {
        return resolve(project.getAnimatorControllers(), ref, c -> c.name);
    }
    public static TextStyle textStyle(GameProject project, String ref) {
        return resolve(project.getTextStyles(), ref, s -> s.name);
    }
    public static PaletteAsset palette(GameProject project, String ref) {
        return resolve(project.getPalettes(), ref, p -> p.name);
    }
    public static FontAsset font(GameProject project, String ref) {
        return resolve(project.getFonts(), ref, f -> f.sourceName);
    }
    public static Asset asset(GameProject project, String ref) {
        return resolve(project.getAssets(), ref, a -> a.sourceName);
    }
    public static InputAction inputAction(GameProject project, String ref) {
        return resolve(project.getInputActions(), ref, a -> a.name);
    }

    public static String levelKey(GameProject p,String ref){Level v=level(p,ref);return v==null?null:v.id;}
    public static String menuKey(GameProject p,String ref){MenuScreen v=menu(p,ref);return v==null?null:v.id;}
    public static String prefabKey(GameProject p,String ref){PrefabDef v=prefab(p,ref);return v==null?null:v.key;}
    public static String particlePresetKey(GameProject p,String ref){ParticlePreset v=particlePreset(p,ref);return v==null?null:v.key;}
    public static String animationClipKey(GameProject p,String ref){AnimationClip v=animationClip(p,ref);return v==null?null:v.key;}
    public static String animatorControllerKey(GameProject p,String ref){AnimatorController v=animatorController(p,ref);return v==null?null:v.key;}
    public static String textStyleKey(GameProject p,String ref){TextStyle v=textStyle(p,ref);return v==null?null:v.key;}
    public static String paletteKey(GameProject p,String ref){PaletteAsset v=palette(p,ref);return v==null?null:v.key;}

    public static <T> T resolve(Map<String,T> values,String rawRef,Function<T,String> displayName){
        if(values==null||values.isEmpty())return null;
        String ref=clean(rawRef);if(ref.isBlank())return null;
        T exact=values.get(ref);if(exact!=null)return exact;
        for(var e:values.entrySet())if(e.getKey().equalsIgnoreCase(ref))return e.getValue();
        T found=null;
        for(T value:values.values()){
            String name=displayName.apply(value);
            if(name==null||!name.equalsIgnoreCase(ref))continue;
            if(found!=null&&found!=value)return null;
            found=value;
        }
        return found;
    }

    public static boolean displayNameAvailable(Collection<String> names,String candidate,String current){
        String value=clean(candidate);if(value.isBlank())return false;
        for(String name:names)if(name!=null&&name.equalsIgnoreCase(value)&&(current==null||!name.equalsIgnoreCase(current)))return false;
        return true;
    }

    /** A script-safe visible reference; names containing whitespace are quoted. */
    public static String scriptName(String name){
        String clean=clean(name);if(clean.matches("[A-Za-z_][A-Za-z0-9_-]*"))return clean;
        return "\""+clean.replace("\"","")+"\"";
    }
}
