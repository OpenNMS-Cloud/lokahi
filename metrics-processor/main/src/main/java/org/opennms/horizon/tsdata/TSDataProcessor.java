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

package org.opennms.horizon.tsdata;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.opennms.horizon.shared.constants.GrpcConstants;
import org.apache.logging.log4j.util.Strings;
import org.opennms.taskset.contract.TenantedTaskSetResults;
import org.springframework.context.annotation.PropertySource;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.google.protobuf.InvalidProtocolBufferException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@PropertySource("classpath:application.yml")
public class TSDataProcessor {

    private final TaskSetResultProcessor taskSetResultProcessor;

    public TSDataProcessor(TaskSetResultProcessor taskSetResultProcessor) {
        this.taskSetResultProcessor = taskSetResultProcessor;
    }

    //headers for future use.
    @KafkaListener(topics = "${kafka.topics}", concurrency = "1")
    public void consume(@Payload byte[] data, @Headers Map<String, Object> headers) {
        try {
            TenantedTaskSetResults results = TenantedTaskSetResults.parseFrom(data);
            String tenantId = results.getTenantId();
            if (Strings.isBlank(tenantId)) {
                throw new RuntimeException("Missing tenant id");
            }

            results.getResultsList().forEach(result -> CompletableFuture.supplyAsync(() -> {
                taskSetResultProcessor.processTaskResult(tenantId, result);
                return null;
            }));
        } catch (InvalidProtocolBufferException e) {
            log.error("Invalid data from kafka", e);
        }
    }

    private String getTenantId(Map<String, Object> headers) {
        return Optional.ofNullable(headers.get(GrpcConstants.TENANT_ID_KEY))
            .map(tenantId -> {
                if (tenantId instanceof byte[]) {
                    return new String((byte[]) tenantId);
                }
                if (tenantId instanceof String) {
                    return (String) tenantId;
                }
                return "" + tenantId;
            })
            .orElseThrow(() -> new RuntimeException("Could not determine tenant id"));
    }
}
