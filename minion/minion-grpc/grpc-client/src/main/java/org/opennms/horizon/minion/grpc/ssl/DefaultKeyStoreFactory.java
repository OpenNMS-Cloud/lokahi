/*
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
 *
 */

package org.opennms.horizon.minion.grpc.ssl;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public class DefaultKeyStoreFactory implements KeyStoreFactory {

    private static final String FILE = "file";

    @Override
    public KeyStore createKeyStore(String type, File file, String password) throws GeneralSecurityException {
        if (FILE.equals(type)) {
            if (password == null || password.isBlank()) { // loading up trusted certificate entry store
                try {
                    String data = Files.readString(file.toPath());
                    KeyStore keyStore = emptyKeyStore();
                    keyStore.setCertificateEntry("trusted", loadCertificate(data));
                    return keyStore;
                } catch (IOException e) {
                    throw new GeneralSecurityException("Failed to initialize empty keystore", e);
                }
            } else {
                throw new GeneralSecurityException("Password protected files are supported only through keystores/truststore. Please update your configuration");
            }
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(type);
            keyStore.load(new FileInputStream(file), passwordAsCharArray(password));
            return keyStore;
        } catch (IOException e) {
            throw new GeneralSecurityException("Could not open keystore file " + file.getAbsolutePath());
        }
    }

    private char[] passwordAsCharArray(String password) {
        return password == null ? null : password.toCharArray();
    }

    private KeyStore emptyKeyStore() throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null);
        return keyStore;
    }

    private static Certificate loadCertificate(String data) throws IOException, GeneralSecurityException {
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
        return certificateFactory.generateCertificate(new ByteArrayInputStream(data.getBytes()));
    }
}
