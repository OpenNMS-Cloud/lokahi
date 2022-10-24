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

package org.opennms.horizon.shared.icmp.proxy;

import java.util.List;
import java.util.Objects;
import java.util.stream.LongStream;

public class PingSummaryCalculator {

    private final List<PingSequence> sequences;

    public PingSummaryCalculator(List<PingSequence> pingSequenceList) {
        this.sequences = Objects.requireNonNull(pingSequenceList);
    }

    public int getPacketsTransmitted() {
        return sequences.size();
    }

    public double getPacketLoss() {
        return 1.0 - (double) getPacketsReceived() / (double) getPacketsTransmitted();
    }

    public int getPacketsReceived() {
        return (int) sequences.stream().filter(PingSequence::isSuccess).count();
    }

    public long getMin() {
        return getDiffTimeNanos().min().orElse(0);
    }

    public double getAvg() {
        return getDiffTimeNanos().average().orElse(0);
    }

    public long getMax() {
        return getDiffTimeNanos().max().orElse(0);
    }

    public double getStdDev() {
        return getAvg() - getMin();
    }

    private LongStream getDiffTimeNanos() {
        return sequences.stream().filter(PingSequence::isSuccess).mapToLong(eachSequence -> (long) eachSequence.getResponse().getRtt());
    }

}
