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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
    @EnabledOnOs(OS.WINDOWS)
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
    @DisabledOnOs(OS.WINDOWS)
    public void testUnescapeCommandLine_QuotesNonWindows() {
        String input = "\"example\"";
        String expected = "\"example\"";
        String result = ProcessUtils.unescapeCommandLine(input);
        assertEquals(expected, result);
    }
}
