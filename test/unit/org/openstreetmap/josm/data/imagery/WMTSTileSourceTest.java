// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.gui.jmapviewer.tilesources.TemplatedTMSTileSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;

/**
 * Unit tests for class {@link WMTSTileSource}.
 */
public class WMTSTileSourceTest {

    private ImageryInfo testImageryTMS =  new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
    private ImageryInfo testImageryPSEUDO_MERCATOR = getImagery(TestUtils.getTestDataRoot() + "wmts/getcapabilities-pseudo-mercator.xml");
    private ImageryInfo testImageryTOPO_PL = getImagery(TestUtils.getTestDataRoot() + "wmts/getcapabilities-TOPO.xml");
    private ImageryInfo testImageryORTO_PL = getImagery(TestUtils.getTestDataRoot() + "wmts/getcapabilities-ORTO.xml");
    private ImageryInfo testImageryWIEN = getImagery(TestUtils.getTestDataRoot() + "wmts/getCapabilities-wien.xml");
    private ImageryInfo testImageryWALLONIE = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Wallonie.xml");
    private ImageryInfo testImageryOntario = getImagery(TestUtils.getTestDataRoot() + "wmts/WMTSCapabilities-Ontario.xml");
    private ImageryInfo testImagery12168 = getImagery(TestUtils.getTestDataRoot() + "wmts/bug12168-WMTSCapabilities.xml");
    private ImageryInfo testLotsOfLayers = getImagery(TestUtils.getTestDataRoot() + "wmts/getCapabilities-lots-of-layers.xml");

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static ImageryInfo getImagery(String path) {
        try {
            return new ImageryInfo(
                    "test",
                    new File(path).toURI().toURL().toString()
                    );
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Test
    public void testPseudoMercator() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryPSEUDO_MERCATOR);
        testSource.initProjection(Main.getProjection());

        verifyMercatorTile(testSource, 0, 0, 1);
        verifyMercatorTile(testSource, 0, 0, 2);
        verifyMercatorTile(testSource, 1, 1, 2);
        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 3);
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 4);
            }
        }

        verifyMercatorTile(testSource, 2 << 9 - 1, 2 << 8 - 1, 10);

        assertEquals("TileXMax", 1, testSource.getTileXMax(0));
        assertEquals("TileYMax", 1, testSource.getTileYMax(0));
        assertEquals("TileXMax", 2, testSource.getTileXMax(1));
        assertEquals("TileYMax", 2, testSource.getTileYMax(1));
        assertEquals("TileXMax", 4, testSource.getTileXMax(2));
        assertEquals("TileYMax", 4, testSource.getTileYMax(2));

    }

    @Test
    public void testWALLONIE() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:31370"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryWALLONIE);
        testSource.initProjection(Main.getProjection());

        assertEquals("http://geoservices.wallonie.be/arcgis/rest/services/DONNEES_BASE/FOND_PLAN_ANNOTATIONS_2012_RW_NB/"
                + "MapServer/WMTS/tile/1.0.0/DONNEES_BASE_FOND_PLAN_ANNOTATIONS_2012_RW_NB/default/default028mm/5/1219/1063.png",
                testSource.getTileUrl(5, 1063, 1219));

        // +bounds=2.54,49.51,6.4,51.5
        Bounds wallonieBounds = new Bounds(
                new LatLon(49.485372459967245, 2.840548314430268),
                new LatLon(50.820959517561256, 6.427849693016202)
                );
        verifyBounds(wallonieBounds, testSource, 5, 1063, 1219);
        verifyBounds(wallonieBounds, testSource, 10, 17724, 20324);
    }

    // XXX - disable this test, needs further working
    // @Test
    public void testWALLONIENoMatrixDimension() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:31370"));
        WMTSTileSource testSource = new WMTSTileSource(getImagery("test/data/wmts/WMTSCapabilities-Wallonie-nomatrixdimension.xml"));
        testSource.initProjection(Main.getProjection());

        Bounds wallonieBounds = new Bounds(
                new LatLon(49.485372459967245, 2.840548314430268),
                new LatLon(50.820959517561256, 6.427849693016202)
                );

        verifyBounds(wallonieBounds, testSource, 6, 1063, 1219);
        verifyBounds(wallonieBounds, testSource, 11, 17724, 20324);
    }

    private void verifyBounds(Bounds bounds, WMTSTileSource testSource, int z, int x, int y) {
        LatLon ret = new LatLon(testSource.tileXYToLatLon(x, y, z));
        assertTrue(ret.toDisplayString() + " doesn't lie within: " + bounds.toString(), bounds.contains(ret));
        int tileXmax = testSource.getTileXMax(z);
        int tileYmax = testSource.getTileYMax(z);
        assertTrue("tile x: " + x + " is greater than allowed max: " + tileXmax, tileXmax >= x);
        assertTrue("tile y: " + y + " is greater than allowed max: " + tileYmax, tileYmax >= y);
    }

    @Test
    public void testWIEN() throws MalformedURLException, IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryWIEN);
        testSource.initProjection(Main.getProjection());
        int zoomOffset = 10;

        verifyMercatorTile(testSource, 0, 0, 1, zoomOffset);
        verifyMercatorTile(testSource, 1105, 709, 2, zoomOffset);
        verifyMercatorTile(testSource, 1, 1, 1, zoomOffset);
        verifyMercatorTile(testSource, 2, 2, 1, zoomOffset);
        verifyMercatorTile(testSource, 0, 0, 2, zoomOffset);
        verifyMercatorTile(testSource, 1, 1, 2, zoomOffset);

        for (int x = 0; x < 4; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 3, zoomOffset);
            }
        }
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 4; y++) {
                verifyMercatorTile(testSource, x, y, 4, zoomOffset);
            }
        }

        verifyMercatorTile(testSource, 2 << 9 - 1, 2 << 8 - 1, 2, zoomOffset);

        verifyMercatorMax(testSource, 1, zoomOffset);
        verifyMercatorMax(testSource, 2, zoomOffset);
        verifyMercatorMax(testSource, 3, zoomOffset);
    }

    private void verifyMercatorMax(WMTSTileSource testSource, int zoom, int zoomOffset) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        int result = testSource.getTileXMax(zoom);
        int expected = verifier.getTileXMax(zoom + zoomOffset);
        assertTrue("TileXMax expected: " + expected + " got: " + result, Math.abs(result - expected) < 5);
        result = testSource.getTileYMax(zoom);
        expected = verifier.getTileYMax(zoom + zoomOffset);
        assertTrue("TileYMax expected: " + expected + " got: " + result, Math.abs(result - expected) < 5);
    }

    @Test
    public void testGeoportalTOPOPL() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryTOPO_PL);
        testSource.initProjection(Main.getProjection());
        verifyTile(new LatLon(56, 12), testSource, 0, 0, 1);
        verifyTile(new LatLon(56, 12), testSource, 0, 0, 2);
        verifyTile(new LatLon(51.13231917844218, 16.867680821557823), testSource, 1, 1, 1);

        assertEquals("TileXMax", 2, testSource.getTileXMax(0));
        assertEquals("TileYMax", 1, testSource.getTileYMax(0));
        assertEquals("TileXMax", 3, testSource.getTileXMax(1));
        assertEquals("TileYMax", 2, testSource.getTileYMax(1));
        assertEquals("TileXMax", 6, testSource.getTileXMax(2));
        assertEquals("TileYMax", 4, testSource.getTileYMax(2));
        assertEquals(
                "http://mapy.geoportal.gov.pl/wss/service/WMTS/guest/wmts/TOPO?SERVICE=WMTS&REQUEST=GetTile&"
                + "VERSION=1.0.0&LAYER=MAPA TOPOGRAFICZNA&STYLE=default&FORMAT=image/jpeg&tileMatrixSet=EPSG:4326&"
                + "tileMatrix=EPSG:4326:0&tileRow=1&tileCol=1",
                testSource.getTileUrl(0,  1,  1));
    }

    @Test
    public void testGeoportalORTOPL4326() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:4326"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORTO_PL);
        testSource.initProjection(Main.getProjection());
        verifyTile(new LatLon(53.60205873528009, 19.552206794646956), testSource, 12412, 3941, 13);
        verifyTile(new LatLon(49.79005619189761, 22.778262259134397), testSource, 17714, 10206, 13);
    }

    @Test
    public void testGeoportalORTOPL2180() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:2180"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryORTO_PL);
        testSource.initProjection(Main.getProjection());

        verifyTile(new LatLon(53.59940948387726, 19.560544913270064), testSource, 6453, 3140, 13);
        verifyTile(new LatLon(49.782984840526055, 22.790064966993445), testSource, 9932, 9305, 13);
    }

    @Test
    public void test12168() throws IOException {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImagery12168);
        testSource.initProjection(Main.getProjection());
        assertEquals(
                "http://www.ngi.be/cartoweb/1.0.0/topo/default/3857/7/1/1.png",
                testSource.getTileUrl(0,  1,  1));
    }


    // XXX: disabled as this needs user action
    //@Test
    public void testTwoTileSetsForOneProjection() throws Exception {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testImageryOntario);
        testSource.initProjection(Main.getProjection());
        verifyTile(new LatLon(45.4105023, -75.7153702), testSource, 303751, 375502, 12);
        verifyTile(new LatLon(45.4601306, -75.7617187), testSource, 1186, 1466, 4);

    }

    // XXX: disabled as this needs user action
    // @Test
    public void testManyLayersScrollbars() throws Exception {
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857"));
        WMTSTileSource testSource = new WMTSTileSource(testLotsOfLayers);
        testSource.initProjection(Main.getProjection());

    }

    private void verifyTile(LatLon expected, WMTSTileSource source, int x, int y, int z) {
        LatLon ll = new LatLon(source.tileXYToLatLon(x, y, z));
        assertEquals("Latitude", expected.lat(), ll.lat(), 1e-05);
        assertEquals("Longitude", expected.lon(), ll.lon(), 1e-05);

    }

    private void verifyMercatorTile(WMTSTileSource testSource, int x, int y, int z) {
        verifyMercatorTile(testSource, x, y, z, 0);
    }

    private void verifyMercatorTile(WMTSTileSource testSource, int x, int y, int z, int zoomOffset) {
        TemplatedTMSTileSource verifier = new TemplatedTMSTileSource(testImageryTMS);
        LatLon result = new LatLon(testSource.tileXYToLatLon(x, y, z));
        LatLon expected = new LatLon(verifier.tileXYToLatLon(x, y, z + zoomOffset));
        //System.out.println(z + "/" + x + "/" + y + " - result: " + result.toDisplayString() + " osmMercator: " +  expected.toDisplayString());
        assertEquals("Longitude", LatLon.normalizeLon(expected.lon() - result.lon()), 0.0, 1e-04);
        assertEquals("Latitude", expected.lat(), result.lat(), 1e-04);
    }
}
