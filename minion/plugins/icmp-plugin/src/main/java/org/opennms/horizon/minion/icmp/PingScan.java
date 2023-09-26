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

package org.opennms.horizon.minion.icmp;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponseImpl;
import org.opennms.horizon.minion.plugin.api.Scanner;
import org.opennms.horizon.shared.icmp.EchoPacket;
import org.opennms.horizon.shared.icmp.PingResponseCallback;
import org.opennms.horizon.shared.icmp.Pinger;
import org.opennms.horizon.shared.icmp.PingerFactory;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.shared.utils.IteratorUtils;
import org.opennms.icmp.contract.IpRange;
import org.opennms.icmp.contract.PingSweepRequest;
import org.opennms.taskset.contract.DiscoveryScanResult;
import org.opennms.taskset.contract.PingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class PingScan implements Scanner {

    private static final Logger LOG = LoggerFactory.getLogger(PingScan.class);

    private final PingerFactory pingerFactory;

    private final ExecutorService executor;

    public PingScan(PingerFactory pingerFactory, ExecutorService executor) {
        this.pingerFactory = pingerFactory;
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ScanResultsResponse> scan(Any config) {

        if (!config.is(PingSweepRequest.class)) {
            throw new IllegalArgumentException("configuration must be an PingSweepRequest; type-url=" + config.getTypeUrl());
        }
        var future = new CompletableFuture<ScanResultsResponse>();
        try {
            var request = config.unpack(PingSweepRequest.class);

            final Pinger pinger = pingerFactory.getInstance();
            final PingSweepResultTracker tracker = new PingSweepResultTracker();

            int packetSize = request.getPacketSize();

            var ipRanges = request.getIpRangeList();
            List<IPPollRange> ranges = new ArrayList<>();
            for (IpRange dto : request.getIpRangeList()) {
                IPPollRange pollRange = new IPPollRange(null, null, dto.getBegin(), dto.getEnd(),
                    request.getTimeout(), request.getRetries());
                ranges.add(pollRange);
            }

            // Use a RateLimiter to limit the ping packets per second that we send
            RateLimiter limiter = RateLimiter.create(request.getPacketsPerSecond());

            List<IPPollAddress> addresses = StreamSupport.stream(getAddresses(ranges).spliterator(), false)
                .filter(j -> j.address() != null).collect(Collectors.toList());

           var response = CompletableFuture.supplyAsync(() -> {
                addresses.forEach(pollAddress -> {
                    try {
                        tracker.expectCallbackFor(pollAddress.address());
                        limiter.acquire();
                        pinger.ping(pollAddress.address(),
                            pollAddress.timeout(), pollAddress.retries(), packetSize, 1, tracker);
                    } catch (Exception e) {
                        tracker.handleError(pollAddress.address(), null, e);
                        tracker.completeExceptionally(e);
                    }
                });

                try {
                    tracker.getLatch().await();
                } catch (InterruptedException e) {
                    throw Throwables.propagate(e);
                }
                tracker.complete();
                return tracker.getPingSweepResponse();
            } , executor);

            var builder = ScanResultsResponseImpl.builder();

           response.whenComplete(((pingSweepResponse, throwable) -> {
               if(throwable != null) {
                   builder.reason(throwable.getMessage());
               } else {
                   var discoveryResultBuilder = DiscoveryScanResult.newBuilder();
                   pingSweepResponse.getPingResults().forEach(result -> {
                       var pingResponse = PingResponse.newBuilder();
                       pingResponse.setIpAddress(InetAddressUtils.toIpAddrString(result.address()));
                       pingResponse.setRtt(result.rtt());
                       discoveryResultBuilder.setActiveDiscoveryId(request.getActiveDiscoveryId());
                       discoveryResultBuilder.addPingResponse(pingResponse);
                   });
                   builder.results(discoveryResultBuilder.build());
                   future.complete(builder.build());
               }
           }
           ));

           return future;

        } catch (InvalidProtocolBufferException | UnknownHostException e) {

            LOG.error("Exception while doing PingScan", e);
            future.completeExceptionally(e);
        }
        return future;
    }


    private static class PingSweepResultTracker extends CompletableFuture<PingSweepResponse>
        implements PingResponseCallback {

        private final Set<InetAddress> waitingFor = Sets.newConcurrentHashSet();
        private final CountDownLatch m_doneSignal = new CountDownLatch(1);
        private final PingSweepResponse pingSweepResponse = new PingSweepResponse();

        public void expectCallbackFor(InetAddress address) {
            waitingFor.add(address);
        }

        @Override
        public void handleResponse(InetAddress address, EchoPacket response) {
            if (response != null) {
                double responseTimeMicros = Math.round(response.elapsedTime(TimeUnit.MICROSECONDS));
                double responseTimeMillis = responseTimeMicros / 1000.0;
                PingResult sweepResult = new PingResult(address, responseTimeMillis);
                pingSweepResponse.addPingResult(sweepResult);
            }
            afterHandled(address);
        }

        @Override
        public void handleTimeout(InetAddress address, EchoPacket request) {
            afterHandled(address);
        }

        @Override
        public void handleError(InetAddress address, EchoPacket request, Throwable t) {
            afterHandled(address);
        }

        private void afterHandled(InetAddress address) {
            waitingFor.remove(address);
            if (waitingFor.isEmpty()) {
                m_doneSignal.countDown();
            }
        }

        public void complete() {
            complete(pingSweepResponse);
        }

        public PingSweepResponse getPingSweepResponse() {
            return pingSweepResponse;
        }

        public CountDownLatch getLatch() {
            return m_doneSignal;
        }

    }

    public Iterable<IPPollAddress> getAddresses(List<IPPollRange> ranges) {
        final List<Iterator<IPPollAddress>> iters = new ArrayList<>();
        for(final IPPollRange range : ranges) {
            iters.add(range.iterator());
        }
        return IteratorUtils.concatIterators(iters);
    }
}
