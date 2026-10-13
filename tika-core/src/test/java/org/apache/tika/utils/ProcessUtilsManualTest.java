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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * IFT3913 tache 2 : tests ecrits a la main pour {@link ProcessUtils}.
 * <p>
 * Contrairement aux tests generes par ChatUniTest (qui moquent ProcessBuilder et Process),
 * ces tests lancent de vrais processus {@code sh}. Ils sont donc desactives sous Windows.
 * Chaque test cible des mutants PIT vivants apres les tests originaux et generes :
 * voir la section "Tests manuels" du README.md a la racine du depot.
 */
@DisabledOnOs(OS.WINDOWS)
public class ProcessUtilsManualTest {

    private static final long LONG_TIMEOUT_MILLIS = 20_000;
    private static final long SHORT_TIMEOUT_MILLIS = 300;
    /** Code de sortie observe en Java pour un processus tue par SIGKILL (128 + 9). */
    private static final int SIGKILL_EXIT_VALUE = 137;

    private static ProcessBuilder sh(String script) {
        return new ProcessBuilder("sh", "-c", script);
    }

    // ------------------------------------------------------------------
    // execute(ProcessBuilder, long, int, int) : stdout et stderr en memoire
    // ------------------------------------------------------------------

    @Test
    public void executeCapturesStdoutStderrAndExitValue() throws IOException {
        FileProcessResult result = ProcessUtils.execute(
                sh("echo hello; echo oops 1>&2; exit 3"), LONG_TIMEOUT_MILLIS, 1000, 1000);

        assertEquals("hello", result.getStdout());
        assertEquals("oops", result.getStderr());
        assertEquals(3, result.getExitValue());
        assertFalse(result.isTimeout());
        assertEquals(5, result.getStdoutLength());
        assertEquals(4, result.getStderrLength());
        assertFalse(result.isStdoutTruncated());
        assertFalse(result.isStderrTruncated());
        // temps ecoule = (maintenant - debut) : borne haute qui detecte un '-' devenu '+'
        assertTrue(result.getProcessTimeMillis() >= 0);
        assertTrue(result.getProcessTimeMillis() < LONG_TIMEOUT_MILLIS);
    }

    @Test
    public void executeTruncatesStdoutAndStderrAtTheirOwnLimits() throws IOException {
        FileProcessResult result = ProcessUtils.execute(
                sh("printf abcdefghij; printf klmnopqrst 1>&2"), LONG_TIMEOUT_MILLIS, 5, 7);

        assertEquals("abcde", result.getStdout());
        assertEquals("klmnopq", result.getStderr());
        assertTrue(result.isStdoutTruncated());
        assertTrue(result.isStderrTruncated());
    }

    @Test
    public void executeKillsProcessThatExceedsTimeout() throws IOException {
        FileProcessResult result = ProcessUtils.execute(
                sh("sleep 30"), SHORT_TIMEOUT_MILLIS, 1000, 1000);

        assertTrue(result.isTimeout());
        // le processus a ete detruit (SIGKILL) et son code de sortie recupere
        assertEquals(SIGKILL_EXIT_VALUE, result.getExitValue());
        assertTrue(result.getProcessTimeMillis() >= SHORT_TIMEOUT_MILLIS);
        assertTrue(result.getProcessTimeMillis() < LONG_TIMEOUT_MILLIS);
    }

    @Test
    public void executeKeepsOutputProducedBeforeTimeout() throws IOException {
        FileProcessResult result = ProcessUtils.execute(
                sh("echo before; echo before-err 1>&2; sleep 30"),
                SHORT_TIMEOUT_MILLIS, 1000, 1000);

        assertTrue(result.isTimeout());
        assertEquals("before", result.getStdout());
        assertEquals("before-err", result.getStderr());
    }

    @Test
    public void executeReportsInterruption() throws IOException {
        Thread.currentThread().interrupt();
        try {
            FileProcessResult result = ProcessUtils.execute(
                    sh("sleep 30"), LONG_TIMEOUT_MILLIS, 1000, 1000);

            assertEquals(-1000, result.getExitValue());
            assertTrue(result.isTimeout());
            assertEquals(-1, result.getProcessTimeMillis());
        } finally {
            //nettoyer le drapeau d'interruption pour ne pas contaminer les autres tests
            Thread.interrupted();
        }
    }

    // ------------------------------------------------------------------
    // execute(ProcessBuilder, long, Path, int) : stdout redirige vers un fichier
    // ------------------------------------------------------------------

    @Test
    public void executeRedirectWritesStdoutToFileAndCreatesMissingDirectories(
            @TempDir Path tempDir) throws IOException {
        Path redirect = tempDir.resolve("does").resolve("not").resolve("exist").resolve("out.txt");

        FileProcessResult result = ProcessUtils.execute(
                sh("echo hello; echo oops 1>&2"), LONG_TIMEOUT_MILLIS, redirect, 1000);

        assertEquals("hello\n", new String(Files.readAllBytes(redirect), StandardCharsets.UTF_8));
        assertEquals(6, result.getStdoutLength());
        assertEquals("", result.getStdout());
        assertEquals("oops", result.getStderr());
        assertEquals(4, result.getStderrLength());
        assertEquals(0, result.getExitValue());
        assertFalse(result.isTimeout());
        assertFalse(result.isStdoutTruncated());
        assertFalse(result.isStderrTruncated());
        assertTrue(result.getProcessTimeMillis() >= 0);
        assertTrue(result.getProcessTimeMillis() < LONG_TIMEOUT_MILLIS);
    }

    @Test
    public void executeRedirectWithExistingDirectoryAndNonZeroExit(
            @TempDir Path tempDir) throws IOException {
        Path redirect = tempDir.resolve("out.txt");

        FileProcessResult result = ProcessUtils.execute(
                sh("printf abc; exit 2"), LONG_TIMEOUT_MILLIS, redirect, 1000);

        assertEquals("abc", new String(Files.readAllBytes(redirect), StandardCharsets.UTF_8));
        assertEquals(3, result.getStdoutLength());
        assertEquals(2, result.getExitValue());
        assertFalse(result.isTimeout());
    }

    @Test
    public void executeRedirectTruncatesStderrOnly(@TempDir Path tempDir) throws IOException {
        FileProcessResult result = ProcessUtils.execute(
                sh("printf klmnopqrst 1>&2"), LONG_TIMEOUT_MILLIS, tempDir.resolve("out.txt"), 4);

        assertEquals("klmn", result.getStderr());
        assertTrue(result.isStderrTruncated());
        assertFalse(result.isStdoutTruncated());
    }

    @Test
    public void executeRedirectTimeoutLeavesExitValueUnset(@TempDir Path tempDir)
            throws IOException {
        FileProcessResult result = ProcessUtils.execute(
                sh("echo partial; sleep 30"), SHORT_TIMEOUT_MILLIS,
                tempDir.resolve("out.txt"), 1000);

        assertTrue(result.isTimeout());
        // contrairement a l'autre surcharge, aucun code de sortie n'est recupere apres le kill
        assertEquals(-1, result.getExitValue());
        assertTrue(result.getProcessTimeMillis() >= SHORT_TIMEOUT_MILLIS);
        assertTrue(result.getProcessTimeMillis() < LONG_TIMEOUT_MILLIS);
        assertEquals("partial\n", new String(
                Files.readAllBytes(tempDir.resolve("out.txt")), StandardCharsets.UTF_8));
    }

    @Test
    public void executeRedirectReportsInterruption(@TempDir Path tempDir) throws IOException {
        Thread.currentThread().interrupt();
        try {
            FileProcessResult result = ProcessUtils.execute(
                    sh("sleep 30"), LONG_TIMEOUT_MILLIS, tempDir.resolve("out.txt"), 1000);

            assertEquals(-1000, result.getExitValue());
            assertTrue(result.isTimeout());
            assertEquals(-1, result.getProcessTimeMillis());
        } finally {
            Thread.interrupted();
        }
    }

    // ------------------------------------------------------------------
    // checkCommand
    // ------------------------------------------------------------------

    @Test
    public void checkCommandReturnsTrueWhenCommandSucceeds() {
        assertTrue(ProcessUtils.checkCommand("true"));
        assertTrue(ProcessUtils.checkCommand(new String[]{"sh", "-c", "exit 0"}));
    }

    @Test
    public void checkCommandOnlyTreats127AsErrorByDefault() {
        //127 = "command not found" : seule valeur d'erreur par defaut
        assertFalse(ProcessUtils.checkCommand(new String[]{"sh", "-c", "exit 127"}));
        //1 n'est pas dans la liste par defaut : la commande est consideree comme disponible
        assertTrue(ProcessUtils.checkCommand(new String[]{"sh", "-c", "exit 1"}));
        assertTrue(ProcessUtils.checkCommand("false"));
    }

    @Test
    public void checkCommandUsesGivenErrorValuesInsteadOfTheDefault() {
        String[] exit1 = {"sh", "-c", "exit 1"};
        assertFalse(ProcessUtils.checkCommand(exit1, 1));
        assertFalse(ProcessUtils.checkCommand(exit1, 5, 1, 9));
        assertTrue(ProcessUtils.checkCommand(exit1, 2, 3));
        //la valeur par defaut (127) ne s'applique plus des qu'on fournit ses propres valeurs
        assertTrue(ProcessUtils.checkCommand(new String[]{"sh", "-c", "exit 127"}, 1));
        assertFalse(ProcessUtils.checkCommand("false", 1));
    }

    @Test
    public void checkCommandReturnsFalseWhenCommandCannotBeStarted() {
        //IOException au lancement (executable introuvable) => false et non une exception
        assertFalse(ProcessUtils.checkCommand("this-command-does-not-exist-ift3913"));
        assertFalse(ProcessUtils.checkCommand(
                new String[]{"this-command-does-not-exist-ift3913", "--version"}));
    }

    @Test
    public void checkCommandDrainsLargeStdoutSoTheProcessCanFinish() {
        //300 Ko > tampon d'un pipe (64 Ko) : sans StreamGobbler sur stdout le processus bloque
        assertTrue(ProcessUtils.checkCommand(new String[]{"sh", "-c", "head -c 300000 /dev/zero"}));
    }

    @Test
    public void checkCommandDrainsLargeStderrSoTheProcessCanFinish() {
        assertTrue(ProcessUtils.checkCommand(
                new String[]{"sh", "-c", "head -c 300000 /dev/zero 1>&2"}));
    }
}
