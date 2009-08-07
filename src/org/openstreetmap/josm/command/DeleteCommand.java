// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.GridBagLayout;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.ConditionalOptionPaneUtil;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.PrimitiveNameFormatter;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * A command to delete a number of primitives from the dataset.
 * @author imi
 */
public class DeleteCommand extends Command {

    /**
     * The primitives that get deleted.
     */
    private final Collection<? extends OsmPrimitive> toDelete;

    /**
     * Constructor for a collection of data
     */
    public DeleteCommand(Collection<? extends OsmPrimitive> data) {
        super();
        this.toDelete = data;
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     */
    public DeleteCommand(OsmPrimitive data) {
        this.toDelete = Collections.singleton(data);
    }

    /**
     * Constructor for a single data item. Use the collection constructor to delete multiple
     * objects.
     *
     * @param layer the layer context for deleting this primitive
     * @param data the primitive to delete
     */
    public DeleteCommand(OsmDataLayer layer, OsmPrimitive data) {
        super(layer);
        this.toDelete = Collections.singleton(data);
    }

    /**
     * Constructor for a collection of data to be deleted in the context of
     * a specific layer
     *
     * @param layer the layer context for deleting these primitives
     * @param data the primitives to delete
     */
    public DeleteCommand(OsmDataLayer layer, Collection<? extends OsmPrimitive> data) {
        super(layer);
        this.toDelete = data;
    }

    @Override
    public boolean executeCommand() {
        super.executeCommand();
        for (OsmPrimitive osm : toDelete) {
            osm.delete(true);
        }
        return true;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        deleted.addAll(toDelete);
    }

    @Override
    public MutableTreeNode description() {
        if (toDelete.size() == 1) {
            OsmPrimitive primitive = toDelete.iterator().next();
            return new DefaultMutableTreeNode(new JLabel(tr("Delete {1} {0}", new PrimitiveNameFormatter()
            .getName(primitive), OsmPrimitiveType.from(primitive).getLocalizedDisplayNameSingular()),
            ImageProvider.get(OsmPrimitiveType.from(primitive)), JLabel.HORIZONTAL));
        }

        String cname = null;
        String apiname = null;
        String cnamem = null;
        for (OsmPrimitive osm : toDelete) {
            if (cname == null) {
                apiname = OsmPrimitiveType.from(osm).getAPIName();
                cname = OsmPrimitiveType.from(osm).getLocalizedDisplayNameSingular();
                cnamem = OsmPrimitiveType.from(osm).getLocalizedDisplayNamePlural();
            } else if (!cname.equals(OsmPrimitiveType.from(osm).getLocalizedDisplayNameSingular())) {
                apiname = "object";
                cname = trn("object", "objects", 1);
                cnamem = trn("object", "objects", 2);
            }
        }
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(new JLabel(tr("Delete {0} {1}", toDelete.size(), trn(
                cname, cnamem, toDelete.size())), ImageProvider.get("data", apiname), JLabel.HORIZONTAL));
        for (OsmPrimitive osm : toDelete) {
            root.add(new DefaultMutableTreeNode(new JLabel(new PrimitiveNameFormatter().getName(osm), ImageProvider
                    .get(OsmPrimitiveType.from(osm)), JLabel.HORIZONTAL)));
        }
        return root;
    }

    /**
     * Delete the primitives and everything they reference.
     *
     * If a node is deleted, the node and all ways and relations the node is part of are deleted as
     * well.
     *
     * If a way is deleted, all relations the way is member of are also deleted.
     *
     * If a way is deleted, only the way and no nodes are deleted.
     *
     * @param selection The list of all object to be deleted.
     * @return command A command to perform the deletions, or null of there is nothing to delete.
     */
    public static Command deleteWithReferences(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data);
        for (OsmPrimitive osm : selection) {
            osm.visit(v);
        }
        v.data.addAll(selection);
        if (v.data.isEmpty())
            return null;
        if (!checkAndConfirmOutlyingDeletes(layer,v.data))
            return null;
        return new DeleteCommand(layer,v.data);
    }

    private static int testRelation(Relation ref, OsmPrimitive osm) {
        PrimitiveNameFormatter formatter = new PrimitiveNameFormatter();
        String role = new String();
        for (RelationMember m : ref.members) {
            if (m.member == osm) {
                role = m.role;
                break;
            }
        }
        if (role.length() > 0)
            return new ExtendedDialog(Main.parent, tr("Conflicting relation"), tr(
                    "Selection \"{0}\" is used by relation \"{1}\" with role {2}.\nDelete from relation?", formatter
                    .getName(osm), formatter.getName(ref), role), new String[] { tr("Delete from relation"),
                tr("Cancel") }, new String[] { "dialogs/delete.png", "cancel.png" }).getValue();
        else
            return new ExtendedDialog(Main.parent, tr("Conflicting relation"), tr(
                    "Selection \"{0}\" is used by relation \"{1}\".\nDelete from relation?", formatter.getName(osm),
                    formatter.getName(ref)), new String[] { tr("Delete from relation"), tr("Cancel") }, new String[] {
                "dialogs/delete.png", "cancel.png" }).getValue();
    }

    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection) {
        return delete(layer, selection, true);
    }

    /**
     * Replies the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too. A node can be deleted if
     * <ul>
     *    <li>it is untagged (see {@see Node#isTagged()}</li>
     *    <li>it is not referred to by other primitives outside of  <code>primitivesToDelete</code></li>
     * <ul>
     * @param layer  the layer in whose context primitives are deleted
     * @param primitivesToDelete  the primitives to delete
     * @return the collection of nodes referred to by primitives in <code>primitivesToDelete</code> which
     * can be deleted too
     */
    protected static Collection<Node> computeNodesToDelete(OsmDataLayer layer, Collection<OsmPrimitive> primitivesToDelete) {
        Collection<Node> nodesToDelete = new HashSet<Node>();
        for (OsmPrimitive osm : primitivesToDelete) {
            if (! (osm instanceof Way) ) {
                continue;
            }
            for (Node n : ((Way) osm).getNodes()) {
                if (n.isTagged()) {
                    continue;
                }
                CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data, false);
                n.visit(v);
                v.data.removeAll(primitivesToDelete);
                if (v.data.isEmpty()) {
                    nodesToDelete.add(n);
                }
            }
        }
        return nodesToDelete;
    }

    /**
     * Try to delete all given primitives.
     *
     * If a node is used by a way, it's removed from that way. If a node or a way is used by a
     * relation, inform the user and do not delete.
     *
     * If this would cause ways with less than 2 nodes to be created, delete these ways instead. If
     * they are part of a relation, inform the user and do not delete.
     *
     * @param layer the {@see OsmDataLayer} in whose context a primitive the primitives are deleted
     * @param selection The objects to delete.
     * @param alsoDeleteNodesInWay <code>true</code> if nodes should be deleted as well
     * @return command a command to perform the deletions, or null if there is nothing to delete.
     */
    public static Command delete(OsmDataLayer layer, Collection<? extends OsmPrimitive> selection, boolean alsoDeleteNodesInWay) {
        if (selection.isEmpty())
            return null;

        Collection<OsmPrimitive> primitivesToDelete = new HashSet<OsmPrimitive>(selection);
        Collection<Way> waysToBeChanged = new HashSet<Way>();
        HashMap<OsmPrimitive, Collection<OsmPrimitive>> relationsToBeChanged = new HashMap<OsmPrimitive, Collection<OsmPrimitive>>();

        if (alsoDeleteNodesInWay) {
            // delete untagged nodes only referenced by primitives in primitivesToDelete,
            // too
            Collection<Node> nodesToDelete = computeNodesToDelete(layer, primitivesToDelete);
            primitivesToDelete.addAll(nodesToDelete);
        }

        if (!checkAndConfirmOutlyingDeletes(layer,primitivesToDelete))
            return null;

        for (OsmPrimitive osm : primitivesToDelete) {
            CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data, false);
            osm.visit(v);
            for (OsmPrimitive ref : v.data) {
                if (primitivesToDelete.contains(ref)) {
                    continue;
                }
                if (ref instanceof Way) {
                    waysToBeChanged.add((Way) ref);
                } else if (ref instanceof Relation) {
                    if (testRelation((Relation) ref, osm) == 1) {
                        Collection<OsmPrimitive> relset = relationsToBeChanged.get(ref);
                        if (relset == null) {
                            relset = new HashSet<OsmPrimitive>();
                        }
                        relset.add(osm);
                        relationsToBeChanged.put(ref, relset);
                    } else
                        return null;
                } else
                    return null;
            }
        }

        Collection<Command> cmds = new LinkedList<Command>();
        for (Way w : waysToBeChanged) {
            Way wnew = new Way(w);
            wnew.removeNodes(primitivesToDelete);
            if (wnew.getNodesCount() < 2) {
                primitivesToDelete.add(w);

                CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(layer.data, false);
                w.visit(v);
                for (OsmPrimitive ref : v.data) {
                    if (primitivesToDelete.contains(ref)) {
                        continue;
                    }
                    if (ref instanceof Relation) {
                        Boolean found = false;
                        Collection<OsmPrimitive> relset = relationsToBeChanged.get(ref);
                        if (relset == null) {
                            relset = new HashSet<OsmPrimitive>();
                        } else {
                            for (OsmPrimitive m : relset) {
                                if (m == w) {
                                    found = true;
                                    break;
                                }
                            }
                        }
                        if (!found) {
                            if (testRelation((Relation) ref, w) == 1) {
                                relset.add(w);
                                relationsToBeChanged.put(ref, relset);
                            } else
                                return null;
                        }
                    } else
                        return null;
                }
            } else {
                cmds.add(new ChangeCommand(w, wnew));
            }
        }

        Iterator<OsmPrimitive> iterator = relationsToBeChanged.keySet().iterator();
        while (iterator.hasNext()) {
            Relation cur = (Relation) iterator.next();
            Relation rel = new Relation(cur);
            for (OsmPrimitive osm : relationsToBeChanged.get(cur)) {
                rel.removeMembersFor(osm);
            }
            cmds.add(new ChangeCommand(cur, rel));
        }

        // #2707: ways to be deleted can include new nodes (with node.id == 0).
        // Remove them from the way before the way is deleted. Otherwise the
        // deleted way is saved (or sent to the API) with a dangling reference to a node
        // Example:
        // <node id='2' action='delete' visible='true' version='1' ... />
        // <node id='1' action='delete' visible='true' version='1' ... />
        // <!-- missing node with id -1 because new deleted nodes are not persisted -->
        // <way id='3' action='delete' visible='true' version='1'>
        // <nd ref='1' />
        // <nd ref='-1' /> <!-- heres the problem -->
        // <nd ref='2' />
        // </way>
        for (OsmPrimitive primitive : primitivesToDelete) {
            if (!(primitive instanceof Way)) {
                continue;
            }
            Way w = (Way) primitive;
            if (w.id == 0) { // new ways with id == 0 are fine,
                continue; // process existing ways only
            }
            Way wnew = new Way(w);
            List<Node> nodesToKeep = new ArrayList<Node>();
            // lookup new nodes which have been added to the set of deleted
            // nodes ...
            for (Node n : wnew.getNodes()) {
                if (n.id != 0 || !primitivesToDelete.contains(n)) {
                    nodesToKeep.add(n);
                }
            }
            // .. and remove them from the way
            //
            wnew.setNodes(nodesToKeep);
            if (nodesToKeep.size() < w.getNodesCount()) {
                cmds.add(new ChangeCommand(w, wnew));
            }
        }

        if (!primitivesToDelete.isEmpty()) {
            cmds.add(new DeleteCommand(layer,primitivesToDelete));
        }

        return new SequenceCommand(tr("Delete"), cmds);
    }

    public static Command deleteWaySegment(OsmDataLayer layer, WaySegment ws) {
        List<Node> n1 = new ArrayList<Node>(), n2 = new ArrayList<Node>();

        n1.addAll(ws.way.getNodes().subList(0, ws.lowerIndex + 1));
        n2.addAll(ws.way.getNodes().subList(ws.lowerIndex + 1, ws.way.getNodesCount()));

        if (n1.size() < 2 && n2.size() < 2)
            return new DeleteCommand(layer, Collections.singleton(ws.way));

        Way wnew = new Way(ws.way);

        if (n1.size() < 2) {
            wnew.setNodes(n2);
            return new ChangeCommand(ws.way, wnew);
        } else if (n2.size() < 2) {
            wnew.setNodes(n1);
            return new ChangeCommand(ws.way, wnew);
        } else {
            Collection<Command> cmds = new LinkedList<Command>();

            wnew.setNodes(n1);
            cmds.add(new ChangeCommand(ws.way, wnew));

            Way wnew2 = new Way();
            wnew2.setKeys(wnew.getKeys());
            wnew2.setNodes(n2);
            cmds.add(new AddCommand(wnew2));

            return new SequenceCommand(tr("Split way segment"), cmds);
        }
    }

    /**
     * Check whether user is about to delete data outside of the download area. Request confirmation
     * if he is.
     *
     * @param layer the layer in whose context data is deleted
     * @param primitivesToDelete the primitives to delete
     * @return true, if deleting outlying primitives is OK; false, otherwise
     */
    private static boolean checkAndConfirmOutlyingDeletes(OsmDataLayer layer, Collection<OsmPrimitive> primitivesToDelete) {
        Area a = layer.data.getDataSourceArea();
        if (a != null) {
            for (OsmPrimitive osm : primitivesToDelete) {
                if (osm instanceof Node && osm.id != 0) {
                    Node n = (Node) osm;
                    if (!a.contains(n.getCoor())) {
                        JPanel msg = new JPanel(new GridBagLayout());
                        msg.add(new JLabel(
                                "<html>" +
                                // leave message in one tr() as there is a grammatical
                                // connection.
                                tr("You are about to delete nodes outside of the area you have downloaded."
                                        + "<br>"
                                        + "This can cause problems because other objects (that you don't see) might use them."
                                        + "<br>" + "Do you really want to delete?") + "</html>"));
                        return ConditionalOptionPaneUtil.showConfirmationDialog(
                                "delete_outside_nodes",
                                Main.parent,
                                msg,
                                tr("Delete confirmation"),
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE,
                                JOptionPane.YES_OPTION
                        );
                    }
                }
            }
        }
        return true;
    }
}
