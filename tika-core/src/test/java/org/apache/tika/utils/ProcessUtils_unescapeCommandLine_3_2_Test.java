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
public class ProcessUtils_unescapeCommandLine_3_2_Test {

    @Test
    public void testUnescapeCommandLine() {
        // Test case 1: Input does not contain spaces and is not a Windows command
        String input1 = "example";
        String expected1 = "example";
        assertEquals(expected1, ProcessUtils.unescapeCommandLine(input1));
        // Test case 2: Input contains spaces and is a Windows command
        String input2 = "\"example command\"";
        String expected2 = SystemUtils.IS_OS_WINDOWS ? "example command" : input2;
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
