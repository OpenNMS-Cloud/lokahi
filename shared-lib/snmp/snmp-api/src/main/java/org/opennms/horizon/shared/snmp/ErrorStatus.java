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
package org.opennms.horizon.shared.snmp;

public enum ErrorStatus {
    NO_ERROR {
        @Override
        public boolean isFatal() {
            return FATAL_NO_ERROR;
        }

        @Override
        public boolean retry() {
            return RETRY_NO_ERROR;
        }
    },
    TOO_BIG {
        @Override
        public boolean isFatal() {
            return FATAL_TOO_BIG;
        }

        @Override
        public boolean retry() {
            return RETRY_TOO_BIG;
        }
    },
    NO_SUCH_NAME {
        @Override
        public boolean isFatal() {
            return FATAL_NO_SUCH_NAME;
        }

        @Override
        public boolean retry() {
            return RETRY_NO_SUCH_NAME;
        }
    },
    BAD_VALUE {
        @Override
        public boolean isFatal() {
            return FATAL_BAD_VALUE;
        }

        @Override
        public boolean retry() {
            return RETRY_BAD_VALUE;
        }
    },
    READ_ONLY {
        @Override
        public boolean isFatal() {
            return FATAL_READ_ONLY;
        }

        @Override
        public boolean retry() {
            return RETRY_READ_ONLY;
        }
    },
    GEN_ERR {
        @Override
        public boolean isFatal() {
            return FATAL_GEN_ERR;
        }

        @Override
        public boolean retry() {
            return RETRY_GEN_ERR;
        }
    },
    NO_ACCESS {
        @Override
        public boolean isFatal() {
            return FATAL_NO_ACCESS;
        }

        @Override
        public boolean retry() {
            return RETRY_NO_ACCESS;
        }
    },
    WRONG_TYPE {
        @Override
        public boolean isFatal() {
            return FATAL_WRONG_TYPE;
        }

        @Override
        public boolean retry() {
            return RETRY_WRONG_TYPE;
        }
    },
    WRONG_LENGTH {
        @Override
        public boolean isFatal() {
            return FATAL_WRONG_LENGTH;
        }

        @Override
        public boolean retry() {
            return RETRY_WRONG_LENGTH;
        }
    },
    WRONG_ENCODING {
        @Override
        public boolean isFatal() {
            return FATAL_WRONG_ENCODING;
        }

        @Override
        public boolean retry() {
            return RETRY_WRONG_ENCODING;
        }
    },
    WRONG_VALUE {
        @Override
        public boolean isFatal() {
            return FATAL_WRONG_VALUE;
        }

        @Override
        public boolean retry() {
            return RETRY_WRONG_VALUE;
        }
    },
    NO_CREATION {
        @Override
        public boolean isFatal() {
            return FATAL_NO_CREATION;
        }

        @Override
        public boolean retry() {
            return RETRY_NO_CREATION;
        }
    },
    INCONSISTENT_VALUE {
        @Override
        public boolean isFatal() {
            return FATAL_INCONSISTENT_VALUE;
        }

        @Override
        public boolean retry() {
            return RETRY_INCONSISTENT_VALUE;
        }
    },
    RESOURCE_UNAVAILABLE {
        @Override
        public boolean isFatal() {
            return FATAL_RESOURCE_UNAVAILABLE;
        }

        @Override
        public boolean retry() {
            return RETRY_RESOURCE_UNAVAILABLE;
        }
    },
    COMMIT_FAILED {
        @Override
        public boolean isFatal() {
            return FATAL_COMMIT_FAILED;
        }

        @Override
        public boolean retry() {
            return RETRY_COMMIT_FAILED;
        }
    },
    UNDO_FAILED {
        @Override
        public boolean isFatal() {
            return FATAL_UNDO_FAILED;
        }

        @Override
        public boolean retry() {
            return RETRY_UNDO_FAILED;
        }
    },
    AUTHORIZATION_ERROR {
        @Override
        public boolean isFatal() {
            return FATAL_AUTHORIZATION_ERROR;
        }

        @Override
        public boolean retry() {
            return RETRY_AUTHORIZATION_ERROR;
        }
    },
    NOT_WRITABLE {
        @Override
        public boolean isFatal() {
            return FATAL_NOT_WRITABLE;
        }

        @Override
        public boolean retry() {
            return RETRY_NOT_WRITABLE;
        }
    },
    INCONSISTENT_NAME {
        @Override
        public boolean isFatal() {
            return FATAL_INCONSISTENT_NAME;
        }

        @Override
        public boolean retry() {
            return RETRY_INCONSISTENT_NAME;
        }
    };

    private static final boolean FATAL_NO_ERROR =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.0.fatal", "false"));
    private static final boolean RETRY_NO_ERROR =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.0.retry", "false"));

    private static final boolean FATAL_TOO_BIG =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.1.fatal", "false"));
    private static final boolean RETRY_TOO_BIG =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.1.retry", "true"));

    private static final boolean FATAL_NO_SUCH_NAME =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.2.fatal", "false"));
    private static final boolean RETRY_NO_SUCH_NAME =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.2.retry", "true"));

    private static final boolean FATAL_BAD_VALUE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.3.fatal", "false"));
    private static final boolean RETRY_BAD_VALUE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.3.retry", "false"));

    private static final boolean FATAL_READ_ONLY =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.4.fatal", "false"));
    private static final boolean RETRY_READ_ONLY =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.4.retry", "false"));

    private static final boolean FATAL_GEN_ERR =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.5.fatal", "false"));
    private static final boolean RETRY_GEN_ERR =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.5.retry", "true"));

    private static final boolean FATAL_NO_ACCESS =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.6.fatal", "false"));
    private static final boolean RETRY_NO_ACCESS =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.6.retry", "true"));

    private static final boolean FATAL_WRONG_TYPE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.7.fatal", "false"));
    private static final boolean RETRY_WRONG_TYPE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.7.retry", "false"));

    private static final boolean FATAL_WRONG_LENGTH =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.8.fatal", "false"));
    private static final boolean RETRY_WRONG_LENGTH =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.8.retry", "false"));

    private static final boolean FATAL_WRONG_ENCODING =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.9.fatal", "false"));
    private static final boolean RETRY_WRONG_ENCODING =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.9.retry", "false"));

    private static final boolean FATAL_WRONG_VALUE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.10.fatal", "false"));
    private static final boolean RETRY_WRONG_VALUE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.10.retry", "false"));

    private static final boolean FATAL_NO_CREATION =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.11.fatal", "false"));
    private static final boolean RETRY_NO_CREATION =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.11.retry", "false"));

    private static final boolean FATAL_INCONSISTENT_VALUE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.12.fatal", "false"));
    private static final boolean RETRY_INCONSISTENT_VALUE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.12.retry", "false"));

    private static final boolean FATAL_RESOURCE_UNAVAILABLE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.13.fatal", "false"));
    private static final boolean RETRY_RESOURCE_UNAVAILABLE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.13.retry", "false"));

    private static final boolean FATAL_COMMIT_FAILED =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.14.fatal", "false"));
    private static final boolean RETRY_COMMIT_FAILED =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.14.retry", "false"));

    private static final boolean FATAL_UNDO_FAILED =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.15.fatal", "false"));
    private static final boolean RETRY_UNDO_FAILED =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.15.retry", "false"));

    private static final boolean FATAL_AUTHORIZATION_ERROR =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.16.fatal", "false"));
    private static final boolean RETRY_AUTHORIZATION_ERROR =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.16.retry", "true"));

    private static final boolean FATAL_NOT_WRITABLE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.17.fatal", "false"));
    private static final boolean RETRY_NOT_WRITABLE =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.17.retry", "false"));

    private static final boolean FATAL_INCONSISTENT_NAME =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.18.fatal", "false"));
    private static final boolean RETRY_INCONSISTENT_NAME =
            Boolean.valueOf(System.getProperty("org.opennms.netmgt.snmp.errorStatus.18.retry", "false"));

    /**
     * Whether or not this error status should be fatal (ie, throw an exception).
     */
    public abstract boolean isFatal();

    /**
     * Whether or not a retry should be attempted upon receiving this error code.
     */
    public abstract boolean retry();

    public static ErrorStatus fromStatus(final int status) {
        return ErrorStatus.values()[status];
    }
}
