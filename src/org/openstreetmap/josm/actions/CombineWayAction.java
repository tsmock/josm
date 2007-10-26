// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.HashSet;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodePair;
import org.openstreetmap.josm.tools.GBC;

/**
 * Combines multiple ways into one.
 * 
 * @author Imi
 */
public class CombineWayAction extends JosmAction implements SelectionChangedListener {

    public CombineWayAction() {
        super(tr("Combine Way"), "combineway", tr("Combine several ways into one."), KeyEvent.VK_C, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
        DataSet.selListeners.add(this);
    }

    private static class RelationRolePair {
        public Relation rel;
        public String role;

        public RelationRolePair(Relation rel, String role) {
            this.rel = rel;
            this.role = role;
        }

        @Override public boolean equals(Object o) {
            return o instanceof RelationRolePair
                && rel == ((RelationRolePair) o).rel
                && role.equals(((RelationRolePair) o).role);
        }

        @Override public int hashCode() {
            return rel.hashCode() ^ role.hashCode();
        }
    }

    public void actionPerformed(ActionEvent event) {
        Collection<OsmPrimitive> selection = Main.ds.getSelected();
        LinkedList<Way> selectedWays = new LinkedList<Way>();
        
        for (OsmPrimitive osm : selection)
            if (osm instanceof Way)
                selectedWays.add((Way)osm);

        if (selectedWays.size() < 2) {
            JOptionPane.showMessageDialog(Main.parent, tr("Please select at least two ways to combine."));
            return;
        }
        
        // Check whether all ways have identical relationship membership. More 
        // specifically: If one of the selected ways is a member of relation X
        // in role Y, then all selected ways must be members of X in role Y.
        
        // FIXME: In a later revision, we should display some sort of conflict 
        // dialog like we do for tags, to let the user choose which relations
        // should be kept.
        
        // Step 1, iterate over all relations and figure out which of our
        // selected ways are members of a relation.
        HashMap<RelationRolePair, HashSet<Way>> backlinks =
            new HashMap<RelationRolePair, HashSet<Way>>();
        HashSet<Relation> relationsUsingWays = new HashSet<Relation>();
        for (Relation r : Main.ds.relations) {
            if (r.deleted || r.incomplete) continue;
            for (RelationMember rm : r.members) {
                if (rm.member instanceof Way) {
                    for(Way w : selectedWays) {
                        if (rm.member == w) {
                            RelationRolePair pair = new RelationRolePair(r, rm.role);
                            HashSet<Way> waylinks = new HashSet<Way>();
                            if (backlinks.containsKey(pair)) {
                                waylinks = backlinks.get(pair);
                            } else {
                                waylinks = new HashSet<Way>();
                                backlinks.put(pair, waylinks);
                            }
                            waylinks.add(w);

                            // this is just a cache for later use
                            relationsUsingWays.add(r);
                        }
                    }
                }
            }
        }
        
        // Step 2, all values of the backlinks HashMap must now equal the size
        // of the selection.
        for (HashSet<Way> waylinks : backlinks.values()) {
            if (!selectedWays.equals(waylinks)) {
                JOptionPane.showMessageDialog(Main.parent, tr("The selected ways cannot be combined as they have differing relation memberships."));
                return;
            }
        }

        // collect properties for later conflict resolving
        Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
        for (Way w : selectedWays) {
            for (Entry<String,String> e : w.entrySet()) {
                if (!props.containsKey(e.getKey()))
                    props.put(e.getKey(), new TreeSet<String>());
                props.get(e.getKey()).add(e.getValue());
            }
        }

        List<Node> nodeList = null;
        Object firstTry = actuallyCombineWays(selectedWays, false);
        if (firstTry instanceof List) {
            nodeList = (List<Node>) firstTry;
        } else {
            Object secondTry = actuallyCombineWays(selectedWays, true);
            if (secondTry instanceof List) {
                int option = JOptionPane.showConfirmDialog(Main.parent,
                    tr("The ways can not be combined in their current directions.  "
                    + "Do you want to reverse some of them?"), tr("Change directions?"),
                    JOptionPane.YES_NO_OPTION);
                if (option != JOptionPane.YES_OPTION) {
                    return;
                }
                nodeList = (List<Node>) secondTry;
            } else {
                JOptionPane.showMessageDialog(Main.parent, (String) secondTry);
                return;
            }
        }

        Way newWay = new Way(selectedWays.get(0));
        newWay.nodes.clear();
        newWay.nodes.addAll(nodeList);
        
        // display conflict dialog
        Map<String, JComboBox> components = new HashMap<String, JComboBox>();
        JPanel p = new JPanel(new GridBagLayout());
        for (Entry<String, Set<String>> e : props.entrySet()) {
            if (e.getValue().size() > 1) {
                JComboBox c = new JComboBox(e.getValue().toArray());
                c.setEditable(true);
                p.add(new JLabel(e.getKey()), GBC.std());
                p.add(Box.createHorizontalStrut(10), GBC.std());
                p.add(c, GBC.eol());
                components.put(e.getKey(), c);
            } else
                newWay.put(e.getKey(), e.getValue().iterator().next());
        }
        
        if (!components.isEmpty()) {
            int answer = JOptionPane.showConfirmDialog(Main.parent, p, tr("Enter values for all conflicts."), JOptionPane.OK_CANCEL_OPTION);
            if (answer != JOptionPane.OK_OPTION)
                return;
            for (Entry<String, JComboBox> e : components.entrySet())
                newWay.put(e.getKey(), e.getValue().getEditor().getItem().toString());
        }

        LinkedList<Command> cmds = new LinkedList<Command>();
        cmds.add(new DeleteCommand(selectedWays.subList(1, selectedWays.size())));
        cmds.add(new ChangeCommand(selectedWays.peek(), newWay));
        
        // modify all relations containing the now-deleted ways
        for (Relation r : relationsUsingWays) {
            Relation newRel = new Relation(r);
            newRel.members.clear();
            for (RelationMember rm : r.members) {
                // only copy member if it is either the first of all the selected
                // ways (indexOf==0) or not one if the selected ways (indexOf==-1)
                if (selectedWays.indexOf(rm.member) < 1) {
                    newRel.members.add(new RelationMember(rm));
                }
            }
            cmds.add(new ChangeCommand(r, newRel));
        }
        Main.main.undoRedo.add(new SequenceCommand(tr("Combine {0} ways", selectedWays.size()), cmds));
        Main.ds.setSelected(selectedWays.peek());
    }

    /**
     * @return a message if combining failed, else a list of nodes.
     */
    private Object actuallyCombineWays(List<Way> ways, boolean ignoreDirection) {
        // Battle plan:
        //  1. Split the ways into small chunks of 2 nodes and weed out
        //	   duplicates.
        //  2. Take a chunk and see if others could be appended or prepended,
        //	   if so, do it and remove it from the list of remaining chunks.
        //	   Rather, rinse, repeat.
        //  3. If this algorithm does not produce a single way,
        //     complain to the user.
        //  4. Profit!
        
        HashSet<NodePair> chunkSet = new HashSet<NodePair>();
        for (Way w : ways) {
            if (w.nodes.size() == 0) continue;
            Node lastN = null;
            for (Node n : w.nodes) {
                if (lastN == null) {
                    lastN = n;
                    continue;
                }

                NodePair np = new NodePair(lastN, n);
                if (ignoreDirection) {
                    np.sort();
                }
                chunkSet.add(np);

                lastN = n;
            }
        }
        LinkedList<NodePair> chunks = new LinkedList<NodePair>(chunkSet);

        if (chunks.isEmpty()) {
            return tr("All the ways were empty");
        }

        List<Node> nodeList = chunks.poll().toArrayList();
        while (!chunks.isEmpty()) {
            ListIterator<NodePair> it = chunks.listIterator();
            boolean foundChunk = false;
            while (it.hasNext()) {
                NodePair curChunk = it.next();
                if (curChunk.a == nodeList.get(nodeList.size() - 1)) { // append
                    nodeList.add(curChunk.b);
                } else if (curChunk.b == nodeList.get(0)) { // prepend
                    nodeList.add(0, curChunk.a);
                } else if (ignoreDirection && curChunk.b == nodeList.get(nodeList.size() - 1)) { // append
                    nodeList.add(curChunk.a);
                } else if (ignoreDirection && curChunk.a == nodeList.get(0)) { // prepend
                    nodeList.add(0, curChunk.b);
                } else {
                    continue;
                }

                foundChunk = true;
                it.remove();
                break;
            }
            if (!foundChunk) break;
        }

        if (!chunks.isEmpty()) {
            return tr("Could not combine ways "
                + "(They could not be merged into a single string of nodes)");
        } 
        
        return nodeList;
    }

    /**
     * Enable the "Combine way" menu option if more then one way is selected
     */
    public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
        boolean first = false;
        for (OsmPrimitive osm : newSelection) {
            if (osm instanceof Way) {
                if (first) {
                    setEnabled(true);
                    return;
                }
                first = true;
            }
        }
        setEnabled(false);
    }
}
