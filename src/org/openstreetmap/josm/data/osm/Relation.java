package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.CopyList;

/**
 * An relation, having a set of tags and any number (0...n) of members.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public final class Relation extends OsmPrimitive {

    /**
     * All members of this relation. Note that after changing this,
     * makeBackReferences and/or removeBackReferences should be called.
     *
     */
    private final List<RelationMember> members = new ArrayList<RelationMember>();

    /**
     * @return Members of the relation. Changes made in returned list are not mapped
     * back to the primitive, use setMembers() to modify the members
     * @since 1925
     */
    public List<RelationMember> getMembers() {
        return new CopyList<RelationMember>(members.toArray(new RelationMember[members.size()]));
    }

    /**
     *
     * @param members Can be null, in that case all members are removed
     * @since 1925
     */
    public void setMembers(List<RelationMember> members) {
        this.members.clear();
        if (members != null) {
            this.members.addAll(members);
        }
    }

    /**
     *
     * @since 1926
     */
    public int getMembersCount() {
        return members.size();
    }

    /**
     *
     * @param index
     * @return
     * @since 1926
     */
    public RelationMember getMember(int index) {
        return members.get(index);
    }

    /**
     *
     * @param member
     * @since 1951
     */
    public void addMember(RelationMember member) {
        members.add(member);
    }

    /**
     *
     * @param index
     * @param member
     * @since 1951
     */
    public void addMember(int index, RelationMember member) {
        members.add(index, member);
    }

    /**
     * Replace member at position specified by index.
     * @param index
     * @param member
     * @return Member that was at the position
     * @since 1951
     */
    public RelationMember setMember(int index, RelationMember member) {
        return members.set(index, member);
    }

    /**
     * Removes member at specified position.
     * @param index
     * @return Member that was at the position
     * @since 1951
     */
    public RelationMember removeMember(int index) {
        return members.remove(index);
    }

    @Override public void visit(Visitor visitor) {
        visitor.visit(this);
    }

    protected Relation(long id, boolean allowNegative) {
        super(id, allowNegative);
    }

    /**
     * Create a new relation with id 0
     */
    public Relation() {
        super(0, false);
    }

    /**
     * Create an identical clone of the argument (including the id)
     */
    public Relation(Relation clone) {
        super(clone.getUniqueId(), true);
        cloneFrom(clone);
    }

    /**
     * Creates a new relation for the given id. If the id > 0, the way is marked
     * as incomplete.
     *
     * @param id the id. > 0 required
     * @throws IllegalArgumentException thrown if id < 0
     */
    public Relation(long id) throws IllegalArgumentException {
        super(id, false);
    }

    @Override public void cloneFrom(OsmPrimitive osm) {
        super.cloneFrom(osm);
        members.clear();
        // we must not add the members themselves, but instead
        // add clones of the members
        for (RelationMember em : ((Relation)osm).getMembers()) {
            members.add(new RelationMember(em));
        }
    }

    @Override public void load(PrimitiveData data) {
        super.load(data);

        RelationData relationData = (RelationData) data;

        List<RelationMember> newMembers = new ArrayList<RelationMember>();
        for (RelationMemberData member : relationData.getMembers()) {
            OsmPrimitive primitive = getDataSet().getPrimitiveById(member);
            if (primitive == null)
                throw new AssertionError("Data consistency problem - relation with missing member detected");
            newMembers.add(new RelationMember(member.getRole(), primitive));
        }
        setMembers(newMembers);
    }

    @Override public RelationData save() {
        RelationData data = new RelationData();
        saveCommonAttributes(data);
        for (RelationMember member:getMembers()) {
            data.getMembers().add(new RelationMemberData(member.getRole(), member.getMember()));
        }
        return data;
    }

    @Override public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("{Relation id=");
        result.append(getUniqueId());
        result.append(" version=");
        result.append(getVersion());
        result.append(" [");
        for (RelationMember rm:getMembers()) {
            result.append(OsmPrimitiveType.from(rm.getMember()));
            result.append(" ");
            result.append(rm.getMember().getUniqueId());
            result.append(", ");
        }
        result.delete(result.length()-2, result.length());
        result.append("]");
        result.append("}");
        return result.toString();
    }

    @Override
    public boolean hasEqualSemanticAttributes(OsmPrimitive other) {
        if (other == null || ! (other instanceof Relation) )
            return false;
        if (! super.hasEqualSemanticAttributes(other))
            return false;
        Relation r = (Relation)other;
        return members.equals(r.members);
    }

    public int compareTo(OsmPrimitive o) {
        return o instanceof Relation ? Long.valueOf(getId()).compareTo(o.getId()) : -1;
    }

    // seems to be different from member "incomplete" - FIXME
    public boolean isIncomplete() {
        for (RelationMember m : members)
            if (m.getMember() == null)
                return true;
        return false;
    }

    public RelationMember firstMember() {
        if (incomplete) return null;
        return (members.size() == 0) ? null : members.get(0);
    }
    public RelationMember lastMember() {
        if (incomplete) return null;
        return (members.size() == 0) ? null : members.get(members.size() -1);
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitive the primitive to check for
     */
    public void removeMembersFor(OsmPrimitive primitive) {
        if (primitive == null)
            return;

        ArrayList<RelationMember> todelete = new ArrayList<RelationMember>();
        for (RelationMember member: members) {
            if (member.getMember() == primitive) {
                todelete.add(member);
            }
        }
        members.removeAll(todelete);
    }

    /**
     * removes all members with member.member == primitive
     *
     * @param primitives the primitives to check for
     */
    public void removeMembersFor(Collection<OsmPrimitive> primitives) {
        if (primitives == null || primitives.isEmpty())
            return;

        ArrayList<RelationMember> todelete = new ArrayList<RelationMember>();
        for (RelationMember member: members) {
            if (primitives.contains(member.getMember())) {
                todelete.add(member);
            }
        }
        members.removeAll(todelete);
    }

    @Override
    public String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }

    /**
     * Replies the set of  {@see OsmPrimitive}s referred to by at least one
     * member of this relation
     *
     * @return the set of  {@see OsmPrimitive}s referred to by at least one
     * member of this relation
     */
    public Set<OsmPrimitive> getMemberPrimitives() {
        HashSet<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (RelationMember m: members) {
            if (m.getMember() != null) {
                ret.add(m.getMember());
            }
        }
        return ret;
    }

    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.RELATION;
    }
}
