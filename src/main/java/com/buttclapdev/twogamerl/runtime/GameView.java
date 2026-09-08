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
    private PendingScene pendingScene;
    private boolean playing;
    private long lastTime;
    private Set<String> contacts = new HashSet<>();
    private Set<String> frameContacts = new HashSet<>();

    public GameView(GameProject source) {
        this.project = source.deepCopy();
        setStyle("-fx-background-color: #0b0f17;");
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
            if (hit != null && clickable(hit)) {
                fire(hit, e.getClickCount() >= 2 ? ScriptProgram.Event.DOUBLECLICK : ScriptProgram.Event.CLICK, null);
            }
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
        loadScene(project.getStartLevel(), null, null);
        playing = true;
        requestFocus();
    }

    private void loadScene(String id, Double targetX, Double targetY) {
        Level next = project.getLevels().get(id);
        if (next == null && !project.getLevels().isEmpty()) next = project.getLevels().values().iterator().next();
        if (next == null) return;
        level = next;
        bodies.clear();
        contacts.clear();
        frameContacts.clear();

        for (EntityDef def : level.entities) {
            Body body = new Body(def.copy());
            normalizeHealth(body);
            bodies.put(def.id, body);
        }
        if (targetX != null && targetY != null) {
            bodies.values().stream()
                    .filter(b -> activeComponent(b, "PlayerController") != null)
                    .findFirst()
                    .ifPresent(b -> { b.def.x = targetX; b.def.y = targetY; b.originX = targetX; b.originY = targetY; });
        }
        for (Body body : List.copyOf(bodies.values())) fire(body, ScriptProgram.Event.START, null);
    }

    private void normalizeHealth(Body body) {
        ComponentDef health = activeComponent(body, "Health");
        if (health == null) return;
        double max = Math.max(0, health.number("max", 100));
        double current = Math.max(0, Math.min(max, health.number("current", max)));
        health.properties.put("max", number(max));
        health.properties.put("current", number(current));
    }

    private void update(double dt) {
        if (level == null) return;
        frameContacts = new HashSet<>();
        for (Body body : List.copyOf(bodies.values())) {
            if (body.destroyed || !body.def.enabled) continue;
            applyBuiltins(body, dt);
            fire(body, ScriptProgram.Event.UPDATE, null);
            integrate(body, dt);
        }
        checkEntityOverlaps();
        contacts = frameContacts;
        bodies.values().removeIf(b -> b.destroyed);
        if (pendingScene != null) {
            PendingScene target = pendingScene;
            pendingScene = null;
            loadScene(target.id, target.x, target.y);
        }
    }

    private void applyBuiltins(Body body, double dt) {
        ComponentDef controller = activeComponent(body, "PlayerController");
        if (controller != null) {
            double dx = 0, dy = 0;
            if (isDown("W") || (controller.bool("allowArrows", true) && isDown("UP"))) dy -= 1;
            if (isDown("S") || (controller.bool("allowArrows", true) && isDown("DOWN"))) dy += 1;
            if (isDown("A") || (controller.bool("allowArrows", true) && isDown("LEFT"))) dx -= 1;
            if (isDown("D") || (controller.bool("allowArrows", true) && isDown("RIGHT"))) dx += 1;
            double len = Math.hypot(dx, dy);
            if (len > 0) { dx /= len; dy /= len; }
            double speed = Math.max(0, controller.number("speed", 4));
            body.vx = dx * speed;
            body.vy = dy * speed;
        }

        ComponentDef patrol = activeComponent(body, "Patrol");
        if (patrol != null && controller == null) {
            double speed = Math.max(0, patrol.number("speed", 1.5));
            double distance = Math.max(.05, patrol.number("distance", 4));
            boolean vertical = patrol.get("axis", "x").equalsIgnoreCase("y");
            double position = vertical ? body.def.y : body.def.x;
            double origin = vertical ? body.originY : body.originX;
            if (body.patrolDirection > 0 && position >= origin + distance) body.patrolDirection = -1;
            else if (body.patrolDirection < 0 && position <= origin) body.patrolDirection = 1;
            if (vertical) body.vy = body.patrolDirection * speed;
            else body.vx = body.patrolDirection * speed;
        }

        ComponentDef rigid = activeComponent(body, "Rigidbody2D");
        if (rigid != null) {
            body.vy += 9.81 * rigid.number("gravityScale", 1) * dt;
            double drag = Math.max(0, rigid.number("drag", .4));
            double factor = Math.max(0, 1 - drag * dt);
            if (controller == null && (patrol == null || patrol.get("axis", "x").equalsIgnoreCase("y"))) body.vx *= factor;
            if (controller == null && (patrol == null || patrol.get("axis", "x").equalsIgnoreCase("x"))) body.vy *= factor;
            double max = Math.max(.1, rigid.number("maxSpeed", 12));
            double speed = Math.hypot(body.vx, body.vy);
            if (speed > max) { body.vx = body.vx / speed * max; body.vy = body.vy / speed * max; }
        }
    }

    private void integrate(Body body, double dt) {
        if (Math.abs(body.vx) > 1e-9) moveBody(body, body.vx * dt, 0, true);
        if (Math.abs(body.vy) > 1e-9) moveBody(body, 0, body.vy * dt, true);
    }

    private void moveBody(Body body, double dx, double dy, boolean collisionEvent) {
        ComponentDef collider = activeComponent(body, "BoxCollider2D");
        double nx = body.def.x + dx, ny = body.def.y + dy;
        if (collider == null || !collider.bool("solid", true)) {
            body.def.x = nx; body.def.y = ny; return;
        }

        Block block = blocked(body, nx, ny);
        if (block == null) {
            body.def.x = nx; body.def.y = ny; return;
        }

        if (dx != 0) body.vx = 0;
        if (dy != 0) body.vy = 0;
        if (activeComponent(body, "Patrol") != null) body.patrolDirection *= -1;
        if (!collisionEvent) return;

        if (block.other != null) processContact(body, block.other, true);
        else fire(body, ScriptProgram.Event.COLLISION, null);
    }

    private Block blocked(Body body, double x, double y) {
        ComponentDef collider = activeComponent(body, "BoxCollider2D");
        if (collider == null || !collider.bool("solid", true)) return null;
        double w = Math.max(.01, collider.number("width", body.def.width));
        double h = Math.max(.01, collider.number("height", body.def.height));

        int minX = (int)Math.floor(x), minY = (int)Math.floor(y);
        int maxX = (int)Math.floor(x + w - .001), maxY = (int)Math.floor(y + h - .001);
        for (int ty = minY; ty <= maxY; ty++) for (int tx = minX; tx <= maxX; tx++) {
            if (tx < 0 || ty < 0 || tx >= level.width || ty >= level.height) return new Block(null);
            TileDef tile = project.getTiles().get(level.get(tx, ty));
            if (tile != null && !tile.walkable) return new Block(null);
        }

        for (Body other : bodies.values()) {
            if (other == body || other.destroyed || !other.def.enabled) continue;
            ComponentDef otherCollider = activeComponent(other, "BoxCollider2D");
            if (otherCollider == null || !otherCollider.bool("solid", true)) continue;
            double ow = Math.max(.01, otherCollider.number("width", other.def.width));
            double oh = Math.max(.01, otherCollider.number("height", other.def.height));
            if (overlaps(x, y, w, h, other.def.x, other.def.y, ow, oh)) return new Block(other);
        }
        return null;
    }

    private void checkEntityOverlaps() {
        List<Body> all = new ArrayList<>(bodies.values());
        for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) {
            Body a = all.get(i), b = all.get(j);
            if (a.destroyed || b.destroyed || !a.def.enabled || !b.def.enabled || !intersects(a, b)) continue;
            processContact(a, b, false);
        }
    }

    private void processContact(Body a, Body b, boolean forcedColliderContact) {
        String key = contactKey(a, b);
        boolean firstThisFrame = frameContacts.add(key);
        boolean entered = firstThisFrame && !contacts.contains(key);
        if (!entered) return;

        boolean colliderContact = forcedColliderContact || (activeComponent(a, "BoxCollider2D") != null && activeComponent(b, "BoxCollider2D") != null);
        if (colliderContact) {
            fire(a, ScriptProgram.Event.COLLISION, b);
            fire(b, ScriptProgram.Event.COLLISION, a);
        }
        handlePortal(a, b); handlePortal(b, a);
        handleTrigger(a, b); handleTrigger(b, a);
        handleDamage(a, b); handleDamage(b, a);
    }

    private String contactKey(Body a, Body b) {
        return a.def.id.compareTo(b.def.id) <= 0 ? a.def.id + "\u0000" + b.def.id : b.def.id + "\u0000" + a.def.id;
    }

    private boolean intersects(Body a, Body b) {
        return overlaps(a.def.x, a.def.y, colliderWidth(a), colliderHeight(a), b.def.x, b.def.y, colliderWidth(b), colliderHeight(b));
    }

    private static boolean overlaps(double ax, double ay, double aw, double ah, double bx, double by, double bw, double bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }

    private double colliderWidth(Body b) {
        ComponentDef c = activeComponent(b, "BoxCollider2D");
        return c == null ? Math.max(.01, b.def.width) : Math.max(.01, c.number("width", b.def.width));
    }

    private double colliderHeight(Body b) {
        ComponentDef c = activeComponent(b, "BoxCollider2D");
        return c == null ? Math.max(.01, b.def.height) : Math.max(.01, c.number("height", b.def.height));
    }

    private void handlePortal(Body portal, Body other) {
        ComponentDef p = activeComponent(portal, "ScenePortal");
        if (p == null || activeComponent(other, "PlayerController") == null || pendingScene != null) return;
        String scene = p.get("targetScene", project.getStartLevel());
        double x = p.number("targetX", 2), y = p.number("targetY", 2);
        pendingScene = new PendingScene(scene, x, y);
        logger.accept("[" + portal.def.name + "] ScenePortal -> " + scene + " @ " + number(x) + "," + number(y));
    }

    private void handleTrigger(Body trigger, Body other) {
        ComponentDef t = activeComponent(trigger, "Trigger");
        if (t == null || activeComponent(other, "PlayerController") == null) return;
        if (trigger.triggerConsumed) return;
        fire(trigger, ScriptProgram.Event.TRIGGER, other);
        if (t.bool("once", false)) trigger.triggerConsumed = true;
    }

    private void handleDamage(Body source, Body target) {
        ComponentDef damage = activeComponent(source, "DamageOnContact");
        ComponentDef health = activeComponent(target, "Health");
        if (damage == null || health == null) return;
        double max = Math.max(0, health.number("max", 100));
        double current = Math.max(0, health.number("current", max) - Math.max(0, damage.number("damage", 10)));
        health.properties.put("current", number(current));
        logger.accept("[" + target.def.name + "] Health " + number(current) + "/" + number(max));
        if (current <= 0) target.destroyed = true;
    }

    private ComponentDef activeComponent(Body body, String type) {
        ComponentDef component = body.def.component(type);
        return component != null && component.bool("enabled", true) ? component : null;
    }

    private boolean clickable(Body body) {
        ComponentDef clickable = body.def.component("Clickable");
        return clickable == null || clickable.bool("enabled", true);
    }

    private void fire(Body body, ScriptProgram.Event event, Body other) {
        if (body.destroyed || !body.def.enabled) return;
        if (body.program == null || !body.scriptSource.equals(body.def.script)) {
            body.scriptSource = body.def.script;
            body.program = ScriptProgram.compile(body.def.script);
        }
        if (!body.program.validation().valid()) return;
        body.program.fire(event, new ScriptContext(body));
    }

    private Body hitEntity(double sx, double sy) {
        if (level == null) return null;
        Viewport v = viewport();
        double wx = (sx - v.ox) / v.tile, wy = (sy - v.oy) / v.tile;
        return bodies.values().stream()
                .filter(b -> !b.destroyed && b.def.enabled && wx >= b.def.x && wy >= b.def.y && wx <= b.def.x + b.def.width && wy <= b.def.y + b.def.height)
                .max(Comparator.comparingInt(b -> b.def.layer)).orElse(null);
    }

    private void render() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);
        g.setFill(Color.web("#0b0f17"));
        g.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        if (level == null) return;
        Viewport v = viewport();
        for (int y = 0; y < level.height; y++) for (int x = 0; x < level.width; x++) {
            TileDef t = project.getTiles().get(level.get(x, y));
            double px = v.ox + x * v.tile, py = v.oy + y * v.tile;
            Image img = t == null ? null : image(t.assetKey);
            if (img != null) g.drawImage(img, px, py, v.tile, v.tile);
            else {
                java.awt.Color c = t == null ? java.awt.Color.MAGENTA : t.color;
                g.setFill(Color.rgb(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() / 255.0));
                g.fillRect(px, py, v.tile, v.tile);
            }
        }
        bodies.values().stream().filter(b -> !b.destroyed && b.def.enabled).sorted(Comparator.comparingInt(b -> b.def.layer)).forEach(b -> {
            double px = Math.round(v.ox + b.def.x * v.tile), py = Math.round(v.oy + b.def.y * v.tile);
            double w = Math.max(1, Math.round(b.def.width * v.tile)), h = Math.max(1, Math.round(b.def.height * v.tile));
            Image img = image(b.def.assetKey);
            if (img != null) g.drawImage(img, px, py, w, h);
            else {
                g.setFill(Color.web("#55b7ff")); g.fillRoundRect(px, py, w, h, 6, 6);
                g.setStroke(Color.WHITE); g.strokeRoundRect(px, py, w, h, 6, 6);
            }
        });
    }

    private Image image(String key) {
        if (key == null || key.isBlank()) return null;
        if (imageCache.containsKey(key)) return imageCache.get(key);
        Asset a = project.getAssets().get(key);
        if (a == null || a.data == null) return null;
        try { Image img = new Image(new ByteArrayInputStream(a.data)); imageCache.put(key, img); return img; }
        catch (Exception e) { return null; }
    }

    private Viewport viewport() {
        double base = Math.max(1, project.getTileSize());
        double fit = Math.min(canvas.getWidth() / Math.max(1, level.width), canvas.getHeight() / Math.max(1, level.height));
        double tile;
        if (fit >= base) {
            double scale = Math.max(1, Math.floor(fit / base));
            tile = base * scale;
        } else {
            tile = Math.max(8, Math.floor(fit));
        }
        double ox = Math.floor((canvas.getWidth() - level.width * tile) / 2.0);
        double oy = Math.floor((canvas.getHeight() - level.height * tile) / 2.0);
        return new Viewport(tile, ox, oy);
    }

    private boolean isDown(String key) {
        try { return keys.contains(KeyCode.valueOf(key.toUpperCase(Locale.ROOT))); }
        catch (Exception e) { return false; }
    }

    private boolean isPressed(String key) {
        try { return pressed.contains(KeyCode.valueOf(key.toUpperCase(Locale.ROOT))); }
        catch (Exception e) { return false; }
    }

    private static String number(double value) {
        return value == Math.rint(value) ? Long.toString((long)value) : String.format(Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private final class ScriptContext implements ScriptProgram.Context {
        private final Body body;
        ScriptContext(Body body) { this.body = body; }
        @Override public boolean keyDown(String key) { return isDown(key); }
        @Override public boolean keyPressed(String key) { return isPressed(key); }
        @Override public void move(double dx, double dy) { moveBody(body, dx, dy, true); }
        @Override public void setVelocity(double vx, double vy) { body.vx = vx; body.vy = vy; }
        @Override public void teleport(double x, double y) { body.def.x = x; body.def.y = y; }
        @Override public void bounce() { body.vx = -body.vx; body.vy = -body.vy; }
        @Override public void destroy() { body.destroyed = true; }
        @Override public void loadScene(String id) { pendingScene = new PendingScene(id, null, null); }
        @Override public void setSprite(String assetKey) { body.def.assetKey = assetKey; }
        @Override public void log(String message) { logger.accept("[" + body.def.name + "] " + message); }
        @Override public void setVariable(String name, String value) { body.def.variables.put(name, value); }
        @Override public String variable(String name) { return body.def.variables.getOrDefault(name, "0"); }
        @Override public void setProperty(String name, String value) {
            try {
                switch (name.toLowerCase(Locale.ROOT)) {
                    case "x" -> body.def.x = Double.parseDouble(value);
                    case "y" -> body.def.y = Double.parseDouble(value);
                    case "width" -> body.def.width = Double.parseDouble(value);
                    case "height" -> body.def.height = Double.parseDouble(value);
                    case "enabled" -> body.def.enabled = Boolean.parseBoolean(value);
                    case "layer" -> body.def.layer = Integer.parseInt(value);
                    default -> body.def.variables.put(name, value);
                }
            } catch (NumberFormatException ignored) {}
        }
    }

    private static final class Body {
        final EntityDef def;
        double vx, vy;
        double originX, originY;
        int patrolDirection = 1;
        boolean destroyed;
        boolean triggerConsumed;
        String scriptSource = "";
        ScriptProgram program;
        Body(EntityDef def) {
            this.def = def;
            this.originX = def.x;
            this.originY = def.y;
        }
    }

    private record Block(Body other) {}
    private record PendingScene(String id, Double x, Double y) {}
    private record Viewport(double tile, double ox, double oy) {}
}
