package org.opennms.horizon.it.gqlmodels;

import java.util.List;

public class MonitorPolicyInputData {

    private String memo;
    private String name;
    private boolean notifyByEmail;
    private boolean notifyByPagerDuty;
    private boolean notifyByWebhooks;
    private String notifyInstruction;
    private List<String> tags;
    private List<PolicyRuleData> rules;

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isNotifyByEmail() {
        return notifyByEmail;
    }

    public void setNotifyByEmail(boolean notifyByEmail1) {
        this.notifyByEmail = notifyByEmail1;
    }

    public boolean isNotifyByPagerDuty() {
        return notifyByPagerDuty;
    }

    public void setNotifyByPagerDuty(boolean notifyByPagerDuty) {
        this.notifyByPagerDuty = notifyByPagerDuty;
    }

    public boolean isNotifyByWebhooks() {
        return notifyByWebhooks;
    }

    public void setNotifyByWebhooks(boolean notifyByWebhooks) {
        this.notifyByWebhooks = notifyByWebhooks;
    }

    public String getNotifyInstruction() {
        return notifyInstruction;
    }

    public void setNotifyInstruction(String notifyInstruction) {
        this.notifyInstruction = notifyInstruction;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<PolicyRuleData> getRules() {
        return rules;
    }

    public void setRules(List<PolicyRuleData> rules) {
        this.rules = rules;
    }
}
