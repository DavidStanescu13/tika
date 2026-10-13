package org.apache.tika.utils;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.mockito.*;
import org.junit.jupiter.api.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_unescapeCommandLine_3_2_Test {

    @Mock
    private SystemUtils systemUtils;

    @Test
    public void testUnescapeCommandLine() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        when(systemUtils.IS_OS_WINDOWS).thenReturn(true);
        String input = "\"example arg\"";
        String expectedOutput = "example arg";
        String result = processUtils.unescapeCommandLine(input);
        assertEquals(expectedOutput, result);
    }

    @Test
    public void testUnescapeCommandLineNoQuotes() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        when(systemUtils.IS_OS_WINDOWS).thenReturn(true);
        String input = "example arg";
        String expectedOutput = "example arg";
        String result = processUtils.unescapeCommandLine(input);
        assertEquals(expectedOutput, result);
    }

    @Test
    public void testUnescapeCommandLineNonWindows() throws IOException {
        ProcessUtils processUtils = new ProcessUtils();
        when(systemUtils.IS_OS_WINDOWS).thenReturn(false);
        String input = "\"example arg\"";
        String expectedOutput = "\"example arg\"";
        String result = processUtils.unescapeCommandLine(input);
        assertEquals(expectedOutput, result);
    }
}
