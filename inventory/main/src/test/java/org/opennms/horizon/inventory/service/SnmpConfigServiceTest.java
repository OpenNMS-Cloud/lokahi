package org.opennms.horizon.inventory.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.inventory.mapper.SnmpConfigMapper;
import org.opennms.horizon.inventory.model.SnmpAgentConfig;
import org.opennms.horizon.inventory.model.SnmpConfig;
import org.opennms.horizon.inventory.repository.SnmpConfigRepository;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.horizon.snmp.api.SnmpConfiguration;

import java.net.InetAddress;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class SnmpConfigServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";
    public static final long TEST_LOCATION = 1313L;
    public static final String TEST_LOCATION_TEXT = "x-location-x";

    private SnmpConfigRepository mockSnmpConfigRepository;
    private SnmpConfigMapper mockSnmpConfigMapper;

    private SnmpConfiguration testSnmpConfigurationProto;
    private SnmpAgentConfig testSnmpConfigurationModel;
    private SnmpConfig testSnmpConfig;  // TODO: the naming of SnmpConfig, SnmpConfiguration and SnmpAgentConfig is confusing
    private InetAddress testInetAddress;

    private SnmpConfigService target;

    @BeforeEach
    public void setUp() {
        mockSnmpConfigRepository = Mockito.mock(SnmpConfigRepository.class);
        mockSnmpConfigMapper = Mockito.mock(SnmpConfigMapper.class);

        testSnmpConfigurationProto =
            SnmpConfiguration.newBuilder()
                .build();

        testSnmpConfigurationModel = new SnmpAgentConfig();
        
        testSnmpConfig = new SnmpConfig();
        testSnmpConfig.setSnmpAgentConfig(testSnmpConfigurationModel);

        testInetAddress = InetAddressUtils.getInetAddress("11.11.11.11");

        target = new SnmpConfigService(mockSnmpConfigRepository, mockSnmpConfigMapper);
    }

    @Test
    void testSaveSnmpConfig() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(
            mockSnmpConfigRepository.findByTenantIdAndLocationIdAndIpAddress(TEST_TENANT_ID, TEST_LOCATION, testInetAddress)
        ).thenReturn(Optional.empty());
        Mockito.when(mockSnmpConfigMapper.mapProtoToModel(testSnmpConfigurationProto)).thenReturn(testSnmpConfigurationModel);

        //
        // Execute
        //
        target.saveOrUpdateSnmpConfig(TEST_TENANT_ID, TEST_LOCATION, "11.11.11.11", testSnmpConfigurationProto);

        //
        // Verify the Results
        //
        Mockito.verify(mockSnmpConfigRepository)
            .save(Mockito.argThat(
                argument ->
                    (
                        ( argument.getSnmpAgentConfig() == testSnmpConfigurationModel ) &&
                        ( Objects.equals(TEST_TENANT_ID, argument.getTenantId()) ) &&
                        ( Objects.equals(TEST_LOCATION, argument.getLocationId()) ) &&
                        ( Objects.equals(testInetAddress, argument.getIpAddress()) )
                    )
            ));
    }

    @Test
    void testUpdateSnmpConfig() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(
            mockSnmpConfigRepository.findByTenantIdAndLocationIdAndIpAddress(TEST_TENANT_ID, TEST_LOCATION, testInetAddress)
        ).thenReturn(Optional.of(testSnmpConfig));
        Mockito.when(mockSnmpConfigMapper.mapProtoToModel(testSnmpConfigurationProto)).thenReturn(testSnmpConfigurationModel);

        //
        // Execute
        //
        target.saveOrUpdateSnmpConfig(TEST_TENANT_ID, TEST_LOCATION, "11.11.11.11", testSnmpConfigurationProto);

        //
        // Verify the Results
        //
        Mockito.verify(mockSnmpConfigRepository).save(testSnmpConfig);
        Assertions.assertSame(testSnmpConfigurationModel, testSnmpConfig.getSnmpAgentConfig());
    }

    @Test
    void testGetSnmpConfig() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(
            mockSnmpConfigRepository.findByTenantIdAndLocationIdAndIpAddress(TEST_TENANT_ID, TEST_LOCATION, testInetAddress)
        ).thenReturn(Optional.of(testSnmpConfig));
        Mockito.when(mockSnmpConfigMapper.mapModelToProto(testSnmpConfigurationModel)).thenReturn(testSnmpConfigurationProto);

        //
        // Execute
        //
        var result = target.getSnmpConfig(TEST_TENANT_ID, TEST_LOCATION, testInetAddress);

        //
        // Verify the Results
        //
        assertSame(testSnmpConfigurationProto, result.orElse(null));
    }

    @Test
    void testGetSnmpConfigNotFound() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(
            mockSnmpConfigRepository.findByTenantIdAndLocationIdAndIpAddress(TEST_TENANT_ID, TEST_LOCATION, testInetAddress)
        ).thenReturn(Optional.empty());

        //
        // Execute
        //
        var result = target.getSnmpConfig(TEST_TENANT_ID, TEST_LOCATION, testInetAddress);

        //
        // Verify the Results
        //
        assertTrue(result.isEmpty());
    }
}
