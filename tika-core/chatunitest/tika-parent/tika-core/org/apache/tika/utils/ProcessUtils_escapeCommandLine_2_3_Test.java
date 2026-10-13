package org.apache.tika.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_escapeCommandLine_2_3_Test {

    @Mock
    private SystemUtils systemUtils;

    @Test
    public void testEscapeCommandLine_NullInput() {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine(null);
        assertNull(result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces_Windows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Windows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_Windows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_NoSpaces_Windows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("\"test\"");
        assertEquals("\"test\"", result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces_NotWindows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NotWindows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("test arg");
        assertEquals("test arg", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_NotWindows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_NoSpaces_NotWindows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        String result = processUtils.escapeCommandLine("\"test\"");
        assertEquals("\"test\"", result);
    }
}
