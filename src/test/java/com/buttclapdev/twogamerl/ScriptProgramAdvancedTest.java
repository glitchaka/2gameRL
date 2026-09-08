package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.script.ScriptProgram;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScriptProgramAdvancedTest {
    @Test
    void blockConditionRepeatGlobalsInterpolationAndEveryExecute() {
        String source="""
                on start
                  setVar score 3
                  repeat 2
                    addVar score 2
                  end
                  ifVar score >= 7
                    setGlobal gate open
                    log "score=${score}"
                  else
                    setGlobal gate closed
                  end
                  every 1 3 addGlobal ticks 1
                end
                """;
        ScriptProgram p=ScriptProgram.compile(source);assertTrue(p.validation().valid(),p.validation().errors().toString());
        Fake c=new Fake();p.fire(ScriptProgram.Event.START,c);
        assertEquals("7",c.vars.get("score"));assertEquals("open",c.globals.get("gate"));assertEquals(List.of("score=7"),c.logs);
        c.advance(3);assertEquals("3",c.globals.get("ticks"));
    }

    @Test
    void entityAndComponentCommandsCompileWithOtherReference() {
        ScriptProgram p=ScriptProgram.compile("""
                on collision
                  ifOther
                    addComponent other Health
                    setComponent other Health max 50
                    setEntity other physicsLayer Enemy
                    damage other 10
                  end
                end
                on destroy
                  create fx
                end
                """);
        assertTrue(p.validation().valid(),p.validation().errors().toString());
    }

    @Test
    void malformedNestedBlocksAreRejected() {
        assertFalse(ScriptProgram.compile("on start\n ifVar a == 1\n log x\nend\n").validation().valid(),"falta el end del evento");
        assertFalse(ScriptProgram.compile("on start\n else\nend\n").validation().valid());
        assertFalse(ScriptProgram.compile("on start\n every 1 0 log x\nend\n").validation().valid());
    }

    private record Scheduled(double due,long order,Runnable action){}
    private static final class Fake implements ScriptProgram.Context {
        final Map<String,String>vars=new HashMap<>(),globals=new HashMap<>();final List<String>logs=new ArrayList<>();
        final PriorityQueue<Scheduled>scheduled=new PriorityQueue<>(Comparator.comparingDouble(Scheduled::due).thenComparingLong(Scheduled::order));double now;long order;
        public boolean keyDown(String key){return false;}public boolean keyPressed(String key){return false;}public void move(double dx,double dy){}public void setVelocity(double vx,double vy){}public void teleport(double x,double y){}public void bounce(){}public void destroy(){}public void loadScene(String id){}public void setSprite(String assetKey){}public void createEntity(String template,Double x,Double y){}public void schedule(double seconds,Runnable action){scheduled.add(new Scheduled(now+seconds,++order,action));}public void log(String message){logs.add(message);}public void setVariable(String name,String value){vars.put(name,value);}public String variable(String name){return vars.getOrDefault(name,"0");}public void setProperty(String name,String value){}public String globalVariable(String name){return globals.getOrDefault(name,"0");}public void setGlobalVariable(String name,String value){globals.put(name,value);}
        void advance(double seconds){double target=now+seconds;while(!scheduled.isEmpty()&&scheduled.peek().due<=target+1e-9){Scheduled s=scheduled.poll();now=s.due();s.action().run();}now=target;}
    }
}
