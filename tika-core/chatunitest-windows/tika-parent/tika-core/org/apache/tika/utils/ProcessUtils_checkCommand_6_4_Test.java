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

    @Test
    public void testCheckCommandWithIOException() throws IOException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 127 };
        when(Runtime.getRuntime().exec(checkCmd)).thenThrow(new IOException());
        boolean result = ProcessUtils.checkCommand(checkCmd, errorValue);
        assertFalse(result);
        verify(mockProcess, never()).destroyForcibly();
    }

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

    @Test
    public void testCheckCommandWithError() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "ls", "-l" };
        int[] errorValue = { 0 };
        when(Runtime.getRuntime().exec(checkCmd)).thenReturn(mockProcess);
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(mockProcess.exitValue()).thenReturn(0);
    }
}
