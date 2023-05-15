package org.opennms.horizon.minion.grpc.channel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.AbstractMap.SimpleEntry;
import java.util.Hashtable;
import java.util.Map.Entry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.minion.grpc.ssl.KeyStoreFactory;

@ExtendWith(MockitoExtension.class)
class SSLChannelFactoryTest {

    @TempDir
    private Path tempDir;

    @Mock
    protected ChannelBuilderFactory channelBuilderFactory;
    @Mock
    protected ManagedChannelBuilder managedChannelBuilder;

    @Mock
    private KeyStoreFactory keyStoreFactory;

    @Test
    public void testNoCredentials() {
        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory);

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
    }

    @Test
    public void testEmptyTrustStore() throws Exception {
        Entry<File, KeyStore> trustStore = getCreateKeyStore("truststore.p12");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory);
        channelFactory.setTrustStore(trustStore.getKey().getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", trustStore.getKey(), "changeit");
    }

    @Test
    public void testEmptyTrustStoreWithException() throws Exception {
        File trustStore = new File(tempDir.toFile(), "truststore2.p12");
        assertTrue(trustStore.createNewFile(), "Failed to create temporary file");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory);
        channelFactory.setTrustStore(trustStore.getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");
        when(keyStoreFactory.createKeyStore("pkcs12", trustStore, "changeit")).thenThrow(new GeneralSecurityException(""));

        assertThrows(RuntimeException.class, () -> channelFactory.create("baz", 443, null));
    }

    @Test
    public void testEmptyKeyStore() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("minion.p12");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", keyStore.getKey(), "changeit");
    }

    @Test
    public void testEmptyKeyStoreWithException() throws Exception {
        File trustStore = new File(tempDir.toFile(), "minion2.p12");
        assertTrue(trustStore.createNewFile(), "Failed to create temporary file");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory);
        channelFactory.setTrustStore(trustStore.getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");
        when(keyStoreFactory.createKeyStore("pkcs12", trustStore, "changeit")).thenThrow(new GeneralSecurityException(""));

        assertThrows(RuntimeException.class, () -> channelFactory.create("baz", 443, null));
    }

    private Entry<File, KeyStore> getCreateKeyStore(String filename) throws IOException, GeneralSecurityException {
        File keyStoreFile = new File(tempDir.toFile(), filename);
        assertTrue(keyStoreFile.createNewFile(), "Could not create temporary file");

        KeyStore keyStore = mock(KeyStore.class);
        when(keyStoreFactory.createKeyStore("pkcs12", keyStoreFile, "changeit")).thenReturn(keyStore);
        when(keyStore.aliases()).thenReturn(new Hashtable<String, String>().keys());
        return new SimpleEntry<>(keyStoreFile, keyStore);
    }
}
