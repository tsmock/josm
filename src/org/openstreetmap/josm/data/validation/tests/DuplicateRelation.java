// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.command.ChangeMembersCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.AbstractPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.MultiMap;

/**
 * Tests if there are duplicate relations
 */
public class DuplicateRelation extends Test {

    /**
     * Class to store one relation members and information about it
     */
    public static class RelMember {
        /** Role of the relation member */
        private final String role;

        /** Type of the relation member */
        private final OsmPrimitiveType type;

        /** Tags of the relation member */
        private Map<String, String> tags;

        /** Coordinates of the relation member */
        private List<LatLon> coor;

        /** ID of the relation member in case it is a {@link Relation} */
        private long relId;

        @Override
        public int hashCode() {
            return Objects.hash(role, type, tags, coor, relId);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RelMember relMember = (RelMember) obj;
            return relId == relMember.relId &&
                    type == relMember.type &&
                    Objects.equals(role, relMember.role) &&
                    Objects.equals(tags, relMember.tags) &&
                    Objects.equals(coor, relMember.coor);
        }

        /** Extract and store relation information based on the relation member
         * @param src The relation member to store information about
         */
        public RelMember(RelationMember src) {
            role = src.getRole();
            type = src.getType();
            relId = 0;
            coor = new ArrayList<>();

            if (src.isNode()) {
                Node r = src.getNode();
                tags = r.getKeys();
                coor = new ArrayList<>(1);
                coor.add(r.getCoor());
            }
            if (src.isWay()) {
                Way r = src.getWay();
                tags = r.getKeys();
                List<Node> wNodes = r.getNodes();
                coor = new ArrayList<>(wNodes.size());
                for (Node wNode : wNodes) {
                    coor.add(wNode.getCoor());
                }
            }
            if (src.isRelation()) {
                Relation r = src.getRelation();
                tags = r.getKeys();
                relId = r.getId();
                coor = new ArrayList<>();
            }
        }
    }

    /**
     * Class to store relation members
     */
    private static class RelationMembers {
        /** Set of member objects of the relation */
        private final Set<RelMember> members;

        /** Store relation information
         * @param members The list of relation members
         */
        RelationMembers(List<RelationMember> members) {
            this.members = new HashSet<>(members.size());
            for (RelationMember member : members) {
                this.members.add(new RelMember(member));
            }
        }

        @Override
        public int hashCode() {
            return Objects.hash(members);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RelationMembers that = (RelationMembers) obj;
            return Objects.equals(members, that.members);
        }
    }

    /**
     * Class to store relation data (keys are usually cleanup and may not be equal to original relation)
     */
    private static class RelationPair {
        /** Member objects of the relation */
        private final RelationMembers members;
        /** Tags of the relation */
        private final Map<String, String> keys;

        /** Store relation information
         * @param members The list of relation members
         * @param keys The set of tags of the relation
         */
        RelationPair(List<RelationMember> members, Map<String, String> keys) {
            this.members = new RelationMembers(members);
            this.keys = keys;
        }

        @Override
        public int hashCode() {
            return Objects.hash(members, keys);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            RelationPair that = (RelationPair) obj;
            return Objects.equals(members, that.members) &&
                    Objects.equals(keys, that.keys);
        }
    }

    /** Code number of completely duplicated relation error */
    protected static final int DUPLICATE_RELATION = 1901;

    /** Code number of relation with same members error */
    protected static final int SAME_RELATION = 1902;

    /** MultiMap of all relations */
    private MultiMap<RelationPair, OsmPrimitive> relations;

    /** MultiMap of all relations, regardless of keys */
    private MultiMap<List<RelationMember>, OsmPrimitive> relationsNoKeys;

    /** List of keys without useful information */
    private final Set<String> ignoreKeys = new HashSet<>(AbstractPrimitive.getUninterestingKeys());

    /**
     * Default constructor
     */
    public DuplicateRelation() {
        super(tr("Duplicated relations"),
                tr("This test checks that there are no relations with same tags and same members with same roles."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        relations = new MultiMap<>(1000);
        relationsNoKeys = new MultiMap<>(1000);
    }

    @Override
    public void endTest() {
        for (Set<OsmPrimitive> duplicated : relations.values()) {
            if (duplicated.size() > 1) {
                TestError testError = TestError.builder(this, Severity.ERROR, DUPLICATE_RELATION)
                        .message(tr("Duplicated relations"))
                        .primitives(duplicated)
                        .build();
                errors.add(testError);
            }
        }
        relations = null;
        for (Set<OsmPrimitive> duplicated : relationsNoKeys.values()) {
            if (duplicated.size() > 1) {
                TestError testError = TestError.builder(this, Severity.WARNING, SAME_RELATION)
                        .message(tr("Relations with same members"))
                        .primitives(duplicated)
                        .build();
                errors.add(testError);
            }
        }
        relationsNoKeys = null;
        super.endTest();
    }

    @Override
    public void visit(Relation r) {
        if (!r.isUsable() || r.hasIncompleteMembers() || "tmc".equals(r.get("type")) || "TMC".equals(r.get("type"))
                || r.getMembers().isEmpty())
            return;
        List<RelationMember> rMembers = r.getMembers();
        Map<String, String> rkeys = r.getKeys();
        for (String key : ignoreKeys) {
            rkeys.remove(key);
        }
        RelationPair rKey = new RelationPair(rMembers, rkeys);
        relations.put(rKey, r);
        relationsNoKeys.put(rMembers, r);
    }

    /**
     * Fix the error by removing all but one instance of duplicate relations
     * @param testError The error to fix, must be of type {@link #DUPLICATE_RELATION}
     */
    @Override
    public Command fixError(TestError testError) {
        if (!isFixable(testError)) return null;

        Set<Relation> relFix = testError.primitives(Relation.class)
                .filter(r -> !r.isDeleted() || r.getDataSet() == null || r.getDataSet().getPrimitiveById(r) == null)
                .collect(Collectors.toSet());

        if (relFix.size() < 2)
            return null;

        long idToKeep = 0;
        Relation relationToKeep = relFix.iterator().next();
        // Find the relation that is member of one or more relations. (If any)
        Relation relationWithRelations = null;
        Collection<Relation> relRef = null;
        for (Relation r : relFix) {
            Collection<Relation> rel = r.referrers(Relation.class).collect(Collectors.toList());
            if (!rel.isEmpty()) {
                if (relationWithRelations != null)
                    throw new AssertionError("Cannot fix duplicate relations: More than one relation is member of another relation.");
                relationWithRelations = r;
                relRef = rel;
            }
            // Only one relation will be kept - the one with lowest positive ID, if such exist
            // or one "at random" if no such exists. Rest of the relations will be deleted
            if (!r.isNew() && (idToKeep == 0 || r.getId() < idToKeep)) {
                idToKeep = r.getId();
                relationToKeep = r;
            }
        }

        Collection<Command> commands = new LinkedList<>();

        // Fix relations.
        if (relationWithRelations != null && relRef != null && relationToKeep != relationWithRelations) {
            for (Relation rel : relRef) {
                List<RelationMember> members = new ArrayList<>(rel.getMembers());
                for (int i = 0; i < rel.getMembers().size(); ++i) {
                    RelationMember m = rel.getMember(i);
                    if (relationWithRelations.equals(m.getMember())) {
                        members.set(i, new RelationMember(m.getRole(), relationToKeep));
                    }
                }
                commands.add(new ChangeMembersCommand(rel, members));
            }
        }

        // Delete all relations in the list
        relFix.remove(relationToKeep);
        commands.add(new DeleteCommand(relFix));
        return new SequenceCommand(tr("Delete duplicate relations"), commands);
    }

    @Override
    public boolean isFixable(TestError testError) {
        if (!(testError.getTester() instanceof DuplicateRelation)
            || testError.getCode() == SAME_RELATION) return false;

        // We fix it only if there is no more than one relation that is relation member.
        Set<Relation> rels = testError.primitives(Relation.class)
                .collect(Collectors.toSet());

        if (rels.size() < 2)
            return false;

        // count relations with relations
        return rels.stream()
                .filter(x -> x.referrers(Relation.class).anyMatch(y -> true))
                .limit(2)
                .count() <= 1;
    }
}
