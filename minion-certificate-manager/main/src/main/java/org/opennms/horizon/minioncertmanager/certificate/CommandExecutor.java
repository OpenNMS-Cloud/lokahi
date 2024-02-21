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
package org.opennms.horizon.minioncertmanager.certificate;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(CommandExecutor.class);
    private static final int TIMEOUT = 10000;
    public static final int STD_TIMEOUT = 30000;

    // CLASS METHODS MAKE UNIT TESTING VERY HARD - not worth it
    // private CommandExecutor() {
    //     throw new IllegalStateException("Utility class");
    // }

    public void executeCommand(String command, String... params) throws IOException, InterruptedException {
        executeCommand(command, null, params);
    }

    public void executeCommand(String command, File directory, String... params)
            throws IOException, InterruptedException {
        executeCommand(command, directory, Map.of(), params);
    }

    public void executeCommand(String command, File directory, Map<String, String> env, String... params)
            throws IOException, InterruptedException {
        String commandToExecute = String.format(command, params);
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", commandToExecute).directory(directory);
        processBuilder.environment().putAll(env);
        Process process = null;
        try {
            process = processBuilder.start();

            // Create CompletableFutures for reading stdout and stderr
            Process finalProcess = process;
            CompletableFuture<Void> stdoutFuture = logStdoutFuture(finalProcess);
            CompletableFuture<Void> stderrFuture = logStderrFuture(finalProcess);

            // Wait for the process to complete with a timeout
            boolean completed = process.waitFor(TIMEOUT, TimeUnit.MILLISECONDS);
            if (!completed) {
                LOG.error("Command timed out: {}. Timeout: {} milliseconds", commandToExecute, TIMEOUT);
                // Cancel both CompletableFutures
                CompletableFuture.allOf(stdoutFuture, stderrFuture).cancel(true);
            } else {
                waitForStdsToComplete(stdoutFuture, stderrFuture);
            }

            // Check the exit value of the process
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                LOG.error("Command exited with error code: {}. Command: {}", exitValue, commandToExecute);
            }
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private CompletableFuture<Void> logStderrFuture(Process finalProcess) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader stderrReader =
                    new BufferedReader(new InputStreamReader(finalProcess.getErrorStream()))) {
                String stderrLine;
                while ((stderrLine = stderrReader.readLine()) != null) {
                    LOG.error("{} {}", Thread.currentThread().getName(), stderrLine);
                }
            } catch (IOException e) {
                LOG.error("{} Error reading stderr: {}", Thread.currentThread().getName(), e.getMessage(), e);
            }
        });
    }

    private CompletableFuture<Void> logStdoutFuture(Process finalProcess) {
        return CompletableFuture.runAsync(() -> {
            try (BufferedReader stdoutReader =
                    new BufferedReader(new InputStreamReader(finalProcess.getInputStream()))) {
                String stdoutLine;
                while ((stdoutLine = stdoutReader.readLine()) != null) {
                    LOG.debug("{} {}", Thread.currentThread().getName(), stdoutLine);
                }
            } catch (IOException e) {
                LOG.error("{} Error reading stdout: {}", Thread.currentThread().getName(), e.getMessage(), e);
            }
        });
    }

    private void waitForStdsToComplete(CompletableFuture<Void> stdoutFuture, CompletableFuture<Void> stderrFuture) {
        // Wait for both CompletableFutures to complete with a timeout
        CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(stdoutFuture, stderrFuture);
        try {
            allOfFuture.get(STD_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            LOG.error("Timeout occurred while waiting for buffer reader to complete: {} milliseconds", STD_TIMEOUT);
            allOfFuture.cancel(true);
        } catch (InterruptedException | ExecutionException e) {
            LOG.error("Error occurred while waiting for buffer reader to complete: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }
    }
}
