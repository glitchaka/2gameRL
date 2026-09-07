package com.buttclapdev.twogamerl.editor;

import com.buttclapdev.twogamerl.model.GameProject.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.Locale;

final class GraphicsEditorPanel extends JPanel {
    private final EditorFrame editor;
    private final DefaultListModel<Asset> assetsModel=new DefaultListModel<>();
    private final JList<Asset> assets=new JList<>(assetsModel);
    private final JLabel preview=new JLabel("Sin imagen",SwingConstants.CENTER);
    private final DefaultListModel<TileDef> tilesModel=new DefaultListModel<>();
    private final JList<TileDef> tiles=new JList<>(tilesModel);
    private final JTextField tileName=new JTextField();
    private final JCheckBox walkable=new JCheckBox("Transitable");
    private final JComboBox<Object> assetCombo=new JComboBox<>();
    private final JButton color=new JButton("Color");

    GraphicsEditorPanel(EditorFrame editor){
        super(new BorderLayout(8,8));this.editor=editor;
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,assetPane(),tilePane());split.setResizeWeight(.45);add(split,BorderLayout.CENTER);refreshModel();
    }
    private JComponent assetPane(){
        JPanel p=new JPanel(new BorderLayout(6,6));p.setBorder(BorderFactory.createTitledBorder("Biblioteca gráfica"));
        assets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);p.add(new JScrollPane(assets),BorderLayout.WEST);preview.setPreferredSize(new Dimension(320,320));preview.setBorder(BorderFactory.createEtchedBorder());p.add(preview,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));JButton imp=new JButton("Importar imagen"),del=new JButton("Eliminar");buttons.add(imp);buttons.add(del);p.add(buttons,BorderLayout.SOUTH);
        imp.addActionListener(e->importAsset());del.addActionListener(e->deleteAsset());assets.addListSelectionListener(this::assetSelected);return p;
    }
    private JComponent tilePane(){
        JPanel p=new JPanel(new BorderLayout(6,6));p.setBorder(BorderFactory.createTitledBorder("Tiles"));p.add(new JScrollPane(tiles),BorderLayout.WEST);
        JPanel form=new JPanel(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.insets=new Insets(5,5,5,5);c.fill=GridBagConstraints.HORIZONTAL;c.weightx=1;c.gridx=0;c.gridy=0;form.add(new JLabel("Nombre"),c);c.gridy++;form.add(tileName,c);c.gridy++;form.add(walkable,c);c.gridy++;form.add(new JLabel("Imagen"),c);c.gridy++;form.add(assetCombo,c);c.gridy++;form.add(color,c);c.gridy++;c.weighty=1;form.add(Box.createVerticalGlue(),c);p.add(form,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));JButton add=new JButton("+ Tile"),del=new JButton("Eliminar tile");buttons.add(add);buttons.add(del);p.add(buttons,BorderLayout.SOUTH);
        tiles.addListSelectionListener(e->{if(!e.getValueIsAdjusting())loadTile();});tileName.addActionListener(e->applyTile());walkable.addActionListener(e->applyTile());assetCombo.addActionListener(e->applyTile());color.addActionListener(e->chooseColor());add.addActionListener(e->addTile());del.addActionListener(e->deleteTile());return p;
    }
    void refreshModel(){
        Asset selected=assets.getSelectedValue();assetsModel.clear();for(Asset a:editor.project().getAssets().values())assetsModel.addElement(a);if(selected!=null)assets.setSelectedValue(editor.project().getAssets().get(selected.key),true);
        TileDef ts=tiles.getSelectedValue();tilesModel.clear();for(TileDef t:editor.project().getTiles().values())tilesModel.addElement(t);if(ts!=null)tiles.setSelectedValue(editor.project().getTiles().get(ts.id),true);if(tiles.getSelectedIndex()<0&&!tilesModel.isEmpty())tiles.setSelectedIndex(0);
        rebuildAssetCombo();
    }
    private void rebuildAssetCombo(){Object old=assetCombo.getSelectedItem();assetCombo.removeAllItems();assetCombo.addItem("(sin imagen)");for(Asset a:editor.project().getAssets().values())assetCombo.addItem(a);if(old instanceof Asset a&&editor.project().getAssets().containsKey(a.key))assetCombo.setSelectedItem(editor.project().getAssets().get(a.key));}
    private void importAsset(){JFileChooser fc=new JFileChooser();fc.setMultiSelectionEnabled(true);if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;for(java.io.File f:fc.getSelectedFiles())try{String ext=f.getName().toLowerCase(Locale.ROOT);if(!(ext.endsWith(".png")||ext.endsWith(".jpg")||ext.endsWith(".jpeg")||ext.endsWith(".gif")))continue;String key=uniqueKey(f.getName());editor.project().getAssets().put(key,new Asset(key,f.getName(),Files.readAllBytes(f.toPath())));}catch(IOException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);}refreshModel();editor.refreshAll();editor.markDirty();}
    private String uniqueKey(String name){String base=name.replaceAll("[^A-Za-z0-9._-]","_");String key=base;int n=2;while(editor.project().getAssets().containsKey(key))key=n+++"_"+base;return key;}
    private void deleteAsset(){Asset a=assets.getSelectedValue();if(a==null)return;editor.project().getAssets().remove(a.key);for(TileDef t:editor.project().getTiles().values())if(t.assetKey.equals(a.key))t.assetKey="";refreshModel();editor.refreshAll();editor.markDirty();}
    private void assetSelected(ListSelectionEvent e){if(e.getValueIsAdjusting())return;Asset a=assets.getSelectedValue();if(a==null||a.image()==null){preview.setIcon(null);preview.setText("Sin imagen");return;}Image img=a.image();int w=Math.max(1,preview.getWidth()-12),h=Math.max(1,preview.getHeight()-12);double s=Math.min((double)w/img.getWidth(null),(double)h/img.getHeight(null));preview.setText("");preview.setIcon(new ImageIcon(img.getScaledInstance(Math.max(1,(int)(img.getWidth(null)*s)),Math.max(1,(int)(img.getHeight(null)*s)),Image.SCALE_FAST)));}
    private void loadTile(){TileDef t=tiles.getSelectedValue();if(t==null)return;tileName.setText(t.name);walkable.setSelected(t.walkable);color.setBackground(t.color);if(t.assetKey.isBlank())assetCombo.setSelectedIndex(0);else{Asset a=editor.project().getAssets().get(t.assetKey);assetCombo.setSelectedItem(a==null?"(sin imagen)":a);}}
    private void applyTile(){TileDef t=tiles.getSelectedValue();if(t==null)return;t.name=tileName.getText().isBlank()?"Tile "+t.id:tileName.getText().trim();t.walkable=walkable.isSelected();Object a=assetCombo.getSelectedItem();t.assetKey=a instanceof Asset asset?asset.key:"";tiles.repaint();editor.refreshLevelEditor();editor.markDirty();}
    private void chooseColor(){TileDef t=tiles.getSelectedValue();if(t==null)return;Color chosen=JColorChooser.showDialog(this,"Color del tile",t.color);if(chosen!=null){t.color=chosen;color.setBackground(chosen);editor.refreshLevelEditor();editor.markDirty();}}
    private void addTile(){int id=editor.project().nextTileId();TileDef t=new TileDef(id,"Tile "+id,new Color(110,110,110),true,"");editor.project().getTiles().put(id,t);refreshModel();tiles.setSelectedValue(t,true);editor.refreshAll();editor.markDirty();}
    private void deleteTile(){TileDef t=tiles.getSelectedValue();if(t==null||t.id==0||editor.project().getTiles().size()<=1)return;editor.project().getTiles().remove(t.id);for(var l:editor.project().getLevels().values())for(int y=0;y<l.height;y++)for(int x=0;x<l.width;x++)if(l.get(x,y)==t.id)l.set(x,y,0);refreshModel();editor.refreshAll();editor.markDirty();}
}
