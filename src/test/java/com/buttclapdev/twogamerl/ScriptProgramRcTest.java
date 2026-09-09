package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.script.ScriptProgram;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScriptProgramRcTest {
    @Test void boundedWhileMutatesPathsAndStopsOnCondition(){
        ScriptProgram p=ScriptProgram.compile("""
                on start
                  self.x = 0
                  while self.x < 5
                    self.x += 1
                  end
                end
                """);
        assertTrue(p.validation().valid(),()->String.join(" | ",p.validation().errors()));Fake c=new Fake();p.fire(ScriptProgram.Event.START,c);assertEquals("5",c.paths.get("self.x"));assertTrue(c.logs.isEmpty());
    }

    @Test void whilePreservesWaitContinuation(){
        ScriptProgram p=ScriptProgram.compile("""
                on start
                  self.x = 0
                  while self.x < 3
                    self.x += 1
                    wait 0.01
                  end
                end
                """);assertTrue(p.validation().valid(),()->String.join(" | ",p.validation().errors()));Fake c=new Fake();p.fire(ScriptProgram.Event.START,c);assertEquals("3",c.paths.get("self.x"));assertEquals(3,c.scheduled);
    }

    @Test void runawayWhileIsCutOffWithDiagnostic(){
        ScriptProgram p=ScriptProgram.compile("""
                on start
                  self.x = 0
                  while self.x == 0
                    log spin
                  end
                end
                """);assertTrue(p.validation().valid(),()->String.join(" | ",p.validation().errors()));Fake c=new Fake();p.fire(ScriptProgram.Event.START,c);assertTrue(c.logs.stream().anyMatch(x->x.contains("while alcanzó el límite")));assertTrue(c.logs.size()<=10001);
    }

    private static final class Fake implements ScriptProgram.Context {
        final Map<String,String>paths=new HashMap<>();final List<String>logs=new ArrayList<>();int scheduled;
        @Override public String readPath(String path){return paths.getOrDefault(path,"0");}@Override public void writePath(String path,String value){paths.put(path,value);}@Override public void schedule(double seconds,Runnable action){scheduled++;action.run();}@Override public void log(String message){logs.add(message);}
        @Override public boolean keyDown(String key){return false;}@Override public boolean keyPressed(String key){return false;}@Override public void move(double dx,double dy){}@Override public void setVelocity(double vx,double vy){}@Override public void teleport(double x,double y){}@Override public void bounce(){}@Override public void destroy(){}@Override public void loadScene(String id){}@Override public void setSprite(String assetKey){}@Override public void createEntity(String template,Double x,Double y){}@Override public void setVariable(String name,String value){}@Override public String variable(String name){return"0";}@Override public void setProperty(String name,String value){}
    }
}
