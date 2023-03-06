package org.opennms.horizon.inventory.repository;

import io.grpc.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.ActiveDiscoveryConfig;
import org.opennms.horizon.shared.constants.GrpcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Arrays;
import java.util.Collections;


@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(initializers = {SpringContextTestInitializer.class})
public class ActiveDiscoveryConfigRepositoryTest {

    private static final String tenantId = "tenant-1";

    @Autowired
    private ActiveDiscoveryRepository repository;

    @Test
    public void testActiveDiscoveryConfigPersistence() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(() ->
        {
            var activeDiscoveryConfig = new ActiveDiscoveryConfig();
            activeDiscoveryConfig.setLocation("MINION");
            activeDiscoveryConfig.setName("Profile-1");
            activeDiscoveryConfig.setTenantId(tenantId);
            activeDiscoveryConfig.setSnmpPorts(Collections.singletonList(1161));
            activeDiscoveryConfig.setSnmpCommunityStrings(Collections.singletonList("OpenNMS"));
            activeDiscoveryConfig.setIpAddressEntries(Arrays.asList("127.0.0.1", "127.0.0.2"));
            var persisted = repository.save(activeDiscoveryConfig);
            Assertions.assertNotNull(persisted);
            Assertions.assertEquals("MINION", activeDiscoveryConfig.getLocation());
            Assertions.assertEquals("Profile-1", activeDiscoveryConfig.getName());
            Assertions.assertEquals("OpenNMS", activeDiscoveryConfig.getSnmpCommunityStrings().get(0));
            Assertions.assertEquals("127.0.0.1", activeDiscoveryConfig.getIpAddressEntries().get(0));
            Assertions.assertEquals("127.0.0.2", activeDiscoveryConfig.getIpAddressEntries().get(1));
            Assertions.assertEquals(1161, activeDiscoveryConfig.getSnmpPorts().get(0));

            var list = repository.findByLocationAndTenantId("MINION", tenantId);
            Assertions.assertFalse(list.isEmpty());

            var optional = repository.findByLocationAndName("MINION", "Profile-1");
            Assertions.assertTrue(optional.isPresent());
        });

    }

    @AfterEach
    public void destroy() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(() ->
            repository.deleteAll());
    }

}
