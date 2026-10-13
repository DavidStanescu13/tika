// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 8 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

import org.apache.tika.utils.ProcessUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_checkCommand_6_4_Test {

    @Mock
    private Process mockProcess;

    @Mock
    private StreamGobbler mockOutGobbler;

    @Mock
    private StreamGobbler mockErrGobbler;

    @Disabled("IFT3913 : Mockito : when() sur un appel qui n'est pas celui d'un mock (Runtime/Process reels) -> MissingMethodInvocationException")

    @Test
    public void testCheckCommandWithValidCommand() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertTrue(result);
        verify(mockProcess, times(1)).destroyForcibly();
    }

    @Disabled("IFT3913 : oracle faux : attend une IOException alors que checkCommand l'attrape et renvoie false")

    @Test
    public void testCheckCommandWithInvalidCommand() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "invalid_command" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(127);
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, times(1)).destroyForcibly();
    }

    @Disabled("IFT3913 : Mockito : when() sur un appel qui n'est pas celui d'un mock (Runtime/Process reels) -> MissingMethodInvocationException")

    @Test
    public void testCheckCommandWithTimeout() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "sleep", "60" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(false);
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, times(1)).destroyForcibly();
    }

    @Disabled("IFT3913 : Mockito : when() sur un appel qui n'est pas celui d'un mock (Runtime/Process reels) -> MissingMethodInvocationException")

    @Test
    public void testCheckCommandWithIOException() throws IOException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenThrow(new IOException());
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, never()).destroyForcibly();
    }

    @Disabled("IFT3913 : Mockito : when() sur un appel qui n'est pas celui d'un mock (Runtime/Process reels) -> MissingMethodInvocationException")

    @Test
    public void testCheckCommandWithInterruptedException() throws IOException, InterruptedException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, times(1)).destroyForcibly();
    }

    @Disabled("IFT3913 : Mockito : when() sur un appel qui n'est pas celui d'un mock (Runtime/Process reels) -> MissingMethodInvocationException")

    @Test
    public void testCheckCommandWithTimeoutException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "sleep", "60" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenThrow(new TimeoutException());
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, times(1)).destroyForcibly();
    }

    @Disabled("IFT3913 : Mockito : when() sur un appel qui n'est pas celui d'un mock (Runtime/Process reels) -> MissingMethodInvocationException")

    @Test
    public void testCheckCommandWithSecurityException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
        doThrow(new SecurityException()).when(mockProcess).destroyForcibly();
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertTrue(result);
        verify(mockProcess, times(1)).destroyForcibly();
    }

    @Disabled("IFT3913 : Mockito strict : stubbings inutiles (le mock n'est jamais appele) -> UnnecessaryStubbingException")

    @Test
    public void testCheckCommandWithError() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 0 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
    }
}
