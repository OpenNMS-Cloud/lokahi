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
package org.opennms.horizon.testtool.miniongateway.wiremock.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.opennms.cloud.grpc.minion.SinkMessage;

@Data
@Builder
@Jacksonized // make sure we can deserialize with Jackson
@JsonIgnoreProperties(ignoreUnknown = true)
public class SinkMessageDto {

    private final String messageId;
    private final String systemId;
    private final String moduleId;

    public static SinkMessageDto from(final SinkMessage sinkMessage) {
        Objects.requireNonNull(sinkMessage);
        return builder()
                .messageId(sinkMessage.getMessageId())
                .systemId(sinkMessage.getIdentity().getSystemId())
                .moduleId(sinkMessage.getModuleId())
                .build();
    }
}
