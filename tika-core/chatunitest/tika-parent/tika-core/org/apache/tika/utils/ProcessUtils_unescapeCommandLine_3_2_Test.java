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
public class ProcessUtils_unescapeCommandLine_3_2_Test {

    @Test
    public void testUnescapeCommandLine() {
        // Test case 1: Input does not contain spaces and is not a Windows command
        String input1 = "example";
        String expected1 = "example";
        assertEquals(expected1, ProcessUtils.unescapeCommandLine(input1));
        // Test case 2: Input contains spaces and is a Windows command
        String input2 = "\"example command\"";
        String expected2 = "example command";
        assertEquals(expected2, ProcessUtils.unescapeCommandLine(input2));
        // Test case 3: Input does not contain spaces and is a Windows command
        String input3 = "examplecommand";
        String expected3 = "examplecommand";
        assertEquals(expected3, ProcessUtils.unescapeCommandLine(input3));
        // Test case 4: Input contains spaces and is not a Windows command
        String input4 = "example command";
        String expected4 = "example command";
        assertEquals(expected4, ProcessUtils.unescapeCommandLine(input4));
    }
}
