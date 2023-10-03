// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link AdvancedPreference} class.
 */
class AdvancedPreferenceTest {
    /**
     * Unit test of {@link AdvancedPreference#AdvancedPreference}.
     */
    @Test
    void testAdvancedPreference() {
        assertNotNull(new AdvancedPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link AdvancedPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AdvancedPreference.Factory(), null);
    }
}
