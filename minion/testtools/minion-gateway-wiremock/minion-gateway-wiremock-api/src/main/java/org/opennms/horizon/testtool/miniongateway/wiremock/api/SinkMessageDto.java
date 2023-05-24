/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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

package org.opennms.horizon.testtool.miniongateway.wiremock.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;
import org.opennms.cloud.grpc.minion.SinkMessage;

import java.util.Objects;

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
