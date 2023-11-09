/*
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2023 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2023 The OpenNMS Group, Inc.
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
 */

package org.opennms.horizon.server.service.graphql;

import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.analysis.QueryDepthInfo;
import graphql.execution.AbortExecutionException;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Function;

@Slf4j
public class MaxDepthInstrumentation extends MaxQueryDepthInstrumentation {
    public MaxDepthInstrumentation(int maxDepth) {
        super(maxDepth);
    }

    public MaxDepthInstrumentation(int maxDepth, Function<QueryDepthInfo, Boolean> maxQueryDepthExceededFunction) {
        super(maxDepth, maxQueryDepthExceededFunction);
    }

    @Override
    protected AbortExecutionException mkAbortException(int depth, int maxDepth) {
        log.debug("maximum query depth exceeded {} > {}", depth, maxDepth);
        return new AbortExecutionException("maximum query depth exceeded");
    }
}
