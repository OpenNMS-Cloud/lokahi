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

package org.opennms.horizon.alertservice.drools;

import org.kie.api.runtime.rule.FactHandle;
import org.opennms.horizon.alertservice.db.entity.AlertAssociation;

public class AlertAssociationAndFact {

    private AlertAssociation alertAssociation;
    private FactHandle fact;

    public AlertAssociationAndFact(AlertAssociation alertAssociation, FactHandle fact) {
        this.alertAssociation = alertAssociation;
        this.fact = fact;
    }

    public AlertAssociation getAlertAssociation() {
        return alertAssociation;
    }

    public void setAlertAssociation(AlertAssociation alertAssociation) {
        this.alertAssociation = alertAssociation;
    }

    public FactHandle getFact() {
        return fact;
    }

    public void setFact(FactHandle fact) {
        this.fact = fact;
    }
}
