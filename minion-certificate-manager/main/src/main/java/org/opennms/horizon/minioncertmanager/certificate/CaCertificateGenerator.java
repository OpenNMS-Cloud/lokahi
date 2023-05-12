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

    private final static AtomicLong SERIAL = new AtomicLong();

    public static Entry<PrivateKey, X509Certificate> generate(File directory, String dn, long validitySec) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
            new X500Principal(dn),
            BigInteger.valueOf(SERIAL.incrementAndGet()),
            new Date(),
            new Date(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(validitySec)),
            new X500Principal(dn),
            keyPair.getPublic()
        );
        X509CertificateHolder cert = certGen.build(new JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.getPrivate()));

        X509Certificate certificate = new JcaX509CertificateConverter().getCertificate(cert);
        write(directory, "ca.crt", "CERTIFICATE", certificate.getEncoded());
        write(directory, "ca.key", "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        return Map.entry(keyPair.getPrivate(), certificate);
    }

    private static void write(File directory, String fileName, String type, byte[] encoded) throws IOException {
        String data = "-----BEGIN " + type + "-----\n" +
            Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(encoded) + "\n" +
            "-----END " + type + "-----";
        Files.write(directory.toPath().resolve(fileName), data.getBytes());
    }

}
