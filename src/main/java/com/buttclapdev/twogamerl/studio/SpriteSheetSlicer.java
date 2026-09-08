package com.buttclapdev.twogamerl.studio;

import com.buttclapdev.twogamerl.model.GameProject.Asset;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;

final class SpriteSheetSlicer {
    private SpriteSheetSlicer() {}

    static int countCells(int total,int margin,int cell,int spacing){if(cell<=0||total-margin<cell)return 0;return 1+Math.max(0,(total-margin-cell)/Math.max(1,cell+spacing));}

    static List<Asset> slice(Asset source,int cellWidth,int cellHeight,int marginX,int marginY,int spacingX,int spacingY,Set<Integer> selected,boolean all)throws Exception{
        BufferedImage image=source==null?null:source.image();if(image==null)throw new IllegalArgumentException("La spritesheet no es una imagen válida.");
        if(cellWidth<=0||cellHeight<=0)throw new IllegalArgumentException("El tamaño de celda debe ser mayor que cero.");
        int columns=countCells(image.getWidth(),marginX,cellWidth,spacingX),rows=countCells(image.getHeight(),marginY,cellHeight,spacingY);
        if(columns<=0||rows<=0)throw new IllegalArgumentException("La configuración no produce celdas dentro de la imagen.");
        String base=source.key.replaceFirst("(?i)\\.[^.]+$","");List<Asset>result=new ArrayList<>();
        for(int row=0;row<rows;row++)for(int col=0;col<columns;col++){
            int index=row*columns+col;if(!all&&(selected==null||!selected.contains(index)))continue;
            int x=marginX+col*(cellWidth+spacingX),y=marginY+row*(cellHeight+spacingY);
            BufferedImage sprite=new BufferedImage(cellWidth,cellHeight,BufferedImage.TYPE_INT_ARGB);
            // Copia píxel por píxel: no hay resize, interpolación, antialias ni cambio de paleta.
            for(int py=0;py<cellHeight;py++)for(int px=0;px<cellWidth;px++)sprite.setRGB(px,py,image.getRGB(x+px,y+py));
            ByteArrayOutputStream out=new ByteArrayOutputStream();ImageIO.write(sprite,"png",out);
            String key=String.format(Locale.ROOT,"%s_%02d_%02d.png",base,col,row);result.add(new Asset(key,key,out.toByteArray()));
        }
        return result;
    }
}
