// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.table.DefaultTableModel;

import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.RelationToChildReference;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

/**
 * This model manages a list of conflicting relation members.
 *
 * It can be used as {@link javax.swing.table.TableModel}.
 */
public class RelationMemberConflictResolverModel extends DefaultTableModel {
    /** the property name for the number conflicts managed by this model */
    public static final String NUM_CONFLICTS_PROP = RelationMemberConflictResolverModel.class.getName() + ".numConflicts";

    /** the list of conflict decisions */
    protected final transient List<RelationMemberConflictDecision> decisions;
    /** the collection of relations for which we manage conflicts */
    protected transient Collection<Relation> relations;
    /** the collection of primitives for which we manage conflicts */
    protected transient Collection<? extends OsmPrimitive> primitives;
    /** the number of conflicts */
    private int numConflicts;
    private final PropertyChangeSupport support;

    /**
     * Replies true if each {@link MultiValueResolutionDecision} is decided.
     *
     * @return true if each {@link MultiValueResolutionDecision} is decided; false otherwise
     */
    public boolean isResolvedCompletely() {
        return numConflicts == 0;
    }

    /**
     * Replies the current number of conflicts
     *
     * @return the current number of conflicts
     */
    public int getNumConflicts() {
        return numConflicts;
    }

    /**
     * Updates the current number of conflicts from list of decisions and emits
     * a property change event if necessary.
     *
     */
    protected void updateNumConflicts() {
        int count = 0;
        for (RelationMemberConflictDecision decision: decisions) {
            if (!decision.isDecided()) {
                count++;
            }
        }
        int oldValue = numConflicts;
        numConflicts = count;
        if (numConflicts != oldValue) {
            support.firePropertyChange(getProperty(), oldValue, numConflicts);
        }
    }

    protected String getProperty() {
        return NUM_CONFLICTS_PROP;
    }

    public void addPropertyChangeListener(PropertyChangeListener l) {
        support.addPropertyChangeListener(l);
    }

    public void removePropertyChangeListener(PropertyChangeListener l) {
        support.removePropertyChangeListener(l);
    }

    public RelationMemberConflictResolverModel() {
        decisions = new ArrayList<>();
        support = new PropertyChangeSupport(this);
    }

    @Override
    public int getRowCount() {
        return getNumDecisions();
    }

    @Override
    public Object getValueAt(int row, int column) {
        if (decisions == null) return null;

        RelationMemberConflictDecision d = decisions.get(row);
        switch(column) {
        case 0: /* relation */ return d.getRelation();
        case 1: /* pos */ return Integer.toString(d.getPos() + 1); // position in "user space" starting at 1
        case 2: /* role */ return d.getRole();
        case 3: /* original */ return d.getOriginalPrimitive();
        case 4: /* decision */ return d.getDecision();
        }
        return null;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        RelationMemberConflictDecision d = decisions.get(row);
        switch(column) {
        case 2: /* role */
            d.setRole((String) value);
            break;
        case 4: /* decision */
            d.decide((RelationMemberConflictDecisionType) value);
            refresh();
            break;
        }
        fireTableDataChanged();
    }

    /**
     * Populates the model with the members of the relation <code>relation</code>
     * referring to <code>primitive</code>.
     *
     * @param relation the parent relation
     * @param primitive the child primitive
     */
    protected void populate(Relation relation, OsmPrimitive primitive) {
        for (int i = 0; i < relation.getMembersCount(); i++) {
            if (relation.getMember(i).refersTo(primitive)) {
                decisions.add(new RelationMemberConflictDecision(relation, i));
            }
        }
    }

    /**
     * Populates the model with the relation members belonging to one of the relations in <code>relations</code>
     * and referring to one of the primitives in <code>memberPrimitives</code>.
     *
     * @param relations  the parent relations. Empty list assumed if null.
     * @param memberPrimitives the child primitives. Empty list assumed if null.
     */
    public void populate(Collection<Relation> relations, Collection<? extends OsmPrimitive> memberPrimitives) {
        decisions.clear();
        relations = relations == null ? Collections.<Relation>emptyList() : relations;
        memberPrimitives = memberPrimitives == null ? new LinkedList<OsmPrimitive>() : memberPrimitives;
        for (Relation r : relations) {
            for (OsmPrimitive p: memberPrimitives) {
                populate(r, p);
            }
        }
        this.relations = relations;
        this.primitives = memberPrimitives;
        refresh();
    }

    /**
     * Populates the model with the relation members represented as a collection of
     * {@link RelationToChildReference}s.
     *
     * @param references the references. Empty list assumed if null.
     */
    public void populate(Collection<RelationToChildReference> references) {
        references = references == null ? new LinkedList<RelationToChildReference>() : references;
        decisions.clear();
        this.relations = new HashSet<>(references.size());
        final Collection<OsmPrimitive> primitives = new HashSet<>();
        for (RelationToChildReference reference: references) {
            decisions.add(new RelationMemberConflictDecision(reference.getParent(), reference.getPosition()));
            relations.add(reference.getParent());
            primitives.add(reference.getChild());
        }
        this.primitives = primitives;
        refresh();
    }

    /**
     * Prepare the default decisions for the current model.
     *
     * Keep/delete decisions are made if every member has the same role and the members are in consecutive order within the relation.
     * For multiple occurrences those conditions are tested stepwise for each occurrence.
     */
    public void prepareDefaultRelationDecisions() {

        if (Utils.forAll(primitives, OsmPrimitive.nodePredicate)) {
            final Collection<OsmPrimitive> primitivesInDecisions = new HashSet<>();
            for (final RelationMemberConflictDecision i : decisions) {
                primitivesInDecisions.add(i.getOriginalPrimitive());
            }
            if (primitivesInDecisions.size() == 1) {
                for (final RelationMemberConflictDecision i : decisions) {
                    i.decide(RelationMemberConflictDecisionType.KEEP);
                }
                refresh();
                return;
            }
        }

        for (final Relation relation : relations) {
            final Map<OsmPrimitive, List<RelationMemberConflictDecision>> decisionsByPrimitive = new LinkedHashMap<>(primitives.size(), 1);
            for (final RelationMemberConflictDecision decision : decisions) {
                if (decision.getRelation() == relation) {
                    final OsmPrimitive primitive = decision.getOriginalPrimitive();
                    if (!decisionsByPrimitive.containsKey(primitive)) {
                        decisionsByPrimitive.put(primitive, new ArrayList<RelationMemberConflictDecision>());
                    }
                    decisionsByPrimitive.get(primitive).add(decision);
                }
            }

            //noinspection StatementWithEmptyBody
            if (!decisionsByPrimitive.keySet().containsAll(primitives)) {
                // some primitives are not part of the relation, leave undecided
            } else {
                final Collection<Iterator<RelationMemberConflictDecision>> iterators = new ArrayList<>(primitives.size());
                for (final Collection<RelationMemberConflictDecision> i : decisionsByPrimitive.values()) {
                    iterators.add(i.iterator());
                }
                while (Utils.forAll(iterators, new Predicate<Iterator<RelationMemberConflictDecision>>() {
                    @Override
                    public boolean evaluate(Iterator<RelationMemberConflictDecision> it) {
                        return it.hasNext();
                    }
                })) {
                    final List<RelationMemberConflictDecision> decisions = new ArrayList<>();
                    final Collection<String> roles = new HashSet<>();
                    final Collection<Integer> indices = new TreeSet<>();
                    for (Iterator<RelationMemberConflictDecision> it : iterators) {
                        final RelationMemberConflictDecision decision = it.next();
                        decisions.add(decision);
                        roles.add(decision.getRole());
                        indices.add(decision.getPos());
                    }
                    if (roles.size() != 1) {
                        // roles to not patch, leave undecided
                        continue;
                    } else if (!isCollectionOfConsecutiveNumbers(indices)) {
                        // not consecutive members in relation, leave undecided
                        continue;
                    }
                    decisions.get(0).decide(RelationMemberConflictDecisionType.KEEP);
                    for (RelationMemberConflictDecision decision : decisions.subList(1, decisions.size())) {
                        decision.decide(RelationMemberConflictDecisionType.REMOVE);
                    }
                }
            }
        }

        refresh();
    }

    static boolean isCollectionOfConsecutiveNumbers(Collection<Integer> numbers) {
        if (numbers.isEmpty()) {
            return true;
        }
        final Iterator<Integer> it = numbers.iterator();
        Integer previousValue = it.next();
        while (it.hasNext()) {
            final Integer i = it.next();
            if (previousValue + 1 != i) {
                return false;
            }
            previousValue = i;
        }
        return true;
    }

    /**
     * Replies the decision at position <code>row</code>
     *
     * @param row position
     * @return the decision at position <code>row</code>
     */
    public RelationMemberConflictDecision getDecision(int row) {
        return decisions.get(row);
    }

    /**
     * Replies the number of decisions managed by this model
     *
     * @return the number of decisions managed by this model
     */
    public int getNumDecisions() {
        return decisions == null /* accessed via super constructor */ ? 0 : decisions.size();
    }

    /**
     * Refreshes the model state. Invoke this method to trigger necessary change
     * events after an update of the model data.
     *
     */
    public void refresh() {
        updateNumConflicts();
        GuiHelper.runInEDTAndWait(new Runnable() {
            @Override public void run() {
                fireTableDataChanged();
            }
        });
    }

    /**
     * Apply a role to all member managed by this model.
     *
     * @param role the role. Empty string assumed if null.
     */
    public void applyRole(String role) {
        role = role == null ? "" : role;
        for (RelationMemberConflictDecision decision : decisions) {
            decision.setRole(role);
        }
        refresh();
    }

    protected RelationMemberConflictDecision getDecision(Relation relation, int pos) {
        for (RelationMemberConflictDecision decision: decisions) {
            if (decision.matches(relation, pos)) return decision;
        }
        return null;
    }

    protected Command buildResolveCommand(Relation relation, OsmPrimitive newPrimitive) {
        final Relation modifiedRelation = new Relation(relation);
        modifiedRelation.setMembers(null);
        boolean isChanged = false;
        for (int i = 0; i < relation.getMembersCount(); i++) {
            final RelationMember member = relation.getMember(i);
            RelationMemberConflictDecision decision = getDecision(relation, i);
            if (decision == null) {
                modifiedRelation.addMember(member);
            } else {
                switch(decision.getDecision()) {
                case KEEP:
                    final RelationMember newMember = new RelationMember(decision.getRole(), newPrimitive);
                    modifiedRelation.addMember(newMember);
                    isChanged |= !member.equals(newMember);
                    break;
                case REMOVE:
                    isChanged = true;
                    // do nothing
                    break;
                case UNDECIDED:
                    // FIXME: this is an error
                    break;
                }
            }
        }
        if (isChanged)
            return new ChangeCommand(relation, modifiedRelation);
        return null;
    }

    /**
     * Builds a collection of commands executing the decisions made in this model.
     *
     * @param newPrimitive the primitive which members shall refer to
     * @return a list of commands
     */
    public List<Command> buildResolutionCommands(OsmPrimitive newPrimitive) {
        List<Command> command = new LinkedList<>();
        for (Relation relation : relations) {
            Command cmd = buildResolveCommand(relation, newPrimitive);
            if (cmd != null) {
                command.add(cmd);
            }
        }
        return command;
    }

    protected boolean isChanged(Relation relation, OsmPrimitive newPrimitive) {
        for (int i = 0; i < relation.getMembersCount(); i++) {
            RelationMemberConflictDecision decision = getDecision(relation, i);
            if (decision == null) {
                continue;
            }
            switch(decision.getDecision()) {
            case REMOVE: return true;
            case KEEP:
                if (!relation.getMember(i).getRole().equals(decision.getRole()))
                    return true;
                if (relation.getMember(i).getMember() != newPrimitive)
                    return true;
            case UNDECIDED:
                // FIXME: handle error
            }
        }
        return false;
    }

    /**
     * Replies the set of relations which have to be modified according
     * to the decisions managed by this model.
     *
     * @param newPrimitive the primitive which members shall refer to
     *
     * @return the set of relations which have to be modified according
     * to the decisions managed by this model
     */
    public Set<Relation> getModifiedRelations(OsmPrimitive newPrimitive) {
        Set<Relation> ret = new HashSet<>();
        for (Relation relation: relations) {
            if (isChanged(relation, newPrimitive)) {
                ret.add(relation);
            }
        }
        return ret;
    }
}
