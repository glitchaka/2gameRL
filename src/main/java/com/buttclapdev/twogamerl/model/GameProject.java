package com.buttclapdev.twogamerl.model;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

public final class GameProject {
    public static final int FORMAT_VERSION = 6;

    private String title = "2gameRL";
    private String startLevel = "level-1";
    private String startMenu = "main";
    private int tileSize = 32;
    private final LinkedHashMap<Integer, TileDef> tiles = new LinkedHashMap<>();
    private final LinkedHashMap<String, Level> levels = new LinkedHashMap<>();
    private final LinkedHashMap<String, MenuScreen> menus = new LinkedHashMap<>();
    private final LinkedHashMap<String, Asset> assets = new LinkedHashMap<>();
    private final LinkedHashMap<String, FontAsset> fonts = new LinkedHashMap<>();
    private final List<String> renderLayers = new ArrayList<>();
    private final LinkedHashMap<String, PhysicsLayerDef> physicsLayers = new LinkedHashMap<>();

    public GameProject() { ensureDefaultLayers(); }

    private void ensureDefaultLayers() {
        if (renderLayers.isEmpty()) renderLayers.addAll(List.of("Fondo", "Suelo", "Objetos", "Personajes", "Frente"));
        if (physicsLayers.isEmpty()) {
            for (String name : List.of("Default", "World", "Player", "Enemy", "Trigger", "Projectile")) physicsLayers.put(name, new PhysicsLayerDef(name));
            for (PhysicsLayerDef a : physicsLayers.values()) a.collidesWith.addAll(physicsLayers.keySet());
        }
    }

    public static GameProject createDefault() {
        GameProject p = new GameProject();
        p.tiles.put(0, new TileDef(0, "Suelo", new Color(42,48,58), true, ""));
        p.tiles.put(1, new TileDef(1, "Muro", new Color(80,88,104), false, ""));
        p.assets.put("placeholder-player.png", new Asset("placeholder-player.png", "placeholder-player.png", placeholderPlayerPng()));
        loadBundledSampleSprites(p);

        Level level = new Level("level-1", "Nivel 1", 24, 16);
        level.backgroundColor = new Color(16,22,32);
        level.backgroundMode = BackgroundMode.COLOR;
        p.levels.put(level.id, level);

        MenuScreen menu = new MenuScreen("main", "2gameRL");
        menu.buttons.add(new MenuButton("Jugar",220,190,200,48,MenuAction.START_GAME,"level-1"));
        menu.buttons.add(new MenuButton("Salir",220,250,200,48,MenuAction.EXIT,""));
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
            project.assets.put(sourceKey, new Asset(sourceKey, "Sample Texture Sheet 32px", bytes, true));
            for (int y=0; y<sheet.getHeight()/32; y++) for (int x=0; x<sheet.getWidth()/32; x++) {
                String key = String.format(Locale.ROOT, "sample-%02d-%02d", x, y);
                project.assets.put(key, new Asset(key, String.format(Locale.ROOT, "Sample Sprite %d,%d", x, y), sourceKey, x*32, y*32, 32, 32));
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
        GameProject c=new GameProject();c.title=title;c.startLevel=startLevel;c.startMenu=startMenu;c.tileSize=tileSize;
        c.renderLayers.clear();c.renderLayers.addAll(renderLayers);c.physicsLayers.clear();physicsLayers.forEach((k,v)->c.physicsLayers.put(k,v.copy()));
        tiles.forEach((k,v)->c.tiles.put(k,v.copy()));levels.forEach((k,v)->c.levels.put(k,v.copy()));menus.forEach((k,v)->c.menus.put(k,v.copy()));assets.forEach((k,v)->c.assets.put(k,v.copy()));fonts.forEach((k,v)->c.fonts.put(k,v.copy()));return c;
    }

    public String getTitle(){return title;} public void setTitle(String v){title=v==null||v.isBlank()?"2gameRL":v.trim();}
    public String getStartLevel(){return startLevel;} public void setStartLevel(String v){startLevel=v;}
    public String getStartMenu(){return startMenu;} public void setStartMenu(String v){startMenu=v;}
    public int getTileSize(){return tileSize;} public void setTileSize(int v){tileSize=Math.max(8,Math.min(128,v));}
    public Map<Integer,TileDef> getTiles(){return tiles;} public Map<String,Level> getLevels(){return levels;} public Map<String,MenuScreen> getMenus(){return menus;} public Map<String,Asset> getAssets(){return assets;} public Map<String,FontAsset> getFonts(){return fonts;}
    public List<String> getRenderLayers(){return renderLayers;} public Map<String,PhysicsLayerDef> getPhysicsLayers(){return physicsLayers;}
    public List<String> getDrawableAssetKeys(){return assets.values().stream().filter(a->!a.sourceOnly||a.isRegion()).map(a->a.key).toList();}
    public int nextTileId(){return tiles.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1)+1;}
    public int renderOrder(String name){int i=renderLayers.indexOf(name);return i<0?Math.max(0,renderLayers.indexOf("Objetos")):i;}
    public boolean canCollide(String a,String b){PhysicsLayerDef aa=physicsLayers.get(a),bb=physicsLayers.get(b);if(aa==null||bb==null)return true;return aa.collidesWith.contains(b)&&bb.collidesWith.contains(a);}
    public void setCollision(String a,String b,boolean value){PhysicsLayerDef aa=physicsLayers.get(a),bb=physicsLayers.get(b);if(aa==null||bb==null)return;if(value){aa.collidesWith.add(b);bb.collidesWith.add(a);}else{aa.collidesWith.remove(b);bb.collidesWith.remove(a);}}

    public static final class PhysicsLayerDef {
        public String name; public final LinkedHashSet<String> collidesWith=new LinkedHashSet<>();
        public PhysicsLayerDef(String name){this.name=name;} public PhysicsLayerDef copy(){PhysicsLayerDef c=new PhysicsLayerDef(name);c.collidesWith.addAll(collidesWith);return c;} @Override public String toString(){return name;}
    }

    public static final class TileDef {
        public final int id; public String name; public Color color; public boolean walkable; public String assetKey;
        public TileDef(int id,String name,Color color,boolean walkable,String assetKey){this.id=id;this.name=name;this.color=color;this.walkable=walkable;this.assetKey=assetKey==null?"":assetKey;}
        public TileDef copy(){return new TileDef(id,name,color,walkable,assetKey);} @Override public String toString(){return id+" · "+name;}
    }

    public static final class Asset {
        public final String key;
        public String sourceName;
        public byte[] data;
        public boolean sourceOnly;
        public String sourceAssetKey="";
        public int regionX,regionY,regionWidth,regionHeight;
        private transient BufferedImage image;

        public Asset(String key,String sourceName,byte[] data){this(key,sourceName,data,false);}
        public Asset(String key,String sourceName,byte[] data,boolean sourceOnly){this.key=key;this.sourceName=sourceName;this.data=data;this.sourceOnly=sourceOnly;}
        public Asset(String key,String sourceName,String sourceAssetKey,int x,int y,int width,int height){this.key=key;this.sourceName=sourceName;this.sourceAssetKey=sourceAssetKey==null?"":sourceAssetKey;this.regionX=Math.max(0,x);this.regionY=Math.max(0,y);this.regionWidth=Math.max(1,width);this.regionHeight=Math.max(1,height);}
        public boolean isRegion(){return !sourceAssetKey.isBlank()&&regionWidth>0&&regionHeight>0;}
        public BufferedImage image(){if(image==null&&data!=null){try{image=ImageIO.read(new ByteArrayInputStream(data));}catch(Exception ignored){}}return image;}
        public Asset copy(){Asset c=isRegion()?new Asset(key,sourceName,sourceAssetKey,regionX,regionY,regionWidth,regionHeight):new Asset(key,sourceName,data==null?null:data.clone(),sourceOnly);c.sourceOnly=sourceOnly;return c;}
        @Override public String toString(){return (sourceOnly&&!isRegion()?"[Hoja] ":isRegion()?"[Región] ":"")+(sourceName==null?key:sourceName);}
    }

    public static final class FontAsset {
        public final String key; public String sourceName; public byte[] data;
        public FontAsset(String key,String sourceName,byte[] data){this.key=key;this.sourceName=sourceName;this.data=data;}
        public FontAsset copy(){return new FontAsset(key,sourceName,data==null?null:data.clone());}
        @Override public String toString(){return sourceName==null?key:sourceName;}
    }

    public enum BackgroundMode { COLOR, STRETCH, COVER, CONTAIN, TILE }

    public static final class TileLayer {
        public final String id; public String name; public boolean visible=true,locked=false,collision=true; public String renderLayer="Suelo",physicsLayer="World"; public int order;
        public String script="# Script de capa\n"; public final LinkedHashMap<String,String> variables=new LinkedHashMap<>();
        private int width,height; private int[]cells;
        public TileLayer(String id,String name,int width,int height,int order){this.id=id;this.name=name;this.width=width;this.height=height;this.order=order;this.cells=new int[width*height];Arrays.fill(this.cells,-1);}
        public int get(int x,int y){if(x<0||y<0||x>=width||y>=height)return-1;return cells[y*width+x];} public void set(int x,int y,int v){if(!locked&&x>=0&&y>=0&&x<width&&y<height)cells[y*width+x]=v;}
        public int[]cells(){return cells;} public void replaceCells(int[]v){if(v!=null&&v.length==width*height)cells=v.clone();}
        public void resize(int nw,int nh){int[]next=new int[nw*nh];Arrays.fill(next,-1);int cw=Math.min(width,nw),ch=Math.min(height,nh);for(int y=0;y<ch;y++)System.arraycopy(cells,y*width,next,y*nw,cw);width=nw;height=nh;cells=next;}
        public TileLayer copy(){TileLayer c=new TileLayer(id,name,width,height,order);c.visible=visible;c.locked=locked;c.collision=collision;c.renderLayer=renderLayer;c.physicsLayer=physicsLayer;c.cells=cells.clone();c.script=script;c.variables.putAll(variables);return c;} @Override public String toString(){return name;}
    }

    public static final class Level {
        public final String id; public String name; public int width,height; public int spawnX,spawnY; public final List<TileLayer> tileLayers=new ArrayList<>(); public final List<EntityDef>entities=new ArrayList<>();
        public String backgroundAssetKey=""; public Color backgroundColor=new Color(16,22,32); public BackgroundMode backgroundMode=BackgroundMode.COLOR;
        public boolean boundaryLeft=false,boundaryRight=false,boundaryTop=false,boundaryBottom=false;
        public String script="# Script de escena\n"; public final LinkedHashMap<String,String> variables=new LinkedHashMap<>();
        public Level(String id,String name,int width,int height){this.id=id;this.name=name;this.width=Math.max(4,width);this.height=Math.max(4,height);tileLayers.add(new TileLayer("base","Suelo",this.width,this.height,0));}
        public TileLayer baseLayer(){if(tileLayers.isEmpty())tileLayers.add(new TileLayer("base","Suelo",width,height,0));return tileLayers.getFirst();}
        public int get(int x,int y){return baseLayer().get(x,y);} public void set(int x,int y,int v){baseLayer().set(x,y,v);} public int[]cells(){return baseLayer().cells();} public void replaceCells(int[]v){baseLayer().replaceCells(v);}
        public void resize(int nw,int nh){nw=Math.max(4,Math.min(256,nw));nh=Math.max(4,Math.min(256,nh));for(TileLayer l:tileLayers)l.resize(nw,nh);width=nw;height=nh;for(EntityDef e:entities){e.x=Math.max(0,Math.min(width-1,e.x));e.y=Math.max(0,Math.min(height-1,e.y));}}
        public EntityDef entity(String id){return entities.stream().filter(e->e.id.equals(id)).findFirst().orElse(null);} public Level copy(){Level c=new Level(id,name,width,height);c.tileLayers.clear();tileLayers.forEach(l->c.tileLayers.add(l.copy()));entities.forEach(e->c.entities.add(e.copy()));c.backgroundAssetKey=backgroundAssetKey;c.backgroundColor=backgroundColor;c.backgroundMode=backgroundMode;c.boundaryLeft=boundaryLeft;c.boundaryRight=boundaryRight;c.boundaryTop=boundaryTop;c.boundaryBottom=boundaryBottom;c.script=script;c.variables.putAll(variables);return c;} @Override public String toString(){return name;}
    }

    public static final class EntityDef {
        public final String id; public String name; public double x,y,width=.82,height=.82; public boolean enabled=true; public int layer=0;
        public String group="",renderLayer="Objetos",physicsLayer="Default",assetKey="",script="# Doble clic sobre una entidad para editar su script.\n";
        public final List<ComponentDef>components=new ArrayList<>(); public final LinkedHashMap<String,String>variables=new LinkedHashMap<>();
        public EntityDef(String id,String name,double x,double y){this.id=id;this.name=name;this.x=x;this.y=y;}
        public ComponentDef component(String type){return components.stream().filter(c->c.type.equals(type)).findFirst().orElse(null);} public boolean has(String type){return component(type)!=null;}
        public EntityDef copy(){EntityDef c=new EntityDef(id,name,x,y);c.width=width;c.height=height;c.enabled=enabled;c.layer=layer;c.group=group;c.renderLayer=renderLayer;c.physicsLayer=physicsLayer;c.assetKey=assetKey;c.script=script;components.forEach(v->c.components.add(v.copy()));c.variables.putAll(variables);return c;} @Override public String toString(){return name;}
    }

    public static final class ComponentDef {
        public final String type; public final LinkedHashMap<String,String>properties=new LinkedHashMap<>();
        public ComponentDef(String type){this.type=type;} public ComponentDef(String type,Map<String,String>d){this.type=type;properties.putAll(d);} public ComponentDef copy(){return new ComponentDef(type,properties);}
        public String get(String k,String d){return properties.getOrDefault(k,d);} public double number(String k,double d){try{return Double.parseDouble(get(k,Double.toString(d)));}catch(Exception e){return d;}} public boolean bool(String k,boolean d){return Boolean.parseBoolean(get(k,Boolean.toString(d)));}
        public static ComponentDef preset(String type){
            LinkedHashMap<String,String>p=new LinkedHashMap<>();p.put("enabled","true");
            switch(type){
                case"Rigidbody2D"->{p.put("mass","1");p.put("gravityScale","1");p.put("drag","0.4");p.put("maxSpeed","12");}
                case"BoxCollider2D"->{p.put("width","0.82");p.put("height","0.82");p.put("solid","true");}
                case"PlayerController"->{p.put("speed","4");p.put("allowArrows","true");}
                case"GridMovement"->{p.put("step","1");p.put("repeatDelay","0.16");p.put("moveDuration","0");p.put("allowDiagonal","false");p.put("allowArrows","true");p.put("snap","true");}
                case"Patrol"->{p.put("axis","x");p.put("distance","4");p.put("speed","1.5");}
                case"ScenePortal"->{p.put("targetScene","level-1");p.put("targetX","2");p.put("targetY","2");}
                case"Trigger"->p.put("once","false");
                case"Health"->{p.put("max","100");p.put("current","100");}
                case"DamageOnContact"->p.put("damage","10");
                case"Clickable"->{}
            }
            return new ComponentDef(type,p);
        }
        @Override public String toString(){return type;}
    }

    public static final List<String> BUILTIN_COMPONENTS=List.of("GridMovement","PlayerController","Rigidbody2D","BoxCollider2D","Patrol","ScenePortal","Trigger","Health","DamageOnContact","Clickable");
    public static String componentDescription(String type){return switch(type){
        case"GridMovement"->"Movimiento discreto por losetas. Configura paso, repetición, transición, diagonal y snap.";
        case"Rigidbody2D"->"Gravedad, inercia, drag y velocidad máxima.";
        case"BoxCollider2D"->"Colisiona por sí solo contra tiles no transitables y otros BoxCollider2D sólidos. Las capas físicas solo filtran contactos.";
        case"PlayerController"->"Movimiento libre continuo con WASD/flechas.";
        case"Patrol"->"Movimiento automático de patrulla.";
        case"ScenePortal"->"Cambia de escena al contactar con un PlayerController o GridMovement.";
        case"Trigger"->"Dispara el evento trigger.";
        case"Health"->"Vida actual y máxima.";
        case"DamageOnContact"->"Aplica daño al contacto.";
        case"Clickable"->"Habilita o deshabilita interacción por clic.";
        default->"Comportamiento integrado.";
    };}

    public enum MenuAction{START_GAME,OPEN_MENU,EXIT}
    public enum MenuAnimation{NONE,PULSE,FLOAT,FADE}
    public enum MenuHoverEffect{NONE,SCALE,GLOW,LIFT}

    public static final class MenuButton {
        public String text; public int x,y,width,height; public MenuAction action; public String target,assetKey="",hoverAssetKey="",fontKey=""; public int fontSize=16;
        public Color textColor=Color.WHITE,backgroundColor=new Color(31,115,170); public MenuAnimation animation=MenuAnimation.NONE; public MenuHoverEffect hoverEffect=MenuHoverEffect.SCALE; public double animationSpeed=1;
        public MenuButton(String text,int x,int y,int width,int height,MenuAction action,String target){this.text=text;this.x=x;this.y=y;this.width=width;this.height=height;this.action=action;this.target=target==null?"":target;}
        public MenuButton copy(){MenuButton c=new MenuButton(text,x,y,width,height,action,target);c.assetKey=assetKey;c.hoverAssetKey=hoverAssetKey;c.fontKey=fontKey;c.fontSize=fontSize;c.textColor=textColor;c.backgroundColor=backgroundColor;c.animation=animation;c.hoverEffect=hoverEffect;c.animationSpeed=animationSpeed;return c;}
    }

    public static final class MenuScreen {
        public final String id; public String title; public Color background=new Color(19,24,34),titleColor=Color.WHITE; public String backgroundAssetKey="",titleAssetKey="",titleFontKey="";
        public int canvasWidth=640,canvasHeight=480,titleX=30,titleY=30,titleWidth=360,titleHeight=80,titleFontSize=34; public MenuAnimation titleAnimation=MenuAnimation.NONE; public double titleAnimationSpeed=1; public final List<MenuButton>buttons=new ArrayList<>();
        public MenuScreen(String id,String title){this.id=id;this.title=title;}
        public MenuScreen copy(){MenuScreen c=new MenuScreen(id,title);c.background=background;c.titleColor=titleColor;c.backgroundAssetKey=backgroundAssetKey;c.titleAssetKey=titleAssetKey;c.titleFontKey=titleFontKey;c.canvasWidth=canvasWidth;c.canvasHeight=canvasHeight;c.titleX=titleX;c.titleY=titleY;c.titleWidth=titleWidth;c.titleHeight=titleHeight;c.titleFontSize=titleFontSize;c.titleAnimation=titleAnimation;c.titleAnimationSpeed=titleAnimationSpeed;buttons.forEach(b->c.buttons.add(b.copy()));return c;}
        @Override public String toString(){return title;}
    }
}
