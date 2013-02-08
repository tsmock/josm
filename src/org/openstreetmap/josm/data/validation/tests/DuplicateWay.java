// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Tests if there are duplicate ways
 */
public class DuplicateWay extends Test
{

    /**
      * Class to store a way reduced to coordinates and keys. Essentially this is used to call the
      * <code>equals{}</code> function.
      */
    private static class WayPair {
        public List<LatLon> coor;
        public Map<String, String> keys;
        public WayPair(List<LatLon> _coor, Map<String, String> _keys) {
            coor=_coor;
            keys=_keys;
        }

        @Override
        public int hashCode() {
            return coor.hashCode() + keys.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof WayPair))
                return false;
            WayPair wp = (WayPair) obj;
            return wp.coor.equals(coor) && wp.keys.equals(keys);
        }
    }

    /**
      * Class to store a way reduced to coordinates. Essentially this is used to call the
      * <code>equals{}</code> function.
      */
    private static class WayPairNoTags {
        public List<LatLon> coor;
        public WayPairNoTags(List<LatLon> _coor) {
            coor=_coor;
        }
        @Override
        public int hashCode() {
            return coor.hashCode();
        }
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof WayPairNoTags)) return false;
            WayPairNoTags wp = (WayPairNoTags) obj;
            return wp.coor.equals(coor);
        }
    }

    /** Test identification for exactly identical ways (coordinates and tags). */
    protected static final int DUPLICATE_WAY = 1401;
    /** Test identification for identical ways (coordinates only). */
    protected static final int SAME_WAY = 1402;

    /** Bag of all ways */
    private MultiMap<WayPair, OsmPrimitive> ways;

    /** Bag of all ways, regardless of tags */
    private MultiMap<WayPairNoTags, OsmPrimitive> waysNoTags;
    
    /** Set of known hashcodes for list of coordinates **/
    private Set<Integer> knownHashCodes;

    /**
     * Constructor
     */
    public DuplicateWay() {
        super(tr("Duplicated ways"),
                tr("This test checks that there are no ways with same node coordinates and optionally also same tags."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new MultiMap<WayPair, OsmPrimitive>(1000);
        waysNoTags = new MultiMap<WayPairNoTags, OsmPrimitive>(1000);
        knownHashCodes = new HashSet<Integer>(1000);
    }

    @Override
    public void endTest() {
        super.endTest();
        for (Set<OsmPrimitive> duplicated : ways.values()) {
            if (duplicated.size() > 1) {
                TestError testError = new TestError(this, Severity.ERROR, tr("Duplicated ways"), DUPLICATE_WAY, duplicated);
                errors.add(testError);
            }
        }

        for(Set<OsmPrimitive> sameway : waysNoTags.values()) {
            if( sameway.size() > 1) {
                //Report error only if at least some tags are different, as otherwise the error was already reported as duplicated ways
                Map<String, String> tags0=null;
                boolean skip=true;

                for(OsmPrimitive o : sameway) {
                    if (tags0==null) {
                        tags0=o.getKeys();
                        removeUninterestingKeys(tags0);
                    } else {
                        Map<String, String> tagsCmp=o.getKeys();
                        removeUninterestingKeys(tagsCmp);
                        if (!tagsCmp.equals(tags0)) {
                            skip=false;
                            break;
                        }
                    }
                }
                if (skip) {
                    continue;
                }
                TestError testError = new TestError(this, Severity.WARNING, tr("Ways with same position"), SAME_WAY, sameway);
                errors.add(testError);
            }
        }
        ways = null;
        waysNoTags = null;
        knownHashCodes = null;
    }

    /**
     * Remove uninteresting discardable keys to normalize the tags
     * @param wkeys The tags of the way, obtained by {@code Way#getKeys}
     */
    public void removeUninterestingKeys(Map<String, String> wkeys) {
        for(String key : OsmPrimitive.getDiscardableKeys()) {
            wkeys.remove(key);
        }
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable())
            return;
        List<Node> wNodes = w.getNodes();                            // The original list of nodes for this way
        List<Node> wNodesToUse = new ArrayList<Node>(wNodes.size()); // The list that will be considered for this test
        if (w.isClosed()) {
            // In case of a closed way, build the list of lat/lon starting from the node with the lowest id
            // to ensure this list will produce the same hashcode as the list obtained from another closed
            // way with the same nodes, in the same order, but that does not start from the same node (fix #8008)
            int lowestIndex = 0;
            long lowestNodeId = wNodes.get(0).getUniqueId();
            for (int i=1; i<wNodes.size(); i++) {
                if (wNodes.get(i).getUniqueId() < lowestNodeId) {
                    lowestNodeId = wNodes.get(i).getUniqueId();
                    lowestIndex = i;
                }
            }
            for (int i=lowestIndex; i<wNodes.size()-1; i++) {
                wNodesToUse.add(wNodes.get(i));
            }
            for (int i=0; i<lowestIndex; i++) {
                wNodesToUse.add(wNodes.get(i));
            }
            wNodesToUse.add(wNodes.get(lowestIndex));
        } else {
            wNodesToUse.addAll(wNodes);
        }
        // Build the list of lat/lon
        List<LatLon> wLat = new ArrayList<LatLon>(wNodesToUse.size());
        for (int i=0; i<wNodesToUse.size(); i++) {
            wLat.add(wNodesToUse.get(i).getCoor());
        }
        // If this way has not direction-dependant keys, make sure the list is ordered the same for all ways (fix #8015)
        if (!w.hasDirectionKeys()) {
            int hash = wLat.hashCode();
            if (!knownHashCodes.contains(hash)) {
                List<LatLon> reversedwLat = new ArrayList<LatLon>(wLat);
                   Collections.reverse(reversedwLat);
                int reverseHash = reversedwLat.hashCode();
                if (!knownHashCodes.contains(reverseHash)) {
                    // Neither hash or reversed hash is known, remember hash
                    knownHashCodes.add(hash);
                } else {
                    // Reversed hash is known, use the reverse list then
                    wLat = reversedwLat;
                }
            }
        }
        Map<String, String> wkeys = w.getKeys();
        removeUninterestingKeys(wkeys);
        WayPair wKey = new WayPair(wLat, wkeys);
        ways.put(wKey, w);
        WayPairNoTags wKeyN = new WayPairNoTags(wLat);
        waysNoTags.put(wKeyN, w);
    }

    /**
     * Fix the error by removing all but one instance of duplicate ways
     */
    @Override
    public Command fixError(TestError testError) {
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Way> ways = new HashSet<Way>();

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Way && !osm.isDeleted()) {
                ways.add((Way)osm);
            }
        }

        if (ways.size() < 2)
            return null;

        long idToKeep = 0;
        Way wayToKeep = ways.iterator().next();
        // Only one way will be kept - the one with lowest positive ID, if such exist
        // or one "at random" if no such exists. Rest of the ways will be deleted
        for (Way w: ways) {
            if (!w.isNew()) {
                if (idToKeep == 0 || w.getId() < idToKeep) {
                    idToKeep = w.getId();
                    wayToKeep = w;
                }
            }
        }

        // Find the way that is member of one or more relations. (If any)
        Way wayWithRelations = null;
        List<Relation> relations = null;
        for (Way w : ways) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                if (wayWithRelations != null)
                    throw new AssertionError("Cannot fix duplicate Ways: More than one way is relation member.");
                wayWithRelations = w;
                relations = rel;
            }
        }

        Collection<Command> commands = new LinkedList<Command>();

        // Fix relations.
        if (wayWithRelations != null && wayToKeep != wayWithRelations) {
            for (Relation rel : relations) {
                Relation newRel = new Relation(rel);
                for (int i = 0; i < newRel.getMembers().size(); ++i) {
                    RelationMember m = newRel.getMember(i);
                    if (wayWithRelations.equals(m.getMember())) {
                        newRel.setMember(i, new RelationMember(m.getRole(), wayToKeep));
                    }
                }
                commands.add(new ChangeCommand(rel, newRel));
            }
        }

        //Delete all ways in the list
        //Note: nodes are not deleted, these can be detected and deleted at next pass
        ways.remove(wayToKeep);
        commands.add(new DeleteCommand(ways));
        return new SequenceCommand(tr("Delete duplicate ways"), commands);
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (!(testError.getTester() instanceof DuplicateWay))
            return false;

        //Do not automatically fix same ways with different tags
        if (testError.getCode()!=DUPLICATE_WAY) return false;

        // We fix it only if there is no more than one way that is relation member.
        Collection<? extends OsmPrimitive> sel = testError.getPrimitives();
        HashSet<Way> ways = new HashSet<Way>();

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Way) {
                ways.add((Way)osm);
            }
        }

        if (ways.size() < 2)
            return false;

        int waysWithRelations = 0;
        for (Way w : ways) {
            List<Relation> rel = OsmPrimitive.getFilteredList(w.getReferrers(), Relation.class);
            if (!rel.isEmpty()) {
                ++waysWithRelations;
            }
        }
        return (waysWithRelations <= 1);
    }
}
