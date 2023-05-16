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

import java.io.FileInputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class PKCS12GeneratorTest {
    @TempDir
    private Path tempDir;

    @InjectMocks
    private PKCS12Generator pkcs8Generator;

    private File caCertFile;
    private File caKeyFile;

    @BeforeEach
    public void setup() throws Exception {
        File caRootDir = tempDir.toFile();
        CaCertificateGenerator.generate(
            caRootDir, "OU=TEST", 3600
        );

        caCertFile = new File(caRootDir, "ca.crt");
        caKeyFile = new File(caRootDir, "ca.key");
    }


    @Test
    public void testGenerate() throws InterruptedException, GeneralSecurityException, IOException {
        // Test input
        String location = "testLocation";
        String tenantId = "testTenantId";

        File p12 = tempDir.resolve("minion.p12").toFile();
        // Test execution
        pkcs8Generator.generate(location, tenantId, tempDir, p12, "foo", caCertFile, caKeyFile);

        assertTrue(p12.exists());

        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(new FileInputStream(p12), "foo".toCharArray());
        assertNotNull(keyStore.getKey("1", "foo".toCharArray()));
    }
}

