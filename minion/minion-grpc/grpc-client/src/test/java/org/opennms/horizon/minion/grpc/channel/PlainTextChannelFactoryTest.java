package org.opennms.horizon.minion.grpc.channel;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.grpc.ChannelCredentials;
import io.grpc.ManagedChannelBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlainTextChannelFactoryTest {

    @Mock
    protected ChannelBuilderFactory channelBuilderFactory;
    @Mock
    protected ManagedChannelBuilder managedChannelBuilder;

    @Test
    public void verifyPlainTextChannelBuilder() {
        prepareMocks("foo", 8990, null, null);

        new PlainTextChannelFactory(channelBuilderFactory)
            .create("foo", 8990, null);

        verifyMock("foo", 8990, null, null);
    }

    @Test
    public void verifyPlainTextChannelBuilderWithCustomAuthority() {
        prepareMocks("faz", 8990, "bar", null);

        new PlainTextChannelFactory(channelBuilderFactory)
            .create("faz", 8990, "bar");

        verifyMock("faz", 8990, "bar", null);
    }

    protected void prepareMocks(String host, int port, String authority, ChannelCredentials credentials) {
        when(channelBuilderFactory.create(host, port, authority, credentials))
            .thenReturn(managedChannelBuilder);

        when(managedChannelBuilder.usePlaintext()).thenReturn(managedChannelBuilder);
    }

    protected void verifyMock(String host, int port, String authority, ChannelCredentials credentials) {
        verify(channelBuilderFactory).create(host, port, authority, credentials);
    }
}
