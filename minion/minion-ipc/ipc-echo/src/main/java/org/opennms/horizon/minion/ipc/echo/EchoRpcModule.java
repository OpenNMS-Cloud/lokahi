/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2016 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2016 The OpenNMS Group, Inc.
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

package org.opennms.horizon.minion.ipc.echo;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import java.io.StringReader;
import java.io.StringWriter;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import org.opennms.horizon.shared.ipc.rpc.api.RpcModule;

public class EchoRpcModule implements RpcModule<EchoRequest, EchoResponse> {

    public static final EchoRpcModule INSTANCE = new EchoRpcModule();

    public static final String RPC_MODULE_ID = "Echo";

    private static final Supplier<Timer> TIMER_SUPPLIER = Suppliers.memoize(() -> new Timer("EchoRpcModule"));

    public EchoRpcModule() {
//        TODO: re-implement this
        throw  new UnsupportedOperationException();

//        try {
//            context = JAXBContext.newInstance(EchoRequest.class, EchoResponse.class);
//        } catch (JAXBException e) {
//            throw new RuntimeException("Failed to initialize EchoRpcModule", e);
//        }
    }

    public void beforeRun() { }

    @Override
    public CompletableFuture<EchoResponse> execute(final EchoRequest request) {
        final CompletableFuture<EchoResponse> future = new CompletableFuture<>();
        if (request.getDelay() != null) {
            TIMER_SUPPLIER.get().schedule(new TimerTask() {
                @Override
                public void run() {
                    processRequest(request, future);
                }
            }, request.getDelay());
        } else {
            processRequest(request, future);
        }
        return future;
    }

    public void processRequest(EchoRequest request, CompletableFuture<EchoResponse> future) {
        beforeRun();
        if (request.shouldThrow()) {
            future.completeExceptionally(new MyEchoException(request.getMessage()));
        } else {
            EchoResponse response = new EchoResponse();
            response.setId(request.getId());
            response.setMessage(request.getMessage());
            response.setBody(request.getBody());
            future.complete(response);
        }
    }

    @Override
    public String getId() {
        return RPC_MODULE_ID;
    }

    @Override
    public String marshalRequest(EchoRequest echoRequest) {
        return marshal(echoRequest);
    }

    @Override
    public EchoRequest unmarshalRequest(String payload) {
        return unmarshal(EchoRequest.class, payload);
    }

    @Override
    public String marshalResponse(EchoResponse echoResponse) {
        return marshal(echoResponse);
    }

    @Override
    public EchoResponse unmarshalResponse(String payload) {
        return unmarshal(EchoResponse.class, payload);
    }

    @Override
    public EchoResponse createResponseWithException(Throwable ex) {
        return new EchoResponse(ex);
    }

    private <T> T unmarshal(Class<T> type, String payload) {
//        TODO: re-implmplement
        throw new UnsupportedOperationException();
//        try {
//            Object unmarshal = context.createUnmarshaller().unmarshal(new StringReader(payload));
//            if (type.isInstance(unmarshal)) {
//                return type.cast(unmarshal);
//            }
//            throw new IllegalArgumentException("Unexpected value " + unmarshal.getClass().getName() + "  received");
//        } catch (JAXBException e) {
//            throw new RuntimeException("Could not ");
//        }
    }

    private <T> String marshal(T payload) {
        //        TODO: re-implmplement
        throw new UnsupportedOperationException();
//        try {
//            StringWriter writer = new StringWriter();
//            context.createMarshaller().marshal(payload, writer);
//            return writer.toString();
//        } catch (JAXBException e) {
//            throw new RuntimeException("Could not serialize " + payload.getClass().getName());
//        }
    }

}
