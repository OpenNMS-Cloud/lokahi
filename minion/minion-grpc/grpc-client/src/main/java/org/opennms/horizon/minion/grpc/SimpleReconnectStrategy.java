package org.opennms.horizon.minion.grpc;

import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SimpleReconnectStrategy implements Runnable, ReconnectStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(SimpleReconnectStrategy.class);
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
    public synchronized void activate() {
        if (reconnectTask != null) {
            LOG.trace("Ignoring activate request. One is already in progress.");
            return;
        }
        onDisconnect.run();
        reconnectTask = executor.scheduleAtFixedRate(this, 5000, 5000, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        reconnectTask.cancel(true);
        executor.shutdownNow();
    }

    @Override
    public void run() {
        ConnectivityState state = channel.getState(true);

        if (state == ConnectivityState.READY) {
            // The onConnect callback may block, so we leave this out of any critical section
            onConnect.run();
            synchronized (this) {
                // After successfully triggering onConnect, cancel future executions
                if (reconnectTask != null) {
                    reconnectTask.cancel(false);
                    reconnectTask = null;
                }
            }
        } else {
            LOG.info("Channel is in currently in state: {}. Waiting for it to be READY.", state);
        }
    }
}
