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

package org.opennms.horizon.server.utils;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MinionDockerZipPackager {
    private static final Logger LOG = LoggerFactory.getLogger(MinionDockerZipPackager.class);

    public static byte[] generateZip(ByteString certificate, String locationName, String password) throws IOException {
        try (var bytesOut = new ByteArrayOutputStream();
        var zipOutStream = new ZipOutputStream(bytesOut)) {
            var minionName = "minion1-" + locationName;
            ZipEntry entry = new ZipEntry("storage/" + minionName + ".p12");

            zipOutStream.putNextEntry(entry);
            zipOutStream.write(certificate.toByteArray());
            zipOutStream.closeEntry();

            byte[] dockerBytes = loadDockerCompose(minionName, password);
            entry = new ZipEntry("docker-compose.yaml");
            zipOutStream.putNextEntry(entry);
            zipOutStream.write(dockerBytes);
            zipOutStream.closeEntry();

            zipOutStream.close();
            bytesOut.close();

            return bytesOut.toByteArray();
        }
    }

    private static byte[] loadDockerCompose(String minionName, String password) throws IOException {
        String dockerCompose = System.getenv("PACKAGED_MINION_FILE");
        if (dockerCompose == null || dockerCompose.isBlank()) {
            dockerCompose = "run-minion-docker-compose.yaml";
        }
        InputStream dockerStream = MinionDockerZipPackager.class.getClassLoader()
            .getResourceAsStream(dockerCompose);
        if (dockerStream == null) {
            throw new IOException("Unable to load docker compose file from resources");
        }

        String minionEndpoint = System.getenv("MINION_ENDPOINT");
        if (minionEndpoint == null) {
            minionEndpoint = "";
        }

        String dockerTxt = new BufferedReader(new InputStreamReader(dockerStream)).lines()
            .parallel().collect(Collectors.joining("\n"));
        dockerTxt = dockerTxt.replace("[KEYSTORE_PASSWORD]", password);
        dockerTxt = dockerTxt.replace("[MINION_NAME]", minionName);
        dockerTxt = dockerTxt.replace("[MINION_ENDPOINT]", minionEndpoint);
        dockerTxt = dockerTxt.replace("[CERT_FILE]", minionName + ".p12");
        return dockerTxt.getBytes();
    }
}
