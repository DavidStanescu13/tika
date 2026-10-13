package org.apache.tika.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_escapeCommandLine_2_1_Test {

    @Test
    public void testEscapeCommandLine_NullInput() {
        String result = ProcessUtils.escapeCommandLine(null);
        assertEquals(null, result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces() {
        String input = "test";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals(input, result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Windows() {
        String input = "test arg";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NoQuotes_Windows() {
        String input = "test arg";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Quotes_Windows() {
        String input = "\"test arg\"";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals("\"test arg\"", result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_NoQuotes_NoWindows() {
        String input = "test arg";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals(input, result);
    }

    @Test
    public void testEscapeCommandLine_WithSpaces_Quotes_NoWindows() {
        String input = "\"test arg\"";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals(input, result);
    }
}
