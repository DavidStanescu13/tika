package org.apache.tika.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.tika.utils.FileProcessResult;
import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StreamGobbler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_execute_5_4_Test {

    @Mock
    private ProcessBuilder processBuilder;

    @Mock
    private Process process;

    @Mock
    private StreamGobbler streamGobbler;

    @InjectMocks
    private ProcessUtils processUtils;

    private Path stdoutRedirect;

    @BeforeEach
    public void setUp() throws IOException {
        stdoutRedirect = Files.createTempFile("test", ".txt");
    }

    @Test
    public void testExecute_Success() throws IOException, InterruptedException {
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        when(streamGobbler.getStreamLength()).thenReturn(100L);
        when(streamGobbler.getIsTruncated()).thenReturn(false);
        when(streamGobbler.getLines()).thenReturn(java.util.Arrays.asList("line1", "line2"));
        FileProcessResult result = processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000);
        assertEquals(1000, result.processTimeMillis);
        assertEquals(100, result.stderrLength);
        assertEquals(100L, result.stdoutLength);
        assertFalse(result.isTimeout);
        assertEquals(0, result.exitValue);
        assertEquals("line1\nline2", result.stderr);
        assertFalse(result.stdoutTruncated);
        assertFalse(result.stderrTruncated);
    }

    @Test
    public void testExecute_Timeout() throws IOException, InterruptedException {
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(process.exitValue()).thenReturn(0);
        when(streamGobbler.getStreamLength()).thenReturn(100L);
        when(streamGobbler.getIsTruncated()).thenReturn(false);
        when(streamGobbler.getLines()).thenReturn(java.util.Arrays.asList("line1", "line2"));
        FileProcessResult result = processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000);
        assertEquals(1000, result.processTimeMillis);
        assertEquals(100, result.stderrLength);
        assertEquals(100L, result.stdoutLength);
        assertTrue(result.isTimeout);
        assertEquals(0, result.exitValue);
        assertEquals("line1\nline2", result.stderr);
        assertFalse(result.stdoutTruncated);
        assertFalse(result.stderrTruncated);
    }

    @Test
    public void testExecute_IOException() throws IOException {
        when(processBuilder.start()).thenThrow(new IOException("Test exception"));
        assertThrows(IOException.class, () -> processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000));
    }

    @Test
    public void testExecute_InterruptedException() throws IOException, InterruptedException {
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("Test exception"));
        FileProcessResult result = processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000);
        assertEquals(1000, result.processTimeMillis);
        assertEquals(0, result.stderrLength);
        assertEquals(0L, result.stdoutLength);
        assertTrue(result.isTimeout);
        assertEquals(-1000, result.exitValue);
        assertEquals("", result.stderr);
        assertFalse(result.stdoutTruncated);
        assertFalse(result.stderrTruncated);
    }
}
