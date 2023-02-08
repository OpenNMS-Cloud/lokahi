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

package org.opennms.horizon.notifications.kafka;

import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.opennms.horizon.notifications.exceptions.NotificationException;
import org.opennms.horizon.notifications.exceptions.NotificationInternalException;
import org.opennms.horizon.notifications.service.NotificationService;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.opennms.horizon.shared.dto.event.AlarmDTO;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class AlarmKafkaConsumerUnitTest {
    @InjectMocks
    AlarmKafkaConsumer alarmKafkaConsumer;

    @Mock
    NotificationService notificationService;

    @Test
    public void testConsume() {
        AlarmDTO alarmDTO = new AlarmDTO();
        Map<String, Object> headers = new HashMap<>();
        alarmKafkaConsumer.consume(alarmDTO,headers);
    }

    @Test
    public void testConsumeSwallowsNotificationException() throws NotificationException {
        doThrow(NotificationInternalException.class)
            .when(notificationService).postNotification(any());

        AlarmDTO alarmDTO = new AlarmDTO();
        Map<String, Object> headers = new HashMap<>();
        alarmKafkaConsumer.consume(alarmDTO,headers);
    }
}
