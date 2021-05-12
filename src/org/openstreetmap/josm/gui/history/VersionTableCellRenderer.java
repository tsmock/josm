// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

/**
 * Renderer for version labels.
 */
public class VersionTableCellRenderer extends JLabel implements TableCellRenderer {

    /**
     * Constructs a new {@code VersionCellRenderer}.
     */
    public VersionTableCellRenderer() {
        setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        String v = "";
        if (value != null) {
            v = value.toString();
        }
        setText(v);
        return this;
    }
}
