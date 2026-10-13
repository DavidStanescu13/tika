/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

/**
 * IFT3913 tache 2 : tests ecrits a la main pour {@link ProcessUtils}, avec {@link Runtime},
 * {@link ProcessBuilder} et {@link Process} simules. Ils ne dependent pas du systeme
 * d'exploitation et couvrent ce que de vrais processus ne permettent pas de tester
 * (delai de 60 s de checkCommand, erreurs {@link Error} de posix_spawn, fuite dans la table
 * interne des processus, threads de lecture des flux).
 * Voir la section "Tests manuels" du README.md a la racine du depot.
 */
public class ProcessUtilsMockedManualTest {

    private static final long TIMEOUT_MILLIS = 10_000;

    @BeforeAll
    public static void loadProcessUtilsBeforeMockingRuntime() {
        //le bloc static de ProcessUtils enregistre un shutdown hook via Runtime.getRuntime() :
        //la classe doit etre initialisee AVANT que Runtime ne soit simule.
        ProcessUtils.escapeCommandLine(null);
    }

    // ------------------------------------------------------------------
    // Outils
    // ------------------------------------------------------------------

    /** Flux qui n'a rien a lire pendant delayMillis, puis fournit data et se termine. */
    private static final class DelayedStream extends InputStream {
        private final byte[] data;
        private final long delayMillis;
        private int pos = -1;

        DelayedStream(String data, long delayMillis) {
            this.data = data.getBytes(StandardCharsets.UTF_8);
            this.delayMillis = delayMillis;
        }

        @Override
        public int read() throws IOException {
            if (pos < 0) {
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException e) {
                    throw new IOException(e);
                }
                pos = 0;
            }
            return pos < data.length ? data[pos++] : -1;
        }
    }

    /** Flux bloque tant qu'on ne l'interrompt pas ; signale l'interruption via un verrou. */
    private static final class BlockedStream extends InputStream {
        final CountDownLatch interrupted = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw new IOException(e);
            }
            return -1;
        }
    }

    private static InputStream fastStream(String content) {
        return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
    }

    private static Process processWith(InputStream stdout, InputStream stderr) {
        Process process = mock(Process.class);
        when(process.getInputStream()).thenReturn(stdout);
        when(process.getErrorStream()).thenReturn(stderr);
        return process;
    }

    private static Process quietProcess() {
        return processWith(new ByteArrayInputStream(new byte[0]),
                new ByteArrayInputStream(new byte[0]));
    }

    private static ProcessBuilder builderStarting(Process process) throws IOException {
        ProcessBuilder pb = mock(ProcessBuilder.class);
        when(pb.start()).thenReturn(process);
        return pb;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Process> processMap() throws ReflectiveOperationException {
        Field f = ProcessUtils.class.getDeclaredField("PROCESS_MAP");
        f.setAccessible(true);
        return (Map<String, Process>) f.get(null);
    }

    private static boolean checkCommandWith(Runtime runtime, String... cmd) {
        try (MockedStatic<Runtime> mocked = mockStatic(Runtime.class)) {
            mocked.when(Runtime::getRuntime).thenReturn(runtime);
            return ProcessUtils.checkCommand(cmd);
        }
    }

    // ------------------------------------------------------------------
    // checkCommand : Runtime simule
    // ------------------------------------------------------------------

    @Test
    public void checkCommandReturnsFalseAndKillsProcessWhenItNeverFinishes() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(false);
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(any(String[].class))).thenReturn(process);

        //le delai (en dur) de 60 s est atteint sans attendre : waitFor est simule
        assertFalse(checkCommandWith(runtime, "never-ends"));
        verify(process, times(1)).destroyForcibly();
    }

    @Test
    public void checkCommandReturnsFalseWhenInterrupted() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(60000, TimeUnit.MILLISECONDS))
                .thenThrow(new InterruptedException("interrupted"));
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(any(String[].class))).thenReturn(process);

        assertFalse(checkCommandWith(runtime, "interrupted"));
        verify(process, times(1)).destroyForcibly();
    }

    @Test
    public void checkCommandUsesExitValueOfTheFinishedProcess() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(any(String[].class))).thenReturn(process);

        when(process.exitValue()).thenReturn(127);
        assertFalse(checkCommandWith(runtime, "cmd"));
        when(process.exitValue()).thenReturn(0);
        assertTrue(checkCommandWith(runtime, "cmd"));
        verify(process, times(2)).destroyForcibly();
    }

    @Test
    public void checkCommandReturnsFalseOnPosixSpawnError() throws Exception {
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(any(String[].class))).thenThrow(new Error("error in posix_spawn"));
        assertFalse(checkCommandWith(runtime, "cmd"));
    }

    @Test
    public void checkCommandReturnsFalseOnUnixProcessError() throws Exception {
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(any(String[].class))).thenThrow(new Error("java.lang.UNIXProcess"));
        assertFalse(checkCommandWith(runtime, "cmd"));
    }

    @Test
    public void checkCommandRethrowsUnrelatedErrors() throws Exception {
        Runtime runtime = mock(Runtime.class);
        Error unrelated = new Error("something else");
        when(runtime.exec(any(String[].class))).thenThrow(unrelated);
        assertSame(unrelated, assertThrows(Error.class, () -> checkCommandWith(runtime, "cmd")));
    }

    @Test
    public void checkCommandRethrowsErrorsWithoutMessage() throws Exception {
        Runtime runtime = mock(Runtime.class);
        Error noMessage = new Error();
        when(runtime.exec(any(String[].class))).thenThrow(noMessage);
        assertSame(noMessage, assertThrows(Error.class, () -> checkCommandWith(runtime, "cmd")));
    }

    @Test
    public void checkCommandRethrowsSecurityException() throws Exception {
        Runtime runtime = mock(Runtime.class);
        SecurityException denied = new SecurityException("denied");
        when(runtime.exec(any(String[].class))).thenThrow(denied);
        assertSame(denied,
                assertThrows(SecurityException.class, () -> checkCommandWith(runtime, "cmd")));
    }

    @Test
    public void checkCommandWaitsForStdoutReaderToFinish() throws Exception {
        assertCheckCommandWaitsForStreams(
                new DelayedStream("late", 400), new ByteArrayInputStream(new byte[0]));
    }

    @Test
    public void checkCommandWaitsForStderrReaderToFinish() throws Exception {
        assertCheckCommandWaitsForStreams(
                new ByteArrayInputStream(new byte[0]), new DelayedStream("late", 400));
    }

    private void assertCheckCommandWaitsForStreams(InputStream stdout, InputStream stderr)
            throws Exception {
        Process process = processWith(stdout, stderr);
        when(process.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        Runtime runtime = mock(Runtime.class);
        when(runtime.exec(any(String[].class))).thenReturn(process);

        long start = System.nanoTime();
        assertTrue(checkCommandWith(runtime, "cmd"));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        //la commande est terminee tout de suite, mais le flux met 400 ms a se fermer :
        //checkCommand doit attendre (join) les threads de lecture avant de rendre la main
        assertTrue(elapsedMillis >= 300, "checkCommand returned after only " + elapsedMillis);
    }

    // ------------------------------------------------------------------
    // execute (stdout en memoire) : Process simule
    // ------------------------------------------------------------------

    @Test
    public void executeWaitsForSlowStdoutReaderOfAFinishedProcess() throws Exception {
        assertExecuteWaitsForReaders(false, true);
    }

    @Test
    public void executeWaitsForSlowStderrReaderOfAFinishedProcess() throws Exception {
        assertExecuteWaitsForReaders(false, false);
    }

    @Test
    public void executeWaitsForSlowStdoutReaderAfterKillingProcessOnTimeout() throws Exception {
        assertExecuteWaitsForReaders(true, true);
    }

    @Test
    public void executeWaitsForSlowStderrReaderAfterKillingProcessOnTimeout() throws Exception {
        assertExecuteWaitsForReaders(true, false);
    }

    /**
     * Un seul des deux flux est lent (500 ms) : chaque join(1000) est ainsi verifie
     * separement. Si les deux flux etaient lents, attendre l'un suffirait a laisser
     * l'autre finir et la suppression d'un join passerait inapercue.
     */
    private void assertExecuteWaitsForReaders(boolean timeout, boolean slowStdout)
            throws Exception {
        InputStream stdout = slowStdout ? new DelayedStream("slow-out", 500) : fastStream("fast-out");
        InputStream stderr = slowStdout ? fastStream("fast-err") : new DelayedStream("slow-err", 500);
        Process process = processWith(stdout, stderr);
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(!timeout);
        when(process.waitFor(500, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(timeout ? 137 : 0);

        FileProcessResult result =
                ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        assertEquals(slowStdout ? "slow-out" : "fast-out", result.getStdout());
        assertEquals(slowStdout ? "fast-err" : "slow-err", result.getStderr());
        assertEquals(timeout, result.isTimeout());
        assertEquals(timeout ? 137 : 0, result.getExitValue());
        //destroyForcibly : une fois dans le finally, plus une fois dans la branche timeout.
        //(un appel de plus en cas de succes trahirait un if (complete) inverse)
        verify(process, times(timeout ? 2 : 1)).destroyForcibly();
    }

    @Test
    public void executeKeepsMinusOneWhenKilledProcessStillReportsRunning() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(process.waitFor(500, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenThrow(new IllegalThreadStateException("still running"));

        FileProcessResult result =
                ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        assertTrue(result.isTimeout());
        assertEquals(-1, result.getExitValue());
    }

    @Test
    public void executeKeepsMinusOneWhenKilledProcessDoesNotStopInTime() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(process.waitFor(500, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(process.exitValue()).thenReturn(137);

        FileProcessResult result =
                ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        assertTrue(result.isTimeout());
        assertEquals(-1, result.getExitValue());
    }

    @Test
    public void executeInterruptsStdoutReaderThatIsStillBlocked() throws Exception {
        BlockedStream stdout = new BlockedStream();
        Process process = processWith(stdout, new ByteArrayInputStream(new byte[0]));
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(true);

        ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        //join(1000) abandonne apres 1 s ; le finally doit alors interrompre le thread de lecture
        assertTrue(stdout.interrupted.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void executeInterruptsStderrReaderThatIsStillBlocked() throws Exception {
        BlockedStream stderr = new BlockedStream();
        Process process = processWith(new ByteArrayInputStream(new byte[0]), stderr);
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(true);

        ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        assertTrue(stderr.interrupted.await(3, TimeUnit.SECONDS));
    }

    @Test
    public void executeDestroysProcessAndForgetsItWhenInterrupted() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                .thenThrow(new InterruptedException("interrupted"));
        int sizeBefore = processMap().size();

        FileProcessResult result =
                ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        assertEquals(-1000, result.getExitValue());
        verify(process, times(1)).destroyForcibly();
        assertEquals(sizeBefore, processMap().size());
    }

    @Test
    public void executeDoesNotLeakProcessesInTheInternalMap() throws Exception {
        Process process = quietProcess();
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(true);
        int sizeBefore = processMap().size();

        ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, 1000, 1000);

        assertEquals(sizeBefore, processMap().size());
    }

    @Test
    public void executeDoesNotRegisterOrLeakAProcessThatCannotStart() throws Exception {
        ProcessBuilder pb = mock(ProcessBuilder.class);
        when(pb.start()).thenThrow(new IOException("cannot start"));
        int sizeBefore = processMap().size();

        assertThrows(IOException.class, () -> ProcessUtils.execute(pb, TIMEOUT_MILLIS, 1000, 1000));
        assertEquals(sizeBefore, processMap().size());
    }

    @Test
    public void concurrentExecutionsAreRegisteredUnderDistinctIds() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        int sizeBefore = processMap().size();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Thread[] threads = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
            Process process = quietProcess();
            when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenAnswer(invocation -> {
                release.await();
                return true;
            });
            ProcessBuilder pb = builderStarting(process);
            threads[i] = new Thread(() -> {
                try {
                    ProcessUtils.execute(pb, TIMEOUT_MILLIS, 1000, 1000);
                } catch (IOException e) {
                    failure.set(e);
                }
            });
            threads[i].start();
        }
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (processMap().size() < sizeBefore + 2 && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            //deux processus en cours => deux entrees : chacun a son propre identifiant
            assertEquals(sizeBefore + 2, processMap().size());
        } finally {
            release.countDown();
            for (Thread t : threads) {
                t.join(5000);
            }
        }
        assertEquals(null, failure.get());
        assertEquals(sizeBefore, processMap().size());
    }

    // ------------------------------------------------------------------
    // execute (stdout redirige vers un fichier) : Process simule
    // ------------------------------------------------------------------

    @Test
    public void executeRedirectWaitsForStderrReaderOfAFinishedProcess(@TempDir Path tempDir)
            throws Exception {
        Process process = processWith(new ByteArrayInputStream(new byte[0]),
                new DelayedStream("late-err", 300));
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        Path redirect = tempDir.resolve("out.txt");
        Files.write(redirect, "x".getBytes(StandardCharsets.UTF_8));

        FileProcessResult result = ProcessUtils.execute(
                builderStarting(process), TIMEOUT_MILLIS, redirect, 1000);

        assertEquals("late-err", result.getStderr());
        assertEquals(1, result.getStdoutLength());
        assertFalse(result.isTimeout());
        verify(process, times(1)).destroyForcibly();
    }

    @Test
    public void executeRedirectWaitsForStderrReaderAfterKillingProcessOnTimeout(
            @TempDir Path tempDir) throws Exception {
        Process process = processWith(new ByteArrayInputStream(new byte[0]),
                new DelayedStream("late-err", 300));
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(false);

        Path redirect = Files.createFile(tempDir.resolve("out.txt"));

        FileProcessResult result = ProcessUtils.execute(
                builderStarting(process), TIMEOUT_MILLIS, redirect, 1000);

        assertTrue(result.isTimeout());
        assertEquals("late-err", result.getStderr());
        verify(process, times(2)).destroyForcibly();
    }

    @Test
    public void executeRedirectDestroysProcessAndForgetsItWhenInterrupted(@TempDir Path tempDir)
            throws Exception {
        Process process = quietProcess();
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS))
                .thenThrow(new InterruptedException("interrupted"));
        int sizeBefore = processMap().size();

        Path redirect = Files.createFile(tempDir.resolve("out.txt"));

        FileProcessResult result = ProcessUtils.execute(
                builderStarting(process), TIMEOUT_MILLIS, redirect, 1000);

        assertEquals(-1000, result.getExitValue());
        verify(process, times(1)).destroyForcibly();
        assertEquals(sizeBefore, processMap().size());
    }

    @Test
    public void executeRedirectDoesNotLeakProcessesInTheInternalMap(@TempDir Path tempDir)
            throws Exception {
        Process process = quietProcess();
        when(process.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)).thenReturn(true);
        int sizeBefore = processMap().size();

        Path redirect = Files.createFile(tempDir.resolve("o"));

        ProcessUtils.execute(builderStarting(process), TIMEOUT_MILLIS, redirect, 1000);

        assertEquals(sizeBefore, processMap().size());
    }
}
