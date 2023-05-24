package org.opennms.horizon.minion.grpc;

import io.grpc.ManagedChannel;
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.grpc.netty.shaded.io.netty.handler.ssl.ClientAuth;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslProvider;
import io.grpc.stub.StreamObserver;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc;
import org.opennms.cloud.grpc.minion.CloudServiceGrpc.CloudServiceStub;
import org.opennms.cloud.grpc.minion.CloudToMinionMessage;
import org.opennms.cloud.grpc.minion.Identity;
import org.opennms.cloud.grpc.minion.RpcRequestProto;
import org.opennms.cloud.grpc.minion.RpcResponseProto;
import org.opennms.horizon.shared.ipc.sink.api.AggregationPolicy;

public class PcksTest {

    public static void main(String[] args) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(new FileInputStream("/home/splatch/projects/opennms/sources/horizon-stream/tools/SSL/keystore.p12"),
            "passw0rd1".toCharArray());

        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            System.out.println(aliases.nextElement());
        }

        NettyChannelBuilder channelBuilder = NettyChannelBuilder.forAddress("minion.onmshs.local", 1443)
            .keepAliveWithoutCalls(true);

        ManagedChannel channel = channelBuilder
            .negotiationType(NegotiationType.TLS)
            .sslContext(buildSslContext().build())
            .build();

        CloudServiceStub asyncStub = CloudServiceGrpc.newStub(channel);
        Identity identity = Identity.newBuilder()
            //.setLocation("Ottawa")
            .setSystemId("Inplace")
            .build();
        asyncStub.cloudToMinionMessages(
            identity,
            new StreamObserver<CloudToMinionMessage>() {
                @Override
                public void onNext(CloudToMinionMessage value) {
                    System.out.println(value);
                }

                @Override
                public void onError(Throwable t) {
                    t.printStackTrace();
                }

                @Override
                public void onCompleted() {
                    System.out.println("Done");
                }
            }
        );

        System.in.read();
    }

    private static SslContextBuilder buildSslContext() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("pkcs12");
        keyStore.load(new FileInputStream("/home/splatch/projects/opennms/sources/horizon-stream/tools/SSL/keystore.p12"),
            "passw0rd1".toCharArray());

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "passw0rd".toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        X509TrustManager trustManager = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        ApplicationProtocolConfig apn = new ApplicationProtocolConfig(Protocol.ALPN, SelectorFailureBehavior.FATAL_ALERT, SelectedListenerFailureBehavior.FATAL_ALERT, "h2");
        return SslContextBuilder.forClient()
            .trustManager(trustManager)
            .clientAuth(ClientAuth.REQUIRE)
            .sslProvider(SslProvider.JDK)
            .applicationProtocolConfig(apn)
            .keyManager(keyManagerFactory);
    }
}
