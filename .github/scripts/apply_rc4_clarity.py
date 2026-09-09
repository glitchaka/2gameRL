from pathlib import Path
import re


def read(path): return Path(path).read_text(encoding='utf-8')
def write(path,s): Path(path).write_text(s,encoding='utf-8',newline='\n')
def replace(path,old,new):
    s=read(path)
    if old not in s: raise SystemExit(f'missing token in {path}: {old[:120]}')
    write(path,s.replace(old,new))

# Graphics browser is now part of the workspace itself, not injected as a compatibility patch.
replace('src/main/java/com/buttclapdev/twogamerl/studio/GraphicsEditorPane.java',
    'GraphicsEditorPane(StudioApp app){this.app=app;audioPreview=new AudioManager(app.project(),app::status);getItems().addAll(left(),center(),right());setDividerPositions(.23,.64);',
    'GraphicsEditorPane(StudioApp app){this.app=app;audioPreview=new AudioManager(app.project(),app::status);getItems().addAll(left(),center(),right());setDividerPositions(.36,.70);')
replace('src/main/java/com/buttclapdev/twogamerl/studio/GraphicsEditorPane.java',
    'VBox imageBox=new VBox(8,search,assets);VBox.setVgrow(assets,Priority.ALWAYS);Button importImage=',
    'AssetExplorerPane assetExplorer=new AssetExplorerPane(app,assets,search);VBox imageBox=new VBox(8,search,assetExplorer);VBox.setVgrow(assetExplorer,Priority.ALWAYS);Button importImage=')

# Avoid the legacy enhancement replacing a browser that is already native to the Graphics pane.
p='src/main/java/com/buttclapdev/twogamerl/studio/GraphicsBrowserEnhancements.java'
s=read(p)
old='static void install(GraphicsEditorPane pane,StudioApp app){\n        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;'
new='static void install(GraphicsEditorPane pane,StudioApp app){\n        if(findFirst(pane,AssetExplorerPane.class)!=null)return;\n        if(Boolean.TRUE.equals(pane.getProperties().get(INSTALLED)))return;'
if old not in s: raise SystemExit('GraphicsBrowserEnhancements install token missing')
write(p,s.replace(old,new))

# Scene center: separate tools from options so labels stay readable at normal desktop widths.
p='src/main/java/com/buttclapdev/twogamerl/studio/SceneEditorPane.java'
s=read(p)
start=s.index('    private Node centerPane(){')
end=s.index('    private ListCell<PrefabDef>brushCell()',start)
method='''    private Node centerPane(){
        ToggleGroup group=new ToggleGroup();
        selectTool=toolButton("Selección",group,true,Tool.SELECT);drawTool=toolButton("Dibujar",group,false,Tool.DRAW);
        ToggleButton fill=toolButton("Rellenar",group,false,Tool.FILL),erase=toolButton("Borrar",group,false,Tool.ERASE),empty=toolButton("Objeto",group,false,Tool.EMPTY_ENTITY);
        HBox toolRow=new HBox(5,selectTool,drawTool,fill,erase,empty);toolRow.setAlignment(Pos.CENTER_LEFT);toolRow.setPadding(new Insets(6,8,3,8));toolRow.getStyleClass().addAll("context-toolbar","scene-tools-row");

        brushPicker.setPrefWidth(230);brushPicker.setMaxWidth(300);brushPicker.setCellFactory(v->brushCell());brushPicker.setButtonCell(brushCell());
        brushPicker.setOnAction(e->{if(loading)return;target=Target.BRUSH;selected=null;selectedEntities.clear();selectedLayer=null;loadScriptTarget();rebuildInspector();redraw();});
        zoomScale.setPrefWidth(76);zoomScale.setButtonCell(scaleCell());zoomScale.setCellFactory(v->scaleCell());zoomScale.valueProperty().addListener((o,a,b)->resizeCanvas());
        gridInfo.getStyleClass().add("muted");Region optionSpacer=new Region();HBox.setHgrow(optionSpacer,Priority.ALWAYS);
        HBox optionRow=new HBox(8,new Label("Pincel"),brushPicker,optionSpacer,new Label("Zoom"),zoomScale,gridInfo);optionRow.setAlignment(Pos.CENTER_LEFT);optionRow.setPadding(new Insets(3,8,6,8));optionRow.getStyleClass().addAll("scene-options-row","context-options");

        VBox commandArea=new VBox(toolRow,optionRow);commandArea.getStyleClass().add("scene-command-area");
        StackPane shell=new StackPane(canvas);shell.getStyleClass().add("canvas-shell");shell.setPadding(new Insets(28));
        ScrollPane scroll=new ScrollPane(shell);scroll.setPannable(true);scroll.getStyleClass().add("editor-scroll");
        VBox center=new VBox(commandArea,scroll);VBox.setVgrow(scroll,Priority.ALWAYS);installCanvasHandlers();return center;
    }
'''
s=s[:start]+method+s[end:]
write(p,s)

# Advanced scene tools join the first row; Stamp selector belongs to the options row.
p='src/main/java/com/buttclapdev/twogamerl/studio/SceneTools222.java'
s=read(p)
s=s.replace('HBox toolbar=findByStyle(pane,HBox.class,"context-toolbar");Canvas base=findFirst(pane,Canvas.class);if(toolbar==null||base==null)return;',
            'HBox toolbar=findByStyle(pane,HBox.class,"scene-tools-row"),options=findByStyle(pane,HBox.class,"scene-options-row");Canvas base=findFirst(pane,Canvas.class);if(toolbar==null||base==null)return;')
s=s.replace('int insert=Math.max(0,toolbar.getChildren().size()-4);toolbar.getChildren().addAll(insert,List.of(line,rect,pick,stamp,stampPicker));',
            'toolbar.getChildren().addAll(line,rect,pick,stamp);if(options!=null){int insert=Math.max(0,options.getChildren().size()-3);options.getChildren().addAll(insert,new Label("Patrón"),stampPicker);}else toolbar.getChildren().add(stampPicker);')
write(p,s)

# Keep icons as recognition aids, never as the only label for editing tools.
p='src/main/java/com/buttclapdev/twogamerl/studio/StudioControlPolish.java'
s=read(p)
s=s.replace('private static final Set<String> ICON_ONLY_TOOLS=Set.of("Selección","Dibujar","Rellenar","Borrar","Objeto");','private static final Set<String> SCENE_TOOLS=Set.of("Selección","Dibujar","Rellenar","Borrar","Objeto","Línea","Rectángulo","Cuentagotas","Stamp");')
s=s.replace('if(ICON_ONLY_TOOLS.contains(clean)){b.setText("");b.setTooltip(new Tooltip(clean));b.getStyleClass().add("scene-tool-button");}else{b.setText(humanizeVisibleConstants(clean));if(b.getTooltip()==null)b.setTooltip(new Tooltip(clean));}',
            'b.setText(humanizeVisibleConstants(clean));if(b.getTooltip()==null)b.setTooltip(new Tooltip(clean));if(SCENE_TOOLS.contains(clean))b.getStyleClass().add("scene-tool-button");')
write(p,s)

# Professional readable sizing instead of fixed 34px icon cells.
p='src/main/resources/com/buttclapdev/twogamerl/studio-rc4.css'
s=read(p)
s=s.replace(''' .scene-tool-button {\n    -fx-min-width: 34; -fx-pref-width: 34; -fx-max-width: 34;\n    -fx-min-height: 32; -fx-pref-height: 32;\n    -fx-padding: 5;\n    -fx-background-color: transparent;\n    -fx-border-color: transparent;\n}\n'''.lstrip(),''' .scene-tool-button {\n    -fx-min-height: 32; -fx-pref-height: 32;\n    -fx-padding: 5 9;\n    -fx-background-color: transparent;\n    -fx-border-color: transparent;\n    -fx-graphic-text-gap: 6;\n}\n'''.lstrip())
s += '''\n/* RC4 clarity pass -------------------------------------------------------- */\n.scene-command-area { -fx-background-color: rgba(0,0,0,.12); -fx-border-color: transparent transparent rgba(255,255,255,.07) transparent; }\n.scene-tools-row { -fx-spacing: 4; }\n.scene-options-row { -fx-background-color: rgba(255,255,255,.018); }\n.scene-tools-row .toggle-button { -fx-text-overrun: CLIP; -fx-min-width: -1; -fx-pref-width: -1; -fx-max-width: Infinity; }\n.scene-tools-row .scene-tool-button:selected { -fx-background-color: rgba(201,164,95,.17); -fx-border-color: rgba(201,164,95,.62); }\n.resource-details .table-row-cell { -fx-cell-size: 40px; }\n.resource-browser-split .split-pane-divider { -fx-padding: 0 0 0 1; }\n'''
write(p,s)
