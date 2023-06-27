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

package org.opennms.horizon.minioncertverifier.controller;


import io.opentelemetry.api.trace.StatusCode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertverifier.parser.BasicDnParser;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CertificateControllerTest {
    private MinionCertificateManagerClient client = mock(MinionCertificateManagerClient.class);

    private CertificateController controller = new CertificateController(new BasicDnParser(), client);

    private String pem = "-----BEGIN%20CERTIFICATE-----%0AMIIDYjCCAkoCFGqSEKVzzGJvXIxidPBLyMJwc5GoMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI2MTY1MDA1WhcNMzMwNjIzMTY1MDA1WjCBqjELMAkGA1UEBhMCQ0Ex%0AHTAbBgNVBAgMFERvTm90VXNlSW5Qcm9kdWN0aW9uMR0wGwYDVQQHDBREb05vdFVz%0AZUluUHJvZHVjdGlvbjEQMA4GA1UECgwHT3Blbk5NUzEjMCEGA1UEAwwab3Blbm5t%0Acy1taW5pb24tc3NsLWdhdGV3YXkxDDAKBgNVBAsMA0w6MzEYMBYGA1UECwwPVDpv%0AcGVubm1zLXByaW1lMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhuvt%0AKvqS4qP5LwoTRTuxBKNmlZIvmfn6iwk2MjXkt9PRWRvLWprxtDmg5Hx%2Byy5XAlmW%0ARY3bY6iZerxX4zDShG1iXTLI2ak27JMTYSY48%2FbyOFRibdDrT71EKQdIvWs0ogFO%0Ajr40gNtle94JptvajOVymX6wLm33K0NIePehxa0%2FW4XP%2B5QA5IPW2Xaqqr7vZWod%0AHB5QR%2Fe1vUVym1zT9nxg9lVWclqHvoSKc9D1NZ%2FFQLBJ1JvdCXtUcW%2FmA%2BNI9TE4%0AMu86oxoQhsHXLGeGdiQbh8e66SHM4sFUUgh4nkQQ2q24Ai8FhvfsAwJDpVkyIR7K%0AOYJef5kWdxm%2FMxL1NQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQAuaxuM76pQJ2d1%0AHHFpOtEv3PAi36hw0yGj%2BfNFMXEtUKxlD50dlDCitFh59Igf6%2BAazTShbYkfmHKJ%0AYc%2B051wNs0TJC1wZAtc7dbsgSOY028sW6eirujq17k37rEhT3kDVcw7Vd9l01jmx%0ANg%2BDxOT%2B2EOgJVBZKou493BAGdKbaJozTDjAOFSNy6XU%2Bk9gSDs%2Brj1Cpjvo%2FtSJ%0AvwtwNp7Fvwy3%2BQsWy7PmWjcDxypEXbQX8T41Keo5rUbT2F2zkJ%2Bj18O7JyCzwRiE%0Af4kAiXbFHMm1GxHP6JshUapP%2BHV2iFOspBrghiHnj33Y6nl%2Bp0Xdl1m5DirstCk9%0APIZPVgyl%0A-----END%20CERTIFICATE-----%0A";

    @Test
    void checkCertificate(){
        when(client.isCertValid("6A9210A573CC626F5C8C6274F04BC8C2707391A8"))
            .thenReturn(IsCertificateValidResponse.newBuilder().setIsValid(true).build());

        var result = controller.validate(pem);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("opennms-prime", result.getHeaders().get("tenant-id").get(0));
        assertEquals("3", result.getHeaders().get("location-id").get(0));
    }

    @Test
    void checkInvalidPem(){
        String invalidPem = "-----BEGIN%20CERTIFICATE-----%0AINVALID%0A-----END%20CERTIFICATE-----%0A";

        var result = controller.validate(invalidPem);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void checkInvalidCertificate(){
        when(client.isCertValid("6A9210A573CC626F5C8C6274F04BC8C2707391A8"))
            .thenReturn(IsCertificateValidResponse.newBuilder().setIsValid(false).build());

        var result = controller.validate(pem);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }
}
