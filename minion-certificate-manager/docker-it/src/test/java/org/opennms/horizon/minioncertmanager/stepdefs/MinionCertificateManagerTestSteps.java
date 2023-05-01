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

import com.google.protobuf.MessageOrBuilder;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.opennms.horizon.minioncertmanager.MinionCertificateManagerGrpcClientUtils;
import org.opennms.horizon.minioncertmanager.RetryUtils;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateRequest;
import org.opennms.horizon.minioncertmanager.proto.GetMinionCertificateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MinionCertificateManagerTestSteps {
    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(MinionCertificateManagerTestSteps.class);

    private final Logger LOG = DEFAULT_LOGGER;

    //========================================
    // Test Injectables
    //========================================
    private final RetryUtils retryUtils;
    private final MinionCertificateManagerGrpcClientUtils clientUtils;

    private GetMinionCertificateRequest getMinionCertificateRequest;
    private GetMinionCertificateResponse getMinionCertificateResponse;

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

    @Given("New Get Minion Certificate with tenantId {string} for location {string}")
    public void newActiveDiscoveryWithIpAddressesAndSNMPCommunityAsAtLocation(String tenantId, String location) {
        getMinionCertificateRequest = GetMinionCertificateRequest.newBuilder()
            .setTenantId(tenantId)
            .setLocation(location)
            .build();
    }

    @Then("send Get Minion Certificate Request with timeout {int}ms and verify success")
    public void sendRequest(long timeout) throws InterruptedException {
        Supplier<MessageOrBuilder> call = () -> {
            getMinionCertificateResponse = clientUtils.getMinionCertificateManagerStub()
                .getMinionCert(getMinionCertificateRequest);
            return getMinionCertificateResponse;
        };
        boolean success = retryUtils.retry(
            () -> this.doRequestAndAssert(call),
            result -> result,
            100,
            timeout,
            false);
        assertTrue("Zip file created", success);
    }

    //========================================
    // Internals
    //========================================
    private boolean doRequestAndAssert(Supplier<MessageOrBuilder> supplier) {
        LOG.debug("Running request");
        GetMinionCertificateResponse message = (GetMinionCertificateResponse) supplier.get();
        assertFalse("Certificate is not empty", message.getCertificate().isEmpty());
        assertFalse("Password is not empty", message.getPassword().isEmpty());
        return true;
    }
}
