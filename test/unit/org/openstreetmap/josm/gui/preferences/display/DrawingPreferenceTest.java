// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link DrawingPreference} class.
 */
class DrawingPreferenceTest {
    /**
     * Unit test of {@link DrawingPreference#DrawingPreference}.
     */
    @Test
    void testDrawingPreference() {
        assertNotNull(new DrawingPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link DrawingPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new DrawingPreference.Factory(), null);
    }
}
