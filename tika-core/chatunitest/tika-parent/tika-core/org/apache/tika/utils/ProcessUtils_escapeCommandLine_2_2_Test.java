package org.apache.tika.utils;

import org.apache.tika.utils.ProcessUtils;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessUtils_escapeCommandLine_2_2_Test {

    @Test
    public void testEscapeCommandLine_NullInput() {
        String result = ProcessUtils.escapeCommandLine(null);
        assertEquals(null, result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces() {
        String result = ProcessUtils.escapeCommandLine("test");
        assertEquals("test", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Windows() {
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NoQuotes_Windows() {
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Quotes_Windows() {
        String result = ProcessUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NoQuotes_NoWindows() {
        String result = ProcessUtils.escapeCommandLine("test arg");
        assertEquals("test arg", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Quotes_NoWindows() {
        String result = ProcessUtils.escapeCommandLine("\"test arg\"");
        assertEquals("\"test arg\"", result);
    }
}
