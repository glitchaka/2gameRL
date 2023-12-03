package com.buttclapdev.main.statemachine.state;

import com.buttclapdev.main.statemachine.state.game.GameManager;

import java.awt.*;

public class StateManager {
    private GameState[] states;
    private GameState currentState;

    public StateManager() {
        startStates();
        startCurrentState();
        System.out.println("estoy en el construcctor del StateManager");
    }

    private void startStates() {
        states = new GameState[1];
        states[0] = new GameManager();
        System.out.println("estoy en el metodo startStates");

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
