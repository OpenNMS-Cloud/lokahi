package org.opennms.horizon.inventory.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter;

import java.io.IOException;

@Configuration
@EnableJpaRepositories(basePackages = "org.opennms.horizon.inventory.repository")
public class PersistenceJPAConfig {
    @Bean
    ProtobufHttpMessageConverter protobufHttpMessageConverter() {
        return new ProtobufHttpMessageConverter();
    }
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return o -> o.serializerByType(Message.class, new JsonSerializer<Message>() {
            @Override
            public void serialize(Message value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                gen.writeRawValue(JsonFormat.printer().print(value));
            }
        });
    }
}
