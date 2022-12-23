/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2018 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.horizon.alarmservice.api;

import java.util.Date;
import java.util.Set;
import org.opennms.horizon.alarmservice.db.entity.Alarm;
import org.opennms.horizon.alarmservice.db.entity.Memo;
import org.opennms.horizon.alarmservice.model.AlarmSeverity;

/**
 * This interface provide functions that should be called
 * immediately after changing the alarm entities while maintaining
 * an open transaction.
 *
 * The implementation should in turn notify any interested listeners
 * i.e. northbounders, correlation engines, etc... about the state change.
 *
 * The implementation should be thread safe.
 *
 * @author jwhite
 */
public interface AlarmEntityNotifier {

    void didCreateAlarm(Alarm alarm);

    void didUpdateAlarmWithReducedEvent(Alarm alarm);

    void didAcknowledgeAlarm(Alarm alarm, String previousAckUser, Date previousAckTime);

    void didUnacknowledgeAlarm(Alarm alarm, String previousAckUser, Date previousAckTime);

    void didUpdateAlarmSeverity(Alarm alarm, AlarmSeverity previousSeverity);

    void didArchiveAlarm(Alarm alarm, String previousReductionKey);

    void didDeleteAlarm(Alarm alarm);

    void didUpdateStickyMemo(Alarm alarm, String previousBody, String previousAuthor, Date previousUpdated);

    void didUpdateReductionKeyMemo(Alarm alarm, String previousBody, String previousAuthor, Date previousUpdated);

    void didDeleteStickyMemo(Alarm alarm, Memo memo);

    void didUpdateLastAutomationTime(Alarm alarm, Date previousLastAutomationTime);

    void didUpdateRelatedAlarms(Alarm alarm, Set<Alarm> previousRelatedAlarms);
}
