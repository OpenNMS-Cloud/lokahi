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

package org.opennms.horizon.server.web;

import org.opennms.horizon.server.service.graphql.BffDataFetchExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles web-level exceptions, ensuring that appropriate data is returned.
 * <p>
 * Note That this exception handler will not handle exceptions that occur during
 * GraphQL data fetching. Those are instead handled with a
 * {@link graphql.execution.DataFetcherExceptionHandler}.
 *
 * @see BffDataFetchExceptionHandler
 * @see org.springframework.boot.autoconfigure.web.reactive.error.ErrorWebFluxAutoConfiguration#errorWebExceptionHandler
 */
@Component
@Order(-2)
public class WebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public WebExceptionHandler(
        ErrorAttributes errorAttributes,
        WebProperties webProperties,
        ApplicationContext applicationContext,
        ObjectProvider<ViewResolver> viewResolvers,
        ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(errorAttributes, webProperties.getResources(), applicationContext);

        // Additional required config. See org.springframework.boot.autoconfigure
        // .web.reactive.error.ErrorWebFluxAutoConfiguration.errorWebExceptionHandler
        this.setViewResolvers(viewResolvers.orderedStream().collect(Collectors.toList()));
        this.setMessageReaders(serverCodecConfigurer.getReaders());
        this.setMessageWriters(serverCodecConfigurer.getWriters());
    }

    /** {@inheritDoc} */
    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable t = getError(request);

        HttpStatus status;
        String message;
        if (t instanceof ResponseStatusException e) {
            // ResponseStatusException#getMessage will reference the root cause,
            // which we don't want to expose externally. If a reason was
            // explicitly provided, display that, otherwise use the status
            // reason phrase (ie, "Not Found")
            status = e.getStatus();
            message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
        } else {
            // Any exceptions we don't explicitly handle should be considered
            // unexpected, and therefore an internal error.
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            message = status.getReasonPhrase();
        }

        Map<String, Object> errorPropertiesMap = getErrorAttributes(request,
            ErrorAttributeOptions.defaults());
        errorPropertiesMap.put("message", message);

        return ServerResponse.status(status)
            .contentType(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(errorPropertiesMap));
    }
}
