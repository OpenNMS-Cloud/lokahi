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

package org.opennms.horizon.traps.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Any;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.opennms.horizon.config.service.api.ConfigConstants;
import org.opennms.horizon.config.service.api.ConfigService;
import org.opennms.horizon.core.lib.Logging;
import org.opennms.horizon.db.dao.api.DistPollerDao;
import org.opennms.horizon.db.dao.api.InterfaceToNodeCache;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.events.api.EventBuilder;
import org.opennms.horizon.events.api.EventConfDao;
import org.opennms.horizon.events.api.EventConstants;
import org.opennms.horizon.events.api.EventForwarder;
import org.opennms.horizon.events.api.EventListener;
import org.opennms.horizon.events.api.EventSubscriptionService;
import org.opennms.horizon.events.conf.xml.LogDestType;
import org.opennms.horizon.events.conf.xml.Logmsg;
import org.opennms.horizon.events.model.IEvent;
import org.opennms.horizon.events.xml.Event;
import org.opennms.horizon.events.xml.Events;
import org.opennms.horizon.events.xml.Log;
import org.opennms.horizon.grpc.traps.contract.TrapDTO;
import org.opennms.horizon.grpc.traps.contract.TrapLogDTO;
import org.opennms.horizon.shared.snmp.SnmpHelper;
import org.opennms.horizon.shared.snmp.traps.TrapListenerConfig;
import org.opennms.horizon.shared.snmp.traps.TrapdConfig;
import org.opennms.horizon.shared.snmp.traps.TrapdConfigBean;
import org.opennms.horizon.shared.snmp.traps.TrapdInstrumentation;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.taskset.manager.TaskSetManager;
import org.opennms.horizon.traps.config.SnmpTrapsConfig;
import org.opennms.horizon.traps.utils.EventCreator;
import org.opennms.sink.traps.contract.ListenerConfig;
import org.opennms.sink.traps.contract.SnmpV3User;
import org.opennms.sink.traps.contract.TrapConfig;
import org.opennms.taskset.contract.TaskDefinition;
import org.opennms.taskset.contract.TaskType;
import org.opennms.taskset.service.api.TaskSetPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.opennms.horizon.shared.utils.InetAddressUtils.addr;

public class TrapSinkConsumer implements  EventListener, Processor {

    public static final String LOG4J_CATEGORY = "trapd";
    public static final String TRAPS_EVENT_SOURCE = "snmp-traps-consumer";
    public static final TrapdInstrumentation trapdInstrumentation = new TrapdInstrumentation();

    private static final Logger LOG = LoggerFactory.getLogger(TrapSinkConsumer.class);

    /**
     * The name of the local host.
     */
    private static final String LOCALHOST_ADDRESS = InetAddressUtils.getLocalHostName();

    private final ObjectMapper objectMapper = new ObjectMapper();

    private EventConfDao eventConfDao;

    private EventForwarder eventForwarder;

    private InterfaceToNodeCache interfaceToNodeCache;

    private TrapdConfigBean config;

    private ConfigService configService;

    private DistPollerDao distPollerDao;

    private EventCreator eventCreator;


    private SessionUtils sessionUtils;

    private EventSubscriptionService eventSubscriptionService;

    private SnmpHelper snmpHelper;

    private TaskSetPublisher taskSetPublisher;

    private TaskSetManager taskSetManager;

    private final SnmpTrapsConfig snmpTrapsConfig = new SnmpTrapsConfig();

    public TrapSinkConsumer() {
    }


    public void init() throws Exception {
        eventSubscriptionService.addEventListener(this, EventConstants.CONFIG_UPDATED_UEI);
        eventCreator = new EventCreator(interfaceToNodeCache, eventConfDao, snmpHelper);
        // Initialize Config.
        initializeConfig();
    }

    void initializeConfig() throws IOException {
        Optional<String> optionalConfig = configService.getConfig(ConfigConstants.SNMP_TRAPS_CONFIG);
        if (optionalConfig.isEmpty()) {
            // Load initial config from resource.
            URL url = this.getClass().getResource("/trapd-config.json");
            // Validate and store config.
            config = objectMapper.readValue(url, TrapdConfigBean.class);
            configService.addConfig(ConfigConstants.SNMP_TRAPS_CONFIG, objectMapper.writeValueAsString(config), TRAPS_EVENT_SOURCE);
        } else {
            try {
                config = objectMapper.readValue(optionalConfig.get(), TrapdConfigBean.class);
            } catch (JsonProcessingException e) {
                LOG.error("Error while mapping json config to TrapConfig", e);
            }
        }
        snmpTrapsConfig.setTrapdConfig(config);
        publishTrapConfig();
    }



    private void publishTrapConfig() {

        TrapConfig trapConfig = mapConfigToProto();

        // TBD888: source tenant ID and stop using the hard-coded default here
        String tenantId = "opennms-prime";

        // Setting the location to be Default for now.
        String location = "Default";

        TaskDefinition taskDefinition = TaskDefinition.newBuilder()
            .setId("traps-config")
            .setPluginName("trapd.listener.config")
            .setType(TaskType.LISTENER)
            .setConfiguration(Any.pack(trapConfig))
            .build();

        taskSetManager.addTaskSet("opennms-prime", location, taskDefinition);
        taskSetPublisher.publishTaskSet(tenantId, location, taskSetManager.getTaskSet("opennms-prime", location));

    }

    private TrapConfig mapConfigToProto() {
        return TrapConfig.newBuilder()
            .setSnmpTrapAddress(config.getSnmpTrapAddress())
            .setSnmpTrapPort(config.getSnmpTrapPort())
            .setNewSuspectOnTrap(config.getNewSuspectOnTrap())
            .setIncludeRawMessage(config.isIncludeRawMessage())
            .setUseAddressFromVarbind(config.shouldUseAddressFromVarbind())
            .setListenerConfig(ListenerConfig.newBuilder()
                .setBatchIntervalMs(config.getBatchIntervalMs())
                .setBatchSize(config.getBatchSize())
                .setQueueSize(config.getQueueSize())
                .setNumThreads(config.getNumThreads()))
            .addAllSnmpV3User(mapSnmpV3Users())
            .build();
    }

    private List<SnmpV3User> mapSnmpV3Users() {
        return config.getSnmpV3Users().stream().map(snmpV3User -> {
           return SnmpV3User.newBuilder()
               .setEngineId(snmpV3User.getEngineId())
               .setAuthPassphrase(snmpV3User.getAuthPassPhrase())
               .setAuthProtocol(snmpV3User.getAuthProtocol())
               .setPrivacyPassphrase(snmpV3User.getPrivPassPhrase())
               .setPrivacyProtocol(snmpV3User.getPrivProtocol())
               .build();
        }).collect(Collectors.toList());
    }

    private Log toLog(TrapLogDTO messageLog) {
        final Log log = new Log();
        final Events events = new Events();
        log.setEvents(events);

        for (TrapDTO eachMessage : messageLog.getTrapDTOList()) {
            try {
                final Event event = eventCreator.createEventFrom(
                    eachMessage,
                    messageLog.getIdentity().getSystemId(),
                    messageLog.getIdentity().getLocation(),
                    InetAddressUtils.getInetAddress(messageLog.getTrapAddress()));
                if (!shouldDiscard(event)) {
                    if (event.getSnmp() != null) {
                        trapdInstrumentation.incTrapsReceivedCount(event.getSnmp().getVersion());
                    }
                    events.addEvent(event);
                } else {
                    LOG.debug("Trap discarded due to matching event having logmsg dest == discardtraps");
                    trapdInstrumentation.incDiscardCount();
                }
            } catch (Throwable e) {
                LOG.error("Unexpected error processing trap: {}", eachMessage, e);
                trapdInstrumentation.incErrorCount();
            }
        }
        return log;
    }

    private void sendNewSuspectEvent(String trapInterface, String distPoller, String location) {
        // construct event with 'trapd' as source
        EventBuilder bldr = new EventBuilder(EventConstants.NEW_SUSPECT_INTERFACE_EVENT_UEI, "trapd");
        bldr.setInterface(addr(trapInterface));
        bldr.setHost(LOCALHOST_ADDRESS);
        bldr.setDistPoller(distPoller);
        bldr.setParam(EventConstants.PARM_LOCATION, location);

        // send the event to eventd
        eventForwarder.sendNow(bldr.getEvent());
    }

    private boolean shouldDiscard(Event event) {
        org.opennms.horizon.events.conf.xml.Event econf = eventConfDao.findByEvent(event);
        if (econf != null) {
            final Logmsg logmsg = econf.getLogmsg();
            return logmsg != null && LogDestType.DISCARDTRAPS.equals(logmsg.getDest());
        }
        return false;
    }


    public EventConfDao getEventConfDao() {
        return eventConfDao;
    }

    public void setEventConfDao(EventConfDao eventConfDao) {
        this.eventConfDao = eventConfDao;
    }

    public EventForwarder getEventForwarder() {
        return eventForwarder;
    }

    public void setEventForwarder(EventForwarder eventForwarder) {
        this.eventForwarder = eventForwarder;
    }

    public InterfaceToNodeCache getInterfaceToNodeCache() {
        return interfaceToNodeCache;
    }

    public void setInterfaceToNodeCache(InterfaceToNodeCache interfaceToNodeCache) {
        this.interfaceToNodeCache = interfaceToNodeCache;
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public void setConfigService(ConfigService configService) {
        this.configService = configService;
    }

    public DistPollerDao getDistPollerDao() {
        return distPollerDao;
    }

    public void setDistPollerDao(DistPollerDao distPollerDao) {
        this.distPollerDao = distPollerDao;
    }


    public void setSessionUtils(SessionUtils sessionUtils) {
        this.sessionUtils = sessionUtils;
    }

    public void setEventSubscriptionService(EventSubscriptionService eventSubscriptionService) {
        this.eventSubscriptionService = eventSubscriptionService;
    }

    public void setSnmpHelper(SnmpHelper snmpHelper) {
        this.snmpHelper = snmpHelper;
    }

    public void setTaskSetPublisher(TaskSetPublisher taskSetPublisher) {
        this.taskSetPublisher = taskSetPublisher;
    }

    public TaskSetManager getTaskSetManager() {
        return taskSetManager;
    }

    public void setTaskSetManager(TaskSetManager taskSetManager) {
        this.taskSetManager = taskSetManager;
    }

    public static TrapListenerConfig from(final TrapdConfig config) {
        final TrapListenerConfig result = new TrapListenerConfig();
        result.setSnmpV3Users(config.getSnmpV3Users());
        return result;
    }

    @Override
    public String getName() {
        return "snmp-traps";
    }

    @Override
    public void onEvent(IEvent event) {
        if (event.getUei().equals(EventConstants.CONFIG_UPDATED_UEI) &&
            event.getParm(EventConstants.PARM_CONFIG_NAME).isValid() &&
            event.getParm(EventConstants.PARM_CONFIG_NAME).getValue().getContent().equals(ConfigConstants.SNMP_TRAPS_CONFIG) &&
            !event.getSource().equals(TRAPS_EVENT_SOURCE)) {

            try {
                initializeConfig();
            } catch (IOException e) {
                LOG.error("Exception while initializing Trap config", e);
            }
        }
    }


    @Override
    public void process(Exchange exchange) throws Exception {

        TrapLogDTO messageLog = exchange.getIn().getMandatoryBody(TrapLogDTO.class);
        try (Logging.MDCCloseable mdc = Logging.withPrefixCloseable(LOG4J_CATEGORY)) {
            final Log eventLog = toLog(messageLog);

            eventForwarder.sendNowSync(eventLog);

            // If configured, also send events for new suspects
            if (config.getNewSuspectOnTrap()) {
                eventLog.getEvents().getEventCollection().stream()
                    .filter(e -> !e.hasNodeid())
                    .forEach(e -> {
                        sendNewSuspectEvent(e.getInterface(), e.getDistPoller(), messageLog.getIdentity().getLocation());
                        LOG.debug("Sent newSuspectEvent for interface {}", e.getInterface());
                    });
            }
        }
    }
}
