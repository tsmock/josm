// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.oauth;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.io.auth.CredentialsAgent;
import org.openstreetmap.josm.io.auth.CredentialsAgentException;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Class holding OAuth access token key and secret.
 * @since 12686 (moved from {@code gui.preferences.server} package)
 */
public class OAuthAccessTokenHolder {
    private static OAuthAccessTokenHolder instance;

    /**
     * Replies the unique instance.
     * @return The unique instance of {@code OAuthAccessTokenHolder}
     */
    public static synchronized OAuthAccessTokenHolder getInstance() {
        if (instance == null) {
            instance = new OAuthAccessTokenHolder();
        }
        return instance;
    }

    private boolean saveToPreferences;
    private String accessTokenKey;
    private String accessTokenSecret;

    /**
     * Replies true if current access token should be saved to the preferences file.
     *
     * @return true if current access token should be saved to the preferences file.
     */
    public boolean isSaveToPreferences() {
        return saveToPreferences;
    }

    /**
     * Sets whether the current access token should be saved to the preferences file.
     *
     * If true, the access token is saved in clear text to the preferences file. The same
     * access token can therefore be used in multiple JOSM sessions.
     *
     * If false, the access token isn't saved to the preferences file. If JOSM is closed,
     * the access token is lost and new token has to be generated by the OSM server the
     * next time JOSM is used.
     *
     * @param saveToPreferences {@code true} to save to preferences file
     */
    public void setSaveToPreferences(boolean saveToPreferences) {
        this.saveToPreferences = saveToPreferences;
    }

    /**
     * Replies the access token key. null, if no access token key is currently set.
     *
     * @return the access token key
     */
    public String getAccessTokenKey() {
        return accessTokenKey;
    }

    /**
     * Sets the access token key. Pass in null to remove the current access token key.
     *
     * @param accessTokenKey the access token key
     */
    public void setAccessTokenKey(String accessTokenKey) {
        this.accessTokenKey = accessTokenKey;
    }

    /**
     * Replies the access token secret. null, if no access token secret is currently set.
     *
     * @return the access token secret
     */
    public String getAccessTokenSecret() {
        return accessTokenSecret;
    }

    /**
     * Sets the access token secret. Pass in null to remove the current access token secret.
     *
     * @param accessTokenSecret access token secret, or null
     */
    public void setAccessTokenSecret(String accessTokenSecret) {
        this.accessTokenSecret = accessTokenSecret;
    }

    /**
     * Replies the access token.
     * @return the access token, can be {@code null}
     */
    public OAuthToken getAccessToken() {
        if (!containsAccessToken())
            return null;
        return new OAuthToken(accessTokenKey, accessTokenSecret);
    }

    /**
     * Sets the access token hold by this holder.
     *
     * @param accessTokenKey the access token key
     * @param accessTokenSecret the access token secret
     */
    public void setAccessToken(String accessTokenKey, String accessTokenSecret) {
        this.accessTokenKey = accessTokenKey;
        this.accessTokenSecret = accessTokenSecret;
    }

    /**
     * Sets the access token hold by this holder.
     *
     * @param token the access token. Can be null to clear the content in this holder.
     */
    public void setAccessToken(OAuthToken token) {
        if (token == null) {
            this.accessTokenKey = null;
            this.accessTokenSecret = null;
        } else {
            this.accessTokenKey = token.getKey();
            this.accessTokenSecret = token.getSecret();
        }
    }

    /**
     * Replies true if this holder contains an complete access token, consisting of an
     * Access Token Key and an Access Token Secret.
     *
     * @return true if this holder contains an complete access token
     */
    public boolean containsAccessToken() {
        return accessTokenKey != null && accessTokenSecret != null;
    }

    /**
     * Initializes the content of this holder from the Access Token managed by the
     * credential manager.
     *
     * @param pref the preferences. Must not be null.
     * @param cm the credential manager. Must not be null.
     * @throws IllegalArgumentException if cm is null
     */
    public void init(Preferences pref, CredentialsAgent cm) {
        CheckParameterUtil.ensureParameterNotNull(pref, "pref");
        CheckParameterUtil.ensureParameterNotNull(cm, "cm");
        OAuthToken token = null;
        try {
            token = cm.lookupOAuthAccessToken();
        } catch (CredentialsAgentException e) {
            Logging.error(e);
            Logging.warn(tr("Failed to retrieve OAuth Access Token from credential manager"));
            Logging.warn(tr("Current credential manager is of type ''{0}''", cm.getClass().getName()));
        }
        saveToPreferences = pref.getBoolean("oauth.access-token.save-to-preferences", true);
        if (token != null) {
            accessTokenKey = token.getKey();
            accessTokenSecret = token.getSecret();
        }
    }

    /**
     * Saves the content of this holder to the preferences and a credential store managed
     * by a credential manager.
     *
     * @param preferences the preferences. Must not be null.
     * @param cm the credentials manager. Must not be null.
     * @throws IllegalArgumentException if preferences is null
     * @throws IllegalArgumentException if cm is null
     */
    public void save(Preferences preferences, CredentialsAgent cm) {
        CheckParameterUtil.ensureParameterNotNull(preferences, "preferences");
        CheckParameterUtil.ensureParameterNotNull(cm, "cm");
        preferences.putBoolean("oauth.access-token.save-to-preferences", saveToPreferences);
        try {
            if (!saveToPreferences) {
                cm.storeOAuthAccessToken(null);
            } else {
                cm.storeOAuthAccessToken(new OAuthToken(accessTokenKey, accessTokenSecret));
            }
        } catch (CredentialsAgentException e) {
            Logging.error(e);
            Logging.warn(tr("Failed to store OAuth Access Token to credentials manager"));
            Logging.warn(tr("Current credential manager is of type ''{0}''", cm.getClass().getName()));
        }
    }

    /**
     * Clears the content of this holder
     */
    public void clear() {
        accessTokenKey = null;
        accessTokenSecret = null;
    }
}
