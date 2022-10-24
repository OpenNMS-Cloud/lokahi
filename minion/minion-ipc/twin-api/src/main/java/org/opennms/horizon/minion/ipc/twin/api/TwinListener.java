package org.opennms.horizon.minion.ipc.twin.api;

import java.util.function.Consumer;

/**
 * A higher level message listener interface for whiteboard registrations.
 *
 * Service registrations should specify subscriber identifier. It is used to guarantee singularity
 * of a listener registrations.
 *
 * @param <T> Type of payload.
 */
public interface TwinListener<T> extends Consumer<T> {

  Class<T> getType();

  String MESSAGE_LISTENER_TOPIC = "message.listener.topic";

}
