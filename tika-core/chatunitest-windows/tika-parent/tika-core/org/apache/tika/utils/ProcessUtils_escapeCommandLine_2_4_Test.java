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
public class ProcessUtils_escapeCommandLine_2_4_Test {

    @Mock
    private SystemUtils systemUtils;

    @Test
    public void testEscapeCommandLine_NullInput() {
        String result = ProcessUtils.escapeCommandLine(null);
        assertNull(result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces_Windows() {
        String result = ProcessUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Windows() {
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_Windows() {
        String result = ProcessUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces_NotWindows() {
        String result = ProcessUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NotWindows() {
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("test arg", result);
    }

    @Test
    public void testEscapeCommandLine_WithQuotes_NotWindows() {
        String result = ProcessUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }
}
