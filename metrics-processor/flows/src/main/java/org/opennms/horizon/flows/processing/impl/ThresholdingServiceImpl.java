/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2019-2020 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2020 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.processing.impl;

import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.opennms.horizon.flows.processing.kvstore.BlobStore;
import org.opennms.horizon.flows.processing.thresholding.ThresholdStateMonitor;
import org.opennms.horizon.flows.processing.thresholding.ThresholdingEventProxy;
import org.opennms.horizon.flows.processing.thresholding.ThresholdingSessionKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import org.opennms.horizon.flows.processing.thresholding.ThresholdingService;
import org.opennms.horizon.flows.processing.thresholding.ThresholdingSession;
import org.opennms.horizon.flows.processing.thresholding.ThresholdingSessionImpl;
import org.opennms.horizon.flows.processing.thresholding.ThresholdingSetPersister;

/**
 * Thresholding Service.
 */
public class ThresholdingServiceImpl implements ThresholdingService {

    private static final Logger LOG = LoggerFactory.getLogger(ThresholdingServiceImpl.class);

    public static final List<String> UEI_LIST =
            Lists.newArrayList(EventConstants.NODE_GAINED_SERVICE_EVENT_UEI,
                               EventConstants.NODE_CATEGORY_MEMBERSHIP_CHANGED_EVENT_UEI,
                               EventConstants.RELOAD_DAEMON_CONFIG_UEI,
                               EventConstants.THRESHOLDCONFIG_CHANGED_EVENT_UEI);

    private ThresholdingSetPersister thresholdingSetPersister;

    private ThresholdingEventProxy eventProxy;
    
    private final AtomicReference<BlobStore> kvStore = new AtomicReference<>();
    
    private ThresholdStateMonitor thresholdStateMonitor;

    private final Timer reInitializeTimer = new Timer();

    private boolean isDistributed = false;

    // OSGi init entry point
    public void initOsgi() {
        // If we were started viag OSGi then we are on Sentinel therefore we will mark ourselves as being distributed
        // for thresholding
        isDistributed = true;
        
        reInitializeTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // On Sentinel we won't have access to an event manager so we will have to manage config updates via
                // timer
                reinitializeOnTimer();
            }
        }, 0, TimeUnit.MILLISECONDS.convert(5, TimeUnit.MINUTES));
    }
    
    private void reinitializeOnTimer() {
        thresholdingSetPersister.reinitializeThresholdingSets();
    }


    public ThresholdingSession createSession(int nodeId, String hostAddress, String serviceName, ServiceParameters serviceParams) {
        Objects.requireNonNull(serviceParams, "ServiceParameters must not be null");

        synchronized (kvStore) {
            if (kvStore.get() == null) {
                waitForKvStore();
            }
        }
        
        ThresholdingSessionKey sessionKey = new ThresholdingSessionKeyImpl(nodeId, hostAddress, serviceName);
        return new ThresholdingSessionImpl(this, sessionKey, serviceParams,
                                           kvStore.get(), isDistributed, thresholdStateMonitor);
    }

    public ThresholdingVisitorImpl getThresholdingVistor(ThresholdingSession session, Long sequenceNumber) throws ThresholdInitializationException {
    /*    ThresholdingSetImpl thresholdingSet = (ThresholdingSetImpl) thresholdingSetPersister.getThresholdingSet(session, eventProxy);
        return new ThresholdingVisitorImpl(thresholdingSet, eventProxy, sequenceNumber);  */
        return null;
    }


    public void setEventProxy(EventForwarder eventForwarder) {
        Objects.requireNonNull(eventForwarder);
    //    eventProxy = new ThresholdingEventProxyImpl(eventForwarder);
    }

    @Override
    public ThresholdingSetPersister getThresholdingSetPersister() {
        return thresholdingSetPersister;
    }

    public void setThresholdingSetPersister(ThresholdingSetPersister thresholdingSetPersister) {
        this.thresholdingSetPersister = thresholdingSetPersister;
    }

    public void close(ThresholdingSessionImpl session) {
        thresholdingSetPersister.clear(session);
    }

    private void waitForKvStore() {
        BlobStore osgiKvStore = null; //SERVICE_LOOKUP.lookup(BlobStore.class, null);

        /*if (osgiKvStore == null) {
            throw new RuntimeException("Timed out waiting for a key value store");
        } else {
            kvStore.set(osgiKvStore);
        } */
    }

    public void setKvStore(BlobStore keyValueStore) {
        Objects.requireNonNull(keyValueStore);

        synchronized (kvStore) {
            if (kvStore.get() == null) {
                kvStore.set(keyValueStore);
            }
        }
    }

    @VisibleForTesting
    public void setDistributed(boolean distributed) {
        isDistributed = distributed;
    }

    public void setThresholdStateMonitor(ThresholdStateMonitor thresholdStateMonitor) {
        this.thresholdStateMonitor = Objects.requireNonNull(thresholdStateMonitor);
    }
}
