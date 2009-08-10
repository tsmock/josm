// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.gui.PrimitiveNameFormatter;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command that adds a relation to an OSM object
 *
 * @author daveh
 */
public class RemoveRelationMemberCommand extends Command {

    // container object in which to replace a sub object
    private final Relation relation;
    // the sub-object to be replaced
    private final RelationMember member;
    // its replacement
    private int location = -1;

    public RemoveRelationMemberCommand(Relation _relation, RelationMember _member) {
        this.relation = _relation;
        this.member = _member;
    }
    public RemoveRelationMemberCommand(Relation _relation, RelationMember _member, int _location) {
        this.relation = _relation;
        this.member = _member;
        location = _location;
    }

    @Override public boolean executeCommand() {
        super.executeCommand();
        int removeIndex = relation.getMembers().indexOf(member);
        if ((location != -1) && (removeIndex != location)) {
            Main.debug("error removing relation member");
            return false;
        } else {
            relation.removeMember(removeIndex);
            relation.modified = true;
            return true;
        }
    }

    @Override public void undoCommand() {
        super.undoCommand();
        relation.addMember(member);
        relation.modified = this.getOrig(relation).modified;
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {}

    @Override public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Remove relation member {0} {1}",
                                OsmPrimitiveType.from(relation).getLocalizedDisplayNameSingular(),
                                new PrimitiveNameFormatter().getName(relation)
                        ),
                        ImageProvider.get(OsmPrimitiveType.from(relation)),
                        JLabel.HORIZONTAL
                )
        );
    }
}
