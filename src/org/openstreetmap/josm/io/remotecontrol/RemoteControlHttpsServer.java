// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.remotecontrol;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.StringProperty;

import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.ExtendedKeyUsageExtension;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNameInterface;
import sun.security.x509.GeneralNames;
import sun.security.x509.IPAddressName;
import sun.security.x509.OIDName;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.URIName;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * Simple HTTPS server that spawns a {@link RequestProcessor} for every secure connection.
 *
 * @since 6941
 */
public class RemoteControlHttpsServer extends Thread {

    /** The server socket */
    private final ServerSocket server;

    /** The server instance for IPv4 */
    private static volatile RemoteControlHttpsServer instance4;
    /** The server instance for IPv6 */
    private static volatile RemoteControlHttpsServer instance6;

    /** SSL context information for connections */
    private SSLContext sslContext;

    /* the default port for HTTPS remote control */
    private static final int HTTPS_PORT = 8112;

    /**
     * JOSM keystore file name.
     * @since 7337
     */
    public static final String KEYSTORE_FILENAME = "josm.keystore";

    /**
     * Preference for keystore password (automatically generated by JOSM).
     * @since 7335
     */
    public static final StringProperty KEYSTORE_PASSWORD = new StringProperty("remotecontrol.https.keystore.password", "");

    /**
     * Preference for certificate password (automatically generated by JOSM).
     * @since 7335
     */
    public static final StringProperty KEYENTRY_PASSWORD = new StringProperty("remotecontrol.https.keyentry.password", "");

    /**
     * Unique alias used to store JOSM localhost entry, both in JOSM keystore and system/browser keystores.
     * @since 7343
     */
    public static final String ENTRY_ALIAS = "josm_localhost";

    /**
     * Creates a GeneralName object from known types.
     * @param t one of 4 known types
     * @param v value
     * @return which one
     * @throws IOException if any I/O error occurs
     */
    private static GeneralName createGeneralName(String t, String v) throws IOException {
        GeneralNameInterface gn;
        switch (t.toLowerCase(Locale.ENGLISH)) {
            case "uri": gn = new URIName(v); break;
            case "dns": gn = new DNSName(v); break;
            case "ip": gn = new IPAddressName(v); break;
            default: gn = new OIDName(v);
        }
        return new GeneralName(gn);
    }

    /**
     * Create a self-signed X.509 Certificate.
     * @param dn the X.509 Distinguished Name, eg "CN=localhost, OU=JOSM, O=OpenStreetMap"
     * @param pair the KeyPair
     * @param days how many days from now the Certificate is valid for
     * @param algorithm the signing algorithm, eg "SHA256withRSA"
     * @param san SubjectAlternativeName extension (optional)
     * @return the self-signed X.509 Certificate
     * @throws GeneralSecurityException if any security error occurs
     * @throws IOException if any I/O error occurs
     */
    private static X509Certificate generateCertificate(String dn, KeyPair pair, int days, String algorithm, String san)
            throws GeneralSecurityException, IOException {
        X509CertInfo info = new X509CertInfo();
        Date from = new Date();
        Date to = new Date(from.getTime() + days * 86_400_000L);
        CertificateValidity interval = new CertificateValidity(from, to);
        BigInteger sn = new BigInteger(64, new SecureRandom());
        X500Name owner = new X500Name(dn);

        info.set(X509CertInfo.VALIDITY, interval);
        info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
        info.set(X509CertInfo.SUBJECT, owner);
        info.set(X509CertInfo.ISSUER, owner);

        info.set(X509CertInfo.KEY, new CertificateX509Key(pair.getPublic()));
        info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
        AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
        info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

        CertificateExtensions ext = new CertificateExtensions();
        // Critical: Not CA, max path len 0
        ext.set(BasicConstraintsExtension.NAME, new BasicConstraintsExtension(Boolean.TRUE, false, 0));
        // Critical: only allow TLS ("serverAuth" = 1.3.6.1.5.5.7.3.1)
        ext.set(ExtendedKeyUsageExtension.NAME, new ExtendedKeyUsageExtension(Boolean.TRUE,
                new Vector<>(Arrays.asList(new ObjectIdentifier("1.3.6.1.5.5.7.3.1")))));

        if (san != null) {
            int colonpos;
            String[] ps = san.split(",");
            GeneralNames gnames = new GeneralNames();
            for (String item: ps) {
                colonpos = item.indexOf(':');
                if (colonpos < 0) {
                    throw new IllegalArgumentException("Illegal item " + item + " in " + san);
                }
                String t = item.substring(0, colonpos);
                String v = item.substring(colonpos+1);
                gnames.add(createGeneralName(t, v));
            }
            // Non critical
            ext.set(SubjectAlternativeNameExtension.NAME, new SubjectAlternativeNameExtension(Boolean.FALSE, gnames));
        }

        info.set(X509CertInfo.EXTENSIONS, ext);

        // Sign the cert to identify the algorithm that's used.
        PrivateKey privkey = pair.getPrivate();
        X509CertImpl cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);

        // Update the algorithm, and resign.
        algo = (AlgorithmId) cert.get(X509CertImpl.SIG_ALG);
        info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
        cert = new X509CertImpl(info);
        cert.sign(privkey, algorithm);
        return cert;
    }

    /**
     * Setup the JOSM internal keystore, used to store HTTPS certificate and private key.
     * @return Path to the (initialized) JOSM keystore
     * @throws IOException if an I/O error occurs
     * @throws GeneralSecurityException if a security error occurs
     * @since 7343
     */
    public static Path setupJosmKeystore() throws IOException, GeneralSecurityException {

        Path dir = Paths.get(RemoteControl.getRemoteControlDir());
        Path path = dir.resolve(KEYSTORE_FILENAME);
        Files.createDirectories(dir);

        if (!path.toFile().exists()) {
            Main.debug("No keystore found, creating a new one");

            // Create new keystore like previous one generated with JDK keytool as follows:
            // keytool -genkeypair -storepass josm_ssl -keypass josm_ssl -alias josm_localhost -dname "CN=localhost, OU=JOSM, O=OpenStreetMap"
            // -ext san=ip:127.0.0.1 -keyalg RSA -validity 1825

            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair pair = generator.generateKeyPair();

            X509Certificate cert = generateCertificate("CN=localhost, OU=JOSM, O=OpenStreetMap", pair, 1825, "SHA256withRSA",
                    // see #10033#comment:20: All browsers respect "ip" in SAN, except IE which only understands DNS entries:
                    // CHECKSTYLE.OFF: LineLength
                    // https://connect.microsoft.com/IE/feedback/details/814744/the-ie-doesnt-trust-a-san-certificate-when-connecting-to-ip-address
                    // CHECKSTYLE.ON: LineLength
                    "dns:localhost,ip:127.0.0.1,dns:127.0.0.1,ip:::1,uri:https://127.0.0.1:"+HTTPS_PORT+",uri:https://::1:"+HTTPS_PORT);

            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(null, null);

            // Generate new passwords. See https://stackoverflow.com/a/41156/2257172
            SecureRandom random = new SecureRandom();
            KEYSTORE_PASSWORD.put(new BigInteger(130, random).toString(32));
            KEYENTRY_PASSWORD.put(new BigInteger(130, random).toString(32));

            char[] storePassword = KEYSTORE_PASSWORD.get().toCharArray();
            char[] entryPassword = KEYENTRY_PASSWORD.get().toCharArray();

            ks.setKeyEntry(ENTRY_ALIAS, pair.getPrivate(), entryPassword, new Certificate[]{cert});
            try (OutputStream out = Files.newOutputStream(path, StandardOpenOption.CREATE)) {
                ks.store(out, storePassword);
            }
        }
        return path;
    }

    /**
     * Loads the JOSM keystore.
     * @return the (initialized) JOSM keystore
     * @throws IOException if an I/O error occurs
     * @throws GeneralSecurityException if a security error occurs
     * @since 7343
     */
    public static KeyStore loadJosmKeystore() throws IOException, GeneralSecurityException {
        try (InputStream in = Files.newInputStream(setupJosmKeystore())) {
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(in, KEYSTORE_PASSWORD.get().toCharArray());

            if (Main.isDebugEnabled()) {
                for (Enumeration<String> aliases = ks.aliases(); aliases.hasMoreElements();) {
                    Main.debug("Alias in JOSM keystore: "+aliases.nextElement());
                }
            }
            return ks;
        }
    }

    /**
     * Initializes the TLS basics.
     * @throws IOException if an I/O error occurs
     * @throws GeneralSecurityException if a security error occurs
     */
    private void initialize() throws IOException, GeneralSecurityException {
        KeyStore ks = loadJosmKeystore();

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, KEYENTRY_PASSWORD.get().toCharArray());

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        if (Main.isTraceEnabled()) {
            Main.trace("SSL Context protocol: " + sslContext.getProtocol());
            Main.trace("SSL Context provider: " + sslContext.getProvider());
        }

        setupPlatform(ks);
    }

    /**
     * Setup the platform-dependant certificate stuff.
     * @param josmKs The JOSM keystore, containing localhost certificate and private key.
     * @return {@code true} if something has changed as a result of the call (certificate installation, etc.)
     * @throws KeyStoreException if the keystore has not been initialized (loaded)
     * @throws NoSuchAlgorithmException in case of error
     * @throws CertificateException in case of error
     * @throws IOException in case of error
     * @since 7343
     */
    public static boolean setupPlatform(KeyStore josmKs) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {
        Enumeration<String> aliases = josmKs.aliases();
        if (aliases.hasMoreElements()) {
            return Main.platform.setupHttpsCertificate(ENTRY_ALIAS,
                    new KeyStore.TrustedCertificateEntry(josmKs.getCertificate(aliases.nextElement())));
        }
        return false;
    }

    /**
     * Starts or restarts the HTTPS server
     */
    public static void restartRemoteControlHttpsServer() {
        stopRemoteControlHttpsServer();
        if (RemoteControl.PROP_REMOTECONTROL_HTTPS_ENABLED.get()) {
            int port = Main.pref.getInteger("remote.control.https.port", HTTPS_PORT);
            try {
                instance4 = new RemoteControlHttpsServer(port, false);
                instance4.start();
            } catch (IOException | GeneralSecurityException ex) {
                Main.debug(ex);
                Main.warn(marktr("Cannot start IPv4 remotecontrol https server on port {0}: {1}"),
                        Integer.toString(port), ex.getLocalizedMessage());
            }
            try {
                instance6 = new RemoteControlHttpsServer(port, true);
                instance6.start();
            } catch (IOException | GeneralSecurityException ex) {
                /* only show error when we also have no IPv4 */
                if (instance4 == null) {
                    Main.debug(ex);
                    Main.warn(marktr("Cannot start IPv6 remotecontrol https server on port {0}: {1}"),
                        Integer.toString(port), ex.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * Stops the HTTPS server
     */
    public static void stopRemoteControlHttpsServer() {
        if (instance4 != null) {
            try {
                instance4.stopServer();
            } catch (IOException ioe) {
                Main.error(ioe);
            }
            instance4 = null;
        }
        if (instance6 != null) {
            try {
                instance6.stopServer();
            } catch (IOException ioe) {
                Main.error(ioe);
            }
            instance6 = null;
        }
    }

    /**
     * Constructs a new {@code RemoteControlHttpsServer}.
     * @param port The port this server will listen on
     * @param ipv6 Whether IPv6 or IPv4 server should be started
     * @throws IOException when connection errors
     * @throws GeneralSecurityException in case of SSL setup errors
     * @since 8339
     */
    public RemoteControlHttpsServer(int port, boolean ipv6) throws IOException, GeneralSecurityException {
        super("RemoteControl HTTPS Server");
        this.setDaemon(true);

        initialize();

        // Create SSL Server factory
        SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
        if (Main.isTraceEnabled()) {
            Main.trace("SSL factory - Supported Cipher suites: "+Arrays.toString(factory.getSupportedCipherSuites()));
        }

        this.server = factory.createServerSocket(port, 1, ipv6 ?
            RemoteControl.getInet6Address() : RemoteControl.getInet4Address());

        if (Main.isTraceEnabled() && server instanceof SSLServerSocket) {
            SSLServerSocket sslServer = (SSLServerSocket) server;
            Main.trace("SSL server - Enabled Cipher suites: "+Arrays.toString(sslServer.getEnabledCipherSuites()));
            Main.trace("SSL server - Enabled Protocols: "+Arrays.toString(sslServer.getEnabledProtocols()));
            Main.trace("SSL server - Enable Session Creation: "+sslServer.getEnableSessionCreation());
            Main.trace("SSL server - Need Client Auth: "+sslServer.getNeedClientAuth());
            Main.trace("SSL server - Want Client Auth: "+sslServer.getWantClientAuth());
            Main.trace("SSL server - Use Client Mode: "+sslServer.getUseClientMode());
        }
    }

    /**
     * The main loop, spawns a {@link RequestProcessor} for each connection.
     */
    @Override
    public void run() {
        Main.info(marktr("RemoteControl::Accepting secure remote connections on {0}:{1}"),
                server.getInetAddress(), Integer.toString(server.getLocalPort()));
        while (true) {
            try {
                @SuppressWarnings("resource")
                Socket request = server.accept();
                if (Main.isTraceEnabled() && request instanceof SSLSocket) {
                    SSLSocket sslSocket = (SSLSocket) request;
                    Main.trace("SSL socket - Enabled Cipher suites: "+Arrays.toString(sslSocket.getEnabledCipherSuites()));
                    Main.trace("SSL socket - Enabled Protocols: "+Arrays.toString(sslSocket.getEnabledProtocols()));
                    Main.trace("SSL socket - Enable Session Creation: "+sslSocket.getEnableSessionCreation());
                    Main.trace("SSL socket - Need Client Auth: "+sslSocket.getNeedClientAuth());
                    Main.trace("SSL socket - Want Client Auth: "+sslSocket.getWantClientAuth());
                    Main.trace("SSL socket - Use Client Mode: "+sslSocket.getUseClientMode());
                    Main.trace("SSL socket - Session: "+sslSocket.getSession());
                }
                RequestProcessor.processRequest(request);
            } catch (SocketException se) {
                if (!server.isClosed()) {
                    Main.error(se);
                }
            } catch (IOException ioe) {
                Main.error(ioe);
            }
        }
    }

    /**
     * Stops the HTTPS server.
     *
     * @throws IOException if any I/O error occurs
     */
    public void stopServer() throws IOException {
        Main.info(marktr("RemoteControl::Server {0}:{1} stopped."),
        server.getInetAddress(), Integer.toString(server.getLocalPort()));
        server.close();
    }
}
