package org.apache.tika.utils;

import org.apache.tika.utils.FileProcessResult;
import org.apache.tika.utils.ProcessUtils;
import org.apache.tika.utils.StreamGobbler;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_execute_5_3_Test {

    @Mock
    private StreamGobbler errGobbler;

    @Mock
    private Process process;

    @InjectMocks
    private ProcessUtils processUtils;

    private Path stdoutRedirect;

    private ProcessBuilder pb;

    @BeforeEach
    public void setUp() throws IOException {
        stdoutRedirect = Files.createTempFile("test", ".txt");
        pb = new ProcessBuilder("echo", "test");
    }

    @Test
    public void testExecute() throws IOException, InterruptedException {
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        when(errGobbler.getStreamLength()).thenReturn(0L);
        when(errGobbler.getIsTruncated()).thenReturn(false);
        when(errGobbler.getLines()).thenReturn(java.util.List.of());
        FileProcessResult result = processUtils.execute(pb, 1000, stdoutRedirect, 1000);
        assertEquals(1000, result.processTimeMillis);
        assertEquals(0, result.stderrLength);
        assertEquals(0, result.stdoutLength);
        assertFalse(result.isTimeout);
        assertEquals(0, result.exitValue);
        assertEquals("", result.stdout);
        assertFalse(result.stderrTruncated);
    }

    @Test
    public void testExecuteTimeout() throws IOException, InterruptedException {
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(false);
        when(process.exitValue()).thenReturn(0);
        when(errGobbler.getStreamLength()).thenReturn(0L);
        when(errGobbler.getIsTruncated()).thenReturn(false);
        when(errGobbler.getLines()).thenReturn(java.util.List.of());
        FileProcessResult result = processUtils.execute(pb, 1000, stdoutRedirect, 1000);
        assertTrue(result.isTimeout);
    }

    @Test
    public void testExecuteException() throws IOException, InterruptedException {
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException());
        when(process.exitValue()).thenReturn(0);
        when(errGobbler.getStreamLength()).thenReturn(0L);
        when(errGobbler.getIsTruncated()).thenReturn(false);
        when(errGobbler.getLines()).thenReturn(java.util.List.of());
        FileProcessResult result = processUtils.execute(pb, 1000, stdoutRedirect, 1000);
        assertEquals(-1000, result.exitValue);
    }

    @Test
    public void testExecuteWithError() throws IOException, InterruptedException {
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(1);
        when(errGobbler.getStreamLength()).thenReturn(10L);
        when(errGobbler.getIsTruncated()).thenReturn(false);
        when(errGobbler.getLines()).thenReturn(java.util.List.of("error"));
        FileProcessResult result = processUtils.execute(pb, 1000, stdoutRedirect, 1000);
        assertEquals(1000, result.processTimeMillis);
        assertEquals(10, result.stderrLength);
        assertEquals(0, result.stdoutLength);
        assertFalse(result.isTimeout);
        assertEquals(1, result.exitValue);
        assertEquals("error", result.stderr);
        assertFalse(result.stderrTruncated);
    }
}
