package org.opennms.horizon.it.gqlmodels;

public class AlertCondition {

    private AlertEventDefinitionInput clearEvent;
    private int count;
    private int overtime;
    private String overtimeUnit;
    private String severity;
    private AlertEventDefinitionInput triggerEvent;

    public AlertEventDefinitionInput getClearEvent() {
        return clearEvent;
    }

    public void setClearEvent(AlertEventDefinitionInput clearEvent) {
        this.clearEvent = clearEvent;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getOvertime() {
        return overtime;
    }

    public void setOvertime(int overtime) {
        this.overtime = overtime;
    }

    public String getOvertimeUnit() {
        return overtimeUnit;
    }

    public void setOvertimeUnit(String overtimeUnit) {
        this.overtimeUnit = overtimeUnit;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public AlertEventDefinitionInput getTriggerEvent() {
        return triggerEvent;
    }

    public void setTriggerEvent(AlertEventDefinitionInput triggerEvent) {
        this.triggerEvent = triggerEvent;
    }
}
