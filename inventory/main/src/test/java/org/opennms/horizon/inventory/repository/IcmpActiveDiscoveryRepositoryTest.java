package org.opennms.horizon.inventory.repository;

import io.grpc.Context;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.inventory.SpringContextTestInitializer;
import org.opennms.horizon.inventory.model.discovery.active.IcmpActiveDiscovery;
import org.opennms.horizon.inventory.repository.discovery.active.IcmpActiveDiscoveryRepository;
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
public class IcmpActiveDiscoveryRepositoryTest {

    private static final String tenantId = "tenant-1";

    @Autowired
    private IcmpActiveDiscoveryRepository repository;

    @Test
    public void testActiveDiscoveryConfigPersistence() {
        Context.current().withValue(GrpcConstants.TENANT_ID_CONTEXT_KEY, tenantId).run(() ->
        {
            var discovery = new IcmpActiveDiscovery();
            discovery.setLocation("MINION");
            discovery.setName("Profile-1");
            discovery.setTenantId(tenantId);
            discovery.setSnmpPorts(Collections.singletonList(1161));
            discovery.setSnmpCommunityStrings(Collections.singletonList("OpenNMS"));
            discovery.setIpAddressEntries(Arrays.asList("127.0.0.1", "127.0.0.2"));
            var persisted = repository.save(discovery);
            Assertions.assertNotNull(persisted);
            Assertions.assertEquals("MINION", discovery.getLocation());
            Assertions.assertEquals("Profile-1", discovery.getName());
            Assertions.assertEquals("OpenNMS", discovery.getSnmpCommunityStrings().get(0));
            Assertions.assertEquals("127.0.0.1", discovery.getIpAddressEntries().get(0));
            Assertions.assertEquals("127.0.0.2", discovery.getIpAddressEntries().get(1));
            Assertions.assertEquals(1161, discovery.getSnmpPorts().get(0));

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
