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

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.CompressionLevel;
import net.lingala.zip4j.model.enums.EncryptionMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Component
public class CertFileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(CertFileUtils.class);

    public void createZipFile(File file, String password, File directory, File caCertFile) throws ZipException {
        ZipParameters zipParameters = new ZipParameters();
        zipParameters.setEncryptFiles(true);
        zipParameters.setCompressionLevel(CompressionLevel.HIGHER);
        zipParameters.setEncryptionMethod(EncryptionMethod.ZIP_STANDARD);

        try (ZipFile zipFile = new ZipFile(file, password.toCharArray())) {
            zipFile.addFile(new File(directory, "client.key"), zipParameters);
            zipFile.addFile(caCertFile, zipParameters);
            zipFile.addFile(new File(directory, "client.signed.cert"), zipParameters);
        } catch (Exception e) {
            LOG.error("Error while creating zip file", e);
            throw new RuntimeException("Failed to generate zip file", e);
        }
    }

    public byte[] readZipFileToByteArray(File file) throws IOException {
        byte[] zipBytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = fis.read(zipBytes);
            while (bytesRead < zipBytes.length) {
                int remainingBytes = zipBytes.length - bytesRead;
                bytesRead += fis.read(zipBytes, bytesRead, remainingBytes);
            }
        }
        return zipBytes;
    }
}
