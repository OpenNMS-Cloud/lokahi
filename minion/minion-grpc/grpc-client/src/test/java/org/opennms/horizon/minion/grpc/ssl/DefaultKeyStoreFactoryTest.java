package org.opennms.horizon.minion.grpc.ssl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Enumeration;
import org.junit.jupiter.api.Test;

class DefaultKeyStoreFactoryTest {

    @Test
    public void verifyTrustStoreStoreWithPasswordAndKeyEntry() throws URISyntaxException, GeneralSecurityException {
        DefaultKeyStoreFactory keyStoreFactory = new DefaultKeyStoreFactory();
        KeyStore store = keyStoreFactory.createKeyStore("pkcs12", resolvePath("/minion.p12"), "passw0rd");

        // make sure we have just one entry
        Enumeration<String> aliases = store.aliases();
        assertTrue(aliases.hasMoreElements());
        String entryId = aliases.nextElement();
        assertFalse(aliases.hasMoreElements());

        Key key = store.getKey(entryId, "passw0rd".toCharArray());
        assertNotNull(key);
    }

    @Test
    public void verifyTrustStoreCreation() throws URISyntaxException, GeneralSecurityException {
        DefaultKeyStoreFactory keyStoreFactory = new DefaultKeyStoreFactory();
        KeyStore store = keyStoreFactory.createKeyStore("file", resolvePath("/CA.cert"), null);

        // make sure we have just one entry
        Enumeration<String> aliases = store.aliases();
        assertTrue(aliases.hasMoreElements());
        String entryId = aliases.nextElement();
        assertFalse(aliases.hasMoreElements());

        Certificate certificate = store.getCertificate(entryId);
        assertNotNull(certificate);
    }

    @Test
    public void verifyRefusesPasswordProtectedFile() throws URISyntaxException, GeneralSecurityException {
        DefaultKeyStoreFactory keyStoreFactory = new DefaultKeyStoreFactory();
        assertThrows(GeneralSecurityException.class, () -> keyStoreFactory.createKeyStore("file", resolvePath("/CA.cert"), "foo"));
    }

    @Test
    public void verifyTrustStoreStoreWithPasswordAndCertificateEntry() throws URISyntaxException, GeneralSecurityException {
        DefaultKeyStoreFactory keyStoreFactory = new DefaultKeyStoreFactory();
        KeyStore store = keyStoreFactory.createKeyStore("pkcs12", resolvePath("/truststore.p12"), "changeme");

        // make sure we have just one entry
        Enumeration<String> aliases = store.aliases();
        assertTrue(aliases.hasMoreElements());
        String entryId = aliases.nextElement();
        assertFalse(aliases.hasMoreElements());

        Certificate certificate = store.getCertificate(entryId);
        assertNotNull(certificate);
    }

    private File resolvePath(String path) throws URISyntaxException {
        return new File(getClass().getResource(path).toURI().getPath());
    }

}
