package org.opennms.horizon.minion.grpc;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SimpleReconnectStrategy implements Runnable, ReconnectStrategy {

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final ManagedChannel channel;
    private final Runnable onConnect;
    private final Runnable onDisconnect;
    private ScheduledFuture<?> reconnectTask;

    public SimpleReconnectStrategy(ManagedChannel channel, Runnable onConnect, Runnable onDisconnect) {
        this.channel = channel;
        this.onConnect = onConnect;
        this.onDisconnect = onDisconnect;
    }

    @Override
    public void activate() {
        onDisconnect.run();
        reconnectTask = executor.scheduleAtFixedRate(this, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        ConnectivityState state = channel.getState(true);
        if (state == ConnectivityState.READY) {
            if (reconnectTask != null) {
                reconnectTask.cancel(false);
                onConnect.run();
                reconnectTask = null;
            }
        }
    }
}
