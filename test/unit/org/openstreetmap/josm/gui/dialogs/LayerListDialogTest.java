// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerListModel;
import org.openstreetmap.josm.gui.dialogs.LayerListDialog.LayerVisibilityAction;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.TMSLayerTest;

/**
 * Unit tests of {@link LayerListDialog} class.
 */
public class LayerListDialogTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link LayerVisibilityAction} class.
     */
    @Test
    public void testLayerVisibilityAction() {
        TMSLayer layer = TMSLayerTest.createTmsLayer();
        try {
            LayerListModel model = LayerListDialog.getInstance().getModel();
            LayerVisibilityAction action = new LayerVisibilityAction(model);
            action.updateEnabledState();
            assertFalse(action.isEnabled());

            Main.map.mapView.addLayer(layer);
            action.updateEnabledState();
            assertTrue(action.isEnabled());
            assertTrue(action.supportLayers(model.getSelectedLayers()));

            // now check values
            action.updateValues();
            assertEquals(1.0, action.opacitySlider.getRealValue(), 1e-15);

            action.opacitySlider.setRealValue(.5);
            action.updateValues();

            assertEquals(0.5, action.opacitySlider.getRealValue(), 1e-15);

            action.setVisibleFlag(false);
            action.updateValues();
            assertFalse(layer.isVisible());

            action.setVisibleFlag(true);
            action.updateValues();
            assertTrue(layer.isVisible());

            // layer stays visible during adjust
            action.opacitySlider.setValueIsAdjusting(true);
            action.opacitySlider.setRealValue(0);
            assertEquals(0, layer.getOpacity(), 1e-15);
            layer.setOpacity(.1); // to make layer.isVisible work
            assertTrue(layer.isVisible());
            layer.setOpacity(0);

            action.opacitySlider.setValueIsAdjusting(false);
            action.opacitySlider.setRealValue(0);
            assertEquals(0, layer.getOpacity(), 1e-15);
            layer.setOpacity(.1); // to make layer.isVisible work
            assertFalse(layer.isVisible());
            layer.setOpacity(0);
            action.updateValues();

            // Opacity reset when it was 0 and user set layer to visible.
            action.setVisibleFlag(true);
            action.updateValues();
            assertEquals(1.0, action.opacitySlider.getRealValue(), 1e-15);
            assertEquals(1.0, layer.getOpacity(), 1e-15);

        } finally {
            Main.map.mapView.removeLayer(layer);
        }
    }
}
