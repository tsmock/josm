// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.actions.AlignInCircleAction;
import org.openstreetmap.josm.actions.AlignInLineAction;
import org.openstreetmap.josm.actions.AutoScaleAction;
import org.openstreetmap.josm.actions.CombineWayAction;
import org.openstreetmap.josm.actions.CopyAction;
import org.openstreetmap.josm.actions.DownloadAction;
import org.openstreetmap.josm.actions.DuplicateAction;
import org.openstreetmap.josm.actions.ExitAction;
import org.openstreetmap.josm.actions.GpxExportAction;
import org.openstreetmap.josm.actions.HelpAction;
import org.openstreetmap.josm.actions.JoinNodeWayAction;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.actions.NewAction;
import org.openstreetmap.josm.actions.OpenAction;
import org.openstreetmap.josm.actions.PasteAction;
import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.actions.RedoAction;
import org.openstreetmap.josm.actions.ReverseWayAction;
import org.openstreetmap.josm.actions.SaveAction;
import org.openstreetmap.josm.actions.SaveAsAction;
import org.openstreetmap.josm.actions.SelectAllAction;
import org.openstreetmap.josm.actions.SplitWayAction;
import org.openstreetmap.josm.actions.UndoAction;
import org.openstreetmap.josm.actions.UnselectAllAction;
import org.openstreetmap.josm.actions.UploadAction;
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.data.DataSetChecker;
import org.openstreetmap.josm.data.Preferences;

/**
 * This is the JOSM main menu bar. It is overwritten to initialize itself and provide
 * all menu entries as member variables (sort of collect them).
 *
 * It also provides possibilities to attach new menu entries (used by plugins).
 *
 * @author Immanuel.Scholz
 */
public class MainMenu extends JMenuBar {

    /* File menu */
    public final NewAction newAction = new NewAction();
    public final OpenAction open = new OpenAction();
    public final JosmAction save = new SaveAction(null);
    public final JosmAction saveAs = new SaveAsAction(null);
    public final JosmAction gpxExport = new GpxExportAction(null);
    public final DownloadAction download = new DownloadAction();
    public final JosmAction upload = new UploadAction();
    public final JosmAction exit = new ExitAction();
    
    /* Edit menu */
    public final UndoAction undo = new UndoAction();
    public final RedoAction redo = new RedoAction();
    public final JosmAction copy = new CopyAction();
    public final JosmAction paste = new PasteAction();
    public final JosmAction duplicate = new DuplicateAction(); 
    public final JosmAction selectAll = new SelectAllAction();
    public final JosmAction unselectAll = new UnselectAllAction();
    /* crashes when loading data, if using JosmAction for search */
    public final JosmAction search = new SearchAction();
    public final JosmAction preferences = new PreferencesAction();
        
    /* View menu */
    
    /* Tools menu */
    public final JosmAction splitWay = new SplitWayAction();
    public final JosmAction combineWay = new CombineWayAction();
    public final JosmAction reverseWay = new ReverseWayAction();
    public final JosmAction alignInCircle = new AlignInCircleAction();
    public final JosmAction alignInLine = new AlignInLineAction();
    public final JosmAction mergeNodes = new MergeNodesAction();
    public final JosmAction joinNodeWay = new JoinNodeWayAction();

    /* Help menu */
    public final HelpAction help = new HelpAction();
    public final JosmAction about = new AboutAction();
    
    public final JMenu fileMenu = new JMenu(tr("File"));
    public final JMenu editMenu = new JMenu(tr("Edit"));
    public final JMenu viewMenu = new JMenu(tr("View"));
    public final JMenu toolsMenu = new JMenu(tr("Tools"));
    public final JMenu presetsMenu = new JMenu(tr("Presets"));
    public final JMenu helpMenu = new JMenu(tr("Help"));
        

    public MainMenu() {
        JMenuItem current;
        
        fileMenu.setMnemonic('F');
        current = fileMenu.add(newAction);
        current.setAccelerator(newAction.shortCut);
        current = fileMenu.add(open);
        current.setAccelerator(open.shortCut);
        fileMenu.addSeparator();
        current = fileMenu.add(save);
        current.setAccelerator(save.shortCut);
        current = fileMenu.add(saveAs);
        current.setAccelerator(saveAs.shortCut);
        current = fileMenu.add(gpxExport);
        current.setAccelerator(gpxExport.shortCut);
        fileMenu.addSeparator();
        current = fileMenu.add(download);
        current.setAccelerator(download.shortCut);
        current = fileMenu.add(upload);
        current.setAccelerator(upload.shortCut);
        fileMenu.addSeparator();
        current = fileMenu.add(exit);
        current.setAccelerator(exit.shortCut);
        add(fileMenu);

        editMenu.setMnemonic('E');
        current = editMenu.add(undo);
        current.setAccelerator(undo.shortCut);
        current = editMenu.add(redo);
        current.setAccelerator(redo.shortCut);
        editMenu.addSeparator();
        current = editMenu.add(copy);
        current.setAccelerator(copy.shortCut);
        current = editMenu.add(paste);
        current.setAccelerator(paste.shortCut);
        current = editMenu.add(duplicate);
        current.setAccelerator(duplicate.shortCut);
        editMenu.addSeparator();
        current = editMenu.add(selectAll);
        current.setAccelerator(selectAll.shortCut);
        current = editMenu.add(unselectAll);
        current.setAccelerator(unselectAll.shortCut);
        editMenu.addSeparator();
        current = editMenu.add(search);
        current.setAccelerator(search.shortCut);
        editMenu.addSeparator();
        current = editMenu.add(preferences);
        current.setAccelerator(preferences.shortCut);
        add(editMenu);
        
        viewMenu.setMnemonic('V');
        for (String mode : AutoScaleAction.modes) {
            JosmAction autoScaleAction = new AutoScaleAction(mode);
            current = viewMenu.add(autoScaleAction);
            current.setAccelerator(autoScaleAction.shortCut);
        }
        viewMenu.addSeparator();

        // TODO move code to an "action" like the others?
        final JCheckBoxMenuItem wireframe = new JCheckBoxMenuItem(tr("Wireframe view"));
        wireframe.setSelected(Main.pref.getBoolean("draw.wireframe", true));     
        wireframe.setAccelerator(KeyStroke.getKeyStroke("ctrl W"));
        wireframe.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                Main.pref.put("draw.wireframe", wireframe.isSelected());
                if (Main.map != null) {
                    Main.map.mapView.repaint();
                }
            }
        });
        viewMenu.add(wireframe);
        
        add(viewMenu);

        toolsMenu.setMnemonic('T');
        current = toolsMenu.add(splitWay);
        current.setAccelerator(splitWay.shortCut);
        current = toolsMenu.add(combineWay);
        current.setAccelerator(combineWay.shortCut);
        toolsMenu.addSeparator();
        current = toolsMenu.add(reverseWay);
        current.setAccelerator(reverseWay.shortCut);
        toolsMenu.addSeparator();
        current = toolsMenu.add(alignInCircle);
        current.setAccelerator(alignInCircle.shortCut);
        current = toolsMenu.add(alignInLine);
        current.setAccelerator(alignInLine.shortCut);
        toolsMenu.addSeparator();
        current = toolsMenu.add(mergeNodes);
        current.setAccelerator(mergeNodes.shortCut);
        current = toolsMenu.add(joinNodeWay);
        current.setAccelerator(joinNodeWay.shortCut);
        add(toolsMenu);

        add(presetsMenu);
        presetsMenu.setMnemonic('P');
        
        helpMenu.setMnemonic('H');
        JMenuItem check = new JMenuItem("DEBUG: Check Dataset");
        check.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent e) {
                DataSetChecker.check();
            }
        });
        current = helpMenu.add(check);
        current = helpMenu.add(help);
        //current.setAccelerator(help.shortCut);
        current = helpMenu.add(about);
        current.setAccelerator(about.shortCut);
        add(helpMenu);
    }
}
