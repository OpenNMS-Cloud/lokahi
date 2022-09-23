/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2022 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2022 The OpenNMS Group, Inc.
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

package org.opennms.horizon.datachoices.internal;

import org.opennms.horizon.db.dao.api.DataChoicesDao;
import org.opennms.horizon.db.dao.api.SessionUtils;
import org.opennms.horizon.db.model.OnmsDataChoices;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class StateManager {
    private final List<StateChangeHandler> listeners = new ArrayList<>();
    private DataChoicesDao dataChoicesDao;
    private SessionUtils sessionUtils;

    public void setEnabled(boolean enabled) {
        this.sessionUtils.withTransaction(() -> {
            OnmsDataChoices dataChoice = this.dataChoicesDao.find();
            for (StateChangeHandler listener : this.listeners) {
                listener.onEnabledChanged(enabled);
            }
            dataChoice.setEnabled(enabled);
            this.dataChoicesDao.saveOrUpdate(dataChoice);
        });
    }

    public OnmsDataChoices getDataChoices() {
        return this.sessionUtils
            .withReadOnlyTransaction(() -> this.dataChoicesDao.find());
    }

    public void onIsEnabledChanged(StateChangeHandler callback) {
        this.listeners.add(Objects.requireNonNull(callback));
    }

    public void setDataChoicesDao(DataChoicesDao dataChoicesDao) {
        this.dataChoicesDao = dataChoicesDao;
    }

    public void setSessionUtils(SessionUtils sessionUtils) {
        this.sessionUtils = sessionUtils;
    }

    public interface StateChangeHandler {
        void onEnabledChanged(boolean enabled);
    }
}
