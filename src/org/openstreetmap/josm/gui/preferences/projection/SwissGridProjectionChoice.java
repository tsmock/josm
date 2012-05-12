// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.projection.SwissGrid;
import org.openstreetmap.josm.gui.widgets.HtmlPanel;
import org.openstreetmap.josm.tools.GBC;

public class SwissGridProjectionChoice extends SingleProjectionChoice {

    public SwissGridProjectionChoice() {
        super("core:swissgrid", tr("Swiss Grid (Switzerland)"), new SwissGrid());
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        JPanel p = new JPanel(new GridBagLayout());
        p.add(new HtmlPanel(tr("<i>CH1903 / LV03 (without local corrections)</i>")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        return p;
    }
}
