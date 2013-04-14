// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.gui.util.GuiHelper;

public class OsmImporter extends FileImporter {

    public static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
            "osm,xml", "osm", tr("OSM Server Files") + " (*.osm *.xml)");
    
    public static class OsmImporterData {

        private OsmDataLayer layer;
        private Runnable postLayerTask;

        public OsmImporterData(OsmDataLayer layer, Runnable postLayerTask) {
            this.layer = layer;
            this.postLayerTask = postLayerTask;
        }

        public OsmDataLayer getLayer() {
            return layer;
        }

        public Runnable getPostLayerTask() {
            return postLayerTask;
        }
    }

    public OsmImporter() {
        super(FILE_FILTER);
    }

    public OsmImporter(ExtensionFileFilter filter) {
        super(filter);
    }

    /**
     * Imports OSM data from file @param file
     * This method supports progress monitoring and canceling by using @param progressMonitor
     */
    @Override
    public void importData(File file, ProgressMonitor progressMonitor) throws IOException, IllegalDataException {
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            importData(in, file, progressMonitor);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new IOException(tr("File ''{0}'' does not exist.", file.getName()));
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * Imports OSM data from stream @param in , sitle will be generated from name of file @param associatedFile
     */
    protected void importData(InputStream in, final File associatedFile) throws IllegalDataException {
        importData(in, associatedFile, NullProgressMonitor.INSTANCE);
    }
    
    /**
     * Imports OSM data from stream @param in , layer name will be generated from name of file @param associatedFile
     * This method supports progress monitoring and canceling by using @param progressMonitor
     */
    protected void importData(InputStream in, final File associatedFile, ProgressMonitor pm) throws IllegalDataException {
        final OsmImporterData data = loadLayer(in, associatedFile,
                associatedFile == null ? OsmDataLayer.createNewName() : associatedFile.getName(), pm);

        // FIXME: remove UI stuff from IO subsystem
        GuiHelper.runInEDT(new Runnable() {
            @Override
            public void run() {
                Main.main.addLayer(data.layer);
                data.postLayerTask.run();
                data.layer.onPostLoadFromFile();
            }
        });
    }

    /**
     * Load osm data layer from InputStream.
     * associatedFile can be null if the stream does not come from a file.
     */
    public OsmImporterData loadLayer(InputStream in, final File associatedFile, final String layerName, ProgressMonitor progressMonitor) throws IllegalDataException {
        final DataSet dataSet = parseDataSet(in, progressMonitor);
        if (dataSet == null) {
            throw new IllegalDataException(tr("Invalid dataset"));
        }
        OsmDataLayer layer = createLayer(dataSet, associatedFile, layerName);
        Runnable postLayerTask = createPostLayerTask(dataSet, associatedFile, layerName, layer);
        return new OsmImporterData(layer, postLayerTask);
    }

    protected DataSet parseDataSet(InputStream in, ProgressMonitor progressMonitor) throws IllegalDataException {
        return OsmReader.parseDataSet(in, progressMonitor);
    }

    protected OsmDataLayer createLayer(final DataSet dataSet, final File associatedFile, final String layerName) {
        return new OsmDataLayer(dataSet, layerName, associatedFile);
    }

    protected Runnable createPostLayerTask(final DataSet dataSet, final File associatedFile, final String layerName, final OsmDataLayer layer) {
        return new Runnable() {
            @Override
            public void run() {
                if (dataSet.allPrimitives().isEmpty()) {
                    String msg;
                    if (associatedFile == null) {
                        msg = tr("No data found for layer ''{0}''.", layerName);
                    } else {
                        msg = tr("No data found in file ''{0}''.", associatedFile.getPath());
                    }
                    JOptionPane.showMessageDialog(
                            Main.parent,
                            msg,
                            tr("Open OSM file"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
                layer.onPostLoadFromFile();
            }
        };
    }
}
