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
public class ProcessUtils_unescapeCommandLine_3_1_Test {

    @Test
    public void testUnescapeCommandLine_NoQuotes() {
        String input = "example";
        String expected = "example";
        String result = ProcessUtils.unescapeCommandLine(input);
        assertEquals(expected, result);
    }

    @Test
    public void testUnescapeCommandLine_QuotesWindows() {
        String input = "\"example\"";
        String expected = "example";
        String result = ProcessUtils.unescapeCommandLine(input);
        assertEquals(expected, result);
    }

    @Test
    public void testUnescapeCommandLine_NoQuotesNonWindows() {
        String input = "example";
        String expected = "example";
        String result = ProcessUtils.unescapeCommandLine(input);
        assertEquals(expected, result);
    }

    @Test
    public void testUnescapeCommandLine_QuotesNonWindows() {
        String input = "\"example\"";
        String expected = "\"example\"";
        String result = ProcessUtils.unescapeCommandLine(input);
        assertEquals(expected, result);
    }
}
