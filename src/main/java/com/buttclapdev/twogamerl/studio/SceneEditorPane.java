package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;
import com.buttclapdev.twogamerl.script.ScriptProgram;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;

final class SceneEditorPane extends SplitPane {
    private enum Tool { SELECT, TILE, ERASE, ENTITY }

    private final StudioApp app;
    private final ComboBox<Level> scenePicker = new ComboBox<>();
    private final ListView<EntityDef> hierarchy = new ListView<>();
    private final ComboBox<TileDef> tilePicker = new ComboBox<>();
    private final Slider zoom = new Slider(16, 96, 32);
    private final Canvas canvas = new Canvas();
    private final TabPane tabs = new TabPane();
    private final Tab inspectorTab = new Tab("Inspector");
    private final Tab scriptTab = new Tab("Script");
    private final VBox inspector = new VBox(10);
    private final TextArea script = new TextArea();
    private final Label scriptStatus = new Label("Selecciona un objeto");
    private ToggleButton selectTool;
    private Tool tool = Tool.SELECT;
    private EntityDef selected;
    private boolean loading;
    private boolean dragging;
    private double dragX, dragY;

    SceneEditorPane(StudioApp app) {
        this.app = app;
        canvas.getGraphicsContext2D().setImageSmoothing(false);
        getItems().addAll(leftPane(), centerPane(), rightPane());
        setDividerPositions(.17, .76);
        refresh();
    }

    private Node leftPane() {
        VBox box = new VBox(8); box.getStyleClass().add("side-panel"); box.setPadding(new Insets(12)); box.setPrefWidth(250);
        scenePicker.setMaxWidth(Double.MAX_VALUE);
        scenePicker.setOnAction(e -> { selected = null; refreshHierarchy(); resizeCanvas(); rebuildInspector(); });
        Button addScene = new Button("＋ Escena"), removeScene = new Button("Eliminar");
        addScene.setOnAction(e -> addScene()); removeScene.setOnAction(e -> removeScene());
        HBox sceneActions = new HBox(6, addScene, removeScene); HBox.setHgrow(addScene, Priority.ALWAYS); HBox.setHgrow(removeScene, Priority.ALWAYS);
        addScene.setMaxWidth(Double.MAX_VALUE); removeScene.setMaxWidth(Double.MAX_VALUE);

        hierarchy.setPrefHeight(460);
        hierarchy.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> { selected = b; redraw(); rebuildInspector(); });
        hierarchy.setOnMouseClicked(e -> { if (e.getClickCount() >= 2 && selected != null) tabs.getSelectionModel().select(scriptTab); });
        Button addObject = new Button("＋ Objeto"), removeObject = new Button("Eliminar");
        addObject.setOnAction(e -> addAtCenter()); removeObject.setOnAction(e -> removeSelected());
        HBox objectActions = new HBox(6, addObject, removeObject);
        VBox.setVgrow(hierarchy, Priority.ALWAYS);
        box.getChildren().addAll(title("ESCENA"), scenePicker, sceneActions, new Separator(), title("OBJETOS DE LA ESCENA"), hierarchy, objectActions);
        return box;
    }

    private Node centerPane() {
        ToggleGroup group = new ToggleGroup();
        selectTool = toolButton("Seleccionar", group, true, Tool.SELECT);
        ToggleButton tile = toolButton("Tile", group, false, Tool.TILE);
        ToggleButton erase = toolButton("Borrar", group, false, Tool.ERASE);
        ToggleButton object = toolButton("Objeto", group, false, Tool.ENTITY);
        tilePicker.setPrefWidth(170);
        zoom.setPrefWidth(130);
        zoom.setMajorTickUnit(8);
        zoom.setMinorTickCount(0);
        zoom.setBlockIncrement(8);
        zoom.setSnapToTicks(true);
        zoom.valueProperty().addListener((o,a,b) -> resizeCanvas());
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(7, selectTool, tile, erase, object, new Separator(), new Label("Tile:"), tilePicker, spacer, new Label("Zoom"), zoom);
        tools.setAlignment(Pos.CENTER_LEFT); tools.setPadding(new Insets(8)); tools.getStyleClass().add("context-toolbar");
        StackPane shell = new StackPane(canvas); shell.getStyleClass().add("canvas-shell"); shell.setPadding(new Insets(28));
        ScrollPane scroll = new ScrollPane(shell); scroll.setPannable(true); scroll.getStyleClass().add("editor-scroll");
        VBox center = new VBox(tools, scroll); VBox.setVgrow(scroll, Priority.ALWAYS);
        installCanvasHandlers();
        return center;
    }

    private Node rightPane() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE); tabs.setPrefWidth(355);
        ScrollPane inspectorScroll = new ScrollPane(inspector); inspectorScroll.setFitToWidth(true);
        inspectorTab.setContent(inspectorScroll); scriptTab.setContent(scriptPane()); tabs.getTabs().addAll(inspectorTab, scriptTab);
        return tabs;
    }

    private ToggleButton toolButton(String text, ToggleGroup group, boolean active, Tool next) {
        ToggleButton b = new ToggleButton(text); b.setToggleGroup(group); b.setSelected(active);
        b.setOnAction(e -> {
            tool = next;
            app.status(switch (next) {
                case SELECT -> "Seleccionar: clic selecciona, arrastra mueve y doble clic abre Script.";
                case TILE -> "Tile: clic o arrastre pinta únicamente el tile elegido.";
                case ERASE -> "Borrar: clic o arrastre devuelve la celda al tile 0.";
                case ENTITY -> "Objeto: un clic coloca un objeto y vuelve automáticamente a Seleccionar.";
            });
        });
        return b;
    }

    private Node scriptPane() {
        script.setStyle("-fx-font-family: 'Consolas','JetBrains Mono',monospace; -fx-font-size: 13px;"); script.setWrapText(false);
        script.textProperty().addListener((o,a,b) -> { if (!loading && selected != null) { selected.script = b; app.changed(); validate(false); } });
        Button validate = new Button("Validar script"); validate.getStyleClass().add("primary-button"); validate.setOnAction(e -> validate(true));
        Button template = new Button("Insertar plantilla"); template.setOnAction(e -> { if (selected != null) script.setText(template()); });
        Label help = new Label("Eventos: start · update · click · doubleClick · collision · trigger\nComandos: move · velocity · teleport · bounce · destroy · loadScene · setSprite · setVar · addVar · set · ifKey · ifPressed · log");
        help.getStyleClass().add("muted"); help.setWrapText(true);
        VBox box = new VBox(8, new HBox(8, validate, template), scriptStatus, script, help); box.setPadding(new Insets(12)); VBox.setVgrow(script, Priority.ALWAYS); return box;
    }

    private void installCanvasHandlers() {
        canvas.setOnMousePressed(e -> {
            Level level = level(); if (level == null) return;
            double z = zoom.getValue(), wx = e.getX()/z, wy = e.getY()/z;
            if (e.getButton() == MouseButton.SECONDARY) {
                if (tool == Tool.TILE || tool == Tool.ERASE) paint((int)wx, (int)wy, true);
                return;
            }
            switch (tool) {
                case SELECT -> {
                    selected = hit(wx, wy); hierarchy.getSelectionModel().select(selected);
                    if (selected != null) {
                        dragging = true; dragX = wx - selected.x; dragY = wy - selected.y;
                        if (e.getClickCount() >= 2) tabs.getSelectionModel().select(scriptTab);
                    }
                    rebuildInspector(); redraw();
                }
                case TILE -> paint((int)wx, (int)wy, false);
                case ERASE -> paint((int)wx, (int)wy, true);
                case ENTITY -> {
                    selected = createObject(wx, wy); refreshHierarchy(); hierarchy.getSelectionModel().select(selected);
                    tool = Tool.SELECT; selectTool.setSelected(true);
                    rebuildInspector(); redraw();
                }
            }
        });
        canvas.setOnMouseDragged(e -> {
            double z = zoom.getValue(), wx = e.getX()/z, wy = e.getY()/z;
            if (tool == Tool.SELECT && dragging && selected != null) {
                selected.x = snap(wx - dragX); selected.y = snap(wy - dragY); app.changed(); redraw();
            } else if (tool == Tool.TILE) paint((int)wx, (int)wy, false);
            else if (tool == Tool.ERASE) paint((int)wx, (int)wy, true);
        });
        canvas.setOnMouseReleased(e -> { dragging = false; if (selected != null) rebuildInspector(); });
    }

    private void refresh() {
        scenePicker.setItems(FXCollections.observableArrayList(app.project().getLevels().values()));
        if (!scenePicker.getItems().isEmpty()) {
            Level start = app.project().getLevels().get(app.project().getStartLevel());
            scenePicker.setValue(start == null ? scenePicker.getItems().getFirst() : start);
        }
        tilePicker.setItems(FXCollections.observableArrayList(app.project().getTiles().values()));
        if (!tilePicker.getItems().isEmpty()) tilePicker.setValue(tilePicker.getItems().getFirst());
        refreshHierarchy(); resizeCanvas(); rebuildInspector();
    }
    private void refreshHierarchy() {
        Level l = level(); hierarchy.setItems(FXCollections.observableArrayList(l == null ? List.of() : l.entities));
        if (selected != null) hierarchy.getSelectionModel().select(selected);
    }
    private Level level() { return scenePicker.getValue(); }
    private void resizeCanvas() {
        Level l = level(); if (l == null) return; double z = zoom.getValue(); canvas.setWidth(l.width*z); canvas.setHeight(l.height*z); redraw();
    }

    private void redraw() {
        Level l = level(); if (l == null) return; double z = zoom.getValue(); GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);
        g.setFill(Color.web("#101620")); g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());
        for (int y=0;y<l.height;y++) for (int x=0;x<l.width;x++) {
            TileDef t = app.project().getTiles().get(l.get(x,y)); Image image = t == null ? null : app.image(t.assetKey);
            if (image != null) g.drawImage(image,x*z,y*z,z,z);
            else { java.awt.Color c=t==null?java.awt.Color.MAGENTA:t.color; g.setFill(Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0)); g.fillRect(x*z,y*z,z,z); }
            g.setStroke(Color.rgb(255,255,255,.055)); g.strokeRect(x*z,y*z,z,z);
        }
        l.entities.stream().filter(e -> e.enabled).sorted(Comparator.comparingInt(e -> e.layer)).forEach(e -> {
            double x=e.x*z,y=e.y*z,w=e.width*z,h=e.height*z; Image image=app.image(e.assetKey);
            if(image!=null) g.drawImage(image,x,y,w,h); else { g.setFill(Color.web("#55b7ff")); g.fillRoundRect(x,y,w,h,7,7); }
            if(e==selected){g.setStroke(Color.web("#52d3ff"));g.setLineWidth(2);g.strokeRect(x-2,y-2,w+4,h+4);}
        });
    }

    private EntityDef hit(double x, double y) {
        Level l=level(); if(l==null)return null;
        return l.entities.stream().filter(e->e.enabled&&x>=e.x&&y>=e.y&&x<=e.x+e.width&&y<=e.y+e.height).max(Comparator.comparingInt(e->e.layer)).orElse(null);
    }
    private void paint(int x,int y,boolean erase) {
        Level l=level(); if(l==null||x<0||y<0||x>=l.width||y>=l.height)return;
        TileDef t=tilePicker.getValue(); l.set(x,y,erase||t==null?0:t.id); app.changed(); redraw();
    }
    private EntityDef createObject(double x,double y) {
        Level l=level(); Set<String> ids=new HashSet<>(); l.entities.forEach(e->ids.add(e.id)); int n=1; while(ids.contains("object"+n))n++;
        EntityDef e=new EntityDef("object"+n,"Objeto "+n,snap(x),snap(y)); e.components.add(ComponentDef.preset("BoxCollider2D")); l.entities.add(e); app.changed(); return e;
    }
    private static double snap(double n){return Math.round(n*4)/4.0;}

    private void rebuildInspector() {
        loading=true; inspector.getChildren().clear(); inspector.setPadding(new Insets(12));
        if(selected==null){inspector.getChildren().addAll(title("INSPECTOR"),new Label("Selecciona un objeto en la escena o en la jerarquía."));script.clear();scriptStatus.setText("Selecciona un objeto");loading=false;return;}
        TextField name=field(selected.name),x=field(number(selected.x)),y=field(number(selected.y)),w=field(number(selected.width)),h=field(number(selected.height));
        CheckBox enabled=new CheckBox("Activo");enabled.setSelected(selected.enabled);
        ComboBox<String> sprite=new ComboBox<>();sprite.getItems().add("(sin sprite)");sprite.getItems().addAll(app.project().getAssets().keySet());sprite.setValue(selected.assetKey.isBlank()?"(sin sprite)":selected.assetKey);
        GridPane transform=form(); row(transform,0,"Nombre",name);row(transform,1,"X",x);row(transform,2,"Y",y);row(transform,3,"Ancho",w);row(transform,4,"Alto",h);row(transform,5,"Sprite",sprite);transform.add(enabled,1,6);
        Runnable apply=()->{if(loading||selected==null)return;selected.name=name.getText().isBlank()?selected.id:name.getText().trim();selected.x=parse(x,selected.x);selected.y=parse(y,selected.y);selected.width=Math.max(.1,parse(w,selected.width));selected.height=Math.max(.1,parse(h,selected.height));selected.enabled=enabled.isSelected();selected.assetKey="(sin sprite)".equals(sprite.getValue())?"":sprite.getValue();app.changed();refreshHierarchy();redraw();};
        for(TextField f:List.of(name,x,y,w,h)){f.setOnAction(e->apply.run());f.focusedProperty().addListener((o,a,b)->{if(!b)apply.run();});}enabled.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());
        VBox cards=new VBox(8);for(ComponentDef c:selected.components)cards.getChildren().add(componentCard(c));
        ComboBox<String> addType=new ComboBox<>(FXCollections.observableArrayList(GameProject.BUILTIN_COMPONENTS));addType.setPromptText("Añadir comportamiento…");
        Button add=new Button("＋ Añadir");add.setOnAction(e->{String type=addType.getValue();if(type!=null&&selected.component(type)==null){selected.components.add(ComponentDef.preset(type));app.changed();rebuildInspector();}});
        HBox addBar=new HBox(6,addType,add);HBox.setHgrow(addType,Priority.ALWAYS);
        inspector.getChildren().addAll(title(selected.name.toUpperCase(Locale.ROOT)),transform,new Separator(),title("COMPORTAMIENTOS"),cards,addBar);
        script.setText(selected.script);validate(false);loading=false;
    }

    private Node componentCard(ComponentDef component) {
        VBox card=new VBox(6);card.getStyleClass().add("component-card");Label label=new Label(component.type);label.getStyleClass().add("component-title");
        Region space=new Region();HBox.setHgrow(space,Priority.ALWAYS);Button remove=new Button("×");remove.getStyleClass().add("danger-ghost");
        remove.setOnAction(e->{selected.components.remove(component);app.changed();rebuildInspector();});card.getChildren().add(new HBox(label,space,remove));
        for(var prop:component.properties.entrySet()){
            TextField f=field(prop.getValue());Runnable save=()->{prop.setValue(f.getText());app.changed();};f.setOnAction(e->save.run());f.focusedProperty().addListener((o,a,b)->{if(!b)save.run();});
            HBox line=new HBox(8,new Label(prop.getKey()),f);line.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(f,Priority.ALWAYS);card.getChildren().add(line);
        }
        return card;
    }

    private void validate(boolean popup) {
        if(selected==null)return;ScriptProgram.Validation v=ScriptProgram.compile(script.getText()).validation();
        scriptStatus.setText(v.valid()?"✓ Script válido":"⚠ "+String.join(" · ",v.errors()));scriptStatus.getStyleClass().removeAll("ok","error");scriptStatus.getStyleClass().add(v.valid()?"ok":"error");
        if(popup&&!v.valid())app.error("Errores de script",String.join("\n",v.errors()));
    }
    private String template(){return "# 2GameScript\non start\n  log \"Objeto iniciado\"\nend\n\non update\n  # ifKey SPACE move 0 -0.05\nend\n\non click\n  log \"clic\"\nend\n\non collision\n  # bounce\nend\n";}

    private void addScene(){TextInputDialog d=new TextInputDialog("Nueva escena");d.setHeaderText("Crear escena");d.showAndWait().ifPresent(name->{if(name.isBlank())return;String id=slug(name,app.project().getLevels().keySet());Level l=new Level(id,name.trim(),24,16);app.project().getLevels().put(id,l);app.changed();scenePicker.getItems().add(l);scenePicker.setValue(l);});}
    private void removeScene(){Level l=level();if(l==null||app.project().getLevels().size()<=1)return;app.project().getLevels().remove(l.id);if(Objects.equals(app.project().getStartLevel(),l.id))app.project().setStartLevel(app.project().getLevels().keySet().iterator().next());app.changed();refresh();}
    private void addAtCenter(){Level l=level();if(l==null)return;selected=createObject(l.width/2.0,l.height/2.0);refreshHierarchy();hierarchy.getSelectionModel().select(selected);rebuildInspector();redraw();}
    private void removeSelected(){Level l=level();if(l==null||selected==null)return;l.entities.remove(selected);selected=null;app.changed();refreshHierarchy();rebuildInspector();redraw();}

    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static TextField field(String text){return new TextField(text==null?"":text);}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(75);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}
    private static double parse(TextField f,double def){try{return Double.parseDouble(f.getText());}catch(Exception e){return def;}}
    private static String number(double n){return n==Math.rint(n)?Long.toString((long)n):String.format(Locale.ROOT,"%.2f",n);}
    private static String slug(String name,Collection<String> used){String base=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="scene";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}
}
