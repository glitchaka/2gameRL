package com.buttclapdev.twogamerl.editor;

import com.buttclapdev.twogamerl.export.ExecutableExporter;
import com.buttclapdev.twogamerl.io.ProjectIO;
import com.buttclapdev.twogamerl.model.GameProject;
import com.buttclapdev.twogamerl.runtime.GameLauncher;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.nio.file.*;

public final class EditorFrame extends JFrame {
    private GameProject project = GameProject.createDefault();
    private Path projectFile;
    private boolean dirty;
    private final LevelEditorPanel levelEditor;
    private final GraphicsEditorPanel graphicsEditor;
    private final MenuEditorPanel menuEditor;
    private final JLabel status = new JLabel("Proyecto nuevo");

    public EditorFrame() {
        super("2gameRL Studio");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1400, 860);
        setLocationRelativeTo(null);
        setJMenuBar(buildMenu());
        JToolBar toolbar = buildToolbar();
        levelEditor = new LevelEditorPanel(this);
        graphicsEditor = new GraphicsEditorPanel(this);
        menuEditor = new MenuEditorPanel(this);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Niveles", levelEditor);
        tabs.addTab("Gráficos", graphicsEditor);
        tabs.addTab("Menús", menuEditor);
        JPanel root = new JPanel(new BorderLayout()); root.add(toolbar, BorderLayout.NORTH); root.add(tabs, BorderLayout.CENTER);
        status.setBorder(BorderFactory.createEmptyBorder(4,8,4,8)); root.add(status, BorderLayout.SOUTH);
        setContentPane(root);
        addWindowListener(new WindowAdapter(){@Override public void windowClosing(WindowEvent e){if(confirmDiscard())dispose();}});
        updateTitle();
    }

    GameProject project() { return project; }
    void markDirty() { dirty=true; updateTitle(); }
    void refreshLevelEditor(){levelEditor.refreshModel();}
    void refreshAll(){levelEditor.refreshModel();graphicsEditor.refreshModel();menuEditor.refreshModel();}

    private JMenuBar buildMenu(){
        JMenuBar bar=new JMenuBar();
        JMenu file=new JMenu("Archivo"), projectMenu=new JMenu("Proyecto"), help=new JMenu("Ayuda");
        file.add(item("Nuevo", KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK), e->newProject()));
        file.add(item("Abrir…", KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK), e->openProject()));
        file.add(item("Guardar", KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), e->save(false)));
        file.add(item("Guardar como…", null, e->save(true)));file.addSeparator();
        file.add(item("Exportar juego ejecutable…", null, e->ExecutableExporter.export(this,project.deepCopy())));file.addSeparator();
        file.add(item("Salir", null, e->dispatchEvent(new WindowEvent(this,WindowEvent.WINDOW_CLOSING))));
        projectMenu.add(item("Configuración…", null, e->projectSettings()));
        projectMenu.add(item("Probar juego", KeyStroke.getKeyStroke(KeyEvent.VK_F5,0), e->GameLauncher.open(project.deepCopy(),false)));
        help.add(item("Acerca de", null, e->JOptionPane.showMessageDialog(this,"2gameRL Studio\nEditor visual y runtime del juego","Acerca de",JOptionPane.INFORMATION_MESSAGE)));
        bar.add(file);bar.add(projectMenu);bar.add(help);return bar;
    }
    private JToolBar buildToolbar(){JToolBar t=new JToolBar();t.setFloatable(false);JButton save=new JButton("Guardar"),play=new JButton("▶ Probar"),export=new JButton("Exportar EXE");save.addActionListener(e->save(false));play.addActionListener(e->GameLauncher.open(project.deepCopy(),false));export.addActionListener(e->ExecutableExporter.export(this,project.deepCopy()));t.add(save);t.addSeparator();t.add(play);t.add(export);return t;}
    private JMenuItem item(String text,KeyStroke key,ActionListener l){JMenuItem i=new JMenuItem(text);if(key!=null)i.setAccelerator(key);i.addActionListener(l);return i;}

    private void newProject(){if(!confirmDiscard())return;project=GameProject.createDefault();projectFile=null;dirty=false;refreshAll();status.setText("Proyecto nuevo");updateTitle();}
    private void openProject(){if(!confirmDiscard())return;JFileChooser fc=new JFileChooser();if(fc.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)return;try{project=ProjectIO.load(fc.getSelectedFile().toPath());projectFile=fc.getSelectedFile().toPath();dirty=false;refreshAll();status.setText("Abierto: "+projectFile);updateTitle();}catch(IOException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Abrir proyecto",JOptionPane.ERROR_MESSAGE);}}
    private boolean save(boolean choose){if(projectFile==null||choose){JFileChooser fc=new JFileChooser();fc.setSelectedFile(new java.io.File(project.getTitle().replaceAll("[^A-Za-z0-9._-]","_")+".2grl"));if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return false;projectFile=withExtension(fc.getSelectedFile().toPath(),".2grl");}try{ProjectIO.save(project,projectFile);dirty=false;status.setText("Guardado: "+projectFile);updateTitle();return true;}catch(IOException ex){JOptionPane.showMessageDialog(this,ex.getMessage(),"Guardar",JOptionPane.ERROR_MESSAGE);return false;}}
    private boolean confirmDiscard(){if(!dirty)return true;int r=JOptionPane.showConfirmDialog(this,"Hay cambios sin guardar. ¿Guardarlos antes de continuar?","2gameRL",JOptionPane.YES_NO_CANCEL_OPTION);if(r==JOptionPane.CANCEL_OPTION||r==JOptionPane.CLOSED_OPTION)return false;if(r==JOptionPane.YES_OPTION)return save(false);return true;}
    private void projectSettings(){
        JTextField title=new JTextField(project.getTitle());JSpinner tile=new JSpinner(new SpinnerNumberModel(project.getTileSize(),8,128,1));JComboBox<String> level=new JComboBox<>(project.getLevels().keySet().toArray(String[]::new)),menu=new JComboBox<>(project.getMenus().keySet().toArray(String[]::new));level.setSelectedItem(project.getStartLevel());menu.setSelectedItem(project.getStartMenu());JPanel p=new JPanel(new GridLayout(0,2,6,6));p.add(new JLabel("Título"));p.add(title);p.add(new JLabel("Tamaño base tile"));p.add(tile);p.add(new JLabel("Nivel inicial"));p.add(level);p.add(new JLabel("Menú inicial"));p.add(menu);if(JOptionPane.showConfirmDialog(this,p,"Configuración del proyecto",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION){project.setTitle(title.getText());project.setTileSize((Integer)tile.getValue());project.setStartLevel((String)level.getSelectedItem());project.setStartMenu((String)menu.getSelectedItem());markDirty();}}
    private void updateTitle(){setTitle("2gameRL Studio — "+project.getTitle()+(dirty?" *":""));}
    private static Path withExtension(Path p,String ext){String n=p.getFileName().toString();return n.toLowerCase().endsWith(ext)?p:p.resolveSibling(n+ext);}
}
