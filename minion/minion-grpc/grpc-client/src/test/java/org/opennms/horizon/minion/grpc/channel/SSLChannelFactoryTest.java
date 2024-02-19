/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.minion.grpc.channel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import org.opennms.horizon.minion.grpc.GrpcErrorMessages;
import org.opennms.horizon.minion.grpc.GrpcShutdownHandler;
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
    protected GrpcShutdownHandler grpcShutdownHandler;

    @Mock
    private KeyStoreFactory keyStoreFactory;

    @Test
    void testValid() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("keystore.p12", "changeit");
        Entry<File, KeyStore> trustStore = getCreateKeyStore("truststore.p12", "changeit");

        SSLChannelFactory channelFactory =
                new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");
        channelFactory.setTrustStore(trustStore.getKey().getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class)))
                .thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", keyStore.getKey(), "changeit");
        verify(keyStoreFactory).createKeyStore("pkcs12", trustStore.getKey(), "changeit");
    }

    @Test
    void testNoCredentials() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("keystore.p12", null);

        SSLChannelFactory channelFactory =
                new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        assertThrows(RuntimeException.class, () -> channelFactory.create("baz", 443, null));
        verify(grpcShutdownHandler).shutdown(GrpcErrorMessages.FAIL_LOADING_CLIENT_KEYSTORE);
    }

    @Test
    void testMissingKeyStore() throws Exception {
        Entry<File, KeyStore> trustStore = getCreateKeyStore("truststore.p12", "changeit");

        SSLChannelFactory channelFactory =
                new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setTrustStore(trustStore.getKey().getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class)))
                .thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", trustStore.getKey(), "changeit");
    }

    @Test
    void testEmptyTrustStoreWithException() throws Exception {
        File trustStore = new File(tempDir.toFile(), "truststore2.p12");
        assertTrue(trustStore.createNewFile(), "Failed to create temporary file");

        SSLChannelFactory channelFactory =
                new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setTrustStore(trustStore.getAbsolutePath());
        channelFactory.setTrustStoreType("pkcs12");
        channelFactory.setTrustStorePassword("changeit");
        when(keyStoreFactory.createKeyStore("pkcs12", trustStore, "changeit"))
                .thenThrow(new GeneralSecurityException(""));
        assertThrows(RuntimeException.class, () -> channelFactory.create("baz", 443, null));
        verify(grpcShutdownHandler).shutdown(GrpcErrorMessages.FAIL_LOADING_TRUST_KEYSTORE);
    }

    @Test
    void testMissingTrustStore() throws Exception {
        Entry<File, KeyStore> keyStore = getCreateKeyStore("minion.p12", "changeit");

        SSLChannelFactory channelFactory =
                new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getKey().getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");

        when(channelBuilderFactory.create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class)))
                .thenReturn(managedChannelBuilder);

        channelFactory.create("baz", 443, null);

        verify(channelBuilderFactory).create(eq("baz"), eq(443), isNull(), any(TlsChannelCredentials.class));
        verify(keyStoreFactory).createKeyStore("pkcs12", keyStore.getKey(), "changeit");
    }

    @Test
    void testEmptyKeyStoreWithException() throws Exception {
        File keyStore = new File(tempDir.toFile(), "minion2.p12");
        assertTrue(keyStore.createNewFile(), "Failed to create temporary file");

        SSLChannelFactory channelFactory =
                new SSLChannelFactory(channelBuilderFactory, keyStoreFactory, grpcShutdownHandler);
        channelFactory.setKeyStore(keyStore.getAbsolutePath());
        channelFactory.setKeyStoreType("pkcs12");
        channelFactory.setKeyStorePassword("changeit");
        when(keyStoreFactory.createKeyStore("pkcs12", keyStore, "changeit"))
                .thenThrow(new GeneralSecurityException(""));
        assertThrows(RuntimeException.class, () -> channelFactory.create("baz", 443, null));
        verify(grpcShutdownHandler).shutdown(GrpcErrorMessages.FAIL_LOADING_CLIENT_KEYSTORE);
    }

    private Entry<File, KeyStore> getCreateKeyStore(String filename, String password)
            throws IOException, GeneralSecurityException {
        File keyStoreFile = new File(tempDir.toFile(), filename);
        assertTrue(keyStoreFile.createNewFile(), "Could not create temporary file");

        KeyStore keyStore = mock(KeyStore.class);
        if (password != null) {
            when(keyStoreFactory.createKeyStore("pkcs12", keyStoreFile, password))
                    .thenReturn(keyStore);
            when(keyStore.aliases()).thenReturn(new Hashtable<String, String>().keys());
        } else {
            when(keyStoreFactory.createKeyStore(eq("pkcs12"), eq(keyStoreFile), any()))
                    .thenThrow(new GeneralSecurityException());
        }
        return new SimpleEntry<>(keyStoreFile, keyStore);
    }
}
