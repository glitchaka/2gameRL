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
    private record RenderPass(int z, Runnable draw) {}

    private final StudioApp app;
    private final ComboBox<Level> scenePicker = new ComboBox<>();
    private final ListView<TileLayer> tileLayers = new ListView<>();
    private final ListView<EntityDef> hierarchy = new ListView<>();
    private final ComboBox<TileDef> tilePicker = new ComboBox<>();
    private final ComboBox<Integer> zoomScale = new ComboBox<>(FXCollections.observableArrayList(1, 2, 3, 4));
    private final Label gridInfo = new Label();
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
        zoomScale.setValue(1);
        getItems().addAll(leftPane(), centerPane(), rightPane());
        setDividerPositions(.21, .76);
        refresh();
    }

    private Node leftPane() {
        VBox box = new VBox(8);
        box.getStyleClass().add("side-panel");
        box.setPadding(new Insets(12));
        box.setPrefWidth(300);

        scenePicker.setMaxWidth(Double.MAX_VALUE);
        scenePicker.setOnAction(e -> {
            selected = null;
            refreshTileLayers();
            refreshHierarchy();
            resizeCanvas();
            rebuildInspector();
        });
        Button addScene = new Button("＋ Escena"), removeScene = new Button("Eliminar");
        addScene.setOnAction(e -> addScene()); removeScene.setOnAction(e -> removeScene());
        HBox sceneActions = new HBox(6, addScene, removeScene);

        tileLayers.setPrefHeight(165);
        tileLayers.setCellFactory(v -> new ListCell<>() {
            @Override protected void updateItem(TileLayer layer, boolean empty) {
                super.updateItem(layer, empty);
                if (empty || layer == null) { setText(""); return; }
                setText((layer.visible ? "◉ " : "○ ") + (layer.locked ? "🔒 " : "") + (layer.collision ? "◆ " : "") + layer.name);
            }
        });
        tileLayers.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> redraw());
        Button addLayer = new Button("＋ Capa"), deleteLayer = new Button("Eliminar");
        Button back = new Button("Enviar atrás"), front = new Button("Traer delante");
        addLayer.setOnAction(e -> addTileLayer());
        deleteLayer.setOnAction(e -> deleteTileLayer());
        back.setOnAction(e -> moveTileLayer(-1));
        front.setOnAction(e -> moveTileLayer(1));
        Button layerProps = new Button("Propiedades de capa"); layerProps.setMaxWidth(Double.MAX_VALUE); layerProps.setOnAction(e -> editTileLayer());
        Button layerSettings = new Button("Capas y matriz de colisiones…"); layerSettings.getStyleClass().add("primary-button"); layerSettings.setMaxWidth(Double.MAX_VALUE);
        layerSettings.setOnAction(e -> LayerSettingsDialog.show(app.owner(), app.project(), () -> { refreshTileLayers(); rebuildInspector(); redraw(); app.changed(); }));

        hierarchy.setPrefHeight(280);
        hierarchy.getSelectionModel().selectedItemProperty().addListener((o,a,b) -> { selected = b; redraw(); rebuildInspector(); });
        hierarchy.setOnMouseClicked(e -> { if (e.getClickCount() >= 2 && selected != null) tabs.getSelectionModel().select(scriptTab); });
        Button addObject = new Button("＋ Objeto"), removeObject = new Button("Eliminar");
        addObject.setOnAction(e -> addAtCenter()); removeObject.setOnAction(e -> removeSelected());
        HBox objectActions = new HBox(6, addObject, removeObject);
        VBox.setVgrow(hierarchy, Priority.ALWAYS);

        Label layerHelp = muted("Orden de tiles: arriba en el dibujo = más adelante. ◉ visible · 🔒 bloqueada · ◆ colisiona.");
        box.getChildren().addAll(
                title("ESCENA"), scenePicker, sceneActions,
                new Separator(), title("CAPAS DE TILES"), tileLayers,
                new HBox(6, addLayer, deleteLayer), new HBox(6, back, front), layerProps, layerSettings, layerHelp,
                new Separator(), title("OBJETOS DE LA ESCENA"), hierarchy, objectActions
        );
        return box;
    }

    private Node centerPane() {
        ToggleGroup group = new ToggleGroup();
        selectTool = toolButton("Seleccionar", group, true, Tool.SELECT);
        ToggleButton tile = toolButton("Tile", group, false, Tool.TILE);
        ToggleButton erase = toolButton("Borrar", group, false, Tool.ERASE);
        ToggleButton object = toolButton("Objeto", group, false, Tool.ENTITY);
        tilePicker.setPrefWidth(175);
        zoomScale.setPrefWidth(72);
        zoomScale.setButtonCell(scaleCell());
        zoomScale.setCellFactory(v -> scaleCell());
        zoomScale.valueProperty().addListener((o,a,b) -> resizeCanvas());
        gridInfo.getStyleClass().add("muted");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox tools = new HBox(7, selectTool, tile, erase, object, new Separator(), new Label("Tile:"), tilePicker, spacer, new Label("Zoom"), zoomScale, gridInfo);
        tools.setAlignment(Pos.CENTER_LEFT); tools.setPadding(new Insets(8)); tools.getStyleClass().add("context-toolbar");
        StackPane shell = new StackPane(canvas); shell.getStyleClass().add("canvas-shell"); shell.setPadding(new Insets(28));
        ScrollPane scroll = new ScrollPane(shell); scroll.setPannable(true); scroll.getStyleClass().add("editor-scroll");
        VBox center = new VBox(tools, scroll); VBox.setVgrow(scroll, Priority.ALWAYS);
        installCanvasHandlers();
        return center;
    }

    private ListCell<Integer> scaleCell() {
        return new ListCell<>() {
            @Override protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item + "×");
            }
        };
    }

    private Node rightPane() {
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefWidth(385);
        ScrollPane inspectorScroll = new ScrollPane(inspector); inspectorScroll.setFitToWidth(true); inspectorScroll.getStyleClass().add("editor-scroll");
        inspectorTab.setContent(inspectorScroll);
        scriptTab.setContent(scriptPane());
        tabs.getTabs().addAll(inspectorTab, scriptTab);
        return tabs;
    }

    private ToggleButton toolButton(String text, ToggleGroup group, boolean active, Tool next) {
        ToggleButton b = new ToggleButton(text); b.setToggleGroup(group); b.setSelected(active);
        b.setOnAction(e -> {
            tool = next;
            app.status(switch (next) {
                case SELECT -> "Seleccionar: clic selecciona, arrastra mueve y doble clic abre Script.";
                case TILE -> "Tile: pinta exactamente una celda en la capa de tiles activa.";
                case ERASE -> "Borrar: deja vacía la celda de la capa activa.";
                case ENTITY -> "Objeto: un clic coloca una entidad y vuelve a Seleccionar.";
            });
        });
        return b;
    }

    private Node scriptPane() {
        script.setStyle("-fx-font-family:'Consolas','JetBrains Mono',monospace;-fx-font-size:13px;");
        script.setWrapText(false);
        script.textProperty().addListener((o,a,b) -> { if (!loading && selected != null) { selected.script = b; app.changed(); validate(false); } });
        Button validate = new Button("Validar script"); validate.getStyleClass().add("primary-button"); validate.setOnAction(e -> validate(true));
        Button template = new Button("Insertar plantilla"); template.setOnAction(e -> { if (selected != null) script.setText(template()); });
        Button tutorial = new Button("Tutorial"); tutorial.setOnAction(e -> ScriptTutorialDialog.show(app.owner(), this::insertTutorialExample));
        Label help = muted("Eventos: start · update · click · doubleClick · collision · trigger\nComandos: move · velocity · teleport · bounce · destroy · loadScene · setSprite · setVar · addVar · set · ifKey · ifPressed · log");
        VBox box = new VBox(8, new HBox(8, validate, template, tutorial), scriptStatus, script, help); box.setPadding(new Insets(12)); VBox.setVgrow(script, Priority.ALWAYS); return box;
    }

    private void insertTutorialExample(String example) {
        if (selected == null || example == null || example.isBlank()) { app.status("Selecciona un objeto antes de insertar un ejemplo de script."); return; }
        int caret = Math.max(0, Math.min(script.getCaretPosition(), script.getLength()));
        String current = script.getText(), before = current.substring(0, caret), after = current.substring(caret);
        String prefix = caret == 0 ? "" : (before.endsWith("\n\n") ? "" : before.endsWith("\n") ? "\n" : "\n\n");
        String suffix = caret >= current.length() ? "" : (after.startsWith("\n\n") ? "" : after.startsWith("\n") ? "\n" : "\n\n");
        String block = prefix + example.strip() + suffix;
        script.insertText(caret, block); tabs.getSelectionModel().select(scriptTab); script.requestFocus(); script.positionCaret(caret + block.length() - suffix.length());
        app.status("Ejemplo insertado en el script de " + selected.name + ".");
    }

    private void installCanvasHandlers() {
        canvas.setOnMousePressed(e -> {
            Level level = level(); if (level == null) return;
            double cell = cellSize(), wx = e.getX()/cell, wy = e.getY()/cell;
            if (e.getButton() == MouseButton.SECONDARY) {
                if (tool == Tool.TILE || tool == Tool.ERASE) paint(cellX(e.getX()), cellY(e.getY()), true);
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
                case TILE -> paint(cellX(e.getX()), cellY(e.getY()), false);
                case ERASE -> paint(cellX(e.getX()), cellY(e.getY()), true);
                case ENTITY -> {
                    selected = createObject(wx, wy); refreshHierarchy(); hierarchy.getSelectionModel().select(selected);
                    tool = Tool.SELECT; selectTool.setSelected(true); rebuildInspector(); redraw();
                }
            }
        });
        canvas.setOnMouseDragged(e -> {
            double cell = cellSize(), wx = e.getX()/cell, wy = e.getY()/cell;
            if (tool == Tool.SELECT && dragging && selected != null) {
                selected.x = snap(wx - dragX); selected.y = snap(wy - dragY); app.changed(); redraw();
            } else if (tool == Tool.TILE) paint(cellX(e.getX()), cellY(e.getY()), false);
            else if (tool == Tool.ERASE) paint(cellX(e.getX()), cellY(e.getY()), true);
        });
        canvas.setOnMouseReleased(e -> { dragging = false; if (selected != null) rebuildInspector(); });
    }

    private int cellX(double pixelX) { return (int)Math.floor(pixelX / cellSize()); }
    private int cellY(double pixelY) { return (int)Math.floor(pixelY / cellSize()); }
    private double cellSize() { return app.project().getTileSize() * Math.max(1, zoomScale.getValue() == null ? 1 : zoomScale.getValue()); }

    private void refresh() {
        scenePicker.setItems(FXCollections.observableArrayList(app.project().getLevels().values()));
        if (!scenePicker.getItems().isEmpty()) {
            Level start = app.project().getLevels().get(app.project().getStartLevel());
            scenePicker.setValue(start == null ? scenePicker.getItems().getFirst() : start);
        }
        tilePicker.setItems(FXCollections.observableArrayList(app.project().getTiles().values()));
        if (!tilePicker.getItems().isEmpty()) tilePicker.setValue(tilePicker.getItems().getFirst());
        refreshTileLayers(); refreshHierarchy(); resizeCanvas(); rebuildInspector();
    }

    private void refreshTileLayers() {
        Level l = level();
        TileLayer previous = tileLayers.getSelectionModel().getSelectedItem();
        List<TileLayer> ordered = l == null ? List.of() : l.tileLayers.stream().sorted(Comparator.comparingInt(layer -> layer.order)).toList();
        tileLayers.setItems(FXCollections.observableArrayList(ordered));
        if (previous != null && tileLayers.getItems().contains(previous)) tileLayers.getSelectionModel().select(previous);
        else if (!tileLayers.getItems().isEmpty()) tileLayers.getSelectionModel().selectFirst();
    }

    private void refreshHierarchy() {
        Level l = level(); hierarchy.setItems(FXCollections.observableArrayList(l == null ? List.of() : l.entities));
        if (selected != null) hierarchy.getSelectionModel().select(selected);
    }

    private Level level() { return scenePicker.getValue(); }
    private TileLayer activeTileLayer() { return tileLayers.getSelectionModel().getSelectedItem(); }

    private void resizeCanvas() {
        Level l = level(); if (l == null) return;
        double cell = cellSize(); canvas.setWidth(l.width * cell); canvas.setHeight(l.height * cell);
        gridInfo.setText("Celda " + (int)cell + " px · " + (zoomScale.getValue() == null ? 1 : zoomScale.getValue()) + "×"); redraw();
    }

    private void redraw() {
        Level l = level(); if (l == null) return;
        double cell = cellSize(); GraphicsContext g = canvas.getGraphicsContext2D(); g.setImageSmoothing(false);
        g.setFill(Color.web("#101620")); g.fillRect(0,0,canvas.getWidth(),canvas.getHeight());

        List<RenderPass> passes = new ArrayList<>();
        for (TileLayer layer : l.tileLayers) if (layer.visible) passes.add(new RenderPass(layer.order * 1000, () -> drawTileLayer(g, l, layer, cell)));
        for (EntityDef entity : l.entities) if (entity.enabled) passes.add(new RenderPass(entityZ(entity), () -> drawEntity(g, entity, cell)));
        passes.stream().sorted(Comparator.comparingInt(RenderPass::z)).forEach(p -> p.draw.run());

        g.setFill(Color.rgb(255,255,255,.11));
        for (int x=0;x<=l.width;x++) g.fillRect(Math.round(x*cell),0,1,l.height*cell);
        for (int y=0;y<=l.height;y++) g.fillRect(0,Math.round(y*cell),l.width*cell,1);
        drawSelectedCollider(g, cell);
    }

    private void drawTileLayer(GraphicsContext g, Level l, TileLayer layer, double cell) {
        for (int y=0;y<l.height;y++) for (int x=0;x<l.width;x++) {
            int id = layer.get(x,y); if (id < 0) continue;
            TileDef t = app.project().getTiles().get(id); if (t == null) continue;
            double px = Math.round(x * cell), py = Math.round(y * cell);
            Image image = app.image(t.assetKey);
            if (image != null) g.drawImage(image, px, py, cell, cell);
            else {
                java.awt.Color c=t.color; g.setFill(Color.rgb(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha()/255.0)); g.fillRect(px,py,cell,cell);
            }
        }
    }

    private void drawEntity(GraphicsContext g, EntityDef e, double cell) {
        double x=Math.round(e.x*cell), y=Math.round(e.y*cell), w=Math.max(1,Math.round(e.width*cell)), h=Math.max(1,Math.round(e.height*cell));
        Image image=app.image(e.assetKey);
        if(image!=null) g.drawImage(image,x,y,w,h);
        else { g.setFill(Color.web("#55b7ff")); g.fillRoundRect(x,y,w,h,7,7); }
        if(e==selected){g.setStroke(Color.web("#52d3ff"));g.setLineWidth(2);g.strokeRect(x-2,y-2,w+4,h+4);}
    }

    private void drawSelectedCollider(GraphicsContext g, double cell) {
        if (selected == null) return;
        ComponentDef collider = selected.component("BoxCollider2D");
        if (collider == null || !collider.bool("enabled", true)) return;
        double w = Math.max(.01, collider.number("width", selected.width)) * cell;
        double h = Math.max(.01, collider.number("height", selected.height)) * cell;
        double x = Math.round(selected.x * cell), y = Math.round(selected.y * cell);
        g.setStroke(Color.rgb(255,92,92,.95)); g.setLineWidth(2); g.setLineDashes(6,4); g.strokeRect(x,y,w,h); g.setLineDashes();
        g.setFill(Color.rgb(255,92,92,.9)); g.fillText("BoxCollider2D · " + selected.physicsLayer, x+4, Math.max(12,y-4));
    }

    private int entityZ(EntityDef e) { return app.project().renderOrder(e.renderLayer) * 1000 + 500 + e.layer; }

    private EntityDef hit(double x, double y) {
        Level l=level(); if(l==null)return null;
        return l.entities.stream().filter(e->e.enabled&&x>=e.x&&y>=e.y&&x<=e.x+e.width&&y<=e.y+e.height).max(Comparator.comparingInt(this::entityZ)).orElse(null);
    }

    private void paint(int x,int y,boolean erase) {
        Level l=level(); TileLayer layer=activeTileLayer();
        if(l==null||layer==null||x<0||y<0||x>=l.width||y>=l.height)return;
        if(layer.locked){app.status("La capa '"+layer.name+"' está bloqueada.");return;}
        TileDef t=tilePicker.getValue(); layer.set(x,y,erase||t==null?-1:t.id); app.changed(); redraw();
    }

    private EntityDef createObject(double x,double y) {
        Level l=level(); Set<String> ids=new HashSet<>(); l.entities.forEach(e->ids.add(e.id)); int n=1; while(ids.contains("object"+n))n++;
        EntityDef e=new EntityDef("object"+n,"Objeto "+n,snap(x),snap(y)); e.renderLayer="Objetos";e.physicsLayer="Default";e.components.add(ComponentDef.preset("BoxCollider2D")); l.entities.add(e); app.changed(); return e;
    }
    private static double snap(double n){return Math.round(n*4)/4.0;}

    private void rebuildInspector() {
        loading=true; inspector.getChildren().clear(); inspector.setPadding(new Insets(12));
        if(selected==null){inspector.getChildren().addAll(title("INSPECTOR"),new Label("Selecciona un objeto en la escena o en la jerarquía."),muted("Los tiles se editan en la capa activa de la izquierda."));script.clear();scriptStatus.setText("Selecciona un objeto");loading=false;return;}

        TextField name=field(selected.name),x=field(number(selected.x)),y=field(number(selected.y)),w=field(number(selected.width)),h=field(number(selected.height)),localOrder=field(Integer.toString(selected.layer));
        CheckBox enabled=new CheckBox("Activo"); enabled.setSelected(selected.enabled);
        ComboBox<String> sprite=new ComboBox<>();sprite.getItems().add("(sin sprite)");sprite.getItems().addAll(app.project().getAssets().keySet());sprite.setValue(selected.assetKey.isBlank()?"(sin sprite)":selected.assetKey);
        ComboBox<String> renderLayer=new ComboBox<>(FXCollections.observableArrayList(app.project().getRenderLayers()));renderLayer.setValue(selected.renderLayer);renderLayer.setMaxWidth(Double.MAX_VALUE);
        ComboBox<String> physicsLayer=new ComboBox<>(FXCollections.observableArrayList(app.project().getPhysicsLayers().keySet()));physicsLayer.setValue(selected.physicsLayer);physicsLayer.setMaxWidth(Double.MAX_VALUE);
        GridPane transform=form();
        row(transform,0,"Nombre",name);row(transform,1,"X",x);row(transform,2,"Y",y);row(transform,3,"Ancho",w);row(transform,4,"Alto",h);row(transform,5,"Sprite",sprite);row(transform,6,"Capa visual",renderLayer);row(transform,7,"Capa física",physicsLayer);row(transform,8,"Orden local",localOrder);transform.add(enabled,1,9);
        Runnable apply=()->{
            if(loading||selected==null)return;
            selected.name=name.getText().isBlank()?selected.id:name.getText().trim();selected.x=parse(x,selected.x);selected.y=parse(y,selected.y);selected.width=Math.max(.1,parse(w,selected.width));selected.height=Math.max(.1,parse(h,selected.height));selected.enabled=enabled.isSelected();selected.assetKey="(sin sprite)".equals(sprite.getValue())?"":sprite.getValue();
            if(renderLayer.getValue()!=null)selected.renderLayer=renderLayer.getValue();if(physicsLayer.getValue()!=null)selected.physicsLayer=physicsLayer.getValue();selected.layer=(int)Math.round(parse(localOrder,selected.layer));
            app.changed();refreshHierarchy();redraw();
        };
        for(TextField f:List.of(name,x,y,w,h,localOrder)){f.setOnAction(e->apply.run());f.focusedProperty().addListener((o,a,b)->{if(!b)apply.run();});}
        enabled.setOnAction(e->apply.run());sprite.setOnAction(e->apply.run());renderLayer.setOnAction(e->apply.run());physicsLayer.setOnAction(e->apply.run());

        VBox cards=new VBox(8);for(ComponentDef c:selected.components)cards.getChildren().add(componentCard(c));
        ComboBox<String> addType=new ComboBox<>(FXCollections.observableArrayList(GameProject.BUILTIN_COMPONENTS));addType.setPromptText("Añadir comportamiento…");
        Button add=new Button("＋ Añadir");
        add.setOnAction(e->{String type=addType.getValue();if(type!=null&&selected.component(type)==null){selected.components.add(ComponentDef.preset(type));app.changed();app.status(type+" añadido a "+selected.name+". Se aplicará al probar el juego.");rebuildInspector();redraw();}});
        HBox addBar=new HBox(6,addType,add);HBox.setHgrow(addType,Priority.ALWAYS);
        Label behaviorHint=muted("BoxCollider2D bloquea por sí solo. Rigidbody2D no es requisito para colisionar. La capa física solo filtra qué categorías se ignoran.");
        inspector.getChildren().addAll(title(selected.name.toUpperCase(Locale.ROOT)),transform,new Separator(),title("COMPORTAMIENTOS"),cards,addBar,behaviorHint);
        script.setText(selected.script);validate(false);loading=false;
    }

    private Node componentCard(ComponentDef component) {
        VBox card=new VBox(6);card.getStyleClass().add("component-card");
        Label label=new Label(component.type);label.getStyleClass().add("component-title");
        Label description=muted(GameProject.componentDescription(component.type));description.getStyleClass().add("component-description");
        Region space=new Region();HBox.setHgrow(space,Priority.ALWAYS);Button remove=new Button("×");remove.getStyleClass().add("danger-ghost");
        remove.setOnAction(e->{selected.components.remove(component);app.changed();rebuildInspector();redraw();});
        card.getChildren().addAll(new HBox(label,space,remove),description);
        for(var prop:component.properties.entrySet()){
            if(isBooleanProperty(prop.getKey(),prop.getValue())){
                CheckBox check=new CheckBox(prop.getKey());check.setSelected(Boolean.parseBoolean(prop.getValue()));check.setOnAction(e->{prop.setValue(Boolean.toString(check.isSelected()));app.changed();redraw();});card.getChildren().add(check);
            }else if(component.type.equals("Patrol")&&prop.getKey().equals("axis")){
                ComboBox<String> axis=new ComboBox<>(FXCollections.observableArrayList("x","y"));axis.setValue(prop.getValue());axis.setOnAction(e->{prop.setValue(axis.getValue());app.changed();});HBox line=new HBox(8,new Label("axis"),axis);HBox.setHgrow(axis,Priority.ALWAYS);card.getChildren().add(line);
            }else{
                TextField f=field(prop.getValue());Runnable save=()->{prop.setValue(f.getText());app.changed();redraw();};f.setOnAction(e->save.run());f.focusedProperty().addListener((o,a,b)->{if(!b)save.run();});HBox line=new HBox(8,new Label(prop.getKey()),f);line.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(f,Priority.ALWAYS);card.getChildren().add(line);
            }
        }
        return card;
    }

    private static boolean isBooleanProperty(String key,String value){return key.equals("enabled")||key.equals("solid")||key.equals("allowArrows")||key.equals("once")||"true".equalsIgnoreCase(value)||"false".equalsIgnoreCase(value);}

    private void validate(boolean popup) {
        if(selected==null)return;ScriptProgram.Validation v=ScriptProgram.compile(script.getText()).validation();
        scriptStatus.setText(v.valid()?"✓ Script válido":"⚠ "+String.join(" · ",v.errors()));scriptStatus.getStyleClass().removeAll("ok","error");scriptStatus.getStyleClass().add(v.valid()?"ok":"error");
        if(popup&&!v.valid())app.error("Errores de script",String.join("\n",v.errors()));
    }
    private String template(){return "# 2GameScript\non start\n  log \"Objeto iniciado\"\nend\n\non update\n  # ifKey SPACE move 0 -0.05\nend\n\non click\n  log \"clic\"\nend\n\non collision\n  log \"colisión\"\nend\n";}

    private void addTileLayer(){
        Level l=level();if(l==null)return;int n=1;Set<String>ids=new HashSet<>();l.tileLayers.forEach(x->ids.add(x.id));while(ids.contains("layer-"+n))n++;
        TileLayer layer=new TileLayer("layer-"+n,"Capa "+n,l.width,l.height,l.tileLayers.size());int[]empty=new int[l.width*l.height];Arrays.fill(empty,-1);layer.replaceCells(empty);l.tileLayers.add(layer);normalizeLayerOrder(l);app.changed();refreshTileLayers();tileLayers.getSelectionModel().select(layer);redraw();
    }
    private void deleteTileLayer(){Level l=level();TileLayer layer=activeTileLayer();if(l==null||layer==null||l.tileLayers.size()<=1)return;l.tileLayers.remove(layer);normalizeLayerOrder(l);app.changed();refreshTileLayers();redraw();}
    private void moveTileLayer(int direction){Level l=level();TileLayer layer=activeTileLayer();if(l==null||layer==null)return;List<TileLayer>ordered=new ArrayList<>(l.tileLayers);ordered.sort(Comparator.comparingInt(x->x.order));int i=ordered.indexOf(layer),j=i+direction;if(j<0||j>=ordered.size())return;Collections.swap(ordered,i,j);l.tileLayers.clear();l.tileLayers.addAll(ordered);normalizeLayerOrder(l);app.changed();refreshTileLayers();tileLayers.getSelectionModel().select(layer);redraw();}
    private static void normalizeLayerOrder(Level l){for(int i=0;i<l.tileLayers.size();i++)l.tileLayers.get(i).order=i;}

    private void editTileLayer(){
        TileLayer layer=activeTileLayer();if(layer==null)return;
        Dialog<Void>d=new Dialog<>();d.initOwner(app.owner());d.setTitle("Propiedades de capa");d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        TextField name=new TextField(layer.name);CheckBox visible=new CheckBox("Visible");visible.setSelected(layer.visible);CheckBox locked=new CheckBox("Bloqueada");locked.setSelected(layer.locked);CheckBox collision=new CheckBox("Participa en colisiones");collision.setSelected(layer.collision);ComboBox<String>physics=new ComboBox<>(FXCollections.observableArrayList(app.project().getPhysicsLayers().keySet()));physics.setValue(layer.physicsLayer);
        GridPane form=form();row(form,0,"Nombre",name);row(form,1,"Capa física",physics);form.add(visible,1,2);form.add(locked,1,3);form.add(collision,1,4);d.getDialogPane().setContent(form);
        Runnable apply=()->{layer.name=name.getText().isBlank()?layer.id:name.getText().trim();layer.visible=visible.isSelected();layer.locked=locked.isSelected();layer.collision=collision.isSelected();if(physics.getValue()!=null)layer.physicsLayer=physics.getValue();app.changed();refreshTileLayers();redraw();};
        name.textProperty().addListener((o,a,b)->apply.run());visible.setOnAction(e->apply.run());locked.setOnAction(e->apply.run());collision.setOnAction(e->apply.run());physics.setOnAction(e->apply.run());d.showAndWait();
    }

    private void addScene(){TextInputDialog d=new TextInputDialog("Nueva escena");d.setHeaderText("Crear escena");d.showAndWait().ifPresent(name->{if(name.isBlank())return;String id=slug(name,app.project().getLevels().keySet());Level l=new Level(id,name.trim(),24,16);app.project().getLevels().put(id,l);app.changed();scenePicker.getItems().add(l);scenePicker.setValue(l);});}
    private void removeScene(){Level l=level();if(l==null||app.project().getLevels().size()<=1)return;app.project().getLevels().remove(l.id);if(Objects.equals(app.project().getStartLevel(),l.id))app.project().setStartLevel(app.project().getLevels().keySet().iterator().next());app.changed();refresh();}
    private void addAtCenter(){Level l=level();if(l==null)return;selected=createObject(l.width/2.0,l.height/2.0);refreshHierarchy();hierarchy.getSelectionModel().select(selected);rebuildInspector();redraw();}
    private void removeSelected(){Level l=level();if(l==null||selected==null)return;l.entities.remove(selected);selected=null;app.changed();refreshHierarchy();rebuildInspector();redraw();}

    private static Label title(String text){Label l=new Label(text);l.getStyleClass().add("panel-title");return l;}
    private static Label muted(String text){Label l=new Label(text);l.getStyleClass().add("muted");l.setWrapText(true);return l;}
    private static TextField field(String text){return new TextField(text==null?"":text);}
    private static GridPane form(){GridPane g=new GridPane();g.setHgap(8);g.setVgap(7);ColumnConstraints a=new ColumnConstraints();a.setMinWidth(90);ColumnConstraints b=new ColumnConstraints();b.setHgrow(Priority.ALWAYS);g.getColumnConstraints().addAll(a,b);return g;}
    private static void row(GridPane g,int r,String label,Node n){g.add(new Label(label),0,r);g.add(n,1,r);if(n instanceof Region region)region.setMaxWidth(Double.MAX_VALUE);}
    private static double parse(TextField f,double def){try{return Double.parseDouble(f.getText());}catch(Exception e){return def;}}
    private static String number(double n){return n==Math.rint(n)?Long.toString((long)n):String.format(Locale.ROOT,"%.2f",n);}
    private static String slug(String name,Collection<String>used){String base=name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="scene";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}
}
