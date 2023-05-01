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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PKCS8GeneratorTest {
    @TempDir
    private Path tempDir;

    @InjectMocks
    private PKCS8Generator pkcs8Generator;

    @Test
    public void testGenerate() throws InterruptedException, IOException {
        // Test input
        String location = "testLocation";
        String tenantId = "testTenantId";
        File caCertFile = new File("src/test/resources/ca.cert");
        File caKeyFile = new File("src/test/resources/ca.key");

        // Test execution
        pkcs8Generator.generate(location, tenantId, tempDir, caCertFile, caKeyFile);

        // Test output
        File pkcs1KeyFile = tempDir.resolve("client.key.pkcs1").toFile();
        File pkcs8KeyFile = tempDir.resolve("client.key").toFile();
        File unsignedCertFile = tempDir.resolve("client.unsigned.cert").toFile();
        File signedCertFile = tempDir.resolve("client.signed.cert").toFile();

        assertTrue(pkcs1KeyFile.exists(), "PKCS1 key file should exist");
        assertTrue(pkcs8KeyFile.exists(), "PKCS8 key file should exist");
        assertTrue(unsignedCertFile.exists(), "Unsigned cert file should exist");
        assertTrue(signedCertFile.exists(), "Signed cert file should exist");

        assertEquals("client.key.pkcs1", pkcs1KeyFile.getName(), "PKCS1 key file name should match");
        assertEquals("client.key", pkcs8KeyFile.getName(), "PKCS8 key file name should match");
        assertEquals("client.unsigned.cert", unsignedCertFile.getName(), "Unsigned cert file name should match");
        assertEquals("client.signed.cert", signedCertFile.getName(), "Signed cert file name should match");
    }
}

