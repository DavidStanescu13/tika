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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProcessUtils_escapeCommandLine_2_0_Test {

    @Test
    public void testEscapeCommandLine_NullInput() {
        String result = ProcessUtils.escapeCommandLine(null);
        assertNull(result);
    }

    @Test
    public void testEscapeCommandLine_NoSpaces() {
        String input = "test";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals(input, result);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testEscapeCommandLine_WithSpaces_Windows() {
        String input = "test command";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals("\"test command\"", result);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testEscapeCommandLine_WithSpaces_NoWindows() {
        String input = "test command";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals(input, result);
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    public void testEscapeCommandLine_WithQuotes_Windows() {
        String input = "\"test command\"";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals("\"test command\"", result);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testEscapeCommandLine_WithQuotes_NoWindows() {
        String input = "\"test command\"";
        String result = ProcessUtils.escapeCommandLine(input);
        assertEquals(input, result);
    }
}
