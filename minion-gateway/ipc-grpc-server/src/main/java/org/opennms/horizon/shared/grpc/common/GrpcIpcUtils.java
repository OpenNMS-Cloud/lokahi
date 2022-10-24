/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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
 *******************************************************************************/

package org.opennms.horizon.shared.grpc.common;

import com.google.common.base.Strings;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrpcIpcUtils {

    private static final Logger LOG = LoggerFactory.getLogger(GrpcIpcUtils.class);
    public static final String GRPC_SERVER_PID = "org.opennms.core.ipc.grpc.server";
    public static final String LOG_PREFIX = "ipc";
    public static final String GRPC_HOST = "host";
    public static final String DEFAULT_GRPC_HOST = "localhost";
    public static final String TLS_ENABLED = "tls.enabled";
    public static final String GRPC_MAX_INBOUND_SIZE = "max.message.size";
    public static final int DEFAULT_MESSAGE_SIZE = 10485760;

    public static final String TRUST_CERTIFICATE_FILE_PATH = "trust.cert.filepath";

    public static final String SERVER_CERTIFICATE_FILE_PATH = "server.cert.filepath";
    public static final String PRIVATE_KEY_FILE_PATH = "server.private.key.filepath";

    public static SslContextBuilder getSslContextBuilder(Properties properties) {
        String certChainFilePath = properties.getProperty(GrpcIpcUtils.SERVER_CERTIFICATE_FILE_PATH);
        String privateKeyFilePath = properties.getProperty(GrpcIpcUtils.PRIVATE_KEY_FILE_PATH);
        String trustCertCollectionFilePath = properties.getProperty(GrpcIpcUtils.TRUST_CERTIFICATE_FILE_PATH);
        if (Strings.isNullOrEmpty(certChainFilePath) || Strings.isNullOrEmpty(privateKeyFilePath)) {
            return null;
        }
        SslContextBuilder sslClientContextBuilder = SslContextBuilder.forServer(new File(certChainFilePath),
                new File(privateKeyFilePath));
        if (!Strings.isNullOrEmpty(trustCertCollectionFilePath)) {
            sslClientContextBuilder.trustManager(new File(trustCertCollectionFilePath));
            sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
        }
        return GrpcSslContexts.configure(sslClientContextBuilder,
                SslProvider.OPENSSL);
    }

    public static Properties getPropertiesFromConfig(ConfigurationAdmin configAdmin, String pid) {
        Properties properties = new Properties();
        try {
            final Dictionary<String, Object> config = configAdmin.getConfiguration(pid).getProperties();
            if (config != null) {
                final Enumeration<String> keys = config.keys();
                while (keys.hasMoreElements()) {
                    final String key = keys.nextElement();
                    properties.put(key, config.get(key));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot load config", e);
        }
        return properties;
    }

}
