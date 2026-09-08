package com.buttclapdev.twogamerl;

import com.buttclapdev.twogamerl.script.ScriptProgram;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ScriptProgramTest {
    @Test
    void scriptExecutesLifecycleCommandsAndInputConditions() {
        String source = """
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
                """;
        ScriptProgram program = ScriptProgram.compile(source);
        assertTrue(program.validation().valid(), program.validation().errors().toString());

        FakeContext context = new FakeContext();
        program.fire(ScriptProgram.Event.START, context);
        assertEquals(List.of("hola mundo"), context.logs);
        assertEquals("5", context.vars.get("score"));
        assertEquals(4, context.x);
        assertEquals(5, context.y);

        context.down.add("W");
        context.pressed.add("SPACE");
        program.fire(ScriptProgram.Event.UPDATE, context);
        assertEquals(5, context.x);
        assertEquals(5, context.y);
        assertEquals(2, context.vx);
        assertEquals(-3, context.vy);

        program.fire(ScriptProgram.Event.CLICK, context);
        assertEquals("hero.png", context.sprite);
    }

    @Test
    void invalidScriptIsRejectedWithUsefulErrors() {
        ScriptProgram program = ScriptProgram.compile("move 1 2\non start\n nope 1\n");
        assertFalse(program.validation().valid());
        assertTrue(program.validation().errors().size() >= 3, program.validation().errors().toString());
    }

    @Test
    void defaultPlayerScriptCompiles() {
        var player = com.buttclapdev.twogamerl.model.GameProject.createDefault().getLevels().get("level-1").entity("player");
        ScriptProgram program = ScriptProgram.compile(player.script);
        assertTrue(program.validation().valid(), program.validation().errors().toString());
    }

    private static final class FakeContext implements ScriptProgram.Context {
        final Set<String> down = new HashSet<>();
        final Set<String> pressed = new HashSet<>();
        final Map<String,String> vars = new HashMap<>();
        final Map<String,String> props = new HashMap<>();
        final List<String> logs = new ArrayList<>();
        double x, y, vx, vy;
        boolean bounced, destroyed;
        String scene, sprite;
        public boolean keyDown(String key) { return down.contains(key); }
        public boolean keyPressed(String key) { return pressed.contains(key); }
        public void move(double dx, double dy) { x += dx; y += dy; }
        public void setVelocity(double vx, double vy) { this.vx = vx; this.vy = vy; }
        public void teleport(double x, double y) { this.x = x; this.y = y; }
        public void bounce() { bounced = true; }
        public void destroy() { destroyed = true; }
        public void loadScene(String id) { scene = id; }
        public void setSprite(String assetKey) { sprite = assetKey; }
        public void log(String message) { logs.add(message); }
        public void setVariable(String name, String value) { vars.put(name, value); }
        public String variable(String name) { return vars.getOrDefault(name, "0"); }
        public void setProperty(String name, String value) { props.put(name, value); }
    }
}
