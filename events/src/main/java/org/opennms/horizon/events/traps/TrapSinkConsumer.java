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

package org.opennms.horizon.events.traps;

import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.events.repository.EventRepository;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@PropertySource("classpath:application.yml")
public class TrapSinkConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(TrapSinkConsumer.class);

    @Autowired
    private EventRepository eventRepository;

    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(byte[] data) {

        try {
            TrapLogDTO trapLogDTO = TrapLogDTO.parseFrom(data);
            LOG.debug("Received trap {}", trapLogDTO);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
    }
}
