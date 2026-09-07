package com.buttclapdev.twogamerl.editor;

import com.buttclapdev.twogamerl.model.GameProject.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;

final class LevelEditorPanel extends JPanel {
    enum Tool { PENCIL, RECTANGLE, FILL, SPAWN }
    private final EditorFrame editor;
    private final JComboBox<Level> levels = new JComboBox<>();
    private final JComboBox<TileDef> palette = new JComboBox<>();
    private final GridCanvas canvas = new GridCanvas();
    private Tool tool = Tool.PENCIL;
    private int selectedTile = 0;

    LevelEditorPanel(EditorFrame editor) {
        super(new BorderLayout(8,8));
        this.editor = editor;
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton add = new JButton("+ Nivel"), remove = new JButton("Eliminar");
        JButton pencil = new JButton("Lápiz"), rect = new JButton("Rectángulo"), fill = new JButton("Rellenar"), spawn = new JButton("Inicio jugador");
        JButton resize = new JButton("Cambiar tamaño");
        top.add(new JLabel("Nivel:")); top.add(levels); top.add(add); top.add(remove); top.add(resize);
        top.add(Box.createHorizontalStrut(16)); top.add(pencil); top.add(rect); top.add(fill); top.add(spawn);
        add(top, BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(4,4));
        left.setBorder(BorderFactory.createTitledBorder("Tile activo"));
        left.add(palette, BorderLayout.NORTH);
        JLabel hint = new JLabel("<html>Izq.: pintar<br>Der.: borrar<br>Rueda: zoom</html>");
        left.add(hint, BorderLayout.SOUTH);
        left.setPreferredSize(new Dimension(180,100));
        add(left, BorderLayout.WEST);
        add(new JScrollPane(canvas), BorderLayout.CENTER);

        levels.addActionListener(e -> { canvas.revalidate(); canvas.repaint(); });
        palette.addActionListener(e -> { TileDef t=(TileDef)palette.getSelectedItem(); if(t!=null) selectedTile=t.id; });
        add.addActionListener(e -> addLevel());
        remove.addActionListener(e -> removeLevel());
        resize.addActionListener(e -> resizeLevel());
        pencil.addActionListener(e -> tool=Tool.PENCIL);
        rect.addActionListener(e -> tool=Tool.RECTANGLE);
        fill.addActionListener(e -> tool=Tool.FILL);
        spawn.addActionListener(e -> tool=Tool.SPAWN);
        refreshModel();
    }

    void refreshModel() {
        Level selected=(Level)levels.getSelectedItem();
        levels.removeAllItems();
        for(Level l:editor.project().getLevels().values()) levels.addItem(l);
        if(selected!=null && editor.project().getLevels().containsKey(selected.id)) levels.setSelectedItem(editor.project().getLevels().get(selected.id));
        TileDef active=(TileDef)palette.getSelectedItem();
        palette.removeAllItems();
        for(TileDef t:editor.project().getTiles().values()) palette.addItem(t);
        if(active!=null && editor.project().getTiles().containsKey(active.id)) palette.setSelectedItem(editor.project().getTiles().get(active.id));
        canvas.revalidate(); canvas.repaint();
    }

    private Level current() { return (Level)levels.getSelectedItem(); }
    private void addLevel() {
        String name=JOptionPane.showInputDialog(this,"Nombre del nivel:","Nuevo nivel",JOptionPane.PLAIN_MESSAGE);
        if(name==null||name.isBlank())return;
        String id=slug(name, editor.project().getLevels().keySet());
        Level l=new Level(id,name.trim(),24,16);
        editor.project().getLevels().put(id,l); levels.addItem(l); levels.setSelectedItem(l); editor.markDirty();
    }
    private void removeLevel(){ Level l=current(); if(l==null||editor.project().getLevels().size()<=1)return; editor.project().getLevels().remove(l.id); refreshModel(); editor.markDirty(); }
    private void resizeLevel(){
        Level l=current(); if(l==null)return;
        JSpinner w=new JSpinner(new SpinnerNumberModel(l.width,4,256,1)), h=new JSpinner(new SpinnerNumberModel(l.height,4,256,1));
        JPanel p=new JPanel(new GridLayout(2,2,6,6)); p.add(new JLabel("Ancho"));p.add(w);p.add(new JLabel("Alto"));p.add(h);
        if(JOptionPane.showConfirmDialog(this,p,"Tamaño del nivel",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){l.resize((Integer)w.getValue(),(Integer)h.getValue());canvas.revalidate();canvas.repaint();editor.markDirty();}
    }
    private static String slug(String s, Set<String> used){String base=s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="level";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}

    private final class GridCanvas extends JComponent {
        private int zoom=32, dragStartX=-1, dragStartY=-1;
        GridCanvas(){
            setOpaque(true); setBackground(new Color(34,37,41));
            MouseAdapter mouse=new MouseAdapter(){
                @Override public void mousePressed(MouseEvent e){ handlePress(e); }
                @Override public void mouseDragged(MouseEvent e){ if(tool==Tool.PENCIL) paintAt(e); }
                @Override public void mouseReleased(MouseEvent e){ if(tool==Tool.RECTANGLE) finishRect(e); }
                @Override public void mouseWheelMoved(MouseWheelEvent e){zoom=Math.max(12,Math.min(64,zoom-e.getWheelRotation()*4));revalidate();repaint();}
            };
            addMouseListener(mouse); addMouseMotionListener(mouse); addMouseWheelListener(mouse);
        }
        @Override public Dimension getPreferredSize(){Level l=current();return l==null?new Dimension(640,480):new Dimension(l.width*zoom+1,l.height*zoom+1);}
        @Override protected void paintComponent(Graphics gg){super.paintComponent(gg);Level l=current();if(l==null)return;Graphics2D g=(Graphics2D)gg.create();g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);for(int y=0;y<l.height;y++)for(int x=0;x<l.width;x++){int px=x*zoom,py=y*zoom;TileDef t=editor.project().getTiles().get(l.get(x,y));BufferedImage img=null;if(t!=null&&!t.assetKey.isBlank()){Asset a=editor.project().getAssets().get(t.assetKey);if(a!=null)img=a.image();}if(img!=null)g.drawImage(img,px,py,zoom,zoom,null);else{g.setColor(t==null?Color.MAGENTA:t.color);g.fillRect(px,py,zoom,zoom);}g.setColor(new Color(255,255,255,30));g.drawRect(px,py,zoom,zoom);}g.setColor(new Color(255,213,79));g.setStroke(new BasicStroke(2));g.drawOval(l.spawnX*zoom+zoom/4,l.spawnY*zoom+zoom/4,zoom/2,zoom/2);g.dispose();}
        private void handlePress(MouseEvent e){Level l=current();if(l==null)return;int x=e.getX()/zoom,y=e.getY()/zoom;if(x<0||y<0||x>=l.width||y>=l.height)return;if(SwingUtilities.isRightMouseButton(e)){l.set(x,y,0);editor.markDirty();repaint();return;}dragStartX=x;dragStartY=y;if(tool==Tool.FILL){flood(l,x,y,l.get(x,y),selectedTile);editor.markDirty();repaint();}else if(tool==Tool.SPAWN){l.spawnX=x;l.spawnY=y;editor.markDirty();repaint();}else if(tool==Tool.PENCIL){l.set(x,y,selectedTile);editor.markDirty();repaint();}}
        private void paintAt(MouseEvent e){Level l=current();int x=e.getX()/zoom,y=e.getY()/zoom;if(l!=null&&x>=0&&y>=0&&x<l.width&&y<l.height){l.set(x,y,selectedTile);editor.markDirty();repaint();}}
        private void finishRect(MouseEvent e){Level l=current();if(l==null||dragStartX<0)return;int x=Math.max(0,Math.min(l.width-1,e.getX()/zoom)),y=Math.max(0,Math.min(l.height-1,e.getY()/zoom));for(int yy=Math.min(y,dragStartY);yy<=Math.max(y,dragStartY);yy++)for(int xx=Math.min(x,dragStartX);xx<=Math.max(x,dragStartX);xx++)l.set(xx,yy,selectedTile);dragStartX=dragStartY=-1;editor.markDirty();repaint();}
        private void flood(Level l,int sx,int sy,int old,int next){if(old==next)return;ArrayDeque<Point>q=new ArrayDeque<>();q.add(new Point(sx,sy));while(!q.isEmpty()){Point p=q.removeFirst();if(p.x<0||p.y<0||p.x>=l.width||p.y>=l.height||l.get(p.x,p.y)!=old)continue;l.set(p.x,p.y,next);q.add(new Point(p.x+1,p.y));q.add(new Point(p.x-1,p.y));q.add(new Point(p.x,p.y+1));q.add(new Point(p.x,p.y-1));}}
    }
}
