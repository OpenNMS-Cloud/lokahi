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

package org.opennms.horizon.shared.dto.notifications;

public class PagerDutyConfigDTO {
    public PagerDutyConfigDTO() {
    }

    public PagerDutyConfigDTO(String integrationkey) {
        this.integrationkey = integrationkey;
    }

    private String integrationkey;

    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;

        PagerDutyConfigDTO that = (PagerDutyConfigDTO) object;

        if (!integrationkey.equals(that.integrationkey)) return false;

        return true;
    }

    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + integrationkey.hashCode();
        return result;
    }

    public String getIntegrationkey() {
        return integrationkey;
    }

    public void setIntegrationkey(String integrationkey) {
        this.integrationkey = integrationkey;
    }
}
