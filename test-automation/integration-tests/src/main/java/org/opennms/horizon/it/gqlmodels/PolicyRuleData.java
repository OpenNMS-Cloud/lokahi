package org.opennms.horizon.it.gqlmodels;

import java.util.List;

public class PolicyRuleData {

    private List<AlertCondition> alertConditions;
    private ManagedObjectType componentType;
    private DetectionMethod detectionMethod;
    private EventType eventType;
    private String name;

    public ManagedObjectType getComponentType() {
        return componentType;
    }

    public void setComponentType(ManagedObjectType componentType) {
        this.componentType = componentType;
    }

    public DetectionMethod getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(DetectionMethod detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AlertCondition> getAlertConditions() {
        return alertConditions;
    }

    public void setAlertConditions(List<AlertCondition> alertConditions) {
        this.alertConditions = alertConditions;
    }
}
