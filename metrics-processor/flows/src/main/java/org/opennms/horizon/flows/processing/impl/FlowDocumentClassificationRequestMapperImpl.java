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
package org.opennms.horizon.flows.processing.impl;

import java.util.function.Function;
import lombok.Setter;
import org.opennms.horizon.flows.classification.ClassificationRequest;
import org.opennms.horizon.flows.classification.persistence.api.Protocol;
import org.opennms.horizon.flows.classification.persistence.api.Protocols;
import org.opennms.horizon.flows.document.FlowDocument;
import org.opennms.horizon.flows.processing.FlowDocumentClassificationRequestMapper;

public class FlowDocumentClassificationRequestMapperImpl implements FlowDocumentClassificationRequestMapper {

    // Testability (TODO: stop using static methods in Protocols and replace this Op with an injected instance)
    @Setter
    private Function<Integer, Protocol> protocolLookupOp = Protocols::getProtocol;

    @Override
    public ClassificationRequest createClassificationRequest(FlowDocument document, String location) {
        ClassificationRequest request = new ClassificationRequest();
        if (document.hasProtocol()) {
            request.setProtocol(protocolLookupOp.apply(document.getProtocol().getValue()));
        }
        request.setLocation(location);
        request.setExporterAddress(document.getHost());
        request.setDstAddress(document.getDstAddress());
        if (document.hasDstPort()) {
            request.setDstPort(document.getDstPort().getValue());
        }
        request.setSrcAddress(document.getSrcAddress());
        if (document.hasSrcPort()) {
            request.setSrcPort(document.getSrcPort().getValue());
        }

        return request;
    }
}
