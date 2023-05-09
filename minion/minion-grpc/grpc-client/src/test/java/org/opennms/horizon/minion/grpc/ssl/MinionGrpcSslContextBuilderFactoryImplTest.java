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

import nl.altindag.ssl.SSLFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.opennms.horizon.minion.grpc.ssl.MinionGrpcSslContextBuilderFactoryImpl.FunctionWithException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509ExtendedTrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.function.Function;
import java.util.function.Supplier;


import static org.junit.jupiter.api.Assertions.*;

public class MinionGrpcSslContextBuilderFactoryImplTest {

    public static final String CLIENT_CERT_PATH = "x-client-cert-path";
    public static final String CLIENT_KEY_PATH = "x-client-key-path";
    public static final String TRUST_CERT_PATH = "x-trust-cert-path";

    private MinionGrpcSslContextBuilderFactoryImpl target;

    private Function<Path, X509ExtendedTrustManager> mockLoadTrustMaterialOp;
    private X509ExtendedTrustManager mockX509ExtendedTrustManager;
    private Supplier<SSLFactory.Builder> mockSslFactoryBuilderSupplier;
    private SSLFactory.Builder mockSslFactoryBuilder;
    private SSLFactory mockSslFactory;
    private SSLContext mockSslContext;

    private Function<String, File> mockFileFactory;
    private File mockClientCertFile;
    private File mockClientKeyFile;
    private File mockTrustCertFile;

    private FunctionWithException<String, KeyStore, KeyStoreException> mockKeyStoreFactory;
    private KeyStore mockKeyStore;

    private FunctionWithException<String, FileInputStream, IOException> mockFileInputStreamFactory;
    private FileInputStream mockPkcs12FileInputStream;

    private FunctionWithException<String, KeyManagerFactory, NoSuchAlgorithmException> mockKeyManagerFactoryFactory;
    private KeyManagerFactory mockKeyManagerFactory;

    private MinionGrpcSslContextBuilderFactoryImpl.LoadIdentityMaterialOp mockLoadIdentityMaterialOp;
    private X509ExtendedKeyManager mockX509ExtendedKeyManager;

    @BeforeEach
    public void setUp() throws Exception {
        mockLoadTrustMaterialOp = Mockito.mock(Function.class);
        mockX509ExtendedTrustManager = Mockito.mock(X509ExtendedTrustManager.class);
        mockSslFactoryBuilderSupplier = Mockito.mock(Supplier.class);
        mockSslFactoryBuilder = Mockito.mock(SSLFactory.Builder.class);
        mockSslFactory = Mockito.mock(SSLFactory.class);
        mockSslContext = Mockito.mock(SSLContext.class);
        mockClientCertFile = Mockito.mock(File.class);
        mockClientKeyFile = Mockito.mock(File.class);
        mockTrustCertFile = Mockito.mock(File.class);
        mockFileFactory = Mockito.mock(Function.class);

        mockKeyStoreFactory = Mockito.mock(FunctionWithException.class);
        mockKeyStore = Mockito.mock(KeyStore.class);
        mockFileInputStreamFactory = Mockito.mock(FunctionWithException.class);
        mockPkcs12FileInputStream = Mockito.mock(FileInputStream.class);
        mockKeyManagerFactoryFactory = Mockito.mock(FunctionWithException.class);
        mockKeyManagerFactory = Mockito.mock(KeyManagerFactory.class);
        mockLoadIdentityMaterialOp = Mockito.mock(MinionGrpcSslContextBuilderFactoryImpl.LoadIdentityMaterialOp.class);

        Mockito.when(mockSslFactoryBuilderSupplier.get()).thenReturn(mockSslFactoryBuilder);
        Mockito.when(mockSslFactoryBuilder.build()).thenReturn(mockSslFactory);
        Mockito.when(mockSslFactory.getSslContext()).thenReturn(mockSslContext);
        Mockito.when(mockKeyStoreFactory.apply("pkcs12")).thenReturn(mockKeyStore);
        Mockito.when(mockFileInputStreamFactory.apply(CLIENT_KEY_PATH)).thenReturn(mockPkcs12FileInputStream);
        Mockito.when(mockKeyManagerFactoryFactory.apply(KeyManagerFactory.getDefaultAlgorithm())).thenReturn(mockKeyManagerFactory);
        Mockito.when(mockFileFactory.apply(TRUST_CERT_PATH)).thenReturn(mockTrustCertFile);
        Mockito.when(mockFileFactory.apply(CLIENT_CERT_PATH)).thenReturn(mockClientCertFile);
        Mockito.when(mockFileFactory.apply(CLIENT_KEY_PATH)).thenReturn(mockClientKeyFile);

        Mockito.when(mockTrustCertFile.getAbsolutePath()).thenReturn("/absolute/path/to/" + TRUST_CERT_PATH);
        Mockito.when(mockClientCertFile.getAbsolutePath()).thenReturn("/absolute/path/to/" + CLIENT_CERT_PATH);
        Mockito.when(mockClientKeyFile.getAbsolutePath()).thenReturn("/absolute/path/to/" + CLIENT_KEY_PATH);
        Mockito.when(mockTrustCertFile.toPath()).thenReturn(Path.of(TRUST_CERT_PATH));
        Mockito.when(mockClientCertFile.toPath()).thenReturn(Path.of(CLIENT_CERT_PATH));
        Mockito.when(mockClientKeyFile.toPath()).thenReturn(Path.of(CLIENT_KEY_PATH));

        target = new MinionGrpcSslContextBuilderFactoryImpl();

        target.setLoadTrustMaterialOp(mockLoadTrustMaterialOp);
        target.setLoadIdentityMaterialOp(mockLoadIdentityMaterialOp);
        target.setSslFactoryBuilderSupplier(mockSslFactoryBuilderSupplier);
        target.setFileFactory(mockFileFactory);
    }

    @Test
    void testCreateEmpty() {
        //
        // Execute
        //
        SSLContext result = target.create();

        //
        // Verify the Results
        //
        assertNull(result);
    }

    @Test
    void testInvalidTrustStore() throws Exception {
        target.setTrustCertCollectionFilePath("invalid");
        Assertions.assertThrows(RuntimeException.class, target::create);

        target.setTrustCertCollectionFilePath("   ");
        Assertions.assertDoesNotThrow(target::create);
    }

    @Test
    void testInvalidCertificateOrKey() throws Exception {
        target.setClientPrivateKeyFilePath("invalid");
        target.setClientCertChainFilePath("invalid");
        Assertions.assertThrows(RuntimeException.class, target::create);

        target.setClientPrivateKeyFilePath("   ");
        target.setClientCertChainFilePath("   ");
        Assertions.assertDoesNotThrow(target::create);

        target.setClientPrivateKeyFilePath("invalid");
        target.setClientCertChainFilePath("   ");
        Assertions.assertThrows(RuntimeException.class, target::create);

        target.setClientPrivateKeyFilePath("   ");
        target.setClientCertChainFilePath("invalid");
        Assertions.assertThrows(RuntimeException.class, target::create);
    }

    @Test
    void testCreatePEM() throws Exception {
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);
        Mockito.when(mockTrustCertFile.exists()).thenReturn(true);

        commonTestCreateWithPEM(null);
    }

    @Test
    void testCreatePEMFailInitializeTls() throws Exception {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);
        Mockito.when(mockTrustCertFile.exists()).thenReturn(true);

        target.setClientCertChainFilePath(CLIENT_CERT_PATH);
        target.setClientPrivateKeyFilePath(CLIENT_KEY_PATH);
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath(TRUST_CERT_PATH);
        RuntimeException testException = new RuntimeException("x-test-exception-x");
        Mockito.when(mockLoadIdentityMaterialOp.loadIdentityMaterial(Path.of(CLIENT_CERT_PATH), Path.of(CLIENT_KEY_PATH), null)).thenThrow(testException);

        //
        // Execute
        //
        Exception caughtException = null;
        try {
            SSLContext result = target.create();
            fail("Missing expected exception");
        } catch (Exception actualException) {
            caughtException = actualException;
        }

        //
        // Verify the Results
        //
        assertTrue(caughtException.getMessage().contains("Failed to initialize TLS"));
        assertSame(testException, caughtException.getCause());
    }

    @Test
    void testCreatePKCS12() throws Exception {
        commonTestCreateWithPkcs12();
    }

    @Test
    void testPasswordNotNullPkcs12() throws Exception {
        target.setClientPrivateKeyPassword("x-password-x");
        commonTestCreateWithPkcs12();
    }

    @Test
    void testPasswordNotNullPEM() throws Exception {
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);
        Mockito.when(mockTrustCertFile.exists()).thenReturn(true);

        target.setClientPrivateKeyPassword("x-password-x");
        commonTestCreateWithPEM("x-password-x");
    }

    @Test
    public void testConfigureKeyManagerException() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        target.setClientCertChainFilePath(CLIENT_CERT_PATH);
        target.setClientPrivateKeyFilePath(CLIENT_KEY_PATH);
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath(TRUST_CERT_PATH);

        Exception actualException = null;

        try {
            SSLContext result = target.create();
            fail("missing expected exception");
        } catch (Exception exc) {
            actualException = exc;
        }

        //
        // Verify the Results
        //
        assertTrue(actualException.getMessage().contains("x-trust-cert-path does not exist"));
    }

    @Test
    public void testConfigureClientKeyDoesNotExist() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        Mockito.when(mockTrustCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(false);

        target.setClientCertChainFilePath(CLIENT_CERT_PATH);
        target.setClientPrivateKeyFilePath(CLIENT_KEY_PATH);
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath(TRUST_CERT_PATH);

        Exception actualException = null;

        try {
            SSLContext result = target.create();
            fail("missing expected exception");
        } catch (Exception exc) {
            actualException = exc;
        }

        //
        // Verify the Results
        //
        assertTrue(actualException.getMessage().contains("Configured client private key /absolute/path/to/x-client-key-path and/or certificate /absolute/path/to/x-client-cert-path do not exist"));
    }

    @Test
    public void testConfigureClientCertificateDoesNotExist() {
        //
        // Setup Test Data and Interactions
        //

        //
        // Execute
        //
        Mockito.when(mockTrustCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientCertFile.exists()).thenReturn(false);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);

        target.setClientCertChainFilePath(CLIENT_CERT_PATH);
        target.setClientPrivateKeyFilePath(CLIENT_KEY_PATH);
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath(TRUST_CERT_PATH);

        Exception actualException = null;

        try {
            target.create();
            fail("missing expected exception");
        } catch (Exception exc) {
            actualException = exc;
        }

        //
        // Verify the Results
        //
        assertTrue(actualException.getMessage().contains("Configured client private key /absolute/path/to/x-client-key-path and/or certificate /absolute/path/to/x-client-cert-path do not exist"));
    }

//========================================
// Internals
//----------------------------------------

    void commonTestCreateWithPEM(String expectedPassword) throws IOException {
        //
        // Setup Test Data and Interactions
        //
        char[] expectedPassArr = null;
        if (expectedPassword != null) {
            expectedPassArr = expectedPassword.toCharArray();
        }

        Mockito.when(mockLoadIdentityMaterialOp.loadIdentityMaterial(Path.of(CLIENT_CERT_PATH), Path.of(CLIENT_KEY_PATH), expectedPassArr)).thenReturn(mockX509ExtendedKeyManager);


        //
        // Execute
        //
        target.setClientCertChainFilePath(CLIENT_CERT_PATH);
        target.setClientPrivateKeyFilePath(CLIENT_KEY_PATH);
        target.setClientPrivateKeyIsPkcs12(false);
        target.setTrustCertCollectionFilePath(TRUST_CERT_PATH);

        SSLContext result = target.create();

        //
        // Verify the Results
        //
        assertSame(result, mockSslContext);
    }

    private void commonTestCreateWithPkcs12() throws IOException {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockClientCertFile.exists()).thenReturn(true);
        Mockito.when(mockClientKeyFile.exists()).thenReturn(true);
        Mockito.when(mockTrustCertFile.exists()).thenReturn(true);

        //
        // Execute
        //
        target.setClientCertChainFilePath(CLIENT_CERT_PATH);
        target.setClientPrivateKeyFilePath(CLIENT_KEY_PATH);
        target.setClientPrivateKeyIsPkcs12(true);
        target.setTrustCertCollectionFilePath(TRUST_CERT_PATH);
        target.setKeyStoreFactory(mockKeyStoreFactory);
        target.setFileInputStreamFactory(mockFileInputStreamFactory);
        target.setKeyManagerFactoryFactory(mockKeyManagerFactoryFactory);
        target.setLoadIdentityMaterialOp(mockLoadIdentityMaterialOp);

        SSLContext result = target.create();

        //
        // Verify the Results
        //
        assertSame(result, mockSslContext);
    }

}
