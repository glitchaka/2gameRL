package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.function.Consumer;

final class SpritesheetSliceDialog {
    private final Asset source;
    private final Consumer<List<Asset>> consumer;
    private final Image previewImage;
    private final BufferedImage sourceImage;
    private final Canvas canvas = new Canvas();
    private final Spinner<Integer> cellWidth;
    private final Spinner<Integer> cellHeight;
    private final Spinner<Integer> marginX;
    private final Spinner<Integer> marginY;
    private final Spinner<Integer> spacingX;
    private final Spinner<Integer> spacingY;
    private final Label info = new Label();
    private final Set<Integer> selectedCells = new LinkedHashSet<>();
    private int columns;
    private int rows;
    private double previewScale = 1;

    private SpritesheetSliceDialog(Asset source, Image previewImage, Consumer<List<Asset>> consumer) {
        this.source = source;
        this.previewImage = previewImage;
        this.consumer = consumer;
        this.sourceImage = source.image();
        int suggested = suggestCell(sourceImage);
        cellWidth = spinner(1, Math.max(1, sourceImage.getWidth()), suggested);
        cellHeight = spinner(1, Math.max(1, sourceImage.getHeight()), suggested);
        marginX = spinner(0, Math.max(0, sourceImage.getWidth() - 1), 0);
        marginY = spinner(0, Math.max(0, sourceImage.getHeight() - 1), 0);
        spacingX = spinner(0, Math.max(0, sourceImage.getWidth() - 1), 0);
        spacingY = spinner(0, Math.max(0, sourceImage.getHeight() - 1), 0);
    }

    static void show(Window owner, Asset source, Image previewImage, Consumer<List<Asset>> consumer) {
        if (source == null || source.image() == null || previewImage == null) return;
        new SpritesheetSliceDialog(source, previewImage, consumer).show(owner);
    }

    private void show(Window owner) {
        Stage stage = new Stage(StageStyle.UNDECORATED);
        if (owner != null) stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle("Cortar spritesheet · 2gameRL");
        stage.setMinWidth(900);
        stage.setMinHeight(650);

        BorderPane root = new BorderPane();
        root.getStyleClass().addAll("studio-root", "window-frame");
        root.setTop(titleBar(stage));

        GridPane form = new GridPane();
        form.setHgap(8); form.setVgap(8); form.setPadding(new Insets(12));
        addRow(form, 0, "Celda", new HBox(6, cellWidth, new Label("×"), cellHeight));
        addRow(form, 1, "Margen", new HBox(6, marginX, new Label("X"), marginY));
        addRow(form, 2, "Separación", new HBox(6, spacingX, new Label("X"), spacingY));
        Button preset16 = new Button("16×16"), preset32 = new Button("32×32"), preset64 = new Button("64×64");
        preset16.setOnAction(e -> setCell(16)); preset32.setOnAction(e -> setCell(32)); preset64.setOnAction(e -> setCell(64));
        form.add(new HBox(6, preset16, preset32, preset64), 1, 3);
        Label hint = new Label("Haz clic sobre una celda para seleccionarla. Ctrl no es necesario: cada clic alterna la selección.");
        hint.setWrapText(true); hint.getStyleClass().add("muted");
        VBox controls = new VBox(10, title("CORTE"), form, new Separator(), info, hint);
        controls.setPadding(new Insets(14)); controls.setPrefWidth(300); controls.getStyleClass().add("side-panel");

        StackPane canvasShell = new StackPane(canvas); canvasShell.getStyleClass().add("canvas-shell"); canvasShell.setPadding(new Insets(24));
        ScrollPane scroll = new ScrollPane(canvasShell); scroll.setPannable(true); scroll.setFitToWidth(true); scroll.setFitToHeight(true); scroll.getStyleClass().add("editor-scroll");
        root.setLeft(controls); root.setCenter(scroll);

        Button clear = new Button("Limpiar selección");
        clear.setOnAction(e -> { selectedCells.clear(); redraw(); });
        Button selected = new Button("Cortar seleccionadas");
        Button all = new Button("Cortar toda la hoja"); all.getStyleClass().add("primary-button");
        Button cancel = new Button("Cancelar"); cancel.setOnAction(e -> stage.close());
        selected.setOnAction(e -> cut(false, stage));
        all.setOnAction(e -> cut(true, stage));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox bottom = new HBox(8, clear, spacer, cancel, selected, all); bottom.setPadding(new Insets(10, 14, 14, 14)); bottom.setAlignment(Pos.CENTER_RIGHT);
        root.setBottom(bottom);

        ChangeListener<Integer> changed = (o,a,b) -> { selectedCells.clear(); rebuildGeometry(); };
        for (Spinner<Integer> spinner : List.of(cellWidth, cellHeight, marginX, marginY, spacingX, spacingY)) spinner.valueProperty().addListener(changed);

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY || columns <= 0 || rows <= 0) return;
            double imageX = e.getX() / previewScale;
            double imageY = e.getY() / previewScale;
            int cw = cellWidth.getValue(), ch = cellHeight.getValue(), sx = spacingX.getValue(), sy = spacingY.getValue();
            int mx = marginX.getValue(), my = marginY.getValue();
            int col = (int)Math.floor((imageX - mx) / Math.max(1, cw + sx));
            int row = (int)Math.floor((imageY - my) / Math.max(1, ch + sy));
            if (col < 0 || row < 0 || col >= columns || row >= rows) return;
            double localX = imageX - (mx + col * (cw + sx));
            double localY = imageY - (my + row * (ch + sy));
            if (localX < 0 || localY < 0 || localX >= cw || localY >= ch) return;
            int index = row * columns + col;
            if (!selectedCells.add(index)) selectedCells.remove(index);
            redraw();
        });

        Scene scene = new Scene(root, 1120, 760);
        var css = SpritesheetSliceDialog.class.getResource("/com/buttclapdev/twogamerl/studio.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        rebuildGeometry();
        if (owner != null) {
            stage.setX(owner.getX() + Math.max(20, (owner.getWidth() - 1120) / 2));
            stage.setY(owner.getY() + Math.max(20, (owner.getHeight() - 760) / 2));
        }
        stage.showAndWait();
    }

    private void rebuildGeometry() {
        int w = sourceImage.getWidth(), h = sourceImage.getHeight();
        int cw = cellWidth.getValue(), ch = cellHeight.getValue();
        int mx = marginX.getValue(), my = marginY.getValue();
        int sx = spacingX.getValue(), sy = spacingY.getValue();
        columns = countCells(w, mx, cw, sx);
        rows = countCells(h, my, ch, sy);
        double maxPreview = 720;
        previewScale = Math.max(1, Math.floor(Math.min(maxPreview / Math.max(1, w), maxPreview / Math.max(1, h))));
        canvas.setWidth(w * previewScale);
        canvas.setHeight(h * previewScale);
        info.setText(columns + " × " + rows + " = " + (columns * rows) + " celdas · imagen " + w + "×" + h + " px");
        redraw();
    }

    private void redraw() {
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);
        g.clearRect(0,0,canvas.getWidth(),canvas.getHeight());
        g.drawImage(previewImage, 0, 0, canvas.getWidth(), canvas.getHeight());
        int cw = cellWidth.getValue(), ch = cellHeight.getValue();
        int mx = marginX.getValue(), my = marginY.getValue();
        int sx = spacingX.getValue(), sy = spacingY.getValue();
        for (int row=0; row<rows; row++) for (int col=0; col<columns; col++) {
            double x = (mx + col * (cw + sx)) * previewScale;
            double y = (my + row * (ch + sy)) * previewScale;
            double w = cw * previewScale, h = ch * previewScale;
            int index = row * columns + col;
            if (selectedCells.contains(index)) {
                g.setFill(Color.rgb(72,184,255,.25)); g.fillRect(x,y,w,h);
                g.setStroke(Color.web("#7ed1ff")); g.setLineWidth(2); g.strokeRect(x+.5,y+.5,w-1,h-1);
            } else {
                g.setStroke(Color.rgb(255,255,255,.55)); g.setLineWidth(1); g.strokeRect(x+.5,y+.5,w-1,h-1);
            }
        }
    }

    private void cut(boolean all, Stage stage) {
        if (columns <= 0 || rows <= 0) return;
        if (!all && selectedCells.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "Selecciona al menos una celda, o usa 'Cortar toda la hoja'.", ButtonType.OK);
            alert.initOwner(stage); alert.setHeaderText("No hay celdas seleccionadas"); alert.showAndWait(); return;
        }
        List<Asset> result = new ArrayList<>();
        String base = source.key.replaceFirst("(?i)\\.[^.]+$", "");
        int cw = cellWidth.getValue(), ch = cellHeight.getValue();
        int mx = marginX.getValue(), my = marginY.getValue();
        int sx = spacingX.getValue(), sy = spacingY.getValue();
        try {
            for (int row=0; row<rows; row++) for (int col=0; col<columns; col++) {
                int index = row * columns + col;
                if (!all && !selectedCells.contains(index)) continue;
                int x = mx + col * (cw + sx), y = my + row * (ch + sy);
                BufferedImage sprite = sourceImage.getSubimage(x, y, cw, ch);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(sprite, "png", out);
                String key = String.format(Locale.ROOT, "%s_%02d_%02d.png", base, col, row);
                result.add(new Asset(key, key, out.toByteArray()));
            }
            consumer.accept(result);
            stage.close();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR, ex.getMessage(), ButtonType.OK);
            alert.initOwner(stage); alert.setHeaderText("No se pudo cortar la spritesheet"); alert.showAndWait();
        }
    }

    private void setCell(int size) {
        cellWidth.getValueFactory().setValue(Math.min(size, sourceImage.getWidth()));
        cellHeight.getValueFactory().setValue(Math.min(size, sourceImage.getHeight()));
    }

    private static int countCells(int total, int margin, int cell, int spacing) {
        if (cell <= 0 || total - margin < cell) return 0;
        return 1 + Math.max(0, (total - margin - cell) / Math.max(1, cell + spacing));
    }

    private static int suggestCell(BufferedImage image) {
        for (int candidate : new int[]{32,16,64,24,8}) {
            if (image.getWidth() >= candidate && image.getHeight() >= candidate && image.getWidth() % candidate == 0 && image.getHeight() % candidate == 0) return candidate;
        }
        return Math.max(1, Math.min(32, Math.min(image.getWidth(), image.getHeight())));
    }

    private static Spinner<Integer> spinner(int min, int max, int value) {
        Spinner<Integer> s = new Spinner<>(min, Math.max(min,max), Math.max(min,Math.min(max,value)));
        s.setEditable(true); s.setPrefWidth(86); return s;
    }
    private static void addRow(GridPane g, int row, String label, javafx.scene.Node value) { g.add(new Label(label),0,row); g.add(value,1,row); }
    private static Label title(String text) { Label l = new Label(text); l.getStyleClass().add("panel-title"); return l; }

    private static HBox titleBar(Stage stage) {
        Label mark = new Label("2G"); mark.getStyleClass().add("window-app-mark");
        Label title = new Label("Cortar spritesheet"); title.getStyleClass().add("window-title");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button close = new Button("×"); close.getStyleClass().addAll("window-control", "window-close"); close.setOnAction(e -> stage.close());
        HBox bar = new HBox(9, mark, title, spacer, close); bar.setAlignment(Pos.CENTER_LEFT); bar.getStyleClass().add("title-bar");
        final double[] drag = new double[2];
        bar.setOnMousePressed(e -> { if (e.getButton()!=MouseButton.PRIMARY || e.getTarget() instanceof Button) return; drag[0]=e.getSceneX(); drag[1]=e.getSceneY(); });
        bar.setOnMouseDragged(e -> { if (e.getButton()!=MouseButton.PRIMARY || e.getTarget() instanceof Button) return; stage.setX(e.getScreenX()-drag[0]); stage.setY(e.getScreenY()-drag[1]); });
        return bar;
    }
}
