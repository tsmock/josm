// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.corrector.ReverseWayNoTagCorrector;
import org.openstreetmap.josm.actions.corrector.ReverseWayTagCorrector;
import org.openstreetmap.josm.command.ChangeNodesCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.UserCancelException;

/**
 * Reverses the ways that are currently selected by the user
 */
public final class ReverseWayAction extends JosmAction {

    /**
     * The resulting way after reversing it and the commands to get there.
     */
    public static class ReverseWayResult {
        private final Collection<Command> tagCorrectionCommands;
        private final Command reverseCommand;

        /**
         * Create a new {@link ReverseWayResult}
         * @param tagCorrectionCommands The commands to correct the tags
         * @param reverseCommand The command to reverse the way
         */
        public ReverseWayResult(Collection<Command> tagCorrectionCommands, Command reverseCommand) {
            this.tagCorrectionCommands = tagCorrectionCommands;
            this.reverseCommand = reverseCommand;
        }

        /**
         * Gets the commands that will be required to do a full way reversal including changing the tags
         * @return The commands
         */
        public Collection<Command> getCommands() {
            List<Command> c = new ArrayList<>();
            c.addAll(tagCorrectionCommands);
            c.add(reverseCommand);
            return c;
        }

        /**
         * Gets a single sequence command for reversing this way including changing the tags
         * @return the command
         */
        public Command getAsSequenceCommand() {
            return new SequenceCommand(tr("Reverse way"), getCommands());
        }

        /**
         * Gets the basic reverse command that only changes the order of the nodes.
         * @return The reorder nodes command
         */
        public Command getReverseCommand() {
            return reverseCommand;
        }

        /**
         * Gets the command to change the tags of the way
         * @return The command to reverse the tags
         */
        public Collection<Command> getTagCorrectionCommands() {
            return tagCorrectionCommands;
        }
    }

    /**
     * Creates a new {@link ReverseWayAction} and binds the shortcut
     */
    public ReverseWayAction() {
        super(tr("Reverse Ways"), "wayflip", tr("Reverse the direction of all selected ways."),
                Shortcut.registerShortcut("tools:reverse", tr("Tools: {0}", tr("Reverse Ways")), KeyEvent.VK_R, Shortcut.DIRECT), true);
        setHelpId(ht("/Action/ReverseWays"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DataSet ds = getLayerManager().getEditDataSet();
        if (!isEnabled() || ds == null)
            return;

        final Collection<Way> sel = new LinkedHashSet<>(ds.getSelectedWays());
        sel.removeIf(w -> w.isIncomplete() || w.isEmpty());
        if (sel.isEmpty()) {
            new Notification(
                    tr("Please select at least one way."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        Collection<Command> c = new LinkedList<>();
        for (Way w : sel) {
            try {
                c.addAll(reverseWay(w).getCommands());
            } catch (IllegalArgumentException ex) {
                Logging.error(ex);
            } catch (UserCancelException ex) {
                Logging.trace(ex);
                return;
            }
        }
        UndoRedoHandler.getInstance().add(new SequenceCommand(tr("Reverse Ways"), c));
    }

    /**
     * Reverses a given way.
     * @param w the way
     * @return the reverse command and the tag correction commands
     * @throws IllegalArgumentException if sanity checks fail
     * @throws UserCancelException if user cancels a reverse warning dialog
     */
    public static ReverseWayResult reverseWay(Way w) throws UserCancelException {
        ReverseWayNoTagCorrector.checkAndConfirmReverseWay(w);
        List<Node> nodesCopy = w.getNodes();
        Collections.reverse(nodesCopy);

        Collection<Command> corrCmds = Collections.<Command>emptyList();
        if (Config.getPref().getBoolean("tag-correction.reverse-way", true)) {
            corrCmds = new ReverseWayTagCorrector().execute(w, w);
        }
        return new ReverseWayResult(corrCmds, new ChangeNodesCommand(w, new ArrayList<>(nodesCopy)));
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection.stream().anyMatch(
                o -> o instanceof Way && !o.isIncomplete() && !o.getDataSet().isLocked()));
    }
}
