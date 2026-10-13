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

    @Test
    public void testCheckCommandWithTimeoutException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        when(mockProcess.waitFor(60000, TimeUnit.MILLISECONDS)).thenReturn(false);
        boolean result = processUtils.checkCommand(checkCmd);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCheckCommandWithIOException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        doThrow(new IOException()).when(mockProcess).waitFor(60000, TimeUnit.MILLISECONDS);
        boolean result = processUtils.checkCommand(checkCmd);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCheckCommandWithInterruptedException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        doThrow(new InterruptedException()).when(mockProcess).waitFor(60000, TimeUnit.MILLISECONDS);
        boolean result = processUtils.checkCommand(checkCmd);
        assertFalse(result);
        verify(mockProcess, times(1)).waitFor(60000, TimeUnit.MILLISECONDS);
    }

    @Test
    public void testCheckCommandWithSecurityException() throws IOException, InterruptedException, TimeoutException {
        String[] checkCmd = { "echo", "test" };
        doThrow(new SecurityException()).when(mockProcess).waitFor(60000, TimeUnit.MILLISECONDS);
        assertThrows(SecurityException.class, () -> processUtils.checkCommand(checkCmd));
    }

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
