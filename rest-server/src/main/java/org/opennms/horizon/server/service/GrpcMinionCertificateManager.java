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
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class GrpcMinionCertificateManager {
    private final MinionCertificateManagerClient client;
    private final ServerHeaderUtil headerUtil;
    private final CertificateMapper mapper;
    private final InventoryClient inventoryClient;


    @GraphQLQuery(name = "getMinionCertificate")
    public Mono<CertificateResponse> getMinionCertificate(Long locationId, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);
        String authHeader = headerUtil.getAuthHeader(env);
        try {
            var monitoringLocation = inventoryClient.getLocationById(locationId, authHeader);
            var location = monitoringLocation.getId();
            CertificateResponse minionCert = mapper.protoToCertificateResponse(client.getMinionCert(tenantId, location, authHeader));
            return Mono.just(minionCert);
        } catch (Exception e) {
            return Mono.error(e);
        }

    }

    @GraphQLMutation(name = "revokeMinionCertificate")
    public Mono<Boolean> revokeMinionCertificate(Long locationId, @GraphQLEnvironment ResolutionEnvironment env) {
        try {
            String tenantId = headerUtil.extractTenant(env);
            client.revokeCertificate(tenantId, locationId, headerUtil.getAuthHeader(env));
            return Mono.just(true);
        } catch (Exception e) {
            return Mono.just(false);
        }
    }
}
