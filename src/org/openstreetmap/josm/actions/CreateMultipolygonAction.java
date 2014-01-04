// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.MultipolygonCreate;
import org.openstreetmap.josm.data.osm.MultipolygonCreate.JoinedPolygon;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.gui.dialogs.relation.DownloadRelationTask;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Shortcut;
import org.openstreetmap.josm.tools.Utils;

/**
 * Create multipolygon from selected ways automatically.
 *
 * New relation with type=multipolygon is created
 *
 * If one or more of ways is already in relation with type=multipolygon or the
 * way is not closed, then error is reported and no relation is created
 *
 * The "inner" and "outer" roles are guessed automatically. First, bbox is
 * calculated for each way. then the largest area is assumed to be outside and
 * the rest inside. In cases with one "outside" area and several cut-ins, the
 * guess should be always good ... In more complex (multiple outer areas) or
 * buggy (inner and outer ways intersect) scenarios the result is likely to be
 * wrong.
 */
public class CreateMultipolygonAction extends JosmAction {

    private final boolean update;

    /**
     * Constructs a new {@code CreateMultipolygonAction}.
     */
    public CreateMultipolygonAction(final boolean update) {
        super(getName(update), "multipoly_create", getName(update),
                update
                        ? null
                        : Shortcut.registerShortcut("tools:multipoly", tr("Tool: {0}", getName(false)), KeyEvent.VK_A, Shortcut.ALT_CTRL),
                true, update ? "multipoly_update" : "multipoly_create", true);
        this.update = update;
    }

    private static String getName(boolean update) {
        return update ? tr("Update multipolygon") : tr("Create multipolygon");
    }

    /**
     * The action button has been clicked
     *
     * @param e Action Event
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!Main.main.hasEditLayer()) {
            new Notification(
                    tr("No data loaded."))
                    .setIcon(JOptionPane.WARNING_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        final Collection<Way> selectedWays = Main.main.getCurrentDataSet().getSelectedWays();
        final Collection<Relation> selectedRelations = Main.main.getCurrentDataSet().getSelectedRelations();

        if (selectedWays.size() < 1) {
            // Sometimes it make sense creating multipoly of only one way (so it will form outer way)
            // and then splitting the way later (so there are multiple ways forming outer way)
            new Notification(
                    tr("You must select at least one way."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        final Relation multipolygonRelation = update
                ? getSelectedMultipolygonRelation(selectedWays, selectedRelations)
                : null;

        // runnable to create/update multipolygon relation
        final Runnable createOrUpdateMultipolygonTask = new Runnable() {
            @Override
            public void run() {
                final Pair<SequenceCommand, Relation> commandAndRelation = createMultipolygonCommand(selectedWays, multipolygonRelation);
                if (commandAndRelation == null) {
                    return;
                }
                final Command command = commandAndRelation.a;
                final Relation relation = commandAndRelation.b;


                // to avoid EDT violations
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Main.main.undoRedo.add(command);
                    }
                });

                // Use 'SwingUtilities.invokeLater' to make sure the relationListDialog
                // knows about the new relation before we try to select it.
                // (Yes, we are already in event dispatch thread. But DatasetEventManager
                // uses 'SwingUtilities.invokeLater' to fire events so we have to do
                // the same.)
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        Main.map.relationListDialog.selectRelation(relation);
                        if (Main.pref.getBoolean("multipoly.show-relation-editor", false)) {
                            //Open relation edit window, if set up in preferences
                            RelationEditor editor = RelationEditor.getEditor(Main.main.getEditLayer(), relation, null);

                            editor.setModal(true);
                            editor.setVisible(true);
                        }
                    }
                });
            }
        };

        // download incomplete relation if necessary
        if (multipolygonRelation != null && (multipolygonRelation.isIncomplete() || multipolygonRelation.hasIncompleteMembers())) {
            Main.worker.submit(new DownloadRelationTask(Collections.singleton(multipolygonRelation), Main.main.getEditLayer()));
        }
        // create/update multipolygon relation
        Main.worker.submit(createOrUpdateMultipolygonTask);

    }

    private Relation getSelectedMultipolygonRelation() {
        return getSelectedMultipolygonRelation(getCurrentDataSet().getSelectedWays(), getCurrentDataSet().getSelectedRelations());
    }

    private static Relation getSelectedMultipolygonRelation(Collection<Way> selectedWays, Collection<Relation> selectedRelations) {
        if (selectedRelations.size() == 1 && "multipolygon".equals(selectedRelations.iterator().next().get("type"))) {
            return selectedRelations.iterator().next();
        } else {
            final HashSet<Relation> relatedRelations = new HashSet<Relation>();
            for (final Way w : selectedWays) {
                relatedRelations.addAll(Utils.filteredCollection(w.getReferrers(), Relation.class));
            }
            return relatedRelations.size() == 1 ? relatedRelations.iterator().next() : null;
        }
    }

    /**
     * Returns a {@link Pair} of the old multipolygon {@link Relation} (or null) and the newly created/modified multipolygon {@link Relation}.
     */
    public static Pair<Relation, Relation> updateMultipolygonRelation(Collection<Way> selectedWays, Relation selectedMultipolygonRelation) {

        // add ways of existing relation to include them in polygon analysis
        selectedWays = new HashSet<Way>(selectedWays);
        selectedWays.addAll(selectedMultipolygonRelation.getMemberPrimitives(Way.class));

        final MultipolygonCreate polygon = analyzeWays(selectedWays, true);
        if (polygon == null) {
            return null; //could not make multipolygon.
        } else {
            return Pair.create(selectedMultipolygonRelation, createRelation(polygon, new Relation(selectedMultipolygonRelation)));
        }
    }

    /**
     * Returns a {@link Pair} null and the newly created/modified multipolygon {@link Relation}.
     */
    public static Pair<Relation, Relation> createMultipolygonRelation(Collection<Way> selectedWays, boolean showNotif) {

        final MultipolygonCreate polygon = analyzeWays(selectedWays, showNotif);
        if (polygon == null) {
            return null; //could not make multipolygon.
        } else {
            return Pair.create(null, createRelation(polygon, new Relation()));
        }
    }

    /**
     * Returns a pair of a multipolygon creating/modifying {@link Command} as well as the multipolygon {@link Relation}.
     */
    public static Pair<SequenceCommand, Relation> createMultipolygonCommand(Collection<Way> selectedWays, Relation selectedMultipolygonRelation) {

        final Pair<Relation, Relation> rr = selectedMultipolygonRelation == null
                ? createMultipolygonRelation(selectedWays, true)
                : updateMultipolygonRelation(selectedWays, selectedMultipolygonRelation);
        if (rr == null) {
            return null;
        }
        final Relation existingRelation = rr.a;
        final Relation relation = rr.b;

        final List<Command> list = removeTagsFromWaysIfNeeded(relation);
        final String commandName;
        if (existingRelation == null) {
            list.add(new AddCommand(relation));
            commandName = getName(false);
        } else {
            list.add(new ChangeCommand(existingRelation, relation));
            commandName = getName(true);
        }
        return Pair.create(new SequenceCommand(commandName, list), relation);
    }

    /** Enable this action only if something is selected */
    @Override protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    /**
      * Enable this action only if something is selected
      *
      * @param selection the current selection, gets tested for emptyness
      */
    @Override protected void updateEnabledState(Collection < ? extends OsmPrimitive > selection) {
        if (update) {
            setEnabled(getSelectedMultipolygonRelation() != null);
        } else {
            setEnabled(!getCurrentDataSet().getSelectedWays().isEmpty());
        }
    }

    /**
     * This method analyzes ways and creates multipolygon.
     * @param selectedWays list of selected ways
     * @return <code>null</code>, if there was a problem with the ways.
     */
    private static MultipolygonCreate analyzeWays(Collection < Way > selectedWays, boolean showNotif) {

        MultipolygonCreate pol = new MultipolygonCreate();
        String error = pol.makeFromWays(selectedWays);

        if (error != null) {
            if (showNotif) {
                new Notification(error)
                        .setIcon(JOptionPane.INFORMATION_MESSAGE)
                        .show();
            }
            return null;
        } else {
            return pol;
        }
    }

    /**
     * Builds a relation from polygon ways.
     * @param pol data storage class containing polygon information
     * @return multipolygon relation
     */
    private static Relation createRelation(MultipolygonCreate pol, final Relation rel) {
        // Create new relation
        rel.put("type", "multipolygon");
        // Add ways to it
        for (JoinedPolygon jway:pol.outerWays) {
            addMembers(jway, rel, "outer");
        }

        for (JoinedPolygon jway:pol.innerWays) {
            addMembers(jway, rel, "inner");
        }
        return rel;
    }

    private static void addMembers(JoinedPolygon polygon, Relation rel, String role) {
        final int count = rel.getMembersCount();
        final HashSet<Way> ways = new HashSet<Way>(polygon.ways);
        for (int i = 0; i < count; i++) {
            final RelationMember m = rel.getMember(i);
            if (ways.contains(m.getMember()) && !role.equals(m.getRole())) {
                rel.setMember(i, new RelationMember(role, m.getMember()));
            }
        }
        ways.removeAll(rel.getMemberPrimitives());
        for (final Way way : ways) {
            rel.addMember(new RelationMember(role, way));
        }
    }

    static public final List<String> DEFAULT_LINEAR_TAGS = Arrays.asList(new String[] {"barrier", "source"});

    /**
     * This method removes tags/value pairs from inner and outer ways and put them on relation if necessary
     * Function was extended in reltoolbox plugin by Zverikk and copied back to the core
     * @param relation the multipolygon style relation to process
     * @return a list of commands to execute
     */
    private static List<Command> removeTagsFromWaysIfNeeded( Relation relation ) {
        Map<String, String> values = new HashMap<String, String>();

        if( relation.hasKeys() ) {
            for( String key : relation.keySet() ) {
                values.put(key, relation.get(key));
            }
        }

        List<Way> innerWays = new ArrayList<Way>();
        List<Way> outerWays = new ArrayList<Way>();

        Set<String> conflictingKeys = new TreeSet<String>();

        for( RelationMember m : relation.getMembers() ) {

            if( m.hasRole() && "inner".equals(m.getRole()) && m.isWay() && m.getWay().hasKeys() ) {
                innerWays.add(m.getWay());
            }

            if( m.hasRole() && "outer".equals(m.getRole()) && m.isWay() && m.getWay().hasKeys() ) {
                Way way = m.getWay();
                outerWays.add(way);

                for( String key : way.keySet() ) {
                    if( !values.containsKey(key) ) { //relation values take precedence
                        values.put(key, way.get(key));
                    } else if( !relation.hasKey(key) && !values.get(key).equals(way.get(key)) ) {
                        conflictingKeys.add(key);
                    }
                }
            }
        }

        // filter out empty key conflicts - we need second iteration
        if( !Main.pref.getBoolean("multipoly.alltags", false) )
            for( RelationMember m : relation.getMembers() )
                if( m.hasRole() && m.getRole().equals("outer") && m.isWay() )
                    for( String key : values.keySet() )
                        if( !m.getWay().hasKey(key) && !relation.hasKey(key) )
                            conflictingKeys.add(key);

        for( String key : conflictingKeys )
            values.remove(key);

        for( String linearTag : Main.pref.getCollection("multipoly.lineartagstokeep", DEFAULT_LINEAR_TAGS) )
            values.remove(linearTag);

        if( values.containsKey("natural") && values.get("natural").equals("coastline") )
            values.remove("natural");

        values.put("area", "yes");

        List<Command> commands = new ArrayList<Command>();
        boolean moveTags = Main.pref.getBoolean("multipoly.movetags", true);

        for (Entry<String, String> entry : values.entrySet()) {
            List<OsmPrimitive> affectedWays = new ArrayList<OsmPrimitive>();
            String key = entry.getKey();
            String value = entry.getValue();

            for (Way way : innerWays) {
                if (way.hasKey(key) && (value.equals(way.get(key)))) {
                    affectedWays.add(way);
                }
            }

            if (moveTags) {
                // remove duplicated tags from outer ways
                for( Way way : outerWays ) {
                    if( way.hasKey(key) ) {
                        affectedWays.add(way);
                    }
                }
            }

            if (!affectedWays.isEmpty()) {
                // reset key tag on affected ways
                commands.add(new ChangePropertyCommand(affectedWays, key, null));
            }
        }

        if (moveTags) {
            // add those tag values to the relation

            boolean fixed = false;
            Relation r2 = new Relation(relation);
            for (Entry<String, String> entry : values.entrySet()) {
                String key = entry.getKey();
                if (!r2.hasKey(key) && !key.equals("area") ) {
                    if (relation.isNew())
                        relation.put(key, entry.getValue());
                    else
                        r2.put(key, entry.getValue());
                    fixed = true;
                }
            }
            if (fixed && !relation.isNew())
                commands.add(new ChangeCommand(relation, r2));
        }

        return commands;
    }
}
