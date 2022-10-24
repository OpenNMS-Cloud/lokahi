package org.opennms.taskset.service.api;

public interface TaskSetForwarder {
    /**
     * Add a listener for task set updates.
     *
     * NOTE: listeners are called inline/synchronously from the thread posting updates, so any long-running, blocking
     * listeners must be avoided.
     *
     * @param location
     * @param listener
     */
    void addListener(String location, TaskSetListener listener);

    /**
     * Remove a listener that was added with addListener().  Note that listeners are tracked by their system identity,
     * so the same instance of the listener that was added must be used to remove the listener.
     *
     * @param location
     * @param listener
     */
    void removeListener(String location, TaskSetListener listener);
}
