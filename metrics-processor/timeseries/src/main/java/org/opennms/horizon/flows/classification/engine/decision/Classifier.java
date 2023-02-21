/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2018-2021 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2021 The OpenNMS Group, Inc.
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
package org.opennms.horizon.flows.classification.engine.decision;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.opennms.horizon.flows.classification.api.ClassificationRequest;
import org.opennms.horizon.flows.classification.api.FilterService;
import org.opennms.horizon.flows.classification.api.model.RuleDefinition;
import org.opennms.horizon.flows.classification.engine.matcher.DstAddressMatcher;
import org.opennms.horizon.flows.classification.engine.matcher.DstPortMatcher;
import org.opennms.horizon.flows.classification.engine.matcher.FilterMatcher;
import org.opennms.horizon.flows.classification.engine.matcher.Matcher;
import org.opennms.horizon.flows.classification.engine.matcher.ProtocolMatcher;
import org.opennms.horizon.flows.classification.engine.matcher.SrcAddressMatcher;
import org.opennms.horizon.flows.classification.engine.matcher.SrcPortMatcher;

/**
 * Classifies classification requests.
 * <p>
 * Classifiers are stored in leaf nodes of classification decision trees. They are derived from
 * {@link RuleDefinition}s. They contain a couple of
 * {@link Matcher}s that are checked during classification. The matchers may do a simplified test because
 * some of their rule's conditions may already have been covered by thresholds along the path through the
 * decision tree. Classifiers have the same sort ordered as their underlying rules.
 */
public class Classifier implements Comparable<Classifier> {

    private static <RV> void addMatcher(List<Matcher> matchers, RV ruleValue, Function<RV, Matcher> matcherCreator) {
        if (ruleValue != null) {
            matchers.add(matcherCreator.apply(ruleValue));
        }
    }

    /**
     * Constructs a classifier for a rule simplifying its conditions corresponding to the given bounds.
     */
    public static Classifier of(PreprocessedRule rule, FilterService filterService, Bounds bounds) {
        final List<Matcher> matchers = new ArrayList<>();
        int matchedAspects = 0;
        if (rule.protocol != null) {
            matchedAspects++;
            addMatcher(matchers, rule.protocol.shrink(bounds.protocol), ProtocolMatcher::new);
        }
        if (rule.srcPort != null) {
            matchedAspects++;
            addMatcher(matchers, rule.srcPort.shrink(bounds.srcPort), SrcPortMatcher::new);
        }
        if (rule.dstPort != null) {
            matchedAspects++;
            addMatcher(matchers, rule.dstPort.shrink(bounds.dstPort), DstPortMatcher::new);
        }
        if (rule.srcAddr != null) {
            matchedAspects++;
            addMatcher(matchers, rule.srcAddr.shrink(bounds.srcAddr), SrcAddressMatcher::new);
        }
        if (rule.dstAddr != null) {
            matchedAspects++;
            addMatcher(matchers, rule.dstAddr.shrink(bounds.dstAddr), DstAddressMatcher::new);
        }
        if (rule.ruleDefinition.hasExportFilterDefinition()) {
            matchers.add(new FilterMatcher(rule.ruleDefinition.getExporterFilter(), filterService));
        }
        return new Classifier(
                matchers.toArray(new Matcher[matchers.size()]),
                new Result(matchedAspects, rule.ruleDefinition.getName()),
                rule.ruleDefinition.getPosition()
        );
    }

    public final Matcher[] matchers;
    public final Result result;
    public final int position;

    public Classifier(Matcher[] matchers, Result result, int position) {
        this.matchers = matchers;
        this.result = result;
        this.position = position;
    }

    public Result classify(ClassificationRequest request) {
        for (var m : matchers) {
            if (!m.matches(request)) {
                return null;
            }
        }
        return result;
    }

    @Override
    public int compareTo(Classifier o) {
        return position < o.position ? -1 : position > o.position ? 1 : 0;
    }

    @Override
    public String toString() {
        return "Classifier{" +
               "result='" + result + '\'' +
               ", position=" + position +
               '}';
    }

    public static class Result {
        // used to break ties in case that several classifiers with the same priority match
        public final int matchedAspects;
        public final String name;
        public Result(int matchedAspects, String name) {
            this.matchedAspects = matchedAspects;
            this.name = name;
        }
        @Override
        public String toString() {
            return "Result{" +
                   "matchedAspects=" + matchedAspects +
                   ", name='" + name + '\'' +
                   '}';
        }
    }
}
