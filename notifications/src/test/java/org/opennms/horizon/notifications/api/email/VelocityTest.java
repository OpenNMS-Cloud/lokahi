package org.opennms.horizon.notifications.api.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.opennms.horizon.alerts.proto.Alert;
import org.opennms.horizon.notifications.api.LokahiUrlUtil;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class VelocityTest {
    @Mock
    private LokahiUrlUtil lokahiUrlUtil;
    @InjectMocks
    Velocity velocity;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(velocity, "alertTemplate", "test.txt.vm");
    }

    @Test
    void populateTemplate() {
        Alert alert = Alert.newBuilder()
            .setDescription("Server down")
            .setLogMessage("Some interesting details")
            .build();

        List<String> completedTemplate = velocity.populateTemplate("bossman@company", alert).lines().toList();
        assertEquals("To: bossman@company", completedTemplate.get(0));
        assertEquals("Description: " + alert.getDescription(), completedTemplate.get(1));
        assertEquals("Message: " + alert.getLogMessage(), completedTemplate.get(2));
    }

}
