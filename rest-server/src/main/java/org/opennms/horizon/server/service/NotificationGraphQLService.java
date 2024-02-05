package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.notifications.dto.PagerDutyConfigDTO;
import org.opennms.horizon.server.mapper.PagerDutyConfigMapper;
import org.opennms.horizon.server.model.notification.PagerDutyConfig;
import org.opennms.horizon.server.service.grpc.NotificationClient;
import org.opennms.horizon.server.utils.ServerHeaderUtil;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@GraphQLApi
@Service
public class NotificationGraphQLService {
    private final NotificationClient client;
    private final PagerDutyConfigMapper mapper;
    private final ServerHeaderUtil headerUtil;

    @GraphQLMutation
    public Mono<Void> savePagerDutyConfig(PagerDutyConfig config, @GraphQLEnvironment ResolutionEnvironment env) {
        PagerDutyConfigDTO protoConfigDTO = mapper.pagerDutyConfigToProto(config);

        String tenantId = headerUtil.extractTenant(env);
        PagerDutyConfigDTO.Builder dtoBuilder = PagerDutyConfigDTO.newBuilder(protoConfigDTO);
        dtoBuilder.setTenantId(tenantId);

        client.postPagerDutyConfig(dtoBuilder.build(), headerUtil.getAuthHeader(env));
        return Mono.empty();
    }
}
