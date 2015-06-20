// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;

/**
 * Constants for GPX handling.
 */
public interface GpxConstants {

    /** GPS name of the element. This field will be transferred to and from the GPS.
     *  GPX does not place restrictions on the length of this field or the characters contained in it.
     *  It is up to the receiving application to validate the field before sending it to the GPS. */
    String GPX_NAME = "name";

    /** GPS element comment. Sent to GPS as comment. */
    String GPX_CMT = "cmt";

    /** Text description of the element. Holds additional information about the element intended for the user, not the GPS. */
    String GPX_DESC = "desc";

    /** Source of data. Included to give user some idea of reliability and accuracy of data. */
    String GPX_SRC = "src";

    String META_PREFIX = "meta.";
    String META_AUTHOR_NAME = META_PREFIX + "author.name";
    String META_AUTHOR_EMAIL = META_PREFIX + "author.email";
    String META_AUTHOR_LINK = META_PREFIX + "author.link";
    String META_COPYRIGHT_AUTHOR = META_PREFIX + "copyright.author";
    String META_COPYRIGHT_LICENSE = META_PREFIX + "copyright.license";
    String META_COPYRIGHT_YEAR = META_PREFIX + "copyright.year";
    String META_DESC = META_PREFIX + "desc";
    String META_KEYWORDS = META_PREFIX + "keywords";
    String META_LINKS = META_PREFIX + "links";
    String META_NAME = META_PREFIX + "name";
    String META_TIME = META_PREFIX + "time";
    String META_BOUNDS = META_PREFIX + "bounds";
    String META_EXTENSIONS = META_PREFIX + "extensions";

    String JOSM_EXTENSIONS_NAMESPACE_URI = Main.getXMLBase() + "/gpx-extensions-1.0";

    /** Elevation (in meters) of the point. */
    String PT_ELE = "ele";

    /** Creation/modification timestamp for the point.
     *  Date and time in are in Univeral Coordinated Time (UTC), not local time!
     *  Conforms to ISO 8601 specification for date/time representation.
     *  Fractional seconds are allowed for millisecond timing in tracklogs. */
    String PT_TIME = "time";

    /** Magnetic variation (in degrees) at the point. 0.0 <= value < 360.0 */
    String PT_MAGVAR = "magvar";

    /** Height, in meters, of geoid (mean sea level) above WGS-84 earth ellipsoid. (NMEA GGA message) */
    String PT_GEOIDHEIGHT = "geoidheight";

    /** Text of GPS symbol name. For interchange with other programs, use the exact spelling of the symbol on the GPS, if known. */
    String PT_SYM = "sym";

    /** Type (textual classification) of element. */
    String PT_TYPE = "type";

    /** Type of GPS fix. none means GPS had no fix. Value comes from list: {'none'|'2d'|'3d'|'dgps'|'pps'} */
    String PT_FIX = "fix";

    /** Number of satellites used to calculate the GPS fix. (not number of satellites in view). */
    String PT_SAT = "sat";

    /** Horizontal dilution of precision. */
    String PT_HDOP = "hdop";

    /** Vertical dilution of precision. */
    String PT_VDOP = "vdop";

    /** Position dilution of precision. */
    String PT_PDOP = "pdop";

    /** Number of seconds since last DGPS update. */
    String PT_AGEOFDGPSDATA = "ageofdgpsdata";

    /** Represents a differential GPS station. 0 <= value <= 1023 */
    String PT_DGPSID = "dgpsid";

    /**
     * Ordered list of all possible waypoint keys.
     */
    List<String> WPT_KEYS = Arrays.asList(PT_ELE, PT_TIME, PT_MAGVAR, PT_GEOIDHEIGHT,
            GPX_NAME, GPX_CMT, GPX_DESC, GPX_SRC, META_LINKS, PT_SYM, PT_TYPE,
            PT_FIX, PT_SAT, PT_HDOP, PT_VDOP, PT_PDOP, PT_AGEOFDGPSDATA, PT_DGPSID, META_EXTENSIONS);

    /**
     * Ordered list of all possible route and track keys.
     */
    List<String> RTE_TRK_KEYS = Arrays.asList(
            GPX_NAME, GPX_CMT, GPX_DESC, GPX_SRC, META_LINKS, "number", PT_TYPE, META_EXTENSIONS);

    /**
     * Possible fix values.
     */
    Collection<String> FIX_VALUES = Arrays.asList("none", "2d", "3d", "dgps", "pps");
}
