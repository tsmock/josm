//License: GPL. Copyright 2008 by Christoph Brill

package org.openstreetmap.josm.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxTrack;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Read a nmea file. Based on information from
 * http://www.kowoma.de/gps/zusatzerklaerungen/NMEA.htm
 *
 * @author cbrill
 */
public class NmeaReader {

    /** Handler for the different types that NMEA speaks. */
    public static enum NMEA_TYPE {

        /** RMC = recommended minimum sentence C. */
        GPRMC("$GPRMC"),
        /** GPS positions. */
        GPGGA("$GPGGA"),
        /** SA = satellites active. */
        GPGSA("$GPGSA"),
        /** Course over ground and ground speed */
        GPVTG("$GPVTG");

        private final String type;

        NMEA_TYPE(String type) {
            this.type = type;
        }

        public String getType() {
            return this.type;
        }

        public boolean equals(String type) {
            return this.type.equals(type);
        }
    }

    // GPVTG
    public static enum GPVTG {
        COURSE(1),COURSE_REF(2), // true course
        COURSE_M(3), COURSE_M_REF(4), // magnetic course
        SPEED_KN(5), SPEED_KN_UNIT(6), // speed in knots
        SPEED_KMH(7), SPEED_KMH_UNIT(8), // speed in km/h
        REST(9); // version-specific rest

        public final int position;

        GPVTG(int position) {
            this.position = position;
        }
    }

    // The following only applies to GPRMC
    public static enum GPRMC {
        TIME(1),
        /** Warning from the receiver (A = data ok, V = warning) */
        RECEIVER_WARNING(2),
        WIDTH_NORTH(3), WIDTH_NORTH_NAME(4), // Latitude, NS
        LENGTH_EAST(5), LENGTH_EAST_NAME(6), // Longitude, EW
        SPEED(7), COURSE(8), DATE(9),           // Speed in knots
        MAGNETIC_DECLINATION(10), UNKNOWN(11),  // magnetic declination
        /**
         * Mode (A = autonom; D = differential; E = estimated; N = not valid; S
         * = simulated)
         *
         * @since NMEA 2.3
         */
        MODE(12);

        public final int position;

        GPRMC(int position) {
            this.position = position;
        }
    }

    // The following only applies to GPGGA
    public static enum GPGGA {
        TIME(1), LATITUDE(2), LATITUDE_NAME(3), LONGITUDE(4), LONGITUDE_NAME(5),
        /**
         * Quality (0 = invalid, 1 = GPS, 2 = DGPS, 6 = estimanted (@since NMEA
         * 2.3))
         */
        QUALITY(6), SATELLITE_COUNT(7),
        HDOP(8), // HDOP (horizontal dilution of precision)
        HEIGHT(9), HEIGHT_UNTIS(10), // height above NN (above geoid)
        HEIGHT_2(11), HEIGHT_2_UNTIS(12), // height geoid - height ellipsoid (WGS84)
        GPS_AGE(13),// Age of differential GPS data
        REF(14); // REF station

        public final int position;
        GPGGA(int position) {
            this.position = position;
        }
    }

    public static enum GPGSA {
        AUTOMATIC(1),
        FIX_TYPE(2), // 1 = not fixed, 2 = 2D fixed, 3 = 3D fixed)
        // PRN numbers for max 12 satellites
        PRN_1(3), PRN_2(4), PRN_3(5), PRN_4(6), PRN_5(7), PRN_6(8),
        PRN_7(9), PRN_8(10), PRN_9(11), PRN_10(12), PRN_11(13), PRN_12(14),
        PDOP(15),   // PDOP (precision)
        HDOP(16),   // HDOP (horizontal precision)
        VDOP(17), ; // VDOP (vertical precision)

        public final int position;
        GPGSA(int position) {
            this.position = position;
        }
    }

    public GpxData data;

//  private final static SimpleDateFormat GGATIMEFMT =
//      new SimpleDateFormat("HHmmss.SSS");
    private final static SimpleDateFormat RMCTIMEFMT =
        new SimpleDateFormat("ddMMyyHHmmss.SSS");

    // functons for reading the error stats
    public NMEAParserState ps;

    public int getParserUnknown() {
        return ps.unknown;
    }
    public int getParserZeroCoordinates() {
        return ps.zero_coord;
    }
    public int getParserChecksumErrors() {
        return ps.checksum_errors;
    }
    public int getParserMalformed() {
        return ps.malformed;
    }
    public int getNumberOfCoordinates() {
        return ps.success;
    }

    public NmeaReader(InputStream source, File relativeMarkerPath) {

        // create the data tree
        data = new GpxData();
        GpxTrack currentTrack = new GpxTrack();
        data.tracks.add(currentTrack);

        try {
            BufferedReader rd =
                new BufferedReader(new InputStreamReader(source));

            StringBuffer sb = new StringBuffer(1024);
            int loopstart_char = rd.read();
            if(loopstart_char == -1) {// zero size file
                //TODO tell user about the problem?
                return;
            }
            sb.append((char)loopstart_char);
            ps = new NMEAParserState();
            ps.p_Date="010100"; // TODO date problem
            while(true) {
                // don't load unparsable files completely to memory
                if(sb.length()>=1020) sb.delete(0, sb.length()-1);
                int c = rd.read();
                if(c=='$') {
                    ParseNMEASentence(sb.toString(), ps);
                    sb.delete(0, sb.length());
                    sb.append('$');
                } else if(c == -1) {
                    // EOF: add last WayPoint if it works out
                    ParseNMEASentence(sb.toString(),ps);
                    break;
                } else sb.append((char)c);
            }
            rd.close();
            Object[] wparr = ps.waypoints.toArray();
            currentTrack.trackSegs.add(ps.waypoints);
            data.recalculateBounds();

        } catch (final IOException e) {
            // TODO tell user about the problem?
        }
    }
    private class NMEAParserState {
        protected Collection<WayPoint> waypoints = new ArrayList<WayPoint>();
        protected String p_Time;
        protected String p_Date;
        protected WayPoint p_Wp;

        protected int success = 0; // number of successfully parsend sentences
        protected int malformed = 0;
        protected int checksum_errors = 0;
        protected int unknown = 0;
        protected int zero_coord = 0;
    }

    // Parses split up sentences into WayPoints which are stored
    // in the collection in the NMEAParserState object.
    // Returns true if the input made sence, false otherwise.
    private boolean ParseNMEASentence(String s, NMEAParserState ps) {
        try {
            if(s.equals("")) throw(null);

            // checksum check:
            // the bytes between the $ and the * are xored;
            // if there is no * or other meanities it will throw
            // and result in a malformed packet.
            String[] chkstrings = s.split("\\*");
            byte[] chb = chkstrings[0].getBytes();
            int chk=0;
            for(int i = 1; i < chb.length; i++) chk ^= chb[i];
            if(Integer.parseInt(chkstrings[1].substring(0,2),16) != chk) {
                //System.out.println("Checksum error");
                ps.checksum_errors++;
                ps.p_Wp=null;
                return false;
            }
            // now for the content
            String[] e = chkstrings[0].split(",");
            String accu;

            WayPoint currentwp = ps.p_Wp;
            String currentDate = ps.p_Date;

            // handle the packet content
            if(e[0].equals("$GPGGA")) {
                // Position
                LatLon latLon = parseLatLon(
                        e[GPGGA.LATITUDE_NAME.position],
                        e[GPGGA.LONGITUDE_NAME.position],
                        e[GPGGA.LATITUDE.position],
                        e[GPGGA.LONGITUDE.position]
                    );
                if(latLon==null) throw(null); // malformed

                if((latLon.lat()==0.0) && (latLon.lon()==0.0)) {
                    ps.zero_coord++;
                    return false;
                }

                // time
                accu = e[GPGGA.TIME.position];
                Date d = RMCTIMEFMT.parse(currentDate+accu, new ParsePosition(0));
                if (d == null) throw(null); // malformed

                if((ps.p_Time==null) || (currentwp==null) || !ps.p_Time.equals(accu)) {
                    // this node is newer than the previous, create a new waypoint.
                    // no matter if previous WayPoint was null, we got something
                    // better now.
                    ps.p_Time=accu;
                    currentwp = new WayPoint(latLon);
                }
                if(!currentwp.attr.containsKey("time")) {
                    // As this sentence has no complete time only use it
                    // if there is no time so far
                    String gpxdate = WayPoint.GPXTIMEFMT.format(d);
                    currentwp.attr.put("time", gpxdate);
                }
                // elevation
                accu=e[GPGGA.HEIGHT_UNTIS.position];
                if(accu.equals("M")) {
                        // Ignore heights that are not in meters for now
                        accu=e[GPGGA.HEIGHT.position];
                        if(!accu.equals("")) {
                        Double.parseDouble(accu);
                        // if it throws it's malformed; this should only happen if the
                        // device sends nonstandard data.
                        if(!accu.equals("")) currentwp.attr.put("ele", accu);
                    }
                }
                // number of sattelites
                accu=e[GPGGA.SATELLITE_COUNT.position];
                int sat = 0;
                if(!accu.equals("")) {
                    sat = Integer.parseInt(accu);
                    currentwp.attr.put("sat", accu);
                }
                // h-dilution
                accu=e[GPGGA.HDOP.position];
                if(!accu.equals("")) {
                    Double.parseDouble(accu);
                    currentwp.attr.put("hdop", accu);
                }
                // fix
                accu=e[GPGGA.QUALITY.position];
                if(!accu.equals("")) {
                    int fixtype = Integer.parseInt(accu);
                    switch(fixtype) {
                    case 0:
                        currentwp.attr.put("fix", "none");
                        break;
                    case 1:
                        if(sat < 4) currentwp.attr.put("fix", "2d");
                        else currentwp.attr.put("fix", "3d");
                        break;
                    case 2:
                        currentwp.attr.put("fix", "dgps");
                        break;
                    default:
                        break;
                    }
                }
            } else if(e[0].equals("$GPVTG")) {
                // COURSE
                accu = e[GPVTG.COURSE_REF.position];
                if(accu.equals("T")) {
                    // other values than (T)rue are ignored
                    accu = e[GPVTG.COURSE.position];
                    if(!accu.equals("")) {
                        Double.parseDouble(accu);
                        currentwp.attr.put("course", accu);
                    }
                }
                // SPEED
                accu = e[GPVTG.SPEED_KMH_UNIT.position];
                if(accu.startsWith("K")) {
                    accu = e[GPVTG.SPEED_KMH.position];
                    if(!accu.equals("")) {
                        double speed = Double.parseDouble(accu);
                        speed /= 3.6; // speed in m/s
                        currentwp.attr.put("speed", Double.toString(speed));
                    }
                }
            } else if(e[0].equals("$GPGSA")) {
                // vdop
                accu=e[GPGSA.VDOP.position];
                if(!accu.equals("")) {
                    Double.parseDouble(accu);
                    currentwp.attr.put("vdop", accu);
                }
                // hdop
                accu=e[GPGSA.HDOP.position];
                if(!accu.equals("")) {
                    Double.parseDouble(accu);
                    currentwp.attr.put("hdop", accu);
                }
                // pdop
                accu=e[GPGSA.PDOP.position];
                if(!accu.equals("")) {
                    Double.parseDouble(accu);
                    currentwp.attr.put("pdop", accu);
                }
            }
            else if(e[0].equals("$GPRMC")) {
                // coordinates
                LatLon latLon = parseLatLon(
                        e[GPRMC.WIDTH_NORTH_NAME.position],
                        e[GPRMC.LENGTH_EAST_NAME.position],
                        e[GPRMC.WIDTH_NORTH.position],
                        e[GPRMC.LENGTH_EAST.position]
                    );
                if(latLon==null) throw(null);
                if((latLon.lat()==0.0) && (latLon.lon()==0.0)) {
                    ps.zero_coord++;
                    return false;
                }
                // time
                currentDate = e[GPRMC.DATE.position];
                String time = e[GPRMC.TIME.position];

                Date d = RMCTIMEFMT.parse(currentDate+time, new ParsePosition(0));
                if (d == null) throw(null);

                if((ps.p_Time==null) || (currentwp==null) || !ps.p_Time.equals(time)) {
                    // this node is newer than the previous, create a new waypoint.
                    ps.p_Time=time;
                    currentwp = new WayPoint(latLon);
                }
                // time: this sentence has complete time so always use it.
                String gpxdate = WayPoint.GPXTIMEFMT.format(d);
                currentwp.attr.put("time", gpxdate);
                // speed
                accu = e[GPRMC.SPEED.position];
                if(!accu.equals("") && !currentwp.attr.containsKey("speed")) {
                    double speed = Double.parseDouble(accu);
                    speed *= 0.514444444; // to m/s
                    currentwp.attr.put("speed", Double.toString(speed));
                }
                // course
                accu = e[GPRMC.COURSE.position];
                if(!accu.equals("") && !currentwp.attr.containsKey("course")) {
                    Double.parseDouble(accu);
                    currentwp.attr.put("course", accu);
                }

                // TODO fix?
                // * Mode (A = autonom; D = differential; E = estimated; N = not valid; S
                // * = simulated)
                // *
                // * @since NMEA 2.3
                //
                //MODE(12);
            } else {
                ps.unknown++;
                return false;
            }
            ps.p_Date = currentDate;
            if(ps.p_Wp != currentwp) {
                if(ps.p_Wp!=null) {
                    ps.p_Wp.setTime();
                }
                ps.p_Wp = currentwp;
                ps.waypoints.add(currentwp);
                ps.success++;
                return true;
            }
            return true;

        } catch(Exception x) {
            // out of bounds and such
            //System.out.println("Malformed line: "+s.toString().trim());
            ps.malformed++;
            ps.p_Wp=null;
            return false;
        }
    }

    private LatLon parseLatLon(String ew, String ns, String dlat, String dlon)
        throws NumberFormatException {
        String widthNorth = dlat.trim();
        String lengthEast = dlon.trim();

        // return a zero latlon instead of null so it is logged as zero coordinate
        // instead of malformed sentence
        if(widthNorth.equals("")&&lengthEast.equals("")) return new LatLon(0.0,0.0);

        // The format is xxDDLL.LLLL
        // xx optional whitespace
        // DD (int) degres
        // LL.LLLL (double) latidude
        int latdegsep = widthNorth.indexOf('.') - 2;
        if (latdegsep < 0) return null;

        int latdeg = Integer.parseInt(widthNorth.substring(0, latdegsep));
        double latmin = Double.parseDouble(widthNorth.substring(latdegsep));
        double lat = latdeg + latmin / 60;
        if ("S".equals(ns)) {
            if(!ew.equals("N")) return null;
            lat = -lat;
        }

        int londegsep = lengthEast.indexOf('.') - 2;
        if (londegsep < 0) return null;

        int londeg = Integer.parseInt(lengthEast.substring(0, londegsep));
        double lonmin = Double.parseDouble(lengthEast.substring(londegsep));
        double lon = londeg + lonmin / 60;
        if ("W".equals(ew)) {
            if(!ew.equals("E")) return null;
            lon = -lon;
        }
        return new LatLon(lat, lon);
    }
}
