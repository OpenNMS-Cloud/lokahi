package org.test;

import org.apache.camel.builder.RouteBuilder;

public class TestRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("netty:udp://0.0.0.0:7070")
            //.log("Received TCP message: ${body}");
          .process(exchange -> {
                // Log message body and headers
                System.out.println("Message body: " + exchange.getIn().getBody());
                System.out.println("Message headers: " + exchange.getIn().getHeaders());
                // Your processing logic here
            })
            .to("mock:result");

    }
}
