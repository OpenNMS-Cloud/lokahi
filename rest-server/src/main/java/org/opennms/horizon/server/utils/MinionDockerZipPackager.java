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
package org.opennms.horizon.server.utils;

import com.google.protobuf.ByteString;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

            var karafShFile = loadFile("runKarafCommand.sh");
            var karafShFileEntry = new ZipEntry("scripts/runKarafCommand.sh");
            zipOutStream.putNextEntry(karafShFileEntry);
            zipOutStream.write(karafShFile);
            zipOutStream.closeEntry();

            byte[] karafBatFile = loadFile("runKarafCommand.bat");
            var karafBatFileEntry = new ZipEntry("scripts/runKarafCommand.bat");
            zipOutStream.putNextEntry(karafBatFileEntry);
            zipOutStream.write(karafBatFile);
            zipOutStream.closeEntry();

            byte[] readMeFile = loadFile(".readme");
            var readMeFileEntry = new ZipEntry("scripts/.readme");
            zipOutStream.putNextEntry(readMeFileEntry);
            zipOutStream.write(readMeFile);
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
        InputStream dockerStream =
                MinionDockerZipPackager.class.getClassLoader().getResourceAsStream(dockerCompose);
        if (dockerStream == null) {
            throw new IOException("Unable to load docker compose file from resources");
        }

        String minionEndpoint = System.getenv("MINION_ENDPOINT");
        if (minionEndpoint == null) {
            minionEndpoint = "";
        }

        String dockerTxt = new BufferedReader(new InputStreamReader(dockerStream))
                .lines()
                .parallel()
                .collect(Collectors.joining("\n"));
        dockerTxt = dockerTxt.replace("[KEYSTORE_PASSWORD]", password);
        dockerTxt = dockerTxt.replace("[MINION_NAME]", minionName);
        dockerTxt = dockerTxt.replace("[MINION_ENDPOINT]", minionEndpoint);
        dockerTxt = dockerTxt.replace("[CERT_FILE]", minionName + ".p12");
        return dockerTxt.getBytes();
    }

    private static byte[] loadFile(String fileName) throws IOException {

        var inputStream = MinionDockerZipPackager.class.getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new IOException("Unable to load " + fileName + " from resources");
        }
        String fileAsText = new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .parallel()
                .collect(Collectors.joining("\n"));
        return fileAsText.getBytes();
    }
}
