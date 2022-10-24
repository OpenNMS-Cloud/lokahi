package org.opennms.horizon.server.service;


import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import org.opennms.horizon.server.model.datachoices.ToggleDataChoicesDTO;
import org.opennms.horizon.server.service.gateway.PlatformGateway;
import org.opennms.horizon.shared.dto.datachoices.UsageStatisticsReportDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

@GraphQLApi
@Service
public class DataChoicesService {
    private final PlatformGateway gateway;

    public DataChoicesService(PlatformGateway gateway) {
        this.gateway = gateway;
    }

    @GraphQLQuery
    public Mono<UsageStatisticsReportDTO> getUsageStatsReport(@GraphQLEnvironment ResolutionEnvironment env) {
        String authHeader = gateway.getAuthHeader(env);
        return gateway.get(PlatformGateway.URL_PATH_DATA_CHOICES, authHeader, UsageStatisticsReportDTO.class);
    }

    @GraphQLMutation
    public Mono<Void> toggleUsageStatsReport(ToggleDataChoicesDTO toggleDataChoices,
                                             @GraphQLEnvironment ResolutionEnvironment env) {
        String uriPath = getToggleUriPath(toggleDataChoices);
        return gateway.post(uriPath, gateway.getAuthHeader(env), null, Void.class);
    }

    private String getToggleUriPath(ToggleDataChoicesDTO toggleDataChoices) {
        boolean toggle = toggleDataChoices.isToggle();

        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory();
        return uriBuilderFactory.builder()
            .path(PlatformGateway.URL_PATH_DATA_CHOICES)
            .queryParam(PlatformGateway.QUERY_PARAM_TOGGLE, toggle)
            .build().toString();
    }
}
