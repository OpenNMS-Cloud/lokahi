/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.server.service;

import org.opennms.horizon.server.service.gateway.PlatformGateway;
import org.opennms.horizon.shared.dto.event.AlarmAckDTO;
import org.opennms.horizon.shared.dto.event.AlarmCollectionDTO;
import org.springframework.stereotype.Service;

import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLMutation;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import io.leangen.graphql.spqr.spring.annotations.GraphQLApi;
import reactor.core.publisher.Mono;


@GraphQLApi
@Service
public class AlarmService {
  private final PlatformGateway gateway;

  public AlarmService(PlatformGateway gateway) {
    this.gateway = gateway;
  }

  @GraphQLQuery
  public Mono<AlarmCollectionDTO> listAlarms(@GraphQLEnvironment ResolutionEnvironment env) {
    return gateway.get(PlatformGateway.URL_PATH_ALARMS_LIST, gateway.getAuthHeader(env), AlarmCollectionDTO.class);
  }

  @GraphQLMutation
  public Mono<String> clearAlarm(@GraphQLArgument(name = "id") Long id, AlarmAckDTO ackDTO, @GraphQLEnvironment ResolutionEnvironment env) {
    return gateway.post(String.format(PlatformGateway.URL_PATH_ALARMS_CLEAR, id), gateway.getAuthHeader(env), ackDTO, String.class);
  }
}
