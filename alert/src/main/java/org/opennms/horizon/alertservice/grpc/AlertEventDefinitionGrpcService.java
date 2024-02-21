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
package org.opennms.horizon.alertservice.grpc;

import io.grpc.stub.StreamObserver;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionProto;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionServiceGrpc;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsRequest;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsResponse;
import org.opennms.horizon.alertservice.db.entity.EventDefinition;
import org.opennms.horizon.alertservice.db.repository.EventDefinitionRepository;
import org.opennms.horizon.alertservice.mapper.EventDefinitionMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertEventDefinitionGrpcService
        extends AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceImplBase {

    private final EventDefinitionRepository eventDefinitionRepository;

    private final EventDefinitionMapper eventDefinitionMapper;

    @Override
    public void listAlertEventDefinitions(
            ListAlertEventDefinitionsRequest request,
            StreamObserver<ListAlertEventDefinitionsResponse> responseObserver) {
        List<EventDefinition> eventDefinitions = eventDefinitionRepository.findByEventType(request.getEventType());
        List<AlertEventDefinitionProto> alertEventDefinitionProtos = eventDefinitions.stream()
                .map(eventDefinitionMapper::entityToProto)
                .toList();
        ListAlertEventDefinitionsResponse result = ListAlertEventDefinitionsResponse.newBuilder()
                .addAllAlertEventDefinitions(alertEventDefinitionProtos)
                .build();
        responseObserver.onNext(result);
        responseObserver.onCompleted();
    }
}
