//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.BackreferencedDataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.TagCollection;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.BackreferencedDataSet.RelationToChildReference;
import org.openstreetmap.josm.gui.conflict.tags.CombinePrimitiveResolverDialog;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.Shortcut;


/**
 * Merges a collection of nodes into one node.
 * 
 */
public class MergeNodesAction extends JosmAction {

    public MergeNodesAction() {
        super(tr("Merge Nodes"), "mergenodes", tr("Merge nodes into the oldest one."),
                Shortcut.registerShortcut("tools:mergenodes", tr("Tool: {0}", tr("Merge Nodes")), KeyEvent.VK_M, Shortcut.GROUP_EDIT), true);
    }

    public void actionPerformed(ActionEvent event) {
        if (!isEnabled())
            return;
        Collection<OsmPrimitive> selection = getCurrentDataSet().getSelected();
        Set<Node> selectedNodes = OsmPrimitive.getFilteredSet(selection, Node.class);
        if (selectedNodes.size() < 2) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least two nodes to merge."),
                    tr("Warning"),
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }


        Node targetNode = selectTargetNode(selectedNodes);
        Command cmd = mergeNodes(Main.main.getEditLayer(), selectedNodes, targetNode);
        if (cmd != null) {
            Main.main.undoRedo.add(cmd);
            Main.main.getEditLayer().data.setSelected(targetNode);
        }
    }

    protected void completeTagCollectionWithMissingTags(TagCollection tc, Collection<Node> mergedNodes) {
        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set if a tag is not present
            // on all merged nodes
            //
            for (Node n: mergedNodes) {
                if (n.get(key) == null) {
                    tc.add(new Tag(key)); // add a tag with key and empty value
                }
            }
        }
        // remove irrelevant tags
        //
        tc.removeByKey("created_by");
    }

    protected void completeTagCollectionForEditing(TagCollection tc) {
        for (String key: tc.getKeys()) {
            // make sure the empty value is in the tag set such that we can delete the tag
            // in the conflict dialog if necessary
            //
            tc.add(new Tag(key,""));
        }
    }

    /**
     * Selects a node out of a collection of candidate nodes. The selected
     * node will become the target node the remaining nodes are merged to.
     * 
     * @param candidates the collection of candidate nodes
     * @return the selected target node
     */
    public Node selectTargetNode(Collection<Node> candidates) {
        // Find which node to merge into (i.e. which one will be left)
        // - this should be combined from two things:
        //   1. It will be the first node in the list that has a
        //      positive ID number, OR the first node.
        //   2. It will be at the position of the first node in the
        //      list.
        //
        // *However* - there is the problem that the selection list is
        // _not_ in the order that the nodes were clicked on, meaning
        // that the user doesn't know which node will be chosen (so
        // (2) is not implemented yet.)  :-(
        Node targetNode = null;
        for (Node n: candidates) {
            if (n.getId() > 0) {
                targetNode = n;
                break;
            }
        }
        if (targetNode == null) {
            // an arbitrary node
            targetNode = candidates.iterator().next();
        }
        return targetNode;
    }


    /**
     * Merges the nodes in <code>node</code> onto one of the nodes. Uses the dataset
     * managed by <code>layer</code> as reference.
     * 
     * @param layer the reference data layer. Must not be null.
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetNode the target node the collection of nodes is merged to. Must not be null.
     * @throws IllegalArgumentException thrown if layer is null
     * @throws IllegalArgumentException thrown if targetNode is null
     * 
     */
    public Command mergeNodes(OsmDataLayer layer, Collection<Node> nodes, Node targetNode) throws IllegalArgumentException{
        if (layer == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "nodes"));
        if (targetNode == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "targetNode"));

        if (nodes == null)
            return null;
        nodes.remove(null); // just in case
        BackreferencedDataSet backreferences = new BackreferencedDataSet(layer.data);
        backreferences.build();
        return mergeNodes(layer,backreferences, nodes, targetNode);
    }

    /**
     * Merges the nodes in <code>node</code> onto one of the nodes. Uses the dataset
     * managed by <code>layer</code> as reference. <code>backreferences</code> is precomputed
     * collection of all parent/child references in the dataset.
     *
     * @param layer layer the reference data layer. Must not be null.
     * @param backreferences if null, backreferneces are first computed from layer.data; otherwise
     *    backreferences.getSource() == layer.data must hold
     * @param nodes the collection of nodes. Ignored if null.
     * @param targetNode the target node the collection of nodes is merged to. Must not be null.
     * @throw IllegalArgumentException thrown if layer is null
     * @throw IllegalArgumentException thrown if  backreferences.getSource() != layer.data
     */
    public Command mergeNodes(OsmDataLayer layer, BackreferencedDataSet backreferences, Collection<Node> nodes, Node targetNode) {
        if (layer == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "nodes"));
        if (targetNode == null)
            throw new IllegalArgumentException(tr("parameter ''{0}'' must not be null", "targetNode"));
        if (nodes == null)
            return null;
        if (backreferences == null) {
            backreferences = new BackreferencedDataSet(layer.data);
            backreferences.build();
        }

        Set<RelationToChildReference> relationToNodeReferences = backreferences.getRelationToChildReferences(nodes);

        // build the tag collection
        //
        TagCollection nodeTags = TagCollection.unionOfAllPrimitives(nodes);
        completeTagCollectionWithMissingTags(nodeTags, nodes);
        TagCollection nodeTagsToEdit = new TagCollection(nodeTags);
        completeTagCollectionForEditing(nodeTagsToEdit);

        // launch a conflict resolution dialog, if necessary
        //
        CombinePrimitiveResolverDialog dialog = CombinePrimitiveResolverDialog.getInstance();
        dialog.getTagConflictResolverModel().populate(nodeTagsToEdit);
        dialog.getRelationMemberConflictResolverModel().populate(relationToNodeReferences);
        dialog.setTargetPrimitive(targetNode);
        dialog.prepareDefaultDecisions();
        if (! nodeTags.isApplicableToPrimitive() || relationToNodeReferences.size() > 1) {
            dialog.setVisible(true);
            if (dialog.isCancelled())
                return null;
        }
        LinkedList<Command> cmds = new LinkedList<Command>();

        // the nodes we will have to delete
        //
        Collection<OsmPrimitive> nodesToDelete = new HashSet<OsmPrimitive>(nodes);
        nodesToDelete.remove(targetNode);

        // change the ways referring to at least one of the merge nodes
        //
        Collection<Way> waysToDelete= new HashSet<Way>();
        for (Way w : OsmPrimitive.getFilteredList(backreferences.getParents(nodesToDelete), Way.class)) {
            // OK - this way contains one or more nodes to change
            ArrayList<Node> newNodes = new ArrayList<Node>(w.getNodesCount());
            for (Node n: w.getNodes()) {
                if (! nodesToDelete.contains(n)) {
                    newNodes.add(n);
                }
            }
            if (newNodes.size() < 2) {
                if (backreferences.getParents(w).isEmpty()) {
                    waysToDelete.add(w);
                } else {
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            tr("Cannot merge nodes: " +
                            "Would have to delete a way that is still used."),
                            tr("Warning"),
                            JOptionPane.WARNING_MESSAGE
                    );
                    return null;
                }
            } else if(newNodes.size() < 2 && backreferences.getParents(w).isEmpty()) {
                waysToDelete.add(w);
            } else {
                Way newWay = new Way(w);
                newWay.setNodes(newNodes);
                cmds.add(new ChangeCommand(w, newWay));
            }
        }

        // build the commands
        //
        if (!nodesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(nodesToDelete));
        }
        if (!waysToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(waysToDelete));
        }
        cmds.addAll(dialog.buildResolutionCommands());
        Command cmd = new SequenceCommand(tr("Merge {0} nodes", nodes.size()), cmds);
        return cmd;
    }


    /**
     * Enable the "Merge Nodes" menu option if more then one node is selected
     */
    @Override
    public void updateEnabledState() {
        if (getCurrentDataSet() == null || getCurrentDataSet().getSelected().isEmpty()) {
            setEnabled(false);
            return;
        }
        boolean ok = true;
        if (getCurrentDataSet().getSelected().size() < 2) {
            setEnabled(false);
            return;
        }
        for (OsmPrimitive osm : getCurrentDataSet().getSelected()) {
            if (!(osm instanceof Node)) {
                ok = false;
                break;
            }
        }
        setEnabled(ok);
    }
}
