package com.buttclapdev.main.statemachine.state;

import com.buttclapdev.main.statemachine.game.GameManager;

import java.awt.*;

public class StateManager {
    private GameState[] states;
    private GameState currentState;

    public StateManager() {
        startStates();
        startCurrentState();
    }

    private void startStates() {
        states = new GameState[1];
        states[0] = new GameManager();

        //agregar e iniciar los demas estados a medida que los creemos
    }

    private void startCurrentState() {
        currentState = states[0];
    }
    public void update(){
        currentState.update();
    }
    public void draw(Graphics g){
        currentState.draw(g);
    }
    public void changeCurrenteState(final int newState){
        currentState = states[newState];
    }
    public GameState getCurrentState(){
        return currentState;
    }
}
