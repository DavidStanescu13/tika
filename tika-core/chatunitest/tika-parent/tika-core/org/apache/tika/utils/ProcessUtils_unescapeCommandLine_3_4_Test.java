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
public class ProcessUtils_unescapeCommandLine_3_4_Test {

    @Test
    public void testUnescapeCommandLine() {
        // Test case 1: Input string does not contain spaces and is not a Windows quoted string
        String input1 = "example";
        String expected1 = "example";
        assertEquals(expected1, ProcessUtils.unescapeCommandLine(input1));
        // Test case 2: Input string contains spaces and is a Windows quoted string
        String input2 = "\"example with spaces\"";
        String expected2 = "example with spaces";
        assertEquals(expected2, ProcessUtils.unescapeCommandLine(input2));
        // Test case 3: Input string contains spaces but is not a Windows quoted string
        String input3 = "example with spaces";
        String expected3 = "example with spaces";
        assertEquals(expected3, ProcessUtils.unescapeCommandLine(input3));
        // Test case 4: Input string is a Windows quoted string but does not contain spaces
        String input4 = "\"example\"";
        String expected4 = "example";
        assertEquals(expected4, ProcessUtils.unescapeCommandLine(input4));
    }
}
