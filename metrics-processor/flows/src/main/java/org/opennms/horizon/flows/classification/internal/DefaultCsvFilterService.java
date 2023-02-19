/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2018 The OpenNMS Group, Inc.
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

package org.opennms.horizon.flows.classification.internal;

import org.opennms.horizon.flows.classification.FilterService;
import org.opennms.horizon.flows.classification.exception.InvalidFilterException;

/**
 * Default Csv filter service currently there is no operation.
 */
public class DefaultCsvFilterService implements FilterService {


    public DefaultCsvFilterService() {
    }

    @Override
    public void validate(final String filterExpression) throws InvalidFilterException {
        if (filterExpression == null || filterExpression.isEmpty()) {
            throw new InvalidFilterException(filterExpression, "Empty filterExpression.");
        }
        if (!filterExpression.contains(",")) {
            throw new InvalidFilterException(filterExpression, "Commas are expected.");
        }
    }

    @Override
    public boolean matches(final String address, final String filterExpression) {
        return false;
    }
}
