/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.minioncertmanager.certificate;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class PKCS12Generator {
    public static final String UNSIGNED_CERT_COMMAND = "openssl req -new -key client.key -out client.unsigned.cert -subj \"/C=CA/ST=DoNotUseInProduction/L=DoNotUseInProduction/O=OpenNMS/CN=opennms-minion-ssl-gateway/OU=L:%s/OU=T:%s\"";
    private static final Logger LOG = LoggerFactory.getLogger(PKCS12Generator.class);
    public static final String PKCS_1_2048_COMMAND = "openssl genrsa -out client.key.pkcs1 2048";
    public static final String PKCS8_COMMAND = "openssl pkcs8 -topk8 -in client.key.pkcs1 -out client.key -nocrypt";

    @Setter  // Testability
    private CommandExecutor commandExecutor = new CommandExecutor();

    @Setter  // Testability
    private CertificateReader certificateReader = new CertificateReader();

    /**
     * generate self-signed certificate and return certificate serial number
     */

    public X509Certificate generate(Long locationId, String tenantId, Path outputDirectoryPath, File archive, String archivePass, File caCertFile, File caKeyFile) throws InterruptedException, IOException, CertificateException {
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
        commandExecutor.executeCommand("openssl x509 -req -in client.unsigned.cert -days 3650 -CA \"%s\" -CAkey \"%s\" -out client.signed.cert", file, caCertFile.getAbsolutePath(), caKeyFile.getAbsolutePath());

        commandExecutor.executeCommand("openssl pkcs12 -export -out \"%s\" -inkey client.key -in client.signed.cert -passout env:\"%s\"", file,
            Map.of("PASS_VAR", archivePass), archive.getAbsolutePath(), "PASS_VAR");

        LOG.info("=== DONE");
        return certificateReader.getX509Certificate(file.getAbsolutePath() + FileSystems.getDefault().getSeparator() + "client.signed.cert");
    }
}
