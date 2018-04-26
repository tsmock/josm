// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.actions.OrthogonalizeAction;
import org.openstreetmap.josm.actions.OrthogonalizeAction.InvalidUserInputException;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;

/**
 * Checks for buildings with angles close to right angle.
 *
 * @author marxin
 * @since 13670
 */
public class RightAngleBuildingTest extends Test {

    /** Maximum angle difference from right angle that is considered as invalid. */
    protected double maxAngleDelta;

    /** Minimum angle difference from right angle that is considered as invalid. */
    protected double minAngleDelta;

    /**
     * Constructs a new {@code RightAngleBuildingTest} test.
     */
    public RightAngleBuildingTest() {
        super(tr("Almost right angle buildings"),
                tr("Checks for buildings that have angles close to right angle and are not orthogonalized."));
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable() || !w.isClosed() || !isBuilding(w)) return;

        List<Pair<Double, Node>> angles = w.getAngles();
        for (Pair<Double, Node> pair: angles) {
            if (checkAngle(w, pair.a, pair.b)) {
                TestError.Builder builder = TestError.builder(this, Severity.WARNING, 3701)
                                                     .message(tr("Building with an almost square angle"))
                                                     .primitives(w)
                                                     .highlight(pair.b);
                if (angles.stream().noneMatch(
                        p -> Math.abs(p.a - 90) >= maxAngleDelta && Math.abs(p.a - 180) >= minAngleDelta)) {
                    builder.fix(() -> {
                        try {
                            return OrthogonalizeAction.orthogonalize(Collections.singleton(w));
                        } catch (InvalidUserInputException e) {
                            Logging.warn(e);
                            return null;
                        }
                    });
                }
                errors.add(builder.build());
                return;
            }
        }
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        maxAngleDelta = Config.getPref().getDouble("validator.RightAngleBuilding.maximumDelta", 10.0);
        minAngleDelta = Config.getPref().getDouble("validator.RightAngleBuilding.minimumDelta", 0.25);
    }

    private boolean checkAngle(Way w, double angle, Node n) {
        double difference = Math.abs(angle - 90);
        return difference > minAngleDelta && difference < maxAngleDelta;
    }
}
