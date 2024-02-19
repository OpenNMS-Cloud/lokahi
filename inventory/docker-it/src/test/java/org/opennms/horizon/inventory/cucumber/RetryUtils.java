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
package org.opennms.horizon.inventory.cucumber;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryUtils {

    private static final Logger DEFAULT_LOGGER = LoggerFactory.getLogger(RetryUtils.class);

    private Logger log = DEFAULT_LOGGER;

    /**
     * Retry the given operation up until the given timeout.  WARNING: the operation is executed synchronously, and
     * hence may well exceed the specified timeout.
     *
     * @param operation the operation to execute on each iteration
     * @param completionPredicate predicate that indicates whether the operation's result completes the retries; when
     *                           false, retry logic will be applied
     * @param iterationDelay the delay, in milliseconds, between individual iterations
     * @param timeout total time, in milliseconds, before counting the operation as timed-out
     * @param initResult initial value to use for result which will be tested, and potentially returned, when the
     *                  operation throws exceptions, followed ultimately by reaching the timeout
     * @param <T>
     * @return
     */
    public <T> T retry(
            Supplier<T> operation, Predicate<T> completionPredicate, long iterationDelay, long timeout, T initResult)
            throws InterruptedException {

        T result = initResult;

        // Calculate timeout
        long now = System.nanoTime();
        long start = now;
        long end = start + (timeout * 1000000L);

        // Prepare storage for exceptions caught be operation
        Exception[] finalExceptionStore = new Exception[1];

        // Initial Attempt
        result = this.safeRunOnce(operation, result, exc -> finalExceptionStore[0] = exc);
        boolean successful = completionPredicate.test(result);

        // Loop until the operation is successful, or timeout is reached.  Note that a timeout of 0 means no retries
        //  will occur - only the initial attempt.
        while ((!successful) && (now < end)) {
            Thread.sleep(iterationDelay);

            result = this.safeRunOnce(operation, result, exc -> finalExceptionStore[0] = exc);
            successful = completionPredicate.test(result);

            now = System.nanoTime();
        }

        if ((!successful) && (finalExceptionStore[0] != null)) {
            this.log.warn(
                    "RETRY UTIL: operation timed out: final exception (may not be from the final attempt)",
                    finalExceptionStore[0]);

            // Propagate the exception so that it shows up in the report
            throw new RuntimeException(
                    "RETRY UTIL: operation timed out: final exception (may not be from the final attempt)",
                    finalExceptionStore[0]);
        }

        return result;
    }

    private <T> T safeRunOnce(Supplier<T> operation, T defaultResult, Consumer<Exception> onException) {
        T result = defaultResult;
        try {
            result = operation.get();
        } catch (Exception exc) {
            this.log.debug("RETRY util: operation threw exception", exc);
            onException.accept(exc);
        }

        return result;
    }
}
