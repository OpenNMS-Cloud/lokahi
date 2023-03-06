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

package org.opennms.horizon.alertservice.model;

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import lombok.Data;

@Data
public class AlertAssociationDTO implements Serializable {

    private static final long serialVersionUID = 4115687014888009683L;

    private Integer alertAssociationId;
    private AlertDTO situationAlert;
    private AlertDTO relatedAlert;
    private Date mappedTime;

    public AlertAssociationDTO() {
    }

    public AlertAssociationDTO(AlertDTO situationAlert, AlertDTO relatedAlert) {
        this(situationAlert, relatedAlert, new Date());
    }

    public AlertAssociationDTO(AlertDTO situationAlert, AlertDTO relatedAlert, Date mappedTime) {
        this.mappedTime = mappedTime;
        this.situationAlert = situationAlert;
        this.relatedAlert = relatedAlert;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AlertAssociationDTO that = (AlertAssociationDTO) o;
        return Objects.equals(situationAlert, that.situationAlert) &&
                Objects.equals(relatedAlert, that.relatedAlert);
    }

    @Override
    public int hashCode() {
        return Objects.hash(situationAlert, relatedAlert);
    }
}
