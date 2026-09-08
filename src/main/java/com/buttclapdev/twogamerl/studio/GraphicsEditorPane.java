package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.TileDef;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

final class GraphicsEditorPane extends SplitPane {
    private static final double PREVIEW_LIMIT = 480;

    private final StudioApp app;
    private final ListView<Asset> assets = new ListView<>();
    private final ImageView preview = new ImageView();
    private final ListView<TileDef> tiles = new ListView<>();
    private final VBox tileInspector = new VBox(10);
    private final Button sliceButton = new Button("Cortar spritesheet");

    GraphicsEditorPane(StudioApp app) {
        this.app = app;
        getItems().addAll(left(), center(), right());
        setDividerPositions(.2, .7);
        refresh();
    }

    private Node left() {
        VBox box = new VBox(8, title("ASSETS"), assets);
        box.getStyleClass().add("side-panel");
        box.setPadding(new Insets(12));
        box.setPrefWidth(300);
        VBox.setVgrow(assets, Priority.ALWAYS);

        Button importB = new Button("Importar imagen");
        Button pixel = new Button("Pixel Lab");
        pixel.getStyleClass().add("primary-button");
        sliceButton.setDisable(true);
        importB.setOnAction(e -> importAssets());
        pixel.setOnAction(e -> new PixelArtEditor().show(app.owner(), a -> {
            String key = unique(a.key);
            app.project().getAssets().put(key, new Asset(key, key, a.data));
            app.changed();
            refreshAssets(key);
        }));
        sliceButton.setOnAction(e -> sliceSelected());

        HBox row1 = new HBox(6, importB, pixel);
        HBox row2 = new HBox(6, sliceButton);
        sliceButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(sliceButton, Priority.ALWAYS);
        box.getChildren().addAll(row1, row2);
        return box;
    }

    private Node center() {
        preview.setPreserveRatio(true);
        preview.setSmooth(false);
        preview.setCache(false);
        StackPane shell = new StackPane(preview);
        shell.getStyleClass().add("canvas-shell");
        shell.setPadding(new Insets(28));
        ScrollPane scroll = new ScrollPane(shell);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setPannable(true);
        scroll.getStyleClass().add("editor-scroll");
        assets.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            showPreview(b);
            sliceButton.setDisable(b == null || b.image() == null);
        });
        return scroll;
    }

    private void showPreview(Asset asset) {
        Image image = asset == null ? null : app.image(asset.key);
        preview.setImage(image);
        if (image == null) {
            preview.setFitWidth(0);
            preview.setFitHeight(0);
            return;
        }
        double width = Math.max(1, image.getWidth());
        double height = Math.max(1, image.getHeight());
        double integerScale = Math.floor(Math.min(PREVIEW_LIMIT / width, PREVIEW_LIMIT / height));
        if (integerScale < 1) integerScale = 1;
        preview.setFitWidth(width * integerScale);
        preview.setFitHeight(height * integerScale);
    }

    private Node right() {
        VBox box = new VBox(8, title("TILES"), tiles, tileInspector);
        box.getStyleClass().add("side-panel");
        box.setPadding(new Insets(12));
        box.setPrefWidth(390);
        VBox.setVgrow(tiles, Priority.ALWAYS);
        tiles.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> rebuildInspector());
        Button add = new Button("＋ Tile");
        add.setOnAction(e -> {
            int id = app.project().nextTileId();
            TileDef t = new TileDef(id, "Tile " + id, new java.awt.Color(80, 88, 104), true, "");
            app.project().getTiles().put(id, t);
            app.changed();
            refresh();
            tiles.getSelectionModel().select(t);
        });
        box.getChildren().add(add);
        return box;
    }

    private void refresh() {
        Asset selectedAsset = assets.getSelectionModel().getSelectedItem();
        TileDef selectedTile = tiles.getSelectionModel().getSelectedItem();
        assets.setItems(FXCollections.observableArrayList(app.project().getAssets().values()));
        tiles.setItems(FXCollections.observableArrayList(app.project().getTiles().values()));
        if (selectedAsset != null) assets.getSelectionModel().select(app.project().getAssets().get(selectedAsset.key));
        else if (!assets.getItems().isEmpty()) assets.getSelectionModel().selectFirst();
        if (selectedTile != null) tiles.getSelectionModel().select(app.project().getTiles().get(selectedTile.id));
        else if (!tiles.getItems().isEmpty()) tiles.getSelectionModel().selectFirst();
        sliceButton.setDisable(assets.getSelectionModel().getSelectedItem() == null);
        rebuildInspector();
    }

    private void refreshAssets(String selectKey) {
        assets.setItems(FXCollections.observableArrayList(app.project().getAssets().values()));
        Asset asset = app.project().getAssets().get(selectKey);
        if (asset != null) assets.getSelectionModel().select(asset);
        showPreview(asset);
        sliceButton.setDisable(asset == null);
        rebuildInspector();
    }

    private void rebuildInspector() {
        tileInspector.getChildren().clear();
        TileDef t = tiles.getSelectionModel().getSelectedItem();
        if (t == null) return;
        TextField name = new TextField(t.name);
        CheckBox walk = new CheckBox("Transitable");
        walk.setSelected(t.walkable);
        ColorPicker color = new ColorPicker(Color.rgb(t.color.getRed(), t.color.getGreen(), t.color.getBlue(), t.color.getAlpha() / 255.0));
        ComboBox<String> sprite = new ComboBox<>();
        sprite.getItems().add("(sin imagen)");
        sprite.getItems().addAll(app.project().getAssets().keySet());
        sprite.setValue(t.assetKey.isBlank() ? "(sin imagen)" : t.assetKey);
        Runnable apply = () -> {
            t.name = name.getText().isBlank() ? "Tile " + t.id : name.getText();
            t.walkable = walk.isSelected();
            Color c = color.getValue();
            t.color = new java.awt.Color((float) c.getRed(), (float) c.getGreen(), (float) c.getBlue(), (float) c.getOpacity());
            t.assetKey = "(sin imagen)".equals(sprite.getValue()) ? "" : sprite.getValue();
            app.changed();
            tiles.refresh();
        };
        name.setOnAction(e -> apply.run());
        name.focusedProperty().addListener((o, a, b) -> { if (!b) apply.run(); });
        walk.setOnAction(e -> apply.run());
        color.setOnAction(e -> apply.run());
        sprite.setOnAction(e -> apply.run());
        GridPane form = form();
        row(form, 0, "Nombre", name);
        row(form, 1, "Imagen", sprite);
        form.add(walk, 1, 2);
        form.add(color, 1, 3);
        Label hint = new Label("Los tiles se dibujan exactamente dentro de una celda de la cuadrícula. El zoom de Escena usa múltiplos enteros del tileSize del proyecto.");
        hint.getStyleClass().add("muted"); hint.setWrapText(true);
        tileInspector.getChildren().addAll(form, hint);
    }

    private void importAssets() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar gráficos");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        List<java.io.File> files = fc.showOpenMultipleDialog(app.owner());
        if (files == null) return;
        String lastKey = null;
        for (java.io.File f : files) {
            try {
                String key = unique(f.getName());
                app.project().getAssets().put(key, new Asset(key, f.getName(), Files.readAllBytes(f.toPath())));
                lastKey = key;
            } catch (Exception ex) {
                app.error("No se pudo importar " + f.getName(), ex.getMessage());
            }
        }
        if (lastKey != null) {
            app.changed();
            refreshAssets(lastKey);
            app.status("Imagen importada. Si es una hoja, selecciónala y usa 'Cortar spritesheet'.");
        }
    }

    private void sliceSelected() {
        Asset source = assets.getSelectionModel().getSelectedItem();
        if (source == null) return;
        Image image = app.image(source.key);
        if (image == null || source.image() == null) {
            app.error("No se puede cortar", "El asset seleccionado no es una imagen válida.");
            return;
        }
        SpritesheetSliceDialog.show(app.owner(), source, image, slices -> {
            if (slices == null || slices.isEmpty()) return;
            List<String> added = new ArrayList<>();
            for (Asset slice : slices) {
                String key = unique(slice.key);
                app.project().getAssets().put(key, new Asset(key, key, slice.data));
                added.add(key);
            }
            app.changed();
            refreshAssets(added.getFirst());
            app.status("Spritesheet cortada: " + added.size() + " sprites añadidos a Assets.");
        });
    }

    private String unique(String base) {
        String clean = base.replaceAll("[^A-Za-z0-9._-]", "_");
        String key = clean;
        int n = 2;
        while (app.project().getAssets().containsKey(key)) key = n++ + "_" + clean;
        return key;
    }

    private static Label title(String text) { Label l = new Label(text); l.getStyleClass().add("panel-title"); return l; }
    private static GridPane form() { GridPane g = new GridPane(); g.setHgap(8); g.setVgap(7); ColumnConstraints a = new ColumnConstraints(); a.setMinWidth(75); ColumnConstraints b = new ColumnConstraints(); b.setHgrow(Priority.ALWAYS); g.getColumnConstraints().addAll(a, b); return g; }
    private static void row(GridPane g, int r, String label, Node n) { g.add(new Label(label), 0, r); g.add(n, 1, r); if (n instanceof Region region) region.setMaxWidth(Double.MAX_VALUE); }
}
