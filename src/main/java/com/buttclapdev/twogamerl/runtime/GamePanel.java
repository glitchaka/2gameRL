package com.buttclapdev.twogamerl.runtime;

import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.model.GameProject.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public final class GamePanel extends JPanel {
    private final GameProject project;
    private String activeMenu;
    private Level level;
    private int playerX, playerY;
    private boolean inGame;

    public GamePanel(GameProject project) {
        this.project = project;
        setPreferredSize(new Dimension(960, 640));
        setBackground(Color.BLACK);
        setFocusable(true);
        activeMenu = project.getStartMenu();
        installKeys();
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { clickMenu(e.getX(), e.getY()); }
        });
    }

    private void installKeys() {
        bind("pressed W", "up", () -> movePlayer(0, -1));
        bind("pressed UP", "up2", () -> movePlayer(0, -1));
        bind("pressed S", "down", () -> movePlayer(0, 1));
        bind("pressed DOWN", "down2", () -> movePlayer(0, 1));
        bind("pressed A", "left", () -> movePlayer(-1, 0));
        bind("pressed LEFT", "left2", () -> movePlayer(-1, 0));
        bind("pressed D", "right", () -> movePlayer(1, 0));
        bind("pressed RIGHT", "right2", () -> movePlayer(1, 0));
        bind("pressed ESCAPE", "escape", () -> { if (inGame) { inGame = false; activeMenu = project.getStartMenu(); repaint(); } });
    }

    private void bind(String stroke, String key, Runnable action) {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(stroke), key);
        getActionMap().put(key, new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { action.run(); } });
    }

    private void startGame() {
        level = project.getLevels().get(project.getStartLevel());
        if (level == null && !project.getLevels().isEmpty()) level = project.getLevels().values().iterator().next();
        if (level == null) return;
        playerX = level.spawnX;
        playerY = level.spawnY;
        inGame = true;
        repaint();
    }

    private void movePlayer(int dx, int dy) {
        if (!inGame || level == null) return;
        int nx = playerX + dx, ny = playerY + dy;
        if (nx < 0 || ny < 0 || nx >= level.width || ny >= level.height) return;
        TileDef tile = project.getTiles().get(level.get(nx, ny));
        if (tile == null || tile.walkable) { playerX = nx; playerY = ny; repaint(); }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        if (inGame) paintLevel(g); else paintMenu(g);
        g.dispose();
    }

    private void paintLevel(Graphics2D g) {
        if (level == null) return;
        int tile = Math.max(12, Math.min(project.getTileSize(), Math.min(getWidth() / Math.max(1, level.width), getHeight() / Math.max(1, level.height))));
        int ox = (getWidth() - level.width * tile) / 2;
        int oy = (getHeight() - level.height * tile) / 2;
        for (int y = 0; y < level.height; y++) for (int x = 0; x < level.width; x++) {
            TileDef def = project.getTiles().get(level.get(x, y));
            paintTile(g, def, ox + x * tile, oy + y * tile, tile);
        }
        int px = ox + playerX * tile;
        int py = oy + playerY * tile;
        g.setColor(new Color(245, 215, 90));
        int pad = Math.max(3, tile / 6);
        g.fillOval(px + pad, py + pad, tile - pad * 2, tile - pad * 2);
        g.setColor(new Color(40, 34, 20));
        g.drawOval(px + pad, py + pad, tile - pad * 2, tile - pad * 2);
        g.setColor(new Color(255,255,255,180));
        g.drawString("WASD/Flechas · ESC menú", 12, getHeight() - 12);
    }

    private void paintTile(Graphics2D g, TileDef def, int x, int y, int size) {
        if (def == null) { g.setColor(Color.MAGENTA); g.fillRect(x,y,size,size); return; }
        BufferedImage image = null;
        if (!def.assetKey.isBlank()) {
            Asset a = project.getAssets().get(def.assetKey);
            if (a != null) image = a.image();
        }
        if (image != null) g.drawImage(image, x, y, size, size, null);
        else { g.setColor(def.color); g.fillRect(x, y, size, size); }
    }

    private void paintMenu(Graphics2D g) {
        MenuScreen menu = project.getMenus().get(activeMenu);
        if (menu == null && !project.getMenus().isEmpty()) menu = project.getMenus().values().iterator().next();
        if (menu == null) { startGame(); return; }
        g.setColor(menu.background);
        g.fillRect(0,0,getWidth(),getHeight());
        g.setColor(Color.WHITE);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 30f));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(menu.title, (getWidth()-fm.stringWidth(menu.title))/2, 100);
        double sx = getWidth() / 640.0, sy = getHeight() / 480.0;
        g.setFont(g.getFont().deriveFont(Font.BOLD, 16f));
        for (MenuButton b : menu.buttons) {
            int x=(int)(b.x*sx), y=(int)(b.y*sy), w=(int)(b.width*sx), h=(int)(b.height*sy);
            g.setColor(new Color(62, 69, 78)); g.fillRoundRect(x,y,w,h,10,10);
            g.setColor(new Color(125,135,148)); g.drawRoundRect(x,y,w,h,10,10);
            g.setColor(Color.WHITE); FontMetrics bf = g.getFontMetrics();
            g.drawString(b.text, x+(w-bf.stringWidth(b.text))/2, y+(h+bf.getAscent()-bf.getDescent())/2);
        }
    }

    private void clickMenu(int mx, int my) {
        if (inGame) return;
        MenuScreen menu = project.getMenus().get(activeMenu);
        if (menu == null) return;
        double sx = getWidth() / 640.0, sy = getHeight() / 480.0;
        for (MenuButton b : menu.buttons) {
            Rectangle r = new Rectangle((int)(b.x*sx),(int)(b.y*sy),(int)(b.width*sx),(int)(b.height*sy));
            if (!r.contains(mx,my)) continue;
            switch (b.action) {
                case START_GAME -> startGame();
                case OPEN_MENU -> { if (project.getMenus().containsKey(b.target)) activeMenu = b.target; repaint(); }
                case EXIT -> SwingUtilities.getWindowAncestor(this).dispose();
            }
            break;
        }
    }
}
