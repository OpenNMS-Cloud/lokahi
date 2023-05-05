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

import lombok.Setter;

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
import java.security.cert.CertificateException;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import nl.altindag.ssl.SSLFactory;
import nl.altindag.ssl.util.PemUtils;

@Slf4j
public class MinionGrpcSslContextBuilderFactoryImpl implements MinionGrpcSslContextBuilderFactory {

    @Setter
    private String clientCertChainFilePath;
    @Setter
    private String clientPrivateKeyFilePath;
    @Setter
    private String clientPrivateKeyPassword;
    @Setter
    private String trustCertCollectionFilePath;
    @Setter
    private boolean clientPrivateKeyIsPkcs12;

    @Setter
    private Function<Path, X509ExtendedTrustManager> loadTrustMaterialOp = PemUtils::loadTrustMaterial;
    @Setter
    private LoadIdentityMaterialOp loadIdentityMaterialOp = PemUtils::loadIdentityMaterial;
    @Setter
    private Supplier<SSLFactory.Builder> sslFactoryBuilderSupplier = SSLFactory::builder;
    @Setter
    private Function<String, File> fileFactory = File::new;
    @Setter
    private FunctionWithException<String, KeyStore, KeyStoreException> keyStoreFactory = KeyStore::getInstance;
    @Setter
    private FunctionWithException<String, FileInputStream, IOException> fileInputStreamFactory = FileInputStream::new;
    @Setter
    private FunctionWithException<String, KeyManagerFactory, NoSuchAlgorithmException> keyManagerFactoryFactory = KeyManagerFactory::getInstance;

    @Override
    public SSLContext create() {
        boolean haveTrustOrIdentity = false;

        var sslFactoryBuilder = sslFactoryBuilderSupplier.get();

        if (isSet(trustCertCollectionFilePath)) {
            File trustCertCollectionFile = fileFactory.apply(trustCertCollectionFilePath.trim());
            if (trustCertCollectionFile.exists()) {
                X509ExtendedTrustManager trustManager = loadTrustMaterialOp.apply(trustCertCollectionFile.toPath());
                sslFactoryBuilder.withTrustMaterial(trustManager);

                haveTrustOrIdentity = true;
            } else {
                throw new RuntimeException("Configured trust store" + trustCertCollectionFile.getAbsolutePath() + " does not exist");
            }
        }

        if (isSet(clientCertChainFilePath) || isSet(clientPrivateKeyFilePath)) {
            File clientCertChainFile = fileFactory.apply(clientCertChainFilePath.trim());
            File clientPrivateKeyFile = fileFactory.apply(clientPrivateKeyFilePath.trim());
            if (clientCertChainFile.exists() && clientPrivateKeyFile.exists()) {
                try {
                    if (clientPrivateKeyIsPkcs12) {
                        configureKeyManagerPkcs12(sslFactoryBuilder);
                    } else {
                        configureKeyManagerOther(sslFactoryBuilder, clientCertChainFile.toPath(), clientPrivateKeyFile.toPath());
                    }

                    haveTrustOrIdentity = true;
                } catch (Exception exc) {
                    throw new RuntimeException("Failed to initialize TLS", exc);
                }
            } else {
                throw new RuntimeException("Configured client private key " + clientPrivateKeyFile.getAbsolutePath() + " and/or certificate " + clientCertChainFile.getAbsolutePath() + " do not exist.");
            }
        }

        SSLContext result = null;
        if (haveTrustOrIdentity) {
            var sslFactory = sslFactoryBuilder.build();
            result = sslFactory.getSslContext();
        }

        return result;
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

//========================================
// Internals
//----------------------------------------

    private void configureKeyManagerOther(SSLFactory.Builder builder, Path certPath, Path keyPath) {
        X509ExtendedKeyManager keyManager = loadIdentityMaterialOp.loadIdentityMaterial(certPath, keyPath, passwordAsCharArray());
        builder.withIdentityMaterial(keyManager);
    }

    private void configureKeyManagerPkcs12(SSLFactory.Builder builder) throws Exception {
        KeyStore keyStore = loadPrivateKeyPkcs12Store();

        KeyManagerFactory keyManagerFactory = keyManagerFactoryFactory.apply(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passwordAsCharArray());

        builder
            .withIdentityMaterial(keyManagerFactory)
            ;
    }

    private KeyStore loadPrivateKeyPkcs12Store() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore keyStore = keyStoreFactory.apply("pkcs12");
        try (FileInputStream fis = fileInputStreamFactory.apply(clientPrivateKeyFilePath)) {
            char[] passwordCharArray = passwordAsCharArray();
            keyStore.load(fis, passwordCharArray);
        }

        return keyStore;
    }

    private char[] passwordAsCharArray() {
        if (clientPrivateKeyPassword != null) {
            return clientPrivateKeyPassword.toCharArray();
        }

        return null;
    }


//========================================
// Functional Interface
//  with Exception Declaration
//----------------------------------------

    public interface FunctionWithException<T,R,E extends Exception> {
        R apply(T arg) throws E;
    }

    public interface LoadIdentityMaterialOp {
        X509ExtendedKeyManager loadIdentityMaterial(Path certPath, Path keyPath, char[] password);
    }
}
