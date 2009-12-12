// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class PrimitivesRemovedEvent extends AbstractDatasetChangedEvent {

    private final List<? extends OsmPrimitive> primitives;

    public PrimitivesRemovedEvent(DataSet dataSet, Collection<? extends OsmPrimitive> primitives) {
        super(dataSet);
        this.primitives = Collections.unmodifiableList(new ArrayList<OsmPrimitive>(primitives));
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.primtivesRemoved(this);
    }

    public List<? extends OsmPrimitive> getPrimitives() {
        return primitives;
    }

}
