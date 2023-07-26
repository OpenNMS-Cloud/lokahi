package org.opennms.horizon.inventory.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opennms.horizon.shared.utils.InetAddressUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;

class IpAddressMapperTest {
    public final String IP = "192.168.0.1";

    private IpAddressMapper mapper = new IpAddressMapperImpl();

    @Test
    void testStringToInet() throws UnknownHostException {
        Assertions.assertEquals(IP, mapper.map(IP).getHostAddress());
        Assertions.assertNull(mapper.map(""));
    }

    @Test
    void testInetToString() {
        Assertions.assertEquals(IP, mapper.map(InetAddressUtils.getInetAddress(IP)));
        Assertions.assertNull(mapper.map((InetAddress) null));
    }
}
