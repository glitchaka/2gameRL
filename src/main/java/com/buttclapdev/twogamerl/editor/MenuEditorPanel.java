package com.buttclapdev.twogamerl.editor;

import com.buttclapdev.twogamerl.model.GameProject.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

final class MenuEditorPanel extends JPanel {
    private final EditorFrame editor;
    private final DefaultListModel<MenuScreen> menusModel=new DefaultListModel<>();
    private final JList<MenuScreen> menus=new JList<>(menusModel);
    private final DefaultListModel<MenuButton> buttonsModel=new DefaultListModel<>();
    private final JList<MenuButton> buttons=new JList<>(buttonsModel);
    private final MenuCanvas canvas=new MenuCanvas();
    private final JTextField text=new JTextField();
    private final JComboBox<MenuAction> action=new JComboBox<>(MenuAction.values());
    private final JComboBox<String> target=new JComboBox<>();
    private final JSpinner x=new JSpinner(new SpinnerNumberModel(100,0,640,1)), y=new JSpinner(new SpinnerNumberModel(100,0,480,1)), w=new JSpinner(new SpinnerNumberModel(180,30,640,1)), h=new JSpinner(new SpinnerNumberModel(44,20,480,1));

    MenuEditorPanel(EditorFrame editor){super(new BorderLayout(8,8));this.editor=editor;add(left(),BorderLayout.WEST);add(canvas,BorderLayout.CENTER);add(right(),BorderLayout.EAST);refreshModel();}
    private JComponent left(){JPanel p=new JPanel(new BorderLayout(5,5));p.setPreferredSize(new Dimension(190,100));p.setBorder(BorderFactory.createTitledBorder("Pantallas"));p.add(new JScrollPane(menus),BorderLayout.CENTER);JPanel b=new JPanel(new GridLayout(1,2));JButton add=new JButton("+"),del=new JButton("−");b.add(add);b.add(del);p.add(b,BorderLayout.SOUTH);menus.addListSelectionListener(e->{if(!e.getValueIsAdjusting())loadMenu();});add.addActionListener(e->addMenu());del.addActionListener(e->deleteMenu());return p;}
    private JComponent right(){JPanel p=new JPanel(new BorderLayout(5,5));p.setPreferredSize(new Dimension(250,100));p.setBorder(BorderFactory.createTitledBorder("Botones"));p.add(new JScrollPane(buttons),BorderLayout.NORTH);JPanel f=new JPanel(new GridLayout(0,2,5,5));f.add(new JLabel("Texto"));f.add(text);f.add(new JLabel("Acción"));f.add(action);f.add(new JLabel("Destino"));f.add(target);f.add(new JLabel("X"));f.add(x);f.add(new JLabel("Y"));f.add(y);f.add(new JLabel("Ancho"));f.add(w);f.add(new JLabel("Alto"));f.add(h);p.add(f,BorderLayout.CENTER);JPanel b=new JPanel(new FlowLayout());JButton add=new JButton("+ Botón"),del=new JButton("Eliminar");b.add(add);b.add(del);p.add(b,BorderLayout.SOUTH);buttons.addListSelectionListener(e->{if(!e.getValueIsAdjusting())loadButton();});text.addActionListener(e->applyButton());action.addActionListener(e->applyButton());target.addActionListener(e->applyButton());for(JSpinner s:new JSpinner[]{x,y,w,h})s.addChangeListener(e->applyButton());add.addActionListener(e->addButton());del.addActionListener(e->deleteButton());return p;}
    void refreshModel(){MenuScreen selected=menus.getSelectedValue();menusModel.clear();target.removeAllItems();for(MenuScreen m:editor.project().getMenus().values()){menusModel.addElement(m);target.addItem(m.id);}if(selected!=null&&editor.project().getMenus().containsKey(selected.id))menus.setSelectedValue(editor.project().getMenus().get(selected.id),true);if(menus.getSelectedIndex()<0&&!menusModel.isEmpty())menus.setSelectedIndex(0);loadMenu();}
    private MenuScreen currentMenu(){return menus.getSelectedValue();}
    private MenuButton currentButton(){return buttons.getSelectedValue();}
    private void loadMenu(){buttonsModel.clear();MenuScreen m=currentMenu();if(m!=null)for(MenuButton b:m.buttons)buttonsModel.addElement(b);canvas.repaint();}
    private void loadButton(){MenuButton b=currentButton();if(b==null)return;text.setText(b.text);action.setSelectedItem(b.action);target.setSelectedItem(b.target);x.setValue(b.x);y.setValue(b.y);w.setValue(b.width);h.setValue(b.height);canvas.repaint();}
    private void applyButton(){MenuButton b=currentButton();if(b==null)return;b.text=text.getText().isBlank()?"Botón":text.getText();b.action=(MenuAction)action.getSelectedItem();Object t=target.getSelectedItem();b.target=t==null?"":t.toString();b.x=(Integer)x.getValue();b.y=(Integer)y.getValue();b.width=(Integer)w.getValue();b.height=(Integer)h.getValue();buttons.repaint();canvas.repaint();editor.markDirty();}
    private void addMenu(){String name=JOptionPane.showInputDialog(this,"Título de la pantalla:","Nueva pantalla",JOptionPane.PLAIN_MESSAGE);if(name==null||name.isBlank())return;String id=slug(name,editor.project().getMenus().keySet());MenuScreen m=new MenuScreen(id,name.trim());editor.project().getMenus().put(id,m);refreshModel();menus.setSelectedValue(m,true);editor.markDirty();}
    private void deleteMenu(){MenuScreen m=currentMenu();if(m==null||editor.project().getMenus().size()<=1)return;editor.project().getMenus().remove(m.id);if(Objects.equals(editor.project().getStartMenu(),m.id))editor.project().setStartMenu(editor.project().getMenus().keySet().iterator().next());refreshModel();editor.markDirty();}
    private void addButton(){MenuScreen m=currentMenu();if(m==null)return;MenuButton b=new MenuButton("Botón",220,180+m.buttons.size()*52,200,44,MenuAction.START_GAME,"");m.buttons.add(b);buttonsModel.addElement(b);buttons.setSelectedValue(b,true);canvas.repaint();editor.markDirty();}
    private void deleteButton(){MenuScreen m=currentMenu();MenuButton b=currentButton();if(m==null||b==null)return;m.buttons.remove(b);loadMenu();editor.markDirty();}
    private static String slug(String s,Set<String>used){String base=s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)","");if(base.isBlank())base="menu";String id=base;int n=2;while(used.contains(id))id=base+"-"+n++;return id;}

    private final class MenuCanvas extends JComponent {
        private MenuButton dragging;private int dx,dy;
        MenuCanvas(){setPreferredSize(new Dimension(640,480));setMinimumSize(new Dimension(480,360));MouseAdapter m=new MouseAdapter(){@Override public void mousePressed(MouseEvent e){MenuScreen menu=currentMenu();if(menu==null)return;double sx=getWidth()/640.0,sy=getHeight()/480.0;for(int i=menu.buttons.size()-1;i>=0;i--){MenuButton b=menu.buttons.get(i);Rectangle r=new Rectangle((int)(b.x*sx),(int)(b.y*sy),(int)(b.width*sx),(int)(b.height*sy));if(r.contains(e.getPoint())){dragging=b;dx=e.getX()-r.x;dy=e.getY()-r.y;buttons.setSelectedValue(b,true);break;}}}@Override public void mouseDragged(MouseEvent e){if(dragging==null)return;double sx=getWidth()/640.0,sy=getHeight()/480.0;dragging.x=Math.max(0,Math.min(640-dragging.width,(int)((e.getX()-dx)/sx)));dragging.y=Math.max(0,Math.min(480-dragging.height,(int)((e.getY()-dy)/sy)));x.setValue(dragging.x);y.setValue(dragging.y);editor.markDirty();repaint();}@Override public void mouseReleased(MouseEvent e){dragging=null;}};addMouseListener(m);addMouseMotionListener(m);}
        @Override protected void paintComponent(Graphics gg){super.paintComponent(gg);MenuScreen menu=currentMenu();if(menu==null)return;Graphics2D g=(Graphics2D)gg.create();g.setColor(menu.background);g.fillRect(0,0,getWidth(),getHeight());double sx=getWidth()/640.0,sy=getHeight()/480.0;g.setColor(Color.WHITE);g.setFont(g.getFont().deriveFont(Font.BOLD,Math.max(18f,(float)(30*sy))));FontMetrics fm=g.getFontMetrics();g.drawString(menu.title,(getWidth()-fm.stringWidth(menu.title))/2,(int)(80*sy));for(MenuButton b:menu.buttons){int bx=(int)(b.x*sx),by=(int)(b.y*sy),bw=(int)(b.width*sx),bh=(int)(b.height*sy);g.setColor(b==currentButton()?new Color(84,108,140):new Color(62,69,78));g.fillRoundRect(bx,by,bw,bh,10,10);g.setColor(new Color(150,160,175));g.drawRoundRect(bx,by,bw,bh,10,10);g.setColor(Color.WHITE);g.setFont(g.getFont().deriveFont(Font.BOLD,Math.max(11f,(float)(15*sy))));FontMetrics bf=g.getFontMetrics();g.drawString(b.text,bx+(bw-bf.stringWidth(b.text))/2,by+(bh+bf.getAscent()-bf.getDescent())/2);}g.dispose();}
    }
}
