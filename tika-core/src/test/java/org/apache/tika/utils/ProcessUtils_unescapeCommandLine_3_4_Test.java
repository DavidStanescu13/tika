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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

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
        String expected2 = SystemUtils.IS_OS_WINDOWS ? "example with spaces" : input2;
        assertEquals(expected2, ProcessUtils.unescapeCommandLine(input2));
        // Test case 3: Input string contains spaces but is not a Windows quoted string
        String input3 = "example with spaces";
        String expected3 = "example with spaces";
        assertEquals(expected3, ProcessUtils.unescapeCommandLine(input3));
        // Test case 4: Input string is a Windows quoted string but does not contain spaces
        String input4 = "\"example\"";
        String expected4 = input4;
        assertEquals(expected4, ProcessUtils.unescapeCommandLine(input4));
    }
}
