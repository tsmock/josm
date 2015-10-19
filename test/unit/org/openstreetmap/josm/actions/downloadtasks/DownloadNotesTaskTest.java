// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.osm.NoteData;

/**
 * Unit tests for class {@link DownloadNotesTask}.
 */
public class DownloadNotesTaskTest {

    private static final String REMOTE_FILE = "https://josm.openstreetmap.de/export/head/josm/trunk/test/data/planet-notes-extract.osn";

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@code DownloadNotesTask#acceptsUrl} method.
     */
    @Test
    public void testAcceptsURL() {
        DownloadNotesTask task = new DownloadNotesTask();
        assertFalse(task.acceptsUrl(null));
        assertFalse(task.acceptsUrl(""));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes.json?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes.xml?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes.gpx?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl(REMOTE_FILE));
    }

    /**
     * Unit test of {@code DownloadNotesTask#loadUrl} method with an external file.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    public void testDownloadExternalFile() throws InterruptedException, ExecutionException {
        DownloadNotesTask task = new DownloadNotesTask();
        task.loadUrl(false, REMOTE_FILE, null).get();
        NoteData data = task.getDownloadedData();
        assertNotNull(data);
        assertFalse(data.getNotes().isEmpty());
    }
}
