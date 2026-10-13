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
public class ProcessUtils_unescapeCommandLine_3_0_Test {

    @Test
    public void testUnescapeCommandLine() {
        // Test case 1: Argument does not contain spaces and is not a Windows quoted string
        String arg1 = "example";
        String expected1 = "example";
        assertEquals(expected1, ProcessUtils.unescapeCommandLine(arg1));
        // Test case 2: Argument contains spaces and is a Windows quoted string
        String arg2 = "\"example arg\"";
        String expected2 = "example arg";
        assertEquals(expected2, ProcessUtils.unescapeCommandLine(arg2));
        // Test case 3: Argument does not contain spaces and is a Windows quoted string
        String arg3 = "\"example\"";
        String expected3 = "example";
        assertEquals(expected3, ProcessUtils.unescapeCommandLine(arg3));
        // Test case 4: Argument contains spaces and is not a Windows quoted string
        String arg4 = "example arg";
        String expected4 = "example arg";
        assertEquals(expected4, ProcessUtils.unescapeCommandLine(arg4));
    }
}
