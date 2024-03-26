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
public class GraphQLNotificationService {
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
