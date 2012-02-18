// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

public final class CopyAction extends JosmAction {

    public CopyAction() {
        super(tr("Copy"), "copy",
                tr("Copy selected objects to paste buffer."),
                Shortcut.registerShortcut("system:copy", tr("Edit: {0}", tr("Copy")), KeyEvent.VK_C, Shortcut.CTRL), true);
        putValue("help", ht("/Action/Copy"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if(isEmptySelection()) return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();

        copy(getEditLayer(), selection);
    }

    public static void copy(OsmDataLayer source, Collection<OsmPrimitive> primitives) {
        /* copy ids to the clipboard */
        StringBuilder idsBuilder = new StringBuilder();
        for (OsmPrimitive p : primitives) {
            idsBuilder.append(p.getId()).append(",");
        }
        String ids = idsBuilder.substring(0, idsBuilder.length() - 1);
        Utils.copyToClipboard(ids);

        Main.pasteBuffer.makeCopy(primitives);
        Main.pasteSource = source;
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }

    private boolean isEmptySelection() {
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        if (sel.isEmpty()) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select something to copy."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return true;
        }
        return false;
    }
}
