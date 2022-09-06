package org.opennms.horizon.minion.weblisten;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import org.opennms.horizon.minion.plugin.api.Listener;
import org.opennms.horizon.minion.plugin.api.ListenerFactory;
import org.opennms.horizon.minion.plugin.api.ServiceMonitorResponse;
import org.opennms.weblisten.contract.WebListenRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * PROTOTYPE
 */
public class WebListenerFactory implements ListenerFactory {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(WebListenerFactory.class);

    private Logger log = DEFAULT_LOGGER;

    @Override
    public Listener create(Consumer<ServiceMonitorResponse> resultProcessor, Any config) {
        if (! config.is(WebListenRequest.class)) {
            throw new IllegalArgumentException("config must be a WebListenRequest; type-url=" + config.getTypeUrl());
        }

        try {
            WebListenRequest request = config.unpack(WebListenRequest.class);
            WebListener listener = new WebListener(resultProcessor, request);

            return listener;
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException("error parsing request", e);
        }
    }
}
