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
                throw new GeneralSecurityException(
                        "Password protected files are supported only through keystores/truststore. Please update your configuration");
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
