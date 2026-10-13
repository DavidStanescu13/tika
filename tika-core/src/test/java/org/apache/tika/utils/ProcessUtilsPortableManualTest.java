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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * IFT3913 - Tache 2 : tests ecrits a la main pour {@link ProcessUtils}.
 *
 * <p>Chaque test est documente dans le README a la racine du depot
 * (nom, intention, motivation des donnees, oracle). Les processus lances
 * sont des JVM executant {@link Helper}, ce qui rend les tests independants
 * des commandes disponibles sur la machine (sh, echo, sleep...).</p>
 */
public class ProcessUtilsPortableManualTest {

    /**
     * Petit programme lance comme sous-processus par les tests.
     * <pre>
     *   Helper emit &lt;exitCode&gt; &lt;nbLignesStdout&gt; &lt;nbLignesStderr&gt;
     *       ecrit "out-i" sur stdout, "err-i" sur stderr, puis quitte avec exitCode
     *   Helper sleep &lt;millis&gt;
     *       ecrit "sleeping" sur stderr, dort puis quitte avec 0
     *   Helper flood &lt;nbLignes&gt;
     *       ecrit nbLignes lignes de 100 caracteres sur stdout ET sur stderr, quitte avec 0
     * </pre>
     */
    public static final class Helper {
        public static void main(String[] args) throws Exception {
            if ("sleep".equals(args[0])) {
                System.err.println("sleeping");
                System.err.flush();
                Thread.sleep(Long.parseLong(args[1]));
                System.exit(0);
            }
            if ("flood".equals(args[0])) {
                int n = Integer.parseInt(args[1]);
                String line = "x".repeat(100);
                for (int i = 0; i < n; i++) {
                    System.out.println(line);
                    System.err.println(line);
                }
                System.out.flush();
                System.err.flush();
                System.exit(0);
            }
            int exit = Integer.parseInt(args[1]);
            int nOut = Integer.parseInt(args[2]);
            int nErr = Integer.parseInt(args[3]);
            for (int i = 0; i < nOut; i++) {
                System.out.println("out-" + i);
            }
            for (int i = 0; i < nErr; i++) {
                System.err.println("err-" + i);
            }
            System.out.flush();
            System.err.flush();
            System.exit(exit);
        }
    }

    private static String[] helperCommand(String... helperArgs) {
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        List<String> cmd = new ArrayList<>(Arrays.asList(javaBin, "-cp",
                System.getProperty("java.class.path"), Helper.class.getName()));
        cmd.addAll(Arrays.asList(helperArgs));
        return cmd.toArray(new String[0]);
    }

    private static ProcessBuilder helper(String... helperArgs) {
        return new ProcessBuilder(helperCommand(helperArgs));
    }

    // ------------------------------------------------------------------
    // execute(ProcessBuilder, timeout, maxStdout, maxStderr)
    // ------------------------------------------------------------------

    @Test
    public void testExecuteCapturesStdoutStderrAndExitValue() throws IOException {
        FileProcessResult r = ProcessUtils.execute(helper("emit", "3", "2", "1"), 30000, 10000, 10000);

        assertEquals(3, r.getExitValue());
        assertFalse(r.isTimeout());
        assertEquals("out-0\nout-1", r.getStdout());
        assertEquals("err-0", r.getStderr());
        assertEquals(10, r.getStdoutLength()); // 2 lignes de 5 caracteres, sans les \n
        assertEquals(5, r.getStderrLength());
        assertFalse(r.isStdoutTruncated());
        assertFalse(r.isStderrTruncated());
        assertTrue(r.getProcessTimeMillis() >= 0, "le temps ecoule doit etre mesure");
        assertTrue(r.getProcessTimeMillis() < 30000, "le temps ecoule est une duree, pas un horodatage");
    }

    @Test
    public void testExecuteTruncatesStdoutAtMaxBuffer() throws IOException {
        // 3 lignes de 5 caracteres, tampon de 7 : "out-0" entre, "out-1" est coupe a "ou", "out-2" est ignoree
        FileProcessResult r = ProcessUtils.execute(helper("emit", "0", "3", "0"), 30000, 7, 10000);

        assertEquals(0, r.getExitValue());
        assertEquals("out-0\nou", r.getStdout());
        assertTrue(r.isStdoutTruncated());
        assertEquals(15, r.getStdoutLength(), "la longueur totale reelle du flux est conservee");
        assertEquals("", r.getStderr());
        assertFalse(r.isStderrTruncated());
    }

    @Test
    public void testExecuteTimeoutKillsProcess() throws IOException {
        long start = System.currentTimeMillis();
        FileProcessResult r = ProcessUtils.execute(helper("sleep", "20000"), 500, 1000, 1000);
        long wall = System.currentTimeMillis() - start;

        assertTrue(r.isTimeout(), "le resultat doit signaler le timeout");
        assertTrue(wall < 10000, "le processus doit avoir ete tue bien avant la fin de son sleep (" + wall + " ms)");
        assertTrue(r.getProcessTimeMillis() >= 400, "processTimeMillis doit refleter l'attente jusqu'au timeout");
        assertTrue(r.getProcessTimeMillis() < 10000, "processTimeMillis est une duree, pas un horodatage");
        assertEquals("sleeping", r.getStderr(), "ce qui a ete ecrit avant le timeout est conserve");
        // destroyForcibly() a ete appele puis waitFor(500ms) : le processus est termine et
        // son code de sortie (non nul) a pu etre lu ; -1 signifierait qu'il tourne encore.
        assertNotEquals(-1, r.getExitValue());
        assertNotEquals(0, r.getExitValue());
    }

    // ------------------------------------------------------------------
    // execute(ProcessBuilder, timeout, Path stdoutRedirect, maxStderr)
    // ------------------------------------------------------------------

    @Test
    public void testExecuteRedirectsStdoutToFileAndCreatesParentDirs(@TempDir Path tmp) throws IOException {
        Path redirect = tmp.resolve("nouveau").resolve("sous-dossier").resolve("stdout.txt");
        assertFalse(Files.exists(redirect.getParent()), "precondition : le dossier parent n'existe pas encore");

        FileProcessResult r = ProcessUtils.execute(helper("emit", "0", "2", "1"), 30000, redirect, 10000);

        assertTrue(Files.isRegularFile(redirect), "stdout doit avoir ete redirige dans le fichier");
        assertEquals(Arrays.asList("out-0", "out-1"), Files.readAllLines(redirect, StandardCharsets.UTF_8));
        assertEquals("", r.getStdout(), "stdout n'est pas garde en memoire quand il est redirige");
        assertEquals(Files.size(redirect), r.getStdoutLength());
        assertFalse(r.isStdoutTruncated());
        assertEquals("err-0", r.getStderr());
        assertEquals(5, r.getStderrLength());
        assertEquals(0, r.getExitValue());
        assertFalse(r.isTimeout());
        assertTrue(r.getProcessTimeMillis() >= 0);
        assertTrue(r.getProcessTimeMillis() < 30000);
    }

    @Test
    public void testExecuteRedirectTimeout(@TempDir Path tmp) throws IOException {
        Path redirect = tmp.resolve("stdout.txt");
        long start = System.currentTimeMillis();
        FileProcessResult r = ProcessUtils.execute(helper("sleep", "20000"), 500, redirect, 1000);
        long wall = System.currentTimeMillis() - start;

        assertTrue(r.isTimeout());
        assertTrue(wall < 10000, "le processus doit avoir ete tue (" + wall + " ms)");
        // Asymetrie documentee (voir README) : contrairement a execute(..., int, int), cette surcharge
        // ne refait pas de waitFor(500 ms) apres destroyForcibly(), le code de sortie reste donc -1.
        assertEquals(-1, r.getExitValue(), "le code de sortie n'est pas relu apres le timeout dans cette surcharge");
        assertTrue(r.getProcessTimeMillis() >= 400 && r.getProcessTimeMillis() < 10000);
        assertEquals("sleeping", r.getStderr());
        assertEquals("", r.getStdout());
        assertTrue(Files.exists(redirect), "le fichier de redirection est cree meme en cas de timeout");
    }

    @Test
    public void testExecuteRedirectStderrTruncation(@TempDir Path tmp) throws IOException {
        Path redirect = tmp.resolve("stdout.txt");
        // 3 lignes stderr de 5 caracteres, tampon de 7
        FileProcessResult r = ProcessUtils.execute(helper("emit", "0", "0", "3"), 30000, redirect, 7);

        assertEquals("err-0\ner", r.getStderr());
        assertTrue(r.isStderrTruncated());
        assertEquals(15, r.getStderrLength());
        assertEquals(0, r.getExitValue());
    }

    @Test
    public void testExecuteDrainsLargeOutputCompletely() throws IOException {
        // 20 000 lignes de 100 caracteres sur chaque flux (~2 Mo) : bien plus que le tampon du
        // pipe de l'OS (~64 Ko). Sans lecture concurrente des flux le processus se bloquerait ;
        // sans attendre la fin des lecteurs, le resultat serait incomplet.
        int n = 20000;
        FileProcessResult r = ProcessUtils.execute(helper("flood", Integer.toString(n)), 60000, 10_000_000, 10_000_000);

        assertEquals(0, r.getExitValue());
        assertFalse(r.isTimeout());
        assertEquals(100L * n, r.getStdoutLength());
        assertEquals(100L * n, r.getStderrLength());
        assertEquals(101 * n - 1, r.getStdout().length(), "toutes les lignes, separees par \\n");
        assertEquals(101 * n - 1, r.getStderr().length());
        assertFalse(r.isStdoutTruncated());
        assertFalse(r.isStderrTruncated());
    }

    @Test
    public void testExecuteRedirectDrainsLargeStderrCompletely(@TempDir Path tmp) throws IOException {
        int n = 20000;
        Path redirect = tmp.resolve("stdout.txt");
        FileProcessResult r = ProcessUtils.execute(helper("flood", Integer.toString(n)), 60000, redirect, 10_000_000);

        assertEquals(0, r.getExitValue());
        // println ecrit le separateur de ligne de la plateforme (\n ou \r\n sous Windows)
        long expectedFileSize = (100L + System.lineSeparator().length()) * n;
        assertEquals(expectedFileSize, Files.size(redirect), "stdout complet dans le fichier (avec les fins de ligne)");
        assertEquals(expectedFileSize, r.getStdoutLength());
        assertEquals(100L * n, r.getStderrLength());
        assertEquals(101 * n - 1, r.getStderr().length());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Process> processMap() throws ReflectiveOperationException {
        Field f = ProcessUtils.class.getDeclaredField("PROCESS_MAP");
        f.setAccessible(true);
        return (Map<String, Process>) f.get(null);
    }

    @Test
    public void testProcessIsUnregisteredAfterExecute(@TempDir Path tmp) throws Exception {
        // Le registre interne sert au shutdown hook ; un processus termine ne doit pas y rester.
        Map<String, Process> map = processMap();
        int before = map.size();

        ProcessUtils.execute(helper("emit", "0", "1", "0"), 30000, 1000, 1000);
        assertEquals(before, map.size(), "execute(..., int, int) doit liberer son entree");

        ProcessUtils.execute(helper("emit", "0", "1", "0"), 30000, tmp.resolve("out.txt"), 1000);
        assertEquals(before, map.size(), "execute(..., Path, int) doit liberer son entree");

        ProcessUtils.execute(helper("sleep", "20000"), 300, 1000, 1000);
        assertEquals(before, map.size(), "meme apres un timeout, l'entree est liberee");
    }

    // ------------------------------------------------------------------
    // checkCommand
    // ------------------------------------------------------------------

    @Test
    public void testCheckCommandMissingExecutableReturnsFalse() {
        String bogus = "ift3913-commande-inexistante-" + UUID.randomUUID();
        assertFalse(ProcessUtils.checkCommand(bogus));
        assertFalse(ProcessUtils.checkCommand(new String[]{bogus, "--version"}));
    }

    @Test
    public void testCheckCommandStringOverloadSuccess() {
        // "java" sans argument affiche l'aide et sort avec 1 : pas 127, donc "commande disponible"
        String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        assertTrue(ProcessUtils.checkCommand(javaBin));
        assertFalse(ProcessUtils.checkCommand(javaBin, 1), "1 declare comme code d'erreur");
    }

    @Test
    public void testCheckCommandDrainsLargeOutput() {
        // la commande ecrit ~2 Mo sur stdout et stderr : checkCommand doit consommer les flux,
        // sinon le processus se bloque sur le pipe plein et on finit en timeout (false)
        assertTrue(ProcessUtils.checkCommand(helperCommand("flood", "20000")));
    }

    @Test
    public void testCheckCommandDefaultErrorValueIs127() {
        // exit 0 -> succes ; exit 127 -> "command not found" par convention -> echec ; exit 3 -> pas une erreur par defaut
        assertTrue(ProcessUtils.checkCommand(helperCommand("emit", "0", "0", "0")));
        assertFalse(ProcessUtils.checkCommand(helperCommand("emit", "127", "0", "0")));
        assertTrue(ProcessUtils.checkCommand(helperCommand("emit", "3", "0", "0")));
    }

    @Test
    public void testCheckCommandHonoursConfiguredErrorValues() {
        String[] exit3 = helperCommand("emit", "3", "1", "1");
        assertFalse(ProcessUtils.checkCommand(exit3, 3), "3 est declare comme code d'erreur");
        assertFalse(ProcessUtils.checkCommand(exit3, 1, 2, 3), "3 figure dans la liste");
        assertTrue(ProcessUtils.checkCommand(exit3, 4), "3 n'est pas dans la liste");
        assertTrue(ProcessUtils.checkCommand(exit3, 2, 4), "ni 2 ni 4");
        // en passant explicitement une liste, 127 n'est plus implicite
        assertTrue(ProcessUtils.checkCommand(helperCommand("emit", "127", "0", "0"), 1));
    }

    // ------------------------------------------------------------------
    // escapeCommandLine / unescapeCommandLine
    // ------------------------------------------------------------------

    @Test
    public void testEscapeCommandLineNullAndNoSpace() {
        assertNull(ProcessUtils.escapeCommandLine(null));
        String noSpace = "C:\\tools\\app.exe";
        assertSame(noSpace, ProcessUtils.escapeCommandLine(noSpace), "sans espace : renvoye tel quel");
        assertSame(noSpace, ProcessUtils.unescapeCommandLine(noSpace));
    }

    @Test
    public void testEscapeCommandLineOnWindows() {
        assumeTrue(SystemUtils.IS_OS_WINDOWS);
        assertEquals("\"C:\\Program Files\\app.exe\"", ProcessUtils.escapeCommandLine("C:\\Program Files\\app.exe"));
        // deja entre guillemets : pas de double echappement
        assertEquals("\"C:\\Program Files\\app.exe\"", ProcessUtils.escapeCommandLine("\"C:\\Program Files\\app.exe\""));
        assertEquals("C:\\Program Files\\app.exe", ProcessUtils.unescapeCommandLine("\"C:\\Program Files\\app.exe\""));
        // guillemet d'un seul cote : ni echappe ni des-echappe
        assertEquals("\"C:\\Program Files", ProcessUtils.unescapeCommandLine("\"C:\\Program Files"));
    }

    @Test
    public void testEscapeCommandLineIsNoOpOutsideWindows() {
        assumeFalse(SystemUtils.IS_OS_WINDOWS);
        String withSpace = "/opt/my tools/app";
        assertSame(withSpace, ProcessUtils.escapeCommandLine(withSpace));
        assertSame(withSpace, ProcessUtils.unescapeCommandLine(withSpace));
        String quoted = "\"/opt/my tools/app\"";
        assertSame(quoted, ProcessUtils.unescapeCommandLine(quoted));
    }

    @Test
    public void testUnescapeCommandLineNullIsNotHandled() {
        // Comportement actuel documente : contrairement a escapeCommandLine, unescapeCommandLine
        // ne tolere pas null (voir README, section "observations").
        assertThrows(NullPointerException.class, () -> ProcessUtils.unescapeCommandLine(null));
    }
}
