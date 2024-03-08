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

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Utility to create ad-hoc self-signed certificates with specific DN.
 */
public class CaCertificateGenerator {

    private static final AtomicLong SERIAL = new AtomicLong();

    public static Entry<PrivateKey, X509Certificate> generate(File directory, String dn, long validitySec)
            throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                new X500Principal(dn),
                BigInteger.valueOf(SERIAL.incrementAndGet()),
                new Date(),
                new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(validitySec)),
                new X500Principal(dn),
                keyPair.getPublic());
        X509CertificateHolder cert =
                certGen.build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));

        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(cert);
        write(directory, "ca.crt", "CERTIFICATE", certificate.getEncoded());
        write(directory, "ca.key", "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        return Map.entry(keyPair.getPrivate(), certificate);
    }

    private static void write(File directory, String fileName, String type, byte[] encoded) throws IOException {
        String data = "-----BEGIN " + type + "-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded)
                + "\n" + "-----END "
                + type + "-----";
        Files.write(directory.toPath().resolve(fileName), data.getBytes());
    }
}
