/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_execute_5_4_Test {

    @Mock
    private ProcessBuilder processBuilder;

    @Mock
    private Process process;

    @InjectMocks
    private ProcessUtils processUtils;

    private Path stdoutRedirect;

    @BeforeEach
    public void setUp() throws IOException {
        stdoutRedirect = Files.createTempFile("test", ".txt");
    }

    @Test
    public void testExecute_Success() throws IOException, InterruptedException {
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(true);
        when(process.exitValue()).thenReturn(0);
        FileProcessResult result = processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000);
        assertTrue(result.processTimeMillis >= 0);
        assertEquals(0, result.stderrLength);
        assertEquals(0L, result.stdoutLength);
        assertFalse(result.isTimeout);
        assertEquals(0, result.exitValue);
        assertEquals("", result.stderr);
        assertFalse(result.stdoutTruncated);
        assertFalse(result.stderrTruncated);
    }

    @Test
    public void testExecute_Timeout() throws IOException, InterruptedException {
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenReturn(false);
        FileProcessResult result = processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000);
        assertTrue(result.processTimeMillis >= 0);
        assertEquals(0, result.stderrLength);
        assertEquals(0L, result.stdoutLength);
        assertTrue(result.isTimeout);
        assertEquals(-1, result.exitValue);
        assertEquals("", result.stderr);
        assertFalse(result.stdoutTruncated);
        assertFalse(result.stderrTruncated);
    }

    @Test
    public void testExecute_IOException() throws IOException {
        when(processBuilder.start()).thenThrow(new IOException("Test exception"));
        assertThrows(IOException.class, () -> processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000));
    }

    @Test
    public void testExecute_InterruptedException() throws IOException, InterruptedException {
        when(processBuilder.start()).thenReturn(process);
        when(process.waitFor(1000, TimeUnit.MILLISECONDS)).thenThrow(new InterruptedException("Test exception"));
        FileProcessResult result = processUtils.execute(processBuilder, 1000, stdoutRedirect, 1000);
        assertEquals(-1, result.processTimeMillis);
        assertEquals(0, result.stderrLength);
        assertEquals(0L, result.stdoutLength);
        assertTrue(result.isTimeout);
        assertEquals(-1000, result.exitValue);
        assertEquals("", result.stderr);
        assertFalse(result.stdoutTruncated);
        assertFalse(result.stderrTruncated);
    }
}
