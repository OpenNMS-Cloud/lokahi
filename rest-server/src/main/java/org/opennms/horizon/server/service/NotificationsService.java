package org.opennms.horizon.server.service;

import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import org.opennms.horizon.server.service.gateway.NotificationGateway;
import org.opennms.horizon.shared.dto.notifications.PagerDutyConfigDTO;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@GraphQLApi
@Service
public class NotificationsService {
    private final NotificationGateway gateway;

    public NotificationsService(NotificationGateway gateway) {
        this.gateway = gateway;
    }

    @GraphQLMutation
    public Mono<Void> savePagerDutyConfig(PagerDutyConfigDTO config, @GraphQLEnvironment ResolutionEnvironment env) {
        return gateway.post(NotificationGateway.URL_PATH_NOTIFICATIONS_CONFIG, gateway.getAuthHeader(env), config, Void.class);
    }

}
