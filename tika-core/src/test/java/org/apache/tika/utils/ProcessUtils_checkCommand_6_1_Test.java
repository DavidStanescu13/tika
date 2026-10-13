// IFT3913 - tache 2 : fichier genere par ChatUniTest (qwen2.5-coder:7b via Ollama), conserve tel quel.
// Seules modifications : 7 test(s) marque(s) @Disabled ou conditionne(s) a la plateforme
// apres execution dans GitHub Actions (Linux + Windows). Voir README, section 4.
package org.apache.tika.utils;

import org.apache.tika.utils.ProcessUtils;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_checkCommand_6_1_Test {

    @InjectMocks
    private ProcessUtils processUtils;

    @Mock
    private Process mockProcess;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Disabled("IFT3913 : le mock de Process n'est jamais utilise par checkCommand (Runtime.exec reel) -> verify echoue (WantedButNotInvoked)")

    @Test
    public void testCheckCommandWithNoErrorValue() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
        boolean result = processUtils.checkCommand(checkCmd);
        assertTrue(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
        verify(mockProcess, times(1)).exitValue();
    }

    @Disabled("IFT3913 : le mock de Process n'est jamais utilise par checkCommand (Runtime.exec reel) -> verify echoue (WantedButNotInvoked)")

    @Test
    public void testCheckCommandWithErrorValue() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        int[] errorValue = { 127 };
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(127);
        boolean result = processUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
        verify(mockProcess, times(1)).exitValue();
    }

    @Disabled("IFT3913 : oracle faux : le mock de Process n'est jamais injecte dans la methode statique ; 'echo test' s'execute reellement et renvoie true")

    @Test
    public void testCheckCommandWithTimeoutException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(false);
        boolean result = processUtils.checkCommand(checkCmd);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
    }

    @Disabled("IFT3913 : Mockito : IOException (checked) n'est pas declaree par Process.waitFor -> MockitoException")

    @Test
    public void testCheckCommandWithIOException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        doThrow(new IOException()).when(mockProcess).waitFor(60000, TimeUnit.MILLISECONDS);
        boolean result = processUtils.checkCommand(checkCmd);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
    }

    @Disabled("IFT3913 : oracle faux : le mock de Process n'est jamais injecte dans la methode statique ; 'echo test' s'execute reellement et renvoie true")

    @Test
    public void testCheckCommandWithInterruptedException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        doThrow(new InterruptedException()).when(mockProcess).waitFor(60000, TimeUnit.MILLISECONDS);
        boolean result = processUtils.checkCommand(checkCmd);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
    }

    @Disabled("IFT3913 : oracle faux : le mock de Process n'est jamais injecte dans la methode statique ; 'echo test' s'execute reellement et renvoie true")

    @Test
    public void testCheckCommandWithSecurityException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        doThrow(new SecurityException()).when(mockProcess).waitFor(60000, TimeUnit.MILLISECONDS);
        assertThrows(SecurityException.class, () -> processUtils.checkCommand(checkCmd));
    }

    @Disabled("IFT3913 : le mock de Process n'est jamais utilise par checkCommand (Runtime.exec reel) -> verify echoue (WantedButNotInvoked)")

    @Test
    public void testCheckCommandWithError() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(1);
        boolean result = processUtils.checkCommand(checkCmd);
        assertTrue(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
        verify(mockProcess, times(1)).exitValue();
    }
}
