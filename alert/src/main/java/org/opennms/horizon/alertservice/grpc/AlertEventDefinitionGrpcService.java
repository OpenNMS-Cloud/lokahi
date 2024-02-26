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

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionProto;
import org.opennms.horizon.alerts.proto.AlertEventDefinitionServiceGrpc;
import org.opennms.horizon.alerts.proto.EventDefinitionsByVendor;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsRequest;
import org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsResponse;
import org.opennms.horizon.alerts.proto.ListEventDefinitionsByVendor;
import org.opennms.horizon.alerts.proto.VendorList;
import org.opennms.horizon.alertservice.db.entity.EventDefinition;
import org.opennms.horizon.alertservice.db.repository.EventDefinitionRepository;
import org.opennms.horizon.alertservice.mapper.EventDefinitionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AlertEventDefinitionGrpcService
        extends AlertEventDefinitionServiceGrpc.AlertEventDefinitionServiceImplBase {

    private static final Logger LOG = LoggerFactory.getLogger(AlertEventDefinitionGrpcService.class);

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

    @Override
    public void listVendors(
            com.google.protobuf.Empty request,
            io.grpc.stub.StreamObserver<org.opennms.horizon.alerts.proto.VendorList> responseObserver) {

        try {
            var listOfVendors = eventDefinitionRepository.findDistinctVendors();
            var vendorListProto =
                    VendorList.newBuilder().addAllVendor(listOfVendors).build();
            responseObserver.onNext(vendorListProto);
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Exception while retrieving vendors", e);
            var status = Status.newBuilder()
                    .setCode(Code.INTERNAL.getNumber())
                    .setMessage("failed to retrieve vendors")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }

    @Override
    public void listAlertEventDefinitionsByVendor(
            org.opennms.horizon.alerts.proto.ListAlertEventDefinitionsRequest request,
            io.grpc.stub.StreamObserver<org.opennms.horizon.alerts.proto.ListEventDefinitionsByVendor>
                    responseObserver) {

        try {
            List<EventDefinition> eventDefinitions = eventDefinitionRepository.findByEventType(request.getEventType());
            var eventDefinitionListByVendor = eventDefinitions.stream()
                    .map(eventDefinitionMapper::entityToProto)
                    // Only filter events with reduction key
                    .filter(alertEventDefinitionProto ->
                            Strings.isNotBlank(alertEventDefinitionProto.getReductionKey()))
                    .collect(Collectors.groupingBy(AlertEventDefinitionProto::getVendor))
                    .entrySet()
                    .stream()
                    .map((entry) -> EventDefinitionsByVendor.newBuilder()
                            .setVendor(entry.getKey())
                            .addAllEventDefinition(entry.getValue())
                            .build())
                    .toList();
            responseObserver.onNext(ListEventDefinitionsByVendor.newBuilder()
                    .addAllEventDefinitionByVendor(eventDefinitionListByVendor)
                    .build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            LOG.error("Exception while retrieving eventDefinitions by vendor", e);
            var status = Status.newBuilder()
                    .setCode(Code.INTERNAL.getNumber())
                    .setMessage("failed to retrieve eventDefinitions by vendor")
                    .build();
            responseObserver.onError(StatusProto.toStatusRuntimeException(status));
        }
    }
}
