// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

public class WMSEndpointTileSourceTest {
    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().projection();

    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public WireMockRule tileServer = new WireMockRule(WireMockConfiguration.options().dynamicPort());

    @Test
    public void testDefaultLayerSetInMaps() throws Exception {

        tileServer.stubFor(
                WireMock.get(WireMock.urlEqualTo("/capabilities?SERVICE=WMS&REQUEST=GetCapabilities"))
                .willReturn(
                        WireMock.aResponse()
                        .withBody(Files.readAllBytes(Paths.get(TestUtils.getTestDataRoot() + "wms/geofabrik-osm-inspector.xml")))
                        )
                );

        tileServer.stubFor(WireMock.get(WireMock.urlEqualTo("//maps")).willReturn(WireMock.aResponse().withBody(
                "<?xml version='1.0' encoding='UTF-8'?>\n" +
                "<imagery xmlns=\"http://josm.openstreetmap.de/maps-1.0\">\n" +
                "<entry>\n" +
                "<name>OSM Inspector: Geometry</name>\n" +
                "<id>OSM_Inspector-Geometry</id>\n" +
                "<type>wms_endpoint</type>\n" +
                "<url><![CDATA[" + tileServer.url("/capabilities") + "]]></url>\n" +
                "<icon>data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAACXBIWXMAAAsSAAALEgHS3X78AAAB5UlEQVQ4y4WTwWsTURDGfy8W1yYmXZOqtGJJFyGw6KF7CEigwYuS0kthrYUi4i0iORS9BU9hQdA/ILcixVBrwENKLz1FUBB0wWOwYFAqxUNYTZq6BfM8yC5d05iBObz3vfnmm3kz4sqDh/zP7szdlG5I+Of1zQ1xFA8xxI4GH2cjg4Cl+UUJcC4SJq6c7FPkKRlIoPQk0+NnuDwxHrhvuYd83+8OVuBlHouE/eDXzW8+/qO9DyHB0vyiVHoy2INSNiPdeg23XuPs3icmIoofPKXGmFJjjEUjgf4EFNi2TT6fJ5FI0Gg0ePrkMRfnbvn41QsJgEAJAQUdbYZyuQxAcvoSpmnydesFAF+cn8f2KUCw/fGt6GgzWJbF706bVCoFwGxyktnk5N8kB79QepL1zQ3xbOulCJWyGbkQHZWlbEZ6JIZhBDI1nQ5Np8P2zi4t9zAwGyNe3QALti11XSedTvsPYrEY73f3Bk+irusAnI6qrNy7z43sNUbFCQC6LYdCoYBbr/k1/2sh690HUalUaH7eIRxXA+6RFItF3HqN6+dP9REIb5lK2Yy0bdsHDMMgl8vRbTkAhOMqlmVhmibLq2ui7xsf1d+IV+0D3zVNw7KsPiXVapXnd2/Lodu4vLomTNMcSvIHY6bDkqJtEqIAAAAASUVORK5CYII=</icon>\n" +
                "<attribution-text mandatory=\"true\">© Geofabrik GmbH, OpenStreetMap contributors, CC-BY-SA</attribution-text>\n" +
                "<attribution-url>http://tools.geofabrik.de/osmi/</attribution-url>\n" +
                "<max-zoom>18</max-zoom>\n" +
                "<valid-georeference>true</valid-georeference>\n" +
                "<defaultLayers>" +
                "<layer name=\"single_node_in_way\" style=\"default\" />" +
                "</defaultLayers>" +
                "</entry>\n" +
                "</imagery>"
                )));

        Config.getPref().putList("imagery.layers.sites", Arrays.asList(tileServer.url("//maps")));
        ImageryLayerInfo.instance.loadDefaults(true, null, false);
        assertEquals(1, ImageryLayerInfo.instance.getDefaultLayers().size());
        ImageryInfo wmsImageryInfo = ImageryLayerInfo.instance.getDefaultLayers().get(0);
        assertEquals("single_node_in_way", wmsImageryInfo.getDefaultLayers().get(0).getLayerName());
        WMSEndpointTileSource tileSource = new WMSEndpointTileSource(wmsImageryInfo, Main.getProjection());
        tileSource.initProjection(Projections.getProjectionByCode("EPSG:3857"));
        assertEquals("https://tools.geofabrik.de/osmi/views/geometry/wxs?FORMAT=image/png&TRANSPARENT=TRUE&VERSION=1.1.1&SERVICE=WMS&REQUEST=GetMap&"
                + "LAYERS=single_node_in_way&STYLES=default&"
                + "SRS=EPSG:3857&WIDTH=512&HEIGHT=512&"
                + "BBOX=20037506.6204108,-60112521.5836107,60112521.5836107,-20037506.6204108", tileSource.getTileUrl(1, 1, 1));
    }
}
