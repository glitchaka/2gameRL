package com.buttclapdev.main.graphics;

import javax.swing.*;
import java.awt.*;

public class Windows extends JFrame{
    private static final long serialVersionUID = -6367572074013267907L;

    private String title;

    public Windows(final String title, final DrawSurface drawSurface){
        this.title = title;

        configWindows(drawSurface);
    }

    private void configWindows(final DrawSurface drawSurface) {
        setTitle(title);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        //setIconImage();
        setLayout(new BorderLayout());
        add(drawSurface, BorderLayout.CENTER);
        //setUndecorated(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }
}
