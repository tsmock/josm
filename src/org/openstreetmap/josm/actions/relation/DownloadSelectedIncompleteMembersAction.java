package org.openstreetmap.josm.actions.relation;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SHORT_DESCRIPTION;
import static javax.swing.Action.SMALL_ICON;


import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationMemberTask;
import org.openstreetmap.josm.tools.ImageProvider;

import static org.openstreetmap.josm.tools.I18n.tr;

/**
 * Action for downloading incomplete members of selected relations
 */
public class DownloadSelectedIncompleteMembersAction extends AbstractRelationAction {

    public DownloadSelectedIncompleteMembersAction() {
        putValue(SHORT_DESCRIPTION, tr("Download incomplete members of selected relations"));
        putValue(SMALL_ICON, ImageProvider.get("dialogs/relation", "downloadincompleteselected"));
        putValue(NAME, tr("Download incomplete members"));
    }

    public Set<OsmPrimitive> buildSetOfIncompleteMembers(Collection<Relation> rels) {
        Set<OsmPrimitive> ret = new HashSet<OsmPrimitive>();
        for (Relation r : rels) {
            ret.addAll(r.getIncompleteMembers());
        }
        return ret;
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled() || relations.isEmpty()) return;
        Main.worker.submit(new DownloadRelationMemberTask(
                relations,
                buildSetOfIncompleteMembers(relations),
                Main.map.mapView.getEditLayer()));
    }

}