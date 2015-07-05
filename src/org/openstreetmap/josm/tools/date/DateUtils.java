// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools.date;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A static utility class dealing with:
 * <ul>
 * <li>parsing XML date quickly and formatting a date to the XML UTC format regardless of current locale</li>
 * <li>providing a single entry point for formatting dates to be displayed in JOSM GUI, based on user preferences</li>
 * </ul>
 * @author nenik
 */
public final class DateUtils {

    private DateUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Property to enable display of ISO dates globally.
     * @since 7299
     */
    public static final BooleanProperty PROP_ISO_DATES = new BooleanProperty("iso.dates", false);

    /**
     * A shared instance used for conversion between individual date fields
     * and long millis time. It is guarded against conflict by the class lock.
     * The shared instance is used because the construction, together
     * with the timezone lookup, is very expensive.
     */
    private static GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    private static final DatatypeFactory XML_DATE;

    static {
        calendar.setTimeInMillis(0);

        DatatypeFactory fact = null;
        try {
            fact = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ce) {
            Main.error(ce);
        }
        XML_DATE = fact;
    }

    /**
     * Parses XML date quickly, regardless of current locale.
     * @param str The XML date as string
     * @return The date
     */
    public static synchronized Date fromString(String str) {
        return new Date(tsFromString(str));
    }

    /**
     * Parses XML date quickly, regardless of current locale.
     * @param str The XML date as string
     * @return The date in milliseconds since epoch
     */
    public static synchronized long tsFromString(String str) {
        // "2007-07-25T09:26:24{Z|{+|-}01:00}"
        if (checkLayout(str, "xxxx-xx-xxTxx:xx:xxZ") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx") ||
                checkLayout(str, "xxxx-xx-xx xx:xx:xx UTC") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx+xx:00") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx-xx:00")) {
            calendar.set(
                parsePart4(str, 0),
                parsePart2(str, 5)-1,
                parsePart2(str, 8),
                parsePart2(str, 11),
                parsePart2(str, 14),
                parsePart2(str, 17));

            if (str.length() == 25) {
                int plusHr = parsePart2(str, 20);
                int mul = str.charAt(19) == '+' ? -3600000 : 3600000;
                return calendar.getTimeInMillis()+plusHr*mul;
            }

            return calendar.getTimeInMillis();
        } else if (checkLayout(str, "xxxx-xx-xxTxx:xx:xx.xxxZ") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx.xxx") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx.xxx+xx:00") ||
                checkLayout(str, "xxxx-xx-xxTxx:xx:xx.xxx-xx:00")) {
            calendar.set(
                parsePart4(str, 0),
                parsePart2(str, 5)-1,
                parsePart2(str, 8),
                parsePart2(str, 11),
                parsePart2(str, 14),
                parsePart2(str, 17));
            long millis = parsePart3(str, 20);
            if (str.length() == 29){
                millis += parsePart2(str, 24) * (str.charAt(23) == '+' ? -3600000 : 3600000);
            }

            return calendar.getTimeInMillis() + millis;
        } else {
            // example date format "18-AUG-08 13:33:03"
            SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yy HH:mm:ss");
            Date d = f.parse(str, new ParsePosition(0));
            if (d != null)
                return d.getTime();
        }

        try {
            return XML_DATE.newXMLGregorianCalendar(str).toGregorianCalendar().getTimeInMillis();
        } catch (Exception ex) {
            return System.currentTimeMillis();
        }
    }

    private static String toXmlFormat(GregorianCalendar cal) {
        XMLGregorianCalendar xgc = XML_DATE.newXMLGregorianCalendar(cal);
        if (cal.get(Calendar.MILLISECOND) == 0) {
            xgc.setFractionalSecond(null);
        }
        return xgc.toXMLFormat();
    }

    /**
     * Formats a date to the XML UTC format regardless of current locale.
     * @param timestamp number of seconds since the epoch
     * @return The formatted date
     */
    public static synchronized String fromTimestamp(int timestamp) {
        calendar.setTimeInMillis(timestamp * 1000L);
        return toXmlFormat(calendar);
    }

    /**
     * Formats a date to the XML UTC format regardless of current locale.
     * @param date The date to format
     * @return The formatted date
     */
    public static synchronized String fromDate(Date date) {
        calendar.setTime(date);
        return toXmlFormat(calendar);
    }

    private static boolean checkLayout(String text, String pattern) {
        if (text.length() != pattern.length()) return false;
        for (int i = 0; i < pattern.length(); i++) {
            char pc = pattern.charAt(i);
            char tc = text.charAt(i);
            if (pc == 'x' && tc >= '0' && tc <= '9') continue;
            else if (pc == 'x' || pc != tc) return false;
        }
        return true;
    }

    private static int num(char c) {
        return c - '0';
    }

    private static int parsePart2(String str, int off) {
        return 10 * num(str.charAt(off)) + num(str.charAt(off + 1));
    }

    private static int parsePart3(String str, int off) {
        return 100 * num(str.charAt(off)) + 10 * num(str.charAt(off + 1)) + num(str.charAt(off + 2));
    }

    private static int parsePart4(String str, int off) {
        return 1000 * num(str.charAt(off)) + 100 * num(str.charAt(off + 1)) + 10 * num(str.charAt(off + 2)) + num(str.charAt(off + 3));
    }

    /**
     * Returns a new {@code SimpleDateFormat} for date only, according to <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>.
     * @return a new ISO 8601 date format, for date only.
     * @since 7299
     */
    public static SimpleDateFormat newIsoDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd");
    }

    /**
     * Returns a new {@code SimpleDateFormat} for date and time, according to <a href="https://en.wikipedia.org/wiki/ISO_8601">ISO 8601</a>.
     * @return a new ISO 8601 date format, for date and time.
     * @since 7299
     */
    public static SimpleDateFormat newIsoDateTimeFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
    }

    /**
     * Returns a new {@code SimpleDateFormat} for date and time, according to format used in OSM API errors.
     * @return a new date format, for date and time, to use for OSM API error handling.
     * @since 7299
     */
    public static SimpleDateFormat newOsmApiDateTimeFormat() {
        // Example: "2010-09-07 14:39:41 UTC".
        // Always parsed with US locale regardless of the current locale in JOSM
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);
    }

    /**
     * Returns the date format to be used for current user, based on user preferences.
     * @param dateStyle The date style as described in {@link DateFormat#getDateInstance}. Ignored if "ISO dates" option is set
     * @return The date format
     * @since 7299
     */
    public static DateFormat getDateFormat(int dateStyle) {
        if (PROP_ISO_DATES.get()) {
            return newIsoDateFormat();
        } else {
            return DateFormat.getDateInstance(dateStyle, Locale.getDefault());
        }
    }

    /**
     * Formats a date to be displayed to current user, based on user preferences.
     * @param date The date to display. Must not be {@code null}
     * @param dateStyle The date style as described in {@link DateFormat#getDateInstance}. Ignored if "ISO dates" option is set
     * @return The formatted date
     * @since 7299
     */
    public static String formatDate(Date date, int dateStyle) {
        CheckParameterUtil.ensureParameterNotNull(date, "date");
        return getDateFormat(dateStyle).format(date);
    }

    /**
     * Returns the time format to be used for current user, based on user preferences.
     * @param timeStyle The time style as described in {@link DateFormat#getTimeInstance}. Ignored if "ISO dates" option is set
     * @return The time format
     * @since 7299
     */
    public static DateFormat getTimeFormat(int timeStyle) {
        if (PROP_ISO_DATES.get()) {
            // This is not strictly conform to ISO 8601. We just want to avoid US-style times such as 3.30pm
            return new SimpleDateFormat("HH:mm:ss");
        } else {
            return DateFormat.getTimeInstance(timeStyle, Locale.getDefault());
        }
    }
    /**
     * Formats a time to be displayed to current user, based on user preferences.
     * @param time The time to display. Must not be {@code null}
     * @param timeStyle The time style as described in {@link DateFormat#getTimeInstance}. Ignored if "ISO dates" option is set
     * @return The formatted time
     * @since 7299
     */
    public static String formatTime(Date time, int timeStyle) {
        CheckParameterUtil.ensureParameterNotNull(time, "time");
        return getTimeFormat(timeStyle).format(time);
    }

    /**
     * Returns the date/time format to be used for current user, based on user preferences.
     * @param dateStyle The date style as described in {@link DateFormat#getDateTimeInstance}. Ignored if "ISO dates" option is set
     * @param timeStyle The time style as described in {@code DateFormat.getDateTimeInstance}. Ignored if "ISO dates" option is set
     * @return The date/time format
     * @since 7299
     */
    public static DateFormat getDateTimeFormat(int dateStyle, int timeStyle) {
        if (PROP_ISO_DATES.get()) {
            // This is not strictly conform to ISO 8601. We just want to avoid US-style times such as 3.30pm
            // and we don't want to use the 'T' separator as a space character is much more readable
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        } else {
            return DateFormat.getDateTimeInstance(dateStyle, timeStyle, Locale.getDefault());
        }
    }

    /**
     * Formats a date/time to be displayed to current user, based on user preferences.
     * @param datetime The date/time to display. Must not be {@code null}
     * @param dateStyle The date style as described in {@link DateFormat#getDateTimeInstance}. Ignored if "ISO dates" option is set
     * @param timeStyle The time style as described in {@code DateFormat.getDateTimeInstance}. Ignored if "ISO dates" option is set
     * @return The formatted date/time
     * @since 7299
     */
    public static String formatDateTime(Date datetime, int dateStyle, int timeStyle) {
        CheckParameterUtil.ensureParameterNotNull(datetime, "datetime");
        return getDateTimeFormat(dateStyle, timeStyle).format(datetime);
    }
}
