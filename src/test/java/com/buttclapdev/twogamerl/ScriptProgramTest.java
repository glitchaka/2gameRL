package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.script.ScriptProgram;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ScriptProgramTest {
    @Test void scriptExecutesLifecycleCommandsAndInputConditions(){String source="""
on start
  log "hola mundo"
  setVar score 2
  addVar score 3
  teleport 4 5
end
on update
  ifKey W move 1 0
  ifPressed SPACE velocity 2 -3
end
on click
  setSprite hero.png
end
""";ScriptProgram p=ScriptProgram.compile(source);assertTrue(p.validation().valid(),p.validation().errors().toString());FakeContext c=new FakeContext();p.fire(ScriptProgram.Event.START,c);assertEquals(List.of("hola mundo"),c.logs);assertEquals("5",c.vars.get("score"));assertEquals(4,c.x);assertEquals(5,c.y);c.down.add("W");c.pressed.add("SPACE");p.fire(ScriptProgram.Event.UPDATE,c);assertEquals(5,c.x);assertEquals(2,c.vx);assertEquals(-3,c.vy);p.fire(ScriptProgram.Event.CLICK,c);assertEquals("hero.png",c.sprite);}

    @Test void dotAssignmentsSupportComponentsGlobalsSaveAndConditions(){String source="""
on start
  Rigidbody2D.gravityScale = 1
  Rigidbody2D.gravityScale *= -1
  global.score = 10
  global.score += 5
  save.nombre = "Adarvio"
  save.monedas = 2
  save.monedas += 3
  if save.monedas >= 5
    self.x = 7
  end
end
""";ScriptProgram p=ScriptProgram.compile(source);assertTrue(p.validation().valid(),p.validation().errors().toString());FakeContext c=new FakeContext();c.paths.put("Rigidbody2D.gravityScale","0");p.fire(ScriptProgram.Event.START,c);assertEquals("-1",c.paths.get("Rigidbody2D.gravityScale"));assertEquals("15",c.globals.get("score"));assertEquals("Adarvio",c.saves.get("nombre"));assertEquals("5",c.saves.get("monedas"));assertEquals("7",c.paths.get("self.x"));}

    @Test void namedEventsEmitAndScreenTextAreParsed(){ScriptProgram listener=ScriptProgram.compile("""
on event alarma
  showText bubble self "¡Alarma!" 4
  emit respuesta
end
""");assertTrue(listener.validation().valid(),listener.validation().errors().toString());FakeContext c=new FakeContext();listener.fireEvent("alarma",c);assertEquals(List.of("respuesta"),c.emitted);assertEquals(1,c.texts.size());assertEquals("bubble",c.texts.getFirst().type());assertEquals(4,c.texts.getFirst().duration());}

    @Test void waitContinuesSameEventLaterAndCreateStillRunsAfterDestroy(){ScriptProgram p=ScriptProgram.compile("on click\n  destroy\n  wait 9\n  create pj\n  log \"respawn terminado\"\nend\n");assertTrue(p.validation().valid());FakeContext c=new FakeContext();p.fire(ScriptProgram.Event.CLICK,c);assertTrue(c.destroyed);assertTrue(c.created.isEmpty());c.advance(8.99);assertTrue(c.created.isEmpty());c.advance(.01);assertEquals(new Created("pj",null,null),c.created.getFirst());assertEquals(List.of("respawn terminado"),c.logs);}
    @Test void timerSchedulesWithoutStoppingBlock(){ScriptProgram p=ScriptProgram.compile("on start\n  timer 2 create explosion 7 8\n  log \"continua ahora\"\nend\n");assertTrue(p.validation().valid());FakeContext c=new FakeContext();p.fire(ScriptProgram.Event.START,c);assertEquals(List.of("continua ahora"),c.logs);c.advance(2);assertEquals(List.of(new Created("explosion",7.0,8.0)),c.created);}
    @Test void invalidTimersAndCreateSyntaxAreRejected(){assertFalse(ScriptProgram.compile("on click\n wait -1\nend\n").validation().valid());assertFalse(ScriptProgram.compile("on click\n timer -1 destroy\nend\n").validation().valid());assertFalse(ScriptProgram.compile("on click\n create pj 1\nend\n").validation().valid());}
    @Test void defaultSceneScriptCompiles(){var scene=com.buttclapdev.twogamerl.model.GameProject.createDefault().getLevels().get("level-1");assertTrue(ScriptProgram.compile(scene.script).validation().valid());assertTrue(ScriptProgram.compile(scene.baseLayer().script).validation().valid());}

    private record Created(String template,Double x,Double y){}private record Scheduled(double due,long order,Runnable action){}
    private static final class FakeContext implements ScriptProgram.Context {
        final Set<String>down=new HashSet<>(),pressed=new HashSet<>();final Map<String,String>vars=new HashMap<>(),props=new HashMap<>(),paths=new HashMap<>(),globals=new HashMap<>(),saves=new HashMap<>();final List<String>logs=new ArrayList<>(),emitted=new ArrayList<>();final List<Created>created=new ArrayList<>();final List<ScriptProgram.TextRequest>texts=new ArrayList<>();final PriorityQueue<Scheduled>scheduled=new PriorityQueue<>(Comparator.comparingDouble(Scheduled::due).thenComparingLong(Scheduled::order));double x,y,vx,vy,now;long order;boolean bounced,destroyed;String scene,sprite;
        public boolean keyDown(String key){return down.contains(key);}public boolean keyPressed(String key){return pressed.contains(key);}public void move(double dx,double dy){x+=dx;y+=dy;}public void setVelocity(double vx,double vy){this.vx=vx;this.vy=vy;}public void teleport(double x,double y){this.x=x;this.y=y;}public void bounce(){bounced=true;}public void destroy(){destroyed=true;}public void loadScene(String id){scene=id;}public void setSprite(String assetKey){sprite=assetKey;}public void createEntity(String template,Double x,Double y){created.add(new Created(template,x,y));}public void schedule(double seconds,Runnable action){scheduled.add(new Scheduled(now+Math.max(0,seconds),++order,action));}public void log(String message){logs.add(message);}public void setVariable(String name,String value){vars.put(name,value);}public String variable(String name){return vars.getOrDefault(name,"0");}public void setProperty(String name,String value){props.put(name,value);}public String globalVariable(String name){return globals.getOrDefault(name,"0");}public void setGlobalVariable(String name,String value){globals.put(name,value);}public String saveVariable(String name){return saves.getOrDefault(name,"0");}public void setSaveVariable(String name,String value){saves.put(name,value);}public String readPath(String path){return paths.getOrDefault(path,"0");}public void writePath(String path,String value){paths.put(path,value);}public void emit(String event){emitted.add(event);}public void showText(ScriptProgram.TextRequest request){texts.add(request);}void advance(double seconds){now+=Math.max(0,seconds);while(!scheduled.isEmpty()&&scheduled.peek().due<=now+1e-9)scheduled.poll().action.run();}
    }
}
