package org.opennms.horizon.notifications.api.dto;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PagerDutyEventAction {
    RESOLVE("resolve"),
    ACKNOWLEDGE("acknowledge"),
    TRIGGER("trigger");

    private String val;
    PagerDutyEventAction(String val) {
        this.val = val;
    }

    @JsonValue()
    public String getVal() {
        return val;
    }
}
