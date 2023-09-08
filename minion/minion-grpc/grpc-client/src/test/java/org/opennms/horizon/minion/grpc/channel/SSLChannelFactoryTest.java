package org.opennms.horizon.minion.grpc.channel;

import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.minion.grpc.GrpcErrorMessages;
import org.opennms.horizon.minion.grpc.GrpcShutdownHandler;
import org.opennms.horizon.minion.grpc.ssl.KeyStoreFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.AbstractMap.SimpleEntry;
import java.util.Hashtable;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SSLChannelFactoryTest {

    @TempDir
    private Path tempDir;

    @Mock
    protected ChannelBuilderFactory channelBuilderFactory;
    @Mock
    protected ManagedChannelBuilder managedChannelBuilder;

    @Mock
    protected GrpcShutdownHandler grpcShutdownHandler;

    @Mock
    private KeyStoreFactory keyStoreFactory;

    @Test
    void testValid() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("keystore.p12", "changeit");
        Entry<File, KeyStore> trustStore = getCreateKeyStore("truststore.p12", "changeit");


        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");
        channelFactory.setTrustStore(trustStore.getKey().getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", keyStore.getKey(), "changeit");
        verify(keyStoreFactory).createKeyStore("pkcs12", trustStore.getKey(), "changeit");
    }
    @Test
    void testNoCredentials() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("keystore.p12", null);

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(grpcShutdownHandler).shutdown(GrpcErrorMessages.FAIL_LOADING_CLIENT_KEYSTORE);
    }

    @Test
    void testMissingKeyStore() throws Exception {
        Entry<File, KeyStore> trustStore = getCreateKeyStore("truststore.p12", "changeit");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setTrustStore(trustStore.getKey().getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", trustStore.getKey(), "changeit");
    }

    @Test
    void testEmptyTrustStoreWithException() throws Exception {
        File trustStore = new File(tempDir.toFile(), "truststore2.p12");
        assertTrue(trustStore.createNewFile(), "Failed to create temporary file");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setTrustStore(trustStore.getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");
        when(keyStoreFactory.createKeyStore("pkcs12", trustStore, "changeit")).thenThrow(new GeneralSecurityException(""));
        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(grpcShutdownHandler).shutdown(GrpcErrorMessages.FAIL_LOADING_TRUST_KEYSTORE);
    }

    @Test
    void testMissingTrustStore() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("minion.p12", "changeit");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", keyStore.getKey(), "changeit");
    }

    @Test
    void testEmptyKeyStoreWithException() throws Exception {
        File keyStore = new File(tempDir.toFile(), "minion2.p12");
        assertTrue(keyStore.createNewFile(), "Failed to create temporary file");

        SSLChannelFactory channelFactory = new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");
        when(keyStoreFactory.createKeyStore("pkcs12", keyStore, "changeit")).thenThrow(new GeneralSecurityException(""));
        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class))).thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(grpcShutdownHandler).shutdown(GrpcErrorMessages.FAIL_LOADING_CLIENT_KEYSTORE);
    }

    private Entry<File, KeyStore> getCreateKeyStore(String filename, String password) throws IOException, GeneralSecurityException {
        File keyStoreFile = new File(tempDir.toFile(), filename);
        assertTrue(keyStoreFile.createNewFile(), "Could not create temporary file");

        KeyStore keyStore = mock(KeyStore.class);
        if(password != null) {
            when(keyStoreFactory.createKeyStore("pkcs12", keyStoreFile, password)).thenReturn(keyStore);
            when(keyStore.aliases()).thenReturn(new Hashtable<String, String>().keys());
        } else {
            when(keyStoreFactory.createKeyStore(eq("pkcs12"), eq(keyStoreFile), any())).thenThrow(new GeneralSecurityException());
        }
        return new SimpleEntry<>(keyStoreFile, keyStore);
    }
}
