package org.opennms.horizon.core.monitor.taskset;

import java.util.HashMap;
import java.util.Map;

public class LocationBasedTaskSetManager {

    private Map<String, TaskSetManager> taskSetManagerByLocation = new HashMap<>();

    public TaskSetManager getManagerForLocation(String location) {
        taskSetManagerByLocation.putIfAbsent(location, new TaskSetManager());

        return taskSetManagerByLocation.get(location);
    }

    public TaskSetManager removeManagerForLocation(String location) {
        return taskSetManagerByLocation.remove(location);
    }
}
