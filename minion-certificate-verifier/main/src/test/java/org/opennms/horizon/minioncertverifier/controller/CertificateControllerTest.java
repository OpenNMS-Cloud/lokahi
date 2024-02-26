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
package org.opennms.horizon.minioncertverifier.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertverifier.parser.BasicDnParser;
import org.springframework.http.HttpStatus;

class CertificateControllerTest {
    private MinionCertificateManagerClient client = mock(MinionCertificateManagerClient.class);

    private CertificateController controller = new CertificateController(new BasicDnParser(), client);

    private String pem =
            "-----BEGIN%20CERTIFICATE-----%0AMIIDYjCCAkoCFGqSEKVzzGJvXIxidPBLyMJwc5GoMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI2MTY1MDA1WhcNMzMwNjIzMTY1MDA1WjCBqjELMAkGA1UEBhMCQ0Ex%0AHTAbBgNVBAgMFERvTm90VXNlSW5Qcm9kdWN0aW9uMR0wGwYDVQQHDBREb05vdFVz%0AZUluUHJvZHVjdGlvbjEQMA4GA1UECgwHT3Blbk5NUzEjMCEGA1UEAwwab3Blbm5t%0Acy1taW5pb24tc3NsLWdhdGV3YXkxDDAKBgNVBAsMA0w6MzEYMBYGA1UECwwPVDpv%0AcGVubm1zLXByaW1lMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhuvt%0AKvqS4qP5LwoTRTuxBKNmlZIvmfn6iwk2MjXkt9PRWRvLWprxtDmg5Hx%2Byy5XAlmW%0ARY3bY6iZerxX4zDShG1iXTLI2ak27JMTYSY48%2FbyOFRibdDrT71EKQdIvWs0ogFO%0Ajr40gNtle94JptvajOVymX6wLm33K0NIePehxa0%2FW4XP%2B5QA5IPW2Xaqqr7vZWod%0AHB5QR%2Fe1vUVym1zT9nxg9lVWclqHvoSKc9D1NZ%2FFQLBJ1JvdCXtUcW%2FmA%2BNI9TE4%0AMu86oxoQhsHXLGeGdiQbh8e66SHM4sFUUgh4nkQQ2q24Ai8FhvfsAwJDpVkyIR7K%0AOYJef5kWdxm%2FMxL1NQIDAQABMA0GCSqGSIb3DQEBCwUAA4IBAQAuaxuM76pQJ2d1%0AHHFpOtEv3PAi36hw0yGj%2BfNFMXEtUKxlD50dlDCitFh59Igf6%2BAazTShbYkfmHKJ%0AYc%2B051wNs0TJC1wZAtc7dbsgSOY028sW6eirujq17k37rEhT3kDVcw7Vd9l01jmx%0ANg%2BDxOT%2B2EOgJVBZKou493BAGdKbaJozTDjAOFSNy6XU%2Bk9gSDs%2Brj1Cpjvo%2FtSJ%0AvwtwNp7Fvwy3%2BQsWy7PmWjcDxypEXbQX8T41Keo5rUbT2F2zkJ%2Bj18O7JyCzwRiE%0Af4kAiXbFHMm1GxHP6JshUapP%2BHV2iFOspBrghiHnj33Y6nl%2Bp0Xdl1m5DirstCk9%0APIZPVgyl%0A-----END%20CERTIFICATE-----%0A";

    private String serialNumber = "6A9210A573CC626F5C8C6274F04BC8C2707391A8";

    @Test
    void checkValidCertificate() {
        when(client.isCertValid(serialNumber))
                .thenReturn(
                        IsCertificateValidResponse.newBuilder().setIsValid(true).build());

        var result = controller.validate(pem);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        var tenantIds = result.getHeaders().get(CertificateController.TENANTID_KEY);
        assertNotNull(tenantIds);
        assertEquals(1, tenantIds.size());
        assertEquals("opennms-prime", tenantIds.get(0));
        var locations = result.getHeaders().get(CertificateController.LOCATIONID_KEY);
        assertNotNull(locations);
        assertEquals(1, locations.size());
        assertEquals("3", locations.get(0));
    }

    @Test
    void checkInvalidPem() {
        String invalidPem = "-----BEGIN%20CERTIFICATE-----%0AINVALID%0A-----END%20CERTIFICATE-----%0A";

        var result = controller.validate(invalidPem);

        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }

    @Test
    void checkExpiredCertificate() {
        when(client.isCertValid(serialNumber))
                .thenReturn(IsCertificateValidResponse.newBuilder()
                        .setIsValid(false)
                        .build());

        var result = controller.validate(pem);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }

    @Test
    void checkMissingValueCertificate() {
        var missingLocationPem =
                "-----BEGIN%20CERTIFICATE-----%0AMIIDCTCCAfECFGRtqrAwfc04UwROh5Cr9jOzmAZBMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI5MDIwODM1WhcNMzMwNjI2MDIwODM1WjBSMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczET%0AMBEGA1UECwwKVDp0ZW5hbnQwMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC%0AggEBANoW2itSUsJfVdUAsoO6GB4yKccJPLr4gjHmmCWAet4ojp9swINNq7WRrTi%2B%0ArHT7cZx4mgc7L%2BMiHFbmEbkUwNixEbF18dhM3iWfSD1uu55HywSKchFaGU1%2FnCDV%0AS7wJEr1Ku%2FP8gN5uHg6g4RYi5ummbh1lQldg1MCue1nUJt%2BQnd533VimnMP5fxCc%0AEyx0MIVuif%2BfbzAZjBFW%2FGe2Eiznqb%2Fmqo%2F27JAagRd9M5C22tgfVD5bwLBETAU8%0AAfqCVW1wwX9o6iG%2ByqqDoGT2xGkRCpUdr6QVbDHTnxaL2zPItK0x1cZ2up1QoLAr%0A9GGHQZIlP0E4FQtk21Nl9u%2Fc6oECAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAajgb%0A4QhQs6QdJ9Mog1d5t1wCmJ4qtiiSM%2FEK2CVhVjSPZbdBUbx0rBP7qyPvDzR8GS8m%0AC%2FbiZl5PjnzSJyqZq%2B4DlazrzLwj4ncTnnBUucXGZLBqDsSrVY7fYrok%2FaR%2Fn7Om%0AmMsWqWLu%2F4ttG2OpNJwdsy%2FeIhcO89XIk8XKtSVcx4sjM36Bh4F6U6o0rUxHNosw%0AzWm%2FdfUQg2Mmv1Pd92nZlgN8WH0P1KLD7c69%2BYucN%2FxENWzkFp6AJb0cNdvgZgKr%0A5pIq2v6dRJJYb806kyd8pjpLCbD7XgqDhvJT3CSZixnqvUTHx0AgMbdie%2B8qg4IJ%0AZmTzcw4Nm1pywf2hZQ%3D%3D%0A-----END%20CERTIFICATE-----";
        when(client.isCertValid("646DAAB0307DCD3853044E8790ABF633B3980641"))
                .thenReturn(
                        IsCertificateValidResponse.newBuilder().setIsValid(true).build());

        var result = controller.validate(missingLocationPem);

        assertEquals(HttpStatus.UNAUTHORIZED, result.getStatusCode());
    }
}
