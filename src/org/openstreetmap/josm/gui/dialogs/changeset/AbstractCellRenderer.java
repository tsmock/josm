// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.changeset;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Font;
import java.text.DateFormat;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.tools.date.DateUtils;

/**
 * Superclass of changeset cell renderers.
 * @since 7715
 */
public abstract class AbstractCellRenderer extends JLabel implements TableCellRenderer {

    protected void reset(JComponent c) {
        c.setBackground(UIManager.getColor("Table.background"));
        c.setForeground(UIManager.getColor("Table.foreground"));
        c.setFont(UIManager.getFont("Table.font"));
        c.setToolTipText(null);
        c.setOpaque(true);
    }

    protected void reset() {
        reset(this);
    }

    protected void renderColors(JComponent c, boolean isSelected) {
        if (isSelected) {
            c.setBackground(UIManager.getColor("Table.selectionBackground"));
            c.setForeground(UIManager.getColor("Table.selectionForeground"));
        } else {
            c.setBackground(UIManager.getColor("Table.background"));
            c.setForeground(UIManager.getColor("Table.foreground"));
        }
    }

    protected void renderColors(boolean isSelected) {
        renderColors(this, isSelected);
    }

    protected void renderId(long id) {
        setText(Long.toString(id));
        setToolTipText(null);
    }

    protected void renderUser(User user) {
        if (user == null || user.getName().trim().isEmpty()) {
            setFont(UIManager.getFont("Table.font").deriveFont(Font.ITALIC));
            setText(tr("anonymous"));
        } else {
            setFont(UIManager.getFont("Table.font"));
            setText(user.getName());
            setToolTipText(user.getName());
        }
    }

    protected void renderDate(Date d) {
        if (d == null) {
            setText("");
        } else {
            setText(DateUtils.formatDateTime(d, DateFormat.SHORT, DateFormat.SHORT));
        }
        setToolTipText(null);
    }
}
