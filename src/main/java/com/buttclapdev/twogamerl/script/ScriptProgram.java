package com.buttclapdev.twogamerl.script;

import java.util.*;

public final class ScriptProgram {
    public enum Event { START, UPDATE, CLICK, DOUBLECLICK, COLLISION, TRIGGER, DESTROY }

    public interface Context {
        boolean keyDown(String key);
        boolean keyPressed(String key);
        void move(double dx, double dy);
        void setVelocity(double vx, double vy);
        void teleport(double x, double y);
        void bounce();
        void destroy();
        void loadScene(String id);
        void setSprite(String assetKey);
        void createEntity(String template, Double x, Double y);
        void schedule(double seconds, Runnable action);
        void log(String message);
        void setVariable(String name, String value);
        String variable(String name);
        void setProperty(String name, String value);

        default String property(String name) { return ""; }
        default String otherEntity() { return ""; }
        default boolean entityExists(String ref) { return false; }
        default String entityProperty(String ref, String name) { return ""; }
        default void setEntityProperty(String ref, String name, String value) {}
        default void moveEntity(String ref, double dx, double dy) {}
        default void teleportEntity(String ref, double x, double y) {}
        default void destroyEntity(String ref) {}
        default void setEntitySprite(String ref, String assetKey) {}
        default String entityVariable(String ref, String name) { return "0"; }
        default void setEntityVariable(String ref, String name, String value) {}
        default boolean componentExists(String ref, String type) { return false; }
        default String componentProperty(String ref, String type, String property) { return ""; }
        default void setComponentProperty(String ref, String type, String property, String value) {}
        default void addComponent(String ref, String type) {}
        default void removeComponent(String ref, String type) {}
        default String globalVariable(String name) { return "0"; }
        default void setGlobalVariable(String name, String value) {}
        default void damage(String ref, double amount) {}
        default void heal(String ref, double amount) {}
        default void showMenu(String id) {}
        default void restartScene() {}
        default double random() { return Math.random(); }
    }

    private interface Instruction { default void run(Context context) {} }
    private interface Condition { boolean test(Context context); }

    private record ActionInstruction(java.util.function.Consumer<Context> action) implements Instruction {
        @Override public void run(Context context) { action.accept(context); }
    }
    private record WaitInstruction(double seconds) implements Instruction {}
    private record TimerInstruction(double seconds, Instruction nested) implements Instruction {
        @Override public void run(Context context) { context.schedule(seconds, () -> executeSingle(nested, context)); }
    }
    private record EveryInstruction(double seconds, int count, Instruction nested) implements Instruction {
        @Override public void run(Context context) {
            int[] remaining = {count};
            Runnable[] task = new Runnable[1];
            task[0] = () -> {
                if (remaining[0] <= 0) return;
                executeSingle(nested, context);
                remaining[0]--;
                if (remaining[0] > 0) context.schedule(seconds, task[0]);
            };
            context.schedule(seconds, task[0]);
        }
    }
    private record IfInstruction(Condition condition, List<Instruction> yes, List<Instruction> no) implements Instruction {}
    private record RepeatInstruction(int count, List<Instruction> body) implements Instruction {}
    private enum StopInstruction implements Instruction { INSTANCE }

    public record Validation(boolean valid, List<String> errors) {}

    private final EnumMap<Event, List<Instruction>> instructions = new EnumMap<>(Event.class);
    private final List<String> errors = new ArrayList<>();

    private ScriptProgram() { for (Event e : Event.values()) instructions.put(e, new ArrayList<>()); }

    public static ScriptProgram compile(String source) {
        ScriptProgram program = new ScriptProgram();
        new Parser(program, source == null ? "" : source).parse();
        return program;
    }

    public Validation validation() { return new Validation(errors.isEmpty(), List.copyOf(errors)); }

    public void fire(Event event, Context context) {
        if (!errors.isEmpty() || context == null) return;
        executeQueue(new ArrayDeque<>(instructions.get(event)), context);
    }

    private static void executeSingle(Instruction instruction, Context context) {
        ArrayDeque<Instruction> queue = new ArrayDeque<>();
        queue.add(instruction);
        executeQueue(queue, context);
    }

    private static void executeQueue(ArrayDeque<Instruction> queue, Context context) {
        int guard = 0;
        while (!queue.isEmpty() && guard++ < 100000) {
            Instruction instruction = queue.removeFirst();
            if (instruction instanceof WaitInstruction wait) {
                ArrayDeque<Instruction> continuation = new ArrayDeque<>(queue);
                context.schedule(wait.seconds(), () -> executeQueue(continuation, context));
                return;
            }
            if (instruction instanceof IfInstruction conditional) {
                pushFront(queue, conditional.condition().test(context) ? conditional.yes() : conditional.no());
                continue;
            }
            if (instruction instanceof RepeatInstruction repeat) {
                for (int i = 0; i < repeat.count(); i++) pushFront(queue, repeat.body());
                continue;
            }
            if (instruction == StopInstruction.INSTANCE) return;
            try { instruction.run(context); }
            catch (RuntimeException ex) { context.log("Script: " + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage())); }
        }
        if (guard >= 100000) context.log("Script: se alcanzó el límite de instrucciones de un evento.");
    }

    private static void pushFront(ArrayDeque<Instruction> queue, List<Instruction> block) {
        ListIterator<Instruction> it = block.listIterator(block.size());
        while (it.hasPrevious()) queue.addFirst(it.previous());
    }

    private void error(int line, String message) { errors.add("Línea " + line + ": " + message); }

    private static final class Parser {
        private record Line(int number, String text) {}
        private record Block(List<Instruction> instructions, boolean hitElse, boolean closed) {}

        private final ScriptProgram program;
        private final List<Line> lines = new ArrayList<>();
        private int index;

        Parser(ScriptProgram program, String source) {
            this.program = program;
            String[] raw = source.replace("\r", "").split("\n", -1);
            for (int i = 0; i < raw.length; i++) {
                String text = stripComment(raw[i]).trim();
                if (!text.isEmpty()) lines.add(new Line(i + 1, text));
            }
        }

        void parse() {
            while (index < lines.size()) {
                Line line = lines.get(index);
                if (!line.text().toLowerCase(Locale.ROOT).startsWith("on ")) {
                    program.error(line.number(), "comando fuera de un bloque 'on ... end'");
                    index++;
                    continue;
                }
                String eventName = line.text().substring(3).trim().replace("_", "").toUpperCase(Locale.ROOT);
                Event event;
                try { event = Event.valueOf(eventName); }
                catch (IllegalArgumentException ex) {
                    program.error(line.number(), "evento desconocido: " + eventName);
                    index++;
                    skipUntilEnd();
                    continue;
                }
                index++;
                Block block = parseBlock(false);
                program.instructions.get(event).addAll(block.instructions());
                if (!block.closed()) program.error(line.number(), "falta 'end' para cerrar el evento " + eventName.toLowerCase(Locale.ROOT));
            }
        }

        private Block parseBlock(boolean allowElse) {
            List<Instruction> result = new ArrayList<>();
            while (index < lines.size()) {
                Line line = lines.get(index);
                String lower = line.text().toLowerCase(Locale.ROOT);
                if (lower.equals("end")) { index++; return new Block(result, false, true); }
                if (lower.equals("else")) {
                    if (allowElse) { index++; return new Block(result, true, true); }
                    program.error(line.number(), "'else' sin bloque condicional");
                    index++;
                    continue;
                }
                if (lower.startsWith("on ")) {
                    program.error(line.number(), "hay un nuevo evento antes de cerrar el bloque actual con 'end'");
                    return new Block(result, false, false);
                }

                List<String> parts = tokens(line.text());
                if (parts.isEmpty()) { index++; continue; }
                String op = parts.get(0).toLowerCase(Locale.ROOT);

                if (isBlockCondition(op, parts)) {
                    Condition condition = parseCondition(line.number(), parts);
                    index++;
                    Block yes = parseBlock(true);
                    List<Instruction> no = List.of();
                    boolean closed = yes.closed();
                    if (yes.hitElse()) {
                        Block otherwise = parseBlock(false);
                        no = otherwise.instructions();
                        closed = otherwise.closed();
                    }
                    if (!closed) program.error(line.number(), "falta 'end' para cerrar el condicional");
                    if (condition != null) result.add(new IfInstruction(condition, List.copyOf(yes.instructions()), List.copyOf(no)));
                    continue;
                }

                if (op.equals("repeat") && parts.size() == 2) {
                    int count = integer(parts.get(1), line.number(), 0, 10000, "repeat");
                    index++;
                    Block body = parseBlock(false);
                    if (!body.closed()) program.error(line.number(), "falta 'end' para cerrar repeat");
                    result.add(new RepeatInstruction(count, List.copyOf(body.instructions())));
                    continue;
                }

                Instruction instruction = parseInstruction(line.number(), line.text());
                if (instruction != null) result.add(instruction);
                index++;
            }
            return new Block(result, false, false);
        }

        private boolean isBlockCondition(String op, List<String> parts) {
            return switch (op) {
                case "ifvar", "ifglobal", "ifproperty", "ifentity", "ifcomponent", "chance", "ifother" -> true;
                case "ifkey", "ifpressed" -> parts.size() == 2;
                default -> false;
            };
        }

        private Condition parseCondition(int lineNo, List<String> parts) {
            String op = parts.get(0).toLowerCase(Locale.ROOT);
            try {
                return switch (op) {
                    case "ifkey", "ifpressed" -> {
                        requireExact(parts, 2, lineNo, op + " <tecla>");
                        String key = parts.get(1);
                        yield c -> op.equals("ifpressed") ? c.keyPressed(key) : c.keyDown(key);
                    }
                    case "ifvar" -> {
                        require(parts, 4, lineNo, "ifVar <nombre> <operador> <valor>");
                        String name = parts.get(1), operator = parts.get(2), value = join(parts, 3);
                        yield c -> compare(c.variable(name), interpolate(c, value), operator);
                    }
                    case "ifglobal" -> {
                        require(parts, 4, lineNo, "ifGlobal <nombre> <operador> <valor>");
                        String name = parts.get(1), operator = parts.get(2), value = join(parts, 3);
                        yield c -> compare(c.globalVariable(name), interpolate(c, value), operator);
                    }
                    case "ifproperty" -> {
                        require(parts, 4, lineNo, "ifProperty <propiedad> <operador> <valor>");
                        String name = parts.get(1), operator = parts.get(2), value = join(parts, 3);
                        yield c -> compare(c.property(name), interpolate(c, value), operator);
                    }
                    case "ifentity" -> {
                        requireExact(parts, 3, lineNo, "ifEntity <entidad> <exists|missing>");
                        String ref = parts.get(1), mode = parts.get(2).toLowerCase(Locale.ROOT);
                        if (!mode.equals("exists") && !mode.equals("missing")) { program.error(lineNo, "ifEntity acepta exists o missing"); throw new ParseFailure(); }
                        yield c -> c.entityExists(ref) == mode.equals("exists");
                    }
                    case "ifcomponent" -> {
                        if (parts.size() < 3 || parts.size() > 4) { program.error(lineNo, "uso: ifComponent <entidad> <tipo> [exists|missing|enabled|disabled]"); throw new ParseFailure(); }
                        String ref = parts.get(1), type = parts.get(2), mode = parts.size() == 4 ? parts.get(3).toLowerCase(Locale.ROOT) : "exists";
                        yield c -> switch (mode) {
                            case "exists" -> c.componentExists(ref, type);
                            case "missing" -> !c.componentExists(ref, type);
                            case "enabled" -> c.componentExists(ref, type) && Boolean.parseBoolean(c.componentProperty(ref, type, "enabled"));
                            case "disabled" -> c.componentExists(ref, type) && !Boolean.parseBoolean(c.componentProperty(ref, type, "enabled"));
                            default -> false;
                        };
                    }
                    case "chance" -> {
                        requireExact(parts, 2, lineNo, "chance <0..1>");
                        double probability = number(parts.get(1), lineNo);
                        if (probability < 0 || probability > 1) { program.error(lineNo, "chance debe estar entre 0 y 1"); throw new ParseFailure(); }
                        yield c -> c.random() < probability;
                    }
                    case "ifother" -> {
                        requireExact(parts, 1, lineNo, "ifOther");
                        yield c -> c.otherEntity() != null && !c.otherEntity().isBlank();
                    }
                    default -> null;
                };
            } catch (ParseFailure ignored) { return null; }
        }

        private Instruction parseInstruction(int lineNo, String line) {
            List<String> parts = tokens(line);
            if (parts.isEmpty()) return null;
            String op = parts.get(0).toLowerCase(Locale.ROOT);
            try {
                return switch (op) {
                    case "log", "print" -> {
                        require(parts, 2, lineNo, "log <texto>");
                        String message = join(parts, 1);
                        yield action(c -> c.log(interpolate(c, message)));
                    }
                    case "move" -> {
                        requireExact(parts, 3, lineNo, "move <x> <y>");
                        double x = number(parts.get(1), lineNo), y = number(parts.get(2), lineNo);
                        yield action(c -> c.move(x, y));
                    }
                    case "velocity" -> {
                        requireExact(parts, 3, lineNo, "velocity <x> <y>");
                        double x = number(parts.get(1), lineNo), y = number(parts.get(2), lineNo);
                        yield action(c -> c.setVelocity(x, y));
                    }
                    case "teleport" -> {
                        requireExact(parts, 3, lineNo, "teleport <x> <y>");
                        double x = number(parts.get(1), lineNo), y = number(parts.get(2), lineNo);
                        yield action(c -> c.teleport(x, y));
                    }
                    case "bounce" -> { requireExact(parts, 1, lineNo, "bounce"); yield action(Context::bounce); }
                    case "destroy" -> { requireExact(parts, 1, lineNo, "destroy"); yield action(Context::destroy); }
                    case "destroyentity" -> {
                        requireExact(parts, 2, lineNo, "destroyEntity <entidad>"); String ref = parts.get(1);
                        yield action(c -> c.destroyEntity(ref));
                    }
                    case "wait" -> {
                        requireExact(parts, 2, lineNo, "wait <segundos>");
                        yield new WaitInstruction(nonNegative(number(parts.get(1), lineNo), lineNo, "wait"));
                    }
                    case "timer" -> {
                        if (parts.size() < 3) { program.error(lineNo, "uso: timer <segundos> <comando>"); throw new ParseFailure(); }
                        double seconds = nonNegative(number(parts.get(1), lineNo), lineNo, "timer");
                        Instruction nested = parseInstruction(lineNo, join(parts, 2));
                        if (nested == null || nested instanceof WaitInstruction || nested instanceof EveryInstruction) { program.error(lineNo, "timer necesita un comando normal y no puede contener wait/every"); throw new ParseFailure(); }
                        yield new TimerInstruction(seconds, nested);
                    }
                    case "every" -> {
                        if (parts.size() < 4) { program.error(lineNo, "uso: every <segundos> <cantidad> <comando>"); throw new ParseFailure(); }
                        double seconds = nonNegative(number(parts.get(1), lineNo), lineNo, "every");
                        int count = integer(parts.get(2), lineNo, 1, 10000, "every");
                        Instruction nested = parseInstruction(lineNo, join(parts, 3));
                        if (nested == null || nested instanceof WaitInstruction || nested instanceof EveryInstruction) { program.error(lineNo, "every necesita un comando normal y no puede contener wait/every"); throw new ParseFailure(); }
                        yield new EveryInstruction(seconds, count, nested);
                    }
                    case "create", "spawn" -> {
                        if (parts.size() != 2 && parts.size() != 4) { program.error(lineNo, "uso: create <entidad> [x y]"); throw new ParseFailure(); }
                        String template = parts.get(1); Double x = null, y = null;
                        if (parts.size() == 4) { x = number(parts.get(2), lineNo); y = number(parts.get(3), lineNo); }
                        Double px = x, py = y;
                        yield action(c -> c.createEntity(template, px, py));
                    }
                    case "loadscene" -> {
                        requireExact(parts, 2, lineNo, "loadScene <id>"); String id = parts.get(1);
                        yield action(c -> c.loadScene(id));
                    }
                    case "showmenu" -> {
                        requireExact(parts, 2, lineNo, "showMenu <id>"); String id = parts.get(1);
                        yield action(c -> c.showMenu(id));
                    }
                    case "restartscene" -> { requireExact(parts, 1, lineNo, "restartScene"); yield action(Context::restartScene); }
                    case "setsprite" -> {
                        requireExact(parts, 2, lineNo, "setSprite <asset>"); String id = parts.get(1);
                        yield action(c -> c.setSprite(interpolate(c, id)));
                    }
                    case "setentitysprite" -> {
                        requireExact(parts, 3, lineNo, "setEntitySprite <entidad> <asset>"); String ref = parts.get(1), asset = parts.get(2);
                        yield action(c -> c.setEntitySprite(ref, interpolate(c, asset)));
                    }
                    case "setvar" -> {
                        require(parts, 3, lineNo, "setVar <nombre> <valor>"); String key = parts.get(1), value = join(parts, 2);
                        yield action(c -> c.setVariable(key, interpolate(c, value)));
                    }
                    case "addvar", "mulvar", "divvar" -> {
                        requireExact(parts, 3, lineNo, op + " <nombre> <número>"); String key = parts.get(1); double amount = number(parts.get(2), lineNo);
                        yield action(c -> {
                            double old = parseDouble(c.variable(key), 0);
                            double next = switch (op) { case "addvar" -> old + amount; case "mulvar" -> old * amount; default -> amount == 0 ? old : old / amount; };
                            c.setVariable(key, trimNumber(next));
                        });
                    }
                    case "randomvar" -> {
                        requireExact(parts, 4, lineNo, "randomVar <nombre> <min> <max>"); String key = parts.get(1); double min = number(parts.get(2), lineNo), max = number(parts.get(3), lineNo);
                        if (max < min) { program.error(lineNo, "randomVar requiere max >= min"); throw new ParseFailure(); }
                        yield action(c -> c.setVariable(key, trimNumber(min + c.random() * (max - min))));
                    }
                    case "setglobal" -> {
                        require(parts, 3, lineNo, "setGlobal <nombre> <valor>"); String key = parts.get(1), value = join(parts, 2);
                        yield action(c -> c.setGlobalVariable(key, interpolate(c, value)));
                    }
                    case "addglobal" -> {
                        requireExact(parts, 3, lineNo, "addGlobal <nombre> <número>"); String key = parts.get(1); double amount = number(parts.get(2), lineNo);
                        yield action(c -> c.setGlobalVariable(key, trimNumber(parseDouble(c.globalVariable(key), 0) + amount)));
                    }
                    case "set" -> {
                        require(parts, 3, lineNo, "set <propiedad> <valor>"); String key = parts.get(1), value = join(parts, 2);
                        yield action(c -> c.setProperty(key, interpolate(c, value)));
                    }
                    case "setentity" -> {
                        require(parts, 4, lineNo, "setEntity <entidad> <propiedad> <valor>"); String ref = parts.get(1), key = parts.get(2), value = join(parts, 3);
                        yield action(c -> c.setEntityProperty(ref, key, interpolate(c, value)));
                    }
                    case "moveentity" -> {
                        requireExact(parts, 4, lineNo, "moveEntity <entidad> <x> <y>"); String ref = parts.get(1); double x = number(parts.get(2), lineNo), y = number(parts.get(3), lineNo);
                        yield action(c -> c.moveEntity(ref, x, y));
                    }
                    case "teleportentity" -> {
                        requireExact(parts, 4, lineNo, "teleportEntity <entidad> <x> <y>"); String ref = parts.get(1); double x = number(parts.get(2), lineNo), y = number(parts.get(3), lineNo);
                        yield action(c -> c.teleportEntity(ref, x, y));
                    }
                    case "setentityvar" -> {
                        require(parts, 4, lineNo, "setEntityVar <entidad> <nombre> <valor>"); String ref = parts.get(1), key = parts.get(2), value = join(parts, 3);
                        yield action(c -> c.setEntityVariable(ref, key, interpolate(c, value)));
                    }
                    case "addentityvar" -> {
                        requireExact(parts, 4, lineNo, "addEntityVar <entidad> <nombre> <número>"); String ref = parts.get(1), key = parts.get(2); double amount = number(parts.get(3), lineNo);
                        yield action(c -> c.setEntityVariable(ref, key, trimNumber(parseDouble(c.entityVariable(ref, key), 0) + amount)));
                    }
                    case "addcomponent" -> {
                        requireExact(parts, 3, lineNo, "addComponent <entidad> <tipo>"); String ref = parts.get(1), type = parts.get(2);
                        yield action(c -> c.addComponent(ref, type));
                    }
                    case "removecomponent" -> {
                        requireExact(parts, 3, lineNo, "removeComponent <entidad> <tipo>"); String ref = parts.get(1), type = parts.get(2);
                        yield action(c -> c.removeComponent(ref, type));
                    }
                    case "setcomponent" -> {
                        require(parts, 5, lineNo, "setComponent <entidad> <tipo> <propiedad> <valor>"); String ref = parts.get(1), type = parts.get(2), key = parts.get(3), value = join(parts, 4);
                        yield action(c -> c.setComponentProperty(ref, type, key, interpolate(c, value)));
                    }
                    case "enablecomponent" -> {
                        requireExact(parts, 4, lineNo, "enableComponent <entidad> <tipo> <true|false>"); String ref = parts.get(1), type = parts.get(2), value = parts.get(3);
                        if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) { program.error(lineNo, "enableComponent requiere true o false"); throw new ParseFailure(); }
                        yield action(c -> c.setComponentProperty(ref, type, "enabled", value.toLowerCase(Locale.ROOT)));
                    }
                    case "damage", "heal" -> {
                        requireExact(parts, 3, lineNo, op + " <entidad> <cantidad>"); String ref = parts.get(1); double amount = nonNegative(number(parts.get(2), lineNo), lineNo, op);
                        yield action(c -> { if (op.equals("damage")) c.damage(ref, amount); else c.heal(ref, amount); });
                    }
                    case "ifkey", "ifpressed" -> parseInlineKeyConditional(lineNo, parts, op.equals("ifpressed"));
                    case "stop", "return" -> { requireExact(parts, 1, lineNo, op); yield StopInstruction.INSTANCE; }
                    default -> { program.error(lineNo, "comando desconocido: " + parts.get(0)); yield null; }
                };
            } catch (ParseFailure ignored) { return null; }
        }

        private Instruction parseInlineKeyConditional(int lineNo, List<String> parts, boolean pressed) {
            if (parts.size() < 3) { program.error(lineNo, (pressed ? "ifPressed" : "ifKey") + " <tecla> <comando>"); throw new ParseFailure(); }
            String key = parts.get(1);
            Instruction nested = parseInstruction(lineNo, join(parts, 2));
            if (nested == null || nested instanceof WaitInstruction) { program.error(lineNo, "wait debe ir en una línea propia"); throw new ParseFailure(); }
            return action(c -> { if (pressed ? c.keyPressed(key) : c.keyDown(key)) executeSingle(nested, c); });
        }

        private void skipUntilEnd() {
            while (index < lines.size()) if (lines.get(index++).text().equalsIgnoreCase("end")) return;
        }

        private static ActionInstruction action(java.util.function.Consumer<Context> action) { return new ActionInstruction(action); }

        private void require(List<String> parts, int count, int lineNo, String usage) { if (parts.size() < count) { program.error(lineNo, "uso: " + usage); throw new ParseFailure(); } }
        private void requireExact(List<String> parts, int count, int lineNo, String usage) { if (parts.size() != count) { program.error(lineNo, "uso: " + usage); throw new ParseFailure(); } }
        private double number(String value, int lineNo) { try { return Double.parseDouble(value); } catch (NumberFormatException e) { program.error(lineNo, "número inválido: " + value); throw new ParseFailure(); } }
        private int integer(String value, int lineNo, int min, int max, String op) { try { int n = Integer.parseInt(value); if (n < min || n > max) throw new NumberFormatException(); return n; } catch (NumberFormatException e) { program.error(lineNo, op + " requiere un entero entre " + min + " y " + max); throw new ParseFailure(); } }
        private double nonNegative(double value, int lineNo, String op) { if (value < 0) { program.error(lineNo, op + " no acepta valores negativos"); throw new ParseFailure(); } return value; }
    }

    private static boolean compare(String left, String right, String operator) {
        String op = operator.toLowerCase(Locale.ROOT);
        Double a = tryNumber(left), b = tryNumber(right);
        if (a != null && b != null) {
            int cmp = Double.compare(a, b);
            return switch (op) { case "==", "=" -> cmp == 0; case "!=" -> cmp != 0; case ">" -> cmp > 0; case ">=" -> cmp >= 0; case "<" -> cmp < 0; case "<=" -> cmp <= 0; default -> false; };
        }
        String l = left == null ? "" : left, r = right == null ? "" : right;
        int cmp = l.compareToIgnoreCase(r);
        return switch (op) {
            case "==", "=" -> l.equalsIgnoreCase(r);
            case "!=" -> !l.equalsIgnoreCase(r);
            case ">" -> cmp > 0; case ">=" -> cmp >= 0; case "<" -> cmp < 0; case "<=" -> cmp <= 0;
            case "contains" -> l.toLowerCase(Locale.ROOT).contains(r.toLowerCase(Locale.ROOT));
            case "startswith" -> l.toLowerCase(Locale.ROOT).startsWith(r.toLowerCase(Locale.ROOT));
            case "endswith" -> l.toLowerCase(Locale.ROOT).endsWith(r.toLowerCase(Locale.ROOT));
            default -> false;
        };
    }

    private static String interpolate(Context context, String text) {
        if (text == null || text.indexOf('$') < 0) return text == null ? "" : text;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < text.length();) {
            if (i + 2 < text.length() && text.charAt(i) == '$' && text.charAt(i + 1) == '{') {
                int end = text.indexOf('}', i + 2);
                if (end > i) {
                    String key = text.substring(i + 2, end);
                    String value;
                    if (key.startsWith("global:")) value = context.globalVariable(key.substring(7));
                    else if (key.startsWith("prop:")) value = context.property(key.substring(5));
                    else if (key.equals("other")) value = context.otherEntity();
                    else value = context.variable(key);
                    out.append(value == null ? "" : value);
                    i = end + 1;
                    continue;
                }
            }
            out.append(text.charAt(i++));
        }
        return out.toString();
    }

    private static List<String> tokens(String line) {
        ArrayList<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') { quoted = !quoted; continue; }
            if (Character.isWhitespace(ch) && !quoted) {
                if (!current.isEmpty()) { result.add(current.toString()); current.setLength(0); }
            } else current.append(ch);
        }
        if (!current.isEmpty()) result.add(current.toString());
        return result;
    }

    private static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '"') quoted = !quoted;
            else if (line.charAt(i) == '#' && !quoted) return line.substring(0, i);
        }
        return line;
    }

    private static String join(List<String> parts, int start) { return String.join(" ", parts.subList(start, parts.size())); }
    private static double parseDouble(String value, double fallback) { try { return Double.parseDouble(value); } catch (Exception e) { return fallback; } }
    private static Double tryNumber(String value) { try { return Double.parseDouble(value); } catch (Exception e) { return null; } }
    private static String trimNumber(double n) { return n == Math.rint(n) ? Long.toString((long)n) : Double.toString(n); }
    private static final class ParseFailure extends RuntimeException {}
}
