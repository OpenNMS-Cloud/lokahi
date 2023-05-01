package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.server.mapper.certificate.CertificateMapper;
import org.opennms.horizon.server.model.certificate.CertificateResponse;
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

    @GraphQLQuery(name = "getMinionCertificate")
    public Mono<CertificateResponse> getMinionCertificate(String location, @GraphQLEnvironment ResolutionEnvironment env) {
        String tenantId = headerUtil.extractTenant(env);

        CertificateResponse minionCert = mapper.protoToCertificateResponse(client.getMinionCert(tenantId, location, headerUtil.getAuthHeader(env)));

        return Mono.just(minionCert);
    }
}
