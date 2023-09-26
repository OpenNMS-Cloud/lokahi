package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.mapper.certificate.CertificateMapper;
import org.opennms.horizon.server.model.certificate.CertificateResponse;
import org.opennms.horizon.server.service.grpc.InventoryClient;
import org.opennms.horizon.server.service.grpc.MinionCertificateManagerClient;
import org.opennms.horizon.server.utils.MinionDockerZipPackager;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcMinionCertificateManager {
    private final MinionCertificateManagerClient client;
    private final ServerHeaderUtil headerUtil;
    private final CertificateMapper mapper;
    private final InventoryClient inventoryClient;

    @GraphQLQuery(name = "getMinionCertificate")
    public Mono<CertificateResponse> getMinionCertificate(Long locationId, @GraphQLEnvironment ResolutionEnvironment env) throws IOException {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);

        var monitoringLocation = inventoryClient.getLocationById(locationId, authHeader);
        var location = monitoringLocation.getId();
        var cert = client.getMinionCert(tenantId, location, authHeader);
        var certPackage = MinionDockerZipPackager.generateZip(cert.getCertificate(), monitoringLocation.getLocation(), cert.getPassword());

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
