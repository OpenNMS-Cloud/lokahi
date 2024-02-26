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
package org.opennms.horizon.minioncertverifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionCertificateVerifierHttpClientUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MinionCertificateVerifierHttpClientUtils.class);

    private static final String LOCALHOST = "localhost";

    private Map<String, String> subjectPemMap;

    private URI uri;
    private Builder clientBuilder;

    public MinionCertificateVerifierHttpClientUtils() {
        // certificate can generate with scripts/genX509.sh
        // e.g. ./genX509.sh "OU=T:tenant03,OU=L:333"
        subjectPemMap = new HashMap<>();
        subjectPemMap.put(
                "OU=T:tenant01,OU=L:111,CN=opennms-minion-ssl-gateway,O=OpenNMS,L=TBD,ST=TBD,C=CA",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDejCCAmICFE8PznD3X%2BURQs9UtMNHpv6YHMdIMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTkzNzIwWhcNMzMwNjI1MTkzNzIwWjCBwjELMAkGA1UEBhMCQ0Ex%0ACzAJBgNVBAgMAk9OMQ8wDQYDVQQHDAZPdHRhd2ExEDAOBgNVBAoMB29wZW5ubXMx%0AEzARBgNVBAsMClQ6dGVuYW50MDExDjAMBgNVBAsMBUw6MTExMSMwIQYDVQQDDBpv%0AcGVubm1zLW1pbmlvbi1zc2wtZ2F0ZXdheTEQMA4GA1UECgwHT3Blbk5NUzEMMAoG%0AA1UEBwwDVEJEMQwwCgYDVQQIDANUQkQxCzAJBgNVBAYTAkNBMIIBIjANBgkqhkiG%0A9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlTfT2lYORGLDdXzoXYtpxLQtqBEko8WJzfhk%0AAT6gDRPhFOxOd6bVqz0eM0fyBp6vZt1SQN%2B4xinU%2B79EJk3ve9XcWEJEgrsdtPO5%0AhgG9jFcrzmvXAhrYfa%2FRBVWvQThtClxgzq7JoQH20sYIXi5wnR9uPTv%2FCEH%2B%2F%2F5Q%0A7c05j1VwqDA413w7LyjdN9wBwqHNbl2DWChUs3eOZCWHlffTZwrFYjhX7MXcllQJ%0ANPdxU13NKC4h0otJIMwEhA%2FBGvUKscRhlmF1UNE7cIlJphU57a0JJyCA%2Fg1wNljv%0Ai2OYkM0NN21v7cxvHauES5zhg2y6OKUN7ldg4bQJ4%2BLVvW7zeQIDAQABMA0GCSqG%0ASIb3DQEBCwUAA4IBAQCw%2BKHa0LEiPusG4V0h6ELx9Zuo1moYpSqea80Hc%2BHsr76q%0AoGWzPF6U6Gcq37FI7EoJpuEUohSQCVQoJ7ViDZRqpgVOHtJGS%2BOkO12i0O4tAt21%0AD%2FCkmk7c26W%2F1a45Re9WcGrjtO%2FaHfqY17teQr%2F6oixLKIF1gRaN2NJ2TfuTsTnA%0ASxJyDarrfEu5gSXPJrX1iMT0%2B9DSWAG%2BzeNaciZyyAshLuH5gj%2Bm3weIggzAKWP4%0A2E5Tdc4TlT2SoOFTTxm0kU21sgKG5HtEbYmuaM%2BUd8uohNHtcWEFGOXVYaJvK3tq%0A9bGzuOBvOxXJ7SrjggniWZw95ziRc594g5aif%2Fvc%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "OU=T:tenant02,OU=L:222",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDGTCCAgECFF55DqMrkVF2B6vcEelN%2BhfVqJARMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTkzOTQxWhcNMzMwNjI1MTkzOTQxWjBiMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczET%0AMBEGA1UECwwKVDp0ZW5hbnQwMjEOMAwGA1UECwwFTDoyMjIwggEiMA0GCSqGSIb3%0ADQEBAQUAA4IBDwAwggEKAoIBAQCVN9PaVg5EYsN1fOhdi2nEtC2oESSjxYnN%2BGQB%0APqANE%2BEU7E53ptWrPR4zR%2FIGnq9m3VJA37jGKdT7v0QmTe971dxYQkSCux2087mG%0AAb2MVyvOa9cCGth9r9EFVa9BOG0KXGDOrsmhAfbSxgheLnCdH249O%2F8IQf7%2F%2FlDt%0AzTmPVXCoMDjXfDsvKN033AHCoc1uXYNYKFSzd45kJYeV99NnCsViOFfsxdyWVAk0%0A93FTXc0oLiHSi0kgzASED8Ea9QqxxGGWYXVQ0TtwiUmmFTntrQknIID%2BDXA2WO%2BL%0AY5iQzQ03bW%2FtzG8dq4RLnOGDbLo4pQ3uV2DhtAnj4tW9bvN5AgMBAAEwDQYJKoZI%0AhvcNAQELBQADggEBAL0%2BBHFYTJm6L3eWzy3umgbZHS4fQCzPLDHek7t2lraapsY%2F%0AAXLbQvilVkaiv28JVdlk%2FOMCq5ZfeHRUKeLA8CWvKug4LPpWunqEVW%2F5HNyKFEdU%0A5D3equvIHrUDRDxwGduk3QfmO4%2BV%2BRmo1MqD68k25xLpdb6jiiO4S%2Bj8a8zZb5FQ%0AVH61CqXwCHs6AUDiLpGmDzYTMF613e6kZLojJ1wqLj9Bpw2oaCR4DfPaTOO8Ke5e%0AKWKEAmO9P4NU1CkdhKRZYeQ6UXR%2FwIkik%2BEvSf5%2B5KOrZ897coRB7HKgBLLkZOZH%0AyDIcvpouDOwX6fe63bFQHjNhHC5KyLc%2BysgOSlA%3D%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "OU=T:tenant03",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDCTCCAfECFFJnnwWuOYXhgH8bCwrjU0t9erhwMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTk0MDI4WhcNMzMwNjI1MTk0MDI4WjBSMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczET%0AMBEGA1UECwwKVDp0ZW5hbnQwMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoC%0AggEBAJU309pWDkRiw3V86F2LacS0LagRJKPFic34ZAE%2BoA0T4RTsTnem1as9HjNH%0A8gaer2bdUkDfuMYp1Pu%2FRCZN73vV3FhCRIK7HbTzuYYBvYxXK85r1wIa2H2v0QVV%0Ar0E4bQpcYM6uyaEB9tLGCF4ucJ0fbj07%2FwhB%2Fv%2F%2BUO3NOY9VcKgwONd8Oy8o3Tfc%0AAcKhzW5dg1goVLN3jmQlh5X302cKxWI4V%2BzF3JZUCTT3cVNdzSguIdKLSSDMBIQP%0AwRr1CrHEYZZhdVDRO3CJSaYVOe2tCScggP4NcDZY74tjmJDNDTdtb%2B3Mbx2rhEuc%0A4YNsujilDe5XYOG0CePi1b1u83kCAwEAATANBgkqhkiG9w0BAQsFAAOCAQEArCVI%0Abd5sGUATvclcTt9qYvZXRVw7ueHjeCWaX%2BFVlQuFQS4M9c3xBe3Br1s5oMv5PsAO%0ArIqDHple8P9w%2Ff1bRjvABlgdqRHYRFb0F0DEwAR3Uo3z3NSCrkOs7p6TMBechjUD%0AiaQAd6DUjcR107ccelKg9Noh%2BDKd1ADA3%2B1f81x2mZcURoFjlZz7YgewY98T2A4j%0AOTLS1R1V00NWUnI4LWTIhV8JCyyAgVZ%2B1lNmIFJ%2Bj5Alvm1Xh%2FoqiXqqZ5tzTsCx%0AjXw3QexfZ0UIniB9fgIUgLlqtATzScb1GdFqBP7b2%2FCMXed21gaCULL%2BXDGBzuV%2F%0AEuk5DfotpMY185LzJw%3D%3D%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "OU=T:,OU=L:333",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDETCCAfkCFC5rTqmSB25LaMQFpKvi%2F46EVD8WMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTk0MTA3WhcNMzMwNjI1MTk0MTA3WjBaMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczEL%0AMAkGA1UECwwCVDoxDjAMBgNVBAsMBUw6MzMzMIIBIjANBgkqhkiG9w0BAQEFAAOC%0AAQ8AMIIBCgKCAQEAlTfT2lYORGLDdXzoXYtpxLQtqBEko8WJzfhkAT6gDRPhFOxO%0Ad6bVqz0eM0fyBp6vZt1SQN%2B4xinU%2B79EJk3ve9XcWEJEgrsdtPO5hgG9jFcrzmvX%0AAhrYfa%2FRBVWvQThtClxgzq7JoQH20sYIXi5wnR9uPTv%2FCEH%2B%2F%2F5Q7c05j1VwqDA4%0A13w7LyjdN9wBwqHNbl2DWChUs3eOZCWHlffTZwrFYjhX7MXcllQJNPdxU13NKC4h%0A0otJIMwEhA%2FBGvUKscRhlmF1UNE7cIlJphU57a0JJyCA%2Fg1wNljvi2OYkM0NN21v%0A7cxvHauES5zhg2y6OKUN7ldg4bQJ4%2BLVvW7zeQIDAQABMA0GCSqGSIb3DQEBCwUA%0AA4IBAQCfVfUoZUMBkvaUKUAPttkmZz8lK36maKmtVMzmuKGostCD1aErkUxYBKKm%0AFSQlHgOXGgM2PIHQD%2Bc5XdU6WW3uDbKtnMrv7VDh2wdBfFWccVguqewv0LpSdbKh%0AW73drhmT8ZZ%2Fr2sfugIthiu3xIhZMaOUyLbVMsHcbqI12%2BG2M2PVnkHDe%2FxjQEk%2F%0AmEbuc2xl3fhPRa2cFv5oB0Ky8M3Z9px%2FPxdFqf1o9%2BseGyio6GewBJZXHWkQlwh9%0Am47oWgKoZqUZa7vcpJKqIbQtwIP3s%2F7fTGWHz3K8EfgKUGA1B2Vt7rZ1gKcAArCl%0AxyW3BOAp4CpyqrHP%2BElVEpgO2By2%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "OU=L:ASD",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDBDCCAewCFF1v79zx0WMEUduJjguQJbz0d0zUMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTk0MTM0WhcNMzMwNjI1MTk0MTM0WjBNMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczEO%0AMAwGA1UECwwFTDpBU0QwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCV%0AN9PaVg5EYsN1fOhdi2nEtC2oESSjxYnN%2BGQBPqANE%2BEU7E53ptWrPR4zR%2FIGnq9m%0A3VJA37jGKdT7v0QmTe971dxYQkSCux2087mGAb2MVyvOa9cCGth9r9EFVa9BOG0K%0AXGDOrsmhAfbSxgheLnCdH249O%2F8IQf7%2F%2FlDtzTmPVXCoMDjXfDsvKN033AHCoc1u%0AXYNYKFSzd45kJYeV99NnCsViOFfsxdyWVAk093FTXc0oLiHSi0kgzASED8Ea9Qqx%0AxGGWYXVQ0TtwiUmmFTntrQknIID%2BDXA2WO%2BLY5iQzQ03bW%2FtzG8dq4RLnOGDbLo4%0ApQ3uV2DhtAnj4tW9bvN5AgMBAAEwDQYJKoZIhvcNAQELBQADggEBAFJSmCE5IaKr%0A%2BXMt9Og6cwAR%2BKbbg5ktsFlwjMksAZAfqh4ej2N3M1pMhDVf2goaU3Cm9TC8N%2BvM%0ADR3VkUw8oBsZJxJgG6Ogj%2FRu0ID77sbGI5xVmPoL1yTD86EPT3YE6RExQW7gR1cG%0AhLXWd6wZtG%2FkttuaK20vV7NKSXo6ZaMHXhJLHDMym5mzaptqA1lw8dk%2FyGl7wSjw%0AKs%2FqzMbDlxMmLfl9L9Als09gqtoPBgpC%2BM%2BqskV9ycMrn6xW5t1dsk4NOZ1MwxLx%0AT1k%2Bw%2BYQEI5EPcPrdy9Ru%2F3rYT64YBsmo2CtYirxeEsEs67wmCNM2rNiCcxQ9dBS%0Ak%2BC%2FgKAdcrU%3D%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "OU=T:tenant03,OU=L:",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDFjCCAf4CFCFThS39tyW4rAUZr8WtYXOmdwg0MA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTk0MjA4WhcNMzMwNjI1MTk0MjA4WjBfMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczET%0AMBEGA1UECwwKVDp0ZW5hbnQwMzELMAkGA1UECwwCTDowggEiMA0GCSqGSIb3DQEB%0AAQUAA4IBDwAwggEKAoIBAQCVN9PaVg5EYsN1fOhdi2nEtC2oESSjxYnN%2BGQBPqAN%0AE%2BEU7E53ptWrPR4zR%2FIGnq9m3VJA37jGKdT7v0QmTe971dxYQkSCux2087mGAb2M%0AVyvOa9cCGth9r9EFVa9BOG0KXGDOrsmhAfbSxgheLnCdH249O%2F8IQf7%2F%2FlDtzTmP%0AVXCoMDjXfDsvKN033AHCoc1uXYNYKFSzd45kJYeV99NnCsViOFfsxdyWVAk093FT%0AXc0oLiHSi0kgzASED8Ea9QqxxGGWYXVQ0TtwiUmmFTntrQknIID%2BDXA2WO%2BLY5iQ%0AzQ03bW%2FtzG8dq4RLnOGDbLo4pQ3uV2DhtAnj4tW9bvN5AgMBAAEwDQYJKoZIhvcN%0AAQELBQADggEBADXinp1FqFoaIGL61C3jBaFYm2dmsMsZVSQNOBo8UVbZDyA7biTe%0AMsItzoo04wAed1pIJWQ%2Ff7zfWAdaaKVvw2qe%2FSJQKJdkfrya3FvV8lZy3MV0ZF6l%0A9%2BqWRG%2BfAslTKYWpswvIPtmPdN8Rd0SDeQpTjiYm0vDTf6mqqK9nJYYfuIa9L%2B5Q%0A5U37FO0%2FtwrzctCa6r8bhcFuEQDPv1caOfFyh9qJm5mZ4QC4b%2B03%2B%2Bqah7VcOhfT%0Adfd28DN304EEI0DasHCPGDJ%2FdIRhopV60V52JvVBpfBoeaY3i7EtcRlfKJz90JlH%0A04lCEa%2BSppmQBqkj1ENuxHZJkKD7U5tdFNY%3D%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "",
                "-----BEGIN%20CERTIFICATE-----%0AMIIC9DCCAdwCFEogmxcCQQBqKlMuITX7TGOysZvyMA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI4MTk0MjUyWhcNMzMwNjI1MTk0MjUyWjA9MQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczCC%0AASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJU309pWDkRiw3V86F2LacS0%0ALagRJKPFic34ZAE%2BoA0T4RTsTnem1as9HjNH8gaer2bdUkDfuMYp1Pu%2FRCZN73vV%0A3FhCRIK7HbTzuYYBvYxXK85r1wIa2H2v0QVVr0E4bQpcYM6uyaEB9tLGCF4ucJ0f%0Abj07%2FwhB%2Fv%2F%2BUO3NOY9VcKgwONd8Oy8o3TfcAcKhzW5dg1goVLN3jmQlh5X302cK%0AxWI4V%2BzF3JZUCTT3cVNdzSguIdKLSSDMBIQPwRr1CrHEYZZhdVDRO3CJSaYVOe2t%0ACScggP4NcDZY74tjmJDNDTdtb%2B3Mbx2rhEuc4YNsujilDe5XYOG0CePi1b1u83kC%0AAwEAATANBgkqhkiG9w0BAQsFAAOCAQEAIx7OdG75CwBaHgEL8YK767YJU0iNi3gT%0AqFHzeC0NQBsm0ho6bPeuu%2B5gHG2y64wRME3sbh3W751jOrG%2BfD%2BQcbCGhqDr8t2P%0A514W5ayCWPvYGauycInBV2QjYxtlS3l8hYgaHiubu5yLdXd06izwmEs%2BzXtvw2A4%0AjSEDSJIeLMygggr04LUt3ebmVwO0h17n9MirmqhL3yWqU1X7npWnn%2B7aOR42KHh1%0AtfMFVZj704eog0%2F9rgcb%2Fplfy74rPU2RbhrrjwIpInJqtG%2FRZ518c8GP9%2FGe51cw%0A%2FUVr5yl3rfdTeSygAy9erIZ%2B8f1sw8CTl8lB%2BxOTig9%2FLMhvNyztRQ%3D%3D%0A-----END%20CERTIFICATE-----");
        subjectPemMap.put(
                "OU=T:tenant03,OU=L:333",
                "-----BEGIN%20CERTIFICATE-----%0AMIIDGTCCAgECFDlHmf5G3dIKpweJUX%2FBuNzxYOj6MA0GCSqGSIb3DQEBCwUAMDAx%0AEjAQBgNVBAMMCWNsaWVudC1jYTENMAsGA1UECgwEVGVzdDELMAkGA1UEBhMCVVMw%0AHhcNMjMwNjI5MDEwMDE3WhcNMzMwNjI2MDEwMDE3WjBiMQswCQYDVQQGEwJDQTEL%0AMAkGA1UECAwCT04xDzANBgNVBAcMBk90dGF3YTEQMA4GA1UECgwHb3Blbm5tczET%0AMBEGA1UECwwKVDp0ZW5hbnQwMzEOMAwGA1UECwwFTDozMzMwggEiMA0GCSqGSIb3%0ADQEBAQUAA4IBDwAwggEKAoIBAQCVN9PaVg5EYsN1fOhdi2nEtC2oESSjxYnN%2BGQB%0APqANE%2BEU7E53ptWrPR4zR%2FIGnq9m3VJA37jGKdT7v0QmTe971dxYQkSCux2087mG%0AAb2MVyvOa9cCGth9r9EFVa9BOG0KXGDOrsmhAfbSxgheLnCdH249O%2F8IQf7%2F%2FlDt%0AzTmPVXCoMDjXfDsvKN033AHCoc1uXYNYKFSzd45kJYeV99NnCsViOFfsxdyWVAk0%0A93FTXc0oLiHSi0kgzASED8Ea9QqxxGGWYXVQ0TtwiUmmFTntrQknIID%2BDXA2WO%2BL%0AY5iQzQ03bW%2FtzG8dq4RLnOGDbLo4pQ3uV2DhtAnj4tW9bvN5AgMBAAEwDQYJKoZI%0AhvcNAQELBQADggEBAHXy602DVaRfmUXFUNyNP9aIpHugILs7LubaEXlD%2BcGcNbMg%0A87GXNyg%2FAmObAmTIcGfPy9zaf4P%2Bhc%2FzKWUI65%2BAuczMhg%2FA4l3ucwLYBNwQ9IKt%0AAUHGPj1FWol4893E5z2Jft7klIwAD%2BAXujqjt0Q51795xlDz4%2F4yKRizTq5xlDF7%0At2Vgl9jVXEuW50D67CFaJOqaJHrr83xjIc%2FTIsSmFpsxg%2BHC%2B2%2Fp2uO96fDhuI5C%0Ax2AuEV0vbzp%2Fgmfy5BdAaXfuR0r9YGLFQ6wAZW%2F9F504STeH57D2G55V%2FsXI7sAH%0A1f0TyorwIBlVliaIKkW%2FpOqRxufcd9%2FVnB0frgM%3D%0A-----END%20CERTIFICATE-----");
    }

    public void externalHttpPortInSystemProperty(String propertyName) {
        String value = System.getProperty(propertyName);
        int port = Integer.parseInt(value);
        this.clientBuilder = HttpRequest.newBuilder();
        this.uri = URI.create("http://" + LOCALHOST + ":" + port + "/certificate/debug");
        LOG.info("Using external service address {}", uri);
    }

    public CompletableFuture<Map<String, List<String>>> validateCertificateData(String dn) {
        HttpRequest rq = clientBuilder
                .uri(uri)
                .GET()
                .header("ssl-client-cert", subjectPemMap.get(dn))
                .build();
        System.out.println(rq);
        return HttpClient.newHttpClient()
                .sendAsync(rq, BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() != 200) {
                        throw new IllegalArgumentException("Result failed to pass validation rules");
                    }
                    return response.headers().map();
                })
                .whenComplete((result, error) -> {
                    if (error != null) {
                        LOG.warn("Error while awaiting service answer", error);
                        return;
                    }
                    LOG.info("Received service headers {}", result);
                });
    }
}
