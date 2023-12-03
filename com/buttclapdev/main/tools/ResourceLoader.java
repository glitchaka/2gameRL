package com.buttclapdev.main.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ResourceLoader {
    public static BufferedImage loadCompatibleImageOpaque(final String route){
        Image image = null;
        try {
            System.out.println("voy a cargar el sprite");
            image = ImageIO.read(/*ClassLoader.class.getResource*/new File(route));
        } catch (IOException e){
            e.printStackTrace();
        }
        GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();

        BufferedImage acceleratedImage = graphicsConfiguration.createCompatibleImage(image.getWidth(null),
                image.getHeight(null), Transparency.OPAQUE);
        
        Graphics g = acceleratedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return acceleratedImage;
    }
    public static BufferedImage loadCompatibleImageTranslucent(final String route){

        Image image = null;
        try {

            image = ImageIO.read(ClassLoader.class.getResource(route));
        } catch (IOException e){
            e.printStackTrace();
        }
        GraphicsConfiguration graphicsConfiguration = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration();

        BufferedImage acceleratedImage = graphicsConfiguration.createCompatibleImage(image.getWidth(null),
                image.getHeight(null), Transparency.TRANSLUCENT
        );

        Graphics g = acceleratedImage.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return acceleratedImage;
    }
}
