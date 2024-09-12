// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openstreetmap.josm.testutils.ImageTestUtils.assertImageEquals;

import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.io.IllegalDataException;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Test cases for {@link StyledMapRenderer} and the MapCSS classes.
 * <p>
 * This test uses the data and reference files stored in the test data directory {@value #TEST_DATA_BASE}
 * @author Michael Zangl
 */
@BasicPreferences
@Projection
public class MapCSSRendererTest {
    private static final String TEST_DATA_BASE = "/renderer/";
    /**
     * lat = 0..1, lon = 0..1
     */
    private static final Bounds AREA_DEFAULT = new Bounds(0, 0, 1, 1);
    private static final int IMAGE_SIZE = 256;

    /**
     * The different configurations of this test.
     *
     * @return The parameters.
     */
    public static Collection<Object[]> runs() {
        return Stream.of(
                /* Tests for StyledMapRenderer#drawNodeSymbol */
                new TestConfig("node-shapes", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Text for nodes */
                new TestConfig("node-text", AREA_DEFAULT).usesFont("DejaVu Sans")
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests that StyledMapRenderer#drawWay respects width */
                new TestConfig("way-width", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests the way color property, including alpha */
                new TestConfig("way-color", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests dashed ways. */
                new TestConfig("way-dashes", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests dashed way clamping algorithm */
                new TestConfig("way-dashes-clamp", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests fill-color property */
                new TestConfig("area-fill-color", AREA_DEFAULT),

                /* Tests the fill-image property. */
                new TestConfig("area-fill-image", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests area label drawing/placement */
                new TestConfig("area-text", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests area icon drawing/placement */
                new TestConfig("area-icon", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests if all styles are sorted correctly. Tests {@link StyleRecord#compareTo(StyleRecord)} */
                new TestConfig("order", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests repeat-image feature for ways */
                new TestConfig("way-repeat-image", AREA_DEFAULT)
                        .setThresholdPixels(2100).setThresholdTotalColorDiff(93_000),
                /* Tests the clamping for repeat-images and repeat-image-phase */
                new TestConfig("way-repeat-image-clamp", AREA_DEFAULT)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests text along a way */
                new TestConfig("way-text", AREA_DEFAULT)
                        .setThresholdPixels(3400).setThresholdTotalColorDiff(0),

                /* Another test for node shapes */
                new TestConfig("node-shapes2").setImageWidth(600)
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),
                /* Tests default values for node shapes */
                new TestConfig("node-shapes-default")
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),
                /* Tests node shapes with both fill and stroke combined */
                new TestConfig("node-shapes-combined")
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),
                /* Another test for dashed ways */
                new TestConfig("way-dashes2")
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),
                /* Tests node text placement */
                new TestConfig("node-text2")
                        .setThresholdPixels(1020).setThresholdTotalColorDiff(0),
                /* Tests relation link selector */
                new TestConfig("relation-linkselector")
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),
                /* Tests parent selector on relation */
                new TestConfig("relation-parentselector")
                        .setThresholdPixels(0).setThresholdTotalColorDiff(0),

                /* Tests evaluation of expressions */
                new TestConfig("eval").setImageWidth(600)
                        .setThresholdPixels(6610).setThresholdTotalColorDiff(0)

                ).map(e -> new Object[] {e, e.testDirectory})
                .collect(Collectors.toList());
    }

    /**
     * Run the test using {@code testConfig}
     * @param testConfig The config to use for this test.
     * @param ignored The name to print it nicely
     * @throws Exception if an error occurs
     */
    @ParameterizedTest(name = "{1}")
    @MethodSource("runs")
    void testRender(TestConfig testConfig, String ignored) throws Exception {
        List<String> fonts = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String font : testConfig.fonts) {
            assumeTrue(fonts.contains(font), "Test requires font: " + font);
        }

        // Force reset of preferences
        StyledMapRenderer.PREFERENCE_ANTIALIASING_USE.put(true);
        StyledMapRenderer.PREFERENCE_TEXT_ANTIALIASING.put("gasp");

        // load the data
        DataSet dataSet = testConfig.getOsmDataSet();
        dataSet.allPrimitives().forEach(this::loadPrimitiveStyle);
        dataSet.setSelected(dataSet.allPrimitives().stream().filter(n -> n.isKeyTrue("selected")).collect(Collectors.toList()));

        ProjectionBounds pb = new ProjectionBounds();
        pb.extend(ProjectionRegistry.getProjection().latlon2eastNorth(testConfig.getTestArea().getMin()));
        pb.extend(ProjectionRegistry.getProjection().latlon2eastNorth(testConfig.getTestArea().getMax()));
        double scale = (pb.maxEast - pb.minEast) / testConfig.imageWidth;

        RenderingHelper.StyleData sd = new RenderingHelper.StyleData();
        sd.styleUrl = testConfig.getStyleSourceUrl();
        RenderingHelper rh = new RenderingHelper(dataSet, testConfig.getTestArea(), scale, Collections.singleton(sd));
        rh.setFillBackground(false);
        rh.setDebugStream(System.out);
        System.out.println("Running " + getClass() + "[" + testConfig.testDirectory + "]");
        BufferedImage image = rh.render();

        assertImageEquals(testConfig.testDirectory,
                testConfig.getReference(), image,
                testConfig.thresholdPixels, testConfig.thresholdTotalColorDiff, diffImage -> {
                    try {
                        // You can use this to debug:
                        ImageIO.write(image, "png", new File(testConfig.getTestDirectory() + "/test-output.png"));
                        ImageIO.write(diffImage, "png", new File(testConfig.getTestDirectory() + "/test-differences.png"));
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });
    }

    private void loadPrimitiveStyle(OsmPrimitive n) {
        n.setHighlighted(n.isKeyTrue("highlight"));
        if (n.isKeyTrue("disabled")) {
            n.setDisabledState(false);
        }
    }

    private static class TestConfig {
        private final String testDirectory;
        private Bounds testArea;
        private final ArrayList<String> fonts = new ArrayList<>();
        private DataSet ds;
        private int imageWidth = IMAGE_SIZE;
        private int thresholdPixels;
        private int thresholdTotalColorDiff;

        TestConfig(String testDirectory, Bounds testArea) {
            this.testDirectory = testDirectory;
            this.testArea = testArea;
        }

        TestConfig(String testDirectory) {
            this.testDirectory = testDirectory;
        }

        public TestConfig setImageWidth(int imageWidth) {
            this.imageWidth = imageWidth;
            return this;
        }

        /**
         * Set the number of pixels that can differ.
         * <p>
         * Needed due to somewhat platform dependent font rendering.
         * @param thresholdPixels the number of pixels that can differ
         * @return this object, for convenience
         */
        public TestConfig setThresholdPixels(int thresholdPixels) {
            this.thresholdPixels = thresholdPixels;
            return this;
        }

        /**
         * Set the threshold for total color difference.
         * Every difference in any color component (and alpha) will be added up and must not exceed this threshold.
         * Needed due to somewhat platform dependent font rendering.
         * @param thresholdTotalColorDiff he threshold for total color difference
         * @return this object, for convenience
         */
        public TestConfig setThresholdTotalColorDiff(int thresholdTotalColorDiff) {
            this.thresholdTotalColorDiff = thresholdTotalColorDiff;
            return this;
        }

        public TestConfig usesFont(String string) {
            this.fonts.add(string);
            return this;
        }

        public File getReference() {
            // Sometimes Java changes how things are rendered. When that happens, use separate reference files. It is
            // usually "reference" + javaSuffix + ".png".
            final File customReferenceFile = new File(getTestDirectory() + "/reference-java" + Utils.getJavaVersion() + ".png");
            if (customReferenceFile.isFile()) {
                return customReferenceFile;
            }
            return new File(getTestDirectory() + "/reference.png");
        }

        private String getTestDirectory() {
            return TestUtils.getTestDataRoot() + TEST_DATA_BASE + testDirectory;
        }

        public String getStyleSourceUrl() {
            return getTestDirectory() + "/style.mapcss";
        }

        public DataSet getOsmDataSet() throws IllegalDataException, IOException {
            if (ds == null) {
                ds = OsmReader.parseDataSet(Files.newInputStream(Paths.get(getTestDirectory(), "data.osm")), null);
            }
            return ds;
        }

        public Bounds getTestArea() throws IllegalDataException, IOException {
            if (testArea == null) {
                testArea = getOsmDataSet().getDataSourceBounds().get(0);
            }
            return testArea;
        }

        @Override
        public String toString() {
            return "TestConfig [testDirectory=" + testDirectory + ", testArea=" + testArea + ']';
        }
    }
}
