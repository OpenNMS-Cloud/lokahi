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
package org.opennms.horizon.inventory.exception;

import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;

public class GrpcConstraintVoilationExceptionHandler {

    public static <T> void handleException(Throwable throwable, StreamObserver<T> responseObserver, final int code) {
        if (throwable instanceof DataIntegrityViolationException dataIntegrityException) {
            Throwable rootCause = dataIntegrityException.getRootCause();
            if (rootCause instanceof PSQLException psqlException) {
                handlePSQLException(psqlException, responseObserver);
                return;
            }
        }
        handleInternalError(throwable, responseObserver, code);
    }

    private static <T> void handlePSQLException(PSQLException psqlException, StreamObserver<T> responseObserver) {
        com.google.rpc.Status status = Status.newBuilder()
                .setCode(Code.INVALID_ARGUMENT_VALUE)
                .setMessage(psqlException.getMessage())
                .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }

    private static <T> void handleInternalError(
            Throwable throwable, StreamObserver<T> responseObserver, final int code) {
        com.google.rpc.Status status = Status.newBuilder()
                .setCode(code)
                .setMessage(throwable.getMessage())
                .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
}
