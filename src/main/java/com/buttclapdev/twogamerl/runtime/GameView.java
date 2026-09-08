package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.script.ScriptProgram;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.function.Consumer;

public final class GameView extends StackPane {
    private final GameProject project;
    private final Canvas canvas = new Canvas(960, 640);
    private final Pane menuLayer = new StackPane();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Set<KeyCode> keys = EnumSet.noneOf(KeyCode.class);
    private final Set<KeyCode> pressed = EnumSet.noneOf(KeyCode.class);
    private final LinkedHashMap<String, Body> bodies = new LinkedHashMap<>();
    private final AnimationTimer timer;
    private Consumer<String> logger = System.out::println;
    private Level level;
    private String pendingScene;
    private boolean playing;
    private long lastTime;
    private double elapsed;

    public GameView(GameProject source) {
        this.project = source.deepCopy();
        setStyle("-fx-background-color: #0b0f17;");
        getChildren().addAll(canvas, menuLayer);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        setFocusTraversable(true);
        setOnKeyPressed(e -> { if (keys.add(e.getCode())) pressed.add(e.getCode()); e.consume(); });
        setOnKeyReleased(e -> { keys.remove(e.getCode()); e.consume(); });
        setOnMouseClicked(e -> {
            if (!playing) return;
            Body hit = hitEntity(e.getX(), e.getY());
            if (hit != null) fire(hit, e.getClickCount() >= 2 ? ScriptProgram.Event.DOUBLECLICK : ScriptProgram.Event.CLICK, null);
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
        if (project.getMenus().isEmpty() || project.getStartMenu() == null || project.getStartMenu().isBlank()) startGame();
        else showMenu(project.getStartMenu());
    }

    private void showMenu(String id) {
        playing = false;
        MenuScreen menu = project.getMenus().get(id);
        if (menu == null) { startGame(); return; }
        menuLayer.getChildren().clear();
        VBox box = new VBox(14); box.setAlignment(Pos.CENTER); box.setPadding(new Insets(36)); box.setMaxWidth(420);
        Label title = new Label(menu.title); title.setFont(Font.font(34)); title.setTextFill(Color.WHITE); title.getStyleClass().add("game-menu-title");
        box.getChildren().add(title);
        for (MenuButton def : menu.buttons) {
            Button b = new Button(def.text); b.setPrefWidth(Math.max(180, def.width)); b.setPrefHeight(Math.max(42, def.height)); b.getStyleClass().add("primary-button");
            b.setOnAction(e -> {
                switch (def.action) {
                    case START_GAME -> startGame();
                    case OPEN_MENU -> showMenu(def.target);
                    case EXIT -> { var w = getScene() == null ? null : getScene().getWindow(); if (w != null) w.hide(); }
                }
            });
            box.getChildren().add(b);
        }
        StackPane background = new StackPane(box);
        java.awt.Color awt = menu.background;
        background.setStyle(String.format(Locale.ROOT, "-fx-background-color: rgba(%d,%d,%d,1);", awt.getRed(), awt.getGreen(), awt.getBlue()));
        menuLayer.getChildren().add(background);
        StackPane.setAlignment(background, Pos.CENTER);
    }

    private void startGame() {
        menuLayer.getChildren().clear();
        loadScene(project.getStartLevel());
        playing = true;
        requestFocus();
    }

    private void loadScene(String id) {
        Level next = project.getLevels().get(id);
        if (next == null && !project.getLevels().isEmpty()) next = project.getLevels().values().iterator().next();
        if (next == null) return;
        level = next;
        bodies.clear();
        elapsed = 0;
        for (EntityDef def : level.entities) {
            Body body = new Body(def.copy()); bodies.put(def.id, body);
        }
        for (Body body : List.copyOf(bodies.values())) fire(body, ScriptProgram.Event.START, null);
    }

    private void update(double dt) {
        if (level == null) return;
        elapsed += dt;
        for (Body body : List.copyOf(bodies.values())) {
            if (body.destroyed || !body.def.enabled) continue;
            applyBuiltins(body, dt);
            fire(body, ScriptProgram.Event.UPDATE, null);
            integrate(body, dt);
        }
        checkEntityCollisions();
        bodies.values().removeIf(b -> b.destroyed);
        if (pendingScene != null) { String target = pendingScene; pendingScene = null; loadScene(target); }
    }

    private void applyBuiltins(Body body, double dt) {
        ComponentDef controller = body.def.component("PlayerController");
        if (controller != null) {
            double dx = 0, dy = 0;
            if (isDown("W") || (controller.bool("allowArrows", true) && isDown("UP"))) dy -= 1;
            if (isDown("S") || (controller.bool("allowArrows", true) && isDown("DOWN"))) dy += 1;
            if (isDown("A") || (controller.bool("allowArrows", true) && isDown("LEFT"))) dx -= 1;
            if (isDown("D") || (controller.bool("allowArrows", true) && isDown("RIGHT"))) dx += 1;
            double len = Math.hypot(dx, dy); if (len > 0) { dx /= len; dy /= len; }
            double speed = controller.number("speed", 4); body.vx = dx * speed; body.vy = dy * speed;
        }
        ComponentDef patrol = body.def.component("Patrol");
        if (patrol != null && controller == null) {
            double speed = patrol.number("speed", 1.5), distance = Math.max(.1, patrol.number("distance", 4));
            double direction = Math.cos(elapsed * speed / distance * Math.PI) >= 0 ? 1 : -1;
            if (patrol.get("axis", "x").equalsIgnoreCase("y")) body.vy = direction * speed; else body.vx = direction * speed;
        }
        ComponentDef rigid = body.def.component("Rigidbody2D");
        if (rigid != null) {
            body.vy += 9.81 * rigid.number("gravityScale", 0) * dt;
            double max = Math.max(.1, rigid.number("maxSpeed", 8));
            double speed = Math.hypot(body.vx, body.vy); if (speed > max) { body.vx = body.vx / speed * max; body.vy = body.vy / speed * max; }
            if (controller == null && patrol == null) {
                double factor = Math.max(0, 1 - rigid.number("drag", 0) * dt); body.vx *= factor; body.vy *= factor;
            }
        }
    }

    private void integrate(Body body, double dt) {
        if (body.vx == 0 && body.vy == 0) return;
        moveBody(body, body.vx * dt, 0, true); moveBody(body, 0, body.vy * dt, true);
    }

    private void moveBody(Body body, double dx, double dy, boolean collisionEvent) {
        double nx = body.def.x + dx, ny = body.def.y + dy;
        if (!body.def.has("BoxCollider2D") || !blocked(body, nx, ny)) { body.def.x = nx; body.def.y = ny; return; }
        if (dx != 0) body.vx = 0; if (dy != 0) body.vy = 0;
        if (collisionEvent) fire(body, ScriptProgram.Event.COLLISION, null);
    }

    private boolean blocked(Body body, double x, double y) {
        ComponentDef collider = body.def.component("BoxCollider2D");
        if (collider == null || !collider.bool("solid", true)) return false;
        double w = collider.number("width", body.def.width), h = collider.number("height", body.def.height);
        int minX = (int)Math.floor(x), minY = (int)Math.floor(y), maxX = (int)Math.floor(x + Math.max(.01,w) - .001), maxY = (int)Math.floor(y + Math.max(.01,h) - .001);
        for (int ty = minY; ty <= maxY; ty++) for (int tx = minX; tx <= maxX; tx++) {
            if (tx < 0 || ty < 0 || tx >= level.width || ty >= level.height) return true;
            TileDef tile = project.getTiles().get(level.get(tx, ty)); if (tile != null && !tile.walkable) return true;
        }
        return false;
    }

    private void checkEntityCollisions() {
        List<Body> all = new ArrayList<>(bodies.values());
        for (int i=0;i<all.size();i++) for (int j=i+1;j<all.size();j++) {
            Body a=all.get(i), b=all.get(j); if (a.destroyed || b.destroyed || !intersects(a,b)) continue;
            fire(a, ScriptProgram.Event.COLLISION, b); fire(b, ScriptProgram.Event.COLLISION, a);
            handlePortal(a,b); handlePortal(b,a); handleTrigger(a,b); handleTrigger(b,a); handleDamage(a,b); handleDamage(b,a);
        }
    }

    private boolean intersects(Body a, Body b) {
        double aw = colliderWidth(a), ah = colliderHeight(a), bw = colliderWidth(b), bh = colliderHeight(b);
        return a.def.x < b.def.x + bw && a.def.x + aw > b.def.x && a.def.y < b.def.y + bh && a.def.y + ah > b.def.y;
    }
    private double colliderWidth(Body b) { ComponentDef c=b.def.component("BoxCollider2D"); return c==null?b.def.width:c.number("width",b.def.width); }
    private double colliderHeight(Body b) { ComponentDef c=b.def.component("BoxCollider2D"); return c==null?b.def.height:c.number("height",b.def.height); }
    private void handlePortal(Body portal, Body other) { ComponentDef p=portal.def.component("ScenePortal"); if(p!=null && other.def.has("PlayerController")) pendingScene=p.get("targetScene",project.getStartLevel()); }
    private void handleTrigger(Body trigger, Body other) { if(trigger.def.has("Trigger") && other.def.has("PlayerController")) fire(trigger, ScriptProgram.Event.TRIGGER,other); }
    private void handleDamage(Body source, Body target) {
        ComponentDef d=source.def.component("DamageOnContact"), h=target.def.component("Health"); if(d==null||h==null)return;
        double current=h.number("current",h.number("max",100))-d.number("damage",10); h.properties.put("current",Double.toString(current)); if(current<=0)target.destroyed=true;
    }

    private void fire(Body body, ScriptProgram.Event event, Body other) {
        if (body.program == null || !body.scriptSource.equals(body.def.script)) { body.scriptSource = body.def.script; body.program = ScriptProgram.compile(body.def.script); }
        if (!body.program.validation().valid()) return;
        body.program.fire(event, new ScriptContext(body));
    }

    private Body hitEntity(double sx, double sy) {
        if (level == null) return null; Viewport v = viewport(); double wx=(sx-v.ox)/v.tile, wy=(sy-v.oy)/v.tile;
        return bodies.values().stream().filter(b->!b.destroyed&&b.def.enabled&&wx>=b.def.x&&wy>=b.def.y&&wx<=b.def.x+b.def.width&&wy<=b.def.y+b.def.height)
                .max(Comparator.comparingInt(b->b.def.layer)).orElse(null);
    }

    private void render() {
        GraphicsContext g=canvas.getGraphicsContext2D(); g.setFill(Color.web("#0b0f17")); g.fillRect(0,0,canvas.getWidth(),canvas.getHeight()); if(level==null)return;
        Viewport v=viewport();
        for(int y=0;y<level.height;y++)for(int x=0;x<level.width;x++){
            TileDef t=project.getTiles().get(level.get(x,y)); double px=v.ox+x*v.tile,py=v.oy+y*v.tile;
            Image img=t==null?null:image(t.assetKey); if(img!=null)g.drawImage(img,px,py,v.tile,v.tile); else { java.awt.Color c=t==null?java.awt.Color.MAGENTA:t.color; g.setFill(Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0)); g.fillRect(px,py,v.tile,v.tile); }
        }
        bodies.values().stream().filter(b->!b.destroyed&&b.def.enabled).sorted(Comparator.comparingInt(b->b.def.layer)).forEach(b->{
            double px=v.ox+b.def.x*v.tile,py=v.oy+b.def.y*v.tile,w=b.def.width*v.tile,h=b.def.height*v.tile; Image img=image(b.def.assetKey);
            if(img!=null)g.drawImage(img,px,py,w,h); else { g.setFill(Color.web("#55b7ff")); g.fillRoundRect(px,py,w,h,6,6); g.setStroke(Color.WHITE); g.strokeRoundRect(px,py,w,h,6,6); }
        });
    }

    private Image image(String key) {
        if(key==null||key.isBlank())return null; if(imageCache.containsKey(key))return imageCache.get(key); Asset a=project.getAssets().get(key); if(a==null||a.data==null)return null;
        try{Image img=new Image(new ByteArrayInputStream(a.data));imageCache.put(key,img);return img;}catch(Exception e){return null;}
    }
    private Viewport viewport(){double tile=Math.max(8,Math.min(project.getTileSize()*2.0,Math.min(canvas.getWidth()/Math.max(1,level.width),canvas.getHeight()/Math.max(1,level.height))));return new Viewport(tile,(canvas.getWidth()-level.width*tile)/2,(canvas.getHeight()-level.height*tile)/2);}
    private boolean isDown(String key){try{return keys.contains(KeyCode.valueOf(key.toUpperCase(Locale.ROOT)));}catch(Exception e){return false;}}
    private boolean isPressed(String key){try{return pressed.contains(KeyCode.valueOf(key.toUpperCase(Locale.ROOT)));}catch(Exception e){return false;}}

    private final class ScriptContext implements ScriptProgram.Context {
        private final Body body; ScriptContext(Body body){this.body=body;}
        @Override public boolean keyDown(String key){return isDown(key);}
        @Override public boolean keyPressed(String key){return isPressed(key);}
        @Override public void move(double dx,double dy){moveBody(body,dx,dy,true);}
        @Override public void setVelocity(double vx,double vy){body.vx=vx;body.vy=vy;}
        @Override public void teleport(double x,double y){body.def.x=x;body.def.y=y;}
        @Override public void bounce(){body.vx=-body.vx;body.vy=-body.vy;}
        @Override public void destroy(){body.destroyed=true;}
        @Override public void loadScene(String id){pendingScene=id;}
        @Override public void setSprite(String assetKey){body.def.assetKey=assetKey;}
        @Override public void log(String message){logger.accept("["+body.def.name+"] "+message);}
        @Override public void setVariable(String name,String value){body.def.variables.put(name,value);}
        @Override public String variable(String name){return body.def.variables.getOrDefault(name,"0");}
        @Override public void setProperty(String name,String value){
            try{switch(name.toLowerCase(Locale.ROOT)){case"x"->body.def.x=Double.parseDouble(value);case"y"->body.def.y=Double.parseDouble(value);case"width"->body.def.width=Double.parseDouble(value);case"height"->body.def.height=Double.parseDouble(value);case"enabled"->body.def.enabled=Boolean.parseBoolean(value);case"layer"->body.def.layer=Integer.parseInt(value);default->body.def.variables.put(name,value);}}catch(NumberFormatException ignored){}
        }
    }

    private static final class Body { final EntityDef def; double vx,vy; boolean destroyed; String scriptSource=""; ScriptProgram program; Body(EntityDef def){this.def=def;} }
    private record Viewport(double tile,double ox,double oy){}
}
