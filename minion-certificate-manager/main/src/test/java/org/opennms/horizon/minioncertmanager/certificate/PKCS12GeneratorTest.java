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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

// TODO: MOVE TO SYSTEM INTEGRATION TEST
public class PKCS12GeneratorTest {

    private CommandExecutor mockCommandExecutor;

    private PKCS12Generator pkcs12Generator;

    @BeforeEach
    public void setUp() {
        mockCommandExecutor = Mockito.mock(CommandExecutor.class);
        pkcs12Generator = new PKCS12Generator();
    }

    @Test
    void testGenerateP12() throws IOException, InterruptedException, CertificateException {
        //
        // Setup Test Data and Interactions
        //
        Path outputDirPath = Path.of("/test-output-dir");
        File mockCaCertFile = Mockito.mock(File.class);
        Mockito.when(mockCaCertFile.exists()).thenReturn(true);

        CertificateReader mockCertificateReader = Mockito.mock(CertificateReader.class);
        X509Certificate mockCertificate = Mockito.mock(X509Certificate.class);
        Mockito.when(mockCertificateReader.getX509Certificate(
                        outputDirPath.toFile().getAbsolutePath()
                                + FileSystems.getDefault().getSeparator() + "client.signed.cert"))
                .thenReturn(mockCertificate);

        pkcs12Generator.setCertificateReader(mockCertificateReader);

        //
        // Execute
        //
        pkcs12Generator.setCommandExecutor(mockCommandExecutor);
        var outputCert = pkcs12Generator.generate(
                1010L,
                "x-tenant-id-x",
                outputDirPath,
                new File("minion.p12"),
                "x-archive-pass-x",
                mockCaCertFile,
                new File("x-ca-key-file-x"));

        //
        // Verify the Results
        //
        Mockito.verify(mockCommandExecutor)
                .executeCommand("openssl genrsa -out client.key.pkcs1 2048", outputDirPath.toFile());
        assertEquals(mockCertificate, outputCert);
    }

    @Test
    void testGenerateP12CaCertFileMissing() throws IOException, InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        Path outputDirPath = Path.of("/test-output-dir");
        File mockCaCertFile = Mockito.mock(File.class);
        Mockito.when(mockCaCertFile.exists()).thenReturn(false);

        //
        // Execute
        //
        Exception actual = null;
        try {
            pkcs12Generator.setCommandExecutor(mockCommandExecutor);
            pkcs12Generator.generate(
                    2020L,
                    "x-tenant-id-x",
                    outputDirPath,
                    new File("minion.p12"),
                    "x-archive-pass-x",
                    mockCaCertFile,
                    new File("x-ca-key-file-x"));
            fail("missing expected exception");
        } catch (Exception caught) {
            actual = caught;
        }

        //
        // Verify the Results
        //
        assertTrue(actual instanceof FileNotFoundException);
    }
}
