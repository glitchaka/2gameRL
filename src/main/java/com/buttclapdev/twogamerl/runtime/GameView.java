package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.script.ScriptProgram;
import javafx.animation.AnimationTimer;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

public final class GameView extends StackPane {
    private final GameProject project;
    private final Canvas canvas = new Canvas(960, 640);
    private final StackPane menuLayer = new StackPane();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Set<KeyCode> keys = EnumSet.noneOf(KeyCode.class);
    private final Set<KeyCode> pressed = EnumSet.noneOf(KeyCode.class);
    private final LinkedHashMap<String, Body> bodies = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> globals = new LinkedHashMap<>();
    private final PriorityQueue<ScheduledAction> scheduled = new PriorityQueue<>(Comparator.comparingDouble(ScheduledAction::due).thenComparingLong(ScheduledAction::order));
    private final AnimationTimer timer;
    private Consumer<String> logger = System.out::println;
    private Level level;
    private PendingScene pendingScene;
    private boolean playing;
    private long lastTime;
    private long scheduledSequence;
    private long runtimeEntitySequence;
    private double runtimeSeconds;
    private Set<String> contacts = new HashSet<>();
    private Set<String> frameContacts = new HashSet<>();

    public GameView(GameProject source) {
        project = source.deepCopy();
        setStyle("-fx-background-color:#0b0f17;");
        getChildren().addAll(canvas, menuLayer);
        canvas.getGraphicsContext2D().setImageSmoothing(false);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        setFocusTraversable(true);
        setOnKeyPressed(e -> { if (keys.add(e.getCode())) pressed.add(e.getCode()); e.consume(); });
        setOnKeyReleased(e -> { keys.remove(e.getCode()); e.consume(); });
        setOnMouseClicked(e -> {
            if (!playing) return;
            Body hit = hitEntity(e.getX(), e.getY());
            if (hit != null && clickable(hit)) fire(hit, e.getClickCount() >= 2 ? ScriptProgram.Event.DOUBLECLICK : ScriptProgram.Event.CLICK, null);
            requestFocus();
        });
        widthProperty().addListener((o,a,b) -> render());
        heightProperty().addListener((o,a,b) -> render());
        timer = new AnimationTimer() {
            @Override public void handle(long now) {
                if (lastTime == 0) lastTime = now;
                double dt = Math.min(.05, (now - lastTime) / 1_000_000_000.0);
                lastTime = now;
                if (playing) update(dt);
                render();
                pressed.clear();
            }
        };
        timer.start();
        showInitialMenu();
    }

    public void setLogger(Consumer<String> logger) { this.logger = logger == null ? System.out::println : logger; }
    public void stop() { timer.stop(); }

    private void showInitialMenu() {
        String start = project.getStartMenu();
        if (start == null || start.isBlank() || !project.getMenus().containsKey(start)) startGame(null);
        else showMenu(start);
    }

    private boolean showMenu(String id) {
        MenuScreen menu = project.getMenus().get(id);
        if (menu == null) {
            logger.accept("[Menu] No existe la pantalla de menú '" + id + "'.");
            return false;
        }
        playing = false;
        menuLayer.getChildren().clear();
        Pane board = new Pane();
        board.setPrefSize(menu.canvasWidth, menu.canvasHeight);
        board.setMinSize(menu.canvasWidth, menu.canvasHeight);
        board.setMaxSize(menu.canvasWidth, menu.canvasHeight);
        java.awt.Color bg = menu.background;
        board.setStyle(String.format(Locale.ROOT, "-fx-background-color:rgba(%d,%d,%d,%.3f);", bg.getRed(), bg.getGreen(), bg.getBlue(), bg.getAlpha()/255.0));
        if (!menu.backgroundAssetKey.isBlank()) {
            Image img = image(menu.backgroundAssetKey);
            if (img != null) board.getChildren().add(pixelImage(img, menu.canvasWidth, menu.canvasHeight, true));
        }
        Node title = menuTitle(menu); title.setLayoutX(menu.titleX); title.setLayoutY(menu.titleY); board.getChildren().add(title);
        for (MenuButton def : menu.buttons) {
            Node button = menuButton(def); button.setLayoutX(def.x); button.setLayoutY(def.y); button.setCursor(Cursor.HAND);
            button.setOnMouseClicked(e -> {
                switch (def.action) {
                    case START_GAME -> startGame(def.target);
                    case OPEN_MENU -> showMenu(def.target);
                    case EXIT -> { var w = getScene() == null ? null : getScene().getWindow(); if (w != null) w.hide(); }
                }
                e.consume();
            });
            board.getChildren().add(button);
        }
        DoubleBinding scale = Bindings.createDoubleBinding(() -> {
            double raw = Math.min(menuLayer.getWidth()/Math.max(1,menu.canvasWidth), menuLayer.getHeight()/Math.max(1,menu.canvasHeight));
            return raw >= 1 ? Math.max(1, Math.floor(raw)) : Math.max(.1, raw);
        }, menuLayer.widthProperty(), menuLayer.heightProperty());
        board.scaleXProperty().bind(scale); board.scaleYProperty().bind(scale);
        menuLayer.getChildren().add(board); StackPane.setAlignment(board, Pos.CENTER);
        return true;
    }

    private ImageView pixelImage(Image image, double width, double height, boolean mouseTransparent) {
        ImageView iv = new ImageView(image);
        iv.setSmooth(false); iv.setCache(false); iv.setPreserveRatio(false); iv.setFitWidth(Math.max(1, Math.rint(width))); iv.setFitHeight(Math.max(1, Math.rint(height))); iv.setMouseTransparent(mouseTransparent);
        return iv;
    }

    private Node menuTitle(MenuScreen menu) {
        StackPane content = new StackPane(); content.setPrefSize(menu.titleWidth, menu.titleHeight); content.setMinSize(menu.titleWidth, menu.titleHeight); content.setMaxSize(menu.titleWidth, menu.titleHeight);
        if (!menu.titleAssetKey.isBlank()) { Image img = image(menu.titleAssetKey); if (img != null) content.getChildren().add(pixelImage(img, menu.titleWidth, menu.titleHeight, true)); }
        if (menu.title != null && !menu.title.isBlank()) {
            Label label = new Label(menu.title); label.setFont(Font.font(Math.max(8, menu.titleFontSize))); label.setTextFill(fx(menu.titleColor)); label.setWrapText(true); label.setAlignment(Pos.CENTER); label.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE); content.getChildren().add(label);
        }
        return MenuEffects.decorate(content, menu.titleAnimation, menu.titleAnimationSpeed, MenuHoverEffect.NONE);
    }

    private Node menuButton(MenuButton def) {
        StackPane content = new StackPane(); content.setPrefSize(def.width, def.height); content.setMinSize(def.width, def.height); content.setMaxSize(def.width, def.height);
        Image normal = def.assetKey.isBlank() ? null : image(def.assetKey), hover = def.hoverAssetKey.isBlank() ? null : image(def.hoverAssetKey);
        ImageView iv = normal == null ? null : pixelImage(normal, def.width, def.height, false);
        if (iv != null) content.getChildren().add(iv);
        else {
            java.awt.Color c = def.backgroundColor;
            content.setStyle(String.format(Locale.ROOT, "-fx-background-color:rgba(%d,%d,%d,%.3f);-fx-background-radius:7;-fx-border-color:#4b6a84;-fx-border-radius:7;", c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0));
        }
        Label label = new Label(def.text); label.setFont(Font.font(Math.max(8,def.fontSize))); label.setTextFill(fx(def.textColor)); label.setMouseTransparent(true); content.getChildren().add(label);
        StackPane wrapper = MenuEffects.decorate(content, def.animation, def.animationSpeed, def.hoverEffect);
        if (iv != null && hover != null) {
            content.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> iv.setImage(hover));
            content.addEventHandler(MouseEvent.MOUSE_EXITED, e -> iv.setImage(normal));
        }
        return wrapper;
    }

    private static Color fx(java.awt.Color c) { return Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0); }

    private void startGame(String requestedScene) {
        menuLayer.getChildren().clear();
        String scene = requestedScene == null || requestedScene.isBlank() ? project.getStartLevel() : requestedScene;
        if (!project.getLevels().containsKey(scene)) {
            logger.accept("[Menu] La escena destino '" + scene + "' no existe; se usará la escena inicial.");
            scene = project.getStartLevel();
        }
        loadScene(scene, null, null);
        playing = true;
        requestFocus();
    }

    private void loadScene(String id, Double targetX, Double targetY) {
        Level next = project.getLevels().get(id);
        if (next == null) { logger.accept("[Runtime] No existe la escena '" + id + "'."); return; }
        level = next;
        bodies.clear(); contacts.clear(); frameContacts.clear(); scheduled.clear();
        runtimeSeconds = 0; scheduledSequence = 0; runtimeEntitySequence = 0;
        for (EntityDef def : level.entities) {
            Body body = new Body(def.copy()); normalizeHealth(body); bodies.put(def.id, body);
        }
        if (targetX != null && targetY != null) {
            bodies.values().stream().filter(b -> activeComponent(b,"PlayerController") != null).findFirst().ifPresent(b -> {
                b.def.x = targetX; b.def.y = targetY; b.originX = targetX; b.originY = targetY;
                logger.accept("[Runtime] Player positioned @ " + number(targetX) + "," + number(targetY));
            });
        }
        resolveInitialPenetrations();
        for (Body body : List.copyOf(bodies.values())) fire(body, ScriptProgram.Event.START, null);
    }

    private void normalizeHealth(Body body) {
        ComponentDef health = activeComponent(body,"Health"); if (health == null) return;
        double max = Math.max(0,health.number("max",100)), current = Math.max(0,Math.min(max,health.number("current",max)));
        health.properties.put("max",number(max)); health.properties.put("current",number(current));
    }

    private void update(double dt) {
        if (level == null) return;
        runtimeSeconds += Math.max(0, dt);
        runScheduledActions();
        frameContacts = new HashSet<>();
        for (Body body : List.copyOf(bodies.values())) {
            if (body.destroyed || !body.def.enabled) continue;
            applyBuiltins(body,dt); fire(body,ScriptProgram.Event.UPDATE,null); integrate(body,dt);
        }
        checkEntityOverlaps();
        contacts = frameContacts;
        bodies.values().removeIf(b -> b.destroyed);
        if (pendingScene != null) { PendingScene target = pendingScene; pendingScene = null; loadScene(target.id,target.x,target.y); }
    }

    private void runScheduledActions() {
        int guard = 0;
        while (!scheduled.isEmpty() && scheduled.peek().due <= runtimeSeconds + 1e-9 && guard++ < 10000) {
            ScheduledAction action = scheduled.poll();
            try { action.action.run(); } catch (RuntimeException ex) { logger.accept("[Script timer] " + ex.getMessage()); }
        }
        if (guard >= 10000) logger.accept("[Script timer] Se alcanzó el límite de acciones programadas en un frame.");
    }

    private void schedule(double seconds, Runnable action) {
        if (action != null) scheduled.add(new ScheduledAction(runtimeSeconds + Math.max(0,seconds), ++scheduledSequence, action));
    }

    private void applyBuiltins(Body body, double dt) {
        ComponentDef controller = activeComponent(body,"PlayerController");
        if (controller != null) {
            double dx=0,dy=0;
            if (isDown("W") || (controller.bool("allowArrows",true)&&isDown("UP"))) dy--;
            if (isDown("S") || (controller.bool("allowArrows",true)&&isDown("DOWN"))) dy++;
            if (isDown("A") || (controller.bool("allowArrows",true)&&isDown("LEFT"))) dx--;
            if (isDown("D") || (controller.bool("allowArrows",true)&&isDown("RIGHT"))) dx++;
            double len=Math.hypot(dx,dy); if(len>0){dx/=len;dy/=len;} double speed=Math.max(0,controller.number("speed",4)); body.vx=dx*speed; body.vy=dy*speed;
        }
        ComponentDef patrol = activeComponent(body,"Patrol");
        if (patrol != null && controller == null) {
            double speed=Math.max(0,patrol.number("speed",1.5)),distance=Math.max(.05,patrol.number("distance",4)); boolean vertical=patrol.get("axis","x").equalsIgnoreCase("y"); double position=vertical?body.def.y:body.def.x,origin=vertical?body.originY:body.originX;
            if(body.patrolDirection>0&&position>=origin+distance)body.patrolDirection=-1;else if(body.patrolDirection<0&&position<=origin)body.patrolDirection=1;
            if(vertical)body.vy=body.patrolDirection*speed;else body.vx=body.patrolDirection*speed;
        }
        ComponentDef rigid = activeComponent(body,"Rigidbody2D");
        if (rigid != null) {
            double mass=Math.max(.01,rigid.number("mass",1)); body.vy+=9.81*rigid.number("gravityScale",1)*dt; double drag=Math.max(0,rigid.number("drag",.4)),factor=Math.max(0,1-(drag/mass)*dt);
            if(controller==null&&(patrol==null||patrol.get("axis","x").equalsIgnoreCase("y")))body.vx*=factor;
            if(controller==null&&(patrol==null||patrol.get("axis","x").equalsIgnoreCase("x")))body.vy*=factor;
            double max=Math.max(.1,rigid.number("maxSpeed",12)),speed=Math.hypot(body.vx,body.vy); if(speed>max){body.vx=body.vx/speed*max;body.vy=body.vy/speed*max;}
        }
    }

    private void integrate(Body body,double dt) { if(Math.abs(body.vx)>1e-9)moveBody(body,body.vx*dt,0,true);if(Math.abs(body.vy)>1e-9)moveBody(body,0,body.vy*dt,true); }

    private void moveBody(Body body,double dx,double dy,boolean collisionEvent) {
        ComponentDef collider=activeComponent(body,"BoxCollider2D"); double nx=body.def.x+dx,ny=body.def.y+dy;
        if(collider==null||!collider.bool("solid",true)){body.def.x=nx;body.def.y=ny;return;}
        Block block=blocked(body,nx,ny); if(block==null){body.def.x=nx;body.def.y=ny;return;}
        double oldVx=body.vx,oldVy=body.vy;if(activeComponent(body,"Patrol")!=null)body.patrolDirection*=-1;
        if(collisionEvent){if(block.other!=null)processContact(body,block.other,true);else fire(body,ScriptProgram.Event.COLLISION,null);}
        if(dx!=0&&Double.compare(body.vx,oldVx)==0)body.vx=0;if(dy!=0&&Double.compare(body.vy,oldVy)==0)body.vy=0;
    }

    private Block blocked(Body body,double x,double y) {
        ComponentDef collider=activeComponent(body,"BoxCollider2D"); if(collider==null||!collider.bool("solid",true))return null;
        double w=Math.max(.01,collider.number("width",body.def.width)),h=Math.max(.01,collider.number("height",body.def.height)); int minX=(int)Math.floor(x),minY=(int)Math.floor(y),maxX=(int)Math.floor(x+w-.001),maxY=(int)Math.floor(y+h-.001);
        if(minX<0||minY<0||maxX>=level.width||maxY>=level.height)return new Block(null);
        for(TileLayer layer:level.tileLayers){if(!layer.collision||!project.canCollide(body.def.physicsLayer,layer.physicsLayer))continue;for(int ty=minY;ty<=maxY;ty++)for(int tx=minX;tx<=maxX;tx++){int id=layer.get(tx,ty);if(id<0)continue;TileDef tile=project.getTiles().get(id);if(tile!=null&&!tile.walkable)return new Block(null);}}
        for(Body other:bodies.values()){if(other==body||other.destroyed||!other.def.enabled||!project.canCollide(body.def.physicsLayer,other.def.physicsLayer))continue;ComponentDef oc=activeComponent(other,"BoxCollider2D");if(oc==null||!oc.bool("solid",true))continue;double ow=Math.max(.01,oc.number("width",other.def.width)),oh=Math.max(.01,oc.number("height",other.def.height));if(overlaps(x,y,w,h,other.def.x,other.def.y,ow,oh))return new Block(other);}
        return null;
    }

    private void resolveInitialPenetrations(){for(int pass=0;pass<4;pass++){boolean changed=false;List<Body>all=new ArrayList<>(bodies.values());for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++){Body a=all.get(i),b=all.get(j);if(solidPair(a,b)&&intersects(a,b)){resolvePenetration(a,b);changed=true;}}if(!changed)break;}}
    private void checkEntityOverlaps(){List<Body>all=new ArrayList<>(bodies.values());for(int i=0;i<all.size();i++)for(int j=i+1;j<all.size();j++){Body a=all.get(i),b=all.get(j);if(a.destroyed||b.destroyed||!a.def.enabled||!b.def.enabled||!project.canCollide(a.def.physicsLayer,b.def.physicsLayer)||!intersects(a,b))continue;boolean solid=solidPair(a,b);processContact(a,b,solid);if(solid&&intersects(a,b))resolvePenetration(a,b);}}
    private boolean solidPair(Body a,Body b){ComponentDef ac=activeComponent(a,"BoxCollider2D"),bc=activeComponent(b,"BoxCollider2D");return ac!=null&&bc!=null&&ac.bool("solid",true)&&bc.bool("solid",true)&&project.canCollide(a.def.physicsLayer,b.def.physicsLayer);}
    private void resolvePenetration(Body a,Body b){double aw=colliderWidth(a),ah=colliderHeight(a),bw=colliderWidth(b),bh=colliderHeight(b),ox=Math.min(a.def.x+aw,b.def.x+bw)-Math.max(a.def.x,b.def.x),oy=Math.min(a.def.y+ah,b.def.y+bh)-Math.max(a.def.y,b.def.y);if(ox<=0||oy<=0)return;boolean ma=isMovable(a),mb=isMovable(b);if(!ma&&!mb)return;double eps=.001;if(ox<oy){double dir=(a.def.x+aw/2)<(b.def.x+bw/2)?-1:1,amount=ox+eps;if(ma&&mb){a.def.x+=dir*amount/2;b.def.x-=dir*amount/2;}else if(ma)a.def.x+=dir*amount;else b.def.x-=dir*amount;a.vx=0;b.vx=0;}else{double dir=(a.def.y+ah/2)<(b.def.y+bh/2)?-1:1,amount=oy+eps;if(ma&&mb){a.def.y+=dir*amount/2;b.def.y-=dir*amount/2;}else if(ma)a.def.y+=dir*amount;else b.def.y-=dir*amount;a.vy=0;b.vy=0;}}
    private boolean isMovable(Body b){return activeComponent(b,"Rigidbody2D")!=null||activeComponent(b,"PlayerController")!=null||activeComponent(b,"Patrol")!=null;}

    private void processContact(Body a,Body b,boolean colliderContact){if(!project.canCollide(a.def.physicsLayer,b.def.physicsLayer))return;String key=contactKey(a,b);boolean first=frameContacts.add(key),entered=first&&!contacts.contains(key);if(!entered)return;if(colliderContact){fire(a,ScriptProgram.Event.COLLISION,b);fire(b,ScriptProgram.Event.COLLISION,a);}handlePortal(a,b);handlePortal(b,a);handleTrigger(a,b);handleTrigger(b,a);handleDamage(a,b);handleDamage(b,a);}
    private String contactKey(Body a,Body b){return a.def.id.compareTo(b.def.id)<=0?a.def.id+"\u0000"+b.def.id:b.def.id+"\u0000"+a.def.id;}
    private boolean intersects(Body a,Body b){return overlaps(a.def.x,a.def.y,colliderWidth(a),colliderHeight(a),b.def.x,b.def.y,colliderWidth(b),colliderHeight(b));}
    private static boolean overlaps(double ax,double ay,double aw,double ah,double bx,double by,double bw,double bh){return ax<bx+bw&&ax+aw>bx&&ay<by+bh&&ay+ah>by;}
    private double colliderWidth(Body b){ComponentDef c=activeComponent(b,"BoxCollider2D");return c==null?Math.max(.01,b.def.width):Math.max(.01,c.number("width",b.def.width));}
    private double colliderHeight(Body b){ComponentDef c=activeComponent(b,"BoxCollider2D");return c==null?Math.max(.01,b.def.height):Math.max(.01,c.number("height",b.def.height));}

    private void handlePortal(Body portal,Body other){ComponentDef p=activeComponent(portal,"ScenePortal");if(p==null||activeComponent(other,"PlayerController")==null||pendingScene!=null)return;String scene=p.get("targetScene",project.getStartLevel());double x=p.number("targetX",2),y=p.number("targetY",2);pendingScene=new PendingScene(scene,x,y);logger.accept("["+portal.def.name+"] ScenePortal -> "+scene+" @ "+number(x)+","+number(y));}
    private void handleTrigger(Body trigger,Body other){ComponentDef t=activeComponent(trigger,"Trigger");if(t==null||activeComponent(other,"PlayerController")==null||trigger.triggerConsumed)return;fire(trigger,ScriptProgram.Event.TRIGGER,other);if(t.bool("once",false))trigger.triggerConsumed=true;}
    private void handleDamage(Body source,Body target){ComponentDef damage=activeComponent(source,"DamageOnContact"),health=activeComponent(target,"Health");if(damage==null||health==null)return;applyDamage(target,Math.max(0,damage.number("damage",10)),source);}
    private ComponentDef activeComponent(Body body,String type){ComponentDef c=body.def.component(type);return c!=null&&c.bool("enabled",true)?c:null;}
    private boolean clickable(Body body){ComponentDef c=body.def.component("Clickable");return c==null||c.bool("enabled",true);}

    private void applyDamage(Body target,double amount,Body source){ComponentDef health=activeComponent(target,"Health");if(health==null)return;double max=Math.max(0,health.number("max",100)),current=Math.max(0,health.number("current",max)-Math.max(0,amount));health.properties.put("current",number(current));logger.accept("["+target.def.name+"] Health "+number(current)+"/"+number(max));if(current<=0)destroyBody(target,source);}
    private void applyHeal(Body target,double amount){ComponentDef health=activeComponent(target,"Health");if(health==null)return;double max=Math.max(0,health.number("max",100)),current=Math.min(max,Math.max(0,health.number("current",max)+Math.max(0,amount)));health.properties.put("current",number(current));}

    private void fire(Body body,ScriptProgram.Event event,Body other){if(body==null||body.destroyed||!body.def.enabled)return;if(body.program==null||!body.scriptSource.equals(body.def.script)){body.scriptSource=body.def.script;body.program=ScriptProgram.compile(body.def.script);}if(!body.program.validation().valid())return;body.program.fire(event,new ScriptContext(body,other));}
    private void destroyBody(Body body,Body cause){if(body==null||body.destroyed||body.destroying)return;body.destroying=true;fire(body,ScriptProgram.Event.DESTROY,cause);body.destroyed=true;body.destroying=false;}

    private void createEntity(Body source,String templateRef,Double x,Double y){if(level==null||templateRef==null||templateRef.isBlank())return;EntityDef template=level.entities.stream().filter(e->e.id.equals(templateRef)).findFirst().orElseGet(()->level.entities.stream().filter(e->e.name.equalsIgnoreCase(templateRef)).findFirst().orElse(null));if(template==null){logger.accept("["+source.def.name+"] create: no existe la entidad plantilla '"+templateRef+"'.");return;}String id=uniqueRuntimeId(template.id);EntityDef def=cloneEntity(template,id);def.x=x==null?source.def.x:x;def.y=y==null?source.def.y:y;Body body=new Body(def);normalizeHealth(body);bodies.put(def.id,body);logger.accept("[Runtime] create "+template.id+" -> "+def.id+" @ "+number(def.x)+","+number(def.y));fire(body,ScriptProgram.Event.START,null);}
    private String uniqueRuntimeId(String base){String clean=base==null||base.isBlank()?"entity":base,id;do{id=clean+"#"+(++runtimeEntitySequence);}while(bodies.containsKey(id));return id;}
    private static EntityDef cloneEntity(EntityDef source,String id){EntityDef copy=new EntityDef(id,source.name,source.x,source.y);copy.width=source.width;copy.height=source.height;copy.enabled=source.enabled;copy.layer=source.layer;copy.renderLayer=source.renderLayer;copy.physicsLayer=source.physicsLayer;copy.assetKey=source.assetKey;copy.script=source.script;source.components.forEach(c->copy.components.add(c.copy()));copy.variables.putAll(source.variables);return copy;}

    private Body resolveBody(String ref,Body self,Body other){if(ref==null||ref.isBlank())return null;if(ref.equalsIgnoreCase("self")||ref.equalsIgnoreCase("this"))return self;if(ref.equalsIgnoreCase("other"))return other;Body exact=bodies.get(ref);if(exact!=null&&!exact.destroyed)return exact;return bodies.values().stream().filter(b->!b.destroyed&&(b.def.id.equalsIgnoreCase(ref)||b.def.name.equalsIgnoreCase(ref))).findFirst().orElse(null);}
    private String bodyProperty(Body b,String name){if(b==null)return"";return switch(name.toLowerCase(Locale.ROOT)){case"id"->b.def.id;case"name"->b.def.name;case"x"->number(b.def.x);case"y"->number(b.def.y);case"width"->number(b.def.width);case"height"->number(b.def.height);case"enabled"->Boolean.toString(b.def.enabled);case"layer"->Integer.toString(b.def.layer);case"renderlayer"->b.def.renderLayer;case"physicslayer"->b.def.physicsLayer;case"sprite","asset","assetkey"->b.def.assetKey;case"vx"->number(b.vx);case"vy"->number(b.vy);default->b.def.variables.getOrDefault(name,"");};}
    private void setBodyProperty(Body b,String name,String value){if(b==null)return;try{switch(name.toLowerCase(Locale.ROOT)){case"name"->b.def.name=value;case"x"->b.def.x=Double.parseDouble(value);case"y"->b.def.y=Double.parseDouble(value);case"width"->b.def.width=Math.max(.01,Double.parseDouble(value));case"height"->b.def.height=Math.max(.01,Double.parseDouble(value));case"enabled"->b.def.enabled=Boolean.parseBoolean(value);case"layer"->b.def.layer=Integer.parseInt(value);case"renderlayer"->b.def.renderLayer=value;case"physicslayer"->b.def.physicsLayer=value;case"sprite","asset","assetkey"->b.def.assetKey=value;case"vx"->b.vx=Double.parseDouble(value);case"vy"->b.vy=Double.parseDouble(value);default->b.def.variables.put(name,value);}}catch(NumberFormatException ex){logger.accept("[Script] Valor inválido para "+name+": "+value);}}

    private Body hitEntity(double sx,double sy){if(level==null)return null;Viewport v=viewport();double wx=(sx-v.ox)/v.tile,wy=(sy-v.oy)/v.tile;return bodies.values().stream().filter(b->!b.destroyed&&b.def.enabled&&wx>=b.def.x&&wy>=b.def.y&&wx<=b.def.x+b.def.width&&wy<=b.def.y+b.def.height).max(Comparator.comparingInt(this::renderZ)).orElse(null);}

    private void render(){GraphicsContext g=canvas.getGraphicsContext2D();g.setImageSmoothing(false);g.setFill(Color.web("#0b0f17"));g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());if(level==null)return;Viewport v=viewport();List<RenderItem>items=new ArrayList<>();for(TileLayer layer:level.tileLayers)if(layer.visible)items.add(new RenderItem(layer.order*1000,()->drawTileLayer(g,v,layer)));for(Body body:bodies.values())if(!body.destroyed&&body.def.enabled)items.add(new RenderItem(renderZ(body),()->drawBody(g,v,body)));items.stream().sorted(Comparator.comparingInt(RenderItem::z)).forEach(i->i.draw.run());}
    private int renderZ(Body body){return project.renderOrder(body.def.renderLayer)*1000+500+body.def.layer;}
    private void drawTileLayer(GraphicsContext g,Viewport v,TileLayer layer){g.setImageSmoothing(false);for(int y=0;y<level.height;y++)for(int x=0;x<level.width;x++){int tileId=layer.get(x,y);if(tileId<0)continue;TileDef t=project.getTiles().get(tileId);if(t==null)continue;double px=Math.rint(v.ox+x*v.tile),py=Math.rint(v.oy+y*v.tile);Image img=image(t.assetKey);if(img!=null)g.drawImage(img,px,py,v.tile,v.tile);else{java.awt.Color c=t.color;g.setFill(Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0));g.fillRect(px,py,v.tile,v.tile);}}}
    private void drawBody(GraphicsContext g,Viewport v,Body b){g.setImageSmoothing(false);double px=Math.rint(v.ox+b.def.x*v.tile),py=Math.rint(v.oy+b.def.y*v.tile),w=Math.max(1,Math.rint(b.def.width*v.tile)),h=Math.max(1,Math.rint(b.def.height*v.tile));Image img=image(b.def.assetKey);if(img!=null)g.drawImage(img,px,py,w,h);else{g.setFill(Color.web("#55b7ff"));g.fillRoundRect(px,py,w,h,6,6);g.setStroke(Color.WHITE);g.strokeRoundRect(px,py,w,h,6,6);}}
    private Image image(String key){if(key==null||key.isBlank())return null;if(imageCache.containsKey(key))return imageCache.get(key);Asset a=project.getAssets().get(key);if(a==null||a.data==null)return null;try{Image img=new Image(new ByteArrayInputStream(a.data),0,0,true,false);imageCache.put(key,img);return img;}catch(Exception e){return null;}}
    private Viewport viewport(){double base=Math.max(1,project.getTileSize()),fit=Math.min(canvas.getWidth()/Math.max(1,level.width),canvas.getHeight()/Math.max(1,level.height)),tile;if(fit>=base){double scale=Math.max(1,Math.floor(fit/base));tile=base*scale;}else tile=Math.max(8,Math.floor(fit));double ox=Math.floor((canvas.getWidth()-level.width*tile)/2.0),oy=Math.floor((canvas.getHeight()-level.height*tile)/2.0);return new Viewport(tile,ox,oy);}
    private boolean isDown(String key){try{return keys.contains(KeyCode.valueOf(key.toUpperCase(Locale.ROOT)));}catch(Exception e){return false;}}
    private boolean isPressed(String key){try{return pressed.contains(KeyCode.valueOf(key.toUpperCase(Locale.ROOT)));}catch(Exception e){return false;}}
    private static String number(double value){return value==Math.rint(value)?Long.toString((long)value):String.format(Locale.ROOT,"%.3f",value).replaceAll("0+$","").replaceAll("\\.$","");}

    public double[] debugEntityPosition(String id){Body b=bodies.get(id);return b==null?null:new double[]{b.def.x,b.def.y};}
    public double debugHealth(String id){Body b=bodies.get(id);if(b==null)return Double.NaN;ComponentDef h=activeComponent(b,"Health");return h==null?Double.NaN:h.number("current",Double.NaN);}
    public long debugEntityCountByName(String name){return bodies.values().stream().filter(b->!b.destroyed&&b.def.enabled&&b.def.name.equalsIgnoreCase(name)).count();}
    public boolean debugHasEntityAt(String name,double x,double y){return bodies.values().stream().anyMatch(b->!b.destroyed&&b.def.enabled&&b.def.name.equalsIgnoreCase(name)&&Math.abs(b.def.x-x)<.001&&Math.abs(b.def.y-y)<.001);}
    public void debugFireClick(String id){Body b=bodies.get(id);if(b!=null)fire(b,ScriptProgram.Event.CLICK,null);}
    public void debugAdvance(double seconds){double remaining=Math.max(0,seconds);while(remaining>1e-9){double step=Math.min(.05,remaining);update(step);remaining-=step;}render();}
    public String debugGlobal(String name){return globals.getOrDefault(name,"");}
    public String debugEntityVariable(String id,String name){Body b=bodies.get(id);return b==null?null:b.def.variables.get(name);}

    private final class ScriptContext implements ScriptProgram.Context {
        private final Body body,other;
        ScriptContext(Body body,Body other){this.body=body;this.other=other;}
        @Override public boolean keyDown(String key){return isDown(key);}
        @Override public boolean keyPressed(String key){return isPressed(key);}
        @Override public void move(double dx,double dy){moveBody(body,dx,dy,true);}
        @Override public void setVelocity(double vx,double vy){body.vx=vx;body.vy=vy;}
        @Override public void teleport(double x,double y){body.def.x=x;body.def.y=y;}
        @Override public void bounce(){body.vx=-body.vx;body.vy=-body.vy;}
        @Override public void destroy(){destroyBody(body,other);}
        @Override public void loadScene(String id){if(project.getLevels().containsKey(id))pendingScene=new PendingScene(id,null,null);else logger.accept("[Script] No existe la escena '"+id+"'.");}
        @Override public void showMenu(String id){if(project.getMenus().containsKey(id)){scheduled.clear();GameView.this.showMenu(id);}else logger.accept("[Script] No existe el menú '"+id+"'.");}
        @Override public void restartScene(){if(level!=null)pendingScene=new PendingScene(level.id,null,null);}
        @Override public void setSprite(String assetKey){body.def.assetKey=assetKey;}
        @Override public void createEntity(String template,Double x,Double y){GameView.this.createEntity(body,template,x,y);}
        @Override public void schedule(double seconds,Runnable action){GameView.this.schedule(seconds,action);}
        @Override public void log(String message){logger.accept("["+body.def.name+"] "+message);}
        @Override public void setVariable(String name,String value){body.def.variables.put(name,value);}
        @Override public String variable(String name){return body.def.variables.getOrDefault(name,"0");}
        @Override public void setProperty(String name,String value){setBodyProperty(body,name,value);}
        @Override public String property(String name){return bodyProperty(body,name);}
        @Override public String otherEntity(){return other==null?"":other.def.id;}
        @Override public boolean entityExists(String ref){return resolveBody(ref,body,other)!=null;}
        @Override public String entityProperty(String ref,String name){return bodyProperty(resolveBody(ref,body,other),name);}
        @Override public void setEntityProperty(String ref,String name,String value){setBodyProperty(resolveBody(ref,body,other),name,value);}
        @Override public void moveEntity(String ref,double dx,double dy){Body target=resolveBody(ref,body,other);if(target!=null)moveBody(target,dx,dy,true);}
        @Override public void teleportEntity(String ref,double x,double y){Body target=resolveBody(ref,body,other);if(target!=null){target.def.x=x;target.def.y=y;}}
        @Override public void destroyEntity(String ref){destroyBody(resolveBody(ref,body,other),body);}
        @Override public void setEntitySprite(String ref,String assetKey){Body target=resolveBody(ref,body,other);if(target!=null)target.def.assetKey=assetKey;}
        @Override public String entityVariable(String ref,String name){Body target=resolveBody(ref,body,other);return target==null?"0":target.def.variables.getOrDefault(name,"0");}
        @Override public void setEntityVariable(String ref,String name,String value){Body target=resolveBody(ref,body,other);if(target!=null)target.def.variables.put(name,value);}
        @Override public boolean componentExists(String ref,String type){Body target=resolveBody(ref,body,other);return target!=null&&target.def.component(type)!=null;}
        @Override public String componentProperty(String ref,String type,String property){Body target=resolveBody(ref,body,other);if(target==null)return"";ComponentDef c=target.def.component(type);return c==null?"":c.get(property,"");}
        @Override public void setComponentProperty(String ref,String type,String property,String value){Body target=resolveBody(ref,body,other);if(target==null)return;ComponentDef c=target.def.component(type);if(c!=null){c.properties.put(property,value);if(type.equals("Health"))normalizeHealth(target);}}
        @Override public void addComponent(String ref,String type){Body target=resolveBody(ref,body,other);if(target==null||target.def.component(type)!=null)return;if(!GameProject.BUILTIN_COMPONENTS.contains(type)){logger.accept("[Script] Componente desconocido: "+type);return;}target.def.components.add(ComponentDef.preset(type));normalizeHealth(target);}
        @Override public void removeComponent(String ref,String type){Body target=resolveBody(ref,body,other);if(target!=null)target.def.components.removeIf(c->c.type.equals(type));}
        @Override public String globalVariable(String name){return globals.getOrDefault(name,"0");}
        @Override public void setGlobalVariable(String name,String value){globals.put(name,value);}
        @Override public void damage(String ref,double amount){Body target=resolveBody(ref,body,other);if(target!=null)applyDamage(target,amount,body);}
        @Override public void heal(String ref,double amount){Body target=resolveBody(ref,body,other);if(target!=null)applyHeal(target,amount);}
        @Override public double random(){return ThreadLocalRandom.current().nextDouble();}
    }

    private static final class Body { final EntityDef def;double vx,vy,originX,originY;int patrolDirection=1;boolean destroyed,destroying,triggerConsumed;String scriptSource="";ScriptProgram program;Body(EntityDef def){this.def=def;originX=def.x;originY=def.y;} }
    private record ScheduledAction(double due,long order,Runnable action) {}
    private record Block(Body other) {}
    private record PendingScene(String id,Double x,Double y) {}
    private record Viewport(double tile,double ox,double oy) {}
    private record RenderItem(int z,Runnable draw) {}
}
