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

import com.google.common.base.Strings;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import lombok.Setter;

import javax.net.ssl.KeyManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;

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
    private Supplier<SslContextBuilder> grpcSslClientContextFactory = GrpcSslContexts::forClient;
    @Setter
    private FunctionWithException<String, KeyStore, KeyStoreException> keyStoreFactory = KeyStore::getInstance;
    @Setter
    private FunctionWithException<String, FileInputStream, IOException> fileInputStreamFactory = FileInputStream::new;
    @Setter
    private FunctionWithException<String, KeyManagerFactory, NoSuchAlgorithmException> keyManagerFactoryFactory = KeyManagerFactory::getInstance;

    @Override
    public SslContextBuilder create() {
        SslContextBuilder builder = grpcSslClientContextFactory.get();

        if (isSet(trustCertCollectionFilePath)) {
            File trustCertCollectionFile = new File(trustCertCollectionFilePath.trim());
            if (trustCertCollectionFile.exists()) {
                builder.trustManager(trustCertCollectionFile);
            } else {
                throw new RuntimeException("Configured trust store" + trustCertCollectionFile.getAbsolutePath() + " does not exist");
            }
        }

        if (isSet(clientCertChainFilePath) || isSet(clientPrivateKeyFilePath)) {
            File clientCertChainFile = new File(clientCertChainFilePath.trim());
            File clientPrivateKeyFile = new File(clientPrivateKeyFilePath.trim());
            if (clientCertChainFile.exists() && clientPrivateKeyFile.exists()) {
                try {
                    if (clientPrivateKeyIsPkcs12) {
                        configureKeyManagerPkcs12(builder);
                    } else {
                        configureKeyManagerOther(builder);
                    }
                } catch (Exception exc) {
                    throw new RuntimeException("Failed to initialize TLS", exc);
                }
            } else {
                throw new RuntimeException("Configured client private key " + clientPrivateKeyFile.getAbsolutePath() + " and/or certificate " + clientCertChainFile.getAbsolutePath() + " do not exist.");
            }
        }

        return builder;
    }

    private boolean isSet(String value) {
        return value != null && !value.isBlank();
    }

//========================================
// Internals
//----------------------------------------

    private void configureKeyManagerOther(SslContextBuilder builder) {
            builder.keyManager(
                new File(clientCertChainFilePath),
                new File(clientPrivateKeyFilePath),
                sanitizePassword()
            );
    }

    private void configureKeyManagerPkcs12(SslContextBuilder builder) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException, UnrecoverableKeyException {
        KeyStore keyStore = loadPrivateKeyPkcs12Store();

        KeyManagerFactory keyManagerFactory = keyManagerFactoryFactory.apply(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, passwordAsCharArray());

        ApplicationProtocolConfig apn =
            new ApplicationProtocolConfig(
                ApplicationProtocolConfig.Protocol.ALPN,
                ApplicationProtocolConfig.SelectorFailureBehavior.FATAL_ALERT,
                ApplicationProtocolConfig.SelectedListenerFailureBehavior.FATAL_ALERT,
                "h2"
            );

        builder
            .keyManager(keyManagerFactory)
            .sslProvider(SslProvider.JDK)
            .applicationProtocolConfig(apn)
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

    private String sanitizePassword() {
        // Password must be null if unset; empty string can lead to the SSL context builder failing
        if (Strings.isNullOrEmpty(clientPrivateKeyPassword)) {
            return null;
        }

        return clientPrivateKeyPassword;
    }


//========================================
// Functional Interface
//  with Exception Declaration
//----------------------------------------

    public interface FunctionWithException<T,R,E extends Exception> {
        R apply(T arg) throws E;
    }
}
