// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.nodes;

import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.WayNodesConflictResolverCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.ListMergeModel;

public class NodeListMergeModel extends ListMergeModel<Node>{

    private static final Logger logger = Logger.getLogger(NodeListMergeModel.class.getName());


    /**
     * Populates the model with the nodes in the two {@see Way}s <code>my</code> and
     * <code>their</code>.
     * 
     * @param my  my way (i.e. the way in the local dataset)
     * @param their their way (i.e. the way in the server dataset)
     * @exception IllegalArgumentException thrown, if my is null
     * @exception IllegalArgumentException  thrown, if their is null
     */
    public void populate(Way my, Way their) {
        if (my == null)
            throw new IllegalArgumentException("parameter 'way' must not be null");
        if (their == null)
            throw new IllegalArgumentException("parameter 'their' must not be null");
        mergedEntries.clear();
        myEntries.clear();
        theirEntries.clear();
        for (Node n : my.nodes) {
            myEntries.add(n);
        }
        for (Node n : their.nodes) {
            theirEntries.add(n);
        }
        if (myAndTheirEntriesEqual()) {
            mergedEntries = new ArrayList<Node>(myEntries);
            setFrozen(true);
        } else {
            setFrozen(false);
        }

        fireModelDataChanged();
    }

    /**
     * Builds the command to resolve conflicts in the node list of a way
     * 
     * @param my  my way. Must not be null.
     * @param their  their way. Must not be null
     * @return the command
     * @exception IllegalArgumentException thrown, if my is null or not a {@see Way}
     * @exception IllegalArgumentException thrown, if their is null or not a {@see Way}
     * @exception IllegalStateException thrown, if the merge is not yet frozen
     */
    public WayNodesConflictResolverCommand buildResolveCommand(Way my, Way their) {
        if (my == null)
            throw new IllegalArgumentException("parameter my most not be null");
        if (their == null)
            throw new IllegalArgumentException("parameter my most not be null");
        if (! isFrozen())
            throw new IllegalArgumentException("merged nodes not frozen yet. Can't build resolution command");
        return new WayNodesConflictResolverCommand(my, their, mergedEntries);
    }


    @Override
    public boolean isEqualEntry(Node e1, Node e2) {
        return e1.id == e2.id;
    }

    @Override
    protected void setValueAt(DefaultTableModel model, Object value, int row, int col) {
        // do nothing - node list tables are not editable
    }

    @Override
    protected Node cloneEntry(Node entry) {
        Node n = new Node(entry.id);
        n.cloneFrom(entry);
        return n;
    }
}
