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

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.opennms.horizon.server.exception.GraphQLException;

/**
 * A {@link DataFetcherExceptionHandler} that ensures exceptions that occur
 * during graphql execution are returned with appropriate response data.
 * Replaces the default
 * {@link graphql.execution.SimpleDataFetcherExceptionHandler}
 */
@Slf4j
public class BffDataFetchExceptionHandler implements DataFetcherExceptionHandler {

    /** {@inheritDoc} */
    @Override
    public DataFetcherExceptionHandlerResult onException(DataFetcherExceptionHandlerParameters parameters) {
        Throwable exception = handleException(parameters.getException());

        ExceptionWhileDataFetching error = new ExceptionWhileDataFetching(
            parameters.getPath(),
            exception,
            parameters.getSourceLocation()
        );

        log.warn(error.getMessage(), exception);

        return DataFetcherExceptionHandlerResult
            .newResult()
            .error(error)
            .build();
    }

    private Throwable handleException(Throwable e) {
        if (e instanceof StatusRuntimeException statusRuntimeException
            && statusRuntimeException.getStatus().getCode().equals(Status.Code.NOT_FOUND)) {
            return new GraphQLException(e.getMessage(), e);
        } else if (e instanceof GraphQLException) {
            return e;
        }

        return new GraphQLException("Internal Error", e);
    }
}
