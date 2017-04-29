// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of the {@code OsmPrimitiveType} class.
 */
public class OsmPrimitiveTypeTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link OsmPrimitiveType} enum.
     */
    @Test
    public void testEnum() {
        TestUtils.superficialEnumCodeCoverage(OsmPrimitiveType.class);
    }

    /**
     * Unit test of {@link OsmPrimitiveType#getAPIName} method.
     */
    @Test
    public void testGetApiName() {
        assertEquals("node", OsmPrimitiveType.NODE.getAPIName());
        assertEquals("way", OsmPrimitiveType.WAY.getAPIName());
        assertEquals("relation", OsmPrimitiveType.RELATION.getAPIName());
    }

    /**
     * Unit test of {@link OsmPrimitiveType#getOsmClass} method.
     */
    @Test
    public void testGetOsmClass() {
        assertEquals(Node.class, OsmPrimitiveType.NODE.getOsmClass());
        assertEquals(Way.class, OsmPrimitiveType.WAY.getOsmClass());
        assertEquals(Relation.class, OsmPrimitiveType.RELATION.getOsmClass());
        assertNull(OsmPrimitiveType.CLOSEDWAY.getOsmClass());
        assertNull(OsmPrimitiveType.MULTIPOLYGON.getOsmClass());
    }

    /**
     * Unit test of {@link OsmPrimitiveType#getDataClass} method.
     */
    @Test
    public void testGetDataClass() {
        assertEquals(NodeData.class, OsmPrimitiveType.NODE.getDataClass());
        assertEquals(WayData.class, OsmPrimitiveType.WAY.getDataClass());
        assertEquals(RelationData.class, OsmPrimitiveType.RELATION.getDataClass());
        assertEquals(WayData.class, OsmPrimitiveType.CLOSEDWAY.getDataClass());
        assertEquals(RelationData.class, OsmPrimitiveType.MULTIPOLYGON.getDataClass());
    }

    /**
     * Unit test of {@link OsmPrimitiveType#fromApiTypeName} method.
     */
    @Test
    public void testFromApiTypeName() {
        assertEquals(OsmPrimitiveType.NODE, OsmPrimitiveType.fromApiTypeName("node"));
        assertEquals(OsmPrimitiveType.WAY, OsmPrimitiveType.fromApiTypeName("way"));
        assertEquals(OsmPrimitiveType.RELATION, OsmPrimitiveType.fromApiTypeName("relation"));
    }

    /**
     * Unit test of {@link OsmPrimitiveType#fromApiTypeName} method - error case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromApiTypeNameError() {
        OsmPrimitiveType.fromApiTypeName("foo");
    }

    /**
     * Unit test of {@link OsmPrimitiveType#from(IPrimitive)} method.
     */
    @Test
    public void testFromIPrimitive() {
        assertEquals(OsmPrimitiveType.NODE, OsmPrimitiveType.from(new Node()));
        assertEquals(OsmPrimitiveType.WAY, OsmPrimitiveType.from(new Way()));
        assertEquals(OsmPrimitiveType.RELATION, OsmPrimitiveType.from(new Relation()));
    }

    /**
     * Unit test of {@link OsmPrimitiveType#from(IPrimitive)} method - error case.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFromIPrimitiveError() {
        OsmPrimitiveType.from((IPrimitive) null);
    }

    /**
     * Unit test of {@link OsmPrimitiveType#from(String)} method.
     */
    @Test
    public void testFromString() {
        assertEquals(OsmPrimitiveType.NODE, OsmPrimitiveType.from("node"));
        assertEquals(OsmPrimitiveType.WAY, OsmPrimitiveType.from("WAY"));
        assertEquals(OsmPrimitiveType.RELATION, OsmPrimitiveType.from("Relation"));
        assertEquals(OsmPrimitiveType.CLOSEDWAY, OsmPrimitiveType.from("closedway"));
        assertEquals(OsmPrimitiveType.MULTIPOLYGON, OsmPrimitiveType.from("multipolygon"));
        assertNull(OsmPrimitiveType.from((String) null));
    }

    /**
     * Unit test of {@link OsmPrimitiveType#dataValues} method.
     */
    @Test
    public void testDataValues() {
        Collection<OsmPrimitiveType> values = OsmPrimitiveType.dataValues();
        assertEquals(3, values.size());
        assertTrue(values.contains(OsmPrimitiveType.NODE));
        assertTrue(values.contains(OsmPrimitiveType.WAY));
        assertTrue(values.contains(OsmPrimitiveType.RELATION));
    }

    /**
     * Unit test of {@link OsmPrimitiveType#newInstance} method.
     */
    @Test
    public void testNewInstance() {
        OsmPrimitive n = OsmPrimitiveType.NODE.newInstance(1, false);
        OsmPrimitive w = OsmPrimitiveType.WAY.newInstance(2, false);
        OsmPrimitive r = OsmPrimitiveType.RELATION.newInstance(3, false);

        assertTrue(n instanceof Node);
        assertTrue(w instanceof Way);
        assertTrue(r instanceof Relation);

        assertEquals(1, n.getId());
        assertEquals(2, w.getId());
        assertEquals(3, r.getId());
    }

    /**
     * Unit test of {@link OsmPrimitiveType#newInstance} method - error case.
     */
    @Test(expected = AssertionError.class)
    public void testNewInstanceError() {
        OsmPrimitiveType.CLOSEDWAY.newInstance(1, false);
    }

    /**
     * Unit test of {@link OsmPrimitiveType#newVersionedInstance} method.
     */
    @Test
    public void testNewVersionedInstance() {
        OsmPrimitive n = OsmPrimitiveType.NODE.newVersionedInstance(1, 4);
        OsmPrimitive w = OsmPrimitiveType.WAY.newVersionedInstance(2, 5);
        OsmPrimitive r = OsmPrimitiveType.RELATION.newVersionedInstance(3, 6);

        assertTrue(n instanceof Node);
        assertTrue(w instanceof Way);
        assertTrue(r instanceof Relation);

        assertEquals(1, n.getId());
        assertEquals(2, w.getId());
        assertEquals(3, r.getId());

        assertEquals(4, n.getVersion());
        assertEquals(5, w.getVersion());
        assertEquals(6, r.getVersion());
    }
}
