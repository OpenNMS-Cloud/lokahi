package org.opennms.horizon.minion.grpc.channel;

import io.grpc.ChannelCredentials;
import io.grpc.ManagedChannelBuilder;
import io.grpc.TlsChannelCredentials;
import org.junit.jupiter.api.Test;

class OkHttpChannelBuilderFactoryTest {

    @Test
    public void verifyPlainText() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.create("host", 443, null, null);
    }

    @Test
    public void verifyTls() {
        ChannelCredentials credentials = TlsChannelCredentials.newBuilder().build();
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.create("host", 443, null, credentials);
    }

    @Test
    public void verifyMessageSize() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.setMaxInboundMessageSize(1000);
        ManagedChannelBuilder<?> channelBuilder = channelBuilderFactory.create("host", 443, "", null);

        // no way to verify it
    }

    @Test
    public void verifyOverrideAuthority() {
        OkHttpChannelBuilderFactory channelBuilderFactory = new OkHttpChannelBuilderFactory();
        channelBuilderFactory.setMaxInboundMessageSize(1000);
        ManagedChannelBuilder<?> channelBuilder = channelBuilderFactory.create("host", 443, "desired", null);

        // no way to verify it
    }

}
