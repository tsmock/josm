// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.ShortCut;

public final class DeleteAction extends JosmAction {

    public DeleteAction() {
        super(tr("Delete"), "dialogs/delete", tr("Delete selected objects."),
        ShortCut.registerShortCut("system:delete", tr("Edit: Delete"), KeyEvent.VK_DELETE, ShortCut.GROUP_DIRECT), true);
        setEnabled(true);
    }

    public void actionPerformed(ActionEvent e) {
        new org.openstreetmap.josm.actions.mapmode.DeleteAction(Main.map)
                .doActionPerformed(e);
    }
}
