package com.buttclapdev.main;

import com.buttclapdev.main.graphics.DrawSurface;
import com.buttclapdev.main.graphics.Windows;
import com.buttclapdev.main.statemachine.state.GameState;
import com.buttclapdev.main.statemachine.state.StateManager;

public class MainManager {
    private boolean onWork = false;
    private String title;
    private int width;
    private int height;

    private DrawSurface drawSurface;
    private Windows windows;
    private StateManager stateManager;

    private MainManager(final String title, final int width, final int height){
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public static void main(String []args){
        MainManager mainManager = new MainManager("2gameRL", 640, 480);
        mainManager.startGame();
        mainManager.StartMainLoop();

    }

    private void startGame() {
        onWork = true;
        initializer();
    }

    private void initializer() {
        drawSurface = new DrawSurface(width, height);
        windows = new Windows(title, drawSurface);
        stateManager = new StateManager();

    }

    private void StartMainLoop() {
        int aps = 0;
        int fps = 0;
        final int NS_PER_SECOND = 1000000000;
        final int APS_TARGET = 60;
        final double NS_PER_UPDATE = NS_PER_SECOND / APS_TARGET;

        long referenceUpdate = System.nanoTime();
        long referenceCounter = System.nanoTime();

        double elapsedTIme;
        double delta = 0;

        System.out.println("EL thread 2 se ejecuta con exito");
        while(onWork){
            final long startLoop = System.nanoTime();

            elapsedTIme = startLoop - referenceUpdate;
            referenceUpdate = startLoop;
            delta += elapsedTIme / NS_PER_UPDATE;
            while(delta >= 1){
                update();
                aps++;
                delta--;
            }
            draw();
            fps++;

            if (System.nanoTime() - referenceCounter > NS_PER_SECOND){
                System.out.println("FPS: " + fps + " APS: " + aps);
                aps = 0;
                fps = 0;
                referenceCounter = System.nanoTime();
            }
        }
    }

    private void update() {
        drawSurface.getKeyboard().update();
        stateManager.update();
    }

    private void draw() {
        drawSurface.draw(stateManager);
    }
}

