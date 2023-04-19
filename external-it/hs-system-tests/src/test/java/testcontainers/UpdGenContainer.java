package testcontainers;

import lombok.SneakyThrows;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.net.InetAddress;
import java.time.Duration;

public class UpdGenContainer extends GenericContainer<UpdGenContainer> {
    @SneakyThrows
    public UpdGenContainer(Object port, String flowType, Object numberOfPackages) {
        super("opennms/udpgen:latest");
        withNetwork(Network.SHARED)
            .withNetworkAliases("horizon-stream")
            .withCommand("/udpgen",
                "-h", InetAddress.getLocalHost().getHostAddress(),
                "-p", port.toString(),
                "-x", flowType,
                "-r", "10", // packages per second
                "-s", numberOfPackages.toString()
            ).withLogConsumer(
                new Slf4jLogConsumer(LoggerFactory.getLogger(UpdGenContainer.class))
            )
            .waitingFor(
                Wait.forLogMessage(".*Sent " + numberOfPackages + " packets.*", 1)
                    .withStartupTimeout(Duration.ofSeconds(Integer.parseInt(numberOfPackages.toString()) / 10 + 60))
            );
    }
}
