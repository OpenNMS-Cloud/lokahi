package org.opennms.horizon.notifications.api.email;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.opennms.horizon.alerts.proto.Alert;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Properties;

@Component
public class Velocity {
    @Value("${spring.mail.template}")
    private String alertTemplate;

    private final VelocityEngine velocityEngine;

    public Velocity() {
        Properties props = new Properties();
        props.setProperty("resource.loaders", "class");
        props.setProperty("resource.loader.class.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

        velocityEngine = new VelocityEngine(props);
        velocityEngine.init();
    }

    public String populateTemplate(String recipient, Alert alert) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("currentYear", LocalDate.now().getYear());
        ctx.put("recipient", recipient);
        ctx.put("alert", alert);

        StringWriter writer = new StringWriter();
        velocityEngine.mergeTemplate(alertTemplate, StandardCharsets.UTF_8.name(), ctx, writer);

        return writer.toString();
    }
}
