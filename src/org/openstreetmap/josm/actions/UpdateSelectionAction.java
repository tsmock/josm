// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.MergeVisitor;
import org.openstreetmap.josm.gui.ExceptionDialogUtil;
import org.openstreetmap.josm.gui.OptionPaneUtil;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.MultiFetchServerObjectReader;
import org.openstreetmap.josm.io.OsmTransferException;
import org.openstreetmap.josm.tools.Shortcut;
import org.xml.sax.SAXException;

/**
 * This action synchronizes a set of primitives with their state on the server.
 *
 */
public class UpdateSelectionAction extends JosmAction {

    /**
     * handle an exception thrown because a primitive was deleted on the server
     *
     * @param id the primitive id
     */
    protected void handlePrimitiveGoneException(long id) {
        MultiFetchServerObjectReader reader = new MultiFetchServerObjectReader();
        reader.append(getCurrentDataSet(),id);
        DataSet ds = null;
        try {
            ds = reader.parseOsm(NullProgressMonitor.INSTANCE);
        } catch(Exception e) {
            ExceptionDialogUtil.explainException(e);
        }
        Main.map.mapView.getEditLayer().mergeFrom(ds);
    }

    /**
     * Updates the data for for the {@see OsmPrimitive}s in <code>selection</code>
     * with the data currently kept on the server.
     *
     * @param selection a collection of {@see OsmPrimitive}s to update
     *
     */
    public void updatePrimitives(final Collection<OsmPrimitive> selection) {
        UpdatePrimitivesTask task = new UpdatePrimitivesTask(selection);
        Main.worker.submit(task);
    }

    /**
     * Updates the data for for the {@see OsmPrimitive}s with id <code>id</code>
     * with the data currently kept on the server.
     *
     * @param id  the id of a primitive in the {@see DataSet} of the current edit layser
     * @exception IllegalStateException thrown if there is no primitive with <code>id</code> in
     *   the current dataset
     * @exception IllegalStateException thrown if there is no current dataset
     * 
     */
    public void updatePrimitive(long id) throws IllegalStateException{
        if (getEditLayer() == null)
            throw new IllegalStateException(tr("No current dataset found"));
        OsmPrimitive primitive = getEditLayer().data.getPrimitiveById(id);
        if (primitive == null)
            throw new IllegalStateException(tr("Didn't find a primitive with id {0} in the current dataset", id));
        updatePrimitives(Collections.singleton(primitive));
    }

    /**
     * constructor
     */
    public UpdateSelectionAction() {
        super(tr("Update Selection"),
                "updateselection",
                tr("Updates the currently selected primitives from the server"),
                Shortcut.registerShortcut("file:updateselection",
                        tr("Update Selection"),
                        KeyEvent.VK_U,
                        Shortcut.GROUP_HOTKEY + Shortcut.GROUPS_ALT2),
                        true);
    }

    /**
     * Refreshes the enabled state
     *
     */
    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null
                && ! getCurrentDataSet().getSelected().isEmpty()
        );
    }

    /**
     * action handler
     */
    public void actionPerformed(ActionEvent e) {
        if (! isEnabled())
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        if (selection.size() == 0) {
            OptionPaneUtil.showMessageDialog(
                    Main.parent,
                    tr("There are no selected primitives to update."),
                    tr("Selection empty"),
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        updatePrimitives(selection);
    }

    /**
     * The asynchronous task for updating the data using multi fetch.
     *
     */
    class UpdatePrimitivesTask extends PleaseWaitRunnable {
        private DataSet ds;
        private boolean canceled;
        private Exception lastException;
        private Collection<? extends OsmPrimitive> toUpdate;
        private MultiFetchServerObjectReader reader;

        public UpdatePrimitivesTask(Collection<? extends OsmPrimitive> toUpdate) {
            super("Update primitives", false /* don't ignore exception*/);
            canceled = false;
            this.toUpdate = toUpdate;
        }

        @Override
        protected void cancel() {
            canceled = true;
            if (reader != null) {
                reader.cancel();
            }
        }

        @Override
        protected void finish() {
            if (canceled)
                return;
            if (lastException != null) {
                ExceptionDialogUtil.explainException(lastException);
                return;
            }
            if (ds != null) {
                Main.map.mapView.getEditLayer().mergeFrom(ds);
            }
        }

        protected void initMultiFetchReaderWithNodes(MultiFetchServerObjectReader reader) {
            for (OsmPrimitive primitive : toUpdate) {
                if (primitive instanceof Node && primitive.id > 0) {
                    reader.append((Node)primitive);
                } else if (primitive instanceof Way) {
                    Way way = (Way)primitive;
                    for (Node node: way.nodes) {
                        if (node.id > 0) {
                            reader.append(node);
                        }
                    }
                }
            }
        }

        protected void initMultiFetchReaderWithWays(MultiFetchServerObjectReader reader) {
            for (OsmPrimitive primitive : toUpdate) {
                if (primitive instanceof Way && primitive.id > 0) {
                    reader.append((Way)primitive);
                }
            }
        }

        protected void initMultiFetchReaderWithRelations(MultiFetchServerObjectReader reader) {
            for (OsmPrimitive primitive : toUpdate) {
                if (primitive instanceof Relation && primitive.id > 0) {
                    reader.append((Relation)primitive);
                }
            }
        }

        @Override
        protected void realRun() throws SAXException, IOException, OsmTransferException {
            progressMonitor.indeterminateSubTask("");
            this.ds = new DataSet();
            DataSet theirDataSet;
            try {
                reader = new MultiFetchServerObjectReader();
                initMultiFetchReaderWithNodes(reader);
                initMultiFetchReaderWithWays(reader);
                initMultiFetchReaderWithRelations(reader);
                theirDataSet = reader.parseOsm(progressMonitor.createSubTaskMonitor(ProgressMonitor.ALL_TICKS, false));
                MergeVisitor merger = new MergeVisitor(ds, theirDataSet);
                merger.merge();
            } catch(Exception e) {
                if (canceled)
                    return;
                lastException = e;
            }
        }
    }
}
