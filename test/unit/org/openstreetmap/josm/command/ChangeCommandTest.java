// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.command.CommandTest.CommandTestData;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataIntegrityProblemException;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.User;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests of {@link ChangeCommand} class.
 */
public class ChangeCommandTest {

    /**
     * We need prefs for nodes.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().i18n();
    private CommandTestData testData;

    /**
     * Set up the test data.
     */
    @Before
    public void createTestData() {
        testData = new CommandTestData();
    }

    /**
     * Test that empty ways are prevented.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testPreventEmptyWays() {
        Way emptyWay = new Way();
        new ChangeCommand(testData.existingWay, emptyWay);
    }

    /**
     * Test {@link ChangeCommand#executeCommand()}
     */
    @Test
    public void testChange() {
        Node newNode = new Node(5);
        newNode.setCoor(LatLon.NORTH_POLE);
        newNode.put("new", "new");

        new ChangeCommand(testData.existingNode, newNode).executeCommand();

        assertEquals("new", testData.existingNode.get("new"));
        assertEquals(null, testData.existingNode.get("existing"));
        assertEquals(LatLon.NORTH_POLE, testData.existingNode.getCoor());

        Way newWay = new Way(10);
        List<Node> newNodes = testData.existingWay.getNodes();
        Collections.reverse(newNodes);
        newWay.setNodes(newNodes);

        new ChangeCommand(testData.existingWay, newWay).executeCommand();
        assertArrayEquals(newNodes.toArray(), testData.existingWay.getNodes().toArray());
    }

    /**
     * Test {@link ChangeCommand#executeCommand()} fails if ID is changed
     */
    @Test(expected = DataIntegrityProblemException.class)
    public void testChangeIdChange() {
        Node newNode = new Node(1);
        newNode.setCoor(LatLon.NORTH_POLE);

        new ChangeCommand(testData.existingNode, newNode).executeCommand();
    }

    /**
     * Test {@link ChangeCommand#undoCommand()}
     */
    @Test
    public void testUndo() {
        Node newNode = new Node(5);
        newNode.setCoor(LatLon.NORTH_POLE);
        newNode.put("new", "new");

        ChangeCommand command = new ChangeCommand(testData.existingNode, newNode);
        command.executeCommand();

        assertEquals("new", testData.existingNode.get("new"));
        assertEquals(LatLon.NORTH_POLE, testData.existingNode.getCoor());

        command.undoCommand();
        assertNull(testData.existingNode.get("new"));
        assertEquals("existing", testData.existingNode.get("existing"));
        assertEquals(LatLon.ZERO, testData.existingNode.getCoor());
    }

    /**
     * Tests {@link ChangeCommand#fillModifiedData(java.util.Collection, java.util.Collection, java.util.Collection)}
     */
    @Test
    public void testFillModifiedData() {
        ArrayList<OsmPrimitive> modified = new ArrayList<>();
        ArrayList<OsmPrimitive> deleted = new ArrayList<>();
        ArrayList<OsmPrimitive> added = new ArrayList<>();
        new ChangeCommand(testData.existingNode, testData.existingNode).fillModifiedData(modified, deleted, added);
        assertArrayEquals(new Object[] {testData.existingNode}, modified.toArray());
        assertArrayEquals(new Object[] {}, deleted.toArray());
        assertArrayEquals(new Object[] {}, added.toArray());
    }

    /**
     * Test {@link ChangeCommand#getDescriptionText()}
     */
    @Test
    public void testDescription() {
        Node node = new Node(LatLon.ZERO);
        node.put("name", "xy");
        Way way = new Way();
        way.addNode(node);
        way.put("name", "xy");
        Relation relation = new Relation();
        relation.put("name", "xy");

        assertTrue(new ChangeCommand(node, node).getDescriptionText().matches("Change node.*xy.*"));
        assertTrue(new ChangeCommand(way, way).getDescriptionText().matches("Change way.*xy.*"));
        assertTrue(new ChangeCommand(relation, relation).getDescriptionText().matches("Change relation.*xy.*"));
    }

    /**
     * Unit test of methods {@link ChangeCommand#equals} and {@link ChangeCommand#hashCode}.
     */
    @Test
    public void equalsContract() {
        EqualsVerifier.forClass(ChangeCommand.class).usingGetClass()
            .withPrefabValues(DataSet.class,
                new DataSet(), new DataSet())
            .withPrefabValues(User.class,
                    User.createOsmUser(1, "foo"), User.createOsmUser(2, "bar"))
            .withPrefabValues(OsmPrimitive.class,
                new Node(1), new Node(2))
            .withPrefabValues(OsmDataLayer.class,
                new OsmDataLayer(new DataSet(), "1", null), new OsmDataLayer(new DataSet(), "2", null))
            .suppress(Warning.NONFINAL_FIELDS)
            .verify();
    }
}
