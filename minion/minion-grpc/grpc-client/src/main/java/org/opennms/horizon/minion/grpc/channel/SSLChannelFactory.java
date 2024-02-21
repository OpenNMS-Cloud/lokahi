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

import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;
import io.grpc.TlsChannelCredentials.Builder;
import java.io.File;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.Setter;
import org.opennms.horizon.minion.grpc.GrpcErrorMessages;
import org.opennms.horizon.minion.grpc.GrpcShutdownHandler;
import org.opennms.horizon.minion.grpc.ssl.KeyStoreFactory;

public class SSLChannelFactory implements ManagedChannelFactory {
    private final ChannelBuilderFactory channelBuilderFactory;

    private final KeyStoreFactory keyStoreFactory;

    private final GrpcShutdownHandler grpcShutdownHandler;

    @Setter
    private String keyStore;

    @Setter
    private String keyStoreType;

    @Setter
    private String keyStorePassword;

    @Setter
    private String trustStore;

    @Setter
    private String trustStoreType;

    @Setter
    private String trustStorePassword;

    public SSLChannelFactory(
            ChannelBuilderFactory channelBuilderFactory,
            KeyStoreFactory keyStoreFactory,
            GrpcShutdownHandler grpcShutdownHandler) {
        this.channelBuilderFactory = channelBuilderFactory;
        this.keyStoreFactory = keyStoreFactory;
        this.grpcShutdownHandler = grpcShutdownHandler;
    }

    @Override
    public ManagedChannel create(String hostname, int port, String authority) {
        Builder credentials = TlsChannelCredentials.newBuilder();

        if (keyStore != null && !keyStore.isBlank()) {
            try {
                KeyManagerFactory keyManagerFactory =
                        KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(
                        loadKeyStore(keyStoreType, keyStore, keyStorePassword), keyStorePassword.toCharArray());
                credentials.keyManager(keyManagerFactory.getKeyManagers());
            } catch (GeneralSecurityException e) {
                grpcShutdownHandler.shutdown(GrpcErrorMessages.FAIL_LOADING_CLIENT_KEYSTORE);
                throw new RuntimeException(GrpcErrorMessages.FAIL_LOADING_CLIENT_KEYSTORE);
            } catch (IllegalArgumentException e) {
                grpcShutdownHandler.shutdown(GrpcErrorMessages.INVALID_CLIENT_STORE);
                throw new RuntimeException(GrpcErrorMessages.INVALID_CLIENT_STORE);
            }
        }

        if (trustStore != null && !trustStore.isBlank()) {
            try {
                TrustManagerFactory trustManagerFactory =
                        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(loadKeyStore(trustStoreType, trustStore, trustStorePassword));
                credentials.trustManager(trustManagerFactory.getTrustManagers());
            } catch (GeneralSecurityException e) {
                grpcShutdownHandler.shutdown(GrpcErrorMessages.FAIL_LOADING_TRUST_KEYSTORE);
                throw new RuntimeException(GrpcErrorMessages.FAIL_LOADING_TRUST_KEYSTORE);
            } catch (IllegalArgumentException e) {
                grpcShutdownHandler.shutdown(GrpcErrorMessages.INVALID_TRUST_STORE);
                throw new RuntimeException(GrpcErrorMessages.INVALID_TRUST_STORE);
            }
        }

        return channelBuilderFactory
                .create(hostname, port, authority, credentials.build())
                // .useTransportSecurity()
                .build();
    }

    private KeyStore loadKeyStore(String type, String location, String password)
            throws GeneralSecurityException, IllegalArgumentException {
        File keyStoreFile = new File(location);
        if (!keyStoreFile.exists() || !keyStoreFile.isFile() || !keyStoreFile.canRead()) {
            throw new IllegalArgumentException(
                    "File " + location + " does not exist, is not a file or can not be read");
        }

        return keyStoreFactory.createKeyStore(type, keyStoreFile, password);
    }
}
