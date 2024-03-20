package org.opennms.horizon.minion.syslog.listener;

import org.apache.camel.builder.RouteBuilder;

public class MyCamelRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("netty:udp://127.0.0.1:8888")
            .log("Received UDP message: ${body}");

    }
}
