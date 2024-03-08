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
package org.opennms.horizon.minioncertmanager.stepdefs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.function.Supplier;
import org.opennms.horizon.minioncertmanager.MinionCertificateManagerGrpcClientUtils;
import org.opennms.horizon.minioncertmanager.RetryUtils;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidRequest;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinionCertificateManagerTestSteps {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionCertificateManagerTestSteps.class);

    private final Logger LOG = DEFAULT_LOGGER;

    // ========================================
    // Test Injectables
    // ========================================
    private final RetryUtils retryUtils;
    private final MinionCertificateManagerGrpcClientUtils clientUtils;

    private MinionCertificateRequest minionCertificateRequest;
    private GetMinionCertificateResponse getMinionCertificateResponse;

    private String serialNumber;

    // ========================================
    // Lifecycle
    // ========================================
    public MinionCertificateManagerTestSteps(
            RetryUtils retryUtils, MinionCertificateManagerGrpcClientUtils clientUtils) {
        this.retryUtils = retryUtils;
        this.clientUtils = clientUtils;
    }

    // ========================================
    // Gherkin Rules
    // ========================================
    @Given("External GRPC Port in system property {string}")
    public void externalGRPCPortInSystemProperty(String propertyName) {
        clientUtils.externalGRPCPortInSystemProperty(propertyName);
    }

    @Given("Grpc TenantId {string}")
    public void grpcTenantId(String tenantId) {
        clientUtils.grpcTenantId(tenantId);
    }

    @Given("Create Grpc Connection")
    public void createGrpcConnection() {
        clientUtils.createGrpcConnection();
    }

    @Given("New Get Minion Certificate with tenantId {string} for location id {long}")
    public void newActiveDiscoveryWithIpAddressesAndSNMPCommunityAsAtLocation(String tenantId, long locationId) {
        minionCertificateRequest = MinionCertificateRequest.newBuilder()
                .setTenantId(tenantId)
                .setLocationId(locationId)
                .build();
    }

    @Then("send Get Minion Certificate Request with timeout {int}ms and verify success")
    public void sendRequest(long timeout) throws InterruptedException {
        Supplier<GetMinionCertificateResponse> call = () -> {
            getMinionCertificateResponse =
                    clientUtils.getMinionCertificateManagerStub().getMinionCert(minionCertificateRequest);
            return getMinionCertificateResponse;
        };
        String serialNumber =
                retryUtils.retry(() -> this.doRequestAndAssert(call), Objects::isNull, 100, timeout, null);
        this.serialNumber = serialNumber;
        assertNotNull("P12 file created", serialNumber);
    }

    @Then("send isValid with last serial number and timeout {int}ms")
    public String checkLastIsValid(long timeout) {
        LOG.info("Checking certificate serial number: {}", serialNumber);
        IsCertificateValidResponse response = clientUtils
                .getMinionCertificateManagerStub()
                .isCertValid(IsCertificateValidRequest.newBuilder()
                        .setSerialNumber(serialNumber)
                        .build());
        assertTrue("Serial number is invalid", response.getIsValid());
        return serialNumber;
    }

    // ========================================
    // Internals
    // ========================================
    private String doRequestAndAssert(Supplier<GetMinionCertificateResponse> supplier) {
        try {
            LOG.debug("Running request");
            GetMinionCertificateResponse message = supplier.get();
            assertFalse("Certificate is not empty", message.getCertificate().isEmpty());
            assertFalse("Password is not empty", message.getPassword().isEmpty());
            return readSerialNumber(message);
        } catch (Exception e) {
            LOG.error("Fail to read serial number from p12. Error={}", e.getMessage());
            return null;
        }
    }

    private String readSerialNumber(GetMinionCertificateResponse response)
            throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        var p12Stream = new ByteArrayInputStream(response.getCertificate().toByteArray());
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(p12Stream, response.getPassword().toCharArray());
        X509Certificate certificate = (X509Certificate) store.getCertificate("1");
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }
}
