package org.opennms.horizon.it.gqlmodels;

public class AlertEventDefinitionInput {

    private String clearKey;
    private EventType eventType;
    private long id;
    private String name;
    private String reductionKey;
    private String uei;

    public String getClearKey() {
        return clearKey;
    }

    public void setClearKey(String clearKey) {
        this.clearKey = clearKey;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReductionKey() {
        return reductionKey;
    }

    public void setReductionKey(String reductionKey) {
        this.reductionKey = reductionKey;
    }

    public String getUei() {
        return uei;
    }

    public void setUei(String uei) {
        this.uei = uei;
    }
}
