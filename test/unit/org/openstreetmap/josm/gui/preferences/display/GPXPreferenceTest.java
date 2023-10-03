// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link GPXPreference} class.
 */
class GPXPreferenceTest {
    /**
     * Unit test of {@link GPXPreference.Factory}.
     */
    @Test
    void testGPXPreference() {
        assertNotNull(new GPXPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link GPXPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new GPXPreference.Factory(), null);
    }
}
