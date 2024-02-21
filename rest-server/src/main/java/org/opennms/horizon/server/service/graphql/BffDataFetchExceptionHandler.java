/*
 * Licensed to The OpenNMS Group, Inc (TOG) under one or more
 * contributor license agreements.  See the LICENSE.md file
 * distributed with this work for additional information
 * regarding copyright ownership.
 *
 * TOG licenses this file to You under the GNU Affero General
 * Public License Version 3 (the "License") or (at your option)
 * any later version.  You may not use this file except in
 * compliance with the License.  You may obtain a copy of the
 * License at:
 *
 *      https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the specific
 * language governing permissions and limitations under the
 * License.
 */
package org.opennms.horizon.server.service.graphql;

import graphql.ExceptionWhileDataFetching;
import graphql.execution.DataFetcherExceptionHandler;
import graphql.execution.DataFetcherExceptionHandlerParameters;
import graphql.execution.DataFetcherExceptionHandlerResult;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import java.util.concurrent.CompletableFuture;
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
    @WithSpan
    @Override
    public CompletableFuture<DataFetcherExceptionHandlerResult> handleException(
            DataFetcherExceptionHandlerParameters parameters) {
        Throwable exception = handleException(parameters.getException());

        ExceptionWhileDataFetching error =
                new ExceptionWhileDataFetching(parameters.getPath(), exception, parameters.getSourceLocation());

        Span.current().recordException(exception);
        log.warn("Caught exception during data fetching", exception);

        var result = DataFetcherExceptionHandlerResult.newResult().error(error).build();
        return CompletableFuture.completedFuture(result);
    }

    private Throwable handleException(Throwable e) {
        if (e instanceof StatusRuntimeException statusRuntimeException
                && (statusRuntimeException.getStatus().getCode().equals(Status.Code.NOT_FOUND)
                        || statusRuntimeException.getStatus().getCode().equals(Status.Code.INVALID_ARGUMENT))) {
            return new GraphQLException(e.getMessage(), e);
        } else if (e instanceof GraphQLException) {
            return e;
        }

        return new GraphQLException("Internal Error", e);
    }
}
