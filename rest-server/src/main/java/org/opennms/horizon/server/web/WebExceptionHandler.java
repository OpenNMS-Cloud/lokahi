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
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.UnsupportedMediaTypeStatusException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

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
@RestControllerAdvice
public class WebExceptionHandler {

    private final ErrorAttributes errorAttributes;
    private final List<HttpMessageReader<?>> messageReaders;


    public WebExceptionHandler(
        ErrorAttributes errorAttributes,
        ServerCodecConfigurer serverCodecConfigurer
    ) {
        this.errorAttributes = errorAttributes;
        this.messageReaders = serverCodecConfigurer.getReaders();
    }

    @ExceptionHandler(UnsupportedMediaTypeStatusException.class)
    public Mono<ResponseEntity<Object>> handle(UnsupportedMediaTypeStatusException e, ServerWebExchange exchange) {
        return createResponse(e, exchange, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Object>> handle(ResponseStatusException e, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();

        return createResponse(e, exchange, status, message);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Object>> handle(Exception e, ServerWebExchange exchange) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        return createResponse(e, exchange, status);
    }

    private Mono<ResponseEntity<Object>> createResponse(
        Throwable t,
        ServerWebExchange exchange,
        HttpStatus status
    ) {
        return createResponse(t, exchange, status, status.getReasonPhrase());
    }

    private Mono<ResponseEntity<Object>> createResponse(
        Throwable t,
        ServerWebExchange exchange,
        HttpStatus status,
        String message
    ) {
        errorAttributes.storeErrorInformation(t, exchange);
        var request = ServerRequest.create(exchange, messageReaders);
        Map<String, Object> attributes = errorAttributes.getErrorAttributes(
            request, ErrorAttributeOptions.defaults()
        );
        attributes.put("message", message);

        return Mono.just(new ResponseEntity<>(attributes, status));
    }
}
