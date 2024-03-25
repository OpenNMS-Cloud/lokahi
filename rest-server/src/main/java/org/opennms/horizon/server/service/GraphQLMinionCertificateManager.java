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
package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.mapper.certificate.CertificateMapper;
import org.opennms.horizon.server.model.certificate.CertificateResponse;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.utils.MinionDockerZipPackager;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GraphQLMinionCertificateManager {
    private final MinionCertificateManagerClient client;
    private final ServerHeaderUtil headerUtil;
    private final CertificateMapper mapper;
    private final InventoryClient inventoryClient;

    @GraphQLQuery(name = "getMinionCertificate")
    public Mono<CertificateResponse> getMinionCertificate(
            Long locationId, @GraphQLEnvironment ResolutionEnvironment env) throws IOException {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);

        var monitoringLocation = inventoryClient.getLocationById(locationId, authHeader);
        var location = monitoringLocation.getId();
        var cert = client.getMinionCert(tenantId, location, authHeader);
        var certPackage = MinionDockerZipPackager.generateZip(
                cert.getCertificate(), monitoringLocation.getLocation(), cert.getPassword());

        CertificateResponse response = new CertificateResponse();
        response.setCertificate(certPackage);
        response.setPassword(cert.getPassword());

        return Mono.just(response);
    }

    @GraphQLMutation(name = "revokeMinionCertificate")
    public Mono<Boolean> revokeMinionCertificate(Long locationId, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);

        var monitoringLocation = inventoryClient.getLocationById(locationId, authHeader);
        client.revokeCertificate(tenantId, monitoringLocation.getId(), authHeader);
        return Mono.just(true);
    }
}
