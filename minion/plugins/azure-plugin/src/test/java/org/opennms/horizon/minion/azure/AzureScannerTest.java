package org.opennms.horizon.minion.azure;

import com.google.protobuf.Any;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Message;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.opennms.azure.contract.AzureScanRequest;
import org.opennms.horizon.azure.api.AzureScanItem;
import org.opennms.horizon.azure.api.AzureScanNetworkInterfaceItem;
import org.opennms.horizon.azure.api.AzureScanResponse;
import org.opennms.horizon.minion.plugin.api.ScanResultsResponse;
import org.opennms.horizon.shared.azure.http.AzureHttpClient;
import org.opennms.horizon.shared.azure.http.dto.instanceview.AzureInstanceView;
import org.opennms.horizon.shared.azure.http.dto.login.AzureOAuthToken;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterface;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.AzureNetworkInterfaces;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfiguration;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.IpConfigurationProps;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.NetworkInterfaceProps;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.PublicIPAddress;
import org.opennms.horizon.shared.azure.http.dto.networkinterface.VirtualMachine;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIPAddress;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.AzurePublicIpAddresses;
import org.opennms.horizon.shared.azure.http.dto.publicipaddresses.PublicIpAddressProps;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureResourceGroups;
import org.opennms.horizon.shared.azure.http.dto.resourcegroup.AzureValue;
import org.opennms.horizon.shared.azure.http.dto.resources.AzureResources;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class AzureScannerTest {
    private static final String TEST_CLIENT_ID = "test-client-id";
    private static final String TEST_CLIENT_SECRET = "test-client-secret";
    private static final String TEST_SUBSCRIPTION_ID = "test-subscription-id";
    private static final String TEST_DIRECTORY_ID = "test-directory-id";
    private static final int TEST_ACTIVE_DISCOVERY_ID = 1;
    private static final int TEST_RETRIES = 2;
    private static final int TEST_TIMEOUT_MS = 30000;
    private static final long FUTURE_TIMEOUT = 5000L;
    private static final String TEST_RESOURCE_GROUP = "test-resource-group";
    private static final String TEST_RESOURCE_NAME = "test-resource-name";
    private static final String TEST_RESOURCE_ID = "test-resource-id";
    private static final String TEST_IP_CONF_ID = "test-ip-conf-id";
    private static final String TEST_PRIVATE_IP_ADDRESS = "127.0.1.1";
    private static final String TEST_PUBLIC_IP_ID = "test-public-ip-id";
    private static final String TEST_PUBLIC_IP_ADDRESS = "10.20.30.40";
    private static final String TEST_PUBLIC_IP_ADDRESS_NAME = "test-public-ip-address-name";
    private static final String TEST_NETWORK_INTERFACE_ID = "test-network-interface-id";
    private static final String TEST_NETWORK_INTERFACE_NAME = "test-network-interface-name";
    private static final String TEST_LOCATION = "test-location";
    private static final String TEST_OS = "test-os";
    private static final String TEST_OS_VERSION = "test-version";
    private static final String TEST_IP_CONF_NAME = "ip-config-name";

    private AzureHttpClient mockAzureHttpClient;
    private AzureScanner scanner;
    private AzureOAuthToken token;

    @Before
    public void before() throws Exception {
        mockAzureHttpClient = Mockito.mock(AzureHttpClient.class);
        scanner = new AzureScanner(mockAzureHttpClient);
        token = getAzureOAuthToken();

        when(mockAzureHttpClient.login(TEST_DIRECTORY_ID, TEST_CLIENT_ID,
            TEST_CLIENT_SECRET, TEST_TIMEOUT_MS, TEST_RETRIES)).thenReturn(token);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testScan() throws Exception {
        AzureResourceGroups azureResourceGroups = getAzureResourceGroups();
        when(mockAzureHttpClient.getResourceGroups(token, TEST_SUBSCRIPTION_ID, TEST_TIMEOUT_MS, TEST_RETRIES)).thenReturn(azureResourceGroups);

        AzureResources azureResources = getAzureResources("Microsoft.Compute/virtualMachines");
        when(mockAzureHttpClient.getResources(token, TEST_SUBSCRIPTION_ID, TEST_RESOURCE_GROUP, TEST_TIMEOUT_MS, TEST_RETRIES)).thenReturn(azureResources);

        AzureNetworkInterfaces azureNetworkInterfaces = getAzureNetworkInterfaces();
        when(mockAzureHttpClient.getNetworkInterfaces(token, TEST_SUBSCRIPTION_ID, TEST_RESOURCE_GROUP, TEST_TIMEOUT_MS, TEST_RETRIES)).thenReturn(azureNetworkInterfaces);

        AzurePublicIpAddresses azurePublicIpAddresses = getAzurePublicIpAddresses();
        when(mockAzureHttpClient.getPublicIpAddresses(token, TEST_SUBSCRIPTION_ID, TEST_RESOURCE_GROUP, TEST_TIMEOUT_MS, TEST_RETRIES)).thenReturn(azurePublicIpAddresses);

        AzureInstanceView instanceView = new AzureInstanceView();
        instanceView.setOsName(TEST_OS);
        instanceView.setOsVersion(TEST_OS_VERSION);
        when(mockAzureHttpClient.getInstanceView(token, TEST_SUBSCRIPTION_ID, TEST_RESOURCE_GROUP,
            azureResources.getValue().get(0).getName(), TEST_TIMEOUT_MS, TEST_RETRIES))
            .thenReturn(instanceView);

        AzureScanRequest request = AzureScanRequest.newBuilder()
            .setClientId(TEST_CLIENT_ID).setClientSecret(TEST_CLIENT_SECRET)
            .setSubscriptionId(TEST_SUBSCRIPTION_ID).setDirectoryId(TEST_DIRECTORY_ID)
            .setActiveDiscoveryId(TEST_ACTIVE_DISCOVERY_ID).setRetries(TEST_RETRIES)
            .setTimeoutMs(TEST_TIMEOUT_MS).build();

        CompletableFuture<ScanResultsResponse> future = scanner.scan(Any.pack(request));
        ScanResultsResponse response = future.get(FUTURE_TIMEOUT, TimeUnit.MILLISECONDS);

        Message results = response.getResults();

        Descriptors.Descriptor descriptorForType = results.getDescriptorForType();
        Descriptors.FieldDescriptor resultsField = descriptorForType.findFieldByNumber(AzureScanResponse.RESULTS_FIELD_NUMBER);
        List<AzureScanItem> resultsList = (List<AzureScanItem>) results.getField(resultsField);

        assertEquals(1, resultsList.size());

        AzureScanItem scanItem = resultsList.get(0);
        assertEquals(TEST_RESOURCE_ID, scanItem.getId());
        assertEquals(TEST_RESOURCE_NAME, scanItem.getName());
        assertEquals(TEST_RESOURCE_GROUP, scanItem.getResourceGroup());
        assertEquals(TEST_ACTIVE_DISCOVERY_ID, scanItem.getActiveDiscoveryId());
        assertEquals(TEST_LOCATION, scanItem.getLocation());
        assertEquals(TEST_OS, scanItem.getOsName());
        assertEquals(TEST_OS_VERSION, scanItem.getOsVersion());

        assertEquals(1, scanItem.getNetworkInterfaceItemsCount());
        List<AzureScanNetworkInterfaceItem> interfaceList = scanItem.getNetworkInterfaceItemsList();
        AzureScanNetworkInterfaceItem interface1 = interfaceList.get(0);
        assertEquals(TEST_IP_CONF_ID, interface1.getId());
        assertEquals(TEST_NETWORK_INTERFACE_NAME, interface1.getInterfaceName());
        assertEquals(TEST_IP_CONF_NAME, interface1.getName());
        assertEquals(TEST_PRIVATE_IP_ADDRESS, interface1.getIpAddress());

        AzureScanNetworkInterfaceItem interface2 = interface1.getPublicIpAddress();
        assertEquals(TEST_PUBLIC_IP_ID, interface2.getId());
        assertEquals(TEST_PUBLIC_IP_ADDRESS_NAME, interface2.getName());
        assertEquals(TEST_PUBLIC_IP_ADDRESS, interface2.getIpAddress());
    }

    private AzurePublicIpAddresses getAzurePublicIpAddresses() {
        AzurePublicIpAddresses azurePublicIpAddresses = new AzurePublicIpAddresses();
        AzurePublicIPAddress azurePublicIPAddress = new AzurePublicIPAddress();
        azurePublicIPAddress.setId(TEST_PUBLIC_IP_ID);
        azurePublicIPAddress.setName(TEST_PUBLIC_IP_ADDRESS_NAME);
        PublicIpAddressProps props = new PublicIpAddressProps();
        props.setIpAddress(TEST_PUBLIC_IP_ADDRESS);
        azurePublicIPAddress.setProperties(props);
        azurePublicIpAddresses.setValue(List.of(azurePublicIPAddress, new AzurePublicIPAddress()));
        return azurePublicIpAddresses;
    }

    private AzureResources getAzureResources(String resourceType) {
        AzureResources azureResources = new AzureResources();
        org.opennms.horizon.shared.azure.http.dto.resources.AzureValue azureValue
            = new org.opennms.horizon.shared.azure.http.dto.resources.AzureValue();
        azureValue.setId(TEST_RESOURCE_ID);
        azureValue.setName(TEST_RESOURCE_NAME);
        azureValue.setType(resourceType);
        azureValue.setLocation(TEST_LOCATION);
        azureResources.setValue(Collections.singletonList(azureValue));
        return azureResources;
    }

    private AzureNetworkInterfaces getAzureNetworkInterfaces() {
        AzureNetworkInterfaces azureNetworkInterfaces = new AzureNetworkInterfaces();
        AzureNetworkInterface azureNetworkInterface = new AzureNetworkInterface();
        azureNetworkInterface.setId(TEST_NETWORK_INTERFACE_ID);
        azureNetworkInterface.setName(TEST_NETWORK_INTERFACE_NAME);
        azureNetworkInterface.setLocation(TEST_LOCATION);
        NetworkInterfaceProps props = new NetworkInterfaceProps();
        IpConfiguration ipConfiguration = new IpConfiguration();
        ipConfiguration.setId(TEST_IP_CONF_ID);
        ipConfiguration.setName(TEST_IP_CONF_NAME);
        IpConfigurationProps ipConfProps = new IpConfigurationProps();
        ipConfProps.setPrivateIPAddress(TEST_PRIVATE_IP_ADDRESS);
        PublicIPAddress publicIPAddress = new PublicIPAddress();
        publicIPAddress.setId(TEST_PUBLIC_IP_ID);
        ipConfProps.setPublicIPAddress(publicIPAddress);
        ipConfiguration.setProperties(ipConfProps);
        props.setIpConfigurations(List.of(ipConfiguration, new IpConfiguration()));
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.setId(TEST_RESOURCE_ID);
        props.setVirtualMachine(virtualMachine);
        azureNetworkInterface.setProperties(props);
        azureNetworkInterfaces.setValue(List.of(azureNetworkInterface, new AzureNetworkInterface()));
        return azureNetworkInterfaces;
    }

    private AzureResourceGroups getAzureResourceGroups() {
        AzureResourceGroups azureResourceGroups = new AzureResourceGroups();
        AzureValue azureValue = new AzureValue();
        azureValue.setName(TEST_RESOURCE_GROUP);
        azureResourceGroups.setValue(List.of(azureValue, new AzureValue()));
        return azureResourceGroups;
    }

    private AzureOAuthToken getAzureOAuthToken() {
        AzureOAuthToken token = new AzureOAuthToken();
        token.setTokenType("Bearer");
        token.setExpiresIn("3599");
        token.setExtExpiresIn("3599");
        token.setExpiresOn("1673347297");
        token.setNotBefore("1673347297");
        token.setResource("http://localhost:12345");
        token.setAccessToken("access-token");
        return token;
    }
}
