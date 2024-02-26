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
        SslContextBuilder sslClientContextBuilder =
                SslContextBuilder.forServer(new File(certChainFilePath), new File(privateKeyFilePath));
        if (!Strings.isNullOrEmpty(trustCertCollectionFilePath)) {
            sslClientContextBuilder.trustManager(new File(trustCertCollectionFilePath));
            sslClientContextBuilder.clientAuth(ClientAuth.REQUIRE);
        }
        return GrpcSslContexts.configure(sslClientContextBuilder, SslProvider.OPENSSL);
    }

    public static Properties getPropertiesFromConfig(ConfigurationAdmin configAdmin, String pid) {
        Properties properties = new Properties();
        try {
            final Dictionary<String, Object> config =
                    configAdmin.getConfiguration(pid).getProperties();
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
