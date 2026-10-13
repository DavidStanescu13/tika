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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @EnabledOnOs(OS.WINDOWS)
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
