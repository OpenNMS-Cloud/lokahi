/*******************************************************************************
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
 *******************************************************************************/

package org.opennms.horizon.minion.grpc;

public class Constant {
    private Constant() {
        throw new IllegalStateException("Constant class");
    }

    //Exit codes
    public static final int CA_PATH_ERROR = 300;
    public static final int CERT_EXPIRED = 301;
    public static final int CERT_NOTYET = 302;
    public static final int UNAUTHENTICATED = 401;

    public static final int INVALID_CLIENT_STORE = 201;
    public static final int FAIL_LOADING_CLIENT_KEYSTORE = 203;

    public static final int INVALID_TRUST_STORE = 211;
    public static final int FAIL_LOADING_TRUST_KEYSTORE = 212;

}
