// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.beans.PropertyChangeEvent;

import javax.swing.JPanel;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.io.SaveLayersModel.Mode;

/**
 * Unit tests of {@link UploadAndSaveProgressRenderer} class.
 */
public class UploadAndSaveProgressRendererTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link UploadAndSaveProgressRenderer#UploadAndSaveProgressRenderer}.
     */
    @Test
    public void testUploadAndSaveProgressRenderer() {
        JPanel parent = new JPanel();
        UploadAndSaveProgressRenderer r = new UploadAndSaveProgressRenderer();
        parent.add(r);
        r.setCustomText(null);
        r.setIndeterminate(true);
        r.setMaximum(10);
        r.setTaskTitle(null);
        r.setValue(5);
        r.propertyChange(new PropertyChangeEvent(this, "", null, null));
        r.propertyChange(new PropertyChangeEvent(this, SaveLayersModel.MODE_PROP, null, Mode.UPLOADING_AND_SAVING));
        assertTrue(r.isVisible());
        r.propertyChange(new PropertyChangeEvent(this, SaveLayersModel.MODE_PROP, null, Mode.EDITING_DATA));
        assertFalse(r.isVisible());
    }
}
