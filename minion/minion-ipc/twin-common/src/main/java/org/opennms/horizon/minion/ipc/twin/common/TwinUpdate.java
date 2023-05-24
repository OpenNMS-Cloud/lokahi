/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.ipc.twin.common;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class TwinUpdate {
    
    private final TwinRequest twinRequest;
    private byte[] object;
    private int version;
    private boolean isPatch;
    private String sessionId;
    @EqualsAndHashCode.Exclude
    private Map<String, String> tracingInfo = new HashMap<>();

    public TwinUpdate() {
        this.twinRequest = new TwinRequest();
    }

    public TwinUpdate(String key, byte[] object) {
        this.twinRequest = new TwinRequest(key);
        this.object = object;
    }
    public TwinUpdate(String key) {
        this.twinRequest = new TwinRequest(key);
    }

    public void setKey(String key) {
        twinRequest.setKey(key);
    }

    public String getKey() {
        return twinRequest.getKey();
    }

    public Map<String, String> getTracingInfo() {
        return tracingInfo;
    }

    public void addTracingInfo(String key, String value) {
        this.tracingInfo.put(key, value);
    }
}
