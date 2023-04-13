/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

import java.io.FileNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
    void testGenerateP12() throws IOException, InterruptedException {
        //
        // Setup Test Data and Interactions
        //
        Path outputDirPath = Path.of("/test-output-dir");
        File mockCaCertFile = Mockito.mock(File.class);
        Mockito.when(mockCaCertFile.exists()).thenReturn(true);

        //
        // Execute
        //
        pkcs12Generator.setCommandExecutor(mockCommandExecutor);
        pkcs12Generator.generate("x-location-x", "x-tenant-id-x", outputDirPath, new File("minion.p12"), "x-archive-pass-x", mockCaCertFile, new File("x-ca-key-file-x"));

        //
        // Verify the Results
        //
        Mockito.verify(mockCommandExecutor).executeCommand("openssl genrsa -out client.key.pkcs1 2048", outputDirPath.toFile());
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
            pkcs12Generator.generate("x-location-x", "x-tenant-id-x", outputDirPath, new File("minion.p12"), "x-archive-pass-x", mockCaCertFile, new File("x-ca-key-file-x"));
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
