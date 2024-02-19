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
package org.opennms.horizon.minioncertverifier.parser;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public class CertificateParser {
    private final X509Certificate certificate;

    public CertificateParser(String pemInput) throws CertificateException {
        var certStream = new ByteArrayInputStream(
                URLDecoder.decode(pemInput, StandardCharsets.UTF_8).getBytes());
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        certificate = (X509Certificate) certFactory.generateCertificate(certStream);
    }

    public String getSubjectDn() {
        return certificate.getSubjectX500Principal().getName();
    }

    public String getSerialNumber() {
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }
}
