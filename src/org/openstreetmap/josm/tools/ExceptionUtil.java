// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.io.OsmApiException;
import org.openstreetmap.josm.io.OsmApiInitializationException;
import org.openstreetmap.josm.io.OsmChangesetCloseException;
import org.openstreetmap.josm.io.OsmTransferException;

public class ExceptionUtil {
    private ExceptionUtil() {
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static String explainOsmApiInitializationException(OsmApiInitializationException e) {
        e.printStackTrace();
        String msg = tr(
                "<html>Failed to initialize communication with the OSM server {0}.<br>"
                + "Check the server URL in your preferences and your internet connection.</html>", Main.pref.get(
                        "osm-server.url", "http://api.openstreetmap.org/api"));
        return msg;
    }

    /**
     * handles an exception caught during OSM API initialization
     *
     * @param e the exception
     */
    public static String explainOsmChangesetCloseException(OsmChangesetCloseException e) {
        e.printStackTrace();
        String changsetId = e.getChangeset() == null ? tr("unknown") : Long.toString(e.getChangeset().getId());
        String msg = tr(
                "<html>Failed to close changeset ''{0}'' on the OSM server ''{1}''.<br>"
                + "The changeset will automatically be closed by the server after a timeout.</html>", changsetId,
                Main.pref.get("osm-server.url", "http://api.openstreetmap.org/api"));
        return msg;
    }


    /**
     * Explains an upload error due to a violated precondition, i.e. a HTTP return code 412
     *
     * @param e the exception
     */
    public static String explainPreconditionFailed(OsmApiException e) {
        e.printStackTrace();
        String msg = tr(
                "<html>Uploading to the server <strong>failed</strong> because your current<br>"
                + "dataset violates a precondition.<br>" + "The error message is:<br>" + "{0}" + "</html>", e
                .getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
        return msg;
    }

    /**
     * Explains an exception with a generic message dialog
     * 
     * @param e the exception
     */
    public static String explainGeneric(Exception e) {
        String msg = e.getMessage();
        if (msg == null || msg.trim().equals("")) {
            msg = e.toString();
        }
        e.printStackTrace();
        return msg;
    }

    /**
     * Explains a {@see SecurityException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when user tries to access the OSM API from within an
     * applet which wasn't loaded from the API server.
     * 
     * @param e the exception
     */

    public static String explainSecurityException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''<br>"
                + "for security reasons. This is most likely because you are running<br>"
                + "in an applet and because you didn''t load your applet from ''{1}''.</html>", apiUrl, host);
        return message;
    }

    /**
     * Explains a {@see SocketException} which has caused an {@see OsmTransferException}.
     * This is most likely because there's not connection to the Internet or because
     * the remote server is not reachable.
     * 
     * @param e the exception
     */

    public static String explainNestedSocketException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''.<br>"
                + "Please check your internet connection.</html>", apiUrl);
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see IOException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when the communication with the remote server is
     * interrupted for any reason.
     * 
     * @param e the exception
     */

    public static String explainNestedIOException(OsmTransferException e) {
        IOException ioe = getNestedException(e, IOException.class);
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("<html>Failed to upload data to or download data from<br>" + "''{0}''<br>"
                + "due to a problem with transferring data.<br>" + "Details(untranslated): {1}</html>", apiUrl, ioe
                .getMessage());
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because of an internal server
     * error in the OSM API server..
     * 
     * @param e the exception
     */

    public static String explainInternalServerError(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("<html>The OSM server<br>" + "''{0}''<br>" + "reported an internal server error.<br>"
                + "This is most likely a temporary problem. Please try again later.</html>", apiUrl);
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see OsmApiException} which was thrown because of a bad
     * request
     * 
     * @param e the exception
     */
    public static String explainBadRequest(OsmApiException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String message = tr("The OSM server ''{0}'' reported a bad request.<br>", apiUrl);
        if (e.getErrorHeader() != null && e.getErrorHeader().startsWith("The maximum bbox")) {
            message += "<br>"
                + tr("The area you tried to download is too big or your request was too large."
                        + "<br>Either request a smaller area or use an export file provided by the OSM community.");
        } else if (e.getErrorHeader() != null) {
            message += tr("<br>Error message(untranslated): {0}", e.getErrorHeader());
        }
        message = "<html>" + message + "</html>";
        e.printStackTrace();
        return message;
    }

    /**
     * Explains a {@see UnknownHostException} which has caused an {@see OsmTransferException}.
     * This is most likely happening when there is an error in the API URL or when
     * local DNS services are not working.
     * 
     * @param e the exception
     */

    public static String explainNestedUnkonwnHostException(OsmTransferException e) {
        String apiUrl = OsmApi.getOsmApi().getBaseUrl();
        String host = tr("unknown");
        try {
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException ex) {
            // shouldn't happen
        }

        String message = tr("<html>Failed to open a connection to the remote server<br>" + "''{0}''.<br>"
                + "Host name ''{1}'' couldn''t be resolved. <br>"
                + "Please check the API URL in your preferences and your internet connection.</html>", apiUrl, host);
        e.printStackTrace();
        return message;
    }

    /**
     * Replies the first nested exception of type <code>nestedClass</code> (including
     * the root exception <code>e</code>) or null, if no such exception is found.
     * 
     * @param <T>
     * @param e the root exception
     * @param nestedClass the type of the nested exception
     * @return the first nested exception of type <code>nestedClass</code> (including
     * the root exception <code>e</code>) or null, if no such exception is found.
     */
    protected static <T> T getNestedException(Exception e, Class<T> nestedClass) {
        Throwable t = e;
        while (t != null && !(nestedClass.isInstance(t))) {
            t = t.getCause();
        }
        if (t == null)
            return null;
        else if (nestedClass.isInstance(t))
            return nestedClass.cast(t);
        return null;
    }

    /**
     * Explains an {@see OsmTransferException} to the user.
     * 
     * @param e the {@see OsmTransferException}
     */
    public static String explainOsmTransferException(OsmTransferException e) {
        if (getNestedException(e, SecurityException.class) != null)
            return explainSecurityException(e);
        if (getNestedException(e, SocketException.class) != null)
            return explainNestedSocketException(e);
        if (getNestedException(e, UnknownHostException.class) != null)
            return explainNestedUnkonwnHostException(e);
        if (getNestedException(e, IOException.class) != null)
            return explainNestedIOException(e);
        if (e instanceof OsmApiInitializationException)
            return explainOsmApiInitializationException((OsmApiInitializationException) e);
        if (e instanceof OsmChangesetCloseException)
            return explainOsmChangesetCloseException((OsmChangesetCloseException) e);

        if (e instanceof OsmApiException) {
            OsmApiException oae = (OsmApiException) e;
            if (oae.getResponseCode() == HttpURLConnection.HTTP_PRECON_FAILED)
                return explainPreconditionFailed(oae);
            if (oae.getResponseCode() == HttpURLConnection.HTTP_GONE)
                return explainGoneForUnknownPrimitive(oae);
            if (oae.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
                return explainInternalServerError(oae);
            if (oae.getResponseCode() == HttpURLConnection.HTTP_BAD_REQUEST)
                return explainBadRequest(oae);
        }
        return explainGeneric(e);
    }

    /**
     * explains the case of an error due to a delete request on an already deleted
     * {@see OsmPrimitive}, i.e. a HTTP response code 410, where we don't know which
     * {@see OsmPrimitive} is causing the error.
     *
     * @param e the exception
     */
    public static String explainGoneForUnknownPrimitive(OsmApiException e) {
        String msg = tr("<html>Uploading <strong>failed</strong> because a primitive you tried to<br>"
                + "delete on the server is already deleted.<br>" + "<br>" + "The error message is:<br>" + "{0}"
                + "</html>", e.getMessage().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"));
        return msg;

    }

    /**
     * Explains an {@see Exception} to the user.
     * 
     * @param e the {@see Exception}
     */
    public static String explainException(Exception e) {
        if (e instanceof OsmTransferException)
            return explainOsmTransferException((OsmTransferException) e);
        return explainGeneric(e);
    }
}
