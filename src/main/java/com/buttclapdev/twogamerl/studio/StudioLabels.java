package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.*;
import javafx.util.StringConverter;

import java.util.Map;

final class StudioLabels {
    private StudioLabels(){}

    static String label(Enum<?> value){
        if(value==null)return "";
        if(value instanceof ScreenMode v)return switch(v){case WINDOWED->"Ventana";case FULLSCREEN->"Pantalla completa";case FULLSCREEN_BORDERLESS->"Pantalla completa sin bordes";};
        if(value instanceof ScaleMode v)return switch(v){case PIXEL_PERFECT->"Píxel perfecto";case KEEP_ASPECT->"Mantener proporción";case EXPAND->"Expandir";case STRETCH->"Estirar";};
        if(value instanceof FilterMode v)return switch(v){case PIXEL->"Píxel / vecino más cercano";case LINEAR->"Suavizado lineal";};
        if(value instanceof BackgroundMode v)return switch(v){case COLOR->"Color";case STRETCH->"Estirar";case COVER->"Cubrir";case CONTAIN->"Contener";case TILE->"Mosaico";};
        if(value instanceof PlacementMode v)return switch(v){case AUTO->"Automática";case TILEMAP->"Tilemap";case ENTITY->"Entidad";};
        if(value instanceof SoundCategory v)return switch(v){case SFX->"Efecto de sonido";case MUSIC->"Música";};
        if(value instanceof ParticleRenderMode v)return switch(v){case PIXEL->"Píxel";case SPRITE->"Sprite";};
        if(value instanceof ParticleShape v)return switch(v){case SQUARE->"Cuadrado";case CIRCLE->"Círculo";case DIAMOND->"Rombo";};
        return value.name().replace('_',' ').toLowerCase(java.util.Locale.ROOT);
    }

    static <E extends Enum<E>> StringConverter<E> converter(){return new StringConverter<>(){
        @Override public String toString(E value){return label(value);}
        @Override public E fromString(String text){return null;}
    };}
}
