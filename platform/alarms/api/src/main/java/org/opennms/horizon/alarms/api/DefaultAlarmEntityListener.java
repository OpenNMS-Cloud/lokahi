package org.opennms.horizon.alarms.api;

import org.opennms.horizon.db.model.*;

import java.util.Date;
import java.util.Set;

public class DefaultAlarmEntityListener implements AlarmEntityListener{
    @Override
    public void onAlarmCreated(OnmsAlarm alarm) {

    }

    @Override
    public void onAlarmUpdatedWithReducedEvent(OnmsAlarm alarm) {

    }

    @Override
    public void onAlarmAcknowledged(OnmsAlarm alarm, String previousAckUser, Date previousAckTime) {

    }

    @Override
    public void onAlarmUnacknowledged(OnmsAlarm alarm, String previousAckUser, Date previousAckTime) {

    }

    @Override
    public void onAlarmSeverityUpdated(OnmsAlarm alarm, OnmsSeverity previousSeverity) {

    }

    @Override
    public void onAlarmArchived(OnmsAlarm alarm, String previousReductionKey) {

    }

    @Override
    public void onAlarmDeleted(OnmsAlarm alarm) {

    }

    @Override
    public void onStickyMemoUpdated(OnmsAlarm alarm, String previousBody, String previousAuthor, Date previousUpdated) {

    }

    @Override
    public void onReductionKeyMemoUpdated(OnmsAlarm alarm, String previousBody, String previousAuthor, Date previousUpdated) {

    }

    @Override
    public void onStickyMemoDeleted(OnmsAlarm alarm, OnmsMemo memo) {

    }

    @Override
    public void onReductionKeyMemoDeleted(OnmsAlarm alarm, OnmsReductionKeyMemo memo) {

    }

    @Override
    public void onLastAutomationTimeUpdated(OnmsAlarm alarm, Date previousLastAutomationTime) {

    }

    @Override
    public void onRelatedAlarmsUpdated(OnmsAlarm alarm, Set<OnmsAlarm> previousRelatedAlarms) {

    }

    @Override
    public void onTicketStateChanged(OnmsAlarm alarm, TroubleTicketState previousState) {

    }
}
