/*
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
 */

package org.opennms.horizon.minioncertmanager.stepdefs;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.opennms.horizon.minioncertmanager.MinionCertificateManagerGrpcClientUtils;
import org.opennms.horizon.minioncertmanager.RetryUtils;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidRequest;
import org.opennms.horizon.minioncertmanager.proto.IsCertificateValidResponse;
import org.opennms.horizon.minioncertmanager.proto.MinionCertificateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MinionCertificateManagerTestSteps {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionCertificateManagerTestSteps.class);

    private final Logger LOG = DEFAULT_LOGGER;

    //========================================
    // Test Injectables
    //========================================
    private final RetryUtils retryUtils;
    private final MinionCertificateManagerGrpcClientUtils clientUtils;

    private MinionCertificateRequest minionCertificateRequest;
    private GetMinionCertificateResponse getMinionCertificateResponse;

    private String serialNumber;

    //========================================
    // Lifecycle
    //========================================
    public MinionCertificateManagerTestSteps(RetryUtils retryUtils, MinionCertificateManagerGrpcClientUtils clientUtils) {
        this.retryUtils = retryUtils;
        this.clientUtils = clientUtils;
    }

    //========================================
    // Gherkin Rules
    //========================================
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
            getMinionCertificateResponse = clientUtils.getMinionCertificateManagerStub()
                .getMinionCert(minionCertificateRequest);
            return getMinionCertificateResponse;
        };
        String serialNumber = retryUtils.retry(
            () -> this.doRequestAndAssert(call),
            Objects::isNull,
            100,
            timeout,
            null);
        this.serialNumber = serialNumber;
        assertNotNull("P12 file created", serialNumber);
    }

    @Then("send isValid with last serial number and timeout {int}ms")
    public String checkLastIsValid(long timeout) {
        LOG.info("Checking certificate serial number: {}", serialNumber);
        IsCertificateValidResponse response = clientUtils.getMinionCertificateManagerStub().isCertValid(
            IsCertificateValidRequest.newBuilder().setSerialNumber(serialNumber).build());
        assertTrue("Serial number is invalid", response.getIsValid());
        return serialNumber;
    }

    //========================================
    // Internals
    //========================================
    private String doRequestAndAssert(Supplier<GetMinionCertificateResponse> supplier) {
        try {
            LOG.debug("Running request");
            GetMinionCertificateResponse message = supplier.get();
            assertFalse("Certificate is not empty", message.getCertificate().isEmpty());
            assertFalse("Password is not empty", message.getPassword().isEmpty());
            return readSerialNumber(message);
        } catch(Exception e){
            LOG.error("Fail to read serial number from p12. Error={}", e.getMessage());
            return null;
        }
    }

    private String readSerialNumber(GetMinionCertificateResponse response) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        var p12Stream = new ByteArrayInputStream(response.getCertificate().toByteArray());
        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(p12Stream, response.getPassword().toCharArray());
        X509Certificate certificate = (X509Certificate)store.getCertificate("1");
        return certificate.getSerialNumber().toString(16).toUpperCase();
    }
}
