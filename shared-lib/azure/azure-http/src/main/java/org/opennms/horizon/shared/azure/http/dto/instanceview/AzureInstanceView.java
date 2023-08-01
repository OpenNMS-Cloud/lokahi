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


package org.opennms.horizon.shared.azure.http.dto.instanceview;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AzureInstanceView {
    private static final String POWER_STATE_RUNNING = "PowerState/running";

    @SerializedName("computerName")
    private String computerName;
    @SerializedName("osName")
    private String osName;
    @SerializedName("osVersion")
    private String osVersion;
    @SerializedName("statuses")
    private List<AzureStatus> statuses = new ArrayList<>();

    public boolean isUp() {
        for (AzureStatus status : getStatuses()) {
            String code = status.getCode();
            if (code.equalsIgnoreCase(POWER_STATE_RUNNING)) {
                return true;
            }
        }
        return false;
    }
}
