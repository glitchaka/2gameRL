package com.buttclapdev.twogamerl.studio;

import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.shape.SVGPath;
import javafx.util.StringConverter;

import java.util.*;

/** Applies the desktop control language to dynamically-created Studio workspaces. */
final class StudioControlPolish {
    private static final String INSTALLED="2rl.control.polish.rc4";
    private static final Set<String> SCENE_TOOLS=Set.of("Selección","Dibujar","Rellenar","Borrar","Objeto","Línea","Rectángulo","Cuentagotas","Stamp");
    private static final Map<String,String> ICONS=Map.ofEntries(
            Map.entry("Nuevo","M12 4v16M4 12h16"),Map.entry("Abrir","M3 7h7l2 2h9v10H3z"),Map.entry("Guardar","M4 3h14l2 2v16H4zM7 3v6h10V3M7 14h10v7H7z"),
            Map.entry("Configuración","M12 8a4 4 0 1 0 0 8 4 4 0 0 0 0-8M12 2v3M12 19v3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M2 12h3M19 12h3M4.9 19.1 7 17M17 7l2.1-2.1"),
            Map.entry("Probar","M7 4l13 8-13 8z"),Map.entry("Exportar","M12 3v12M7 10l5 5 5-5M4 20h16"),
            Map.entry("Escena","M3 4h18v16H3zM7 8h4v4H7zM14 8h3v8h-3z"),Map.entry("Menús","M4 6h16M4 12h16M4 18h16"),Map.entry("Gráficos","M4 4h16v16H4zM7 16l4-5 3 3 3-4 3 6"),
            Map.entry("Selección","M5 3l12 9-6 1 3 7-3 1-3-7-4 4z"),Map.entry("Dibujar","M4 19l1-5L16 3l5 5L10 19z"),Map.entry("Rellenar","M5 5l9 9-6 6-5-5 9-9M15 16h6v5h-6z"),Map.entry("Borrar","M5 15 14 6l5 5-9 9H5z"),Map.entry("Objeto","M4 4h16v16H4z"),
            Map.entry("Eliminar","M6 7h12M9 7V4h6v3M8 9l1 11h6l1-11"),Map.entry("Carpeta","M3 6h7l2 2h9v11H3z"),Map.entry("Renombrar","M4 20l4-1 11-11-3-3L5 16z"),Map.entry("Subir","M12 20V6M7 11l5-5 5 5"),
            Map.entry("Capa","M4 6h16v12H4zM7 3h10M7 21h10"),Map.entry("Atrás","M18 6 6 12M6 12h13"),Map.entry("Delante","M6 6l12 6-12 6M6 12h13"),Map.entry("Propiedades","M4 5h16M4 12h16M4 19h16M8 3v4M15 10v4M11 17v4"),
            Map.entry("Script","M8 4 3 12 8 20M16 4l5 8-5 8"),Map.entry("Validar","M4 12l5 5L20 6"),Map.entry("Plantilla","M5 3h14v18H5zM8 8h8M8 12h8M8 16h5"),
            Map.entry("Importar","M12 3v12M7 8l5-5 5 5M4 20h16"),Map.entry("Región","M4 4h6M4 4v6M20 4h-6M20 4v6M4 20h6M4 20v-6M20 20h-6M20 20v-6"),
            Map.entry("Acción","M12 3v18M3 12h18"),Map.entry("Prefab","M4 4h7v7H4zM13 4h7v7h-7zM4 13h7v7H4zM13 13h7v7h-7z")
    );
    private StudioControlPolish(){}

    static void install(Node root){if(root==null)return;polish(root);if(root instanceof Parent parent&&!Boolean.TRUE.equals(parent.getProperties().get(INSTALLED+".watch"))){parent.getProperties().put(INSTALLED+".watch",true);parent.getChildrenUnmodifiable().addListener((ListChangeListener<Node>)change->{while(change.next())for(Node n:change.getAddedSubList())install(n);});for(Node n:parent.getChildrenUnmodifiable())install(n);}}

    private static void polish(Node node){
        if(node instanceof ComboBox<?> combo)polishEnumCombo(combo);
        if(node instanceof ButtonBase button)polishButton(button);
        else if(node instanceof Label label)polishLabel(label);
    }

    private static void polishLabel(Label label){String text=label.getText();if(text==null||text.isBlank())return;String human=humanizeVisibleConstants(text);if(!human.equals(text))label.setText(human);}

    @SuppressWarnings({"rawtypes","unchecked"})
    private static void polishEnumCombo(ComboBox<?> box){
        if(Boolean.TRUE.equals(box.getProperties().get(INSTALLED+".enum")))return;
        Runnable apply=()->{Object sample=box.getValue();if(sample==null&&!box.getItems().isEmpty())sample=box.getItems().getFirst();if(!(sample instanceof Enum<?>))return;ComboBox raw=(ComboBox)box;StringConverter converter=new StringConverter<Object>(){@Override public String toString(Object value){return value instanceof Enum<?> e?enumLabel(e):Objects.toString(value,"");}@Override public Object fromString(String s){return null;}};raw.setConverter(converter);raw.setButtonCell(enumCell());raw.setCellFactory(v->enumCell());};
        box.getProperties().put(INSTALLED+".enum",true);apply.run();box.getItems().addListener((ListChangeListener)change->apply.run());box.valueProperty().addListener((o,a,b)->apply.run());
    }

    @SuppressWarnings({"rawtypes","unchecked"})
    private static ListCell enumCell(){return new ListCell<>(){@Override protected void updateItem(Object item,boolean empty){super.updateItem(item,empty);setText(empty||item==null?"":item instanceof Enum<?> e?enumLabel(e):humanizeVisibleConstants(item.toString()));}};}

    static String enumLabel(Enum<?> value){
        if(value==null)return "";String n=value.name();return switch(n){
            case "WINDOWED"->"Ventana";case "FULLSCREEN"->"Pantalla completa";case "FULLSCREEN_BORDERLESS"->"Pantalla completa sin bordes";
            case "PIXEL_PERFECT"->"Píxel perfecto";case "KEEP_ASPECT"->"Mantener proporción";case "EXPAND"->"Expandir área visible";case "STRETCH"->"Estirar";
            case "PIXEL"->"Píxel (sin suavizado)";case "LINEAR"->"Suavizado lineal";case "AUTO"->"Automático";case "ENTITY"->"Entidad";case "TILEMAP"->"Tilemap";
            case "IMAGE"->"Imagen";case "SPRITESHEET"->"Hoja de sprites";case "SPRITE"->"Sprite";case "BACKGROUND"->"Fondo";case "UI"->"Interfaz";case "TILESET"->"Tileset";case "VFX"->"Efecto visual";case "PORTRAIT"->"Retrato";
            case "NONE"->"Ninguno";case "FOUR_WAY"->"4 direcciones";case "EIGHT_WAY"->"8 direcciones";case "SQUARE"->"Cuadrado";case "CIRCLE"->"Círculo";case "DIAMOND"->"Rombo";
            case "BOOL"->"Booleano";case "INT"->"Entero";case "FLOAT"->"Decimal";case "TRIGGER"->"Disparador";case "LOOP"->"Bucle";case "PING_PONG"->"Ida y vuelta";
            default->humanize(n);
        };}

    private static String humanize(String raw){String[] words=raw.toLowerCase(Locale.ROOT).split("_");StringBuilder out=new StringBuilder();for(String w:words){if(w.isBlank())continue;if(!out.isEmpty())out.append(' ');out.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));}return out.toString();}

    static String humanizeVisibleConstants(String text){return text
            .replace("FULLSCREEN_BORDERLESS","Pantalla completa sin bordes").replace("PIXEL_PERFECT","Píxel perfecto")
            .replace("KEEP_ASPECT","Mantener proporción").replace("AUTO→TILEMAP","Automático → Tilemap").replace("AUTO→ENTIDAD","Automático → Entidad")
            .replace("SPRITESHEET","Hoja de sprites");}

    private static void polishButton(ButtonBase b){
        if(Boolean.TRUE.equals(b.getProperties().get(INSTALLED)))return;String raw=b.getText()==null?"":b.getText().trim();if(raw.isBlank()||b.getStyleClass().contains("window-control"))return;
        String clean=raw.replace("＋","").replace("⚙","").replace("▶","").trim();String key=iconKey(clean);String path=ICONS.get(key);if(path==null){b.setText(humanizeVisibleConstants(clean));return;}
        SVGPath icon=new SVGPath();icon.setContent(path);icon.getStyleClass().add("studio-glyph");b.setGraphic(icon);b.getStyleClass().add("modern-command");
        b.setText(humanizeVisibleConstants(clean));if(b.getTooltip()==null)b.setTooltip(new Tooltip(clean));if(SCENE_TOOLS.contains(clean))b.getStyleClass().add("scene-tool-button");
        if(Set.of("Escena","Menús","Gráficos").contains(clean))b.getStyleClass().add("workspace-command");
        b.getProperties().put(INSTALLED,true);
    }

    private static String iconKey(String text){
        if(text.startsWith("Nueva carpeta"))return "Carpeta";if(text.startsWith("Subir"))return "Subir";if(text.startsWith("Renombrar"))return "Renombrar";if(text.startsWith("Eliminar"))return "Eliminar";
        if(text.startsWith("Capa")||text.endsWith("Capa"))return "Capa";if(text.startsWith("Enviar atrás"))return "Atrás";if(text.startsWith("Traer delante"))return "Delante";if(text.startsWith("Propiedades"))return "Propiedades";
        if(text.startsWith("Validar"))return "Validar";if(text.contains("plantilla"))return "Plantilla";if(text.startsWith("Importar"))return "Importar";if(text.contains("regiones"))return "Región";
        if(text.startsWith("Acción"))return "Acción";if(text.contains("Prefab"))return "Prefab";for(String key:ICONS.keySet())if(text.equals(key)||text.startsWith(key+" "))return key;return text;
    }
}
