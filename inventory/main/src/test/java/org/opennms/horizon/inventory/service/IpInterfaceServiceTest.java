package org.opennms.horizon.inventory.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.inventory.dto.IpInterfaceDTO;
import org.opennms.horizon.inventory.exception.InventoryRuntimeException;
import org.opennms.horizon.inventory.mapper.IpInterfaceMapper;
import org.opennms.horizon.inventory.model.AzureInterface;
import org.opennms.horizon.inventory.model.IpInterface;
import org.opennms.horizon.inventory.model.Node;
import org.opennms.horizon.inventory.model.SnmpInterface;
import org.opennms.horizon.inventory.repository.IpInterfaceRepository;
import org.opennms.horizon.shared.utils.IPAddress;
import org.opennms.horizon.shared.utils.InetAddressUtils;
import org.opennms.node.scan.contract.IpInterfaceResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IpInterfaceServiceTest {

    public static final String TEST_TENANT_ID = "x-tenant-id-x";
    public static final long TEST_LOCATION_ID = 1313L;
    public static final String TEST_LOCATION_ID_TEXT = String.valueOf(TEST_LOCATION_ID);

    private IpInterfaceRepository mockIpInterfaceRepository;
    private IpInterfaceMapper mockIpInterfaceMapper;

    private IpInterface testIpInterface;
    private IpInterfaceDTO testIpInterfaceDTO;

    private IpInterfaceService target;

    @BeforeEach
    public void setUp() {
        mockIpInterfaceRepository = Mockito.mock(IpInterfaceRepository.class);
        mockIpInterfaceMapper = Mockito.mock(IpInterfaceMapper.class);

        testIpInterface = new IpInterface();
        testIpInterfaceDTO =
            IpInterfaceDTO.newBuilder()
                .setId(1313)
                .build();

        target = new IpInterfaceService(mockIpInterfaceRepository, mockIpInterfaceMapper);
    }

    @Test
    void testGetByTenantId() {
        //
        // Setup Test Data and Interactions
        //
        var testIpInterface1 = new IpInterface();
        var testIpInterface2 = new IpInterface();
        var testIpInterfaceList = List.of(testIpInterface1, testIpInterface2);
        var testIpInterfaceDTO1 = IpInterfaceDTO.newBuilder().setId(1313).build();
        var testIpInterfaceDTO2 = IpInterfaceDTO.newBuilder().setId(1717).build();

        Mockito.when(mockIpInterfaceRepository.findByTenantId(TEST_TENANT_ID)).thenReturn(testIpInterfaceList);
        Mockito.when(mockIpInterfaceMapper.modelToDTO(testIpInterface1)).thenReturn(testIpInterfaceDTO1);
        Mockito.when(mockIpInterfaceMapper.modelToDTO(testIpInterface2)).thenReturn(testIpInterfaceDTO2);

        //
        // Execute
        //
        var result = target.findByTenantId(TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertEquals(2, result.size());
        assertSame(testIpInterfaceDTO1, result.get(0));
        assertSame(testIpInterfaceDTO2, result.get(1));
    }

    @Test
    void testGetByIdAndTenantId() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockIpInterfaceRepository.findByIdAndTenantId(1313, TEST_TENANT_ID)).thenReturn(Optional.of(testIpInterface));
        Mockito.when(mockIpInterfaceMapper.modelToDTO(testIpInterface)).thenReturn(testIpInterfaceDTO);

        //
        // Execute
        //
        var result = target.getByIdAndTenantId(1313, TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertSame(testIpInterfaceDTO, result.get());
    }

    @Test
    void testCreateFromAzureScanResult() {
        //
        // Setup Test Data and Interactions
        //
        var testNode = new Node();
        var azureInterface = new AzureInterface();
        azureInterface.setId(1L);
        var testAzureScanNetworkInterfaceItem =
            AzureScanNetworkInterfaceItem.newBuilder()
                .setIpAddress("11.11.11.11")
                .build();

        //
        // Execute
        //
        target.createFromAzureScanResult(TEST_TENANT_ID, testNode, azureInterface, testAzureScanNetworkInterfaceItem);

        //
        // Verify the Results
        //
        Mockito.verify(mockIpInterfaceRepository).save(
            Mockito.argThat(
                argument -> (
                    ( ! argument.getSnmpPrimary() ) &&
                    ( argument.getNode() == testNode ) &&
                    ( Objects.equals("11.11.11.11", argument.getIpAddress().getHostName()) )
                )
            )
        );
    }

    @Test
    void testFindByIpAddressAndLocationAndTenantId() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockIpInterfaceRepository.findByIpAddressAndLocationIdAndTenantId(new IPAddress("11.11.11.11").toInetAddress(), TEST_LOCATION_ID, TEST_TENANT_ID)).thenReturn(List.of(testIpInterface));
        Mockito.when(mockIpInterfaceMapper.modelToDTO(testIpInterface)).thenReturn(testIpInterfaceDTO);

        //
        // Execute
        //
        var result = target.findByIpAddressAndLocationIdAndTenantId("11.11.11.11", TEST_LOCATION_ID_TEXT, TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertSame(testIpInterfaceDTO, result.orElse(null));
    }

    @Test
    void testFindByIpAddressAndLocationAndTenantIdNotFound() {
        //
        // Setup Test Data and Interactions
        //
        Mockito.when(mockIpInterfaceRepository.findByIpAddressAndLocationIdAndTenantId(new IPAddress("11.11.11.11").toInetAddress(), TEST_LOCATION_ID, TEST_TENANT_ID)).thenReturn(new ArrayList<>());

        //
        // Execute
        //
        var result = target.findByIpAddressAndLocationIdAndTenantId("11.11.11.11", TEST_LOCATION_ID_TEXT, TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByIpAddressAndLocationAndTenantIdMoreThanOne() {
        //
        // Setup Test Data and Interactions
        //
        IpInterface ipAddress1  = new IpInterface();
        ipAddress1.setId(1L);
        IpInterface ipAddress2  = new IpInterface();
        ipAddress2.setId(2L);
        IpInterface ipAddress3  = new IpInterface();
        ipAddress3.setId(3L);
        ipAddress3.setSnmpPrimary(true);
        var testIpInterfaceDTO1 = IpInterfaceDTO.newBuilder().setId(1L).build();
        var testIpInterfaceDTO3 = IpInterfaceDTO.newBuilder().setId(1L).build();
        Mockito.when(mockIpInterfaceRepository.findByIpAddressAndLocationIdAndTenantId(new IPAddress("11.11.11.11").toInetAddress(), TEST_LOCATION_ID, TEST_TENANT_ID))
            .thenReturn(List.of(ipAddress1, ipAddress2));
        Mockito.when(mockIpInterfaceRepository.findByIpAddressAndLocationIdAndTenantId(new IPAddress("11.11.11.12").toInetAddress(), TEST_LOCATION_ID, TEST_TENANT_ID))
            .thenReturn(List.of(ipAddress1, ipAddress2, ipAddress3));
        Mockito.when(mockIpInterfaceMapper.modelToDTO(ipAddress1)).thenReturn(testIpInterfaceDTO1);
        Mockito.when(mockIpInterfaceMapper.modelToDTO(ipAddress3)).thenReturn(testIpInterfaceDTO3);

        //
        // Execute
        //
        var expectFirstAddress = target.findByIpAddressAndLocationIdAndTenantId("11.11.11.11", TEST_LOCATION_ID_TEXT, TEST_TENANT_ID);
        var expectSnmpAddress = target.findByIpAddressAndLocationIdAndTenantId("11.11.11.12", TEST_LOCATION_ID_TEXT, TEST_TENANT_ID);

        //
        // Verify the Results
        //
        assertTrue(expectFirstAddress.isPresent());
        assertEquals(testIpInterfaceDTO1.getId(), expectFirstAddress.get().getId());
        assertTrue(expectSnmpAddress.isPresent());
        assertEquals(testIpInterfaceDTO3.getId(), expectSnmpAddress.get().getId());
    }

    @Test
    void testCreateOrUpdateFromScanResult() {
        //
        // Setup Test Data and Interactions
        //
        var testNode = new Node();
        testNode.setId(1313);
        var testIpInterfaceResult =
            IpInterfaceResult.newBuilder()
                .setIfIndex(1)
                .setIpHostName("x-hostname-x")
                .setNetmask("x-netmask-x")
                .setIpAddress("11.11.11.11")
                .build();
        var testSnmpInterface1 = new SnmpInterface();
        var testSnmpInterface2 = new SnmpInterface();
        Map<Integer, SnmpInterface> snmpInterfaceMap =
            Map.of(
                1, testSnmpInterface1,
                2, testSnmpInterface2
            );
        Mockito.when(mockIpInterfaceRepository.findByNodeIdAndTenantIdAndIpAddress(1313, TEST_TENANT_ID, new IPAddress("11.11.11.11").toInetAddress())).thenReturn(Optional.of(testIpInterface));

        //
        // Execute
        //
        target.createOrUpdateFromScanResult(TEST_TENANT_ID, testNode, testIpInterfaceResult, snmpInterfaceMap);

        //
        // Verify the Results
        //
        Mockito.verify(mockIpInterfaceRepository).save(Mockito.argThat(
            argument ->
                (
                    ( Objects.equals("x-hostname-x", argument.getHostname()) ) &&
                    ( Objects.equals("x-netmask-x", argument.getNetmask()) )
                )
        ));
    }

    @Test
    void testCreateOrUpdateFromScanResultNotFound() {
        //
        // Setup Test Data and Interactions
        //
        var testNode = new Node();
        testNode.setId(1313);
        var testIpInterfaceResult =
            IpInterfaceResult.newBuilder()
                .setIfIndex(1)
                .setIpHostName("x-hostname-x")
                .setNetmask("x-netmask-x")
                .setIpAddress("11.11.11.11")
                .build();
        var testSnmpInterface1 = new SnmpInterface();
        var testSnmpInterface2 = new SnmpInterface();
        Map<Integer, SnmpInterface> snmpInterfaceMap =
            Map.of(
                1, testSnmpInterface1,
                2, testSnmpInterface2
            );
        Mockito.when(mockIpInterfaceRepository.findByNodeIdAndTenantIdAndIpAddress(1313, TEST_TENANT_ID, new IPAddress("11.11.11.11").toInetAddress())).thenReturn(Optional.empty());
        Mockito.when(mockIpInterfaceMapper.fromScanResult(testIpInterfaceResult)).thenReturn(testIpInterface);

        //
        // Execute
        //
        target.createOrUpdateFromScanResult(TEST_TENANT_ID, testNode, testIpInterfaceResult, snmpInterfaceMap);

        //
        // Verify the Results
        //
        Mockito.verify(mockIpInterfaceRepository).save(Mockito.same(testIpInterface));
        assertSame(testNode, testIpInterface.getNode());
        assertEquals(TEST_TENANT_ID, testIpInterface.getTenantId());
        assertFalse(testIpInterface.getSnmpPrimary());
        assertEquals("x-hostname-x", testIpInterface.getHostname());
        assertEquals(1, testIpInterface.getIfIndex());
        assertSame(testSnmpInterface1, testIpInterface.getSnmpInterface());
    }
}
