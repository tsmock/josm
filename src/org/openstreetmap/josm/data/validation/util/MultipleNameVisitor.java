// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.util;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Able to create a name and an icon for a collection of elements.
 *
 * @author frsantos
 */
public class MultipleNameVisitor extends NameVisitor
{
    public static final int MULTIPLE_NAME_MAX_LENGTH = 80;
    
    /** The class name of the combined primitives */
    private String multipleClassname;
    /* name to be displayed */
    private String displayName;
    /** Size of the collection */
    private int size;

    /**
     * Visits a collection of primitives
     * @param data The collection of primitives
     */
    public void visit(Collection<? extends OsmPrimitive> data) {
        String multipleName = "";
        String multiplePluralClassname = null;
        size = data.size();

        multipleClassname = null;
        for (OsmPrimitive osm : data) {
            String name = osm.get("name");
            if (name == null) {
                name = osm.get("ref");
            }
            if (name != null && !name.isEmpty() && multipleName.length() <= MULTIPLE_NAME_MAX_LENGTH) {
                if (!multipleName.isEmpty()) {
                    multipleName += ", ";
                }
                multipleName += name;
            }

            osm.visit(this);
            if (multipleClassname == null) {
                multipleClassname = className;
                multiplePluralClassname = classNamePlural;
            } else if (!multipleClassname.equals(className)) {
                multipleClassname = "object";
                multiplePluralClassname = trn("object", "objects", 2);
            }
        }

        if (size <= 1) {
            displayName = name;
        } else {
            displayName = size + " " + trn(multipleClassname, multiplePluralClassname, size);
            if (!multipleName.isEmpty()) {
                if (multipleName.length() <= MULTIPLE_NAME_MAX_LENGTH) {
                    displayName += ": " + multipleName;
                } else {
                    displayName += ": " + multipleName.substring(0, MULTIPLE_NAME_MAX_LENGTH) + "...";
                }
            }
        }
    }

    @Override
    public JLabel toLabel() {
        return new JLabel(getText(), getIcon(), JLabel.HORIZONTAL);
    }

    /**
     * Gets the name of the items
     * @return the name of the items
     */
    public String getText() {
        return displayName;
    }

    /**
     * Gets the icon of the items
     * @return the icon of the items
     */
    public Icon getIcon() {
        if (size <= 1)
            return icon;
        else
            return ImageProvider.get("data", multipleClassname);
    }

    @Override
    public String toString() {
        return getText();
    }
}
