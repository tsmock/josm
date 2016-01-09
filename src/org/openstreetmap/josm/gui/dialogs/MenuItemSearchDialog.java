// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.JosmAction;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.widgets.SearchTextResultListPanel;
import org.openstreetmap.josm.tools.Shortcut;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import static org.openstreetmap.josm.tools.I18n.tr;

public class MenuItemSearchDialog extends ExtendedDialog {

    private final Selector selector;
    private static final MenuItemSearchDialog INSTANCE = new MenuItemSearchDialog(Main.main.menu);

    private MenuItemSearchDialog(MainMenu menu) {
        super(Main.parent, tr("Search menu items"), new String[]{tr("Select"), tr("Cancel")});
        this.selector = new Selector(menu);
        this.selector.setDblClickListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonAction(0, null);
            }
        });
        setContent(selector);
        setPreferredSize(new Dimension(600, 300));
    }

    /**
     * Returns the unique instance of {@code MenuItemSearchDialog}.
     *
     * @return the unique instance of {@code MenuItemSearchDialog}.
     */
    public static synchronized MenuItemSearchDialog getInstance() {
        return INSTANCE;
    }

    @Override
    public ExtendedDialog showDialog() {
        selector.init();
        super.showDialog();
        selector.clearSelection();
        return this;
    }

    @Override
    protected void buttonAction(int buttonIndex, ActionEvent evt) {
        super.buttonAction(buttonIndex, evt);
        if (buttonIndex == 0 && selector.getSelectedItem() != null && selector.getSelectedItem().isEnabled()) {
            selector.getSelectedItem().getAction().actionPerformed(evt);
        }
    }

    private static class Selector extends SearchTextResultListPanel<JMenuItem> {

        private final MainMenu menu;

        public Selector(MainMenu menu) {
            super();
            this.menu = menu;
            lsResult.setCellRenderer(new CellRenderer());
            lsResult.setSelectionModel(new DefaultListSelectionModel() {

            });
        }

        public JMenuItem getSelectedItem() {
            final JMenuItem selected = lsResult.getSelectedValue();
            if (selected != null) {
                return selected;
            } else if (!lsResultModel.isEmpty()) {
                return lsResultModel.getElementAt(0);
            } else {
                return null;
            }
        }

        @Override
        protected void filterItems() {
            lsResultModel.setItems(menu.findMenuItems(edSearchText.getText(), true));
        }
    }

    private static class CellRenderer implements ListCellRenderer<JMenuItem> {

        private final DefaultListCellRenderer def = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList<? extends JMenuItem> list, JMenuItem value, int index, boolean isSelected, boolean cellHasFocus) {
            final JLabel label = (JLabel) def.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(value.getText());
            label.setIcon(value.getIcon());
            label.setEnabled(value.isEnabled());
            final JMenuItem item = new JMenuItem(value.getText());
            item.setAction(value.getAction());
            if (isSelected) {
                item.setBackground(list.getSelectionBackground());
                item.setForeground(list.getSelectionForeground());
            } else {
                item.setBackground(list.getBackground());
                item.setForeground(list.getForeground());
            }
            return item;
        }
    }

    public static class Action extends JosmAction {

        public Action() {
            super(tr("Search menu items"), "dialogs/search", null,
                    Shortcut.registerShortcut("help:search-items", "Search menu items", KeyEvent.VK_SPACE, Shortcut.CTRL), false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            MenuItemSearchDialog.getInstance().showDialog();
        }
    }
}
