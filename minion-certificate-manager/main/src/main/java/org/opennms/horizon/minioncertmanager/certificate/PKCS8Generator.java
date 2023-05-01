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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

public class PKCS8Generator {
    public static final String UNSIGNED_CERT_COMMAND = "openssl req -new -key client.key -out client.unsigned.cert -subj \"/C=CA/ST=DoNotUseInProduction/L=DoNotUseInProduction/O=OpenNMS/CN=opennms-minion-ssl-gateway/OU=L:";
    private static final Logger LOG = LoggerFactory.getLogger(PKCS8Generator.class);
    public static final String PKCS_1_2048_COMMAND = "openssl genrsa -out client.key.pkcs1 2048";
    public static final String PKCS8_COMMAND = "openssl pkcs8 -topk8 -in client.key.pkcs1 -out client.key -nocrypt";

    public void generate(String location, String tenantId, Path path, File caCertFile, File caKeyFile) throws InterruptedException, IOException {
        // Check if caCertFile exists
        if (!caCertFile.exists()) {
            throw new FileNotFoundException("CA certificate file not found: " + caCertFile.getPath());
        }

        LOG.info("=== GENERATING CERTIFICATE FOR LOCATION: {} AND TENANT: {}", location, tenantId);
        LOG.info("=== CA CERT: {}", caCertFile.getAbsolutePath());
        LOG.info("=== CA KEY: {}", caKeyFile.getAbsolutePath());
        LOG.info("=== PATH: {}", path.toAbsolutePath());
        File file = path.toFile();
        LOG.info("=== FILE: {}", file);
        LOG.info("=== FILE exists: {}", file.exists());

        LOG.debug("=== MAKING PKCS1 KEY");
        CommandExecutor.executeCommand(PKCS_1_2048_COMMAND, file);

        LOG.debug("=== CONVERTING TO PKCS8");
        CommandExecutor.executeCommand(PKCS8_COMMAND, file);

        LOG.debug("=== GENERATING THE UNSIGNED CERT");
        CommandExecutor.executeCommand(UNSIGNED_CERT_COMMAND + location + "/OU=T:" + tenantId + "\"", file);

        LOG.info("=== SIGNING CERT");
        LOG.info("=== CA CERT: {}", caCertFile.getAbsolutePath());
        // Do not use this in Production (10 years is not a good idea)
        CommandExecutor.executeCommand("openssl x509 -req -in client.unsigned.cert -days 3650 -CA " + caCertFile.getAbsolutePath() + " -CAkey " + caKeyFile.getAbsolutePath() + " -CAcreateserial -out client.signed.cert", file);

        LOG.info("=== DONE");
    }
}
