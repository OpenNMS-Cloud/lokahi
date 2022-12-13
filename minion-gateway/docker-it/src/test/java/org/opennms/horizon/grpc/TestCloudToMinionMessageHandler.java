/*
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
 */

package org.opennms.horizon.grpc;

import io.grpc.stub.StreamObserver;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class TestCloudToMinionMessageHandler implements StreamObserver<CloudToMinionMessage> {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(TestCloudToMinionMessageHandler.class);

    private Logger LOG = DEFAULT_LOGGER;

    private List<CloudToMinionMessage> receivedMessages = new LinkedList<>();

    private final Object lock = new Object();

//========================================
// StreamObserver Interface
//----------------------------------------

    @Override
    public void onNext(CloudToMinionMessage value) {
        LOG.info("RECEIVED CLOUD-TO-MINION MESSAGE; type={}",
            value.hasTwinResponse() ? "TWIN-RESPONSE" : value.getAnyVal().getTypeUrl());

        synchronized (lock) {
            receivedMessages.add(value);
        }
    }

    @Override
    public void onError(Throwable t) {

    }

    @Override
    public void onCompleted() {

    }

//========================================
// Test Data Access
//----------------------------------------

    public CloudToMinionMessage[] getReceivedMessagesSnapshot() {
        synchronized (lock) {
            return receivedMessages.toArray(new CloudToMinionMessage[0]);
        }
    }

}
