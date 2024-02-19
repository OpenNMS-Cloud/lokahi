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
package org.opennms.horizon.minioncertmanager.certificate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PKCS12Generator {
    public static final String UNSIGNED_CERT_COMMAND =
            "openssl req -new -key client.key -out client.unsigned.cert -subj \"/C=CA/ST=DoNotUseInProduction/L=DoNotUseInProduction/O=OpenNMS/CN=opennms-minion-ssl-gateway/OU=L:%s/OU=T:%s\"";
    private static final Logger LOG = LoggerFactory.getLogger(PKCS12Generator.class);
    public static final String PKCS_1_2048_COMMAND = "openssl genrsa -out client.key.pkcs1 2048";
    public static final String PKCS8_COMMAND = "openssl pkcs8 -topk8 -in client.key.pkcs1 -out client.key -nocrypt";

    @Setter // Testability
    private CommandExecutor commandExecutor = new CommandExecutor();

    @Setter // Testability
    private CertificateReader certificateReader = new CertificateReader();

    /**
     * generate self-signed certificate and return certificate serial number
     */
    public X509Certificate generate(
            Long locationId,
            String tenantId,
            Path outputDirectoryPath,
            File archive,
            String archivePass,
            File caCertFile,
            File caKeyFile)
            throws InterruptedException, IOException, CertificateException {
        // Check if caCertFile exists
        if (!caCertFile.exists()) {
            throw new FileNotFoundException("CA certificate file not found: " + caCertFile.getPath());
        }

        LOG.info("=== GENERATING CERTIFICATE FOR LOCATION: {} AND TENANT: {}", locationId, tenantId);
        LOG.info("=== CA CERT: {}", caCertFile.getAbsolutePath());
        LOG.info("=== CA KEY: {}", caKeyFile.getAbsolutePath());
        LOG.info("=== PATH: {}", outputDirectoryPath.toAbsolutePath());
        File file = outputDirectoryPath.toFile();
        LOG.info("=== FILE: {}", file);
        LOG.info("=== FILE exists: {}", file.exists());

        LOG.debug("=== MAKING PKCS1 KEY");
        commandExecutor.executeCommand(PKCS_1_2048_COMMAND, file);

        LOG.debug("=== CONVERTING TO PKCS8");
        commandExecutor.executeCommand(PKCS8_COMMAND, file);

        LOG.debug("=== GENERATING THE UNSIGNED CERT");
        commandExecutor.executeCommand(UNSIGNED_CERT_COMMAND, file, String.valueOf(locationId), tenantId);

        LOG.info("=== SIGNING CERT");
        LOG.info("=== CA CERT: {}", caCertFile.getAbsolutePath());
        // Do not use this in Production (10 years is not a good idea)
        commandExecutor.executeCommand(
                "openssl x509 -req -in client.unsigned.cert -days 3650 -CA \"%s\" -CAkey \"%s\" -out client.signed.cert",
                file, caCertFile.getAbsolutePath(), caKeyFile.getAbsolutePath());

        commandExecutor.executeCommand(
                "openssl pkcs12 -export -out \"%s\" -inkey client.key -in client.signed.cert -passout env:\"%s\"",
                file, Map.of("PASS_VAR", archivePass), archive.getAbsolutePath(), "PASS_VAR");

        LOG.info("=== DONE");
        return certificateReader.getX509Certificate(
                file.getAbsolutePath() + FileSystems.getDefault().getSeparator() + "client.signed.cert");
    }
}
