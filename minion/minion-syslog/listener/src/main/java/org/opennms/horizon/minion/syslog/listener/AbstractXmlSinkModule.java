package org.opennms.horizon.minion.syslog.listener;/*
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


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public abstract class AbstractXmlSinkModule<S extends Message, T extends Message> implements SinkModule<S, T> {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractXmlSinkModule.class);



    /**
     * Store a thread-local reference to the {@link XmlHandler} because 
     * Unmarshalers are not thread-safe.


    public AbstractXmlSinkModule(Class<T> messageClazz) {
        this.messageClazz = Objects.requireNonNull(messageClazz);
    }



    @Override
    public byte[] marshalSingleMessage(S message) {
        return marshal((T)getAggregationPolicy().aggregate(null, message));
    }

    public abstract String getId();

    public abstract int getNumConsumerThreads();



    public abstract AsyncPolicy getAsyncPolicy();

    /** Modules with different aggregated message should override this method **/
    @Override
    public S unmarshalSingleMessage(byte[] bytes) {
        T log = unmarshal(bytes);
        return (S)log;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        return getClass() == obj.getClass();
    }



}
