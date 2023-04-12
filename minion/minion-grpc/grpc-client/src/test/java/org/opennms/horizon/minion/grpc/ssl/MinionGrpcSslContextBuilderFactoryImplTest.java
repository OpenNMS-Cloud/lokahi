/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *
 */

package org.opennms.horizon.minion.grpc.ssl;

import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;

import org.opennms.horizon.minion.grpc.ssl.MinionGrpcSslContextBuilderFactoryImpl.FunctionWithException;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.function.Supplier;


import static org.junit.jupiter.api.Assertions.*;

public class MinionGrpcSslContextBuilderFactoryImplTest {

    private MinionGrpcSslContextBuilderFactoryImpl target;

    private Supplier<SslContextBuilder> mockGrpcSslClientContextFactory;
    private SslContextBuilder mockSslContextBuilder;

    private FunctionWithException<String, KeyStore, KeyStoreException> mockKeyStoreFactory;
    private KeyStore mockKeyStore;

    private FunctionWithException<String, FileInputStream, IOException> mockFileInputStreamFactory;
    private FileInputStream mockPkcs12FileInputStream;

    private FunctionWithException<String, KeyManagerFactory, NoSuchAlgorithmException> mockKeyManagerFactoryFactory;
    private KeyManagerFactory mockKeyManagerFactory;

    @BeforeEach
    public void setUp() throws Exception {
        mockGrpcSslClientContextFactory = Mockito.mock(Supplier.class);
        mockSslContextBuilder = Mockito.mock(SslContextBuilder.class);
        mockKeyStoreFactory = Mockito.mock(FunctionWithException.class);
        mockKeyStore = Mockito.mock(KeyStore.class);
        mockFileInputStreamFactory = Mockito.mock(FunctionWithException.class);
        mockPkcs12FileInputStream = Mockito.mock(FileInputStream.class);
        mockKeyManagerFactoryFactory = Mockito.mock(FunctionWithException.class);
        mockKeyManagerFactory = Mockito.mock(KeyManagerFactory.class);

        Mockito.when(mockGrpcSslClientContextFactory.get()).thenReturn(mockSslContextBuilder);
        Mockito.when(mockKeyStoreFactory.apply("pkcs12")).thenReturn(mockKeyStore);
        Mockito.when(mockFileInputStreamFactory.apply("x-client-key-path")).thenReturn(mockPkcs12FileInputStream);
        Mockito.when(mockKeyManagerFactoryFactory.apply(KeyManagerFactory.getDefaultAlgorithm())).thenReturn(mockKeyManagerFactory);

        target = new MinionGrpcSslContextBuilderFactoryImpl();

        target.setGrpcSslClientContextFactory(mockGrpcSslClientContextFactory);
    }

    @Test
    void testCreateEmpty() {
        //
        // Execute
        //
        SslContextBuilder result = target.create();

        //
        // Verify the Results
        //
        assertSame(result, mockSslContextBuilder);
        Mockito.verifyNoInteractions(mockSslContextBuilder);
    }

    @Test
    void testCreatePEM() {
        commonTestCreatePEM(null);
    }

    @Test
    void testCreatePKCS12() {
        commonTestCreatePkcs12();
    }

    @Test
    void testPasswordNotNullPkcs12() {
        target.setClientPrivateKeyPassword("x-password-x");
        commonTestCreatePkcs12();
    }

    @Test
    void testPasswordNotNullPEM() {
        target.setClientPrivateKeyPassword("x-password-x");
        commonTestCreatePEM("x-password-x");
    }

    @Test
    public void testConfigureKeyManagerException() {
        //
        // Setup Test Data and Interactions
        //
        var testException = new RuntimeException("x-text-exc-x");
        Mockito.when(mockSslContextBuilder.keyManager(new File("x-client-cert-path"), new File("x-client-key-path"), null)).thenThrow(testException);

        //
        // Execute
        //
        target.setClientCertChainFilePath("x-client-cert-path");
        target.setClientPrivateKeyFilePath("x-client-key-path");
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath("x-trust-cert-path");

        Exception actualException = null;

        try {
            SslContextBuilder result = target.create();
            fail("missing expected exception");
        } catch (Exception exc) {
            actualException = exc;
        }

        //
        // Verify the Results
        //
        assertEquals("Failed to initialize TLS", actualException.getMessage());
        assertSame(testException, actualException.getCause());
    }

//========================================
// Internals
//----------------------------------------

    void commonTestCreatePEM(String expectedPassword) {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.setClientCertChainFilePath("x-client-cert-path");
        target.setClientPrivateKeyFilePath("x-client-key-path");
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath("x-trust-cert-path");

        SslContextBuilder result = target.create();

        //
        // Verify the Results
        //
        assertSame(result, mockSslContextBuilder);
        Mockito.verify(mockSslContextBuilder).trustManager(new File("x-trust-cert-path"));
        Mockito.verify(mockSslContextBuilder).keyManager(new File("x-client-cert-path"), new File("x-client-key-path"), expectedPassword);
    }

    private void commonTestCreatePkcs12() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockSslContextBuilder.keyManager(Mockito.any(KeyManagerFactory.class))).thenReturn(mockSslContextBuilder);
        Mockito.when(mockSslContextBuilder.sslProvider(SslProvider.JDK)).thenReturn(mockSslContextBuilder);
        Mockito.when(mockSslContextBuilder.applicationProtocolConfig(Mockito.any(ApplicationProtocolConfig.class))).thenReturn(mockSslContextBuilder);

        //
        // Execute
        //
        target.setClientCertChainFilePath("x-client-cert-path");
        target.setClientPrivateKeyFilePath("x-client-key-path");
        target.setClientPrivateKeyIsPkcs12(true);
        target.setTrustCertCollectionFilePath("x-trust-cert-path");
        target.setKeyStoreFactory(mockKeyStoreFactory);
        target.setFileInputStreamFactory(mockFileInputStreamFactory);
        target.setKeyManagerFactoryFactory(mockKeyManagerFactoryFactory);

        SslContextBuilder result = target.create();

        //
        // Verify the Results
        //
        var matcher =
            makeApplicationProtocolConfigMatcher(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT,
                "h2");

        assertSame(result, mockSslContextBuilder);
        Mockito.verify(mockSslContextBuilder).trustManager(new File("x-trust-cert-path"));
        Mockito.verify(mockSslContextBuilder).keyManager(mockKeyManagerFactory);
        Mockito.verify(mockSslContextBuilder).applicationProtocolConfig(Mockito.argThat(matcher));
    }

    private ArgumentMatcher<ApplicationProtocolConfig>
    makeApplicationProtocolConfigMatcher(
        ApplicationProtocolConfig.Protocol protocol,
        ApplicationProtocolConfig.SelectorFailureBehavior selectorBehavior,
        ApplicationProtocolConfig.SelectedListenerFailureBehavior selectedBehavior,
        String... supportedProtocols) {

        return argument -> {
            if (
                (argument.protocol() == protocol) &&
                (argument.selectorFailureBehavior() == selectorBehavior) &&
                (argument.selectedListenerFailureBehavior() == selectedBehavior) &&
                (argument.supportedProtocols().size() == supportedProtocols.length)) {

                int cur = 0;
                while (cur < supportedProtocols.length) {
                    if (!Objects.equals(supportedProtocols[cur], argument.supportedProtocols().get(cur))) {
                        return false;
                    }
                    cur++;
                }

                return true;
            }

            return false;
        };
    }
}
