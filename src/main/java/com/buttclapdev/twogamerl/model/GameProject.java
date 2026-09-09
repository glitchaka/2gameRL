package com.buttclapdev.twogamerl.model;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public final class GameProject {
    public static final int FORMAT_VERSION = 9;

    private String title = "2gameRL";
    private String startLevel = "level-1";
    private String startMenu = "main";
    private int tileSize = 32;

    private int logicalWidth = 320;
    private int logicalHeight = 180;
    private ScreenMode screenMode = ScreenMode.FULLSCREEN_BORDERLESS;
    private ScaleMode scaleMode = ScaleMode.PIXEL_PERFECT;
    private FilterMode defaultFilter = FilterMode.PIXEL;
    private boolean keepAspect = true;
    private boolean integerScale = true;
    private boolean uiSmooth = true;
    private Color letterboxColor = new Color(8,12,18);

    private final LinkedHashMap<Integer, TileDef> tiles = new LinkedHashMap<>();
    private final LinkedHashMap<String, Level> levels = new LinkedHashMap<>();
    private final LinkedHashMap<String, MenuScreen> menus = new LinkedHashMap<>();
    private final LinkedHashMap<String, Asset> assets = new LinkedHashMap<>();
    private final LinkedHashMap<String, FontAsset> fonts = new LinkedHashMap<>();
    private final LinkedHashMap<String, SoundAsset> sounds = new LinkedHashMap<>();
    private final LinkedHashMap<String, TextStyle> textStyles = new LinkedHashMap<>();
    private final LinkedHashMap<String, AnimationClip> animationClips = new LinkedHashMap<>();
    private final LinkedHashMap<String, AnimatorController> animatorControllers = new LinkedHashMap<>();
    private final LinkedHashMap<String, PrefabDef> prefabs = new LinkedHashMap<>();
    private final LinkedHashMap<String, InputAction> inputActions = new LinkedHashMap<>();
    private final LinkedHashMap<String, PaletteAsset> palettes = new LinkedHashMap<>();
    private final LinkedHashMap<String, ParticlePreset> particlePresets = new LinkedHashMap<>();
    private final LinkedHashMap<String, UISkinAsset> uiSkins = new LinkedHashMap<>();
    private final LinkedHashMap<String, AnimationSet> animationSets = new LinkedHashMap<>();
    private final LinkedHashMap<String, StampPattern> stampPatterns = new LinkedHashMap<>();
    private final LinkedHashSet<String> assetFolders = new LinkedHashSet<>();
    private final List<String> renderLayers = new ArrayList<>();
    private final LinkedHashMap<String, PhysicsLayerDef> physicsLayers = new LinkedHashMap<>();

    public GameProject() { ensureDefaultLayers(); ensureDefaultInput(); ensureDefaultTextStyles(); }

    private void ensureDefaultLayers() {
        if (renderLayers.isEmpty()) renderLayers.addAll(List.of("Fondo", "Suelo", "Objetos", "Personajes", "Frente", "UI"));
        if (physicsLayers.isEmpty()) {
            for (String name : List.of("Default", "World", "Player", "Enemy", "Trigger", "Projectile")) physicsLayers.put(name, new PhysicsLayerDef(name));
            for (PhysicsLayerDef a : physicsLayers.values()) a.collidesWith.addAll(physicsLayers.keySet());
        }
    }

    private void ensureDefaultInput(){
        if(!inputActions.isEmpty())return;
        inputActions.put("MoveLeft",new InputAction("MoveLeft",List.of("A","LEFT")));
        inputActions.put("MoveRight",new InputAction("MoveRight",List.of("D","RIGHT")));
        inputActions.put("MoveUp",new InputAction("MoveUp",List.of("W","UP")));
        inputActions.put("MoveDown",new InputAction("MoveDown",List.of("S","DOWN")));
        inputActions.put("Jump",new InputAction("Jump",List.of("SPACE")));
        inputActions.put("Accept",new InputAction("Accept",List.of("ENTER","SPACE")));
        inputActions.put("Cancel",new InputAction("Cancel",List.of("ESCAPE")));
    }

    private void ensureDefaultTextStyles(){
        if(!textStyles.isEmpty())return;
        TextStyle free=new TextStyle("default-free","Default FREE");
        free.fontSize=28;free.bold=true;free.textColor=Color.WHITE;free.shadow=true;free.shadowColor=new Color(0,0,0,210);free.fadeIn=.15;free.fadeOut=.35;free.maxWidth=720;
        TextStyle bubble=new TextStyle("default-bubble","Default BUBBLE");
        bubble.fontSize=16;bubble.textColor=new Color(17,19,24);bubble.backgroundColor=new Color(255,255,255,245);bubble.borderColor=new Color(20,25,32,205);bubble.borderWidth=2;bubble.radius=14;bubble.paddingX=14;bubble.paddingY=10;bubble.fadeIn=.15;bubble.fadeOut=.35;bubble.maxWidth=360;bubble.tail=true;
        TextStyle novel=new TextStyle("default-novel","Default NOVEL");
        novel.fontSize=18;novel.textColor=Color.WHITE;novel.speakerColor=new Color(143,207,255);novel.speakerFontSize=16;novel.boldSpeaker=true;novel.backgroundColor=new Color(8,12,18,220);novel.borderColor=new Color(120,160,195,115);novel.borderWidth=1;novel.paddingX=28;novel.paddingY=18;novel.fadeOut=.35;novel.panelHeight=190;
        textStyles.put(free.key,free);textStyles.put(bubble.key,bubble);textStyles.put(novel.key,novel);
    }

    public static GameProject createDefault() {
        GameProject p = new GameProject();
        TileDef floor=new TileDef(0,"Suelo",new Color(42,48,58),true,"");floor.tags.add("ground");
        TileDef wall=new TileDef(1,"Muro",new Color(80,88,104),false,"");wall.tags.add("wall");
        p.tiles.put(0,floor);p.tiles.put(1,wall);
        p.prefabs.put("suelo",PrefabDef.tile("suelo","Suelo",0));
        p.prefabs.put("muro",PrefabDef.tile("muro","Muro",1));
        p.assets.put("placeholder-player.png", new Asset("placeholder-player.png", "placeholder-player.png", placeholderPlayerPng()));
        loadBundledSampleSprites(p);

        Level level = new Level("level-1", "Nivel 1", 24, 16);
        level.backgroundColor = new Color(16,22,32);
        level.backgroundMode = BackgroundMode.COLOR;
        p.levels.put(level.id, level);

        MenuScreen menu = new MenuScreen("main", "2gameRL");
        menu.canvasWidth=p.logicalWidth;menu.canvasHeight=p.logicalHeight;
        menu.buttons.add(new MenuButton("Jugar",60,82,100,26,MenuAction.START_GAME,"level-1"));
        menu.buttons.add(new MenuButton("Salir",60,114,100,26,MenuAction.EXIT,""));
        p.menus.put(menu.id,menu);
        return p;
    }

    private static void loadBundledSampleSprites(GameProject project) {
        try (InputStream input = GameProject.class.getResourceAsStream("/samples/TexturesSheet/test.png")) {
            if (input == null) return;
            byte[] bytes = input.readAllBytes();
            BufferedImage sheet = ImageIO.read(new ByteArrayInputStream(bytes));
            if (sheet == null) return;
            String sourceKey = "sample-sheet-32.png";
            Asset source=new Asset(sourceKey,"Sample Texture Sheet 32px",bytes,true);source.category=AssetCategory.SPRITESHEET;project.assets.put(sourceKey,source);
            for (int y=0; y<sheet.getHeight()/32; y++) for (int x=0; x<sheet.getWidth()/32; x++) {
                String key = String.format(Locale.ROOT, "sample-%02d-%02d", x, y);
                Asset region=new Asset(key,String.format(Locale.ROOT,"Sample Sprite %d,%d",x,y),sourceKey,x*32,y*32,32,32);region.category=AssetCategory.SPRITE;project.assets.put(key,region);
            }
        } catch (Exception ignored) {}
    }

    private static byte[] placeholderPlayerPng() {
        try {
            int[][] px={{0,0,0,1,1,1,1,0,0,0},{0,0,1,2,2,2,2,1,0,0},{0,1,2,2,2,2,2,2,1,0},{0,1,2,3,2,2,3,2,1,0},{0,1,2,2,2,2,2,2,1,0},{0,0,1,2,3,3,2,1,0,0},{0,1,1,4,4,4,4,1,1,0},{1,4,4,4,4,4,4,4,4,1},{0,0,1,4,1,1,4,1,0,0},{0,0,1,1,0,0,1,1,0,0}};
            int[] colors={0x00000000,0xFF172033,0xFFF2C7A5,0xFF34445F,0xFF4FA8FF};
            BufferedImage image=new BufferedImage(10,10,BufferedImage.TYPE_INT_ARGB);
            for(int y=0;y<10;y++)for(int x=0;x<10;x++)image.setRGB(x,y,colors[px[y][x]]);
            ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
        } catch(Exception e){return new byte[0];}
    }

    public GameProject deepCopy(){
        GameProject c=new GameProject();
        c.title=title;c.startLevel=startLevel;c.startMenu=startMenu;c.tileSize=tileSize;c.logicalWidth=logicalWidth;c.logicalHeight=logicalHeight;c.screenMode=screenMode;c.scaleMode=scaleMode;c.defaultFilter=defaultFilter;c.keepAspect=keepAspect;c.integerScale=integerScale;c.uiSmooth=uiSmooth;c.letterboxColor=letterboxColor;
        c.renderLayers.clear();c.renderLayers.addAll(renderLayers);c.physicsLayers.clear();physicsLayers.forEach((k,v)->c.physicsLayers.put(k,v.copy()));
        c.tiles.clear();tiles.forEach((k,v)->c.tiles.put(k,v.copy()));c.levels.clear();levels.forEach((k,v)->c.levels.put(k,v.copy()));c.menus.clear();menus.forEach((k,v)->c.menus.put(k,v.copy()));c.assets.clear();assets.forEach((k,v)->c.assets.put(k,v.copy()));c.fonts.clear();fonts.forEach((k,v)->c.fonts.put(k,v.copy()));c.sounds.clear();sounds.forEach((k,v)->c.sounds.put(k,v.copy()));
        c.textStyles.clear();textStyles.forEach((k,v)->c.textStyles.put(k,v.copy()));c.animationClips.clear();animationClips.forEach((k,v)->c.animationClips.put(k,v.copy()));c.animatorControllers.clear();animatorControllers.forEach((k,v)->c.animatorControllers.put(k,v.copy()));c.prefabs.clear();prefabs.forEach((k,v)->c.prefabs.put(k,v.copy()));c.inputActions.clear();inputActions.forEach((k,v)->c.inputActions.put(k,v.copy()));c.palettes.clear();palettes.forEach((k,v)->c.palettes.put(k,v.copy()));c.particlePresets.clear();particlePresets.forEach((k,v)->c.particlePresets.put(k,v.copy()));c.uiSkins.clear();uiSkins.forEach((k,v)->c.uiSkins.put(k,v.copy()));c.animationSets.clear();animationSets.forEach((k,v)->c.animationSets.put(k,v.copy()));c.stampPatterns.clear();stampPatterns.forEach((k,v)->c.stampPatterns.put(k,v.copy()));c.assetFolders.clear();c.assetFolders.addAll(assetFolders);return c;
    }

    public String getTitle(){return title;} public void setTitle(String v){title=v==null||v.isBlank()?"2gameRL":v.trim();}
    public String getStartLevel(){return startLevel;} public void setStartLevel(String v){startLevel=v;}
    public String getStartMenu(){return startMenu;} public void setStartMenu(String v){startMenu=v;}
    public int getTileSize(){return tileSize;} public void setTileSize(int v){tileSize=Math.max(8,Math.min(128,v));}
    public int getLogicalWidth(){return logicalWidth;} public void setLogicalWidth(int v){logicalWidth=Math.max(160,Math.min(7680,v));}
    public int getLogicalHeight(){return logicalHeight;} public void setLogicalHeight(int v){logicalHeight=Math.max(90,Math.min(4320,v));}
    public ScreenMode getScreenMode(){return screenMode;} public void setScreenMode(ScreenMode v){screenMode=v==null?ScreenMode.FULLSCREEN_BORDERLESS:v;}
    public ScaleMode getScaleMode(){return scaleMode;} public void setScaleMode(ScaleMode v){scaleMode=v==null?ScaleMode.PIXEL_PERFECT:v;}
    public FilterMode getDefaultFilter(){return defaultFilter;} public void setDefaultFilter(FilterMode v){defaultFilter=v==null?FilterMode.PIXEL:v;}
    public boolean isKeepAspect(){return keepAspect;} public void setKeepAspect(boolean v){keepAspect=v;}
    public boolean isIntegerScale(){return integerScale;} public void setIntegerScale(boolean v){integerScale=v;}
    public boolean isUiSmooth(){return uiSmooth;} public void setUiSmooth(boolean v){uiSmooth=v;}
    public Color getLetterboxColor(){return letterboxColor;} public void setLetterboxColor(Color v){letterboxColor=v==null?new Color(8,12,18):v;}
    public Map<Integer,TileDef> getTiles(){return tiles;} public Map<String,Level> getLevels(){return levels;} public Map<String,MenuScreen> getMenus(){return menus;} public Map<String,Asset> getAssets(){return assets;} public Map<String,FontAsset> getFonts(){return fonts;} public Map<String,SoundAsset> getSounds(){return sounds;}
    public Map<String,TextStyle> getTextStyles(){return textStyles;} public Map<String,AnimationClip> getAnimationClips(){return animationClips;} public Map<String,AnimatorController> getAnimatorControllers(){return animatorControllers;} public Map<String,PrefabDef> getPrefabs(){return prefabs;} public Map<String,InputAction> getInputActions(){return inputActions;} public Map<String,PaletteAsset> getPalettes(){return palettes;} public Map<String,ParticlePreset> getParticlePresets(){return particlePresets;} public Map<String,UISkinAsset> getUiSkins(){return uiSkins;} public Map<String,AnimationSet> getAnimationSets(){return animationSets;} public Map<String,StampPattern> getStampPatterns(){return stampPatterns;} public Set<String> getAssetFolders(){return assetFolders;}
    public List<String> getRenderLayers(){return renderLayers;} public Map<String,PhysicsLayerDef> getPhysicsLayers(){return physicsLayers;}
    public List<String> getDrawableAssetKeys(){return assets.values().stream().filter(a->!a.sourceOnly||a.isRegion()).map(a->a.key).toList();}
    public int nextTileId(){return tiles.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1)+1;}
    public int renderOrder(String name){int i=renderLayers.indexOf(name);return i<0?Math.max(0,renderLayers.indexOf("Objetos")):i;}
    public boolean canCollide(String a,String b){PhysicsLayerDef aa=physicsLayers.get(a),bb=physicsLayers.get(b);if(aa==null||bb==null)return true;return aa.collidesWith.contains(b)&&bb.collidesWith.contains(a);}
    public void setCollision(String a,String b,boolean value){PhysicsLayerDef aa=physicsLayers.get(a),bb=physicsLayers.get(b);if(aa==null||bb==null)return;if(value){aa.collidesWith.add(b);bb.collidesWith.add(a);}else{aa.collidesWith.remove(b);bb.collidesWith.remove(a);}}
    public InputAction action(String name){if(name==null)return null;InputAction exact=inputActions.get(name);if(exact!=null)return exact;return inputActions.values().stream().filter(a->a.name.equalsIgnoreCase(name)||a.key.equalsIgnoreCase(name)).findFirst().orElse(null);}
    public SoundAsset sound(String ref){if(ref==null)return null;SoundAsset exact=sounds.get(ref);if(exact!=null)return exact;return sounds.values().stream().filter(s->s.key.equalsIgnoreCase(ref)||s.name.equalsIgnoreCase(ref)).findFirst().orElse(null);}
    public ParticlePreset particlePreset(String ref){if(ref==null)return null;ParticlePreset exact=particlePresets.get(ref);if(exact!=null)return exact;return particlePresets.values().stream().filter(p->p.key.equalsIgnoreCase(ref)||p.name.equalsIgnoreCase(ref)).findFirst().orElse(null);}

    /** Renames a particle preset canonically and migrates project references. Returns the effective unique key. */
    public String renameParticlePreset(String oldKey,String requested){ParticlePreset old=particlePresets.get(oldKey);if(old==null)return oldKey;String base=resourceKey(requested,"particles");String key=base;int n=2;while(!key.equals(oldKey)&&particlePresets.containsKey(key))key=base+"-"+n++;if(key.equals(oldKey)){old.name=key;return key;}ParticlePreset replacement=old.copyAs(key);replacement.name=key;LinkedHashMap<String,ParticlePreset>next=new LinkedHashMap<>();for(var e:particlePresets.entrySet())next.put(e.getKey().equals(oldKey)?key:e.getKey(),e.getKey().equals(oldKey)?replacement:e.getValue());particlePresets.clear();particlePresets.putAll(next);for(Level l:levels.values()){l.script=replacePresetLiteral(l.script,oldKey,key);for(TileLayer layer:l.tileLayers)layer.script=replacePresetLiteral(layer.script,oldKey,key);for(EntityDef e:l.entities)migrateParticleReference(e,oldKey,key);}for(PrefabDef p:prefabs.values()){if(p.template!=null)migrateParticleReference(p.template,oldKey,key);if(oldKey.equals(p.variantOverrides.get("ParticleEmitter2D.preset")))p.variantOverrides.put("ParticleEmitter2D.preset",key);}return key;}
    private static void migrateParticleReference(EntityDef e,String oldKey,String key){ComponentDef c=e.component("ParticleEmitter2D");if(c!=null&&oldKey.equals(c.get("preset","")))c.properties.put("preset",key);if(oldKey.equals(e.prefabOverrides.get("ParticleEmitter2D.preset")))e.prefabOverrides.put("ParticleEmitter2D.preset",key);e.script=replacePresetLiteral(e.script,oldKey,key);}
    private static String replacePresetLiteral(String script,String oldKey,String key){if(script==null||script.isEmpty())return script;return script.replaceAll("(?im)(ParticleEmitter2D\\.preset\\s*=\\s*)"+Pattern.quote(oldKey)+"(?=\\s*(?:#.*)?$)","$1"+java.util.regex.Matcher.quoteReplacement(key));}
    public static String resourceKey(String value,String fallback){String clean=value==null?"":value.trim().replaceAll("[^A-Za-z0-9._-]","_");return clean.isBlank()?fallback:clean;}

    public enum ScreenMode { WINDOWED, FULLSCREEN, FULLSCREEN_BORDERLESS }
    public enum ScaleMode { PIXEL_PERFECT, KEEP_ASPECT, EXPAND, STRETCH }
    public enum FilterMode { PIXEL, LINEAR }
    public enum AssetCategory { IMAGE, SPRITESHEET, SPRITE, BACKGROUND, UI, TILESET, VFX, PORTRAIT }
    public enum SoundCategory { SFX, MUSIC }
    public enum PlacementMode { AUTO, TILEMAP, ENTITY }
    public enum AnimationCategory { ENTITY, UI, VFX, TILE }
    public enum TileAutotileMode { NONE, FOUR_WAY, EIGHT_WAY }
    public enum FrameShapeRole { HITBOX, HURTBOX }
    public enum AnimatorParameterType { BOOL, INT, FLOAT, TRIGGER }
    public enum BlendTreeType { NONE, ONE_D, TWO_D }
    public enum DirectionMode { TWO, FOUR, EIGHT }

    public static final class PhysicsLayerDef {
        public String name; public final LinkedHashSet<String> collidesWith=new LinkedHashSet<>();
        public PhysicsLayerDef(String name){this.name=name;} public PhysicsLayerDef copy(){PhysicsLayerDef c=new PhysicsLayerDef(name);c.collidesWith.addAll(collidesWith);return c;} @Override public String toString(){return name;}
    }

    public static final class TileVariant {public String assetKey="";public double weight=1;public TileVariant(){}public TileVariant(String assetKey,double weight){this.assetKey=assetKey==null?"":assetKey;this.weight=Math.max(0,weight);}public TileVariant copy(){return new TileVariant(assetKey,weight);}}
    public static final class TileDef {
        public final int id; public String name; public Color color; public boolean walkable; public String assetKey; public boolean oneWay; public double friction=1,damage; public String animationClip=""; public final LinkedHashSet<String>tags=new LinkedHashSet<>();
        public TileAutotileMode autotileMode=TileAutotileMode.NONE;public String connectTag="";public final List<TileVariant>variants=new ArrayList<>();public final LinkedHashMap<Integer,String>autotileAssets=new LinkedHashMap<>();
        public TileDef(int id,String name,Color color,boolean walkable,String assetKey){this.id=id;this.name=name;this.color=color;this.walkable=walkable;this.assetKey=assetKey==null?"":assetKey;}
        public boolean solid(){return !walkable;} public TileDef copy(){TileDef c=new TileDef(id,name,color,walkable,assetKey);c.oneWay=oneWay;c.friction=friction;c.damage=damage;c.animationClip=animationClip;c.tags.addAll(tags);c.autotileMode=autotileMode;c.connectTag=connectTag;variants.forEach(v->c.variants.add(v.copy()));c.autotileAssets.putAll(autotileAssets);return c;} @Override public String toString(){return name;}
    }

    public static final class Asset {
        public final String key; public String sourceName; public byte[] data; public boolean sourceOnly,favorite; public String sourceAssetKey="",folder=""; public int regionX,regionY,regionWidth,regionHeight; public FilterMode filterMode=FilterMode.PIXEL; public AssetCategory category=AssetCategory.IMAGE; public double pivotX=.5,pivotY=.5; public String sourcePath=""; public long sourceModified; public int sliceTop,sliceBottom,sliceLeft,sliceRight; public final LinkedHashSet<String>tags=new LinkedHashSet<>(); private transient BufferedImage image;
        public Asset(String key,String sourceName,byte[]data){this(key,sourceName,data,false);} public Asset(String key,String sourceName,byte[]data,boolean sourceOnly){this.key=key;this.sourceName=sourceName;this.data=data;this.sourceOnly=sourceOnly;}
        public Asset(String key,String sourceName,String sourceAssetKey,int x,int y,int width,int height){this.key=key;this.sourceName=sourceName;this.sourceAssetKey=sourceAssetKey==null?"":sourceAssetKey;this.regionX=Math.max(0,x);this.regionY=Math.max(0,y);this.regionWidth=Math.max(1,width);this.regionHeight=Math.max(1,height);this.category=AssetCategory.SPRITE;}
        public boolean isRegion(){return !sourceAssetKey.isBlank()&&regionWidth>0&&regionHeight>0;} public boolean isNineSlice(){return sliceTop>0||sliceBottom>0||sliceLeft>0||sliceRight>0;} public BufferedImage image(){if(image==null&&data!=null){try{image=ImageIO.read(new ByteArrayInputStream(data));}catch(Exception ignored){}}return image;}
        public Asset copy(){Asset c=isRegion()?new Asset(key,sourceName,sourceAssetKey,regionX,regionY,regionWidth,regionHeight):new Asset(key,sourceName,data==null?null:data.clone(),sourceOnly);c.sourceOnly=sourceOnly;c.favorite=favorite;c.folder=folder;c.filterMode=filterMode;c.category=category;c.pivotX=pivotX;c.pivotY=pivotY;c.sourcePath=sourcePath;c.sourceModified=sourceModified;c.sliceTop=sliceTop;c.sliceBottom=sliceBottom;c.sliceLeft=sliceLeft;c.sliceRight=sliceRight;c.tags.addAll(tags);return c;} @Override public String toString(){return (favorite?"★ ":"")+(sourceOnly&&!isRegion()?"[Hoja] ":isRegion()?"[Región] ":"")+(sourceName==null?key:sourceName);}
    }

    public static final class FontAsset {public final String key; public String sourceName; public byte[] data; public FontAsset(String key,String sourceName,byte[]data){this.key=key;this.sourceName=sourceName;this.data=data;} public FontAsset copy(){return new FontAsset(key,sourceName,data==null?null:data.clone());} @Override public String toString(){return sourceName==null?key:sourceName;}}
    public static final class SoundAsset {public final String key;public String name;public byte[]data;public double volume=1;public boolean loop;public SoundCategory category=SoundCategory.SFX;public SoundAsset(String key,String name,byte[]data){this.key=key;this.name=name==null?key:name;this.data=data;}public SoundAsset copy(){SoundAsset c=new SoundAsset(key,name,data==null?null:data.clone());c.volume=volume;c.loop=loop;c.category=category;return c;}@Override public String toString(){return name+" · "+category;}}

    public static final class TextStyle {
        public final String key; public String name,fontKey="",skinAssetKey="",uiSkinKey=""; public int fontSize=18,speakerFontSize=16; public Color textColor=Color.WHITE,speakerColor=new Color(143,207,255),backgroundColor=new Color(0,0,0,0),borderColor=new Color(0,0,0,0),outlineColor=Color.BLACK,shadowColor=new Color(0,0,0,180); public boolean bold,boldSpeaker,shadow,tail; public double borderWidth,radius,paddingX=12,paddingY=8,outlineWidth,fadeIn=.15,fadeOut=.35,maxWidth=720,panelHeight=190; public String alignment="CENTER";
        public TextStyle(String key,String name){this.key=key;this.name=name;} public TextStyle copy(){TextStyle c=new TextStyle(key,name);c.fontKey=fontKey;c.skinAssetKey=skinAssetKey;c.uiSkinKey=uiSkinKey;c.fontSize=fontSize;c.speakerFontSize=speakerFontSize;c.textColor=textColor;c.speakerColor=speakerColor;c.backgroundColor=backgroundColor;c.borderColor=borderColor;c.outlineColor=outlineColor;c.shadowColor=shadowColor;c.bold=bold;c.boldSpeaker=boldSpeaker;c.shadow=shadow;c.tail=tail;c.borderWidth=borderWidth;c.radius=radius;c.paddingX=paddingX;c.paddingY=paddingY;c.outlineWidth=outlineWidth;c.fadeIn=fadeIn;c.fadeOut=fadeOut;c.maxWidth=maxWidth;c.panelHeight=panelHeight;c.alignment=alignment;return c;} @Override public String toString(){return name;}
    }

    public static final class FrameShape {public String name="shape";public FrameShapeRole role=FrameShapeRole.HITBOX;public double x,y,width=.5,height=.5;public FrameShape(){}public FrameShape(String name,FrameShapeRole role,double x,double y,double width,double height){this.name=name==null?"shape":name;this.role=role==null?FrameShapeRole.HITBOX:role;this.x=x;this.y=y;this.width=Math.max(.001,width);this.height=Math.max(.001,height);}public FrameShape copy(){return new FrameShape(name,role,x,y,width,height);}}
    public static final class AnimationFrame {public String assetKey;public double duration=.1;public String event="";public final List<FrameShape>shapes=new ArrayList<>();public AnimationFrame(String assetKey,double duration){this.assetKey=assetKey;this.duration=Math.max(.01,duration);}public AnimationFrame copy(){AnimationFrame c=new AnimationFrame(assetKey,duration);c.event=event;shapes.forEach(s->c.shapes.add(s.copy()));return c;}}
    public static final class AnimationClip {public final String key;public String name;public AnimationCategory category=AnimationCategory.ENTITY;public final List<AnimationFrame>frames=new ArrayList<>();public boolean loop=true,pingPong,randomStart,reverse;public double speed=1,fps;public AnimationClip(String key,String name){this.key=key;this.name=name;}public double frameDuration(int index){if(fps>0)return 1.0/fps;if(index<0||index>=frames.size())return .1;return Math.max(.01,frames.get(index).duration);}public double duration(){if(fps>0)return frames.isEmpty()?0:frames.size()/fps;return frames.stream().mapToDouble(f->Math.max(.01,f.duration)).sum();}public AnimationClip copy(){AnimationClip c=new AnimationClip(key,name);c.category=category;c.loop=loop;c.pingPong=pingPong;c.randomStart=randomStart;c.reverse=reverse;c.speed=speed;c.fps=fps;frames.forEach(f->c.frames.add(f.copy()));return c;}@Override public String toString(){return name;}}
    public static final class AnimatorParameter {public String name;public AnimatorParameterType type=AnimatorParameterType.FLOAT;public String defaultValue="0";public AnimatorParameter(String name,AnimatorParameterType type,String defaultValue){this.name=name;this.type=type==null?AnimatorParameterType.FLOAT:type;this.defaultValue=defaultValue==null?"0":defaultValue;}public AnimatorParameter copy(){return new AnimatorParameter(name,type,defaultValue);}}
    public static final class BlendChild {public String clipKey="";public double x,y;public BlendChild(){}public BlendChild(String clipKey,double x,double y){this.clipKey=clipKey==null?"":clipKey;this.x=x;this.y=y;}public BlendChild copy(){return new BlendChild(clipKey,x,y);}}
    public static final class BlendTree {public BlendTreeType type=BlendTreeType.NONE;public String parameterX="",parameterY="";public final List<BlendChild>children=new ArrayList<>();public BlendTree copy(){BlendTree b=new BlendTree();b.type=type;b.parameterX=parameterX;b.parameterY=parameterY;children.forEach(c->b.children.add(c.copy()));return b;}}
    public static final class AnimatorState {public String name,clipKey;public double speed=1;public final BlendTree blendTree=new BlendTree();public AnimatorState(String name,String clipKey){this.name=name;this.clipKey=clipKey;}public AnimatorState copy(){AnimatorState c=new AnimatorState(name,clipKey);c.speed=speed;c.blendTree.type=blendTree.type;c.blendTree.parameterX=blendTree.parameterX;c.blendTree.parameterY=blendTree.parameterY;blendTree.children.forEach(v->c.blendTree.children.add(v.copy()));return c;}@Override public String toString(){return name;}}
    public static final class AnimatorTransition {public String from,to,path="",operator="==",value="true";public double exitTime=-1;public int priority;public AnimatorTransition(String from,String to){this.from=from;this.to=to;}public AnimatorTransition copy(){AnimatorTransition c=new AnimatorTransition(from,to);c.path=path;c.operator=operator;c.value=value;c.exitTime=exitTime;c.priority=priority;return c;}}
    public static final class AnimatorController {public final String key;public String name,defaultState="";public final LinkedHashMap<String,AnimatorParameter>parameters=new LinkedHashMap<>();public final LinkedHashMap<String,AnimatorState>states=new LinkedHashMap<>();public final List<AnimatorTransition>transitions=new ArrayList<>();public AnimatorController(String key,String name){this.key=key;this.name=name;}public AnimatorController copy(){AnimatorController c=new AnimatorController(key,name);c.defaultState=defaultState;parameters.forEach((k,v)->c.parameters.put(k,v.copy()));states.forEach((k,v)->c.states.put(k,v.copy()));transitions.forEach(t->c.transitions.add(t.copy()));return c;}@Override public String toString(){return name;}}

    public static final class UISkinAsset {public final String key;public String name,normalAssetKey="",hoverAssetKey="",pressedAssetKey="",disabledAssetKey="",fontKey="";public Color textColor=Color.WHITE,disabledTextColor=new Color(170,170,170),backgroundColor=new Color(31,115,170),borderColor=new Color(75,106,132);public double paddingX=12,paddingY=8,borderWidth=1,radius=7;public MenuAnimation animation=MenuAnimation.NONE;public UISkinAsset(String key,String name){this.key=key;this.name=name==null?key:name;}public UISkinAsset copy(){UISkinAsset c=new UISkinAsset(key,name);c.normalAssetKey=normalAssetKey;c.hoverAssetKey=hoverAssetKey;c.pressedAssetKey=pressedAssetKey;c.disabledAssetKey=disabledAssetKey;c.fontKey=fontKey;c.textColor=textColor;c.disabledTextColor=disabledTextColor;c.backgroundColor=backgroundColor;c.borderColor=borderColor;c.paddingX=paddingX;c.paddingY=paddingY;c.borderWidth=borderWidth;c.radius=radius;c.animation=animation;return c;}@Override public String toString(){return name;}}
    public static final class AnimationSet {public final String key;public String name;public DirectionMode directionMode=DirectionMode.FOUR;public boolean useFlipX;public final LinkedHashMap<String,String>clips=new LinkedHashMap<>();public AnimationSet(String key,String name){this.key=key;this.name=name==null?key:name;}public String clip(String state,String direction){String s=state==null?"":state,d=direction==null?"DOWN":direction;String exact=clips.get(s+"|"+d);if(exact!=null&&!exact.isBlank())return exact;return clips.getOrDefault(s+"|DEFAULT","");}public AnimationSet copy(){AnimationSet c=new AnimationSet(key,name);c.directionMode=directionMode;c.useFlipX=useFlipX;c.clips.putAll(clips);return c;}@Override public String toString(){return name;}}
    public static final class StampPattern {public final String key;public String name;public int width,height;private int[]cells;public StampPattern(String key,String name,int width,int height){this.key=key;this.name=name==null?key:name;this.width=Math.max(1,width);this.height=Math.max(1,height);cells=new int[this.width*this.height];Arrays.fill(cells,-1);}public int get(int x,int y){return x<0||y<0||x>=width||y>=height?-1:cells[y*width+x];}public void set(int x,int y,int tile){if(x>=0&&y>=0&&x<width&&y<height)cells[y*width+x]=tile;}public int[]cells(){return cells.clone();}public void replaceCells(int[]v){if(v!=null&&v.length==width*height)cells=v.clone();}public void resize(int w,int h){w=Math.max(1,w);h=Math.max(1,h);int[]n=new int[w*h];Arrays.fill(n,-1);for(int y=0;y<Math.min(height,h);y++)System.arraycopy(cells,y*width,n,y*w,Math.min(width,w));width=w;height=h;cells=n;}public StampPattern copy(){StampPattern s=new StampPattern(key,name,width,height);s.cells=cells.clone();return s;}@Override public String toString(){return name;}}

    public static final class PrefabDef {public final String key;public String name,parentPrefab="";public PlacementMode placement=PlacementMode.AUTO;public int tileId=-1;public EntityDef template;public final LinkedHashMap<String,String>variantOverrides=new LinkedHashMap<>();public PrefabDef(String key,String name){this.key=key;this.name=name;}public static PrefabDef tile(String key,String name,int tileId){PrefabDef p=new PrefabDef(key,name);p.placement=PlacementMode.TILEMAP;p.tileId=tileId;return p;}public static PrefabDef entity(String key,String name,EntityDef template){PrefabDef p=new PrefabDef(key,name);p.placement=PlacementMode.ENTITY;p.template=template;return p;}public boolean isTileBrush(){return placement==PlacementMode.TILEMAP||placement==PlacementMode.AUTO&&tileId>=0;}public PrefabDef copy(){PrefabDef c=new PrefabDef(key,name);c.parentPrefab=parentPrefab;c.placement=placement;c.tileId=tileId;c.template=template==null?null:template.copy();c.variantOverrides.putAll(variantOverrides);return c;}@Override public String toString(){return name;}}
    public static final class InputAction {public final String key;public String name;public final List<String>bindings=new ArrayList<>();public double deadZone=.2;public InputAction(String key,Collection<String>bindings){this.key=key;this.name=key;this.bindings.addAll(bindings);}public InputAction copy(){InputAction c=new InputAction(key,bindings);c.name=name;c.deadZone=deadZone;return c;}@Override public String toString(){return name;}}
    public static final class PaletteAsset {public final String key;public String name;public final LinkedHashMap<Integer,Integer>colors=new LinkedHashMap<>();public PaletteAsset(String key,String name){this.key=key;this.name=name;}public PaletteAsset copy(){PaletteAsset c=new PaletteAsset(key,name);c.colors.putAll(colors);return c;}@Override public String toString(){return name;}}
    public static final class ParticlePreset {
        public final String key;
        public String name,assetKey="";
        public ParticleRenderMode renderMode=ParticleRenderMode.PIXEL;
        public ParticleShape shape=ParticleShape.SQUARE;
        public Color color=Color.WHITE;
        public double rate=10,lifetime=1,speed=1,spread=45,direction=-90,gravity,startScale=1,endScale=.2,startOpacity=1,endOpacity=0;
        public boolean burst,localSpace=true;
        public int burstCount=12;
        public ParticlePreset(String key,String name){this.key=key;this.name=name==null?key:name;}
        public ParticlePreset copy(){return copyAs(key);}
        public ParticlePreset copyAs(String newKey){ParticlePreset c=new ParticlePreset(newKey,name);c.assetKey=assetKey;c.renderMode=renderMode;c.shape=shape;c.color=color;c.rate=rate;c.lifetime=lifetime;c.speed=speed;c.spread=spread;c.direction=direction;c.gravity=gravity;c.startScale=startScale;c.endScale=endScale;c.startOpacity=startOpacity;c.endOpacity=endOpacity;c.burst=burst;c.localSpace=localSpace;c.burstCount=burstCount;return c;}
        @Override public String toString(){return key;}
    }

    public enum ParticleRenderMode { PIXEL, SPRITE }
    public enum ParticleShape { SQUARE, CIRCLE, DIAMOND }
    public enum BackgroundMode { COLOR, STRETCH, COVER, CONTAIN, TILE }
    public static final class TileLayer {public final String id;public String name;public boolean visible=true,locked=false,collision=true,ySort;public String renderLayer="Suelo",physicsLayer="World";public int order;public double opacity=1,parallaxX=1,parallaxY=1;public String script="# Script de capa\n";public final LinkedHashMap<String,String>variables=new LinkedHashMap<>();private int width,height;private int[]cells;public TileLayer(String id,String name,int width,int height,int order){this.id=id;this.name=name;this.width=width;this.height=height;this.order=order;cells=new int[width*height];Arrays.fill(cells,-1);}public int get(int x,int y){if(x<0||y<0||x>=width||y>=height)return-1;return cells[y*width+x];}public void set(int x,int y,int v){if(!locked&&x>=0&&y>=0&&x<width&&y<height)cells[y*width+x]=v;}public int[]cells(){return cells;}public void replaceCells(int[]v){if(v!=null&&v.length==width*height)cells=v.clone();}public void resize(int nw,int nh){int[]next=new int[nw*nh];Arrays.fill(next,-1);int cw=Math.min(width,nw),ch=Math.min(height,nh);for(int y=0;y<ch;y++)System.arraycopy(cells,y*width,next,y*nw,cw);width=nw;height=nh;cells=next;}public TileLayer copy(){TileLayer c=new TileLayer(id,name,width,height,order);c.visible=visible;c.locked=locked;c.collision=collision;c.ySort=ySort;c.renderLayer=renderLayer;c.physicsLayer=physicsLayer;c.opacity=opacity;c.parallaxX=parallaxX;c.parallaxY=parallaxY;c.cells=cells.clone();c.script=script;c.variables.putAll(variables);return c;}@Override public String toString(){return name;}}

    public static final class Level {public final String id;public String name;public int width,height,spawnX,spawnY;public final List<TileLayer>tileLayers=new ArrayList<>();public final List<EntityDef>entities=new ArrayList<>();public String backgroundAssetKey="";public Color backgroundColor=new Color(16,22,32);public BackgroundMode backgroundMode=BackgroundMode.COLOR;public boolean boundaryLeft,boundaryRight,boundaryTop,boundaryBottom;public String script="# Script de escena\n";public final LinkedHashMap<String,String>variables=new LinkedHashMap<>();public String cameraTarget="";public double cameraZoom=1,cameraOffsetX,cameraOffsetY;public boolean cameraPixelSnap=true;public Level(String id,String name,int width,int height){this.id=id;this.name=name;this.width=Math.max(4,width);this.height=Math.max(4,height);tileLayers.add(new TileLayer("base","Suelo",this.width,this.height,0));}public TileLayer baseLayer(){if(tileLayers.isEmpty())tileLayers.add(new TileLayer("base","Suelo",width,height,0));return tileLayers.getFirst();}public int get(int x,int y){return baseLayer().get(x,y);}public void set(int x,int y,int v){baseLayer().set(x,y,v);}public int[]cells(){return baseLayer().cells();}public void replaceCells(int[]v){baseLayer().replaceCells(v);}public void resize(int nw,int nh){nw=Math.max(4,Math.min(1024,nw));nh=Math.max(4,Math.min(1024,nh));for(TileLayer l:tileLayers)l.resize(nw,nh);width=nw;height=nh;for(EntityDef e:entities){e.x=Math.max(0,Math.min(width-1,e.x));e.y=Math.max(0,Math.min(height-1,e.y));}}public EntityDef entity(String id){return entities.stream().filter(e->e.id.equals(id)).findFirst().orElse(null);}public Level copy(){Level c=new Level(id,name,width,height);c.tileLayers.clear();tileLayers.forEach(l->c.tileLayers.add(l.copy()));entities.forEach(e->c.entities.add(e.copy()));c.backgroundAssetKey=backgroundAssetKey;c.backgroundColor=backgroundColor;c.backgroundMode=backgroundMode;c.boundaryLeft=boundaryLeft;c.boundaryRight=boundaryRight;c.boundaryTop=boundaryTop;c.boundaryBottom=boundaryBottom;c.script=script;c.variables.putAll(variables);c.cameraTarget=cameraTarget;c.cameraZoom=cameraZoom;c.cameraOffsetX=cameraOffsetX;c.cameraOffsetY=cameraOffsetY;c.cameraPixelSnap=cameraPixelSnap;return c;}@Override public String toString(){return name;}}

    public static final class EntityDef {public final String id;public String name;public double x,y,width=.82,height=.82;public boolean enabled=true;public int layer;public String group="",renderLayer="Objetos",physicsLayer="Default",assetKey="",script="# Doble clic sobre una entidad para editar su script.\n",prefabKey="";public final List<ComponentDef>components=new ArrayList<>();public final LinkedHashMap<String,String>variables=new LinkedHashMap<>(),prefabOverrides=new LinkedHashMap<>();public EntityDef(String id,String name,double x,double y){this.id=id;this.name=name;this.x=x;this.y=y;}public ComponentDef component(String type){return components.stream().filter(c->c.type.equals(type)).findFirst().orElse(null);}public boolean has(String type){return component(type)!=null;}public EntityDef copy(){EntityDef c=new EntityDef(id,name,x,y);c.width=width;c.height=height;c.enabled=enabled;c.layer=layer;c.group=group;c.renderLayer=renderLayer;c.physicsLayer=physicsLayer;c.assetKey=assetKey;c.script=script;c.prefabKey=prefabKey;components.forEach(v->c.components.add(v.copy()));c.variables.putAll(variables);c.prefabOverrides.putAll(prefabOverrides);return c;}@Override public String toString(){return name;}}

    public static final class ComponentDef {public final String type;public final LinkedHashMap<String,String>properties=new LinkedHashMap<>();public ComponentDef(String type){this.type=type;}public ComponentDef(String type,Map<String,String>d){this.type=type;properties.putAll(d);}public ComponentDef copy(){return new ComponentDef(type,properties);}public String get(String k,String d){return properties.getOrDefault(k,d);}public double number(String k,double d){try{return Double.parseDouble(get(k,Double.toString(d)));}catch(Exception e){return d;}}public boolean bool(String k,boolean d){return Boolean.parseBoolean(get(k,Boolean.toString(d)));}
        public static ComponentDef preset(String type){LinkedHashMap<String,String>p=new LinkedHashMap<>();p.put("enabled","true");switch(type){
            case"SpriteRenderer"->{p.put("opacity","1");p.put("flipX","false");p.put("flipY","false");p.put("tint","#FFFFFFFF");p.put("palette","");}
            case"Animator"->{p.put("controller","");p.put("animationSet","");p.put("clip","");p.put("state","");p.put("direction","DOWN");p.put("speed","1");p.put("flipX","false");p.put("flipY","false");p.put("frame","0");p.put("finished","false");}
            case"Camera2D"->{p.put("target","");p.put("follow","true");p.put("zoom","1");p.put("zoomSpeed","0");p.put("offsetX","0");p.put("offsetY","0");p.put("pixelSnap","true");p.put("priority","0");p.put("smoothSpeed","0");p.put("deadZoneX","0");p.put("deadZoneY","0");p.put("minX","");p.put("minY","");p.put("maxX","");p.put("maxY","");p.put("shakeIntensity","0");p.put("shakeDuration","0");}
            case"ParticleEmitter2D"->{p.put("preset","");p.put("playing","true");}
            case"Rigidbody2D"->{p.put("mass","1");p.put("gravityScale","1");p.put("drag","0.4");p.put("maxSpeed","12");p.put("freezeX","false");p.put("freezeY","false");p.put("grounded","false");p.put("touchingLeft","false");p.put("touchingRight","false");p.put("touchingTop","false");}
            case"BoxCollider2D"->{p.put("width","0.82");p.put("height","0.82");p.put("offsetX","0");p.put("offsetY","0");p.put("solid","true");}
            case"CircleCollider2D"->{p.put("radius","0.41");p.put("offsetX","0.41");p.put("offsetY","0.41");p.put("solid","true");}
            case"PlayerController"->{p.put("speed","4");p.put("allowArrows","true");}
            case"PlatformerController"->{p.put("speed","5");p.put("jumpSpeed","8");p.put("leftAction","MoveLeft");p.put("rightAction","MoveRight");p.put("jumpAction","Jump");}
            case"GridMovement"->{p.put("step","1");p.put("repeatDelay","0.16");p.put("moveDuration","0");p.put("allowDiagonal","false");p.put("allowArrows","true");p.put("snap","true");}
            case"Patrol"->{p.put("axis","x");p.put("distance","4");p.put("speed","1.5");}
            case"ScenePortal"->{p.put("targetScene","level-1");p.put("targetX","2");p.put("targetY","2");p.put("transition","fade");}
            case"Trigger"->p.put("once","false");case"Health"->{p.put("max","100");p.put("current","100");}case"DamageOnContact"->p.put("damage","10");case"Clickable"->{}case"SortingGroup"->{p.put("order","0");p.put("ySort","false");}
        }return new ComponentDef(type,p);}
        @Override public String toString(){return type;}
    }

    public static final List<String> BUILTIN_COMPONENTS=List.of("SpriteRenderer","Animator","Camera2D","ParticleEmitter2D","GridMovement","PlayerController","PlatformerController","Rigidbody2D","BoxCollider2D","CircleCollider2D","Patrol","ScenePortal","Trigger","Health","DamageOnContact","Clickable","SortingGroup");
    public static String componentDescription(String type){return switch(type){case"SpriteRenderer"->"Opacidad, flip, tinte y paleta del sprite.";case"Animator"->"Reproduce AnimationClip y AnimatorController con estados y eventos.";case"Camera2D"->"Cámara con objetivo, zoom, suavizado, dead zone, shake y pixel snap.";case"ParticleEmitter2D"->"Emisor de partículas basado en un ParticlePreset.";case"GridMovement"->"Movimiento discreto por losetas.";case"PlayerController"->"Movimiento libre continuo con WASD/flechas.";case"PlatformerController"->"Movimiento lateral y salto usando Input Map y grounded.";case"Rigidbody2D"->"Gravedad, inercia, drag, límites y sensores de contacto.";case"BoxCollider2D"->"Collider rectangular sólido o sensor.";case"CircleCollider2D"->"Collider circular; el runtime usa una aproximación robusta para contactos.";case"Patrol"->"Movimiento automático de patrulla.";case"ScenePortal"->"Cambia de escena y aplica transición.";case"Trigger"->"Dispara el evento trigger.";case"Health"->"Vida actual y máxima.";case"DamageOnContact"->"Aplica daño al contacto.";case"Clickable"->"Interacción por clic.";case"SortingGroup"->"Agrupa orden visual y Y-sort.";default->"Comportamiento integrado.";};}

    public enum MenuAction{START_GAME,OPEN_MENU,EXIT}
    public enum MenuAnimation{NONE,PULSE,FLOAT,FADE}
    public enum MenuHoverEffect{NONE,SCALE,GLOW,LIFT}
    public static final class MenuButton {public String text;public int x,y,width,height;public MenuAction action;public String target,assetKey="",hoverAssetKey="",fontKey="",textStyleKey="",uiSkinKey="";public int fontSize=16;public boolean enabled=true;public Color textColor=Color.WHITE,backgroundColor=new Color(31,115,170);public MenuAnimation animation=MenuAnimation.NONE;public MenuHoverEffect hoverEffect=MenuHoverEffect.SCALE;public double animationSpeed=1;public MenuButton(String text,int x,int y,int width,int height,MenuAction action,String target){this.text=text;this.x=x;this.y=y;this.width=width;this.height=height;this.action=action;this.target=target==null?"":target;}public MenuButton copy(){MenuButton c=new MenuButton(text,x,y,width,height,action,target);c.assetKey=assetKey;c.hoverAssetKey=hoverAssetKey;c.fontKey=fontKey;c.textStyleKey=textStyleKey;c.uiSkinKey=uiSkinKey;c.enabled=enabled;c.fontSize=fontSize;c.textColor=textColor;c.backgroundColor=backgroundColor;c.animation=animation;c.hoverEffect=hoverEffect;c.animationSpeed=animationSpeed;return c;}}
    public static final class MenuScreen {public final String id;public String title;public Color background=new Color(19,24,34),titleColor=Color.WHITE;public String backgroundAssetKey="",titleAssetKey="",titleFontKey="",titleTextStyleKey="";public int canvasWidth=640,canvasHeight=480,titleX=30,titleY=30,titleWidth=360,titleHeight=80,titleFontSize=34;public MenuAnimation titleAnimation=MenuAnimation.NONE;public double titleAnimationSpeed=1;public final List<MenuButton>buttons=new ArrayList<>();public MenuScreen(String id,String title){this.id=id;this.title=title;}public MenuScreen copy(){MenuScreen c=new MenuScreen(id,title);c.background=background;c.titleColor=titleColor;c.backgroundAssetKey=backgroundAssetKey;c.titleAssetKey=titleAssetKey;c.titleFontKey=titleFontKey;c.titleTextStyleKey=titleTextStyleKey;c.canvasWidth=canvasWidth;c.canvasHeight=canvasHeight;c.titleX=titleX;c.titleY=titleY;c.titleWidth=titleWidth;c.titleHeight=titleHeight;c.titleFontSize=titleFontSize;c.titleAnimation=titleAnimation;c.titleAnimationSpeed=titleAnimationSpeed;buttons.forEach(b->c.buttons.add(b.copy()));return c;}@Override public String toString(){return title;}}
}
