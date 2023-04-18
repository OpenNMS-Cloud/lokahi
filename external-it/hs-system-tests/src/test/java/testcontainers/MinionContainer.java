package testcontainers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

import static org.testcontainers.containers.Network.SHARED;

public class MinionContainer extends GenericContainer<MinionContainer> {
    public String gatewayHost;
    public String minionId;
    public String minionLocation;

    public MinionContainer(String gatewayHost, String minionId, String minionLocation) {
        super("opennms/horizon-stream-minion:latest");
        withExposedPorts(8101)
            .withNetworkAliases("flows")
            .withNetwork(SHARED)
            .withEnv("TZ", "America/New_York")
            .withEnv("MINION_ID", minionId)
            .withEnv("MINION_LOCATION", minionLocation)
            .withEnv("USE_KUBERNETES", "false")
            .withEnv("IGNITE_SERVER_ADDRESSES", "localhost")
            .withEnv("MINION_GATEWAY_HOST", gatewayHost)
            .withEnv("MINION_GATEWAY_PORT", "443")
            .withEnv("MINION_GATEWAY_TLS", "true")
            .waitingFor(
                Wait.forLogMessage(".* Udp Flow Listener started at .*", 3)
                    .withStartupTimeout(Duration.ofMinutes(5))
            )
            .waitingFor(
                Wait.forLogMessage(".*Sending heartbeat from Minion.*", 2)
                    .withStartupTimeout(Duration.ofMinutes(2))
            )
            .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(5)));

        this.gatewayHost = gatewayHost;
        this.minionId = minionId.toUpperCase();
        this.minionLocation = minionLocation;
    }

}
