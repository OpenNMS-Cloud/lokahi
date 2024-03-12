package org.opennms.horizon.inventory.exception;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;

public class GrpcConstraintVoilationExceptionHandler {

    public static <T> void handleException(Throwable throwable, StreamObserver<T> responseObserver) {
        if (throwable instanceof DataIntegrityViolationException dataIntegrityException) {
            Throwable rootCause = dataIntegrityException.getRootCause();
            if (rootCause instanceof PSQLException psqlException) {
                handlePSQLException(psqlException, responseObserver);
                return;
            }
        }
        handleInternalError(throwable, responseObserver);
    }

    private static <T> void handlePSQLException(PSQLException psqlException, StreamObserver<T> responseObserver) {
         com.google.rpc.Status status = Status.newBuilder()
            .setCode(Code.INVALID_ARGUMENT_VALUE)
            .setMessage(psqlException.getMessage())
            .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }

    private static <T> void handleInternalError(Throwable throwable, StreamObserver<T> responseObserver) {
         com.google.rpc.Status status = Status.newBuilder()
            .setCode(Code.INTERNAL_VALUE)
            .setMessage(throwable.getMessage())

            .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }


}
