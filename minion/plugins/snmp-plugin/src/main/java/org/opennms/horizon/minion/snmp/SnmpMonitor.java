/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2002-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.snmp;

import com.google.protobuf.Any;
import org.opennms.horizon.minion.plugin.api.MonitoredService;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse.Status;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponseImpl.ServiceMonitorResponseImplBuilder;
import org.opennms.horizon.shared.snmp.SnmpAgentConfig;
import org.opennms.horizon.shared.snmp.SnmpObjId;
import org.opennms.horizon.shared.snmp.SnmpUtils;
import org.opennms.horizon.shared.snmp.StrategyResolver;
import org.opennms.snmp.contract.SnmpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * <P>
 * This class is designed to be used by the service poller framework to test the
 * availability of the SNMP service on remote interfaces. The class implements
 * the ServiceMonitor interface that allows it to be used along with other
 * plug-ins by the service poller framework.
 * </P>
 * <p>
 * This does SNMP and therefore relies on the SNMP configuration so it is not distributable.
 * </p>
 *
 * @author <A HREF="mailto:tarus@opennms.org">Tarus Balog </A>
 * @author <A HREF="mailto:mike@opennms.org">Mike Davidson </A>
 * @author <A HREF="http://www.opennms.org/">OpenNMS </A>
 */
public class SnmpMonitor extends SnmpMonitorStrategy {
    
    public static final Logger LOG = LoggerFactory.getLogger(SnmpMonitor.class);

    /**
     * Default object to collect if "oid" property not available.
     */
    private static final String DEFAULT_OBJECT_IDENTIFIER = ".1.3.6.1.2.1.1.2.0"; // MIB-II
                                                                                // System
                                                                                // Object
                                                                                // Id

    private static final String DEFAULT_REASON_TEMPLATE = "Observed value '${observedValue}' does not meet criteria '${operator} ${operand}'";
    private final StrategyResolver strategyResolver;

    public SnmpMonitor(StrategyResolver strategyResolver) {
        this.strategyResolver = strategyResolver;
    }

    /**
     * {@inheritDoc}
     *
     * <P>
     * The poll() method is responsible for polling the specified address for
     * SNMP service availability.
     * </P>
     * @exception RuntimeException
     *                Thrown for any unrecoverable errors.
     */

    @Override
    public CompletableFuture<ServiceMonitorResponse> poll(MonitoredService svc, Any config) {

        CompletableFuture<ServiceMonitorResponse> future = null;
        String hostAddress = null;

        // Establish SNMP session with interface
        //
        try {
            if (! config.is(SnmpRequest.class)) {
                throw new IllegalArgumentException("config must be an SnmpRequest; type-url=" + config.getTypeUrl());
            }

            SnmpRequest snmpRequest = config.unpack(SnmpRequest.class);

            // Retrieve this interface's SNMP peer object
            //
            SnmpAgentConfig agentConfig = getAgentConfig(svc);
            hostAddress = snmpRequest.getHost();

            // Get configuration parameters
            //
            String oid = snmpRequest.getOid();
            // String operator = ParameterMap.getKeyedString(config, "operator", null);
            String operator = null;
            // String operand = ParameterMap.getKeyedString(config, "operand", null);
            String operand = null;
            // String walkstr = ParameterMap.getKeyedString(config, "walk", "false");
            // String matchstr = ParameterMap.getKeyedString(config, "match-all", "true");
            // int countMin = ParameterMap.getKeyedInteger(config, "minimum", 0);
            // int countMax = ParameterMap.getKeyedInteger(config, "maximum", 0);
            // String reasonTemplate = ParameterMap.getKeyedString(config, "reason-template", DEFAULT_REASON_TEMPLATE);
            String reasonTemplate = DEFAULT_REASON_TEMPLATE;
            // String hexstr = ParameterMap.getKeyedString(config, "hex", "false");
            String hexstr = "false";

            hex = "true".equalsIgnoreCase(hexstr);
            // set timeout and retries on SNMP peer object
            //
            agentConfig.setTimeout((int) snmpRequest.getTimeout());
            agentConfig.setRetries(snmpRequest.getRetries());
            // agentConfig.setPort(ParameterMap.getKeyedInteger(config, "port", agentConfig.getPort()));

            // Squirrel the configuration parameters away in a Properties for later expansion if service is down
            // Properties svcParams = new Properties();
            // svcParams.setProperty("oid", oid);
            // svcParams.setProperty("operator", String.valueOf(operator));
            // svcParams.setProperty("operand", String.valueOf(operand));
            // svcParams.setProperty("walk", walkstr);
            // svcParams.setProperty("matchAll", matchstr);
            // svcParams.setProperty("minimum", String.valueOf(countMin));
            // svcParams.setProperty("maximum", String.valueOf(countMax));
            // svcParams.setProperty("timeout", String.valueOf(agentConfig.getTimeout()));
            // svcParams.setProperty("retry", String.valueOf(agentConfig.getRetries()));
            // svcParams.setProperty("retries", svcParams.getProperty("retry"));
            // svcParams.setProperty("ipaddr", hostAddress);
            // svcParams.setProperty("port", String.valueOf(agentConfig.getPort()));
            // svcParams.setProperty("hex", hexstr);


//            TODO: Removing to decouple from horizon core
//            TimeoutTracker tracker = new TimeoutTracker(parameters, agentConfig.getRetries(), agentConfig.getTimeout());
//            tracker.reset();
//            tracker.startAttempt();

            // This if block will count the number of matches within a walk and mark the service
            // as up if it is between the minimum and maximum number, down if otherwise. Setting
            // the parameter "matchall" to "count" will act as if "walk" has been set to "true".

                if (DEFAULT_REASON_TEMPLATE.equals(reasonTemplate)) {
                    if (operator != null) {
                        reasonTemplate = "Observed value '${observedValue}' does not meet criteria '${operator} ${operand}'";
                    } else {
                        reasonTemplate = "Observed value '${observedValue}' was null";
                    }
                }

            final String finalHostAddress = hostAddress;
            SnmpObjId snmpObjectId = SnmpObjId.get(oid);
            future = SnmpUtils.getAsync(agentConfig, (SnmpObjId[]) Arrays.asList(snmpObjectId).toArray()).
                thenApply(result -> {
                    ServiceMonitorResponseImplBuilder builder = ServiceMonitorResponseImpl.builder()
                        .status(Status.Unknown);

                    Map<String, Number> metrics = new HashMap<>();

                    if (result[0] != null) {
                        // svcParams.setProperty("observedValue", getStringValue(result[0]));
                        LOG.debug("poll: SNMP poll succeeded, addr={} oid={} value={}", finalHostAddress, oid, result);

                        if (result[0].isNumeric()) {
                            metrics.put("observedValue", result[0].toLong());
                        }

                        if (meetsCriteria(result[0], operator, operand)) {
                            builder.status(Status.Up);
                        } else {
                            builder.status(Status.Down);
                        }
                    } else {
                        String reason = "SNMP poll failed, addr=" + finalHostAddress + " oid=" + oid;
                        LOG.debug(reason);
                    }

                    builder.properties(metrics);
                    return (ServiceMonitorResponse) builder.build();
                }).orTimeout(agentConfig.getTimeout(), TimeUnit.MILLISECONDS);

            return future;
        } catch (NumberFormatException e) {
            LOG.debug("Number operator used in a non-number evaluation", e);
            return CompletableFuture.completedFuture(ServiceMonitorResponseImpl.builder().reason(e.getMessage()).status(Status.Unknown).build());
        } catch (IllegalArgumentException e) {
            LOG.debug("Invalid SNMP Criteria", e);
            return CompletableFuture.completedFuture(ServiceMonitorResponseImpl.builder().reason(e.getMessage()).status(Status.Unknown).build());
        } catch (Throwable t) {
            LOG.debug("Unexpected exception during SNMP poll of interface {}", hostAddress, t);
            return CompletableFuture.completedFuture(ServiceMonitorResponseImpl.builder().reason(t.getMessage()).status(Status.Unknown).build());
        }
    }

}
