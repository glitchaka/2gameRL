package com.buttclapdev.twogamerl.script;

import java.util.*;

public final class ScriptProgram {
    public enum Event { START, UPDATE, CLICK, DOUBLECLICK, COLLISION, TRIGGER }

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
    }

    private interface Instruction { void run(Context context); }
    private record WaitInstruction(double seconds) implements Instruction { @Override public void run(Context context) {} }
    private record TimerInstruction(double seconds, Instruction nested) implements Instruction {
        @Override public void run(Context context) { context.schedule(seconds, () -> nested.run(context)); }
    }
    public record Validation(boolean valid, List<String> errors) {}

    private final EnumMap<Event, List<Instruction>> instructions = new EnumMap<>(Event.class);
    private final List<String> errors = new ArrayList<>();

    private ScriptProgram() { for (Event e : Event.values()) instructions.put(e, new ArrayList<>()); }

    public static ScriptProgram compile(String source) {
        ScriptProgram p = new ScriptProgram();
        p.parse(source == null ? "" : source);
        return p;
    }

    public Validation validation() { return new Validation(errors.isEmpty(), List.copyOf(errors)); }

    public void fire(Event event, Context context) {
        if (!errors.isEmpty()) return;
        execute(instructions.get(event), 0, context);
    }

    private void execute(List<Instruction> sequence, int start, Context context) {
        for (int i = start; i < sequence.size(); i++) {
            Instruction instruction = sequence.get(i);
            if (instruction instanceof WaitInstruction wait) {
                int next = i + 1;
                context.schedule(wait.seconds(), () -> execute(sequence, next, context));
                return;
            }
            try {
                instruction.run(context);
            } catch (RuntimeException ex) {
                context.log("Script: " + ex.getMessage());
            }
        }
    }

    private void parse(String source) {
        Event current = null;
        String[] lines = source.replace("\r", "").split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) continue;
            if (line.toLowerCase(Locale.ROOT).startsWith("on ")) {
                if (current != null) { error(lineNo, "ya hay un bloque 'on' abierto"); continue; }
                String eventName = line.substring(3).trim().replace("_", "").toUpperCase(Locale.ROOT);
                try { current = Event.valueOf(eventName); }
                catch (IllegalArgumentException e) { error(lineNo, "evento desconocido: " + eventName); }
                continue;
            }
            if (line.equalsIgnoreCase("end")) {
                if (current == null) error(lineNo, "'end' sin bloque 'on'");
                current = null;
                continue;
            }
            if (current == null) { error(lineNo, "comando fuera de un bloque 'on ... end'"); continue; }
            Instruction instruction = parseInstruction(lineNo, line);
            if (instruction != null) instructions.get(current).add(instruction);
        }
        if (current != null) errors.add("Falta 'end' al final del script.");
    }

    private Instruction parseInstruction(int lineNo, String line) {
        List<String> parts = tokens(line);
        if (parts.isEmpty()) return null;
        String op = parts.get(0).toLowerCase(Locale.ROOT);
        try {
            return switch (op) {
                case "log" -> {
                    require(parts, 2, lineNo, "log <texto>");
                    String message = join(parts, 1);
                    yield c -> c.log(message);
                }
                case "move" -> {
                    requireExact(parts, 3, lineNo, "move <x> <y>");
                    double x = number(parts.get(1), lineNo), y = number(parts.get(2), lineNo);
                    yield c -> c.move(x, y);
                }
                case "velocity" -> {
                    requireExact(parts, 3, lineNo, "velocity <x> <y>");
                    double x = number(parts.get(1), lineNo), y = number(parts.get(2), lineNo);
                    yield c -> c.setVelocity(x, y);
                }
                case "teleport" -> {
                    requireExact(parts, 3, lineNo, "teleport <x> <y>");
                    double x = number(parts.get(1), lineNo), y = number(parts.get(2), lineNo);
                    yield c -> c.teleport(x, y);
                }
                case "bounce" -> { requireExact(parts, 1, lineNo, "bounce"); yield Context::bounce; }
                case "destroy" -> { requireExact(parts, 1, lineNo, "destroy"); yield Context::destroy; }
                case "wait" -> {
                    requireExact(parts, 2, lineNo, "wait <segundos>");
                    double seconds = nonNegative(number(parts.get(1), lineNo), lineNo, "wait");
                    yield new WaitInstruction(seconds);
                }
                case "timer" -> {
                    if (parts.size() < 3) { error(lineNo, "uso: timer <segundos> <comando>"); throw new ParseFailure(); }
                    double seconds = nonNegative(number(parts.get(1), lineNo), lineNo, "timer");
                    Instruction nested = parseInstruction(lineNo, join(parts, 2));
                    if (nested == null || nested instanceof WaitInstruction) {
                        if (nested instanceof WaitInstruction) error(lineNo, "timer no puede contener wait; usa 'wait <segundos>' en una línea propia");
                        throw new ParseFailure();
                    }
                    yield new TimerInstruction(seconds, nested);
                }
                case "create" -> {
                    if (parts.size() != 2 && parts.size() != 4) { error(lineNo, "uso: create <entidad> [x y]"); throw new ParseFailure(); }
                    String template = parts.get(1);
                    Double x = null, y = null;
                    if (parts.size() == 4) { x = number(parts.get(2), lineNo); y = number(parts.get(3), lineNo); }
                    Double px = x, py = y;
                    yield c -> c.createEntity(template, px, py);
                }
                case "loadscene" -> {
                    requireExact(parts, 2, lineNo, "loadScene <id>");
                    String id = parts.get(1);
                    yield c -> c.loadScene(id);
                }
                case "setsprite" -> {
                    requireExact(parts, 2, lineNo, "setSprite <asset>");
                    String id = parts.get(1);
                    yield c -> c.setSprite(id);
                }
                case "setvar" -> {
                    require(parts, 3, lineNo, "setVar <nombre> <valor>");
                    String key = parts.get(1), value = join(parts, 2);
                    yield c -> c.setVariable(key, value);
                }
                case "addvar" -> {
                    requireExact(parts, 3, lineNo, "addVar <nombre> <número>");
                    String key = parts.get(1); double amount = number(parts.get(2), lineNo);
                    yield c -> {
                        double old;
                        try { old = Double.parseDouble(c.variable(key)); } catch (Exception e) { old = 0; }
                        c.setVariable(key, trimNumber(old + amount));
                    };
                }
                case "set" -> {
                    require(parts, 3, lineNo, "set <propiedad> <valor>");
                    String key = parts.get(1), value = join(parts, 2);
                    yield c -> c.setProperty(key, value);
                }
                case "ifkey", "ifpressed" -> parseConditional(lineNo, parts, op.equals("ifpressed"));
                default -> { error(lineNo, "comando desconocido: " + parts.get(0)); yield null; }
            };
        } catch (ParseFailure ignored) { return null; }
    }

    private Instruction parseConditional(int lineNo, List<String> parts, boolean pressed) {
        if (parts.size() < 3) { error(lineNo, (pressed ? "ifPressed" : "ifKey") + " <tecla> <comando>"); throw new ParseFailure(); }
        String key = parts.get(1);
        Instruction nested = parseInstruction(lineNo, join(parts, 2));
        if (nested == null || nested instanceof WaitInstruction) {
            if (nested instanceof WaitInstruction) error(lineNo, "wait debe ir en una línea propia, no dentro de ifKey/ifPressed");
            throw new ParseFailure();
        }
        return c -> { if (pressed ? c.keyPressed(key) : c.keyDown(key)) nested.run(c); };
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
    private void require(List<String> parts, int count, int lineNo, String usage) { if (parts.size() < count) { error(lineNo, "uso: " + usage); throw new ParseFailure(); } }
    private void requireExact(List<String> parts, int count, int lineNo, String usage) { if (parts.size() != count) { error(lineNo, "uso: " + usage); throw new ParseFailure(); } }
    private double number(String value, int lineNo) { try { return Double.parseDouble(value); } catch (NumberFormatException e) { error(lineNo, "número inválido: " + value); throw new ParseFailure(); } }
    private double nonNegative(double value, int lineNo, String op) { if (value < 0) { error(lineNo, op + " no acepta tiempo negativo"); throw new ParseFailure(); } return value; }
    private void error(int line, String message) { errors.add("Línea " + line + ": " + message); }
    private static String trimNumber(double n) { return n == Math.rint(n) ? Long.toString((long)n) : Double.toString(n); }
    private static final class ParseFailure extends RuntimeException {}
}
