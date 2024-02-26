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
package org.opennms.horizon.minion.taskset.worker.metrics.linux;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Service that runs in the background and reports interface metrics every few seconds via logging (5 seconds by default).
 *
 * WARNING: this implementation is very simplistic and fragile to changes in the /proc/net/dev format (although this
 * format appears to be stable - unchanged for about 10 years).
 *
 * Sample /proc/net/dev content:
 *
 *   Inter-|   Receive                                                |  Transmit
 *    face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
 *       lo: 42483875821 60427385    0    0    0     0          0         0 42483875821 60427385    0    0    0     0       0          0
 *   enp42s0: 1986914701387 3096125226    0 2468    0     0          0   1341512 981280814993 2726199798    0    0    0     0       0          0
 */
// @Component
@Slf4j
public class NetworkInterfaceMetricReporterService {
    public static final int DEFAULT_REPORT_INTERVAL = 30_000;
    public static final String[] PROC_NET_DEV_FIELDS = {
        "dev",
        "rx-bytes",
        "rx-pkts",
        "rx-errs",
        "rx-drop",
        "rx-fifo",
        "rx-frame",
        "rx-compressed",
        "rx-multicast",
        "tx-bytes",
        "tx-pkts",
        "tx-errs",
        "tx-drop",
        "tx-fifo",
        "tx-colls",
        "tx-carrier",
        "tx-compressed"
    };

    private Thread runnerThread;

    // TODO: move to blueprint
    //    @Value("${poc.network.stat.devs:#{null}}")
    private Set<String> reportDevices;

    //    @Value("${poc.network.stat.enable:false}")
    private boolean enable;

    //    @Value("${poc.network.stat.interval:" + DEFAULT_REPORT_INTERVAL + "}")
    private int reportInterval;

    private boolean shutdown = false;

    private Map<String, Map<String, Long>> previousStats;

    // ========================================
    // Lifecycle
    // ----------------------------------------

    //    @PostConstruct
    public void start() {
        if (!enable) {
            return;
        }

        runnerThread = new Thread(this::run, "network-interface-metric-reporter-service");

        runnerThread.start();
    }

    //    @PreDestroy
    public void shutdown() {
        shutdown = true;

        if (runnerThread != null) {
            runnerThread.interrupt();
        }
    }

    // ========================================
    // Internals
    // ----------------------------------------

    private void run() {
        while (!shutdown) {
            try {
                produceReport();

                Thread.sleep(reportInterval);
            } catch (InterruptedException intExc) {
                log.debug("Interrupted", intExc);
            }
        }
    }

    private void produceReport() {
        Map<String, Map<String, Long>> stats = readStats();

        if ((stats == null) || (stats.isEmpty())) {
            log.info("EMPTY REPORT - no devices or failure reading /proc/net/dev?");
        } else {
            for (Map.Entry<String, Map<String, Long>> oneDevStats : stats.entrySet()) {
                if ((reportDevices == null) || (reportDevices.contains(oneDevStats.getKey()))) {
                    produceInterfaceReport(oneDevStats.getKey(), oneDevStats.getValue(), false);
                }
            }

            if (previousStats != null) {
                produceDeltaReport(stats);
            }

            previousStats = stats;
        }
    }

    private void produceDeltaReport(Map<String, Map<String, Long>> stats) {
        for (Map.Entry<String, Map<String, Long>> oneDevStats : stats.entrySet()) {
            if ((reportDevices == null) || (reportDevices.contains(oneDevStats.getKey()))) {
                Map<String, Long> previousIfStats = previousStats.get(oneDevStats.getKey());

                if (previousIfStats != null) {
                    Map<String, Long> deltaStats = calcDelta(previousIfStats, oneDevStats.getValue());
                    produceInterfaceReport(oneDevStats.getKey(), deltaStats, true);
                }
            }
        }
    }

    private void produceInterfaceReport(String ifName, Map<String, Long> stats, boolean deltaInd) {
        StringBuilder buffer = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, Long> oneStat : stats.entrySet()) {
            if (first) {
                first = false;
            } else {
                buffer.append("; ");
            }

            buffer.append(oneStat.getKey());
            buffer.append("=");
            buffer.append(oneStat.getValue());
        }

        String label = "NET-STATS";
        if (deltaInd) {
            label = "NET-STATS-DELTA";
        }
        log.info("{}: dev={}; {}", label, ifName, buffer);
    }

    private Map<String, Map<String, Long>> readStats() {
        Map<String, Map<String, Long>> metrics = new TreeMap<>();

        try (InputStream inputStream = new FileInputStream("/proc/net/dev")) {
            try (InputStreamReader reader = new InputStreamReader(inputStream)) {
                try (BufferedReader bufferedReader = new BufferedReader(reader)) {
                    skipHeaders(bufferedReader);

                    String oneLine = bufferedReader.readLine();
                    while (oneLine != null) {
                        processOneEntry(oneLine, metrics);

                        oneLine = bufferedReader.readLine();
                    }
                }
            }

            return metrics;
        } catch (IOException ioExc) {
            log.error("Failed to read /proc/net/dev", ioExc);
        }

        return null;
    }

    private Map<String, Long> calcDelta(Map<String, Long> first, Map<String, Long> second) {
        Map<String, Long> delta = new TreeMap<>();

        for (String oneMetric : first.keySet()) {
            delta.put(oneMetric, second.get(oneMetric) - first.get(oneMetric));
        }

        return delta;
    }

    /**
     * @param bufferedReader
     * @return
     * @throws IOException
     */
    private void skipHeaders(BufferedReader bufferedReader) throws IOException {
        String oneLine = bufferedReader.readLine();

        while ((oneLine != null) && (!oneLine.contains("bytes"))) {
            oneLine = bufferedReader.readLine();
        }
    }

    private void processOneEntry(String oneLine, Map<String, Map<String, Long>> metrics) {
        String normalized = oneLine.replace(':', ' ').trim();
        String[] parts = normalized.split("\\s+");

        String ifName = parts[0];

        int cur = 1;
        while (cur < parts.length) {
            String value = parts[cur].trim();

            if (!value.isBlank()) {
                String fieldName;
                if (cur < PROC_NET_DEV_FIELDS.length) {
                    fieldName = PROC_NET_DEV_FIELDS[cur];
                } else {
                    fieldName = "misc" + cur;
                }

                long longValue = safeParseLong(value);

                Map<String, Long> metricsForInterface =
                        metrics.computeIfAbsent(ifName, (name) -> new TreeMap<String, Long>());
                metricsForInterface.put(fieldName, longValue);
            }

            cur++;
        }
    }

    private Long safeParseLong(String text) {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException nfExc) {
            return null;
        }
    }
}
