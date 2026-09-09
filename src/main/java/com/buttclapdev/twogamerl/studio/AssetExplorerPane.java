package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;
import com.buttclapdev.twogamerl.model.GameProject.AssetCategory;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.shape.SVGPath;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Desktop-style project asset explorer. Assets remain project resources, but are
 * presented and manipulated as files in persistent virtual folders.
 */
final class AssetExplorerPane extends BorderPane {
    private static final DataFormat ASSET_KEYS = new DataFormat("application/x-2gamerl-asset-keys");
    private static final DataFormat FOLDER_PATH = new DataFormat("application/x-2gamerl-folder-path");
    private static final String EXPLICIT_ROOT = "~";
    private static final List<String> STANDARD_FOLDERS = List.of("Imágenes", "Spritesheets", "Sprites", "Fondos", "UI", "Tilesets", "VFX", "Retratos");

    private final StudioApp app;
    private final ListView<Asset> selectionBridge;
    private final TextField search;
    private final HBox breadcrumbs = new HBox(3);
    private final FlowPane grid = new FlowPane(10, 10);
    private final VBox rows = new VBox(2);
    private final ScrollPane scroll = new ScrollPane();
    private final Label status = new Label();
    private final ToggleButton gridMode = new ToggleButton("Cuadrícula");
    private final ToggleButton listMode = new ToggleButton("Lista");
    private final Button up = new Button("↑");
    private final Button newFolder = new Button("＋ Carpeta");
    private final Button renameFolder = new Button("Renombrar");
    private final Button deleteFolder = new Button("Eliminar carpeta");
    private final LinkedHashSet<String> selectedKeys = new LinkedHashSet<>();
    private String currentFolder = "";
    private String selectedFolder = "";

    AssetExplorerPane(StudioApp app, ListView<Asset> selectionBridge, TextField search) {
        this.app = app;
        this.selectionBridge = selectionBridge;
        this.search = search;
        getStyleClass().add("asset-explorer");
        setFocusTraversable(true);

        ToggleGroup views = new ToggleGroup();
        gridMode.setToggleGroup(views); listMode.setToggleGroup(views); gridMode.setSelected(true);
        gridMode.getStyleClass().add("compact-toggle"); listMode.getStyleClass().add("compact-toggle");
        gridMode.setOnAction(e -> { if (!gridMode.isSelected()) gridMode.setSelected(true); rebuild(); });
        listMode.setOnAction(e -> { if (!listMode.isSelected()) listMode.setSelected(true); rebuild(); });

        up.setTooltip(new Tooltip("Subir una carpeta"));
        up.setOnAction(e -> openFolder(parent(currentFolder)));
        newFolder.setOnAction(e -> createFolder());
        renameFolder.setOnAction(e -> renameSelectedFolder());
        deleteFolder.setOnAction(e -> deleteSelectedFolder());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox toolbar = new HBox(6, up, breadcrumbs, spacer, newFolder, renameFolder, deleteFolder, gridMode, listMode);
        toolbar.setAlignment(Pos.CENTER_LEFT); toolbar.getStyleClass().add("resource-toolbar");
        breadcrumbs.setAlignment(Pos.CENTER_LEFT); HBox.setHgrow(breadcrumbs, Priority.ALWAYS);

        grid.getStyleClass().add("resource-grid"); grid.setPadding(new Insets(10));
        rows.getStyleClass().add("resource-list"); rows.setPadding(new Insets(6));
        scroll.setFitToWidth(true); scroll.setPannable(true); scroll.getStyleClass().add("resource-scroll");
        scroll.setOnDragOver(this::acceptDropToCurrent); scroll.setOnDragDropped(this::dropToCurrent);

        status.getStyleClass().add("resource-status");
        BorderPane.setMargin(status, new Insets(5, 8, 7, 8));
        setTop(toolbar); setCenter(scroll); setBottom(status);

        selectionBridge.itemsProperty().addListener((o, a, b) -> rebuild());
        search.textProperty().addListener((o, a, b) -> rebuild());
        setOnKeyPressed(this::handleKey);
        rebuild();
    }

    void refresh() { rebuild(); }

    private void handleKey(KeyEvent e) {
        if (e.isControlDown() && e.getCode() == KeyCode.A) {
            selectedKeys.clear();
            visibleAssets().forEach(a -> selectedKeys.add(a.key));
            syncBridgeSelection(); rebuild(); e.consume();
        } else if (e.getCode() == KeyCode.BACK_SPACE && !currentFolder.isBlank()) {
            openFolder(parent(currentFolder)); e.consume();
        } else if (e.getCode() == KeyCode.F2 && !selectedFolder.isBlank()) {
            renameSelectedFolder(); e.consume();
        }
    }

    private void rebuild() {
        rebuildBreadcrumbs();
        grid.getChildren().clear(); rows.getChildren().clear();
        String q = search.getText() == null ? "" : search.getText().trim();
        boolean searching = !q.isBlank();
        List<Asset> assets = visibleAssets();
        List<String> folders = searching ? List.of() : childFolders(currentFolder);

        if (gridMode.isSelected()) {
            for (String folder : folders) grid.getChildren().add(folderTile(folder));
            for (Asset asset : assets) if (searching || effectiveFolder(asset).equals(currentFolder)) grid.getChildren().add(assetTile(asset));
            scroll.setContent(grid);
        } else {
            for (String folder : folders) rows.getChildren().add(folderRow(folder));
            for (Asset asset : assets) if (searching || effectiveFolder(asset).equals(currentFolder)) rows.getChildren().add(assetRow(asset));
            scroll.setContent(rows);
        }

        int shownAssets = (int) assets.stream().filter(a -> searching || effectiveFolder(a).equals(currentFolder)).count();
        status.setText(searching
                ? shownAssets + " resultado" + (shownAssets == 1 ? "" : "s") + " · búsqueda en todo el proyecto"
                : folders.size() + " carpeta" + (folders.size() == 1 ? "" : "s") + " · " + shownAssets + " archivo" + (shownAssets == 1 ? "" : "s"));
        up.setDisable(currentFolder.isBlank());
        boolean editableFolder = !selectedFolder.isBlank() && app.project().getAssetFolders().contains(selectedFolder);
        renameFolder.setDisable(!editableFolder); deleteFolder.setDisable(!editableFolder);
    }

    private List<Asset> visibleAssets() {
        ObservableList<Asset> items = selectionBridge.getItems();
        if (items == null) return List.of();
        return new ArrayList<>(items);
    }

    private void rebuildBreadcrumbs() {
        breadcrumbs.getChildren().clear();
        Button root = breadcrumb("Assets", ""); breadcrumbs.getChildren().add(root);
        if (currentFolder.isBlank()) return;
        String path = "";
        for (String part : currentFolder.split("/")) {
            if (part.isBlank()) continue;
            breadcrumbs.getChildren().add(new Label("›"));
            path = path.isBlank() ? part : path + "/" + part;
            breadcrumbs.getChildren().add(breadcrumb(part, path));
        }
    }

    private Button breadcrumb(String text, String path) {
        Button b = new Button(text); b.getStyleClass().add("breadcrumb-button");
        b.setOnAction(e -> openFolder(path));
        b.setOnDragOver(e -> acceptTarget(e, path));
        b.setOnDragDropped(e -> dropOnPath(e, path));
        return b;
    }

    private Node folderTile(String path) {
        VBox box = new VBox(7, folderIcon(38), new Label(name(path)));
        box.getStyleClass().addAll("resource-tile", "folder-tile");
        box.setAlignment(Pos.CENTER_LEFT); box.setPrefSize(128, 94);
        installFolderBehavior(box, path);
        return box;
    }

    private Node folderRow(String path) {
        Label name = new Label(name(path)); name.getStyleClass().add("resource-file-name");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label count = new Label(folderItemCount(path) + " elementos"); count.getStyleClass().add("muted");
        HBox row = new HBox(10, folderIcon(22), name, spacer, count);
        row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().addAll("resource-row", "folder-row");
        installFolderBehavior(row, path); return row;
    }

    private void installFolderBehavior(Node node, String path) {
        node.setOnMouseClicked(e -> {
            requestFocus(); selectedFolder = path; selectedKeys.clear(); syncBridgeSelection();
            if (e.getClickCount() >= 2 && e.getButton() == MouseButton.PRIMARY) openFolder(path); else rebuild();
        });
        node.setOnDragOver(e -> acceptTarget(e, path));
        node.setOnDragEntered(e -> addClass(node, "drop-target"));
        node.setOnDragExited(e -> node.getStyleClass().remove("drop-target"));
        node.setOnDragDropped(e -> { node.getStyleClass().remove("drop-target"); dropOnPath(e, path); });
        if (app.project().getAssetFolders().contains(path)) {
            node.setOnDragDetected(e -> {
                Dragboard db = node.startDragAndDrop(TransferMode.MOVE); ClipboardContent c = new ClipboardContent(); c.put(FOLDER_PATH, path); db.setContent(c); e.consume();
            });
        }
        ContextMenu menu = new ContextMenu();
        MenuItem open = new MenuItem("Abrir"); open.setOnAction(e -> openFolder(path)); menu.getItems().add(open);
        if (app.project().getAssetFolders().contains(path)) {
            MenuItem rename = new MenuItem("Renombrar"); rename.setOnAction(e -> { selectedFolder = path; renameSelectedFolder(); });
            MenuItem delete = new MenuItem("Eliminar carpeta"); delete.setOnAction(e -> { selectedFolder = path; deleteSelectedFolder(); });
            menu.getItems().addAll(new SeparatorMenuItem(), rename, delete);
        }
        if (node instanceof Control c) c.setContextMenu(menu); else node.setOnContextMenuRequested(e -> menu.show(node, e.getScreenX(), e.getScreenY()));
    }

    private Node assetTile(Asset asset) {
        StackPane thumb = thumbnail(asset, 82, 58);
        Label name = new Label(displayName(asset)); name.setWrapText(true); name.setMaxWidth(112); name.getStyleClass().add("resource-file-name");
        Label type = new Label(typeName(asset)); type.getStyleClass().add("resource-file-type");
        VBox box = new VBox(6, thumb, name, type); box.setAlignment(Pos.TOP_LEFT); box.setPrefSize(128, 126);
        box.getStyleClass().add("resource-tile"); if (selectedKeys.contains(asset.key)) box.getStyleClass().add("selected");
        installAssetBehavior(box, asset); return box;
    }

    private Node assetRow(Asset asset) {
        StackPane thumb = thumbnail(asset, 44, 34);
        Label name = new Label(displayName(asset)); name.getStyleClass().add("resource-file-name");
        Label type = new Label(typeName(asset)); type.getStyleClass().add("muted");
        VBox text = new VBox(1, name, type); Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Label path = new Label(effectiveFolder(asset).isBlank() ? "Assets" : effectiveFolder(asset)); path.getStyleClass().add("resource-path");
        HBox row = new HBox(10, thumb, text, spacer, path); row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("resource-row");
        if (selectedKeys.contains(asset.key)) row.getStyleClass().add("selected");
        installAssetBehavior(row, asset); return row;
    }

    private void installAssetBehavior(Node node, Asset asset) {
        node.setOnMouseClicked(e -> {
            requestFocus();
            if (e.isControlDown() || e.isShiftDown()) {
                if (!selectedKeys.add(asset.key)) selectedKeys.remove(asset.key);
            } else { selectedKeys.clear(); selectedKeys.add(asset.key); }
            selectedFolder = ""; syncBridgeSelection(); rebuild(); e.consume();
        });
        node.setOnDragDetected(e -> {
            if (!selectedKeys.contains(asset.key)) { selectedKeys.clear(); selectedKeys.add(asset.key); syncBridgeSelection(); }
            Dragboard db = node.startDragAndDrop(TransferMode.MOVE); ClipboardContent c = new ClipboardContent();
            c.put(ASSET_KEYS, String.join("\n", selectedKeys)); db.setContent(c); e.consume();
        });
        ContextMenu menu = new ContextMenu();
        MenuItem favorite = new MenuItem(asset.favorite ? "Quitar de favoritos" : "Marcar favorito");
        favorite.setOnAction(e -> { asset.favorite = !asset.favorite; app.changed(); rebuild(); });
        Menu move = new Menu("Mover a");
        MenuItem root = new MenuItem("Assets (raíz)"); root.setOnAction(e -> moveAssets(Set.of(asset.key), "")); move.getItems().add(root);
        for (String folder : allFolders()) { MenuItem target = new MenuItem(folder); target.setOnAction(e -> moveAssets(Set.of(asset.key), folder)); move.getItems().add(target); }
        menu.getItems().addAll(favorite, move);
        if (node instanceof Control c) c.setContextMenu(menu); else node.setOnContextMenuRequested(e -> menu.show(node, e.getScreenX(), e.getScreenY()));
    }

    private StackPane thumbnail(Asset asset, double w, double h) {
        StackPane pane = new StackPane(); pane.setPrefSize(w, h); pane.setMinSize(w, h); pane.setMaxSize(w, h); pane.getStyleClass().add("asset-thumbnail");
        Image image = app.image(asset.key);
        if (image != null) {
            ImageView iv = new ImageView(image); iv.setPreserveRatio(true); iv.setSmooth(asset.filterMode != null && asset.filterMode.name().equals("LINEAR")); iv.setFitWidth(w - 8); iv.setFitHeight(h - 8); pane.getChildren().add(iv);
        } else {
            Label fallback = new Label(asset.sourceOnly ? "SHEET" : "IMG"); fallback.getStyleClass().add("asset-thumbnail-fallback"); pane.getChildren().add(fallback);
        }
        if (asset.favorite) { Label star = new Label("★"); star.getStyleClass().add("favorite-badge"); StackPane.setAlignment(star, Pos.TOP_RIGHT); StackPane.setMargin(star, new Insets(3)); pane.getChildren().add(star); }
        return pane;
    }

    private SVGPath folderIcon(double size) {
        SVGPath icon = new SVGPath(); icon.setContent("M2 5h7l2 2h11v12H2z"); icon.getStyleClass().add("resource-folder-icon"); icon.setScaleX(size / 24.0); icon.setScaleY(size / 24.0);
        StackPane holder = new StackPane(icon); holder.setMinSize(size, size * .78); holder.setPrefSize(size, size * .78); holder.setMaxSize(size, size * .78); return icon;
    }

    private void syncBridgeSelection() {
        MultipleSelectionModel<Asset> sm = selectionBridge.getSelectionModel(); sm.clearSelection();
        ObservableList<Asset> items = selectionBridge.getItems(); if (items == null) return;
        for (int i = 0; i < items.size(); i++) if (selectedKeys.contains(items.get(i).key)) sm.select(i);
    }

    private void openFolder(String path) { currentFolder = normalize(path); selectedFolder = ""; selectedKeys.clear(); syncBridgeSelection(); rebuild(); }

    private void createFolder() {
        TextInputDialog d = new TextInputDialog(); d.setTitle("Nueva carpeta"); d.setHeaderText(currentFolder.isBlank() ? "Crear carpeta en Assets" : "Crear carpeta dentro de " + currentFolder); d.setContentText("Nombre:");
        d.showAndWait().map(String::trim).filter(s -> !s.isBlank()).ifPresent(name -> {
            String clean = cleanName(name); if (clean.isBlank()) return; String path = join(currentFolder, clean);
            if (allFolders().contains(path)) { app.status("Ya existe la carpeta: " + path); return; }
            app.project().getAssetFolders().add(path); app.changed(); selectedFolder = path; rebuild();
        });
    }

    private void renameSelectedFolder() {
        String old = selectedFolder; if (old.isBlank() || !app.project().getAssetFolders().contains(old)) return;
        TextInputDialog d = new TextInputDialog(name(old)); d.setTitle("Renombrar carpeta"); d.setHeaderText(old); d.setContentText("Nuevo nombre:");
        d.showAndWait().map(String::trim).filter(s -> !s.isBlank()).ifPresent(value -> {
            String next = join(parent(old), cleanName(value)); if (next.equals(old) || allFolders().contains(next)) return;
            remapFolder(old, next); selectedFolder = next; currentFolder = remapPath(currentFolder, old, next); app.changed(); rebuild();
        });
    }

    private void deleteSelectedFolder() {
        String old = selectedFolder; if (old.isBlank() || !app.project().getAssetFolders().contains(old)) return;
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Los archivos y subcarpetas se moverán a " + (parent(old).isBlank() ? "Assets" : parent(old)) + ".", ButtonType.CANCEL, ButtonType.OK);
        a.setHeaderText("Eliminar carpeta “" + name(old) + "”");
        if (a.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        String target = parent(old);
        for (Asset asset : app.project().getAssets().values()) {
            String f = effectiveFolder(asset); if (f.equals(old) || f.startsWith(old + "/")) asset.folder = externalFolder(remapPath(f, old, target));
        }
        LinkedHashSet<String> next = new LinkedHashSet<>();
        for (String folder : app.project().getAssetFolders()) if (!folder.equals(old)) next.add(folder.startsWith(old + "/") ? remapPath(folder, old, target) : folder);
        app.project().getAssetFolders().clear(); app.project().getAssetFolders().addAll(next);
        if (currentFolder.equals(old) || currentFolder.startsWith(old + "/")) currentFolder = target;
        selectedFolder = ""; app.changed(); rebuild();
    }

    private void moveAssets(Collection<String> keys, String target) {
        String normalized = normalize(target); int moved = 0;
        for (String key : keys) { Asset a = app.project().getAssets().get(key); if (a == null) continue; a.folder = externalFolder(normalized); moved++; }
        if (moved > 0) { app.changed(); app.status("Movidos " + moved + " recurso" + (moved == 1 ? "" : "s") + " a " + (normalized.isBlank() ? "Assets" : normalized)); rebuild(); }
    }

    private void moveFolder(String source, String targetParent) {
        source = normalize(source); targetParent = normalize(targetParent);
        if (source.isBlank() || !app.project().getAssetFolders().contains(source) || targetParent.equals(source) || targetParent.startsWith(source + "/")) return;
        String target = join(targetParent, name(source)); if (allFolders().contains(target)) { app.status("Ya existe " + target); return; }
        remapFolder(source, target); currentFolder = remapPath(currentFolder, source, target); selectedFolder = target; app.changed(); rebuild();
    }

    private void remapFolder(String old, String next) {
        LinkedHashSet<String> folders = new LinkedHashSet<>();
        for (String f : app.project().getAssetFolders()) folders.add(f.equals(old) || f.startsWith(old + "/") ? remapPath(f, old, next) : f);
        app.project().getAssetFolders().clear(); app.project().getAssetFolders().addAll(folders);
        for (Asset asset : app.project().getAssets().values()) {
            String f = effectiveFolder(asset); if (f.equals(old) || f.startsWith(old + "/")) asset.folder = externalFolder(remapPath(f, old, next));
        }
    }

    private void acceptDropToCurrent(DragEvent e) { acceptTarget(e, currentFolder); }
    private void dropToCurrent(DragEvent e) { dropOnPath(e, currentFolder); }
    private void acceptTarget(DragEvent e, String path) {
        Dragboard db = e.getDragboard();
        if (db.hasContent(ASSET_KEYS) || db.hasContent(FOLDER_PATH)) { e.acceptTransferModes(TransferMode.MOVE); e.consume(); }
    }
    private void dropOnPath(DragEvent e, String path) {
        Dragboard db = e.getDragboard(); boolean ok = false;
        if (db.hasContent(ASSET_KEYS)) { String raw = Objects.toString(db.getContent(ASSET_KEYS), ""); moveAssets(Arrays.stream(raw.split("\\R")).filter(s -> !s.isBlank()).toList(), path); ok = true; }
        else if (db.hasContent(FOLDER_PATH)) { moveFolder(Objects.toString(db.getContent(FOLDER_PATH), ""), path); ok = true; }
        e.setDropCompleted(ok); e.consume();
    }

    private List<String> childFolders(String parent) {
        String base = normalize(parent); TreeSet<String> children = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String folder : allFolders()) {
            if (folder.equals(base)) continue; String p = parent(folder); if (p.equals(base)) children.add(folder);
        }
        return new ArrayList<>(children);
    }

    private Set<String> allFolders() {
        LinkedHashSet<String> folders = new LinkedHashSet<>(STANDARD_FOLDERS);
        folders.addAll(app.project().getAssetFolders().stream().map(AssetExplorerPane::normalize).filter(s -> !s.isBlank()).toList());
        for (Asset asset : app.project().getAssets().values()) {
            String path = effectiveFolder(asset); while (!path.isBlank()) { folders.add(path); path = parent(path); }
        }
        return folders;
    }

    private int folderItemCount(String path) {
        int assets = (int) app.project().getAssets().values().stream().filter(a -> effectiveFolder(a).equals(path)).count();
        int children = (int) allFolders().stream().filter(f -> parent(f).equals(path)).count(); return assets + children;
    }

    private String effectiveFolder(Asset a) {
        String folder = a.folder == null ? "" : a.folder.trim(); if (EXPLICIT_ROOT.equals(folder)) return ""; if (!folder.isBlank()) return normalize(folder); return categoryFolder(a);
    }
    private String externalFolder(String displayPath) { return normalize(displayPath).isBlank() ? EXPLICIT_ROOT : normalize(displayPath); }

    private static String categoryFolder(Asset a) {
        if (a.sourceOnly && !a.isRegion()) return "Spritesheets";
        if (a.isRegion()) return "Sprites";
        AssetCategory c = a.category == null ? AssetCategory.IMAGE : a.category;
        return switch (c) { case SPRITESHEET -> "Spritesheets"; case SPRITE -> "Sprites"; case BACKGROUND -> "Fondos"; case UI -> "UI"; case TILESET -> "Tilesets"; case VFX -> "VFX"; case PORTRAIT -> "Retratos"; default -> "Imágenes"; };
    }
    private static String typeName(Asset a) { return a.sourceOnly && !a.isRegion() ? "Spritesheet" : a.isRegion() ? "Región de sprite" : (a.category == null ? "Imagen" : a.category.name()); }
    private static String displayName(Asset a) { String n = a.sourceName == null || a.sourceName.isBlank() ? a.key : a.sourceName; return n; }
    private static String normalize(String path) { if (path == null) return ""; return Arrays.stream(path.replace('\\', '/').split("/+")) .map(String::trim).filter(s -> !s.isBlank() && !s.equals(".")).collect(Collectors.joining("/")); }
    private static String parent(String path) { String n = normalize(path); int i = n.lastIndexOf('/'); return i < 0 ? "" : n.substring(0, i); }
    private static String name(String path) { String n = normalize(path); int i = n.lastIndexOf('/'); return i < 0 ? n : n.substring(i + 1); }
    private static String join(String parent, String child) { String p = normalize(parent), c = cleanName(child); return p.isBlank() ? c : c.isBlank() ? p : p + "/" + c; }
    private static String cleanName(String name) { if (name == null) return ""; return name.replace('/', ' ').replace('\\', ' ').replace(':', ' ').replace('*', ' ').replace('?', ' ').replace('"', ' ').replace('<', ' ').replace('>', ' ').replace('|', ' ').trim().replaceAll("\\s+", " "); }
    private static String remapPath(String path, String oldRoot, String newRoot) { String p = normalize(path), old = normalize(oldRoot), next = normalize(newRoot); if (p.equals(old)) return next; if (p.startsWith(old + "/")) return next.isBlank() ? p.substring(old.length() + 1) : next + p.substring(old.length()); return p; }
    private static void addClass(Node n, String cls) { if (!n.getStyleClass().contains(cls)) n.getStyleClass().add(cls); }
}
