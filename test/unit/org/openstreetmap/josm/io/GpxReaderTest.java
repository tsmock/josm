// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.xml.sax.SAXException;

/**
 * Tests the {@link GpxReader}.
 */
public class GpxReaderTest {

    /**
     * Tests the {@code munich.gpx} test file.
     * @throws Exception if something goes wrong
     */
    @Test
    public void testMunich() throws Exception {
        final GpxData result;
        try (final FileInputStream in = new FileInputStream(new File("data_nodist/munich.gpx"))) {
            final GpxReader reader = new GpxReader(in);
            assertTrue(reader.parse(false));
            result = reader.getGpxData();
        }
        assertEquals(2762, result.tracks.size());
        assertEquals(0, result.routes.size());
        assertEquals(903, result.waypoints.size());

        final WayPoint tenthWayPoint = ((List<WayPoint>) result.waypoints).get(10);
        assertEquals("128970", tenthWayPoint.get(GpxData.GPX_NAME));
        assertEquals(new LatLon(48.183956146240234, 11.43463134765625), tenthWayPoint.getCoor());
    }

    /**
     * Tests invalid data.
     * @throws Exception always SAXException
     */
    @Test(expected = SAXException.class)
    public void testException() throws Exception {
        new GpxReader(new ByteArrayInputStream("--foo--bar--".getBytes(StandardCharsets.UTF_8))).parse(true);
    }
}
