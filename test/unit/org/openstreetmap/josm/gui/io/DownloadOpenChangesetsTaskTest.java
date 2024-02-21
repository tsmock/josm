// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.gui.oauth.OAuthAuthorizationWizard;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.OsmApi;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;
import org.openstreetmap.josm.testutils.mockers.WindowMocker;
import org.openstreetmap.josm.tools.UserCancelException;

import mockit.Invocation;
import mockit.Mock;
import mockit.MockUp;

/**
 * Unit tests of {@link DownloadOpenChangesetsTask} class.
 */
@BasicPreferences
@OsmApi(OsmApi.APIType.DEV)
class DownloadOpenChangesetsTaskTest {
    /**
     * OAuth wizard mocker.
     */
    public static class OAuthWizardMocker extends MockUp<OAuthAuthorizationWizard> {
        /** {@code true} if wizard has been called */
        public boolean called;

        @Mock
        void showDialog() throws UserCancelException {
            this.called = true;
            throw new UserCancelException();
        }

        @Mock
        void obtainAccessToken(final Invocation invocation, final URL serverUrl) {
            if (GraphicsEnvironment.isHeadless()) {
                // we can't really let execution proceed any further as construction of the ui
                // elements will fail with a mocked Window
                this.called = true;
                return;
            }
            // else we can allow a bit more of the code to be covered before we raise
            // UserCancelException in showDialog
            invocation.proceed(serverUrl);
        }
    }

    /**
     * Test of {@link DownloadOpenChangesetsTask} class when anonymous.
     */
    @Test
    void testAnonymous() {
        TestUtils.assumeWorkingJMockit();
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        final Map<String, Object> optionPaneMock = new HashMap<>(2);
        optionPaneMock.put("<html>Could not retrieve the list of your open changesets because<br>JOSM does not know "
                + "your identity.<br>You have either chosen to work anonymously or you are not "
                + "entitled<br>to know the identity of the user on whose behalf you are working.</html>", JOptionPane.OK_OPTION);
        optionPaneMock.put("Obtain OAuth 2.0 token for authentication?", JOptionPane.NO_OPTION);
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(optionPaneMock);

        DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(new JPanel());
        assertNull(task.getChangesets());

        assertTrue(UserIdentityManager.getInstance().isAnonymous());
        task.run();
        assertNull(task.getChangesets());

        assertEquals(2, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(1);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Missing user identity", invocationLogEntry[2]);

        invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.NO_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Obtain authentication to OSM servers", invocationLogEntry[2]);
    }

    /**
     * Test of {@link DownloadOpenChangesetsTask} class when "partially identified".
     */
    @Test
    void testPartiallyIdentified() {
        TestUtils.assumeWorkingJMockit();
        if (GraphicsEnvironment.isHeadless()) {
            new WindowMocker();
        }
        final Map<String, Object> optionPaneMock = new HashMap<>(2);
        optionPaneMock.put("There are no open changesets", JOptionPane.OK_OPTION);
        optionPaneMock.put("Obtain OAuth 2.0 token for authentication?", JOptionPane.NO_OPTION);
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(optionPaneMock);

        DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(new JPanel());
        UserIdentityManager.getInstance().setPartiallyIdentified(System.getProperty("osm.username", "josm_test"));
        assertTrue(UserIdentityManager.getInstance().isPartiallyIdentified());
        task.run();
        assertNotNull(task.getChangesets());

        assertEquals(2, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(1);
        assertEquals(JOptionPane.OK_OPTION, (int) invocationLogEntry[0]);
        assertEquals("No open changesets", invocationLogEntry[2]);

        invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(JOptionPane.NO_OPTION, (int) invocationLogEntry[0]);
        assertEquals("Obtain authentication to OSM servers", invocationLogEntry[2]);
    }
}
